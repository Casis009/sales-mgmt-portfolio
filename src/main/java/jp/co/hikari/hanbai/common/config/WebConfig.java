package jp.co.hikari.hanbai.common.config;

import jp.co.hikari.hanbai.common.web.RequestMdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 画面共通の設定（ソフトウェア構成 3章 common.config）。 */
@Configuration(proxyBeanMethods = false)
public class WebConfig {

  /** Spring Security のフィルタ（既定の順序 -100）の直後。認証情報を参照するため後に置く。 */
  private static final int AFTER_SECURITY_FILTER = -99;

  /**
   * ログの MDC を設定するフィルタを登録する。
   *
   * @return フィルタの登録
   */
  @Bean
  public FilterRegistrationBean<RequestMdcFilter> requestMdcFilter() {
    FilterRegistrationBean<RequestMdcFilter> registration =
        new FilterRegistrationBean<>(new RequestMdcFilter());
    registration.setOrder(AFTER_SECURITY_FILTER);
    return registration;
  }
}
