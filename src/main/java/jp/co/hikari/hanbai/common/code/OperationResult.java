package jp.co.hikari.hanbai.common.code;

/** 操作結果（コード定義書 14章）。 */
public enum OperationResult implements CodeEnum {
  /** 成功。 */
  SUCCESS("1", "成功"),
  /** 失敗。 */
  FAILURE("9", "失敗");

  private final String code;
  private final String label;

  OperationResult(String code, String label) {
    this.code = code;
    this.label = label;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
