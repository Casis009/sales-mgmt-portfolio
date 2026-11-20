package jp.co.hikari.hanbai.common.batch;

import java.time.LocalDateTime;

/**
 * ジョブの実行状況（バッチ詳細設計 4.2）。
 *
 * @param executionId 実行ID
 * @param jobName ジョブ名
 * @param status 状態
 * @param readCount 読み込み件数（全ステップの合計。請求締めでは対象得意先数として使う）
 * @param writeCount 書き込み件数（全ステップの合計。請求締めでは処理済み得意先数として使う）
 * @param startTime 開始日時（未開始の場合は null）
 * @param endTime 終了日時（実行中の場合は null）
 */
public record JobProgress(
    long executionId,
    String jobName,
    Status status,
    long readCount,
    long writeCount,
    LocalDateTime startTime,
    LocalDateTime endTime) {

  /** 画面に表示する状態。Spring Batch の状態をこの3つにまとめる。 */
  public enum Status {
    /** 実行中（起動待ちを含む）。 */
    RUNNING,
    /** 完了。 */
    COMPLETED,
    /** 異常終了（停止・放棄を含む）。 */
    FAILED
  }
}
