package jp.co.hikari.hanbai.common.operationlog;

import jakarta.servlet.http.HttpServletRequest;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link OperationLogged} の付いたメソッドが正常終了したときに、操作ログを登録する（F-004）。
 *
 * <p>トランザクションの内側で実行するため、順序を最低優先（{@code TransactionConfig} のトランザクションより内側）とする。
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OperationLogAspect {

  private final OperationLogService operationLogService;

  /**
   * 操作ログの登録部品を受け取る。
   *
   * @param operationLogService 操作ログ
   */
  public OperationLogAspect(OperationLogService operationLogService) {
    this.operationLogService = operationLogService;
  }

  /**
   * メソッドの正常終了後に操作ログを登録する。
   *
   * @param joinPoint 実行したメソッド
   * @param logged アノテーション
   * @param returned 戻り値
   */
  @AfterReturning(pointcut = "@annotation(logged)", returning = "returned")
  public void afterReturning(JoinPoint joinPoint, OperationLogged logged, Object returned) {
    OperationLog log = OperationLog.of(logged.type(), OperationResult.SUCCESS);
    log.setScreenId(logged.screenId());
    log.setTargetType(logged.targetType());
    if (returned instanceof OperationLogTarget target) {
      log.setTargetKey(target.targetKey());
      log.setDetail(target.detail());
    } else if (returned != null) {
      log.setTargetKey(returned.toString());
    }
    LoginUser user = findUser(joinPoint.getArgs());
    if (user != null) {
      log.setUserId(user.getUserId());
      log.setLoginId(user.getLoginId());
    }
    log.setIpAddress(currentIpAddress());
    operationLogService.record(log);
  }

  private static LoginUser findUser(Object[] args) {
    for (Object arg : args) {
      if (arg instanceof LoginUser user) {
        return user;
      }
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof LoginUser user ? user : null;
  }

  private static String currentIpAddress() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      HttpServletRequest request = attrs.getRequest();
      return request.getRemoteAddr();
    }
    return null;
  }
}
