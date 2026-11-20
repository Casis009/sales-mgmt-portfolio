package poc;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * P-01：MyBatis の動作確認用 Mapper。
 *
 * <p>本番設計で使う3つの書き方（行ロック付き取得、Optional での単一行取得、更新件数の取得）を確認する。
 */
@Mapper
public interface StockMapper {

  /** 行ロックを取得して現在庫数を返す（SELECT … FOR UPDATE）。 */
  Optional<Integer> selectQuantityForUpdate(
      @Param("productId") long productId, @Param("warehouseId") long warehouseId);

  /** 現在庫数を増減し、更新件数を返す。 */
  int addQuantity(
      @Param("productId") long productId,
      @Param("warehouseId") long warehouseId,
      @Param("delta") int delta);
}
