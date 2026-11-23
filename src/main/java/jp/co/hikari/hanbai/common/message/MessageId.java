package jp.co.hikari.hanbai.common.message;

/**
 * メッセージID の定数（メッセージ一覧）。文言は messages.properties に定義する。
 *
 * <p>使用するメッセージIDのみを定義し、機能の実装に合わせて追加する。
 */
public final class MessageId {

  /** {0}を登録しました。 */
  public static final String I_CREATED = "I-001";

  /** {0}を更新しました。 */
  public static final String I_UPDATED = "I-002";

  /** パスワードを変更しました。 */
  public static final String I_PASSWORD_CHANGED = "I-009";

  /** 該当するデータはありません。 */
  public static final String I_NO_DATA = "I-010";

  /** 検索結果が{0}件を超えています。 */
  public static final String W_TOO_MANY_RESULTS = "W-002";

  /** {0}を入力してください。 */
  public static final String E_REQUIRED = "E-001";

  /** {0}は{1}文字以内で入力してください。 */
  public static final String E_MAX_LENGTH = "E-002";

  /** {0}は既に登録されています。 */
  public static final String E_DUPLICATE = "E-005";

  /** {0}が存在しないか、無効になっています。 */
  public static final String E_NOT_FOUND = "E-006";

  /** {0}は{1}以降の日付を入力してください。 */
  public static final String E_DATE_ORDER = "E-007";

  /** ログインIDまたはパスワードが正しくありません。 */
  public static final String E_BAD_CREDENTIALS = "E-010";

  /** アカウントがロックされています。 */
  public static final String E_LOCKED = "E-011";

  /** この操作を行う権限がありません。 */
  public static final String E_ACCESS_DENIED = "E-012";

  /** 一定時間操作がなかったため、ログアウトしました。 */
  public static final String E_SESSION_TIMEOUT = "E-013";

  /** 現在のパスワードが正しくありません。 */
  public static final String E_CURRENT_PASSWORD = "E-014";

  /** パスワードは8文字以上で、英字と数字を両方含めてください。 */
  public static final String E_PASSWORD_POLICY = "E-015";

  /** ログインIDと同じパスワードは使用できません。 */
  public static final String E_PASSWORD_SAME_AS_LOGIN_ID = "E-016";

  /** 新しいパスワードと確認用のパスワードが一致しません。 */
  public static final String E_PASSWORD_CONFIRM = "E-017";

  /** 自分自身を無効にしたり、システム管理者の権限を外したりすることはできません。 */
  public static final String E_SELF_ADMIN_CHANGE = "E-018";

  /** 有効なシステム管理者が1人もいなくなるため、変更できません。 */
  public static final String E_NO_ADMIN_LEFT = "E-019";

  /** 他のユーザーが先に更新しました。 */
  public static final String E_OPTIMISTIC_LOCK = "E-020";

  /** システムエラーが発生しました。（受付番号：{0}） */
  public static final String E_SYSTEM = "E-090";

  /** 指定されたページは存在しません。 */
  public static final String E_NOT_FOUND_PAGE = "E-091";

  private MessageId() {}
}
