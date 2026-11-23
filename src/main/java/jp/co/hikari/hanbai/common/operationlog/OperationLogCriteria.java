package jp.co.hikari.hanbai.common.operationlog;

import java.time.LocalDateTime;
import jp.co.hikari.hanbai.common.code.OperationType;

/**
 * 操作ログの検索条件（SCR-013）。null の項目は条件にしない。
 *
 * @param operatedFrom 操作日時（以上）
 * @param operatedToExclusive 操作日時（未満）
 * @param loginId ログインID（完全一致）
 * @param operationType 操作種別
 * @param target 対象種別または対象キー（部分一致）
 */
public record OperationLogCriteria(
    LocalDateTime operatedFrom,
    LocalDateTime operatedToExclusive,
    String loginId,
    OperationType operationType,
    String target) {

  /**
   * 対象の部分一致検索に使うパターンを返す。LIKE の特殊文字はエスケープする。
   *
   * @return 例：%SO2701%（対象の指定がない場合は null）
   */
  public String targetPattern() {
    if (target == null) {
      return null;
    }
    String escaped = target.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
