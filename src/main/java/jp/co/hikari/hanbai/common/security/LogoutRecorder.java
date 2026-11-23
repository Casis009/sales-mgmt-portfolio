package jp.co.hikari.hanbai.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** ログアウト時に操作ログ（LOGOUT）を記録する（SCR-001 イベント 5）。セッションの破棄より前に実行する。 */
@Component
public class LogoutRecorder implements LogoutHandler {

  private final LoginService loginService;

  /**
   * 依存部品を受け取る。
   *
   * @param loginService ログインの記録
   */
  public LogoutRecorder(LoginService loginService) {
    this.loginService = loginService;
  }

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication != null && authentication.getPrincipal() instanceof LoginUser user) {
      loginService.recordLogout(user, request.getRemoteAddr());
    }
  }
}
