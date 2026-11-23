package jp.co.hikari.hanbai.common.security;

import java.io.Serial;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jp.co.hikari.hanbai.common.code.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * ログイン中のユーザー（ソフトウェア構成 4.1）。セッションに保持する。
 *
 * <p>業務データは保持しない（セキュリティ設計 2章 No.5）。Service にはこのオブジェクトを引数で渡す（ソフトウェア構成 2章）。
 */
public final class LoginUser implements UserDetails, CredentialsContainer {

  @Serial private static final long serialVersionUID = 1L;

  private final long userId;
  private final String loginId;
  private final String userName;
  private final Long defaultWarehouseId;
  private final Set<Role> roles;
  private final boolean enabled;
  private final boolean locked;
  private final boolean passwordChangeRequired;
  private String password;

  /**
   * ログインユーザーを生成する。
   *
   * @param userId ユーザーID
   * @param loginId ログインID
   * @param userName 氏名
   * @param defaultWarehouseId 所属倉庫ID（なしは null）
   * @param roles ロール
   * @param enabled 有効フラグ
   * @param locked ロックフラグ
   * @param passwordChangeRequired パスワード変更要求フラグ
   * @param password パスワードハッシュ（認証後に消去する）
   */
  public LoginUser(
      long userId,
      String loginId,
      String userName,
      Long defaultWarehouseId,
      Collection<Role> roles,
      boolean enabled,
      boolean locked,
      boolean passwordChangeRequired,
      String password) {
    this.userId = userId;
    this.loginId = Objects.requireNonNull(loginId);
    this.userName = userName;
    this.defaultWarehouseId = defaultWarehouseId;
    this.roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
    this.enabled = enabled;
    this.locked = locked;
    this.passwordChangeRequired = passwordChangeRequired;
    this.password = password;
  }

  /**
   * パスワード変更後のログインユーザーを返す（変更要求フラグを解除する）。
   *
   * @return 新しいログインユーザー
   */
  public LoginUser withPasswordChanged() {
    return new LoginUser(
        userId, loginId, userName, defaultWarehouseId, roles, enabled, locked, false, null);
  }

  /**
   * 指定のロールを持つかを返す。
   *
   * @param role ロール
   * @return 持つ場合 true
   */
  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  /**
   * ヘッダーに表示するロール名を返す（例：営業担当・承認者）。
   *
   * @return ロール名を「・」で連結した文字列
   */
  public String roleLabels() {
    return roles.stream().sorted().map(Role::getLabel).collect(Collectors.joining("・"));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<SimpleGrantedAuthority> list =
        roles.stream().sorted().map(r -> new SimpleGrantedAuthority(r.authority())).toList();
    return list;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return loginId;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void eraseCredentials() {
    password = null;
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
   * ログインIDを返す。
   *
   * @return ログインID
   */
  public String getLoginId() {
    return loginId;
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
   * 所属倉庫IDを返す。
   *
   * @return 所属倉庫ID（なしは null）
   */
  public Long getDefaultWarehouseId() {
    return defaultWarehouseId;
  }

  /**
   * ロールを返す。
   *
   * @return ロール（変更不可）
   */
  public Set<Role> getRoles() {
    return roles;
  }

  /**
   * パスワード変更要求フラグを返す。
   *
   * @return 初期パスワードのままの場合 true
   */
  public boolean isPasswordChangeRequired() {
    return passwordChangeRequired;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof LoginUser other && userId == other.userId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(userId);
  }

  @Override
  public String toString() {
    // パスワードハッシュをログに出さない
    return "LoginUser[userId=" + userId + ", loginId=" + loginId + ", roles=" + roles + "]";
  }
}
