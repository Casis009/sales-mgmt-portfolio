package jp.co.hikari.hanbai.master.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import jp.co.hikari.hanbai.common.code.Role;

/**
 * ユーザー一覧（SCR-011）の1行。
 *
 * @param userId ユーザーID
 * @param loginId ログインID
 * @param userName 氏名
 * @param department 所属
 * @param enabled 有効フラグ
 * @param locked ロックフラグ
 * @param lastLoginAt 最終ログイン日時
 * @param roles ロール
 */
public record UserListRow(
    long userId,
    String loginId,
    String userName,
    String department,
    boolean enabled,
    boolean locked,
    LocalDateTime lastLoginAt,
    List<Role> roles) {

  /**
   * ロールを付けた行を返す。
   *
   * @param userRoles ロール
   * @return 新しい行
   */
  public UserListRow withRoles(List<Role> userRoles) {
    List<Role> copied = List.copyOf(userRoles);
    return new UserListRow(
        userId, loginId, userName, department, enabled, locked, lastLoginAt, copied);
  }

  /**
   * 画面に表示するロール名を返す。
   *
   * @return 例：営業担当・承認者
   */
  public String roleLabels() {
    return roles.stream().sorted().map(Role::getLabel).collect(Collectors.joining("・"));
  }
}
