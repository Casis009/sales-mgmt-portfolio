package jp.co.hikari.hanbai.common.controller;

import jp.co.hikari.hanbai.common.config.SecurityConfig;
import jp.co.hikari.hanbai.common.menu.DashboardService;
import jp.co.hikari.hanbai.common.menu.MenuService;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** SCR-002 メニュー（ダッシュボード。F-006）。 */
@Controller
public class MenuController {

  private final MenuService menuService;
  private final DashboardService dashboardService;

  /**
   * 依存部品を受け取る。
   *
   * @param menuService メニュー
   * @param dashboardService ダッシュボード
   */
  public MenuController(MenuService menuService, DashboardService dashboardService) {
    this.menuService = menuService;
    this.dashboardService = dashboardService;
  }

  /**
   * トップページはメニューへ遷移する。
   *
   * @return リダイレクト
   */
  @GetMapping("/")
  public String root() {
    return "redirect:" + SecurityConfig.MENU_URL;
  }

  /**
   * 権限に応じたメニューとダッシュボードを表示する。
   *
   * @param user ログインユーザー
   * @param model モデル
   * @return 画面
   */
  @GetMapping(SecurityConfig.MENU_URL)
  public String show(@AuthenticationPrincipal LoginUser user, Model model) {
    model.addAttribute("menuGroups", menuService.menuFor(user));
    model.addAttribute("dashboardItems", dashboardService.itemsFor(user));
    return "common/menu";
  }
}
