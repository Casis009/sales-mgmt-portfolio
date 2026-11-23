package jp.co.hikari.hanbai.common.web;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * 二重送信防止のトークン（共通処理設計 10章 ③）。
 *
 * <p>入力画面の表示時にトークンを発行してセッションに保持し、登録時に照合・破棄する。一致しない場合（同じトークンでの2回目の送信、
 * 古い画面からの送信）は処理しない。トークンは入力画面の種類（フォームキー）ごとに1つ保持する。
 */
@Component
public class TransactionTokenManager {

  /** 画面の hidden 項目・モデル属性の名前。 */
  public static final String PARAM = "_txToken";

  private static final String SESSION_PREFIX = TransactionTokenManager.class.getName() + ".";

  /**
   * トークンを発行し、セッションに保持する（同じフォームキーの古いトークンは無効になる）。
   *
   * @param session セッション
   * @param formKey 入力画面の種類（例：SCR-012）
   * @return トークン
   */
  public String issue(HttpSession session, String formKey) {
    String token = UUID.randomUUID().toString();
    session.setAttribute(SESSION_PREFIX + formKey, token);
    return token;
  }

  /**
   * トークンを照合し、一致した場合は破棄する。
   *
   * @param session セッション
   * @param formKey 入力画面の種類
   * @param token 画面から送信されたトークン
   * @return 一致した場合 true（処理を続けてよい）
   */
  public boolean consume(HttpSession session, String formKey, String token) {
    // 同じセッションの同時リクエスト（ダブルクリック）で、両方が一致と判定されないように直列化する
    synchronized (WebUtils.getSessionMutex(session)) {
      Object expected = session.getAttribute(SESSION_PREFIX + formKey);
      if (token == null || !token.equals(expected)) {
        return false;
      }
      session.removeAttribute(SESSION_PREFIX + formKey);
      return true;
    }
  }
}
