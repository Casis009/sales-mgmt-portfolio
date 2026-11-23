package jp.co.hikari.hanbai.common.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * メッセージIDから文言を取得し、画面のメッセージ表示欄に設定する。
 *
 * <p>埋め込み値に {@link #label(String)} を渡すと、その値もメッセージとして解決する（例：「{0}を登録しました。」の {0}
 * に「ユーザー」）。プログラムに文言を書かないためである。
 */
@Component
public class MessageHelper {

  private static final Locale LOCALE = Locale.JAPANESE;

  private final MessageSource messageSource;

  /**
   * メッセージの取得元を受け取る。
   *
   * @param messageSource messages.properties
   */
  public MessageHelper(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * 埋め込み値として使う表示名（messages.properties のキー）を返す。
   *
   * @param key 例：label.user
   * @return 埋め込み値
   */
  public static DefaultMessageSourceResolvable label(String key) {
    return new DefaultMessageSourceResolvable(new String[] {key}, key);
  }

  /**
   * メッセージを取得する。
   *
   * @param messageId メッセージID
   * @param args 埋め込み値
   * @return 文言
   */
  public String get(String messageId, Object... args) {
    return messageSource.getMessage(messageId, args, LOCALE);
  }

  /**
   * リダイレクト先の画面に情報メッセージを表示する（PRG パターンの完了メッセージ）。
   *
   * @param redirect リダイレクト属性
   * @param messageId メッセージID
   * @param args 埋め込み値
   */
  public void flashInfo(RedirectAttributes redirect, String messageId, Object... args) {
    redirect.addFlashAttribute(
        UiMessage.ATTRIBUTE, List.of(new UiMessage(UiMessage.Level.INFO, get(messageId, args))));
  }

  /**
   * 表示する画面にメッセージを追加する。
   *
   * @param model モデル
   * @param level 種別
   * @param messageId メッセージID
   * @param args 埋め込み値
   */
  public void add(Model model, UiMessage.Level level, String messageId, Object... args) {
    List<UiMessage> messages = new ArrayList<>();
    if (model.getAttribute(UiMessage.ATTRIBUTE) instanceof List<?> current) {
      for (Object m : current) {
        if (m instanceof UiMessage um) {
          messages.add(um);
        }
      }
    }
    messages.add(new UiMessage(level, get(messageId, args)));
    model.addAttribute(UiMessage.ATTRIBUTE, messages);
  }

  /**
   * 業務エラーを表示する画面にエラーメッセージとして追加する。
   *
   * @param model モデル
   * @param e 業務エラー
   */
  public void addError(Model model, BusinessException e) {
    add(model, UiMessage.Level.ERROR, e.getMessageId(), e.getArgs().toArray());
  }
}
