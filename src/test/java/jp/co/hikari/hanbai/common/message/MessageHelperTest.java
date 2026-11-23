package jp.co.hikari.hanbai.common.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class MessageHelperTest {

  private final MessageHelper helper = new MessageHelper(messageSource());

  private static ResourceBundleMessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("messages");
    source.setDefaultEncoding("UTF-8");
    return source;
  }

  @Test
  void 埋め込み値の表示名もメッセージから解決する() {
    assertThat(helper.get(MessageId.I_CREATED, MessageHelper.label("label.user")))
        .isEqualTo("ユーザーを登録しました。");
  }

  @Test
  void 完了メッセージをリダイレクト先に渡す() {
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    helper.flashInfo(redirect, MessageId.I_PASSWORD_CHANGED);
    assertThat(redirect.getFlashAttributes().get(UiMessage.ATTRIBUTE))
        .isEqualTo(List.of(new UiMessage(UiMessage.Level.INFO, "パスワードを変更しました。")));
  }

  @Test
  void 業務エラーを既存のメッセージに追加する() {
    ExtendedModelMap model = new ExtendedModelMap();
    helper.add(model, UiMessage.Level.WARN, MessageId.W_TOO_MANY_RESULTS, 5000);
    helper.addError(model, new BusinessException(MessageId.E_OPTIMISTIC_LOCK));

    @SuppressWarnings("unchecked")
    List<UiMessage> messages = (List<UiMessage>) model.getAttribute(UiMessage.ATTRIBUTE);
    assertThat(messages)
        .extracting(UiMessage::level)
        .containsExactly(UiMessage.Level.WARN, UiMessage.Level.ERROR);
    assertThat(messages.get(0).text()).isEqualTo("検索結果が5,000件を超えています。条件を絞り込んでください。");
    assertThat(messages.get(1).level().cssClass()).isEqualTo("msg-error");
  }
}
