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
import jp.co.hikari.hanbai.master.form.PartnerForm;
import jp.co.hikari.hanbai.master.form.PartnerFormValidator;
import jp.co.hikari.hanbai.master.model.Partner;
import jp.co.hikari.hanbai.master.service.PartnerService;
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
 * SCR-121 取引先一覧・SCR-122 取引先登録・編集（F-103）。
 *
 * <p>一覧は全員、登録・編集は営業担当・経理担当・システム管理者（URL は SecurityConfig、更新は Service でも確認する）。
 */
@Controller
@RequestMapping(PartnerController.BASE_URL)
public class PartnerController {

  /** 取引先一覧の URL。 */
  public static final String BASE_URL = "/master/partners";

  private static final String LIST_VIEW = "master/partner-list";
  private static final String FORM_VIEW = "master/partner-form";
  private static final String FORM_KEY = "SCR-122";
  private static final String FORM = "form";
  private static final String REDIRECT_LIST = "redirect:" + BASE_URL + "?search=1";

  private final PartnerService partnerService;
  private final PartnerFormValidator validator;
  private final TransactionTokenManager tokenManager;
  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param partnerService 取引先マスタ
   * @param validator 相関チェック
   * @param tokenManager 二重送信防止
   * @param messageHelper メッセージ
   */
  public PartnerController(
      PartnerService partnerService,
      PartnerFormValidator validator,
      TransactionTokenManager tokenManager,
      MessageHelper messageHelper) {
    this.partnerService = partnerService;
    this.validator = validator;
    this.tokenManager = tokenManager;
    this.messageHelper = messageHelper;
  }

  /**
   * 取引先一覧（SCR-121）。初期表示では検索しない。
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
          partnerService::count,
          offset -> partnerService.search(criteria, Pagination.PAGE_SIZE, offset),
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
    model.addAttribute(FORM, new PartnerForm());
    return showForm(session, model);
  }

  /**
   * 取引先を登録する。
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
      @Validated @ModelAttribute(FORM) PartnerForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    form.setPartnerId(null);
    validator.validate(form, result);
    if (result.hasErrors()) {
      return showForm(session, model);
    }
    try {
      partnerService.create(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model);
    }
    messageHelper.flashInfo(redirect, MessageId.I_CREATED, MessageHelper.label("label.partner"));
    return REDIRECT_LIST;
  }

  /**
   * 編集画面を表示する。
   *
   * @param partnerId 取引先ID
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/{partnerId}/edit")
  public String editForm(@PathVariable long partnerId, HttpSession session, Model model) {
    model.addAttribute(FORM, toForm(find(partnerId)));
    return showForm(session, model);
  }

  /**
   * 取引先を更新する。
   *
   * @param operator 操作者
   * @param partnerId 取引先ID
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param session セッション
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping("/{partnerId}")
  public String update(
      @AuthenticationPrincipal LoginUser operator,
      @PathVariable long partnerId,
      @Validated @ModelAttribute(FORM) PartnerForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    // 取引先コードは変更しない（画面から送信された値は使わない）
    form.setPartnerId(partnerId);
    form.setPartnerCode(find(partnerId).getPartnerCode());
    validator.validate(form, result);
    if (result.hasErrors()) {
      return showForm(session, model);
    }
    try {
      partnerService.update(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model);
    }
    messageHelper.flashInfo(redirect, MessageId.I_UPDATED, MessageHelper.label("label.partner"));
    return REDIRECT_LIST;
  }

  private Partner find(long partnerId) {
    return partnerService
        .find(partnerId)
        .orElseThrow(() -> new NotFoundException("m_partner#" + partnerId));
  }

  private String showForm(HttpSession session, Model model) {
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return FORM_VIEW;
  }

  private static PartnerForm toForm(Partner partner) {
    PartnerForm form = new PartnerForm();
    form.setPartnerId(partner.getPartnerId());
    form.setVersion(partner.getVersion());
    form.setPartnerCode(partner.getPartnerCode());
    form.setPartnerName(partner.getPartnerName());
    form.setPartnerNameKana(partner.getPartnerNameKana());
    form.setCustomer(partner.isCustomer());
    form.setSupplier(partner.isSupplier());
    form.setPostalCode(partner.getPostalCode());
    form.setAddress(partner.getAddress());
    form.setTel(partner.getTel());
    form.setFax(partner.getFax());
    form.setContactName(partner.getContactName());
    form.setClosingDay(partner.getClosingDay());
    form.setPaymentMonthOffset(partner.getPaymentMonthOffset());
    form.setPaymentDay(partner.getPaymentDay());
    form.setEnabled(partner.isEnabled());
    return form;
  }
}
