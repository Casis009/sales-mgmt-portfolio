package jp.co.hikari.hanbai.common.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * 共通リスナー（バッチ詳細設計 1章）。ジョブの開始・終了をアプリケーションログに出力する。
 *
 * <p>異常終了は ERROR で出力し、監視が検知する。操作ログ（BATCH）への記録は、操作ログ部品を実装する製造 2段階で追加する。
 */
@Component
public class JobLogListener implements JobExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(JobLogListener.class);

  @Override
  public void beforeJob(JobExecution jobExecution) {
    LOG.info(
        "ジョブ開始: job={}, executionId={}, parameters={}",
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getId(),
        jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    long read = 0;
    long write = 0;
    for (StepExecution step : jobExecution.getStepExecutions()) {
      read += step.getReadCount();
      write += step.getWriteCount();
    }
    String jobName = jobExecution.getJobInstance().getJobName();
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      LOG.info(
          "ジョブ終了: job={}, executionId={}, read={}, write={}",
          jobName,
          jobExecution.getId(),
          read,
          write);
    } else {
      LOG.error(
          "ジョブ異常終了: job={}, executionId={}, status={}, exitDescription={}",
          jobName,
          jobExecution.getId(),
          jobExecution.getStatus(),
          jobExecution.getExitStatus().getExitDescription());
    }
  }
}
