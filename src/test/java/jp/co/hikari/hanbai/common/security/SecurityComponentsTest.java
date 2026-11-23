package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 認証まわりの小さな部品（LoginUser、強制パスワード変更フィルタ、権限エラー処理）。 */
class SecurityComponentsTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static LoginUser user(boolean passwordChangeRequired) {
    return new LoginUser(
        5L,
        "tanaka",
        "田中",
        2L,
        List.of(Role.APPROVER, Role.SALES),
        true,
        false,
        passwordChangeRequired,
        "$2a$hash");
  }

  private static void login(LoginUser user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
  }

  private static MockFilterChain filter(String path) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    new PasswordChangeRequiredFilter()
        .doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), chain);
    return chain;
  }

  @Test
  void ログインユーザーはロールを権限名とヘッダー表示名に変換する() {
    LoginUser user = user(false);
    assertThat(user.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_SALES", "ROLE_APPROVER");
    assertThat(user.roleLabels()).isEqualTo("営業担当・承認者");
    assertThat(user.hasRole(Role.APPROVER)).isTrue();
    assertThat(user.hasRole(Role.ADMIN)).isFalse();
    assertThat(user.getUsername()).isEqualTo("tanaka");
    assertThat(user.getDefaultWarehouseId()).isEqualTo(2L);
    assertThat(user.isAccountNonLocked()).isTrue();
    assertThat(user.isEnabled()).isTrue();
  }

  @Test
  void パスワードハッシュは消去でき文字列表現に含めない() {
    LoginUser user = user(false);
    assertThat(user.toString()).doesNotContain("$2a$hash");
    user.eraseCredentials();
    assertThat(user.getPassword()).isNull();
  }

  @Test
  void パスワード変更後は変更要求フラグが解除され同じユーザーとして扱う() {
    LoginUser before = user(true);
    LoginUser after = before.withPasswordChanged();
    assertThat(after.isPasswordChangeRequired()).isFalse();
    assertThat(after).isEqualTo(before).hasSameHashCodeAs(before);
    assertThat(after.getRoles()).isEqualTo(before.getRoles());
  }

  @Test
  void ロールなしのユーザーも作成できる() {
    LoginUser none = new LoginUser(9L, "x", "x", null, List.of(), true, true, false, null);
    assertThat(none.getRoles()).isEmpty();
    assertThat(none.isAccountNonLocked()).isFalse();
  }

  @Test
  void 初期パスワードのユーザーはパスワード変更画面以外へのアクセスでリダイレクトされる() throws Exception {
    login(user(true));
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new PasswordChangeRequiredFilter()
        .doFilter(new MockHttpServletRequest("GET", "/menu"), response, chain);
    assertThat(response.getRedirectedUrl()).isEqualTo("/password");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void 初期パスワードのユーザーもパスワード変更とログアウトと静的ファイルは利用できる() throws Exception {
    login(user(true));
    for (String path : List.of("/password", "/logout", "/css/app.css", "/js/app.js", "/error")) {
      assertThat(filter(path).getRequest()).as(path).isNotNull();
    }
  }

  @Test
  void パスワード変更済みのユーザーと未ログインは制限しない() throws Exception {
    assertThat(filter("/menu").getRequest()).isNotNull();
    login(user(false));
    assertThat(filter("/menu").getRequest()).isNotNull();
  }

  @Test
  void 権限エラーは403を返す() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/users");
    new AccessDeniedLoggingHandler().handle(request, response, new AccessDeniedException("x"));
    assertThat(response.getStatus()).isEqualTo(403);

    request.setUserPrincipal(() -> "tanaka");
    MockHttpServletResponse second = new MockHttpServletResponse();
    new AccessDeniedLoggingHandler().handle(request, second, new AccessDeniedException("x"));
    assertThat(second.getStatus()).isEqualTo(403);
  }
}
