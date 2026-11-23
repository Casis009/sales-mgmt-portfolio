package jp.co.hikari.hanbai.common.menu;

import static jp.co.hikari.hanbai.common.code.Role.APPROVER;
import static jp.co.hikari.hanbai.common.code.Role.SALES;
import static jp.co.hikari.hanbai.common.code.Role.WAREHOUSE;

import java.util.List;
import java.util.Set;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.LoginUser;
import org.springframework.stereotype.Service;

/**
 * ダッシュボードの件数（F-006、SCR-002）。権限に応じた件数を表示し、クリックで各一覧へ遷移する。
 *
 * <p>製造 2段階（共通）では件数を 0 とする。発注アラート・承認待ちは 4段階（購買）、未出荷受注は 5段階（販売）で、
 * 各サブシステムの Service から件数を取得するように置き換える（他サブシステムの Mapper は直接呼ばない）。
 */
@Service
public class DashboardService {

  private static final List<Definition> DEFINITIONS =
      List.of(
          new Definition("発注アラート件数", "/purchase/reorder-alerts", Set.of(WAREHOUSE)),
          new Definition("承認待ち件数", "/purchase/orders", Set.of(APPROVER)),
          new Definition("未出荷受注件数", "/sales/orders", Set.of(WAREHOUSE, SALES)));

  /**
   * ユーザーに表示する件数を返す。
   *
   * @param user ログインユーザー
   * @return 件数（表示対象がない場合は空）
   */
  public List<DashboardItem> itemsFor(LoginUser user) {
    return DEFINITIONS.stream()
        .filter(d -> d.roles().stream().anyMatch(user::hasRole))
        .map(d -> new DashboardItem(d.label(), 0, d.url()))
        .toList();
  }

  private record Definition(String label, String url, Set<Role> roles) {}

  /**
   * ダッシュボードの1項目。
   *
   * @param label 表示名
   * @param count 件数
   * @param url 遷移先の一覧
   */
  public record DashboardItem(String label, long count, String url) {}
}
