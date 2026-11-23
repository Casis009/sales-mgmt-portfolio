package jp.co.hikari.hanbai.common.security;

import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** テスト用のユーザーの作成（ログインユーザーの生成と、m_user・m_user_role への登録）。 */
public final class TestUsers {

  /** テストユーザーの共通パスワード（NF-24 の条件を満たす）。 */
  public static final String PASSWORD = "Passw0rd";

  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

  private TestUsers() {}

  /**
   * DB に登録しないログインユーザーを作る（MockMvc の認証済みユーザー、Service のテスト用）。
   *
   * @param roles ロール
   * @return ログインユーザー（ユーザーID 1、パスワード変更済み）
   */
  public static LoginUser of(Role... roles) {
    return new LoginUser(
        1L, "user" + roles.length, "テスト ユーザー", null, List.of(roles), true, false, false, null);
  }

  /**
   * 業務テーブルを空にする（操作ログ・ユーザー）。テスト用コンテナの DB ユーザーは所有者のため削除できる。
   *
   * @param jdbc JdbcTemplate
   */
  public static void clean(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM t_operation_log");
    jdbc.update("DELETE FROM m_user_role");
    jdbc.update("DELETE FROM m_user");
  }

  /**
   * ユーザーを登録する。
   *
   * @param jdbc JdbcTemplate
   * @param loginId ログインID
   * @param enabled 有効フラグ
   * @param locked ロックフラグ
   * @param failedLoginCount 認証失敗回数
   * @param passwordChangeRequired パスワード変更要求フラグ
   * @param roles ロール
   * @return ユーザーID
   */
  public static long insert(
      JdbcTemplate jdbc,
      String loginId,
      boolean enabled,
      boolean locked,
      int failedLoginCount,
      boolean passwordChangeRequired,
      Role... roles) {
    Long userId =
        jdbc.queryForObject(
            "INSERT INTO m_user (login_id, password_hash, user_name, enabled, locked,"
                + " failed_login_count, password_change_required,"
                + " created_at, created_by, updated_at, updated_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, now(), 0, now(), 0) RETURNING user_id",
            Long.class,
            loginId,
            ENCODER.encode(PASSWORD),
            loginId + " 氏名",
            enabled,
            locked,
            failedLoginCount,
            passwordChangeRequired);
    for (Role role : roles) {
      jdbc.update(
          "INSERT INTO m_user_role (user_id, role_code, created_at, created_by)"
              + " VALUES (?, ?, now(), 0)",
          userId,
          role.name());
    }
    return userId;
  }

  /**
   * 有効・パスワード変更済みのユーザーを登録する。
   *
   * @param jdbc JdbcTemplate
   * @param loginId ログインID
   * @param roles ロール
   * @return ユーザーID
   */
  public static long insert(JdbcTemplate jdbc, String loginId, Role... roles) {
    return insert(jdbc, loginId, true, false, 0, false, roles);
  }
}
