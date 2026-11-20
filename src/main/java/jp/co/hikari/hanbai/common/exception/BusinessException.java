package jp.co.hikari.hanbai.common.exception;

import java.io.Serial;
import java.util.List;

/**
 * 業務エラー（共通処理設計 5章）。メッセージ一覧のメッセージIDと埋め込み値を持つ。
 *
 * <p>画面への振り分け（GlobalExceptionHandler）は製造 2段階で実装する。
 */
public class BusinessException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String messageId;
  private final transient List<Object> args;

  /**
   * 業務エラーを生成する。
   *
   * @param messageId メッセージID（例：E-052）
   * @param args メッセージの埋め込み値
   */
  public BusinessException(String messageId, Object... args) {
    super(messageId);
    this.messageId = messageId;
    this.args = List.of(args);
  }

  /**
   * メッセージIDを返す。
   *
   * @return メッセージID
   */
  public String getMessageId() {
    return messageId;
  }

  /**
   * メッセージの埋め込み値を返す。
   *
   * @return 埋め込み値（変更不可）
   */
  public List<Object> getArgs() {
    return args;
  }
}
