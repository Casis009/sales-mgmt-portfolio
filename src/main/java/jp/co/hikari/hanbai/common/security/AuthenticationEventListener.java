package jp.co.hikari.hanbai.common.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** 認証の成功・失敗イベントを受け取り、失敗回数・ロック・操作ログを記録する（セキュリティ設計 1章）。 */
@Component
public class AuthenticationEventListener {

  private final LoginService loginService;

  /**
   * 依存部品を受け取る。
   *
   * @param loginService ログインの記録
   */
  public AuthenticationEventListener(LoginService loginService) {
    this.loginService = loginService;
  }

  /**
   * 認証成功。
   *
   * @param event イベント
   */
  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    if (event.getAuthentication().getPrincipal() instanceof LoginUser user) {
      loginService.recordSuccess(user, ipAddress(event));
    }
  }

  /**
   * パスワード不一致（存在しないログインIDを含む）。
   *
   * @param event イベント
   */
  @EventListener
  public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
    Object credentials = event.getAuthentication().getCredentials();
    if (credentials == null || credentials.toString().isEmpty()) {
      // パスワード未入力は入力チェックのエラー（E-001）であり、認証の試行として数えない
      return;
    }
    loginService.recordBadCredentials(event.getAuthentication().getName(), ipAddress(event));
  }

  /**
   * ロック中のユーザー。
   *
   * @param event イベント
   */
  @EventListener
  public void onLocked(AuthenticationFailureLockedEvent event) {
    loginService.recordRejected(event.getAuthentication().getName(), ipAddress(event), "ロック中");
  }

  /**
   * 無効のユーザー。
   *
   * @param event イベント
   */
  @EventListener
  public void onDisabled(AuthenticationFailureDisabledEvent event) {
    loginService.recordRejected(event.getAuthentication().getName(), ipAddress(event), "無効ユーザー");
  }

  private static String ipAddress(AbstractAuthenticationEvent event) {
    Authentication auth = event.getAuthentication();
    return auth.getDetails() instanceof WebAuthenticationDetails d ? d.getRemoteAddress() : null;
  }
}
