package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.Size;
import jp.co.hikari.hanbai.master.dto.UserCriteria;

/**
 * SCR-011 ユーザー一覧の検索条件。
 *
 * <p>初期表示では検索しない（画面一覧 5.2 No.1）。検索ボタン・ページのリンクは {@code search} パラメータを付けて送信する。
 */
public class UserSearchForm {

  @Size(max = 50)
  private String keyword;

  private boolean includeDisabled;

  private Integer page;

  private String search;

  /**
   * 検索条件に変換する。
   *
   * @return 検索条件
   */
  public UserCriteria toCriteria() {
    return new UserCriteria(keyword, includeDisabled);
  }

  /**
   * 検索を実行するか（検索ボタン・ページのリンクから送信されたか）を返す。
   *
   * @return 検索する場合 true
   */
  public boolean isSearchRequested() {
    return search != null;
  }

  /**
   * キーワードを返す。
   *
   * @return ログインID・氏名（部分一致）
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   * キーワードを設定する。
   *
   * @param keyword ログインID・氏名
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * 無効のユーザーも表示するかを返す。
   *
   * @return 表示する場合 true
   */
  public boolean isIncludeDisabled() {
    return includeDisabled;
  }

  /**
   * 無効のユーザーも表示するかを設定する。
   *
   * @param includeDisabled 表示する場合 true
   */
  public void setIncludeDisabled(boolean includeDisabled) {
    this.includeDisabled = includeDisabled;
  }

  /**
   * ページ番号を返す。
   *
   * @return ページ番号（1始まり）
   */
  public Integer getPage() {
    return page;
  }

  /**
   * ページ番号を設定する。
   *
   * @param page ページ番号
   */
  public void setPage(Integer page) {
    this.page = page;
  }

  /**
   * 検索の指定を返す。
   *
   * @return 検索の指定
   */
  public String getSearch() {
    return search;
  }

  /**
   * 検索の指定を設定する。
   *
   * @param search 検索の指定
   */
  public void setSearch(String search) {
    this.search = search;
  }
}
