package jp.co.hikari.hanbai.common.exception;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * システムエラーの受付番号を発行する（共通処理設計 5.2）。
 *
 * <p>形式は「日時（yyyyMMddHHmmss）＋ランダム4桁」。エラー画面とエラーログの両方に出力し、問合せ時にログを特定する。
 */
@Component
public class ReceiptNumberGenerator {

  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final int RANDOM_BOUND = 10_000;

  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  /**
   * 現在日時の取得元を受け取る。
   *
   * @param clock 時計
   */
  public ReceiptNumberGenerator(Clock clock) {
    this.clock = clock;
  }

  /**
   * 受付番号を発行する。
   *
   * @return 例：202701181030150042（18桁）
   */
  public String next() {
    return LocalDateTime.now(clock).format(FORMAT)
        + String.format("%04d", random.nextInt(RANDOM_BOUND));
  }
}
