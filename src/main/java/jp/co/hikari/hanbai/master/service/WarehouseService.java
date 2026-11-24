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
import jp.co.hikari.hanbai.master.dto.WarehouseCommand;
import jp.co.hikari.hanbai.master.dto.WarehouseOption;
import jp.co.hikari.hanbai.master.mapper.WarehouseMapper;
import jp.co.hikari.hanbai.master.model.Warehouse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 倉庫マスタ（F-102、SCR-111・SCR-112）。登録・変更はシステム管理者のみ（権限マトリクス 2.1）。
 *
 * <p>倉庫は物理削除せず無効にする。無効の倉庫は新しい伝票・設定で選択できない（画面一覧 5.2 No.12）。
 * 他のサブシステムは倉庫を参照する場合も本 Service を経由する（ソフトウェア構成 2章）。
 */
@Service
public class WarehouseService {

  private static final String SCREEN_ID = "SCR-112";
  private static final String TARGET_TYPE = "m_warehouse";

  private final WarehouseMapper mapper;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 倉庫の SQL
   * @param clock 時計
   */
  public WarehouseService(WarehouseMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * 倉庫の選択肢を返す。有効な倉庫に加え、現在選択されている倉庫は無効でも含める（編集画面で値が消えないように）。
   *
   * @param currentId 現在選択されている倉庫ID（なしは null）
   * @return 倉庫コードの昇順
   */
  @Transactional(readOnly = true)
  public List<WarehouseOption> findOptions(Long currentId) {
    return mapper.findOptions(currentId);
  }

  /**
   * 有効な倉庫かを返す。
   *
   * @param warehouseId 倉庫ID
   * @return 存在し有効な場合 true
   */
  @Transactional(readOnly = true)
  public boolean isEnabled(long warehouseId) {
    return mapper.existsEnabled(warehouseId);
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
   * 倉庫を検索する（倉庫コードの昇順）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 倉庫
   */
  @Transactional(readOnly = true)
  public List<Warehouse> search(MasterCriteria criteria, int limit, long offset) {
    return mapper.search(criteria, limit, offset);
  }

  /**
   * 倉庫を取得する。
   *
   * @param warehouseId 倉庫ID
   * @return 倉庫
   */
  @Transactional(readOnly = true)
  public Optional<Warehouse> find(long warehouseId) {
    return mapper.findById(warehouseId);
  }

  /**
   * 倉庫を登録する。
   *
   * @param command 登録内容
   * @param operator 操作者
   * @return 操作ログの対象（倉庫コード）
   * @throws BusinessException 倉庫コードが登録済み（E-005）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @OperationLogged(type = OperationType.CREATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget create(WarehouseCommand command, LoginUser operator) {
    if (mapper.existsByCode(command.warehouseCode())) {
      throw duplicateCode();
    }
    Warehouse warehouse = toModel(command);
    try {
      mapper.insert(warehouse, LocalDateTime.now(clock), operator.getUserId());
    } catch (DuplicateKeyException e) {
      // 同時に同じコードが登録された場合（存在確認とすれ違い）
      throw duplicateCode();
    }
    return OperationLogTarget.of(command.warehouseCode(), command.warehouseName());
  }

  /**
   * 倉庫を更新する（楽観的排他）。倉庫コードは変更しない。
   *
   * @param command 更新内容（version は画面表示時の値）
   * @param operator 操作者
   * @return 操作ログの対象（倉庫コード、変更内容）
   * @throws BusinessException 他の利用者が先に更新した（E-020）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @OperationLogged(type = OperationType.UPDATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget update(WarehouseCommand command, LoginUser operator) {
    Warehouse before =
        mapper.findById(command.warehouseId()).orElseThrow(OptimisticLockException::new);
    Warehouse after = toModel(command);
    after.setWarehouseCode(before.getWarehouseCode());
    if (mapper.update(after, LocalDateTime.now(clock), operator.getUserId()) == 0) {
      throw new OptimisticLockException();
    }
    String detail =
        new ChangeDetail()
            .change("倉庫名", before.getWarehouseName(), after.getWarehouseName())
            .change("郵便番号", before.getPostalCode(), after.getPostalCode())
            .change("住所", before.getAddress(), after.getAddress())
            .change("電話番号", before.getTel(), after.getTel())
            .change(
                "状態",
                MasterLabels.enabled(before.isEnabled()),
                MasterLabels.enabled(after.isEnabled()))
            .build();
    return OperationLogTarget.of(before.getWarehouseCode(), detail);
  }

  private static Warehouse toModel(WarehouseCommand command) {
    Warehouse warehouse = new Warehouse();
    warehouse.setWarehouseId(command.warehouseId());
    warehouse.setVersion(command.version());
    warehouse.setWarehouseCode(command.warehouseCode());
    warehouse.setWarehouseName(command.warehouseName());
    warehouse.setPostalCode(command.postalCode());
    warehouse.setAddress(command.address());
    warehouse.setTel(command.tel());
    warehouse.setEnabled(command.enabled());
    return warehouse;
  }

  private static BusinessException duplicateCode() {
    return new BusinessException(MessageId.E_DUPLICATE, MessageHelper.label("warehouseCode"));
  }
}
