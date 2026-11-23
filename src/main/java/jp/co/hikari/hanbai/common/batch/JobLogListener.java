package jp.co.hikari.hanbai.common.batch;

import java.util.Map;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.operationlog.OperationLog;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * 共通リスナー（バッチ詳細設計 1章）。ジョブの開始・終了をアプリケーションログと操作ログ（BATCH）に出力する。
 *
 * <p>異常終了は ERROR で出力し、監視が検知する。操作ログはジョブの結果によらず残すため、独立したトランザクションで登録する。
 */
@Component
public class JobLogListener implements JobExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(JobLogListener.class);

  /** ジョブ名とバッチID（バッチ一覧）の対応。操作ログの画面IDに記録する。 */
  private static final Map<String, String> BATCH_IDS =
      Map.of(
          "reorderPointCheckJob", "BAT-01",
          "monthlyStockSummaryJob", "BAT-02",
          "invoiceClosingJob", "BAT-03");

  private static final String TARGET_TYPE = "batch_job_execution";

  private final OperationLogService operationLogService;

  /**
   * 操作ログの登録部品を受け取る。
   *
   * @param operationLogService 操作ログ
   */
  public JobLogListener(OperationLogService operationLogService) {
    this.operationLogService = operationLogService;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    LOG.info(
        "ジョブ開始: job={}, executionId={}, parameters={}",
        jobExecution.getJobInstance().getJobName(),
        jobExecution.getId(),
        jobExecution.getJobParameters());
    record(jobExecution, OperationResult.SUCCESS, "開始");
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
      record(jobExecution, OperationResult.SUCCESS, "終了 read=" + read + ", write=" + write);
    } else {
      LOG.error(
          "ジョブ異常終了: job={}, executionId={}, status={}, exitDescription={}",
          jobName,
          jobExecution.getId(),
          jobExecution.getStatus(),
          jobExecution.getExitStatus().getExitDescription());
      record(
          jobExecution, OperationResult.FAILURE, "異常終了 status=" + jobExecution.getStatus());
    }
  }

  private void record(JobExecution jobExecution, OperationResult result, String detail) {
    String jobName = jobExecution.getJobInstance().getJobName();
    OperationLog log = OperationLog.of(OperationType.BATCH, result);
    log.setScreenId(BATCH_IDS.get(jobName));
    log.setTargetType(TARGET_TYPE);
    log.setTargetKey(jobName + "#" + jobExecution.getId());
    log.setDetail(detail);
    operationLogService.recordIndependently(log);
  }
}
