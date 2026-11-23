package jp.co.hikari.hanbai.master.model;

import java.time.LocalDateTime;

/** ユーザー（TBL-001 m_user）の1行。パスワードハッシュは保持しない（認証は common.security が扱う）。 */
public class User {

  private Long userId;
  private String loginId;
  private String userName;
  private String department;
  private Long defaultWarehouseId;
  private boolean enabled;
  private boolean locked;
  private int failedLoginCount;
  private LocalDateTime lastLoginAt;
  private int version;

  /**
   * ユーザーIDを返す。
   *
   * @return ユーザーID
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * ユーザーIDを設定する。
   *
   * @param userId ユーザーID
   */
  public void setUserId(Long userId) {
    this.userId = userId;
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
   * 氏名を返す。
   *
   * @return 氏名
   */
  public String getUserName() {
    return userName;
  }

  /**
   * 氏名を設定する。
   *
   * @param userName 氏名
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * 所属を返す。
   *
   * @return 所属
   */
  public String getDepartment() {
    return department;
  }

  /**
   * 所属を設定する。
   *
   * @param department 所属
   */
  public void setDepartment(String department) {
    this.department = department;
  }

  /**
   * 所属倉庫IDを返す。
   *
   * @return 所属倉庫ID
   */
  public Long getDefaultWarehouseId() {
    return defaultWarehouseId;
  }

  /**
   * 所属倉庫IDを設定する。
   *
   * @param defaultWarehouseId 所属倉庫ID
   */
  public void setDefaultWarehouseId(Long defaultWarehouseId) {
    this.defaultWarehouseId = defaultWarehouseId;
  }

  /**
   * 有効フラグを返す。
   *
   * @return 有効フラグ
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * 有効フラグを設定する。
   *
   * @param enabled 有効フラグ
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * ロックフラグを返す。
   *
   * @return ロックフラグ
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * ロックフラグを設定する。
   *
   * @param locked ロックフラグ
   */
  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  /**
   * 認証失敗回数を返す。
   *
   * @return 認証失敗回数
   */
  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  /**
   * 認証失敗回数を設定する。
   *
   * @param failedLoginCount 認証失敗回数
   */
  public void setFailedLoginCount(int failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  /**
   * 最終ログイン日時を返す。
   *
   * @return 最終ログイン日時
   */
  public LocalDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  /**
   * 最終ログイン日時を設定する。
   *
   * @param lastLoginAt 最終ログイン日時
   */
  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * バージョンを返す。
   *
   * @return バージョン
   */
  public int getVersion() {
    return version;
  }

  /**
   * バージョンを設定する。
   *
   * @param version バージョン
   */
  public void setVersion(int version) {
    this.version = version;
  }
}
