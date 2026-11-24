package jp.co.hikari.hanbai.master.model;

import java.math.BigDecimal;
import jp.co.hikari.hanbai.common.code.TaxCategory;

/** 商品（TBL-101 m_product）の1行。単価は税抜・円単位（BR-13）。 */
public class Product {

  private Long productId;
  private String productCode;
  private String productName;
  private String modelNumber;
  private String unit;
  private BigDecimal standardSalesPrice;
  private BigDecimal standardPurchasePrice;
  private TaxCategory taxCategory;
  private Long mainSupplierId;
  private boolean enabled;
  private int version;

  /**
   * 商品IDを返す。
   *
   * @return 商品ID
   */
  public Long getProductId() {
    return productId;
  }

  /**
   * 商品IDを設定する。
   *
   * @param productId 商品ID
   */
  public void setProductId(Long productId) {
    this.productId = productId;
  }

  /**
   * 商品コードを返す。
   *
   * @return 商品コード
   */
  public String getProductCode() {
    return productCode;
  }

  /**
   * 商品コードを設定する。
   *
   * @param productCode 商品コード
   */
  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  /**
   * 品名を返す。
   *
   * @return 品名
   */
  public String getProductName() {
    return productName;
  }

  /**
   * 品名を設定する。
   *
   * @param productName 品名
   */
  public void setProductName(String productName) {
    this.productName = productName;
  }

  /**
   * 型番を返す。
   *
   * @return 型番
   */
  public String getModelNumber() {
    return modelNumber;
  }

  /**
   * 型番を設定する。
   *
   * @param modelNumber 型番
   */
  public void setModelNumber(String modelNumber) {
    this.modelNumber = modelNumber;
  }

  /**
   * 単位を返す。
   *
   * @return 単位
   */
  public String getUnit() {
    return unit;
  }

  /**
   * 単位を設定する。
   *
   * @param unit 単位
   */
  public void setUnit(String unit) {
    this.unit = unit;
  }

  /**
   * 標準販売単価を返す。
   *
   * @return 標準販売単価（税抜）
   */
  public BigDecimal getStandardSalesPrice() {
    return standardSalesPrice;
  }

  /**
   * 標準販売単価を設定する。
   *
   * @param standardSalesPrice 標準販売単価
   */
  public void setStandardSalesPrice(BigDecimal standardSalesPrice) {
    this.standardSalesPrice = standardSalesPrice;
  }

  /**
   * 標準仕入単価を返す。
   *
   * @return 標準仕入単価（税抜）
   */
  public BigDecimal getStandardPurchasePrice() {
    return standardPurchasePrice;
  }

  /**
   * 標準仕入単価を設定する。
   *
   * @param standardPurchasePrice 標準仕入単価
   */
  public void setStandardPurchasePrice(BigDecimal standardPurchasePrice) {
    this.standardPurchasePrice = standardPurchasePrice;
  }

  /**
   * 税区分を返す。
   *
   * @return 税区分
   */
  public TaxCategory getTaxCategory() {
    return taxCategory;
  }

  /**
   * 税区分を設定する。
   *
   * @param taxCategory 税区分
   */
  public void setTaxCategory(TaxCategory taxCategory) {
    this.taxCategory = taxCategory;
  }

  /**
   * 主仕入先IDを返す。
   *
   * @return 主仕入先ID（なしは null）
   */
  public Long getMainSupplierId() {
    return mainSupplierId;
  }

  /**
   * 主仕入先IDを設定する。
   *
   * @param mainSupplierId 主仕入先ID
   */
  public void setMainSupplierId(Long mainSupplierId) {
    this.mainSupplierId = mainSupplierId;
  }

  /**
   * 有効フラグを返す。
   *
   * @return 有効フラグ（false＝取扱中止）
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * 有効フラグを設定する。
   *
   * @param enabled 有効フラグ
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * バージョンを返す。
   *
   * @return バージョン
   */
  public int getVersion() {
    return version;
  }

  /**
   * バージョンを設定する。
   *
   * @param version バージョン
   */
  public void setVersion(int version) {
    this.version = version;
  }
}
