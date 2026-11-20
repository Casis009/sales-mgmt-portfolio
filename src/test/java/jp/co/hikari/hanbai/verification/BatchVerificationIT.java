package jp.co.hikari.hanbai.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.batch.JobLaunchService;
import jp.co.hikari.hanbai.common.batch.JobLogListener;
import jp.co.hikari.hanbai.common.batch.JobProgress;
import jp.co.hikari.hanbai.common.batch.StaleJobExecutionRecoverer;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 技術検証 P-05：Spring Batch（Boot 4.1 同梱の 6.0 系）で、画面からの非同期起動と実行状況の取得ができること。
 *
 * <p>合格基準（技術検証結果報告書 2章）：ジョブを非同期で起動し、実行中・完了・異常終了の状態と処理件数を取得できること。
 * 同じジョブの二重起動を検知できること。あわせて、停止時に実行中のまま残った実行の復旧（バッチ詳細設計 5章）も確認する。
 *
 * <p>検証用ジョブは請求締め（BAT-03）と同じ構成（チャンクサイズ 1、Reader が対象IDの一覧を返す）とする。
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BatchVerificationIT.VerificationJobConfig.class})
class BatchVerificationIT {

  /** Writer が処理を進めてよいかを制御する（実行中の状態を観測するため）。 */
  static volatile CountDownLatch gate = new CountDownLatch(0);

  /** Writer が例外を送出する得意先ID（異常終了の検証用）。0 は送出しない。 */
  static volatile long failOnId;

  @Autowired JobLaunchService launcher;
  @Autowired JobRepository jobRepository;
  @Autowired StaleJobExecutionRecoverer recoverer;
  @Autowired JdbcTemplate jdbc;

  @Autowired
  @Qualifier("verificationJob")
  Job verificationJob;

  @AfterEach
  void tearDown() {
    gate.countDown();
    failOnId = 0;
    await().atMost(Duration.ofSeconds(10))
        .until(() -> jobRepository.findRunningJobExecutions("verificationJob").isEmpty());
  }

  @Test
  void 非同期で起動し_実行中の状態を取得でき_完了後に件数を取得できる() {
    gate = new CountDownLatch(1);

    long executionId = launcher.launch(verificationJob, params("2027-04-30"));

    // launch はジョブの完了を待たずに戻る（非同期）
    assertThat(launcher.findProgress("verificationJob", executionId))
        .get()
        .extracting(JobProgress::status)
        .isEqualTo(JobProgress.Status.RUNNING);

    gate.countDown();
    JobProgress done = awaitFinished(executionId);
    assertThat(done.status()).isEqualTo(JobProgress.Status.COMPLETED);
    assertThat(done.readCount()).isEqualTo(3);
    assertThat(done.writeCount()).isEqualTo(3);
    assertThat(done.endTime()).isNotNull();
  }

  @Test
  void 同じジョブの実行中に起動すると_E052の業務エラーになる() {
    gate = new CountDownLatch(1);
    launcher.launch(verificationJob, params("2027-04-30"));

    assertThatThrownBy(() -> launcher.launch(verificationJob, params("2027-04-30")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getMessageId())
        .isEqualTo(JobLaunchService.MSG_ALREADY_RUNNING);
  }

  @Test
  void 同じ業務パラメータで_完了後に再実行できる() {
    long first = launcher.launch(verificationJob, params("2027-04-30"));
    awaitFinished(first);
    long second = launcher.launch(verificationJob, params("2027-04-30"));

    assertThat(awaitFinished(second).status()).isEqualTo(JobProgress.Status.COMPLETED);
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void 処理中に例外が発生すると_異常終了となり_それまでの得意先はコミットされている() {
    failOnId = 2;

    long executionId = launcher.launch(verificationJob, params("2027-04-30"));
    JobProgress done = awaitFinished(executionId);

    assertThat(done.status()).isEqualTo(JobProgress.Status.FAILED);
    // チャンクサイズ 1：1社目は書き込み済み（コミット済み）、2社目で異常終了
    assertThat(done.writeCount()).isEqualTo(1);
  }

  @Test
  void 他のジョブ名を指定した場合や存在しない実行IDは_空を返す() {
    long executionId = launcher.launch(verificationJob, params("2027-04-30"));
    awaitFinished(executionId);

    assertThat(launcher.findProgress("invoiceClosingJob", executionId)).isEmpty();
    assertThat(launcher.findProgress("verificationJob", 999_999L)).isEmpty();
  }

  @Test
  void 停止で実行中のまま残った実行は_起動時の復旧処理で異常終了になる() {
    // アプリケーションが停止して「実行中」のまま残った状態を、実行管理テーブルに直接作る
    JobParameters parameters = params("2027-03-31");
    JobExecution stale =
        jobRepository.createJobExecution(
            jobRepository.createJobInstance("verificationJob", parameters),
            parameters,
            new org.springframework.batch.infrastructure.item.ExecutionContext());
    stale.setStatus(BatchStatus.STARTED);
    stale.setStartTime(java.time.LocalDateTime.now());
    jobRepository.update(stale);
    assertThatThrownBy(() -> launcher.launch(verificationJob, params("2027-04-30")))
        .isInstanceOf(BusinessException.class);

    assertThat(recoverer.recoverAll()).isEqualTo(1);

    assertThat(jobRepository.getJobExecution(stale.getId()).getStatus())
        .isEqualTo(BatchStatus.FAILED);
    long executionId = launcher.launch(verificationJob, params("2027-04-30"));
    assertThat(awaitFinished(executionId).status()).isEqualTo(JobProgress.Status.COMPLETED);
  }

  private static JobParameters params(String closingDate) {
    return new JobParametersBuilder().addString("closingDate", closingDate).toJobParameters();
  }

  private JobProgress awaitFinished(long executionId) {
    return await()
        .atMost(Duration.ofSeconds(20))
        .pollInterval(Duration.ofMillis(100))
        .until(
            () -> launcher.findProgress("verificationJob", executionId).orElseThrow(),
            p -> p.status() != JobProgress.Status.RUNNING);
  }

  /** 検証用ジョブ。請求締め（BAT-03）と同じく、対象IDを1件ずつ読み、1件ごとにコミットする。 */
  @TestConfiguration(proxyBeanMethods = false)
  static class VerificationJobConfig {

    /**
     * 対象IDの一覧を返す Reader。
     *
     * <p>{@link StepScope} により、ステップの実行ごとに新しい Reader を作る。シングルトンにすると、1回目の実行で
     * 一覧を読み終えた Reader が2回目以降も使われ、0件で「完了」になる（P-05 の検証で判明）。
     */
    @Bean
    @StepScope
    ListItemReader<Long> verificationReader() {
      return new ListItemReader<>(List.of(1L, 2L, 3L));
    }

    @Bean
    Job verificationJob(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        JobLogListener listener,
        ListItemReader<Long> verificationReader) {
      var step =
          new StepBuilder("verificationStep", jobRepository)
              .<Long, Long>chunk(1)
              .transactionManager(transactionManager)
              .reader(verificationReader)
              .writer(
                  chunk -> {
                    gate.await(10, TimeUnit.SECONDS);
                    for (Long id : chunk) {
                      if (id == failOnId) {
                        throw new IllegalStateException("検証用の例外: id=" + id);
                      }
                    }
                  })
              .build();
      return new JobBuilder("verificationJob", jobRepository)
          .listener(listener)
          .start(step)
          .build();
    }
  }
}
