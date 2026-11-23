package jp.co.hikari.hanbai.common.operationlog;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作ログの登録・検索（F-004、F-005。共通処理設計 7章）。
 *
 * <p>業務操作の記録は業務処理と同じトランザクションで行い、業務処理がロールバックされた場合は記録も残らない。
 * 失敗の記録（ログイン失敗など）だけは別のトランザクションで登録する（共通処理設計 1章 No.6）。
 */
@Service
public class OperationLogService {

  private static final int LOGIN_ID_LENGTH = 20;
  private static final int IP_ADDRESS_LENGTH = 45;
  private static final int SCREEN_ID_LENGTH = 10;
  private static final int TARGET_TYPE_LENGTH = 30;
  private static final int TARGET_KEY_LENGTH = 50;

  private final OperationLogMapper mapper;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 操作ログの SQL
   * @param clock 時計
   */
  public OperationLogService(OperationLogMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * 呼び出し元のトランザクションで操作ログを登録する。
   *
   * <p>トランザクションの外から呼ばれた場合は例外とする（業務処理と記録の一体性を保証するため）。
   *
   * @param log 操作ログ（操作日時が未設定の場合は現在日時）
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(OperationLog log) {
    insert(log);
  }

  /**
   * 独立したトランザクションで操作ログを登録する。ログイン失敗・バッチなど、呼び出し元の結果によらず残す記録に使う。
   *
   * @param log 操作ログ
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordIndependently(OperationLog log) {
    insert(log);
  }

  /**
   * 操作ログの件数を返す（SCR-013）。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  @Transactional(readOnly = true)
  public long count(OperationLogCriteria criteria) {
    return mapper.count(criteria);
  }

  /**
   * 操作ログを検索する（SCR-013）。照会は操作ログに記録しない（共通処理設計 7.2）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 操作日時の降順
   */
  @Transactional(readOnly = true)
  public List<OperationLog> search(OperationLogCriteria criteria, int limit, long offset) {
    return mapper.search(criteria, limit, offset);
  }

  private void insert(OperationLog log) {
    if (log.getOperatedAt() == null) {
      log.setOperatedAt(LocalDateTime.now(clock));
    }
    // ログイン失敗時の入力値など、桁数を超える値で登録に失敗しないよう切り詰める
    log.setLoginId(truncate(log.getLoginId(), LOGIN_ID_LENGTH));
    log.setIpAddress(truncate(log.getIpAddress(), IP_ADDRESS_LENGTH));
    log.setScreenId(truncate(log.getScreenId(), SCREEN_ID_LENGTH));
    log.setTargetType(truncate(log.getTargetType(), TARGET_TYPE_LENGTH));
    log.setTargetKey(truncate(log.getTargetKey(), TARGET_KEY_LENGTH));
    mapper.insert(log);
  }

  private static String truncate(String value, int length) {
    return value == null || value.length() <= length ? value : value.substring(0, length);
  }
}
