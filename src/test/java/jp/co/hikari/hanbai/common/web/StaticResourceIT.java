package jp.co.hikari.hanbai.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * 静的ファイル（CSS・JS）の配信を、実際の組み込みサーバー（Tomcat）で確認する。
 *
 * <p>MockMvc では再現できない動作（URL へのセッションIDの付与、Cookie の古いセッションID）を対象とする。
 * 初回アクセス時・アプリ再起動後・セッション切れ後でも、ログイン画面の CSS が読み込めることを確認する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class StaticResourceIT {

  /** アプリの再起動前に発行された（現在は存在しない）セッションID。 */
  private static final String STALE_SESSION = "JSESSIONID=0123456789ABCDEF0123456789ABCDEF";

  @LocalServerPort int port;

  private final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  private HttpResponse<String> get(String path, String cookie) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
    if (cookie != null) {
      request.header("Cookie", cookie);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void 初回アクセスのログイン画面のCSSとJSのURLにセッションIDが付かない() throws Exception {
    HttpResponse<String> login = get("/login", null);

    assertThat(login.statusCode()).isEqualTo(200);
    assertThat(login.body())
        .contains("href=\"/css/app.css\"", "src=\"/js/app.js\"")
        .doesNotContain("jsessionid");
  }

  @Test
  void 古いセッションIDのCookieが付いていてもCSSとJSを配信する() throws Exception {
    HttpResponse<String> css = get("/css/app.css", STALE_SESSION);
    HttpResponse<String> js = get("/js/app.js", STALE_SESSION);

    assertThat(css.statusCode()).isEqualTo(200);
    assertThat(css.headers().firstValue("Content-Type"))
        .hasValueSatisfying(v -> assertThat(v).startsWith("text/css"));
    assertThat(js.statusCode()).isEqualTo(200);
  }

  @Test
  void 静的ファイルの取得ではセッションを作成しない() throws Exception {
    HttpResponse<String> css = get("/css/app.css", null);
    assertThat(css.statusCode()).isEqualTo(200);
    assertThat(css.headers().allValues("Set-Cookie")).isEmpty();
  }

  @Test
  void 古いセッションIDで画面を開くとセッション切れとしてログイン画面へ遷移する() throws Exception {
    HttpResponse<String> menu = get("/menu", STALE_SESSION);
    assertThat(menu.statusCode()).isEqualTo(302);
    assertThat(menu.headers().firstValue("Location"))
        .hasValueSatisfying(v -> assertThat(v).endsWith("/login?timeout=1"));
  }
}
