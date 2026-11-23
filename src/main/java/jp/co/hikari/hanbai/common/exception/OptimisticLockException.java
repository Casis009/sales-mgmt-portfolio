package jp.co.hikari.hanbai.common.exception;

import java.io.Serial;
import jp.co.hikari.hanbai.common.message.MessageId;

/**
 * 楽観的排他エラー（共通処理設計 2.2：更新件数が 0 の場合に送出する。E-020）。
 *
 * <p>業務エラーの一種として、Controller で捕捉して元の画面に表示する。
 */
public class OptimisticLockException extends BusinessException {

  @Serial private static final long serialVersionUID = 1L;

  /** E-020 の業務エラーを生成する。 */
  public OptimisticLockException() {
    super(MessageId.E_OPTIMISTIC_LOCK);
  }
}
