package jp.co.hikari.hanbai.common.web;

import java.beans.PropertyEditorSupport;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 全画面の入力値の共通処理（共通処理設計 6章）。
 *
 * <p>文字列項目は前後の空白を除去してからチェックする（空白のみの入力は未入力として扱う）。
 * ただしパスワードは入力されたとおりに扱う（空白の除去により、利用者の意図しないパスワードが登録されることを防ぐ）。
 */
@ControllerAdvice
public class CommonBinderAdvice {

  /** 空白を除去しない項目（パスワード）。 */
  private static final String[] RAW_FIELDS = {
    "password", "currentPassword", "newPassword", "confirmPassword", "initialPassword"
  };

  /**
   * 文字列の前後の空白を除去する。
   *
   * @param binder バインダー
   */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    for (String field : RAW_FIELDS) {
      binder.registerCustomEditor(String.class, field, new RawStringEditor());
    }
  }

  /** 入力値をそのまま設定するエディタ。 */
  private static final class RawStringEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
      setValue(text);
    }
  }
}
