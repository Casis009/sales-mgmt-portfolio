package jp.co.hikari.hanbai.common.code;

/** ロール（コード定義書 1章）。Spring Security の権限名は {@code ROLE_} + 定数名とする。 */
public enum Role implements CodeEnum {
  /** 営業担当。 */
  SALES("営業担当"),
  /** 物流担当。 */
  WAREHOUSE("物流担当"),
  /** 経理担当。 */
  ACCOUNTING("経理担当"),
  /** 承認者。 */
  APPROVER("承認者"),
  /** システム管理者。 */
  ADMIN("システム管理者");

  private final String label;

  Role(String label) {
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

  /**
   * Spring Security の権限名を返す。
   *
   * @return 例：ROLE_ADMIN
   */
  public String authority() {
    return "ROLE_" + name();
  }
}
