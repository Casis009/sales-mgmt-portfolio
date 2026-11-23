package jp.co.hikari.hanbai.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * トランザクションの設定（共通処理設計 1章）。
 *
 * <p>操作ログの AOP（{@code OperationLogAspect}、最低優先）を業務処理と同じトランザクションの中で実行するため、
 * トランザクションの順序をそれより1つ外側に明示する。既定ではどちらも最低優先となり、実行順序が保証されない。
 * タイムアウト（30秒）は application.yml の {@code spring.transaction.default-timeout} で設定する。
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true, order = TransactionConfig.ORDER)
public class TransactionConfig {

  /** トランザクションの AOP の順序（操作ログの AOP より外側）。 */
  public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;
}
