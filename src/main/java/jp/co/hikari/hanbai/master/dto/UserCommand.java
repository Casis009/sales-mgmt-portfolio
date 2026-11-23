package jp.co.hikari.hanbai.master.dto;

import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;

/**
 * ユーザーの登録・更新の内容（SCR-012）。Controller が Form から作り、Service に渡す。
 *
 * @param userId ユーザーID（登録時は null）
 * @param version バージョン（登録時は 0）
 * @param loginId ログインID（更新時は変更しない）
 * @param userName 氏名
 * @param department 所属
 * @param defaultWarehouseId 所属倉庫ID
 * @param roles ロール
 * @param enabled 有効フラグ
 * @param unlock ロックを解除する場合 true（更新時のみ）
 * @param initialPassword 初期パスワード（登録時のみ）
 */
public record UserCommand(
    Long userId,
    int version,
    String loginId,
    String userName,
    String department,
    Long defaultWarehouseId,
    List<Role> roles,
    boolean enabled,
    boolean unlock,
    String initialPassword) {

  /** パスワードを文字列表現（ログ）に含めない。 */
  @Override
  public String toString() {
    return "UserCommand[userId=" + userId + ", loginId=" + loginId + ", roles=" + roles + "]";
  }
}
