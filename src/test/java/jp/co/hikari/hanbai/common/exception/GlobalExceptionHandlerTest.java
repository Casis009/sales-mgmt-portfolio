package jp.co.hikari.hanbai.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

class GlobalExceptionHandlerTest {

  private final Clock clock =
      Clock.fixed(Instant.parse("2027-01-18T01:30:15Z"), ZoneId.of("Asia/Tokyo"));
  private final GlobalExceptionHandler handler =
      new GlobalExceptionHandler(new ReceiptNumberGenerator(clock));

  @Test
  void 受付番号は日時14桁とランダム4桁() {
    String receipt = new ReceiptNumberGenerator(clock).next();
    assertThat(receipt).hasSize(18).startsWith("20270118103015").containsOnlyDigits();
  }

  @Test
  void 想定外の例外は受付番号付きの500画面になり例外の内容を画面に渡さない() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/menu");
    ModelAndView mav =
        handler.handleSystemError(new IllegalStateException("SQL: SELECT secret"), request);

    assertThat(mav.getViewName()).isEqualTo("error/500");
    assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(mav.getModel())
        .containsOnlyKeys(GlobalExceptionHandler.RECEIPT_NUMBER)
        .extractingByKey(GlobalExceptionHandler.RECEIPT_NUMBER)
        .asString()
        .startsWith("20270118103015");
  }

  @Test
  void 権限エラーは403画面になる() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/users");
    request.setUserPrincipal(() -> "yamamoto");
    ModelAndView mav = handler.handleAccessDenied(request);
    assertThat(mav.getViewName()).isEqualTo("error/403");
    assertThat(mav.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);

    ModelAndView anonymous = handler.handleAccessDenied(new MockHttpServletRequest());
    assertThat(anonymous.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void 存在しないページは404画面になる() {
    ModelAndView mav = handler.handleNotFound();
    assertThat(mav.getViewName()).isEqualTo("error/404");
    assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void 楽観的排他エラーはE020の業務エラー() {
    assertThat(new OptimisticLockException())
        .isInstanceOf(BusinessException.class)
        .extracting(BusinessException::getMessageId)
        .isEqualTo("E-020");
  }
}
