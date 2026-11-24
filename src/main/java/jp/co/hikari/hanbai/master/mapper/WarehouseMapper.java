package jp.co.hikari.hanbai.master.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.dto.WarehouseOption;
import jp.co.hikari.hanbai.master.model.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 倉庫（m_warehouse）の SQL（F-102）。 */
@Mapper
public interface WarehouseMapper {

  /**
   * 有効な倉庫と、指定の倉庫（無効でも）を倉庫コードの昇順で返す。
   *
   * @param includeId 無効でも含める倉庫ID（なしは null）
   * @return 倉庫の選択肢
   */
  List<WarehouseOption> findOptions(@Param("includeId") Long includeId);

  /**
   * 有効な倉庫かを返す。
   *
   * @param warehouseId 倉庫ID
   * @return 存在し有効な場合 true
   */
  boolean existsEnabled(@Param("warehouseId") long warehouseId);

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  long count(@Param("c") MasterCriteria criteria);

  /**
   * 検索条件に該当する倉庫を倉庫コードの昇順で返す。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 倉庫
   */
  List<Warehouse> search(
      @Param("c") MasterCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") long offset);

  /**
   * 倉庫を取得する。
   *
   * @param warehouseId 倉庫ID
   * @return 倉庫
   */
  Optional<Warehouse> findById(@Param("warehouseId") long warehouseId);

  /**
   * 倉庫コードが登録済みかを返す。
   *
   * @param warehouseCode 倉庫コード
   * @return 登録済みの場合 true
   */
  boolean existsByCode(@Param("warehouseCode") String warehouseCode);

  /**
   * 倉庫を登録する。採番した倉庫IDを {@code warehouse.warehouseId} に設定する。
   *
   * @param warehouse 倉庫
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insert(
      @Param("w") Warehouse warehouse,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 倉庫を更新する（楽観的排他。倉庫コードは変更しない）。
   *
   * @param warehouse 倉庫（version は画面表示時の値）
   * @param now 現在日時
   * @param operatorId 更新者
   * @return 更新件数（version が一致しない場合は 0）
   */
  int update(
      @Param("w") Warehouse warehouse,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);
}
