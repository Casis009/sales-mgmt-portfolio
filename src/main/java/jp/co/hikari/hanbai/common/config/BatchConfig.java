package jp.co.hikari.hanbai.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Batch の設定（バッチ詳細設計 1章）。
 *
 * <p>{@link BatchTaskExecutor} を付けた TaskExecutor を定義すると、Spring Boot が自動構成する {@code JobOperator}
 * がこのスレッドでジョブを実行する。これにより、ジョブの起動は実行IDを返してすぐに戻り（非同期）、画面は実行状況を
 * 別のリクエストで取得できる（技術検証 P-05）。
 */
@Configuration(proxyBeanMethods = false)
public class BatchConfig {

  /**
   * バッチ実行用のスレッドプール。
   *
   * @param poolSize スレッド数（hanbai.batch.pool-size）
   * @return TaskExecutor
   */
  @Bean
  @BatchTaskExecutor
  public TaskExecutor batchTaskExecutor(@Value("${hanbai.batch.pool-size:3}") int poolSize) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    // キューに溜めない。BAT-01～03 は同じジョブを二重に起動しないため、スレッド数を超えることはない
    executor.setQueueCapacity(0);
    executor.setThreadNamePrefix("batch-");
    // アプリケーション停止時は実行中のジョブの終了を待つ
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }
}
