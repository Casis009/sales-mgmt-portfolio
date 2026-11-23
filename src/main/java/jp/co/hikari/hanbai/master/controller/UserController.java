package jp.co.hikari.hanbai.master.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.NotFoundException;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.message.UiMessage;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.security.PasswordPolicy;
import jp.co.hikari.hanbai.common.web.PageUrls;
import jp.co.hikari.hanbai.common.web.Pagination;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import jp.co.hikari.hanbai.master.dto.UserCriteria;
import jp.co.hikari.hanbai.master.dto.UserDetail;
import jp.co.hikari.hanbai.master.form.UserForm;
import jp.co.hikari.hanbai.master.form.UserSearchForm;
import jp.co.hikari.hanbai.master.model.User;
import jp.co.hikari.hanbai.master.service.UserService;
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
 * SCR-011 ユーザー一覧・SCR-012 ユーザー登録・編集（F-003）。
 *
 * <p>システム管理者のみ利用できる（URL は SecurityConfig、更新は Service の {@code @PreAuthorize} でも確認する）。
 */
@Controller
@RequestMapping(UserController.BASE_URL)
public class UserController {

  /** ユーザー一覧の URL。 */
  public static final String BASE_URL = "/admin/users";

  private static final String LIST_VIEW = "master/user-list";
  private static final String FORM_VIEW = "master/user-form";
  private static final String FORM_KEY = "SCR-012";
  private static final String FORM = "form";
  private static final String INITIAL_PASSWORD = "initialPassword";
  private static final String REDIRECT_LIST = "redirect:" + BASE_URL + "?search=1";

  private final UserService userService;
  private final WarehouseService warehouseService;
  private final PasswordPolicy passwordPolicy;
  private final TransactionTokenManager tokenManager;
  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param userService ユーザー管理
   * @param warehouseService 倉庫マスタ（所属倉庫の選択肢）
   * @param passwordPolicy パスワードの条件
   * @param tokenManager 二重送信防止
   * @param messageHelper メッセージ
   */
  public UserController(
      UserService userService,
      WarehouseService warehouseService,
      PasswordPolicy passwordPolicy,
      TransactionTokenManager tokenManager,
      MessageHelper messageHelper) {
    this.userService = userService;
    this.warehouseService = warehouseService;
    this.passwordPolicy = passwordPolicy;
    this.tokenManager = tokenManager;
    this.messageHelper = messageHelper;
  }

  /**
   * ユーザー一覧（SCR-011）。初期表示では検索しない。
   *
   * @param searchForm 検索条件
   * @param result 入力チェックの結果
   * @param model モデル
   * @return 画面
   */
  @GetMapping
  public String list(
      @Validated @ModelAttribute("searchForm") UserSearchForm searchForm,
      BindingResult result,
      Model model) {
    if (!searchForm.isSearchRequested() || result.hasErrors()) {
      return LIST_VIEW;
    }
    UserCriteria criteria = searchForm.toCriteria();
    long total = userService.count(criteria);
    Pagination pagination =
        Pagination.of(searchForm.getPage(), total, PageUrls.currentWithoutPage());
    model.addAttribute("pagination", pagination);
    model.addAttribute(
        "rows", userService.search(criteria, pagination.limit(), pagination.offset()));
    if (total == 0) {
      messageHelper.add(model, UiMessage.Level.INFO, MessageId.I_NO_DATA);
    } else if (pagination.tooMany()) {
      messageHelper.add(
          model, UiMessage.Level.WARN, MessageId.W_TOO_MANY_RESULTS, Pagination.MAX_RESULTS);
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
    model.addAttribute(FORM, new UserForm());
    return showForm(session, model, null);
  }

  /**
   * ユーザーを登録する。
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
      @Validated @ModelAttribute(FORM) UserForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    form.setUserId(null);
    String password = form.getInitialPassword();
    if (password == null || password.isEmpty()) {
      result.rejectValue(
          INITIAL_PASSWORD,
          MessageId.E_REQUIRED,
          new Object[] {MessageHelper.label(INITIAL_PASSWORD)},
          null);
    }
    passwordPolicy.validate(password, form.getLoginId(), result, INITIAL_PASSWORD);
    if (result.hasErrors()) {
      return showForm(session, model, null);
    }
    try {
      userService.create(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model, null);
    }
    messageHelper.flashInfo(redirect, MessageId.I_CREATED, MessageHelper.label("label.user"));
    return REDIRECT_LIST;
  }

  /**
   * 編集画面を表示する。
   *
   * @param userId ユーザーID
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/{userId}/edit")
  public String editForm(@PathVariable long userId, HttpSession session, Model model) {
    UserDetail detail = find(userId);
    model.addAttribute(FORM, toForm(detail));
    return showForm(session, model, detail);
  }

  /**
   * ユーザーを更新する。
   *
   * @param operator 操作者
   * @param userId ユーザーID
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param session セッション
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping("/{userId}")
  public String update(
      @AuthenticationPrincipal LoginUser operator,
      @PathVariable long userId,
      @Validated @ModelAttribute(FORM) UserForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpSession session,
      Model model,
      RedirectAttributes redirect) {
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return REDIRECT_LIST;
    }
    UserDetail detail = find(userId);
    // ログインIDは変更しない（画面から送信された値は使わない）
    form.setUserId(userId);
    form.setLoginId(detail.user().getLoginId());
    form.setInitialPassword(null);
    if (result.hasErrors()) {
      return showForm(session, model, detail);
    }
    try {
      userService.update(form.toCommand(), operator);
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return showForm(session, model, detail);
    }
    messageHelper.flashInfo(redirect, MessageId.I_UPDATED, MessageHelper.label("label.user"));
    return REDIRECT_LIST;
  }

  private UserDetail find(long userId) {
    return userService.find(userId).orElseThrow(() -> new NotFoundException("m_user#" + userId));
  }

  private String showForm(HttpSession session, Model model, UserDetail detail) {
    model.addAttribute("detail", detail);
    model.addAttribute("allRoles", List.of(Role.values()));
    model.addAttribute("warehouses", warehouseService.findEnabledOptions());
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return FORM_VIEW;
  }

  private static UserForm toForm(UserDetail detail) {
    User user = detail.user();
    UserForm form = new UserForm();
    form.setUserId(user.getUserId());
    form.setVersion(user.getVersion());
    form.setLoginId(user.getLoginId());
    form.setUserName(user.getUserName());
    form.setDepartment(user.getDepartment());
    form.setDefaultWarehouseId(user.getDefaultWarehouseId());
    form.setRoles(detail.roles());
    form.setEnabled(user.isEnabled());
    return form;
  }
}
