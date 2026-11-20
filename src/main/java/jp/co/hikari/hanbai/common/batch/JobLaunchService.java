package jp.co.hikari.hanbai.common.batch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

/**
 * ジョブ起動部品（バッチ詳細設計 1章・4章）。画面（SCR-501）と BatchScheduler から使用する。
 *
 * <ul>
 *   <li>ジョブは {@code BatchConfig} の TaskExecutor で非同期に実行される。本クラスは実行IDを返してすぐに戻る。
 *   <li>同じジョブが実行中の場合は起動しない（E-052）。
 *   <li>業務パラメータに起動日時（launchedAt）を加え、同じ業務パラメータで何度でも実行できるようにする。
 * </ul>
 */
@Service
public class JobLaunchService {

  /** 二重起動のメッセージID（メッセージ一覧）。 */
  public static final String MSG_ALREADY_RUNNING = "E-052";

  /** 実行ごとに異なる値を持たせるためのパラメータ名。 */
  public static final String PARAM_LAUNCHED_AT = "launchedAt";

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;
  private final Clock clock;

  /**
   * コンストラクタ。
   *
   * @param jobOperator ジョブの起動
   * @param jobRepository 実行管理情報の参照
   * @param clock 現在日時の取得元
   */
  public JobLaunchService(JobOperator jobOperator, JobRepository jobRepository, Clock clock) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.clock = clock;
  }

  /**
   * ジョブを非同期で起動する。
   *
   * <p>二重起動の確認と起動を1つの JVM 内で直列化するため synchronized とする（アプリケーションは1台構成）。
   * 起動処理は実行管理情報を登録した時点で戻るため、ロックを保持する時間は短い。
   *
   * @param job 起動するジョブ
   * @param businessParameters 業務パラメータ
   * @return 実行ID
   * @throws BusinessException 同じジョブが実行中の場合（E-052）
   */
  public synchronized long launch(Job job, JobParameters businessParameters) {
    if (!jobRepository.findRunningJobExecutions(job.getName()).isEmpty()) {
      throw new BusinessException(MSG_ALREADY_RUNNING);
    }
    JobParameters parameters =
        new JobParametersBuilder(businessParameters)
            .addLocalDateTime(PARAM_LAUNCHED_AT, LocalDateTime.now(clock))
            .toJobParameters();
    try {
      return jobOperator.start(job, parameters).getId();
    } catch (Exception e) {
      // launchedAt により毎回新しいジョブインスタンスになるため、再開・完了済みの例外は通常発生しない
      throw new IllegalStateException("ジョブを起動できません: " + job.getName(), e);
    }
  }

  /**
   * 実行状況を取得する。
   *
   * @param jobName ジョブ名（他のジョブの実行IDを指定された場合に空を返すため）
   * @param executionId 実行ID
   * @return 実行状況。実行IDが存在しない、または別のジョブの実行IDの場合は空
   */
  public Optional<JobProgress> findProgress(String jobName, long executionId) {
    JobExecution execution;
    try {
      execution = jobRepository.getJobExecution(executionId);
    } catch (EmptyResultDataAccessException e) {
      // Spring Batch 6 の JDBC 実装は、存在しない実行IDに対して null ではなく例外を送出する（技術検証 P-05）
      return Optional.empty();
    }
    if (execution == null || !jobName.equals(execution.getJobInstance().getJobName())) {
      return Optional.empty();
    }
    long readCount = 0;
    long writeCount = 0;
    for (StepExecution step : execution.getStepExecutions()) {
      readCount += step.getReadCount();
      writeCount += step.getWriteCount();
    }
    return Optional.of(
        new JobProgress(
            executionId,
            jobName,
            toStatus(execution.getStatus()),
            readCount,
            writeCount,
            execution.getStartTime(),
            execution.getEndTime()));
  }

  private static JobProgress.Status toStatus(BatchStatus status) {
    return switch (status) {
      case STARTING, STARTED, STOPPING -> JobProgress.Status.RUNNING;
      case COMPLETED -> JobProgress.Status.COMPLETED;
      default -> JobProgress.Status.FAILED;
    };
  }
}
