package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.master.dto.UserCommand;

/**
 * SCR-012 ユーザー登録・編集の入力。
 *
 * <p>ログインIDは登録時のみ入力でき、編集時は変更しない。初期パスワードは登録時のみ入力する（必須・条件の確認は Controller で行う）。
 */
public class UserForm {

  private Long userId;

  private int version;

  /** 半角英数字 4～20 文字（SCR-001 項目定義と同じ）。 */
  @NotBlank
  @Pattern(regexp = "[A-Za-z0-9]{4,20}")
  private String loginId;

  @NotBlank
  @Size(max = 50)
  private String userName;

  @Size(max = 50)
  private String department;

  private Long defaultWarehouseId;

  @NotEmpty private List<Role> roles = new ArrayList<>();

  private boolean enabled = true;

  private boolean unlock;

  private String initialPassword;

  /**
   * Service に渡す登録・更新の内容に変換する。
   *
   * @return 登録・更新の内容
   */
  public UserCommand toCommand() {
    return new UserCommand(
        userId,
        version,
        loginId,
        userName,
        department,
        defaultWarehouseId,
        List.copyOf(roles),
        enabled,
        unlock,
        initialPassword);
  }

  /**
   * 新規登録かを返す。
   *
   * @return ユーザーIDが未設定の場合 true
   */
  public boolean isNew() {
    return userId == null;
  }

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
    this.roles = roles == null ? new ArrayList<>() : new ArrayList<>(roles);
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
   * ロック解除の指定を返す。
   *
   * @return ロックを解除する場合 true
   */
  public boolean isUnlock() {
    return unlock;
  }

  /**
   * ロック解除の指定を設定する。
   *
   * @param unlock ロックを解除する場合 true
   */
  public void setUnlock(boolean unlock) {
    this.unlock = unlock;
  }

  /**
   * 初期パスワードを返す。
   *
   * @return 初期パスワード
   */
  public String getInitialPassword() {
    return initialPassword;
  }

  /**
   * 初期パスワードを設定する。
   *
   * @param initialPassword 初期パスワード
   */
  public void setInitialPassword(String initialPassword) {
    this.initialPassword = initialPassword;
  }
}
