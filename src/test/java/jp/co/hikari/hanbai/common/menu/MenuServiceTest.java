package jp.co.hikari.hanbai.common.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.menu.DashboardService.DashboardItem;
import jp.co.hikari.hanbai.common.menu.MenuService.MenuGroup;
import jp.co.hikari.hanbai.common.menu.MenuService.MenuItem;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.junit.jupiter.api.Test;

class MenuServiceTest {

  private final MenuService menuService = new MenuService();
  private final DashboardService dashboardService = new DashboardService();

  private static LoginUser user(Role... roles) {
    return new LoginUser(1L, "u", "氏名", null, List.of(roles), true, false, false, null);
  }

  private List<String> screens(Role... roles) {
    return menuService.menuFor(user(roles)).stream()
        .flatMap(g -> g.items().stream())
        .map(MenuItem::screenId)
        .toList();
  }

  @Test
  void 営業担当は受注登録と取引先などを表示し在庫調整やユーザー管理は表示しない() {
    assertThat(screens(Role.SALES))
        .contains("SCR-101", "SCR-121", "SCR-201", "SCR-401", "SCR-402", "SCR-502", "SCR-003")
        .doesNotContain("SCR-203", "SCR-301", "SCR-501", "SCR-011", "SCR-013");
  }

  @Test
  void システム管理者は照会画面とユーザー管理を表示し登録画面は表示しない() {
    assertThat(screens(Role.ADMIN))
        .contains("SCR-011", "SCR-013", "SCR-202", "SCR-301", "SCR-401", "SCR-502")
        .doesNotContain("SCR-203", "SCR-302", "SCR-402", "SCR-501");
  }

  @Test
  void 複数ロールはいずれかのロールで利用できる画面を表示する() {
    assertThat(screens(Role.SALES, Role.APPROVER)).contains("SCR-402", "SCR-301");
  }

  @Test
  void 利用できる画面のないグループは表示しない() {
    List<MenuGroup> groups = menuService.menuFor(user(Role.APPROVER));
    assertThat(groups).extracting(MenuGroup::title).doesNotContain("販売管理", "請求管理");
  }

  @Test
  void ダッシュボードは権限に応じた件数を0件で表示する() {
    assertThat(dashboardService.itemsFor(user(Role.WAREHOUSE)))
        .extracting(DashboardItem::label)
        .containsExactly("発注アラート件数", "未出荷受注件数");
    assertThat(dashboardService.itemsFor(user(Role.APPROVER)))
        .extracting(DashboardItem::label, DashboardItem::count)
        .containsExactly(tuple("承認待ち件数", 0L));
    assertThat(dashboardService.itemsFor(user(Role.ADMIN))).isEmpty();
  }
}
