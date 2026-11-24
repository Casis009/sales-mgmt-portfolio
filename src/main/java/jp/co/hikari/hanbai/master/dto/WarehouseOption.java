package jp.co.hikari.hanbai.master.dto;

/**
 * 画面の倉庫の選択肢。
 *
 * @param warehouseId 倉庫ID
 * @param warehouseCode 倉庫コード
 * @param warehouseName 倉庫名
 * @param enabled 有効フラグ（無効の倉庫は、現在の値として表示する場合のみ選択肢に含める）
 */
public record WarehouseOption(
    long warehouseId, String warehouseCode, String warehouseName, boolean enabled) {

  /**
   * 選択肢の表示名を返す。
   *
   * @return 例：HQ 本社倉庫（無効の場合は末尾に「（無効）」）
   */
  public String label() {
    return warehouseCode + " " + warehouseName + (enabled ? "" : "（無効）");
  }
}
