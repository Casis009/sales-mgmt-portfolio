package jp.co.hikari.hanbai.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 現在日時の取得元（共通処理設計 10章）。
 *
 * <p>業務処理は {@code LocalDate.now(clock)} で日付を取得する。テストでは固定の {@link Clock} に差し替えて、
 * 締日・月替わりなど日付に依存する処理を検証する。
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  /** 業務で使用するタイムゾーン。 */
  public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

  /**
   * システム時計（Asia/Tokyo）を返す。
   *
   * @return 時計
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZONE);
  }
}
