package jp.co.hikari.hanbai.master.mapper;

import java.time.LocalDateTime;
import java.util.List;
import jp.co.hikari.hanbai.master.dto.ReorderSettingRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品倉庫設定（m_product_warehouse）の SQL（F-104）。行が存在しない「商品 × 倉庫」は発注点チェックの対象外とする。
 */
@Mapper
public interface ProductWarehouseMapper {

  /**
   * 画面に表示する倉庫ごとの設定を返す。有効な倉庫と、設定済みの倉庫（無効でも）を倉庫コードの昇順で返す。
   *
   * @param productId 商品ID（登録画面は null）
   * @return 倉庫ごとの設定
   */
  List<ReorderSettingRow> findRows(@Param("productId") Long productId);

  /**
   * 設定を登録する。
   *
   * @param productId 商品ID
   * @param warehouseId 倉庫ID
   * @param reorderPoint 発注点
   * @param standardOrderQuantity 標準発注数
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insert(
      @Param("productId") long productId,
      @Param("warehouseId") long warehouseId,
      @Param("reorderPoint") int reorderPoint,
      @Param("standardOrderQuantity") int standardOrderQuantity,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 設定を更新する。
   *
   * @param productId 商品ID
   * @param warehouseId 倉庫ID
   * @param reorderPoint 発注点
   * @param standardOrderQuantity 標準発注数
   * @param now 現在日時
   * @param operatorId 更新者
   * @return 更新件数
   */
  int update(
      @Param("productId") long productId,
      @Param("warehouseId") long warehouseId,
      @Param("reorderPoint") int reorderPoint,
      @Param("standardOrderQuantity") int standardOrderQuantity,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 設定を削除する（発注点チェックの対象外にする）。
   *
   * @param productId 商品ID
   * @param warehouseId 倉庫ID
   * @return 削除件数
   */
  int delete(@Param("productId") long productId, @Param("warehouseId") long warehouseId);
}
