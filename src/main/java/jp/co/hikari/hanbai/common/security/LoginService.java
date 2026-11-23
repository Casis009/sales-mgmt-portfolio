package jp.co.hikari.hanbai.common.security;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.operationlog.OperationLog;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ログイン・ログアウトの記録（F-001。セキュリティ設計 1章「認証処理の流れ」）。
 *
 * <p>認証失敗のたびに失敗回数を1加算し、{@value #MAX_FAILED_LOGIN_COUNT} 回に達したらロックする（NF-25）。
 * 認証成功で 0 に戻す。
 */
@Service
public class LoginService {

  /** アカウントをロックする認証失敗回数（NF-25）。 */
  public static final int MAX_FAILED_LOGIN_COUNT = 5;

  private static final String SCREEN_ID = "SCR-001";
  private static final String TARGET_TYPE = "m_user";

  private final LoginUserMapper mapper;
  private final OperationLogService operationLogService;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 認証用の SQL
   * @param operationLogService 操作ログ
   * @param clock 時計
   */
  public LoginService(
      LoginUserMapper mapper, OperationLogService operationLogService, Clock clock) {
    this.mapper = mapper;
    this.operationLogService = operationLogService;
    this.clock = clock;
  }

  /**
   * 認証成功を記録する。失敗回数を 0 に戻し、最終ログイン日時を更新し、操作ログ（LOGIN）を登録する。
   *
   * @param user ログインユーザー
   * @param ipAddress 接続元IPアドレス
   */
  @Transactional
  public void recordSuccess(LoginUser user, String ipAddress) {
    mapper.recordSuccess(user.getUserId(), LocalDateTime.now(clock));
    operationLogService.record(userLog(OperationType.LOGIN, user, ipAddress));
  }

  /**
   * パスワード不一致を記録する。ユーザーが存在すれば失敗回数を加算し、上限に達したらロックする。
   *
   * <p>ログイン画面の処理がどうなっても記録を残すため、独立したトランザクションで実行する（共通処理設計 1章 No.6）。
   *
   * @param loginId 入力されたログインID
   * @param ipAddress 接続元IPアドレス
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordBadCredentials(String loginId, String ipAddress) {
    String trimmed = loginId == null ? "" : loginId.strip();
    if (trimmed.isEmpty()) {
      // 未入力のまま送信された場合は記録しない（入力チェックのエラーであり、認証の試行ではないため）
      return;
    }
    OperationLog log = failureLog(trimmed, ipAddress, "パスワード不一致");
    Optional<LoginUserRow> found = mapper.findByLoginIdForUpdate(trimmed);
    if (found.isPresent()) {
      LoginUserRow user = found.get();
      int count = user.getFailedLoginCount() + 1;
      boolean lockNow = !user.isLocked() && count >= MAX_FAILED_LOGIN_COUNT;
      mapper.recordFailure(user.getUserId(), count, user.isLocked() || lockNow);
      log.setUserId(user.getUserId());
      log.setDetail("パスワード不一致（失敗回数：" + count + "）" + (lockNow ? "、ロック" : ""));
    }
    operationLogService.record(log);
  }

  /**
   * ロック中・無効ユーザーのログインを記録する（失敗回数は加算しない）。
   *
   * @param loginId 入力されたログインID
   * @param ipAddress 接続元IPアドレス
   * @param reason 理由（例：ロック中）
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRejected(String loginId, String ipAddress, String reason) {
    String trimmed = loginId == null ? "" : loginId.strip();
    OperationLog log = failureLog(trimmed, ipAddress, reason);
    mapper.findByLoginId(trimmed).ifPresent(u -> log.setUserId(u.getUserId()));
    operationLogService.record(log);
  }

  /**
   * ログアウトを記録する（操作ログ LOGOUT）。
   *
   * @param user ログインユーザー
   * @param ipAddress 接続元IPアドレス
   */
  @Transactional
  public void recordLogout(LoginUser user, String ipAddress) {
    operationLogService.record(userLog(OperationType.LOGOUT, user, ipAddress));
  }

  private static OperationLog userLog(OperationType type, LoginUser user, String ipAddress) {
    OperationLog log = OperationLog.of(type, OperationResult.SUCCESS);
    log.setScreenId(SCREEN_ID);
    log.setTargetType(TARGET_TYPE);
    log.setTargetKey(user.getLoginId());
    log.setUserId(user.getUserId());
    log.setLoginId(user.getLoginId());
    log.setIpAddress(ipAddress);
    return log;
  }

  private static OperationLog failureLog(String loginId, String ipAddress, String detail) {
    OperationLog log = OperationLog.of(OperationType.LOGIN_FAILURE, OperationResult.FAILURE);
    log.setScreenId(SCREEN_ID);
    log.setTargetType(TARGET_TYPE);
    log.setTargetKey(loginId);
    log.setLoginId(loginId);
    log.setIpAddress(ipAddress);
    log.setDetail(detail);
    return log;
  }
}
