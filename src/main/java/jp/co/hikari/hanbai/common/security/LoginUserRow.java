package jp.co.hikari.hanbai.common.security;

import java.util.ArrayList;
import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;

/** 認証に使うユーザーの検索結果（m_user と m_user_role の結合）。MyBatis の結果の受け取り専用。 */
public class LoginUserRow {

  private long userId;
  private String loginId;
  private String passwordHash;
  private String userName;
  private Long defaultWarehouseId;
  private boolean enabled;
  private boolean locked;
  private int failedLoginCount;
  private boolean passwordChangeRequired;
  private List<Role> roles = new ArrayList<>();

  /**
   * ログインユーザーに変換する。
   *
   * @return ログインユーザー
   */
  public LoginUser toLoginUser() {
    return new LoginUser(
        userId,
        loginId,
        userName,
        defaultWarehouseId,
        roles,
        enabled,
        locked,
        passwordChangeRequired,
        passwordHash);
  }

  /**
   * ユーザーIDを返す。
   *
   * @return ユーザーID
   */
  public long getUserId() {
    return userId;
  }

  /**
   * ユーザーIDを設定する。
   *
   * @param userId ユーザーID
   */
  public void setUserId(long userId) {
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
   * パスワードハッシュを返す。
   *
   * @return パスワードハッシュ
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * パスワードハッシュを設定する。
   *
   * @param passwordHash パスワードハッシュ
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
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
   * パスワード変更要求フラグを返す。
   *
   * @return パスワード変更要求フラグ
   */
  public boolean isPasswordChangeRequired() {
    return passwordChangeRequired;
  }

  /**
   * パスワード変更要求フラグを設定する。
   *
   * @param passwordChangeRequired パスワード変更要求フラグ
   */
  public void setPasswordChangeRequired(boolean passwordChangeRequired) {
    this.passwordChangeRequired = passwordChangeRequired;
  }

  /**
   * ロールを返す。
   *
   * @return ロール
   */
  public List<Role> getRoles() {
    return roles;
  }

  /**
   * ロールを設定する。
   *
   * @param roles ロール
   */
  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }
}
