package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import jp.co.hikari.hanbai.common.form.PasswordChangeForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/** パスワードの条件（CM-06：境界値・同値分割）。 */
class PasswordPolicyTest {

  private static final String LOGIN_ID = "tanaka01";

  private final PasswordPolicy policy = new PasswordPolicy();

  /** 検査結果のエラーコード（メッセージID）。違反なしは OK。 */
  private String check(String password) {
    BeanPropertyBindingResult errors =
        new BeanPropertyBindingResult(new PasswordChangeForm(), "form");
    policy.validate(password, LOGIN_ID, errors, "newPassword");
    FieldError error = errors.getFieldError("newPassword");
    return error == null ? "OK" : error.getCode();
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @CsvSource({
    "7文字は不可,  7,  E-015",
    "8文字は可,    8,  OK",
    "64文字は可,   64, OK",
    "65文字は不可, 65, E-002",
  })
  void 文字数の境界(String description, int length, String expected) {
    String password = "a1" + "x".repeat(length - 2);
    assertThat(check(password)).as(description).isEqualTo(expected);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @CsvSource({
    "英字のみは不可,                 abcdefgh,   E-015",
    "数字のみは不可,                 12345678,   E-015",
    "英字と数字と記号は可,           Abc-1234!,  OK",
    "全角文字を含むと不可,           Abcd1234あ, E-015",
    "空白を含むと不可,               'Abc 1234', E-015",
    "ログインIDと同じは不可,         tanaka01,   E-016",
    "ログインIDと大文字小文字違いは可, Tanaka01, OK",
  })
  void 文字の種類とログインID(String description, String password, String expected) {
    assertThat(check(password)).as(description).isEqualTo(expected);
  }

  @Test
  void 未入力は必須チェックに任せて検査しない() {
    assertThat(check("")).isEqualTo("OK");
    assertThat(check(null)).isEqualTo("OK");
  }
}
