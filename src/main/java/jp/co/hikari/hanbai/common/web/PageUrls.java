package jp.co.hikari.hanbai.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/** 一覧画面の URL の組み立て。 */
public final class PageUrls {

  private static final String PAGE = "page";

  private PageUrls() {}

  /**
   * 現在のリクエストの URL とパラメータ（{@code page} を除く）から、ページングのリンクの元になる URL を返す。
   *
   * @return 例：/admin/users?search=1&amp;keyword=tanaka
   */
  public static String currentWithoutPage() {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
    for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
      if (!PAGE.equals(e.getKey())) {
        builder.queryParam(e.getKey(), (Object[]) e.getValue());
      }
    }
    return builder.encode().build().toUriString();
  }
}
