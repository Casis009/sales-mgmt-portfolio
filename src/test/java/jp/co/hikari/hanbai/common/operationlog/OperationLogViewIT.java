package jp.co.hikari.hanbai.common.operationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDateTime;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 操作ログ照会（F-005、SCR-013）。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OperationLogViewIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired OperationLogService operationLogService;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM t_operation_log");
    insert("2027-01-18T09:00:10", "tanaka", OperationType.LOGIN, OperationResult.SUCCESS, "tanaka");
    insert(
        "2027-01-18T09:30:59",
        "yamamoto",
        OperationType.LOGIN_FAILURE,
        OperationResult.FAILURE,
        "yamamoto");
    insert("2027-01-19T10:00:00", "yamamoto", OperationType.CREATE, OperationResult.SUCCESS, "SH1");
  }

  private void insert(
      String at, String loginId, OperationType type, OperationResult result, String key) {
    OperationLog log = OperationLog.of(type, result);
    log.setOperatedAt(LocalDateTime.parse(at));
    log.setLoginId(loginId);
    log.setTargetType("m_user");
    log.setTargetKey(key);
    operationLogService.recordIndependently(log);
  }

  private String page(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(user(TestUsers.of(Role.ADMIN))))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void 初期表示では検索しない() throws Exception {
    assertThat(page(get("/admin/operation-logs"))).doesNotContain("<table").contains("操作種別");
  }

  @Test
  void 期間とユーザーと操作種別で検索し終了日時はその分を含む() throws Exception {
    String result =
        page(
            get("/admin/operation-logs")
                .param("search", "1")
                .param("operatedFrom", "2027-01-18T09:00")
                .param("operatedTo", "2027-01-18T09:30")
                .param("loginId", "yamamoto"));
    assertThat(result).contains("2027/01/18 09:30:59", "ログイン失敗", "失敗").doesNotContain("SH1");

    String byType =
        page(get("/admin/operation-logs").param("search", "1").param("operationType", "CREATE"));
    assertThat(byType).contains("SH1").doesNotContain("tanaka");
  }

  @Test
  void 終了日時が開始日時より前の場合はE007() throws Exception {
    String result =
        page(
            get("/admin/operation-logs")
                .param("search", "1")
                .param("operatedFrom", "2027-01-19T00:00")
                .param("operatedTo", "2027-01-18T00:00"));
    assertThat(result).contains("操作日時（終了）は操作日時（開始）以降の日付を入力してください。");
  }

  @Test
  void 該当なしはI010() throws Exception {
    assertThat(page(get("/admin/operation-logs").param("search", "1").param("loginId", "x")))
        .contains("該当するデータはありません。");
  }
}
