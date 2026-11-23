package jp.co.hikari.hanbai.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 初期パスワードのままのユーザーを、パスワード変更画面（SCR-003）へ強制的に遷移させる（セキュリティ設計 1章 No.4）。
 *
 * <p>パスワードを変更するまで、パスワード変更・ログアウト・静的ファイル・エラー画面以外は利用できない。
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

  /** パスワード変更画面の URL。 */
  public static final String PASSWORD_URL = "/password";

  private static final List<String> ALLOWED_PREFIXES =
      List.of(PASSWORD_URL, "/logout", "/css/", "/js/", "/error");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getPrincipal() instanceof LoginUser user
        && user.isPasswordChangeRequired()
        && !isAllowed(request)) {
      response.sendRedirect(request.getContextPath() + PASSWORD_URL);
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean isAllowed(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
  }
}
