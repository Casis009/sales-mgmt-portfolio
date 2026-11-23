package jp.co.hikari.hanbai.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ログの MDC にリクエストID・ログインID を設定する（共通処理設計 8章）。
 *
 * <p>ログの形式「日時 レベル [リクエストID] [ログインID] クラス名 - メッセージ」の2項目を埋め、1つのリクエストで出力されたログをリクエストIDで追跡できるようにする。
 * Spring Security のフィルタより後に実行し、認証済みであればログインID を設定する。
 */
public class RequestMdcFilter extends OncePerRequestFilter {

  /** MDC のキー：リクエストID。 */
  public static final String REQUEST_ID = "requestId";

  /** MDC のキー：ログインID。 */
  public static final String LOGIN_ID = "loginId";

  private static final int REQUEST_ID_LENGTH = 8;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    MDC.put(REQUEST_ID, UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH));
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      MDC.put(LOGIN_ID, auth.getName());
    }
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(REQUEST_ID);
      MDC.remove(LOGIN_ID);
    }
  }
}
