package jp.co.hikari.hanbai.master.dto;

import java.util.List;
import jp.co.hikari.hanbai.master.model.Product;

/**
 * 商品編集画面（SCR-102）に表示する商品と倉庫ごとの設定。
 *
 * @param product 商品
 * @param settings 倉庫ごとの発注点・標準発注数
 */
public record ProductDetail(Product product, List<ReorderSettingRow> settings) {}
