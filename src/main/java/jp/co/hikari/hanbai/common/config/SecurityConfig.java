package jp.co.hikari.hanbai.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 認証・認可の設定。
 *
 * <p>製造 1段階（開発環境構築）の暫定設定。ヘルスチェックのみ認証なしで公開し、それ以外は認証を必須とする。
 * ログイン画面・ロールによる認可・セッション設定は、製造 2段階（共通機能）でセキュリティ設計に従って実装する。
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

  /**
   * セキュリティフィルタを定義する。
   *
   * @param http HttpSecurity
   * @return フィルタチェーン
   * @throws Exception 設定に失敗した場合
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(Customizer.withDefaults());
    return http.build();
  }
}
