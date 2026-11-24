package jp.co.hikari.hanbai.master.dto;

import java.math.BigDecimal;
import java.util.List;
import jp.co.hikari.hanbai.common.code.TaxCategory;

/**
 * 商品の登録・更新の内容（SCR-102。F-101・F-104）。
 *
 * @param productId 商品ID（登録時は null）
 * @param version バージョン（登録時は 0）
 * @param productCode 商品コード（更新時は変更しない）
 * @param productName 品名
 * @param modelNumber 型番
 * @param unit 単位
 * @param standardSalesPrice 標準販売単価
 * @param standardPurchasePrice 標準仕入単価
 * @param taxCategory 税区分
 * @param mainSupplierId 主仕入先ID（なしは null）
 * @param enabled 有効フラグ
 * @param settings 倉庫ごとの発注点・標準発注数
 */
public record ProductCommand(
    Long productId,
    int version,
    String productCode,
    String productName,
    String modelNumber,
    String unit,
    BigDecimal standardSalesPrice,
    BigDecimal standardPurchasePrice,
    TaxCategory taxCategory,
    Long mainSupplierId,
    boolean enabled,
    List<Setting> settings) {

  /**
   * 倉庫ごとの発注点・標準発注数。発注点が null の場合は「発注点チェックの対象外」（行を作らない）。
   *
   * @param warehouseId 倉庫ID
   * @param reorderPoint 発注点
   * @param standardOrderQuantity 標準発注数
   */
  public record Setting(long warehouseId, Integer reorderPoint, Integer standardOrderQuantity) {

    /**
     * 発注点チェックの対象とするかを返す。
     *
     * @return 発注点が入力されている場合 true
     */
    public boolean configured() {
      return reorderPoint != null;
    }
  }
}
