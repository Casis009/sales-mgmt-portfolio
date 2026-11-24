package jp.co.hikari.hanbai.common.code;

/** 税区分（コード定義書 2章）。税率は消費税率マスタ（m_tax_rate）から取得する。 */
public enum TaxCategory implements CodeEnum {
  /** 標準税率。 */
  STANDARD("1", "標準税率"),
  /** 軽減税率。 */
  REDUCED("2", "軽減税率");

  private final String code;
  private final String label;

  TaxCategory(String code, String label) {
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
