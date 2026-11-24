package jp.co.hikari.hanbai.common.form;

/**
 * 入力形式の正規表現（画面一覧 5.4。違反時は E-004）。Form の {@code @Pattern} で使う。
 *
 * <p>前後の空白は入力値の共通処理（CommonBinderAdvice）で除去済みとする。
 */
public final class InputPatterns {

  /** マスタのコード：半角英数字とハイフン。 */
  public static final String CODE = "[A-Za-z0-9-]+";

  /** 郵便番号：999-9999。 */
  public static final String POSTAL_CODE = "[0-9]{3}-[0-9]{4}";

  /** 電話番号・FAX番号：数字とハイフン。 */
  public static final String TEL = "[0-9-]+";

  /** カナ：全角カタカナ・長音・空白（全角 U+3000・半角）。 */
  public static final String KANA = "[ァ-ヶー\\x{3000} ]+";

  private InputPatterns() {}
}
