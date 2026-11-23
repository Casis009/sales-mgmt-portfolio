package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.web.TransactionTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** パスワード変更（F-002、SCR-003）を、ログインから実際の DB まで通して確認する。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PasswordChangeIT {

  private static final String NEW_PASSWORD = "NewPass99";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    TestUsers.clean(jdbc);
  }

  private MockHttpSession login(String loginId) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/login")
                    .param("loginId", loginId)
                    .param("password", TestUsers.PASSWORD)
                    .with(csrf()))
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession();
  }

  /** 入力画面を開いて二重送信防止のトークンを取得する。 */
  private String openForm(MockHttpSession session) throws Exception {
    MvcResult result = mvc.perform(get("/password").session(session)).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    return (String) result.getModelAndView().getModel().get(TransactionTokenManager.PARAM);
  }

  private MvcResult submit(
      MockHttpSession session, String token, String current, String newPw, String confirm)
      throws Exception {
    return mvc.perform(
            post("/password")
                .session(session)
                .with(csrf())
                .param(TransactionTokenManager.PARAM, token)
                .param("currentPassword", current)
                .param("newPassword", newPw)
                .param("confirmPassword", confirm))
        .andReturn();
  }

  @Test
  void 初期パスワードを変更するとメニューを利用できるようになり操作ログが記録される() throws Exception {
    long userId = TestUsers.insert(jdbc, "newuser", true, false, 0, true, Role.SALES);
    MockHttpSession session = login("newuser");
    mvc.perform(get("/menu").session(session)).andExpect(redirectedUrl("/password"));

    MvcResult result =
        submit(session, openForm(session), TestUsers.PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/menu");
    mvc.perform(get("/menu").session(session)).andExpect(status().isOk());
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM m_user WHERE user_id = ?", String.class, userId);
    assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT password_change_required FROM m_user WHERE user_id = ?",
                Boolean.class,
                userId))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT detail IS NULL AND target_key = 'newuser' FROM t_operation_log"
                    + " WHERE operation_type = 'UPDATE' AND screen_id = 'SCR-003'",
                Boolean.class))
        .isTrue();
  }

  @Test
  void 現在のパスワードが誤っているとE014で変更されない() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    MockHttpSession session = login("yamamoto");

    MvcResult result = submit(session, openForm(session), "Wrong999", NEW_PASSWORD, NEW_PASSWORD);

    assertThat(result.getResponse().getContentAsString()).contains("現在のパスワードが正しくありません。");
    assertThat(operationLogCount("UPDATE")).isZero();
  }

  @Test
  void 確認用が一致しない場合と条件を満たさない場合は項目の下にエラーを表示する() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    MockHttpSession session = login("yamamoto");

    String mismatch =
        submit(session, openForm(session), TestUsers.PASSWORD, NEW_PASSWORD, "Other999")
            .getResponse()
            .getContentAsString();
    assertThat(mismatch).contains("新しいパスワードと確認用のパスワードが一致しません。");

    String weak =
        submit(session, openForm(session), TestUsers.PASSWORD, "abcdefgh", "abcdefgh")
            .getResponse()
            .getContentAsString();
    assertThat(weak).contains("パスワードは8文字以上で、英字と数字を両方含めてください。");

    String blank =
        submit(session, openForm(session), "", "", "").getResponse().getContentAsString();
    assertThat(blank).contains("現在のパスワードを入力してください。", "新しいパスワード（確認用）を入力してください。");
    assertThat(operationLogCount("UPDATE")).isZero();
  }

  @Test
  void 同じトークンで2回送信すると2回目は処理しない() throws Exception {
    TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    MockHttpSession session = login("yamamoto");
    String token = openForm(session);

    submit(session, token, TestUsers.PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
    MvcResult second = submit(session, token, NEW_PASSWORD, "Another99", "Another99");

    assertThat(second.getResponse().getRedirectedUrl()).isEqualTo("/menu");
    assertThat(operationLogCount("UPDATE")).isEqualTo(1);
  }

  private long operationLogCount(String type) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM t_operation_log WHERE operation_type = ?", Long.class, type);
  }
}
