package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.operationlog.OperationLog;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

  private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

  @Mock LoginUserMapper mapper;
  @Mock OperationLogService operationLogService;

  private final Clock clock = Clock.fixed(Instant.parse("2027-01-18T00:00:00Z"), TOKYO);

  private LoginService service() {
    return new LoginService(mapper, operationLogService, clock);
  }

  private static LoginUserRow row(int failedCount, boolean locked) {
    LoginUserRow row = new LoginUserRow();
    row.setUserId(10L);
    row.setLoginId("yamamoto");
    row.setFailedLoginCount(failedCount);
    row.setLocked(locked);
    return row;
  }

  private OperationLog capturedLog() {
    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(operationLogService).record(captor.capture());
    return captor.getValue();
  }

  @Test
  void 認証成功で失敗回数を0に戻し最終ログイン日時と操作ログを記録する() {
    service().recordSuccess(TestUsers.of(Role.SALES), "10.0.0.1");

    verify(mapper).recordSuccess(1L, LocalDateTime.of(2027, 1, 18, 9, 0));
    OperationLog log = capturedLog();
    assertThat(log.getOperationType()).isEqualTo(OperationType.LOGIN);
    assertThat(log.getResult()).isEqualTo(OperationResult.SUCCESS);
    assertThat(log.getIpAddress()).isEqualTo("10.0.0.1");
    assertThat(log.getScreenId()).isEqualTo("SCR-001");
  }

  @Test
  void 失敗回数3回の後の失敗は4回目でロックしない() {
    when(mapper.findByLoginIdForUpdate("yamamoto")).thenReturn(Optional.of(row(3, false)));
    service().recordBadCredentials("yamamoto", null);
    verify(mapper).recordFailure(10L, 4, false);
    assertThat(capturedLog().getDetail()).isEqualTo("パスワード不一致（失敗回数：4）");
  }

  @Test
  void 失敗回数4回の後の失敗は5回目でロックする() {
    when(mapper.findByLoginIdForUpdate("yamamoto")).thenReturn(Optional.of(row(4, false)));
    service().recordBadCredentials(" yamamoto ", null);
    verify(mapper).recordFailure(10L, 5, true);
    OperationLog log = capturedLog();
    assertThat(log.getDetail()).isEqualTo("パスワード不一致（失敗回数：5）、ロック");
    assertThat(log.getUserId()).isEqualTo(10L);
    assertThat(log.getResult()).isEqualTo(OperationResult.FAILURE);
  }

  @Test
  void 存在しないログインIDは失敗回数を更新せず操作ログのみ記録する() {
    when(mapper.findByLoginIdForUpdate("nobody")).thenReturn(Optional.empty());
    service().recordBadCredentials("nobody", null);
    verify(mapper, never()).recordFailure(anyLong(), anyInt(), anyBoolean());
    OperationLog log = capturedLog();
    assertThat(log.getUserId()).isNull();
    assertThat(log.getLoginId()).isEqualTo("nobody");
  }

  @Test
  void 未入力のログインIDは記録しない() {
    service().recordBadCredentials("  ", null);
    service().recordBadCredentials(null, null);
    verifyNoInteractions(mapper, operationLogService);
  }

  @Test
  void ロック中のログインは失敗回数を加算せず理由を記録する() {
    when(mapper.findByLoginId("yamamoto")).thenReturn(Optional.of(row(5, true)));
    service().recordRejected("yamamoto", null, "ロック中");
    verify(mapper, never()).recordFailure(anyLong(), anyInt(), anyBoolean());
    OperationLog log = capturedLog();
    assertThat(log.getDetail()).isEqualTo("ロック中");
    assertThat(log.getUserId()).isEqualTo(10L);
  }

  @Test
  void ログアウトを記録する() {
    service().recordLogout(TestUsers.of(Role.ADMIN), "10.0.0.2");
    assertThat(capturedLog().getOperationType()).isEqualTo(OperationType.LOGOUT);
    verify(mapper, never()).recordSuccess(anyLong(), any());
  }
}
