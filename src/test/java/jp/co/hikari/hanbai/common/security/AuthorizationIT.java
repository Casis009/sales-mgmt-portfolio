package jp.co.hikari.hanbai.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * URL 単位の認可（CM-07・CM-08）。
 *
 * <p>権限マトリクス・画面一覧の各行について、許可されたロールでは 403 にならず、許可されていないロールでは 403 となることを確認する。
 * 未実装の画面は、許可されたロールでは 404 となる（認可を通過したことの確認になる）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorizationIT {

  private static final Set<Role> ALL = EnumSet.allOf(Role.class);

  @Autowired MockMvc mvc;

  static Stream<Arguments> matrix() {
    return Stream.of(
        // 共通
        row("GET", "/menu", ALL),
        row("GET", "/password", ALL),
        row("GET", "/admin/users", EnumSet.of(Role.ADMIN)),
        row("GET", "/admin/operation-logs", EnumSet.of(Role.ADMIN)),
        // マスタ管理
        row("GET", "/master/products", ALL),
        row("GET", "/master/products/new", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        row("POST", "/master/products", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        row("GET", "/master/warehouses", ALL),
        row("GET", "/master/warehouses/1/edit", EnumSet.of(Role.ADMIN)),
        row("GET", "/master/partners", ALL),
        row("GET", "/master/partners/new", EnumSet.of(Role.SALES, Role.ACCOUNTING, Role.ADMIN)),
        // 在庫管理
        row("GET", "/stock/stocks", ALL),
        row("GET", "/stock/movements", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        row("GET", "/stock/adjustments/new", EnumSet.of(Role.WAREHOUSE)),
        row("POST", "/stock/transfers", EnumSet.of(Role.WAREHOUSE)),
        row("GET", "/stock/stocktakings", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        // 購買管理
        row("GET", "/purchase/orders", EnumSet.of(Role.WAREHOUSE, Role.APPROVER, Role.ADMIN)),
        row("GET", "/purchase/reorder-alerts", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        // 販売管理
        row(
            "GET",
            "/sales/orders",
            EnumSet.of(Role.SALES, Role.WAREHOUSE, Role.ACCOUNTING, Role.ADMIN)),
        row("GET", "/sales/shipment-plans", EnumSet.of(Role.WAREHOUSE, Role.ADMIN)),
        // 請求管理
        row("GET", "/billing/closings/new", EnumSet.of(Role.ACCOUNTING)),
        row("GET", "/billing/invoices", EnumSet.of(Role.ACCOUNTING, Role.SALES, Role.ADMIN)),
        // 定義していない URL は全員拒否
        row("GET", "/unknown", EnumSet.noneOf(Role.class)));
  }

  private static Arguments row(String method, String url, Set<Role> allowed) {
    return Arguments.of(method, url, allowed);
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("matrix")
  void 許可されたロールのみアクセスできる(String method, String url, Set<Role> allowed)
      throws Exception {
    for (Role role : Role.values()) {
      MockHttpServletRequestBuilder request =
          "POST".equals(method) ? post(url).with(csrf()) : get(url);
      int status =
          mvc.perform(request.with(user(TestUsers.of(role)))).andReturn().getResponse().getStatus();
      if (allowed.contains(role)) {
        assertThat(status).as("%s %s %s", role, method, url).isNotEqualTo(403);
      } else {
        assertThat(status).as("%s %s %s", role, method, url).isEqualTo(403);
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("updateUrls")
  void 照会のみ可のシステム管理者が更新用URLにPOSTすると403(String url) throws Exception {
    int status =
        mvc.perform(post(url).with(csrf()).with(user(TestUsers.of(Role.ADMIN))))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(403);
  }

  static Stream<String> updateUrls() {
    return Stream.of("/stock/adjustments", "/stock/transfers", "/billing/closings");
  }
}
