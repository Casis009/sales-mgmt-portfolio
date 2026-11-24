package jp.co.hikari.hanbai.master.dto;

import java.math.BigDecimal;
import jp.co.hikari.hanbai.common.code.TaxCategory;

/**
 * 商品一覧（SCR-101）の1行（m_product と主仕入先の m_partner の結合）。
 *
 * @param productId 商品ID
 * @param productCode 商品コード
 * @param productName 品名
 * @param modelNumber 型番
 * @param unit 単位
 * @param standardSalesPrice 標準販売単価
 * @param standardPurchasePrice 標準仕入単価
 * @param taxCategory 税区分
 * @param mainSupplierName 主仕入先名（なしは null）
 * @param enabled 有効フラグ
 */
public record ProductListRow(
    long productId,
    String productCode,
    String productName,
    String modelNumber,
    String unit,
    BigDecimal standardSalesPrice,
    BigDecimal standardPurchasePrice,
    TaxCategory taxCategory,
    String mainSupplierName,
    boolean enabled) {}
