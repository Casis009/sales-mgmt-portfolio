package jp.co.hikari.hanbai.master.dto;

/**
 * ユーザー一覧（SCR-011）の検索条件。
 *
 * @param keyword ログインID・氏名（部分一致。null は条件にしない）
 * @param includeDisabled 無効のユーザーも表示する場合 true
 */
public record UserCriteria(String keyword, boolean includeDisabled) {

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
