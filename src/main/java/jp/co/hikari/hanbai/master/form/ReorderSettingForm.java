package jp.co.hikari.hanbai.master.form;

/**
 * SCR-102 の倉庫ごとの発注点・標準発注数の入力（F-104）。
 *
 * <p>数値は文字列で受け取り、{@link ProductFormValidator} で検査する（明細行の項目名「発注点（倉庫）」でエラーを表示するため）。
 */
public class ReorderSettingForm {

  private Long warehouseId;

  private String warehouseLabel;

  private String reorderPoint;

  private String standardOrderQuantity;

  /**
   * 倉庫IDを返す。
   *
   * @return 倉庫ID
   */
  public Long getWarehouseId() {
    return warehouseId;
  }

  /**
   * 倉庫IDを設定する。
   *
   * @param warehouseId 倉庫ID
   */
  public void setWarehouseId(Long warehouseId) {
    this.warehouseId = warehouseId;
  }

  /**
   * 倉庫の表示名を返す（表示専用。登録には使わない）。
   *
   * @return 倉庫の表示名
   */
  public String getWarehouseLabel() {
    return warehouseLabel;
  }

  /**
   * 倉庫の表示名を設定する。
   *
   * @param warehouseLabel 倉庫の表示名
   */
  public void setWarehouseLabel(String warehouseLabel) {
    this.warehouseLabel = warehouseLabel;
  }

  /**
   * 発注点を返す。
   *
   * @return 発注点（入力値）
   */
  public String getReorderPoint() {
    return reorderPoint;
  }

  /**
   * 発注点を設定する。
   *
   * @param reorderPoint 発注点
   */
  public void setReorderPoint(String reorderPoint) {
    this.reorderPoint = reorderPoint;
  }

  /**
   * 標準発注数を返す。
   *
   * @return 標準発注数（入力値）
   */
  public String getStandardOrderQuantity() {
    return standardOrderQuantity;
  }

  /**
   * 標準発注数を設定する。
   *
   * @param standardOrderQuantity 標準発注数
   */
  public void setStandardOrderQuantity(String standardOrderQuantity) {
    this.standardOrderQuantity = standardOrderQuantity;
  }
}
