package jp.co.hikari.hanbai.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class TransactionTokenManagerTest {

  private final TransactionTokenManager manager = new TransactionTokenManager();
  private final MockHttpSession session = new MockHttpSession();

  @Test
  void 同じトークンで2回送信すると2回目は処理されない() {
    String token = manager.issue(session, "SCR-012");
    assertThat(manager.consume(session, "SCR-012", token)).isTrue();
    assertThat(manager.consume(session, "SCR-012", token)).isFalse();
  }

  @Test
  void 古い画面のトークンや他の画面のトークンは一致しない() {
    String old = manager.issue(session, "SCR-012");
    String current = manager.issue(session, "SCR-012");
    assertThat(manager.consume(session, "SCR-012", old)).isFalse();
    assertThat(manager.consume(session, "SCR-003", current)).isFalse();
    assertThat(manager.consume(session, "SCR-012", null)).isFalse();
    assertThat(manager.consume(session, "SCR-012", current)).isTrue();
  }
}
