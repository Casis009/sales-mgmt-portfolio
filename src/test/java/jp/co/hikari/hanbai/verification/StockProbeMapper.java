package jp.co.hikari.hanbai.verification;

import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 技術検証 P-01 用の Mapper。本番の StockMapper（製造 3段階）で使う書き方を、実際の t_stock で確認する。
 *
 * <ul>
 *   <li>行ロック付きの単一行取得を Optional で返す
 *   <li>増減値による更新と更新件数の取得
 *   <li>INSERT … ON CONFLICT DO NOTHING（共通処理設計 9章 No.1）
 * </ul>
 */
@Mapper
public interface StockProbeMapper {

  /** 行ロックを取得して現在庫数を返す。 */
  Optional<Integer> selectQuantityForUpdate(
      @Param("productId") long productId, @Param("warehouseId") long warehouseId);

  /** 現在庫数を増減し、更新件数を返す。 */
  int addQuantity(
      @Param("productId") long productId,
      @Param("warehouseId") long warehouseId,
      @Param("delta") int delta,
      @Param("now") LocalDateTime now);

  /** 在庫行がなければ作成する。作成した場合は 1、既にあれば 0 を返す。 */
  int insertIfAbsent(
      @Param("productId") long productId,
      @Param("warehouseId") long warehouseId,
      @Param("now") LocalDateTime now);
}
