package jp.co.hikari.hanbai.master.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import jp.co.hikari.hanbai.common.code.TaxCategory;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.NotFoundException;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.web.Pagination;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.dto.ProductCommand;
import jp.co.hikari.hanbai.master.dto.ProductDetail;
import jp.co.hikari.hanbai.master.dto.ReorderSettingRow;
import jp.co.hikari.hanbai.master.form.MasterSearchForm;
import jp.co.hikari.hanbai.master.form.ProductForm;
import jp.co.hikari.hanbai.master.form.ProductFormValidator;
import jp.co.hikari.hanbai.master.form.ReorderSettingForm;
import jp.co.hikari.hanbai.master.model.Product;
import jp.co.hikari.hanbai.master.service.PartnerService;
import jp.co.hikari.hanbai.master.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SCR-101 商品一覧・SCR-102 商品登録・編集（F-101、F-104 発注点設定）。
 *
 * <p>一覧は全員、登録・編集は物流担当・システム管理者（URL は SecurityConfig、更新は Service でも確認する）。
 */
@Controller
@RequestMapping(ProductController.BASE_URL)
public class ProductController {

  /** 商品一覧の URL。 */
  public static final String BASE_URL = "/master/products";

  private static final String LIST_VIEW = "master/product-list";
  private static final String FORM_VIEW = "master/product-form";
  private static final String FORM_KEY = "SCR-102";
  private static final String FORM = "form";
  private static final String REDIRECT_LIST = "redirect:" + BASE_URL + "?search=1";

  private final ProductService productService;
  private final PartnerService partnerService;
  private final ProductFormValidator validator;
  private final TransactionTokenManager tokenManager;
  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param productService 商品マスタ
   * @param partnerService 取引先マスタ（主仕入先の選択肢）
   * @param validator 倉庫ごとの設定の検査
   * @param tokenManager 二重送信防止
   * @param messageHelper メッセージ
   */
  public ProductController(
      ProductService productService,
      PartnerService partnerService,
      ProductFormValidator validator,
      TransactionTokenManager tokenManager,
      MessageHelper messageHelper) {
    this.productService = productService;
    this.partnerService = partnerService;
    this.validator = validator;
    this.tokenManager = tokenManager;
    this.messageHelper = messageHelper;
  }

  /**
   * 商品一覧（SCR-101）。初期表示では検索しない。
   *
   * @param searchForm 検索条件
   * @param result 入力チェックの結果
   * @param model モデル
   * @return 画面
   */
  @GetMapping
  public String list(
      @Validated @ModelAttribute("searchForm") MasterSearchForm searchForm,
      BindingResult result,
      Model model) {
    model.addAttribute("suppliers", partnerService.findSupplierOptions(searchForm.getSupplierId()));
    if (searchForm.isSearchRequested() && !result.hasErrors()) {
      MasterCriteria criteria = searchForm.toCriteria();
      MasterPages.search(
          criteria,
          searchForm.getPage(),
          productService::count,
          offset -> productService.search(criteria, Pagination.PAGE_SIZE, offset),
          model,
          messageHelper);
    }
    return LIST_VIEW;
  }

  /**
   * 登録画面を表示する。倉庫ごとの設定欄は有効な倉庫の分を表示する。
   *
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/new")
  public String newForm(HttpSession session, Model model) {
    ProductForm form = new ProductForm();
    form.setSettings(toSettingForms(productService.newSettingRows()));
    model.addAttribute(FORM, form);
    return showForm(session, model, form);
  }

  /**
   * 商品を登録する。
   *
   * @param operator 操作者
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param session セッション
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping
  public String create(
      @AuthenticationPrincipal LoginUser operator,
      @Validated @ModelAttribute(FORM) ProductForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    form.setProductId(null);
    List<ProductCommand.Setting> settings = validator.validate(form, result);
    if (result.hasErrors()) {
      return showForm(session, model, form);
    }
    try {
      productService.create(form.toCommand(settings), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model, form);
    }
    messageHelper.flashInfo(redirect, MessageId.I_CREATED, MessageHelper.label("label.product"));
    return REDIRECT_LIST;
  }

  /**
   * 編集画面を表示する。
   *
   * @param productId 商品ID
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/{productId}/edit")
  public String editForm(@PathVariable long productId, HttpSession session, Model model) {
    ProductForm form = toForm(find(productId));
    model.addAttribute(FORM, form);
    return showForm(session, model, form);
  }

  /**
   * 商品を更新する。
   *
   * @param operator 操作者
   * @param productId 商品ID
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param session セッション
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping("/{productId}")
  public String update(
      @AuthenticationPrincipal LoginUser operator,
      @PathVariable long productId,
      @Validated @ModelAttribute(FORM) ProductForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    // 商品コードは変更しない（画面から送信された値は使わない）
    form.setProductId(productId);
    form.setProductCode(find(productId).product().getProductCode());
    List<ProductCommand.Setting> settings = validator.validate(form, result);
    if (result.hasErrors()) {
      return showForm(session, model, form);
    }
    try {
      productService.update(form.toCommand(settings), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model, form);
    }
    messageHelper.flashInfo(redirect, MessageId.I_UPDATED, MessageHelper.label("label.product"));
    return REDIRECT_LIST;
  }

  private ProductDetail find(long productId) {
    return productService
        .find(productId)
        .orElseThrow(() -> new NotFoundException("m_product#" + productId));
  }

  private String showForm(HttpSession session, Model model, ProductForm form) {
    model.addAttribute("taxCategories", List.of(TaxCategory.values()));
    model.addAttribute("suppliers", partnerService.findSupplierOptions(form.getMainSupplierId()));
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return FORM_VIEW;
  }

  private static List<ReorderSettingForm> toSettingForms(List<ReorderSettingRow> rows) {
    return rows.stream()
        .map(
            row -> {
              ReorderSettingForm setting = new ReorderSettingForm();
              setting.setWarehouseId(row.warehouseId());
              setting.setWarehouseLabel(row.warehouseLabel());
              setting.setReorderPoint(toText(row.reorderPoint()));
              setting.setStandardOrderQuantity(toText(row.standardOrderQuantity()));
              return setting;
            })
        .toList();
  }

  private static String toText(Integer value) {
    return value == null ? null : value.toString();
  }

  private static ProductForm toForm(ProductDetail detail) {
    Product product = detail.product();
    ProductForm form = new ProductForm();
    form.setProductId(product.getProductId());
    form.setVersion(product.getVersion());
    form.setProductCode(product.getProductCode());
    form.setProductName(product.getProductName());
    form.setModelNumber(product.getModelNumber());
    form.setUnit(product.getUnit());
    form.setStandardSalesPrice(product.getStandardSalesPrice());
    form.setStandardPurchasePrice(product.getStandardPurchasePrice());
    form.setTaxCategory(product.getTaxCategory());
    form.setMainSupplierId(product.getMainSupplierId());
    form.setEnabled(product.isEnabled());
    form.setSettings(toSettingForms(detail.settings()));
    return form;
  }
}
