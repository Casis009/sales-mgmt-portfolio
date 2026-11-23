package jp.co.hikari.hanbai.master.mapper;

import java.util.List;
import jp.co.hikari.hanbai.master.dto.WarehouseOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 倉庫（m_warehouse）の SQL。
 *
 * <p>製造 2段階（共通）では、ユーザー管理の所属倉庫に必要な参照のみを定義する。倉庫マスタ（F-102）の実装時に拡張する。
 */
@Mapper
public interface WarehouseMapper {

  /**
   * 有効な倉庫を倉庫コードの昇順で返す。
   *
   * @return 倉庫の選択肢
   */
  List<WarehouseOption> findEnabled();

  /**
   * 有効な倉庫かを返す。
   *
   * @param warehouseId 倉庫ID
   * @return 存在し有効な場合 true
   */
  boolean existsEnabled(@Param("warehouseId") long warehouseId);
}
