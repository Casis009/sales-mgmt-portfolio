package jp.co.hikari.hanbai.common.form;

import jakarta.validation.constraints.NotBlank;

/** SCR-003 パスワード変更の入力。パスワードは画面に再表示しない。 */
public class PasswordChangeForm {

  @NotBlank private String currentPassword;

  @NotBlank private String newPassword;

  @NotBlank private String confirmPassword;

  /**
   * 現在のパスワードを返す。
   *
   * @return 現在のパスワード
   */
  public String getCurrentPassword() {
    return currentPassword;
  }

  /**
   * 現在のパスワードを設定する。
   *
   * @param currentPassword 現在のパスワード
   */
  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  /**
   * 新しいパスワードを返す。
   *
   * @return 新しいパスワード
   */
  public String getNewPassword() {
    return newPassword;
  }

  /**
   * 新しいパスワードを設定する。
   *
   * @param newPassword 新しいパスワード
   */
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  /**
   * 確認用のパスワードを返す。
   *
   * @return 確認用のパスワード
   */
  public String getConfirmPassword() {
    return confirmPassword;
  }

  /**
   * 確認用のパスワードを設定する。
   *
   * @param confirmPassword 確認用のパスワード
   */
  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }
}
