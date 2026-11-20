package jp.co.hikari.hanbai.common.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 起動時に「実行中」のまま残っているジョブ実行を「異常終了」に更新する（バッチ詳細設計 5章）。
 *
 * <p>バッチ実行中にアプリケーションが停止すると、実行管理テーブルに実行中のまま残り、二重起動チェックで
 * 以後そのジョブを起動できなくなる。アプリケーションは1台構成のため、起動時点で実行中のジョブは存在しない。
 */
@Component
public class StaleJobExecutionRecoverer implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(StaleJobExecutionRecoverer.class);

  private final JobRepository jobRepository;
  private final JobOperator jobOperator;

  /**
   * コンストラクタ。
   *
   * @param jobRepository 実行管理情報の参照
   * @param jobOperator 実行状態の更新
   */
  public StaleJobExecutionRecoverer(JobRepository jobRepository, JobOperator jobOperator) {
    this.jobRepository = jobRepository;
    this.jobOperator = jobOperator;
  }

  @Override
  public void run(ApplicationArguments args) {
    recoverAll();
  }

  /**
   * 実行中のまま残っているジョブ実行をすべて異常終了に更新する。
   *
   * @return 更新した件数
   */
  public int recoverAll() {
    int count = 0;
    for (String jobName : jobRepository.getJobNames()) {
      for (JobExecution execution : jobRepository.findRunningJobExecutions(jobName)) {
        jobOperator.recover(execution);
        LOG.warn(
            "実行中のまま残っていたジョブ実行を異常終了に更新しました: job={}, executionId={}",
            jobName,
            execution.getId());
        count++;
      }
    }
    return count;
  }
}
