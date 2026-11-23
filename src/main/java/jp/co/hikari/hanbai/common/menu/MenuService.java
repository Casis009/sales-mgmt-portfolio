package jp.co.hikari.hanbai.common.menu;

import static jp.co.hikari.hanbai.common.code.Role.ACCOUNTING;
import static jp.co.hikari.hanbai.common.code.Role.ADMIN;
import static jp.co.hikari.hanbai.common.code.Role.APPROVER;
import static jp.co.hikari.hanbai.common.code.Role.SALES;
import static jp.co.hikari.hanbai.common.code.Role.WAREHOUSE;

import java.util.List;
import java.util.Set;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.springframework.stereotype.Service;

/**
 * メニュー（SCR-002）に表示する画面の一覧。権限に応じたメニューのみを返す（画面一覧 5.3）。
 *
 * <p>表示する権限は、画面一覧の「利用権限」と同じとする。登録画面は、登録できるロールにのみ表示する（権限マトリクス）。
 */
@Service
public class MenuService {

  private static final Set<Role> ALL = Set.of();

  private static final List<MenuGroup> MENU =
      List.of(
          new MenuGroup(
              "マスタ管理",
              List.of(
                  item("SCR-101", "商品一覧", "/master/products", ALL),
                  item("SCR-111", "倉庫一覧", "/master/warehouses", ALL),
                  item("SCR-121", "取引先一覧", "/master/partners", ALL))),
          new MenuGroup(
              "在庫管理",
              List.of(
                  item("SCR-201", "在庫照会", "/stock/stocks", ALL),
                  item("SCR-202", "入出庫履歴照会", "/stock/movements", Set.of(WAREHOUSE, ADMIN)),
                  item("SCR-203", "在庫調整登録", "/stock/adjustments/new", Set.of(WAREHOUSE)),
                  item("SCR-204", "倉庫間移動登録", "/stock/transfers/new", Set.of(WAREHOUSE)),
                  item("SCR-211", "棚卸一覧", "/stock/stocktakings", Set.of(WAREHOUSE, ADMIN)))),
          new MenuGroup(
              "購買管理",
              List.of(
                  item("SCR-301", "発注一覧", "/purchase/orders", Set.of(WAREHOUSE, APPROVER, ADMIN)),
                  item("SCR-302", "発注登録", "/purchase/orders/new", Set.of(WAREHOUSE)),
                  item(
                      "SCR-305",
                      "発注アラート一覧",
                      "/purchase/reorder-alerts",
                      Set.of(WAREHOUSE, ADMIN)))),
          new MenuGroup(
              "販売管理",
              List.of(
                  item(
                      "SCR-401",
                      "受注一覧",
                      "/sales/orders",
                      Set.of(SALES, WAREHOUSE, ACCOUNTING, ADMIN)),
                  item("SCR-402", "受注登録", "/sales/orders/new", Set.of(SALES)),
                  item("SCR-404", "出荷予定一覧", "/sales/shipment-plans", Set.of(WAREHOUSE, ADMIN)))),
          new MenuGroup(
              "請求管理",
              List.of(
                  item("SCR-501", "請求締め処理", "/billing/closings/new", Set.of(ACCOUNTING)),
                  item(
                      "SCR-502",
                      "請求一覧",
                      "/billing/invoices",
                      Set.of(ACCOUNTING, SALES, ADMIN)))),
          new MenuGroup(
              "システム管理",
              List.of(
                  item("SCR-011", "ユーザー一覧", "/admin/users", Set.of(ADMIN)),
                  item("SCR-013", "操作ログ照会", "/admin/operation-logs", Set.of(ADMIN)),
                  item("SCR-003", "パスワード変更", "/password", ALL))));

  private static MenuItem item(String screenId, String label, String url, Set<Role> roles) {
    return new MenuItem(screenId, label, url, roles);
  }

  /**
   * ユーザーが利用できるメニューを返す。利用できる画面が1つもないグループは除く。
   *
   * @param user ログインユーザー
   * @return メニューのグループ
   */
  public List<MenuGroup> menuFor(LoginUser user) {
    return MENU.stream()
        .map(g -> new MenuGroup(g.title(), visibleItems(g, user)))
        .filter(g -> !g.items().isEmpty())
        .toList();
  }

  private static List<MenuItem> visibleItems(MenuGroup group, LoginUser user) {
    return group.items().stream().filter(i -> i.isVisibleTo(user)).toList();
  }

  /**
   * メニューのグループ。
   *
   * @param title グループ名
   * @param items 画面
   */
  public record MenuGroup(String title, List<MenuItem> items) {}

  /**
   * メニューの1項目。
   *
   * @param screenId 画面ID
   * @param label 画面名
   * @param url URL
   * @param roles 表示するロール（空は全員）
   */
  public record MenuItem(String screenId, String label, String url, Set<Role> roles) {

    /**
     * ユーザーに表示するかを返す。
     *
     * @param user ログインユーザー
     * @return いずれかのロールを持つ場合（全員向けの場合）true
     */
    public boolean isVisibleTo(LoginUser user) {
      return roles.isEmpty() || roles.stream().anyMatch(user::hasRole);
    }
  }
}
