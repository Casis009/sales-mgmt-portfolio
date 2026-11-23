package jp.co.hikari.hanbai.master.service;

import java.util.List;
import jp.co.hikari.hanbai.master.dto.WarehouseOption;
import jp.co.hikari.hanbai.master.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 倉庫マスタ（F-102）。
 *
 * <p>製造 2段階（共通）では、ユーザー管理の所属倉庫に必要な参照のみを実装する。登録・編集は製造 2段階（マスタ）で追加する。
 * 他のサブシステムは倉庫を参照する場合も本 Service を経由する（ソフトウェア構成 2章）。
 */
@Service
public class WarehouseService {

  private final WarehouseMapper mapper;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 倉庫の SQL
   */
  public WarehouseService(WarehouseMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * 有効な倉庫の選択肢を返す。
   *
   * @return 倉庫コードの昇順
   */
  @Transactional(readOnly = true)
  public List<WarehouseOption> findEnabledOptions() {
    return mapper.findEnabled();
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
}
