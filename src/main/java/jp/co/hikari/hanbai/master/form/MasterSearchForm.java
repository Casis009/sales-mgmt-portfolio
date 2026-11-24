package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.Size;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;

/**
 * マスタ一覧（SCR-101・SCR-111・SCR-121）の検索条件（画面一覧 5.4）。
 *
 * <p>初期表示では検索しない（画面一覧 5.2 No.1）。検索ボタン・ページのリンクは {@code search} パラメータを付けて送信する。
 * 画面ごとに使わない項目（取引先区分、主仕入先）は null のまま送信される。
 */
public class MasterSearchForm {

  @Size(max = 50)
  private String keyword;

  private MasterCriteria.PartyType partyType;

  private Long supplierId;

  private boolean includeDisabled;

  private Integer page;

  private String search;

  /**
   * 検索条件に変換する。
   *
   * @return 検索条件
   */
  public MasterCriteria toCriteria() {
    return new MasterCriteria(keyword, partyType, supplierId, includeDisabled);
  }

  /**
   * 検索を実行するかを返す。
   *
   * @return 検索ボタン・ページのリンクから送信された場合 true
   */
  public boolean isSearchRequested() {
    return search != null;
  }

  /**
   * キーワードを返す。
   *
   * @return キーワード
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   * キーワードを設定する。
   *
   * @param keyword キーワード
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * 取引先区分を返す。
   *
   * @return 取引先区分
   */
  public MasterCriteria.PartyType getPartyType() {
    return partyType;
  }

  /**
   * 取引先区分を設定する。
   *
   * @param partyType 取引先区分
   */
  public void setPartyType(MasterCriteria.PartyType partyType) {
    this.partyType = partyType;
  }

  /**
   * 主仕入先IDを返す。
   *
   * @return 主仕入先ID
   */
  public Long getSupplierId() {
    return supplierId;
  }

  /**
   * 主仕入先IDを設定する。
   *
   * @param supplierId 主仕入先ID
   */
  public void setSupplierId(Long supplierId) {
    this.supplierId = supplierId;
  }

  /**
   * 無効のデータも表示するかを返す。
   *
   * @return 表示する場合 true
   */
  public boolean isIncludeDisabled() {
    return includeDisabled;
  }

  /**
   * 無効のデータも表示するかを設定する。
   *
   * @param includeDisabled 表示する場合 true
   */
  public void setIncludeDisabled(boolean includeDisabled) {
    this.includeDisabled = includeDisabled;
  }

  /**
   * ページ番号を返す。
   *
   * @return ページ番号
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
