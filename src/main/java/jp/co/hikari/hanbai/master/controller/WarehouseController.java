package jp.co.hikari.hanbai.master.controller;

import jakarta.servlet.http.HttpSession;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.NotFoundException;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.web.Pagination;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import jp.co.hikari.hanbai.master.form.MasterSearchForm;
import jp.co.hikari.hanbai.master.form.WarehouseForm;
import jp.co.hikari.hanbai.master.model.Warehouse;
import jp.co.hikari.hanbai.master.service.WarehouseService;
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
 * SCR-111 倉庫一覧・SCR-112 倉庫登録・編集（F-102）。
 *
 * <p>一覧は全員、登録・編集はシステム管理者のみ（URL は SecurityConfig、更新は Service でも確認する）。
 */
@Controller
@RequestMapping(WarehouseController.BASE_URL)
public class WarehouseController {

  /** 倉庫一覧の URL。 */
  public static final String BASE_URL = "/master/warehouses";

  private static final String LIST_VIEW = "master/warehouse-list";
  private static final String FORM_VIEW = "master/warehouse-form";
  private static final String FORM_KEY = "SCR-112";
  private static final String FORM = "form";
  private static final String REDIRECT_LIST = "redirect:" + BASE_URL + "?search=1";

  private final WarehouseService warehouseService;
  private final TransactionTokenManager tokenManager;
  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param warehouseService 倉庫マスタ
   * @param tokenManager 二重送信防止
   * @param messageHelper メッセージ
   */
  public WarehouseController(
      WarehouseService warehouseService,
      TransactionTokenManager tokenManager,
      MessageHelper messageHelper) {
    this.warehouseService = warehouseService;
    this.tokenManager = tokenManager;
    this.messageHelper = messageHelper;
  }

  /**
   * 倉庫一覧（SCR-111）。初期表示では検索しない。
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
    if (searchForm.isSearchRequested() && !result.hasErrors()) {
      MasterCriteria criteria = searchForm.toCriteria();
      MasterPages.search(
          criteria,
          searchForm.getPage(),
          warehouseService::count,
          offset -> warehouseService.search(criteria, Pagination.PAGE_SIZE, offset),
          model,
          messageHelper);
    }
    return LIST_VIEW;
  }

  /**
   * 登録画面を表示する。
   *
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/new")
  public String newForm(HttpSession session, Model model) {
    model.addAttribute(FORM, new WarehouseForm());
    return showForm(session, model);
  }

  /**
   * 倉庫を登録する。
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
      @Validated @ModelAttribute(FORM) WarehouseForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    form.setWarehouseId(null);
    if (result.hasErrors()) {
      return showForm(session, model);
    }
    try {
      warehouseService.create(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model);
    }
    messageHelper.flashInfo(redirect, MessageId.I_CREATED, MessageHelper.label("label.warehouse"));
    return REDIRECT_LIST;
  }

  /**
   * 編集画面を表示する。
   *
   * @param warehouseId 倉庫ID
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/{warehouseId}/edit")
  public String editForm(@PathVariable long warehouseId, HttpSession session, Model model) {
    model.addAttribute(FORM, toForm(find(warehouseId)));
    return showForm(session, model);
  }

  /**
   * 倉庫を更新する。
   *
   * @param operator 操作者
   * @param warehouseId 倉庫ID
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param session セッション
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping("/{warehouseId}")
  public String update(
      @AuthenticationPrincipal LoginUser operator,
      @PathVariable long warehouseId,
      @Validated @ModelAttribute(FORM) WarehouseForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    // 倉庫コードは変更しない（画面から送信された値は使わない）
    form.setWarehouseId(warehouseId);
    form.setWarehouseCode(find(warehouseId).getWarehouseCode());
    if (result.hasErrors()) {
      return showForm(session, model);
    }
    try {
      warehouseService.update(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model);
    }
    messageHelper.flashInfo(redirect, MessageId.I_UPDATED, MessageHelper.label("label.warehouse"));
    return REDIRECT_LIST;
  }

  private Warehouse find(long warehouseId) {
    return warehouseService
        .find(warehouseId)
        .orElseThrow(() -> new NotFoundException("m_warehouse#" + warehouseId));
  }

  private String showForm(HttpSession session, Model model) {
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return FORM_VIEW;
  }

  private static WarehouseForm toForm(Warehouse warehouse) {
    WarehouseForm form = new WarehouseForm();
    form.setWarehouseId(warehouse.getWarehouseId());
    form.setVersion(warehouse.getVersion());
    form.setWarehouseCode(warehouse.getWarehouseCode());
    form.setWarehouseName(warehouse.getWarehouseName());
    form.setPostalCode(warehouse.getPostalCode());
    form.setAddress(warehouse.getAddress());
    form.setTel(warehouse.getTel());
    form.setEnabled(warehouse.isEnabled());
    return form;
  }
}
