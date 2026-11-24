package jp.co.hikari.hanbai.master.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.OptimisticLockException;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.operationlog.ChangeDetail;
import jp.co.hikari.hanbai.common.operationlog.OperationLogTarget;
import jp.co.hikari.hanbai.common.operationlog.OperationLogged;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.dto.PartnerCommand;
import jp.co.hikari.hanbai.master.dto.PartnerOption;
import jp.co.hikari.hanbai.master.mapper.PartnerMapper;
import jp.co.hikari.hanbai.master.model.Partner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 取引先マスタ（F-103、SCR-121・SCR-122）。登録・変更は営業担当・経理担当・システム管理者（権限マトリクス 2.1）。
 *
 * <p>得意先と仕入先を1つのテーブルでフラグにより区別する。得意先の場合は締日・支払条件を必須とし（BR-32）、
 * 得意先でない場合は締日・支払条件を保持しない。
 */
@Service
public class PartnerService {

  private static final String SCREEN_ID = "SCR-122";
  private static final String TARGET_TYPE = "m_partner";

  private final PartnerMapper mapper;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 取引先の SQL
   * @param clock 時計
   */
  public PartnerService(PartnerMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * 仕入先の選択肢を返す。有効な仕入先に加え、現在選択されている取引先は無効でも含める。
   *
   * @param currentId 現在選択されている取引先ID（なしは null）
   * @return 取引先コードの昇順
   */
  @Transactional(readOnly = true)
  public List<PartnerOption> findSupplierOptions(Long currentId) {
    return mapper.findSupplierOptions(currentId);
  }

  /**
   * 有効な仕入先かを返す。
   *
   * @param partnerId 取引先ID
   * @return 存在し、有効で、仕入先の場合 true
   */
  @Transactional(readOnly = true)
  public boolean isEnabledSupplier(long partnerId) {
    return mapper.existsEnabledSupplier(partnerId);
  }

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  @Transactional(readOnly = true)
  public long count(MasterCriteria criteria) {
    return mapper.count(criteria);
  }

  /**
   * 取引先を検索する（取引先コードの昇順）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 取引先
   */
  @Transactional(readOnly = true)
  public List<Partner> search(MasterCriteria criteria, int limit, long offset) {
    return mapper.search(criteria, limit, offset);
  }

  /**
   * 取引先を取得する。
   *
   * @param partnerId 取引先ID
   * @return 取引先
   */
  @Transactional(readOnly = true)
  public Optional<Partner> find(long partnerId) {
    return mapper.findById(partnerId);
  }

  /**
   * 取引先を登録する。
   *
   * @param command 登録内容（区分・締日・支払条件は入力チェックで確認済み）
   * @param operator 操作者
   * @return 操作ログの対象（取引先コード）
   * @throws BusinessException 取引先コードが登録済み（E-005）
   */
  @PreAuthorize("hasAnyRole('SALES', 'ACCOUNTING', 'ADMIN')")
  @OperationLogged(type = OperationType.CREATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget create(PartnerCommand command, LoginUser operator) {
    if (mapper.existsByCode(command.partnerCode())) {
      throw duplicateCode();
    }
    Partner partner = toModel(command);
    try {
      mapper.insert(partner, LocalDateTime.now(clock), operator.getUserId());
    } catch (DuplicateKeyException e) {
      throw duplicateCode();
    }
    String detail =
        "区分：" + MasterLabels.partyType(partner) + appendTerms(MasterLabels.terms(partner));
    return OperationLogTarget.of(command.partnerCode(), detail);
  }

  /**
   * 取引先を更新する（楽観的排他）。取引先コードは変更しない。
   *
   * @param command 更新内容（version は画面表示時の値）
   * @param operator 操作者
   * @return 操作ログの対象（取引先コード、変更内容）
   * @throws BusinessException 他の利用者が先に更新した（E-020）
   */
  @PreAuthorize("hasAnyRole('SALES', 'ACCOUNTING', 'ADMIN')")
  @OperationLogged(type = OperationType.UPDATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget update(PartnerCommand command, LoginUser operator) {
    Partner before = mapper.findById(command.partnerId()).orElseThrow(OptimisticLockException::new);
    Partner after = toModel(command);
    after.setPartnerCode(before.getPartnerCode());
    if (mapper.update(after, LocalDateTime.now(clock), operator.getUserId()) == 0) {
      throw new OptimisticLockException();
    }
    String detail =
        new ChangeDetail()
            .change("取引先名", before.getPartnerName(), after.getPartnerName())
            .change("カナ", before.getPartnerNameKana(), after.getPartnerNameKana())
            .change("区分", MasterLabels.partyType(before), MasterLabels.partyType(after))
            .change("締日・支払条件", MasterLabels.terms(before), MasterLabels.terms(after))
            .change("住所", before.getAddress(), after.getAddress())
            .change("電話番号", before.getTel(), after.getTel())
            .change("担当者名", before.getContactName(), after.getContactName())
            .change(
                "状態",
                MasterLabels.enabled(before.isEnabled()),
                MasterLabels.enabled(after.isEnabled()))
            .build();
    return OperationLogTarget.of(before.getPartnerCode(), detail);
  }

  private static String appendTerms(String terms) {
    return terms.isEmpty() ? "" : "、" + terms;
  }

  private static Partner toModel(PartnerCommand command) {
    Partner partner = new Partner();
    partner.setPartnerId(command.partnerId());
    partner.setVersion(command.version());
    partner.setPartnerCode(command.partnerCode());
    partner.setPartnerName(command.partnerName());
    partner.setPartnerNameKana(command.partnerNameKana());
    partner.setCustomer(command.customer());
    partner.setSupplier(command.supplier());
    partner.setPostalCode(command.postalCode());
    partner.setAddress(command.address());
    partner.setTel(command.tel());
    partner.setFax(command.fax());
    partner.setContactName(command.contactName());
    // 得意先でない場合、締日・支払条件は保持しない
    if (command.customer()) {
      partner.setClosingDay(command.closingDay());
      partner.setPaymentMonthOffset(command.paymentMonthOffset());
      partner.setPaymentDay(command.paymentDay());
    }
    partner.setEnabled(command.enabled());
    return partner;
  }

  private static BusinessException duplicateCode() {
    return new BusinessException(MessageId.E_DUPLICATE, MessageHelper.label("partnerCode"));
  }
}
