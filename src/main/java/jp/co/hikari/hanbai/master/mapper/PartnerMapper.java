package jp.co.hikari.hanbai.master.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.dto.PartnerOption;
import jp.co.hikari.hanbai.master.model.Partner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 取引先（m_partner）の SQL（F-103）。 */
@Mapper
public interface PartnerMapper {

  /**
   * 有効な仕入先と、指定の取引先（無効・仕入先でなくても）を取引先コードの昇順で返す。
   *
   * @param includeId 必ず含める取引先ID（なしは null）
   * @return 仕入先の選択肢
   */
  List<PartnerOption> findSupplierOptions(@Param("includeId") Long includeId);

  /**
   * 有効な仕入先かを返す。
   *
   * @param partnerId 取引先ID
   * @return 存在し、有効で、仕入先の場合 true
   */
  boolean existsEnabledSupplier(@Param("partnerId") long partnerId);

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  long count(@Param("c") MasterCriteria criteria);

  /**
   * 検索条件に該当する取引先を取引先コードの昇順で返す。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 取引先
   */
  List<Partner> search(
      @Param("c") MasterCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") long offset);

  /**
   * 取引先を取得する。
   *
   * @param partnerId 取引先ID
   * @return 取引先
   */
  Optional<Partner> findById(@Param("partnerId") long partnerId);

  /**
   * 取引先コードが登録済みかを返す。
   *
   * @param partnerCode 取引先コード
   * @return 登録済みの場合 true
   */
  boolean existsByCode(@Param("partnerCode") String partnerCode);

  /**
   * 取引先を登録する。採番した取引先IDを {@code partner.partnerId} に設定する。
   *
   * @param partner 取引先
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insert(
      @Param("p") Partner partner,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 取引先を更新する（楽観的排他。取引先コードは変更しない）。
   *
   * @param partner 取引先（version は画面表示時の値）
   * @param now 現在日時
   * @param operatorId 更新者
   * @return 更新件数（version が一致しない場合は 0）
   */
  int update(
      @Param("p") Partner partner,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);
}
