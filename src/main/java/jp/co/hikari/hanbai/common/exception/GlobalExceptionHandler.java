package jp.co.hikari.hanbai.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Controller で処理されなかった例外を、共通のエラー画面に振り分ける（共通処理設計 5章）。
 *
 * <p>業務エラー（{@link BusinessException}）は、入力内容を保持したまま元の画面に戻すため各 Controller で捕捉する。
 * ここに到達した例外はすべて、利用者には受付番号付きのシステムエラー（E-090）として表示する。 エラー画面には例外の内容・スタックトレースを表示しない（NF-32）。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  /** エラー画面のモデル属性：受付番号。 */
  public static final String RECEIPT_NUMBER = "receiptNumber";

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ReceiptNumberGenerator receiptNumberGenerator;

  /**
   * 受付番号の発行部品を受け取る。
   *
   * @param receiptNumberGenerator 受付番号の発行
   */
  public GlobalExceptionHandler(ReceiptNumberGenerator receiptNumberGenerator) {
    this.receiptNumberGenerator = receiptNumberGenerator;
  }

  /**
   * 権限エラー（E-012）。Service の {@code @PreAuthorize} で拒否された場合などに到達する。
   *
   * @param request リクエスト
   * @return 403 画面
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ModelAndView handleAccessDenied(HttpServletRequest request) {
    Principal user = request.getUserPrincipal();
    LOG.warn(
        "権限エラー: user={}, url={}",
        user == null ? "-" : user.getName(),
        request.getRequestURI());
    return view("error/403", HttpStatus.FORBIDDEN);
  }

  /**
   * 存在しないページ（E-091）。
   *
   * @return 404 画面
   */
  @ExceptionHandler({NoResourceFoundException.class, NotFoundException.class})
  public ModelAndView handleNotFound() {
    return view("error/404", HttpStatus.NOT_FOUND);
  }

  /**
   * 想定外の例外（E-090）。受付番号を発行し、スタックトレースとともに ERROR ログに出力する。
   *
   * @param e 例外
   * @param request リクエスト
   * @return 500 画面
   */
  @ExceptionHandler(Exception.class)
  public ModelAndView handleSystemError(Exception e, HttpServletRequest request) {
    String receiptNumber = receiptNumberGenerator.next();
    LOG.error("システムエラー: 受付番号={}, url={}", receiptNumber, request.getRequestURI(), e);
    ModelAndView mav = view("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    mav.addObject(RECEIPT_NUMBER, receiptNumber);
    return mav;
  }

  private static ModelAndView view(String name, HttpStatus status) {
    ModelAndView mav = new ModelAndView(name);
    mav.setStatus(status);
    return mav;
  }
}
