package jp.co.hikari.hanbai.common.message;

/**
 * 画面のメッセージ表示欄に出すメッセージ（画面一覧 5.1：情報＝緑、警告＝黄、エラー＝赤）。
 *
 * @param level 種別
 * @param text 表示する文言（メッセージIDから解決済み）
 */
public record UiMessage(Level level, String text) {

  /** 画面のモデル・フラッシュ属性の名前。 */
  public static final String ATTRIBUTE = "uiMessages";

  /** メッセージの種別。 */
  public enum Level {
    /** 情報。 */
    INFO,
    /** 警告。 */
    WARN,
    /** エラー。 */
    ERROR;

    /**
     * 画面の CSS クラス名を返す。
     *
     * @return 例：msg-info
     */
    public String cssClass() {
      return "msg-" + name().toLowerCase(java.util.Locale.ROOT);
    }
  }
}
