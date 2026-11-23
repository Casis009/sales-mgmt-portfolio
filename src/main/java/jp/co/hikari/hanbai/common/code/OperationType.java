package jp.co.hikari.hanbai.common.code;

/** 操作種別（コード定義書 13章）。 */
public enum OperationType implements CodeEnum {
  /** ログイン（認証成功）。 */
  LOGIN("ログイン"),
  /** ログイン失敗（認証失敗、ロック）。 */
  LOGIN_FAILURE("ログイン失敗"),
  /** ログアウト。 */
  LOGOUT("ログアウト"),
  /** 登録。 */
  CREATE("登録"),
  /** 更新。 */
  UPDATE("更新"),
  /** 取消。 */
  CANCEL("取消"),
  /** 承認申請。 */
  APPLY("承認申請"),
  /** 承認。 */
  APPROVE("承認"),
  /** 差戻し。 */
  REJECT("差戻し"),
  /** 確定。 */
  CONFIRM("確定"),
  /** 出力（帳票・CSV）。 */
  EXPORT("出力"),
  /** バッチの開始・終了。 */
  BATCH("バッチ");

  private final String label;

  OperationType(String label) {
    this.label = label;
  }

  @Override
  public String getCode() {
    return name();
  }

  @Override
  public String getLabel() {
    return label;
  }
}
