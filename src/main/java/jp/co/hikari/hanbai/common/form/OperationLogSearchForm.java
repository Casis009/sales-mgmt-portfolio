package jp.co.hikari.hanbai.common.form;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.operationlog.OperationLogCriteria;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * SCR-013 操作ログ照会の検索条件（操作日時（範囲）、ユーザー、操作種別、対象。画面一覧 5.3）。
 *
 * <p>操作日時は分単位で入力し、終了は入力した分を含む。初期表示では検索しない（画面一覧 5.2 No.1）。
 */
public class OperationLogSearchForm {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime operatedFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime operatedTo;

  @Size(max = 20)
  private String loginId;

  private OperationType operationType;

  @Size(max = 50)
  private String target;

  private Integer page;

  private String search;

  /**
   * 検索条件に変換する。終了日時は、その分の終わりまでを含める。
   *
   * @return 検索条件
   */
  public OperationLogCriteria toCriteria() {
    LocalDateTime toExclusive = operatedTo == null ? null : operatedTo.plusMinutes(1);
    return new OperationLogCriteria(operatedFrom, toExclusive, loginId, operationType, target);
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
   * 操作日時（開始）を返す。
   *
   * @return 操作日時（開始）
   */
  public LocalDateTime getOperatedFrom() {
    return operatedFrom;
  }

  /**
   * 操作日時（開始）を設定する。
   *
   * @param operatedFrom 操作日時（開始）
   */
  public void setOperatedFrom(LocalDateTime operatedFrom) {
    this.operatedFrom = operatedFrom;
  }

  /**
   * 操作日時（終了）を返す。
   *
   * @return 操作日時（終了）
   */
  public LocalDateTime getOperatedTo() {
    return operatedTo;
  }

  /**
   * 操作日時（終了）を設定する。
   *
   * @param operatedTo 操作日時（終了）
   */
  public void setOperatedTo(LocalDateTime operatedTo) {
    this.operatedTo = operatedTo;
  }

  /**
   * ログインIDを返す。
   *
   * @return ログインID
   */
  public String getLoginId() {
    return loginId;
  }

  /**
   * ログインIDを設定する。
   *
   * @param loginId ログインID
   */
  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  /**
   * 操作種別を返す。
   *
   * @return 操作種別
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * 操作種別を設定する。
   *
   * @param operationType 操作種別
   */
  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  /**
   * 対象を返す。
   *
   * @return 対象種別・対象キー（部分一致）
   */
  public String getTarget() {
    return target;
  }

  /**
   * 対象を設定する。
   *
   * @param target 対象種別・対象キー
   */
  public void setTarget(String target) {
    this.target = target;
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
