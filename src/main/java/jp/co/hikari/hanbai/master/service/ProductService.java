package jp.co.hikari.hanbai.master.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import jp.co.hikari.hanbai.master.dto.ProductCommand;
import jp.co.hikari.hanbai.master.dto.ProductDetail;
import jp.co.hikari.hanbai.master.dto.ProductListRow;
import jp.co.hikari.hanbai.master.dto.ReorderSettingRow;
import jp.co.hikari.hanbai.master.mapper.ProductMapper;
import jp.co.hikari.hanbai.master.mapper.ProductWarehouseMapper;
import jp.co.hikari.hanbai.master.model.Product;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品マスタ（F-101）と発注点設定（F-104）。SCR-101・SCR-102。
 *
 * <p>登録・変更は物流担当・システム管理者（権限マトリクス 2.1）。商品は物理削除せず無効（取扱中止）にする。
 * 発注点設定は倉庫ごとに持ち、発注点が未入力の倉庫は行を作らない（発注点チェックの対象外。TBL-104）。
 */
@Service
public class ProductService {

  private static final String SCREEN_ID = "SCR-102";
  private static final String TARGET_TYPE = "m_product";

  private final ProductMapper mapper;
  private final ProductWarehouseMapper settingMapper;
  private final PartnerService partnerService;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 商品の SQL
   * @param settingMapper 商品倉庫設定の SQL
   * @param partnerService 取引先マスタ（主仕入先の確認）
   * @param clock 時計
   */
  public ProductService(
      ProductMapper mapper,
      ProductWarehouseMapper settingMapper,
      PartnerService partnerService,
      Clock clock) {
    this.mapper = mapper;
    this.settingMapper = settingMapper;
    this.partnerService = partnerService;
    this.clock = clock;
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
   * 商品を検索する（商品コードの昇順）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 一覧の行
   */
  @Transactional(readOnly = true)
  public List<ProductListRow> search(MasterCriteria criteria, int limit, long offset) {
    return mapper.search(criteria, limit, offset);
  }

  /**
   * 編集画面に表示する商品と倉庫ごとの設定を取得する。
   *
   * @param productId 商品ID
   * @return 商品と設定
   */
  @Transactional(readOnly = true)
  public Optional<ProductDetail> find(long productId) {
    return mapper
        .findById(productId)
        .map(p -> new ProductDetail(p, settingMapper.findRows(productId)));
  }

  /**
   * 登録画面に表示する倉庫ごとの設定欄（有効な倉庫）を返す。
   *
   * @return 設定なしの行
   */
  @Transactional(readOnly = true)
  public List<ReorderSettingRow> newSettingRows() {
    return settingMapper.findRows(null);
  }

  /**
   * 商品と倉庫ごとの設定を登録する。
   *
   * @param command 登録内容
   * @param operator 操作者
   * @return 操作ログの対象（商品コード）
   * @throws BusinessException 商品コードが登録済み（E-005）、主仕入先・倉庫が無効（E-006）
   */
  @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
  @OperationLogged(type = OperationType.CREATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget create(ProductCommand command, LoginUser operator) {
    if (mapper.existsByCode(command.productCode())) {
      throw duplicateCode();
    }
    checkSupplier(command.mainSupplierId());
    Product product = toModel(command);
    LocalDateTime now = LocalDateTime.now(clock);
    try {
      mapper.insert(product, now, operator.getUserId());
    } catch (DuplicateKeyException e) {
      throw duplicateCode();
    }
    ChangeDetail detail = new ChangeDetail().note("品名：" + command.productName());
    saveSettings(product.getProductId(), command, List.of(), now, operator, detail);
    return OperationLogTarget.of(command.productCode(), detail.build());
  }

  /**
   * 商品と倉庫ごとの設定を更新する（楽観的排他は商品の version で行う）。商品コードは変更しない。
   *
   * @param command 更新内容（version は画面表示時の値）
   * @param operator 操作者
   * @return 操作ログの対象（商品コード、変更内容）
   * @throws BusinessException 他の利用者が先に更新した（E-020）、主仕入先・倉庫が無効（E-006）
   */
  @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
  @OperationLogged(type = OperationType.UPDATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget update(ProductCommand command, LoginUser operator) {
    Product before = mapper.findById(command.productId()).orElseThrow(OptimisticLockException::new);
    // 主仕入先を変えない場合は、その仕入先が後から無効になっていても更新できるようにする
    if (!Objects.equals(before.getMainSupplierId(), command.mainSupplierId())) {
      checkSupplier(command.mainSupplierId());
    }
    Product after = toModel(command);
    after.setProductCode(before.getProductCode());
    LocalDateTime now = LocalDateTime.now(clock);
    if (mapper.update(after, now, operator.getUserId()) == 0) {
      throw new OptimisticLockException();
    }
    ChangeDetail detail =
        new ChangeDetail()
            .change("品名", before.getProductName(), after.getProductName())
            .change("型番", before.getModelNumber(), after.getModelNumber())
            .change("単位", before.getUnit(), after.getUnit())
            .change("標準販売単価", before.getStandardSalesPrice(), after.getStandardSalesPrice())
            .change(
                "標準仕入単価", before.getStandardPurchasePrice(), after.getStandardPurchasePrice())
            .change("税区分", label(before), label(after))
            .change("主仕入先ID", before.getMainSupplierId(), after.getMainSupplierId())
            .change(
                "状態",
                MasterLabels.enabled(before.isEnabled()),
                MasterLabels.enabled(after.isEnabled()));
    saveSettings(
        before.getProductId(),
        command,
        settingMapper.findRows(before.getProductId()),
        now,
        operator,
        detail);
    return OperationLogTarget.of(before.getProductCode(), detail.build());
  }

  /**
   * 倉庫ごとの設定を、行の追加・更新・削除で反映する。新しく設定する倉庫は有効でなければならない。
   *
   * @param current 現在の設定（登録時は空）
   */
  private void saveSettings(
      long productId,
      ProductCommand command,
      List<ReorderSettingRow> current,
      LocalDateTime now,
      LoginUser operator,
      ChangeDetail detail) {
    Map<Long, ReorderSettingRow> rows =
        (current.isEmpty() ? settingMapper.findRows(null) : current)
            .stream()
            .collect(Collectors.toMap(ReorderSettingRow::warehouseId, Function.identity()));
    for (ProductCommand.Setting setting : command.settings()) {
      ReorderSettingRow row = rows.get(setting.warehouseId());
      if (row == null || (!row.configured() && setting.configured() && !row.warehouseEnabled())) {
        // 画面に表示していない倉庫、または無効の倉庫への新規設定
        throw new BusinessException(
            MessageId.E_NOT_FOUND, MessageHelper.label("settings.warehouseId"));
      }
      String name = "発注点・標準発注数（" + row.warehouseCode() + "）";
      if (setting.configured()) {
        int point = setting.reorderPoint();
        int quantity = Objects.requireNonNullElse(setting.standardOrderQuantity(), 0);
        long userId = operator.getUserId();
        if (!row.configured()) {
          settingMapper.insert(productId, row.warehouseId(), point, quantity, now, userId);
        } else {
          settingMapper.update(productId, row.warehouseId(), point, quantity, now, userId);
        }
        detail.change(name, text(row), point + "・" + quantity);
      } else if (row.configured()) {
        settingMapper.delete(productId, row.warehouseId());
        detail.change(name, text(row), null);
      }
    }
  }

  private void checkSupplier(Long supplierId) {
    if (supplierId != null && !partnerService.isEnabledSupplier(supplierId)) {
      throw new BusinessException(MessageId.E_NOT_FOUND, MessageHelper.label("mainSupplierId"));
    }
  }

  private static String text(ReorderSettingRow row) {
    return row.configured() ? row.reorderPoint() + "・" + row.standardOrderQuantity() : null;
  }

  private static String label(Product product) {
    return product.getTaxCategory() == null ? null : product.getTaxCategory().getLabel();
  }

  private static Product toModel(ProductCommand command) {
    Product product = new Product();
    product.setProductId(command.productId());
    product.setVersion(command.version());
    product.setProductCode(command.productCode());
    product.setProductName(command.productName());
    product.setModelNumber(command.modelNumber());
    product.setUnit(command.unit());
    product.setStandardSalesPrice(command.standardSalesPrice());
    product.setStandardPurchasePrice(command.standardPurchasePrice());
    product.setTaxCategory(command.taxCategory());
    product.setMainSupplierId(command.mainSupplierId());
    product.setEnabled(command.enabled());
    return product;
  }

  private static BusinessException duplicateCode() {
    return new BusinessException(MessageId.E_DUPLICATE, MessageHelper.label("productCode"));
  }
}
