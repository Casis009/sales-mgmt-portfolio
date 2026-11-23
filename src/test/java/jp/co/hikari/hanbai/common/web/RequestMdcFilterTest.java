package jp.co.hikari.hanbai.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestMdcFilterTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 認証済みならリクエストIDとログインIDを設定し終了後に消す() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "yamamoto", null, AuthorityUtils.createAuthorityList("ROLE_WAREHOUSE")));
    Map<String, String> during = new HashMap<>();

    new RequestMdcFilter()
        .doFilter(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            (req, res) -> during.putAll(MDC.getCopyOfContextMap()));

    assertThat(during.get(RequestMdcFilter.REQUEST_ID)).hasSize(8);
    assertThat(during.get(RequestMdcFilter.LOGIN_ID)).isEqualTo("yamamoto");
    assertThat(MDC.get(RequestMdcFilter.REQUEST_ID)).isNull();
    assertThat(MDC.get(RequestMdcFilter.LOGIN_ID)).isNull();
  }

  @Test
  void 未認証ならログインIDを設定しない() throws Exception {
    Map<String, String> during = new HashMap<>();
    new RequestMdcFilter()
        .doFilter(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            (req, res) -> during.putAll(MDC.getCopyOfContextMap()));
    assertThat(during).containsKey(RequestMdcFilter.REQUEST_ID);
    assertThat(during).doesNotContainKey(RequestMdcFilter.LOGIN_ID);
  }
}
