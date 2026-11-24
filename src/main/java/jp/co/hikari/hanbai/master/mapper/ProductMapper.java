package jp.co.hikari.hanbai.master.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.dto.ProductListRow;
import jp.co.hikari.hanbai.master.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商品（m_product）の SQL（F-101）。 */
@Mapper
public interface ProductMapper {

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  long count(@Param("c") MasterCriteria criteria);

  /**
   * 検索条件に該当する商品を商品コードの昇順で返す。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 一覧の行
   */
  List<ProductListRow> search(
      @Param("c") MasterCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") long offset);

  /**
   * 商品を取得する。
   *
   * @param productId 商品ID
   * @return 商品
   */
  Optional<Product> findById(@Param("productId") long productId);

  /**
   * 商品コードが登録済みかを返す。
   *
   * @param productCode 商品コード
   * @return 登録済みの場合 true
   */
  boolean existsByCode(@Param("productCode") String productCode);

  /**
   * 商品を登録する。採番した商品IDを {@code product.productId} に設定する。
   *
   * @param product 商品
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insert(
      @Param("p") Product product,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 商品を更新する（楽観的排他。商品コードは変更しない）。
   *
   * @param product 商品（version は画面表示時の値）
   * @param now 現在日時
   * @param operatorId 更新者
   * @return 更新件数（version が一致しない場合は 0）
   */
  int update(
      @Param("p") Product product,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);
}
