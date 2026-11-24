package jp.co.hikari.hanbai.master;

import org.springframework.jdbc.core.JdbcTemplate;

/** マスタのテストデータの作成と削除。テスト用コンテナの DB ユーザーは所有者のため削除できる。 */
public final class MasterTestData {

  private MasterTestData() {}

  /**
   * マスタと操作ログを削除する（参照される側を後にする）。
   *
   * @param jdbc JdbcTemplate
   */
  public static void clean(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM t_operation_log");
    jdbc.update("DELETE FROM m_product_warehouse");
    jdbc.update("DELETE FROM m_product");
    jdbc.update("UPDATE m_user SET default_warehouse_id = NULL");
    jdbc.update("DELETE FROM m_warehouse");
    jdbc.update("DELETE FROM m_partner");
  }

  /**
   * 倉庫を登録する。
   *
   * @param jdbc JdbcTemplate
   * @param code 倉庫コード
   * @param name 倉庫名
   * @param enabled 有効フラグ
   * @return 倉庫ID
   */
  public static long warehouse(JdbcTemplate jdbc, String code, String name, boolean enabled) {
    return jdbc.queryForObject(
        "INSERT INTO m_warehouse (warehouse_code, warehouse_name, enabled,"
            + " created_at, created_by, updated_at, updated_by)"
            + " VALUES (?, ?, ?, now(), 0, now(), 0) RETURNING warehouse_id",
        Long.class,
        code,
        name,
        enabled);
  }

  /**
   * 仕入先を登録する。
   *
   * @param jdbc JdbcTemplate
   * @param code 取引先コード
   * @param name 取引先名
   * @param enabled 有効フラグ
   * @return 取引先ID
   */
  public static long supplier(JdbcTemplate jdbc, String code, String name, boolean enabled) {
    return jdbc.queryForObject(
        "INSERT INTO m_partner (partner_code, partner_name, is_customer, is_supplier, enabled,"
            + " created_at, created_by, updated_at, updated_by)"
            + " VALUES (?, ?, FALSE, TRUE, ?, now(), 0, now(), 0) RETURNING partner_id",
        Long.class,
        code,
        name,
        enabled);
  }

  /**
   * 得意先を登録する（締日 末日、翌月末日払い）。
   *
   * @param jdbc JdbcTemplate
   * @param code 取引先コード
   * @param name 取引先名
   * @return 取引先ID
   */
  public static long customer(JdbcTemplate jdbc, String code, String name) {
    return jdbc.queryForObject(
        "INSERT INTO m_partner (partner_code, partner_name, is_customer, is_supplier,"
            + " closing_day, payment_month_offset, payment_day, enabled,"
            + " created_at, created_by, updated_at, updated_by)"
            + " VALUES (?, ?, TRUE, FALSE, 99, 1, 99, TRUE, now(), 0, now(), 0)"
            + " RETURNING partner_id",
        Long.class,
        code,
        name);
  }
}
