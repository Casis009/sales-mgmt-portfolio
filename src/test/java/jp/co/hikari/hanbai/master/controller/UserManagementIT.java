package jp.co.hikari.hanbai.master.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.security.TestUsers;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import jp.co.hikari.hanbai.master.dto.UserCommand;
import jp.co.hikari.hanbai.master.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** ユーザー管理（F-003、SCR-011・SCR-012）を、画面から実際の DB まで通して確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserManagementIT {

  /** 操作者（DB に存在しないユーザーID。登録したユーザーと同じIDにならないようにする）。 */
  private final LoginUser admin =
      new LoginUser(
          Long.MAX_VALUE, "operator", "操作者", null, List.of(Role.ADMIN), true, false, false, null);

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserService userService;
  @Autowired PasswordEncoder passwordEncoder;

  private long warehouseId;

  @BeforeEach
  void setUp() {
    TestUsers.clean(jdbc);
    jdbc.update("DELETE FROM m_warehouse");
    warehouseId =
        jdbc.queryForObject(
            "INSERT INTO m_warehouse (warehouse_code, warehouse_name, enabled,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES ('HQ', '本社倉庫', TRUE, now(), 0, now(), 0) RETURNING warehouse_id",
            Long.class);
  }

  private String page(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(user(admin))).andReturn().getResponse().getContentAsString();
  }

  /** 入力画面を開いてトークンを取得し、そのセッションで送信する。値の「,」は複数値の区切り。 */
  private MvcResult submit(String formUrl, String postUrl, Map<String, String> params)
      throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult opened = mvc.perform(get(formUrl).session(session).with(user(admin))).andReturn();
    assertThat(opened.getResponse().getStatus()).isEqualTo(200);
    String token =
        (String) opened.getModelAndView().getModel().get(TransactionTokenManager.PARAM);
    MockHttpServletRequestBuilder request =
        post(postUrl)
            .session(session)
            .with(csrf())
            .with(user(admin))
            .param(TransactionTokenManager.PARAM, token);
    params.forEach((k, v) -> request.param(k, v.split(",", -1)));
    return mvc.perform(request).andReturn();
  }

  private static Map<String, String> newUser(String loginId, String password) {
    Map<String, String> params = new HashMap<>();
    params.put("loginId", loginId);
    params.put("userName", "鈴木 一郎");
    params.put("department", "営業部");
    params.put("roles", "SALES,APPROVER");
    params.put("enabled", "true");
    params.put("initialPassword", password);
    return params;
  }

  private long operationLogCount() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM t_operation_log", Long.class);
  }

  @Test
  void 一覧の初期表示では検索せず検索するとキーワードと無効を条件に表示する() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    TestUsers.insert(jdbc, "tanaka", Role.SALES, Role.APPROVER);
    TestUsers.insert(jdbc, "retired", false, false, 0, false, Role.SALES);

    assertThat(page(get("/admin/users"))).doesNotContain("<table");

    String all = page(get("/admin/users").param("search", "1"));
    assertThat(all).contains("tanaka", "yamamoto", "営業担当・承認者").doesNotContain("retired");

    String keyword = page(get("/admin/users").param("search", "1").param("keyword", "yama"));
    assertThat(keyword).contains("yamamoto").doesNotContain("tanaka");

    String withDisabled =
        page(get("/admin/users").param("search", "1").param("includeDisabled", "true"));
    assertThat(withDisabled).contains("retired", "無効");

    assertThat(page(get("/admin/users").param("search", "1").param("keyword", "nobody")))
        .contains("該当するデータはありません。");
  }

  @Test
  void 一覧は50件ごとにページングし検索条件を保持したリンクを表示する() throws Exception {
    for (int i = 1; i <= 51; i++) {
      TestUsers.insert(jdbc, String.format("user%02d", i), Role.SALES);
    }
    String first = page(get("/admin/users").param("search", "1").param("keyword", "user"));
    assertThat(first).contains("51件中 1～50件", "user50").doesNotContain("user51");
    assertThat(first).contains("search=1&amp;keyword=user&amp;page=2");

    String second =
        page(get("/admin/users").param("search", "1").param("keyword", "user").param("page", "2"));
    assertThat(second).contains("51件中 51～51件", "user51");
  }

  @Test
  void ユーザーを登録すると初期パスワード変更要求付きで登録され操作ログが記録される() throws Exception {
    Map<String, String> params = newUser("suzuki", "Init1234");
    params.put("defaultWarehouseId", String.valueOf(warehouseId));
    MvcResult result = submit("/admin/users/new", "/admin/users", params);

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users?search=1");
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT user_id, password_hash, password_change_required, default_warehouse_id"
                + " FROM m_user WHERE login_id = 'suzuki'");
    assertThat(row.get("password_change_required")).isEqualTo(true);
    assertThat(passwordEncoder.matches("Init1234", (String) row.get("password_hash"))).isTrue();
    assertThat(row.get("default_warehouse_id")).isEqualTo(warehouseId);
    assertThat(
            jdbc.queryForList(
                "SELECT role_code FROM m_user_role WHERE user_id = ? ORDER BY role_code",
                String.class,
                row.get("user_id")))
        .containsExactly("APPROVER", "SALES");
    assertThat(
            jdbc.queryForMap(
                "SELECT operation_type, screen_id, target_key, detail FROM t_operation_log"))
        .containsEntry("operation_type", "CREATE")
        .containsEntry("screen_id", "SCR-012")
        .containsEntry("target_key", "suzuki")
        .containsEntry("detail", "権限：営業担当・承認者、状態：有効");
  }

  @Test
  void 登録の入力エラーと業務エラーを表示し登録しない() throws Exception {
    TestUsers.insert(jdbc, "tanaka", Role.SALES);

    String duplicate =
        submit("/admin/users/new", "/admin/users", newUser("tanaka", "Init1234"))
            .getResponse()
            .getContentAsString();
    assertThat(duplicate).contains("ログインIDは既に登録されています。");

    String weak =
        submit("/admin/users/new", "/admin/users", newUser("suzuki", "password"))
            .getResponse()
            .getContentAsString();
    assertThat(weak).contains("パスワードは8文字以上で、英字と数字を両方含めてください。");

    Map<String, String> invalidParams = newUser("ab", "");
    invalidParams.remove("roles");
    String invalid =
        submit("/admin/users/new", "/admin/users", invalidParams)
            .getResponse()
            .getContentAsString();
    assertThat(invalid)
        .contains("ログインIDの形式が正しくありません。", "権限を入力してください。", "初期パスワードを入力してください。");

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM m_user", Long.class)).isEqualTo(1);
    assertThat(operationLogCount()).isZero();
  }

  @Test
  void 編集でロックを解除し権限と状態を変更できるがログインIDは変わらない() throws Exception {
    long userId = TestUsers.insert(jdbc, "yamamoto", true, true, 5, false, Role.WAREHOUSE);
    String edit = "/admin/users/" + userId + "/edit";
    assertThat(page(get(edit))).contains("ロック中", "ロックを解除する");

    Map<String, String> params = new HashMap<>();
    params.put("version", "0");
    params.put("loginId", "changed");
    params.put("userName", "山本 太郎");
    params.put("roles", "WAREHOUSE,ADMIN");
    params.put("enabled", "false");
    params.put("unlock", "true");
    MvcResult result = submit(edit, "/admin/users/" + userId, params);

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/users?search=1");
    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM m_user WHERE user_id = ?", userId);
    assertThat(row)
        .containsEntry("login_id", "yamamoto")
        .containsEntry("locked", false)
        .containsEntry("enabled", false)
        .containsEntry("version", 1);
    assertThat(((Number) row.get("failed_login_count")).intValue()).isZero();
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .contains("権限：物流担当→物流担当・システム管理者", "状態：有効→無効", "ロック解除");
  }

  @Test
  void 他の利用者が先に更新していた場合はE020() throws Exception {
    long userId = TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    jdbc.update("UPDATE m_user SET version = 1 WHERE user_id = ?", userId);

    Map<String, String> params = new HashMap<>();
    params.put("version", "0");
    params.put("loginId", "yamamoto");
    params.put("userName", "山本");
    params.put("roles", "WAREHOUSE");
    params.put("enabled", "true");
    String result =
        submit("/admin/users/" + userId + "/edit", "/admin/users/" + userId, params)
            .getResponse()
            .getContentAsString();

    assertThat(result).contains("他のユーザーが先に更新しました。");
    assertThat(operationLogCount()).isZero();
  }

  private Map<String, String> editParams(String roles, String enabled) {
    Map<String, String> params = new HashMap<>();
    params.put("version", "0");
    params.put("loginId", "admin1");
    params.put("userName", "管理者");
    params.put("roles", roles);
    params.put("enabled", enabled);
    return params;
  }

  private String submitAs(LoginUser operator, long userId, Map<String, String> params)
      throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult opened =
        mvc.perform(get("/admin/users/" + userId + "/edit").session(session).with(user(operator)))
            .andReturn();
    String token =
        (String) opened.getModelAndView().getModel().get(TransactionTokenManager.PARAM);
    MockHttpServletRequestBuilder request =
        post("/admin/users/" + userId)
            .session(session)
            .with(csrf())
            .with(user(operator))
            .param(TransactionTokenManager.PARAM, token);
    params.forEach((k, v) -> request.param(k, v.split(",", -1)));
    return mvc.perform(request).andReturn().getResponse().getContentAsString();
  }

  @Test
  void 自分自身の無効化とADMINの削除はE018で変更されない() throws Exception {
    long selfId = TestUsers.insert(jdbc, "admin1", Role.ADMIN);
    TestUsers.insert(jdbc, "admin2", Role.ADMIN);
    LoginUser self =
        new LoginUser(selfId, "admin1", "管理者", null, List.of(Role.ADMIN), true, false, false, null);

    assertThat(submitAs(self, selfId, editParams("ADMIN", "false")))
        .contains("自分自身を無効にしたり、システム管理者の権限を外したりすることはできません。");
    assertThat(submitAs(self, selfId, editParams("SALES", "true")))
        .contains("自分自身を無効にしたり、システム管理者の権限を外したりすることはできません。");
    // ADMIN を維持する自分自身の変更はできる（成功時はリダイレクトのため本文は空）
    assertThat(submitAs(self, selfId, editParams("ADMIN,SALES", "true")))
        .doesNotContain("自分自身を無効");

    assertThat(
            jdbc.queryForList(
                "SELECT role_code FROM m_user_role WHERE user_id = ? ORDER BY role_code",
                String.class,
                selfId))
        .containsExactly("ADMIN", "SALES");
  }

  @Test
  void 有効なシステム管理者が0人になる変更はE019で他に管理者がいれば変更できる() throws Exception {
    long onlyAdmin = TestUsers.insert(jdbc, "admin1", Role.ADMIN);
    TestUsers.insert(jdbc, "retired", false, false, 0, false, Role.ADMIN);

    assertThat(submitAs(admin, onlyAdmin, editParams("SALES", "true")))
        .contains("有効なシステム管理者が1人もいなくなるため、変更できません。");
    assertThat(
            jdbc.queryForObject(
                "SELECT enabled FROM m_user WHERE user_id = ?", Boolean.class, onlyAdmin))
        .isTrue();
    assertThat(operationLogCount()).isZero();

    TestUsers.insert(jdbc, "admin2", Role.ADMIN);
    submitAs(admin, onlyAdmin, editParams("ADMIN", "false"));
    assertThat(
            jdbc.queryForObject(
                "SELECT enabled FROM m_user WHERE user_id = ?", Boolean.class, onlyAdmin))
        .isFalse();
  }

  @Test
  void 存在しないユーザーの編集画面は404() throws Exception {
    int status =
        mvc.perform(get("/admin/users/999999/edit").with(user(admin)))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(404);
  }

  @Test
  void システム管理者以外はServiceの更新処理も実行できない() {
    LoginUser sales = TestUsers.of(Role.SALES);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                sales, null, sales.getAuthorities()));
    try {
      UserCommand command =
          new UserCommand(
              null, 0, "hacker", "x", null, null, List.of(Role.ADMIN), true, false, "Init1234");
      assertThatThrownBy(() -> userService.create(command, sales))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void ユーザーがいない場合の初期管理者はパスワード変更を求められる() throws Exception {
    assertThat(userService.createInitialAdminIfNoUsers("admin", "Admin2027")).isTrue();
    assertThat(userService.createInitialAdminIfNoUsers("admin", "Admin2027")).isFalse();

    MvcResult login =
        mvc.perform(
                post("/login")
                    .param("loginId", "admin")
                    .param("password", "Admin2027")
                    .with(csrf()))
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
    assertThat(
            mvc.perform(get("/admin/users").session(session))
                .andReturn()
                .getResponse()
                .getRedirectedUrl())
        .isEqualTo("/password");
  }
}
