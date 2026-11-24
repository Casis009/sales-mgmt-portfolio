package jp.co.hikari.hanbai.master.dto;

/**
 * マスタ一覧（SCR-101・SCR-111・SCR-121）の検索条件。null の項目は条件にしない。
 *
 * @param keyword キーワード（コード・名称などの部分一致）
 * @param partyType 取引先区分（取引先一覧のみ。CUSTOMER または SUPPLIER）
 * @param supplierId 主仕入先ID（商品一覧のみ）
 * @param includeDisabled 無効のデータも表示する場合 true
 */
public record MasterCriteria(
    String keyword, PartyType partyType, Long supplierId, boolean includeDisabled) {

  /** 取引先区分の検索条件。 */
  public enum PartyType {
    /** 得意先。 */
    CUSTOMER,
    /** 仕入先。 */
    SUPPLIER
  }

  /**
   * 部分一致検索に使うパターンを返す。LIKE の特殊文字はエスケープする。
   *
   * @return パターン（キーワードの指定がない場合は null）
   */
  public String keywordPattern() {
    if (keyword == null) {
      return null;
    }
    return "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
