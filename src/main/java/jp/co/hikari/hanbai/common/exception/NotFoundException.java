package jp.co.hikari.hanbai.common.exception;

import java.io.Serial;

/** URL で指定されたデータが存在しない（E-091、HTTP 404）。{@link GlobalExceptionHandler} が 404 画面に振り分ける。 */
public class NotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * 存在しないデータを示す例外を生成する。
   *
   * @param target 対象（ログ用。例：m_user#99）
   */
  public NotFoundException(String target) {
    super(target);
  }
}
