package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

/** ログイン・ログアウト（F-001、CM-04・CM-05）と画面共通部品を、実際の DB と Spring Security で確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIT {

  private static final String E010 = "ログインIDまたはパスワードが正しくありません。";
  private static final String E011 = "アカウントがロックされています。システム管理者に連絡してください。";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    TestUsers.clean(jdbc);
  }

  @Test
  void ログインに成功するとメニューへ遷移し失敗回数が0に戻り操作ログが記録される() throws Exception {
    long userId = TestUsers.insert(jdbc, "yamamoto", true, false, 4, false, Role.WAREHOUSE);

    mvc.perform(login(" yamamoto ", TestUsers.PASSWORD))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/menu"));

    Map<String, Object> user =
        jdbc.queryForMap(
            "SELECT failed_login_count, last_login_at FROM m_user WHERE user_id = ?", userId);
    assertThat(((Number) user.get("failed_login_count")).intValue()).isZero();
    assertThat(user.get("last_login_at")).isNotNull();
    assertThat(operationLogs()).containsExactly("LOGIN:1");
  }

  @Test
  void 認証に5回失敗するとロックされ正しいパスワードでもE011になる() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);

    for (int i = 1; i <= 4; i++) {
      assertThat(pageAfter(login("yamamoto", "wrong-pass1"))).contains(E010);
    }
    assertThat(failedCount("yamamoto")).isEqualTo(4);
    assertThat(locked("yamamoto")).isFalse();

    assertThat(pageAfter(login("yamamoto", "wrong-pass1"))).contains(E010);
    assertThat(failedCount("yamamoto")).isEqualTo(5);
    assertThat(locked("yamamoto")).isTrue();

    assertThat(pageAfter(login("yamamoto", TestUsers.PASSWORD))).contains(E011);
    assertThat(operationLogs()).hasSize(6).allMatch(s -> s.equals("LOGIN_FAILURE:9"));
  }

  @Test
  void 存在しないログインIDと無効ユーザーはどちらもE010でロックとは区別されない() throws Exception {
    TestUsers.insert(jdbc, "retired", false, false, 0, false, Role.SALES);

    String unknown = pageAfter(login("nobody", TestUsers.PASSWORD));
    String disabled = pageAfter(login("retired", TestUsers.PASSWORD));

    assertThat(unknown).contains(E010).doesNotContain(E011);
    assertThat(disabled).contains(E010).doesNotContain(E011);
    assertThat(failedCount("retired")).isZero();
  }

  @Test
  void 未入力の場合はE001を表示し入力したログインIDを再表示する() throws Exception {
    String page = pageAfter(login("yamamoto", ""));
    assertThat(page)
        .contains("パスワードを入力してください。")
        .doesNotContain("ログインIDを入力してください。")
        .contains("value=\"yamamoto\"");

    assertThat(pageAfter(login("", ""))).contains("ログインIDを入力してください。");
    assertThat(operationLogs()).isEmpty();
  }

  @Test
  void 初期パスワードのユーザーはパスワード変更画面へ強制的に遷移する() throws Exception {
    TestUsers.insert(jdbc, "newuser", true, false, 0, true, Role.SALES);
    MockHttpSession session =
        sessionOf(mvc.perform(login("newuser", TestUsers.PASSWORD)).andReturn());

    mvc.perform(get("/menu").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/password"));
  }

  @Test
  void ログアウトするとセッションを破棄し操作ログが記録される() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    MockHttpSession session =
        sessionOf(mvc.perform(login("yamamoto", TestUsers.PASSWORD)).andReturn());

    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));

    assertThat(session.isInvalid()).isTrue();
    assertThat(operationLogs()).containsExactly("LOGIN:1", "LOGOUT:1");
  }

  @Test
  void 未ログインで画面を開くとログイン画面へ遷移しセッション切れはE013を表示する() throws Exception {
    mvc.perform(get("/menu"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
    mvc.perform(get("/login").param("timeout", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("一定時間操作がなかったため")));
  }

  @Test
  void ログイン済みでログイン画面を開くとメニューへ遷移する() throws Exception {
    mvc.perform(get("/login").with(user(TestUsers.of(Role.SALES))))
        .andExpect(redirectedUrl("/menu"));
  }

  @Test
  void CSRFトークンのないPOSTは処理せずセッション切れとしてログイン画面へ遷移する() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    mvc.perform(post("/login").param("loginId", "yamamoto").param("password", TestUsers.PASSWORD))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?timeout=1"));
    assertThat(operationLogs()).isEmpty();
  }

  @Test
  void クリックジャッキング対策のヘッダーを付ける() throws Exception {
    mvc.perform(get("/login")).andExpect(header().string("X-Frame-Options", "DENY"));
  }

  @Test
  void メニューは権限に応じた画面とダッシュボードを表示する() throws Exception {
    String warehouse =
        mvc.perform(get("/menu").with(user(TestUsers.of(Role.WAREHOUSE))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(warehouse)
        .contains("発注アラート件数", "未出荷受注件数", "在庫調整登録", "テスト ユーザー", "（物流担当）")
        .doesNotContain("承認待ち件数", "ユーザー一覧", "請求締め処理");

    mvc.perform(get("/menu").with(user(TestUsers.of(Role.ADMIN))))
        .andExpect(content().string(containsString("ユーザー一覧")))
        .andExpect(content().string(not(containsString("発注アラート件数"))));
  }

  @Test
  void 許可されたロールで存在しないページを開くとE091を表示する() throws Exception {
    // 在庫照会は製造 3段階で実装する（それまでは、許可されたロールでも存在しないページ）
    mvc.perform(get("/stock/stocks").with(user(TestUsers.of(Role.SALES))))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("指定されたページは存在しません。")));
  }

  private static RequestBuilder login(String loginId, String password) {
    return post("/login").param("loginId", loginId).param("password", password).with(csrf());
  }

  private static MockHttpSession sessionOf(MvcResult result) {
    return (MockHttpSession) result.getRequest().getSession();
  }

  /** ログインを実行し、リダイレクト先のログイン画面に表示された内容を返す。 */
  private String pageAfter(RequestBuilder login) throws Exception {
    MvcResult result =
        mvc.perform(login)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andReturn();
    return mvc.perform(get("/login").session(sessionOf(result)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private int failedCount(String loginId) {
    return jdbc.queryForObject(
        "SELECT failed_login_count FROM m_user WHERE login_id = ?", Integer.class, loginId);
  }

  private boolean locked(String loginId) {
    return jdbc.queryForObject(
        "SELECT locked FROM m_user WHERE login_id = ?", Boolean.class, loginId);
  }

  private List<String> operationLogs() {
    return jdbc.queryForList(
        "SELECT operation_type || ':' || result FROM t_operation_log ORDER BY operation_log_id",
        String.class);
  }
}
