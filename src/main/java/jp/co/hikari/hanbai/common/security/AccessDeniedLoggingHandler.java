package jp.co.hikari.hanbai.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * URL 単位の権限エラー（E-012、HTTP 403。セキュリティ設計 3章）。
 *
 * <p>WARN ログにユーザー・URL を出力し、エラー画面（error/403）を表示する。
 */
public class AccessDeniedLoggingHandler implements AccessDeniedHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AccessDeniedLoggingHandler.class);

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    Principal user = request.getUserPrincipal();
    LOG.warn(
        "権限エラー: user={}, url={} {}",
        user == null ? "-" : user.getName(),
        request.getMethod(),
        request.getRequestURI());
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}
