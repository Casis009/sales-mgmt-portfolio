package jp.co.hikari.hanbai.common.operationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

/** 操作ログの記録（CM-10）と検索 SQL を、実際の DB で確認する。 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, OperationLogIT.SampleConfig.class})
class OperationLogIT {

  private static final String SCREEN = "SCR-T01";

  @Autowired SampleService sampleService;
  @Autowired OperationLogService operationLogService;
  @Autowired JdbcTemplate jdbc;

  private final LoginUser user =
      new LoginUser(7L, "yamamoto", "山本", null, List.of(Role.WAREHOUSE), true, false, false, null);

  @BeforeEach
  void clean() {
    // テスト用コンテナの DB ユーザーは所有者のため DELETE できる（本番のアプリ用ユーザーは不可）
    jdbc.update("DELETE FROM t_operation_log");
  }

  @Test
  void 正常終了すると同じトランザクションで操作種別と画面IDと対象キーが記録される() {
    sampleService.register(user, false);

    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM t_operation_log");
    assertThat(row)
        .containsEntry("operation_type", "CREATE")
        .containsEntry("screen_id", SCREEN)
        .containsEntry("target_type", "t_sample")
        .containsEntry("target_key", "SMP-0001")
        .containsEntry("user_id", 7L)
        .containsEntry("login_id", "yamamoto")
        .containsEntry("result", "1");
  }

  @Test
  void 業務エラーで終了すると記録されない() {
    assertThatThrownBy(() -> sampleService.register(user, true))
        .isInstanceOf(BusinessException.class);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_operation_log", Long.class)).isZero();
  }

  @Test
  void 戻り値がOperationLogTargetなら操作内容も記録される() {
    sampleService.update(user);
    assertThat(jdbc.queryForMap("SELECT target_key, detail FROM t_operation_log"))
        .containsEntry("target_key", "SMP-0002")
        .containsEntry("detail", "数量: 1→2");
  }

  @Test
  void トランザクションの外からの記録はエラーになる() {
    assertThatThrownBy(() -> operationLogService.record(log(OperationType.CREATE)))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void 桁数を超える値は切り詰めて記録する() {
    OperationLog log = log(OperationType.LOGIN_FAILURE);
    log.setLoginId("x".repeat(100));
    operationLogService.recordIndependently(log);
    assertThat(jdbc.queryForObject("SELECT login_id FROM t_operation_log", String.class))
        .hasSize(20);
  }

  @Test
  void 期間とログインIDと操作種別と対象で検索し操作日時の降順で返す() {
    insert("2027-01-18T09:00", "tanaka", OperationType.LOGIN, "m_user", "tanaka");
    insert("2027-01-18T10:00", "yamamoto", OperationType.CREATE, "t_shipment", "SH27010001");
    insert("2027-01-18T11:00", "yamamoto", OperationType.CREATE, "t_shipment", "SH27010002");
    insert("2027-01-19T00:00", "yamamoto", OperationType.CREATE, "t_shipment", "SH27010003");

    OperationLogCriteria criteria =
        new OperationLogCriteria(
            LocalDateTime.parse("2027-01-18T00:00"),
            LocalDateTime.parse("2027-01-19T00:00"),
            "yamamoto",
            OperationType.CREATE,
            "SH2701");

    assertThat(operationLogService.count(criteria)).isEqualTo(2);
    assertThat(operationLogService.search(criteria, 50, 0))
        .extracting(OperationLog::getTargetKey)
        .containsExactly("SH27010002", "SH27010001");
    assertThat(operationLogService.search(criteria, 1, 1))
        .extracting(OperationLog::getTargetKey)
        .containsExactly("SH27010001");
    assertThat(operationLogService.search(criteria, 50, 0).get(0))
        .extracting(OperationLog::getOperationType, OperationLog::getResult)
        .containsExactly(OperationType.CREATE, OperationResult.SUCCESS);
  }

  @Test
  void 条件なしは全件でLIKEの特殊文字はそのままの文字として検索する() {
    insert("2027-01-18T09:00", "a", OperationType.LOGIN, "m_user", "100%_off");
    insert("2027-01-18T09:01", "b", OperationType.LOGIN, "m_user", "100abc");

    assertThat(operationLogService.count(new OperationLogCriteria(null, null, null, null, null)))
        .isEqualTo(2);
    assertThat(
            operationLogService.search(
                new OperationLogCriteria(null, null, null, null, "%_"), 50, 0))
        .extracting(OperationLog::getTargetKey)
        .containsExactly("100%_off");
  }

  private void insert(
      String at, String loginId, OperationType type, String targetType, String key) {
    OperationLog log = log(type);
    log.setOperatedAt(LocalDateTime.parse(at));
    log.setLoginId(loginId);
    log.setTargetType(targetType);
    log.setTargetKey(key);
    operationLogService.recordIndependently(log);
  }

  private static OperationLog log(OperationType type) {
    return OperationLog.of(type, OperationResult.SUCCESS);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class SampleConfig {
    @Bean
    SampleService sampleService() {
      return new SampleService();
    }
  }

  /** 操作ログを記録する業務 Service の見本。 */
  static class SampleService {

    @OperationLogged(type = OperationType.CREATE, screenId = SCREEN, targetType = "t_sample")
    @Transactional
    public String register(LoginUser user, boolean fail) {
      if (fail) {
        throw new BusinessException("E-020");
      }
      return "SMP-0001";
    }

    @OperationLogged(type = OperationType.UPDATE, screenId = SCREEN, targetType = "t_sample")
    @Transactional
    public OperationLogTarget update(LoginUser user) {
      return new OperationLogTarget() {
        @Override
        public String targetKey() {
          return "SMP-0002";
        }

        @Override
        public String detail() {
          return "数量: 1→2";
        }
      };
    }
  }
}
