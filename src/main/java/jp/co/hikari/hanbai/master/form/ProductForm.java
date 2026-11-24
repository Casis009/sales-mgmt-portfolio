package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jp.co.hikari.hanbai.common.code.TaxCategory;
import jp.co.hikari.hanbai.common.form.InputPatterns;
import jp.co.hikari.hanbai.master.dto.ProductCommand;
import org.hibernate.validator.constraints.Range;

/**
 * SCR-102 商品登録・編集の入力（F-101・F-104）。商品コードは登録時のみ入力でき、編集時は変更しない。
 *
 * <p>倉庫ごとの発注点・標準発注数は {@link ProductFormValidator} で検査し、検査後に {@link #toCommand(List)} で数値に変換する。
 */
public class ProductForm {

  /** 単価の上限（NUMERIC(9,0)）。 */
  public static final long MAX_PRICE = 999_999_999L;

  private Long productId;

  private int version;

  @NotBlank
  @Size(max = 20)
  @Pattern(regexp = InputPatterns.CODE)
  private String productCode;

  @NotBlank
  @Size(max = 100)
  private String productName;

  @Size(max = 50)
  private String modelNumber;

  @NotBlank
  @Size(max = 10)
  private String unit;

  @NotNull
  @Digits(integer = 9, fraction = 0)
  @Range(min = 0, max = MAX_PRICE)
  private BigDecimal standardSalesPrice = BigDecimal.ZERO;

  @NotNull
  @Digits(integer = 9, fraction = 0)
  @Range(min = 0, max = MAX_PRICE)
  private BigDecimal standardPurchasePrice = BigDecimal.ZERO;

  @NotNull private TaxCategory taxCategory = TaxCategory.STANDARD;

  private Long mainSupplierId;

  private boolean enabled = true;

  private List<ReorderSettingForm> settings = new ArrayList<>();

  /**
   * Service に渡す登録・更新の内容に変換する。
   *
   * @param parsedSettings 検査・変換済みの倉庫ごとの設定
   * @return 登録・更新の内容
   */
  public ProductCommand toCommand(List<ProductCommand.Setting> parsedSettings) {
    return new ProductCommand(
        productId,
        version,
        productCode,
        productName,
        modelNumber,
        unit,
        standardSalesPrice,
        standardPurchasePrice,
        taxCategory,
        mainSupplierId,
        enabled,
        List.copyOf(parsedSettings));
  }

  /**
   * 新規登録かを返す。
   *
   * @return 商品IDが未設定の場合 true
   */
  public boolean isNew() {
    return productId == null;
  }

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
   * @return 標準販売単価
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
   * @return 標準仕入単価
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
   * @return 主仕入先ID
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
   * @return 有効フラグ
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
   * 倉庫ごとの設定を返す。
   *
   * @return 倉庫ごとの設定
   */
  public List<ReorderSettingForm> getSettings() {
    return settings;
  }

  /**
   * 倉庫ごとの設定を設定する。
   *
   * @param settings 倉庫ごとの設定
   */
  public void setSettings(List<ReorderSettingForm> settings) {
    this.settings = settings == null ? new ArrayList<>() : new ArrayList<>(settings);
  }
}
