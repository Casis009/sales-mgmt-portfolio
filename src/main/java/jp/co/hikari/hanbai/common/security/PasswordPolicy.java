package jp.co.hikari.hanbai.common.security;

import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * パスワードの条件（NF-24。セキュリティ設計 1章 No.3）。パスワード変更（SCR-003）とユーザー登録（SCR-012）で共通に使う。
 *
 * <ul>
 *   <li>8文字以上64文字以下（8文字未満は E-015、65文字以上は E-002）
 *   <li>半角英数字記号のみで、英字と数字をそれぞれ1文字以上含む（E-015）
 *   <li>ログインIDと同じ文字列は不可（E-016）
 * </ul>
 */
@Component
public class PasswordPolicy {

  /** 最小文字数。 */
  public static final int MIN_LENGTH = 8;

  /** 最大文字数。 */
  public static final int MAX_LENGTH = 64;

  private static final char PRINTABLE_FIRST = '!';
  private static final char PRINTABLE_LAST = '~';

  /**
   * パスワードを検査し、違反があれば入力チェックのエラーとして項目に設定する。未入力は検査しない（必須チェックで別に検出する）。
   *
   * @param password パスワード
   * @param loginId ログインID
   * @param errors 入力チェックの結果
   * @param field エラーを設定する項目名
   */
  public void validate(String password, String loginId, Errors errors, String field) {
    if (password == null || password.isEmpty()) {
      return;
    }
    if (password.length() > MAX_LENGTH) {
      errors.rejectValue(
          field,
          MessageId.E_MAX_LENGTH,
          new Object[] {MessageHelper.label(field), MAX_LENGTH},
          null);
    } else if (!satisfiesComposition(password)) {
      errors.rejectValue(field, MessageId.E_PASSWORD_POLICY);
    } else if (password.equals(loginId)) {
      errors.rejectValue(field, MessageId.E_PASSWORD_SAME_AS_LOGIN_ID);
    }
  }

  private static boolean satisfiesComposition(String password) {
    if (password.length() < MIN_LENGTH) {
      return false;
    }
    boolean letter = false;
    boolean digit = false;
    for (char c : password.toCharArray()) {
      if (c < PRINTABLE_FIRST || c > PRINTABLE_LAST) {
        return false;
      }
      letter |= (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
      digit |= c >= '0' && c <= '9';
    }
    return letter && digit;
  }
}
