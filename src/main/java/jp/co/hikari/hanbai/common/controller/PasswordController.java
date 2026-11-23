package jp.co.hikari.hanbai.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import jp.co.hikari.hanbai.common.config.SecurityConfig;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.form.PasswordChangeForm;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.security.PasswordChangeRequiredFilter;
import jp.co.hikari.hanbai.common.security.PasswordPolicy;
import jp.co.hikari.hanbai.common.security.PasswordService;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** SCR-003 パスワード変更（F-002）。 */
@Controller
@RequestMapping(PasswordChangeRequiredFilter.PASSWORD_URL)
public class PasswordController {

  private static final String VIEW = "common/password";
  private static final String FORM_KEY = "SCR-003";

  private final PasswordService passwordService;
  private final PasswordPolicy passwordPolicy;
  private final TransactionTokenManager tokenManager;
  private final MessageHelper messageHelper;
  private final SecurityContextRepository contextRepository =
      new HttpSessionSecurityContextRepository();

  /**
   * 依存部品を受け取る。
   *
   * @param passwordService パスワード変更
   * @param passwordPolicy パスワードの条件
   * @param tokenManager 二重送信防止
   * @param messageHelper メッセージ
   */
  public PasswordController(
      PasswordService passwordService,
      PasswordPolicy passwordPolicy,
      TransactionTokenManager tokenManager,
      MessageHelper messageHelper) {
    this.passwordService = passwordService;
    this.passwordPolicy = passwordPolicy;
    this.tokenManager = tokenManager;
    this.messageHelper = messageHelper;
  }

  /**
   * 入力画面を表示する。
   *
   * @param form 入力
   * @param session セッション
   * @param model モデル
   * @return 画面
   */
  @GetMapping
  public String show(
      @ModelAttribute("form") PasswordChangeForm form, HttpSession session, Model model) {
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return VIEW;
  }

  /**
   * パスワードを変更する。完了後はメニューへ遷移する（初期パスワードの場合は、ここで他の画面が利用できるようになる）。
   *
   * @param user ログインユーザー
   * @param form 入力
   * @param result 入力チェックの結果
   * @param token 二重送信防止のトークン
   * @param request リクエスト
   * @param response レスポンス
   * @param model モデル
   * @param redirect リダイレクト属性
   * @return 遷移先
   */
  @PostMapping
  public String change(
      @AuthenticationPrincipal LoginUser user,
      @Validated @ModelAttribute("form") PasswordChangeForm form,
      BindingResult result,
      @RequestParam(name = TransactionTokenManager.PARAM, required = false) String token,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model,
      RedirectAttributes redirect) {
    HttpSession session = request.getSession();
    if (!tokenManager.consume(session, FORM_KEY, token)) {
      return "redirect:" + SecurityConfig.MENU_URL;
    }
    passwordPolicy.validate(form.getNewPassword(), user.getLoginId(), result, "newPassword");
    if (!result.hasFieldErrors("newPassword")
        && !result.hasFieldErrors("confirmPassword")
        && !Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
      result.rejectValue("confirmPassword", MessageId.E_PASSWORD_CONFIRM);
    }
    if (result.hasErrors()) {
      return redisplay(session, model);
    }
    try {
      passwordService.changePassword(user, form.getCurrentPassword(), form.getNewPassword());
    } catch (BusinessException e) {
      messageHelper.addError(model, e);
      return redisplay(session, model);
    }
    refreshLoginUser(user.withPasswordChanged(), request, response);
    messageHelper.flashInfo(redirect, MessageId.I_PASSWORD_CHANGED);
    return "redirect:" + SecurityConfig.MENU_URL;
  }

  private String redisplay(HttpSession session, Model model) {
    model.addAttribute(TransactionTokenManager.PARAM, tokenManager.issue(session, FORM_KEY));
    return VIEW;
  }

  /** セッションのログインユーザーを、パスワード変更要求フラグを解除したものに置き換える。 */
  private void refreshLoginUser(
      LoginUser changed, HttpServletRequest request, HttpServletResponse response) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            changed, null, changed.getAuthorities()));
    SecurityContextHolder.setContext(context);
    contextRepository.saveContext(context, request, response);
  }
}
