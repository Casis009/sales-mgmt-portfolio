package jp.co.hikari.hanbai.common.config;

import static jp.co.hikari.hanbai.common.code.Role.ACCOUNTING;
import static jp.co.hikari.hanbai.common.code.Role.ADMIN;
import static jp.co.hikari.hanbai.common.code.Role.APPROVER;
import static jp.co.hikari.hanbai.common.code.Role.SALES;
import static jp.co.hikari.hanbai.common.code.Role.WAREHOUSE;

import java.util.Arrays;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.AccessDeniedLoggingHandler;
import jp.co.hikari.hanbai.common.security.LoginFailureHandler;
import jp.co.hikari.hanbai.common.security.LogoutRecorder;
import jp.co.hikari.hanbai.common.security.PasswordChangeRequiredFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * 認証・認可の設定（セキュリティ設計 1～3章、4章 No.3～5）。
 *
 * <p>URL とロールの対応はセキュリティ設計 3章「URL とロールの対応」と画面一覧の利用権限に従う。
 * 明示的に許可した URL 以外はすべて拒否する。更新処理の権限は、Service の {@code @PreAuthorize} でも確認する（3章 ②）。
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

  /** ログイン画面。 */
  public static final String LOGIN_URL = LoginFailureHandler.LOGIN_URL;

  /** ログイン後の画面（メニュー）。 */
  public static final String MENU_URL = "/menu";

  /** セッション切れで遷移したことを示すパラメータ（E-013）。 */
  public static final String TIMEOUT_PARAM = "timeout";

  /** 静的ファイル（セッションを使わずに配信する）。 */
  private static final String[] STATIC_RESOURCES = {"/css/**", "/js/**"};

  /** マスタの一覧画面（全員が照会できる）。 */
  private static final String[] MASTER_LISTS = {
    "/master/products", "/master/warehouses", "/master/partners"
  };

  /** ログアウト後に削除する Cookie。 */
  private static final String SESSION_COOKIE = "JSESSIONID";

  /**
   * 静的ファイル（CSS・JS）のセキュリティフィルタ。画面用のフィルタより先に適用する。
   *
   * <p>セッションを参照・作成しない。アプリの再起動後やセッション切れの後は、ブラウザが存在しないセッションIDを送るため、
   * 画面用のフィルタではセッション切れ（E-013）として静的ファイルの要求までログイン画面へ転送され、CSS が適用されない画面が
   * 表示される。静的ファイルは利用者に依存しないため、セッションを使わずに配信する（セキュリティ設計 2章 No.1）。
   *
   * @param http HttpSecurity
   * @return フィルタチェーン
   * @throws Exception 設定に失敗した場合
   */
  @Bean
  @Order(1)
  public SecurityFilterChain staticResourceFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(STATIC_RESOURCES)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityContext(context -> context.disable())
        .requestCache(cache -> cache.disable())
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.deny()));
    return http.build();
  }

  /**
   * 画面のセキュリティフィルタを定義する。
   *
   * @param http HttpSecurity
   * @param failureHandler 認証失敗時の処理
   * @param logoutRecorder ログアウトの記録
   * @return フィルタチェーン
   * @throws Exception 設定に失敗した場合
   */
  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, LoginFailureHandler failureHandler, LogoutRecorder logoutRecorder)
      throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth
                    // 全員（未認証を含む）
                    .requestMatchers(
                        LOGIN_URL, "/css/**", "/js/**", "/error", "/actuator/health/**")
                    .permitAll()
                    // 共通
                    .requestMatchers("/", MENU_URL, PasswordChangeRequiredFilter.PASSWORD_URL)
                    .authenticated()
                    .requestMatchers("/admin/**")
                    .hasRole(ADMIN.name())
                    // マスタ管理（一覧は全員。登録・編集は画面一覧の利用権限）
                    .requestMatchers(HttpMethod.GET, MASTER_LISTS)
                    .authenticated()
                    .requestMatchers("/master/products/**")
                    .hasAnyRole(names(WAREHOUSE, ADMIN))
                    .requestMatchers("/master/warehouses/**")
                    .hasRole(ADMIN.name())
                    .requestMatchers("/master/partners/**")
                    .hasAnyRole(names(SALES, ACCOUNTING, ADMIN))
                    // 在庫管理
                    .requestMatchers("/stock/stocks/**")
                    .authenticated()
                    .requestMatchers("/stock/adjustments/**", "/stock/transfers/**")
                    .hasRole(WAREHOUSE.name())
                    .requestMatchers("/stock/movements/**", "/stock/stocktakings/**")
                    .hasAnyRole(names(WAREHOUSE, ADMIN))
                    // 購買管理
                    .requestMatchers("/purchase/reorder-alerts/**")
                    .hasAnyRole(names(WAREHOUSE, ADMIN))
                    .requestMatchers("/purchase/orders/**")
                    .hasAnyRole(names(WAREHOUSE, APPROVER, ADMIN))
                    // 販売管理
                    .requestMatchers("/sales/shipment-plans/**")
                    .hasAnyRole(names(WAREHOUSE, ADMIN))
                    .requestMatchers("/sales/orders/**")
                    .hasAnyRole(names(SALES, WAREHOUSE, ACCOUNTING, ADMIN))
                    // 請求管理
                    .requestMatchers("/billing/closings/**")
                    .hasRole(ACCOUNTING.name())
                    .requestMatchers("/billing/invoices/**")
                    .hasAnyRole(names(ACCOUNTING, SALES, ADMIN))
                    // 上記以外は拒否する
                    .anyRequest()
                    .denyAll())
        .formLogin(
            form ->
                form.loginPage(LOGIN_URL)
                    .usernameParameter(LoginFailureHandler.LOGIN_ID)
                    .passwordParameter(LoginFailureHandler.PASSWORD)
                    .defaultSuccessUrl(MENU_URL, true)
                    .failureHandler(failureHandler))
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .addLogoutHandler(logoutRecorder)
                    .invalidateHttpSession(true)
                    .deleteCookies(SESSION_COOKIE)
                    .logoutSuccessUrl(LOGIN_URL))
        .sessionManagement(
            session ->
                session
                    // セッション固定攻撃への対策：ログイン成功時にセッションIDを再発行する（NF-32）
                    .sessionFixation(fixation -> fixation.changeSessionId())
                    // 無操作30分（server.servlet.session.timeout）で切れたセッションでの操作（E-013）
                    // 値を付ける（空の値は入力値の共通処理で null に変換され、判定できないため）
                    .invalidSessionUrl(LOGIN_URL + "?" + TIMEOUT_PARAM + "=1"))
        .exceptionHandling(ex -> ex.accessDeniedHandler(new AccessDeniedLoggingHandler()))
        .headers(headers -> headers.frameOptions(frame -> frame.deny()))
        .addFilterAfter(new PasswordChangeRequiredFilter(), AuthorizationFilter.class);
    return http.build();
  }

  /**
   * パスワードのハッシュ化（BCrypt。ストレッチング回数は既定値。NF-30）。
   *
   * @return パスワードエンコーダー
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private static String[] names(Role... roles) {
    return Arrays.stream(roles).map(Role::name).toArray(String[]::new);
  }
}
