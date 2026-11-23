package jp.co.hikari.hanbai.common.controller;

import jp.co.hikari.hanbai.common.config.SecurityConfig;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.message.UiMessage;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * SCR-001 ログイン（F-001）。認証（POST /login）とログアウト（POST /logout）は Spring Security が処理する。
 */
@Controller
public class LoginController {

  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param messageHelper メッセージ
   */
  public LoginController(MessageHelper messageHelper) {
    this.messageHelper = messageHelper;
  }

  /**
   * ログイン画面を表示する。ログイン済みの場合はメニューへ遷移する。セッション切れで遷移してきた場合は E-013 を表示する。
   *
   * @param user ログイン中のユーザー（未ログインは null）
   * @param timeout セッション切れのパラメータ
   * @param model モデル
   * @return 画面
   */
  @GetMapping(SecurityConfig.LOGIN_URL)
  public String show(
      @AuthenticationPrincipal LoginUser user,
      @RequestParam(name = SecurityConfig.TIMEOUT_PARAM, required = false) String timeout,
      Model model) {
    if (user != null) {
      return "redirect:" + SecurityConfig.MENU_URL;
    }
    if (timeout != null) {
      messageHelper.add(model, UiMessage.Level.ERROR, MessageId.E_SESSION_TIMEOUT);
    }
    return "common/login";
  }
}
