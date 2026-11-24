package jp.co.hikari.hanbai.master.dto;

/**
 * 商品倉庫設定（TBL-104）の画面の1行。有効な倉庫と、設定済みの倉庫（無効でも）を表示する。
 *
 * @param warehouseId 倉庫ID
 * @param warehouseCode 倉庫コード
 * @param warehouseName 倉庫名
 * @param warehouseEnabled 倉庫の有効フラグ
 * @param reorderPoint 発注点（設定なしは null）
 * @param standardOrderQuantity 標準発注数（設定なしは null）
 */
public record ReorderSettingRow(
    long warehouseId,
    String warehouseCode,
    String warehouseName,
    boolean warehouseEnabled,
    Integer reorderPoint,
    Integer standardOrderQuantity) {

  /**
   * 設定があるか（m_product_warehouse の行が存在するか）を返す。
   *
   * @return 設定がある場合 true
   */
  public boolean configured() {
    return reorderPoint != null;
  }

  /**
   * 倉庫の表示名を返す。
   *
   * @return 例：HQ 本社倉庫（無効の場合は末尾に「（無効）」）
   */
  public String warehouseLabel() {
    return warehouseCode + " " + warehouseName + (warehouseEnabled ? "" : "（無効）");
  }
}
