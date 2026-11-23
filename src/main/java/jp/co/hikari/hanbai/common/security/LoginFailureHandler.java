package jp.co.hikari.hanbai.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.message.UiMessage;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * 認証失敗時に、ログイン画面へメッセージを渡してリダイレクトする（SCR-001 イベント 3・4）。
 *
 * <ul>
 *   <li>ログインID・パスワードが未入力：E-001（項目ごと）
 *   <li>ロック中：E-011
 *   <li>上記以外（ID・パスワードの誤り、無効ユーザー）：E-010
 * </ul>
 *
 * <p>入力したログインIDは再表示し、パスワードは再表示しない。
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

  /** ログイン画面のモデル属性・リクエストパラメータ：ログインID。 */
  public static final String LOGIN_ID = "loginId";

  /** リクエストパラメータ：パスワード。 */
  public static final String PASSWORD = "password";

  /** ログイン画面の URL。 */
  public static final String LOGIN_URL = "/login";

  private final MessageHelper messageHelper;
  private final SessionFlashMapManager flashMapManager = new SessionFlashMapManager();

  /**
   * 依存部品を受け取る。
   *
   * @param messageHelper メッセージ
   */
  public LoginFailureHandler(MessageHelper messageHelper) {
    this.messageHelper = messageHelper;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String loginId = request.getParameter(LOGIN_ID);
    List<UiMessage> messages = new ArrayList<>();
    if (isBlank(loginId)) {
      messages.add(error(MessageId.E_REQUIRED, MessageHelper.label(LOGIN_ID)));
    }
    if (isBlank(request.getParameter(PASSWORD))) {
      messages.add(error(MessageId.E_REQUIRED, MessageHelper.label(PASSWORD)));
    }
    if (messages.isEmpty()) {
      messages.add(
          error(
              exception instanceof LockedException
                  ? MessageId.E_LOCKED
                  : MessageId.E_BAD_CREDENTIALS));
    }

    FlashMap flash = new FlashMap();
    flash.put(UiMessage.ATTRIBUTE, messages);
    flash.put(LOGIN_ID, loginId == null ? "" : loginId.strip());
    flash.setTargetRequestPath(request.getContextPath() + LOGIN_URL);
    flashMapManager.saveOutputFlashMap(flash, request, response);
    response.sendRedirect(request.getContextPath() + LOGIN_URL);
  }

  private UiMessage error(String messageId, Object... args) {
    return new UiMessage(UiMessage.Level.ERROR, messageHelper.get(messageId, args));
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
