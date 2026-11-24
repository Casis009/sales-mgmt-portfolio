package jp.co.hikari.hanbai.master.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.common.security.TestUsers;
import jp.co.hikari.hanbai.master.FormSubmitter;
import jp.co.hikari.hanbai.master.MasterTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** 倉庫マスタ（F-102、SCR-111・SCR-112）と、ユーザー画面の所属倉庫の選択肢。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WarehouseManagementIT {

  private static final LoginUser ADMIN =
      new LoginUser(
          Long.MAX_VALUE, "operator", "操作者", null, List.of(Role.ADMIN), true, false, false, null);

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private FormSubmitter admin;

  @BeforeEach
  void setUp() {
    TestUsers.clean(jdbc);
    MasterTestData.clean(jdbc);
    admin = new FormSubmitter(mvc, ADMIN);
  }

  private static Map<String, String> warehouse(String code, String name) {
    Map<String, String> params = new HashMap<>();
    params.put("warehouseCode", code);
    params.put("warehouseName", name);
    params.put("postalCode", "812-0011");
    params.put("address", "福岡市博多区博多駅前1-1-1");
    params.put("tel", "092-000-0000");
    params.put("enabled", "true");
    return params;
  }

  @Test
  void 一覧は全員が照会でき登録と編集のリンクはシステム管理者にだけ表示する() throws Exception {
    MasterTestData.warehouse(jdbc, "HQ", "本社倉庫", true);
    MasterTestData.warehouse(jdbc, "OLD", "旧倉庫", false);

    String sales =
        new FormSubmitter(mvc, TestUsers.of(Role.SALES))
            .page(get("/master/warehouses").param("search", "1"));
    assertThat(sales).contains("本社倉庫").doesNotContain("旧倉庫", "新規登録", "/edit");

    String byAdmin =
        admin.page(get("/master/warehouses").param("search", "1").param("includeDisabled", "true"));
    assertThat(byAdmin).contains("本社倉庫", "旧倉庫", "新規登録", "/edit");
  }

  @Test
  void 倉庫を登録すると操作ログが記録され同じコードはE005() throws Exception {
    assertThat(
            admin
                .submit("/master/warehouses/new", "/master/warehouses", warehouse("HQ", "本社倉庫"))
                .getResponse()
                .getRedirectedUrl())
        .isEqualTo("/master/warehouses?search=1");
    assertThat(jdbc.queryForMap("SELECT * FROM m_warehouse"))
        .containsEntry("warehouse_code", "HQ")
        .containsEntry("postal_code", "812-0011");
    assertThat(
            jdbc.queryForMap("SELECT screen_id, operation_type, target_key FROM t_operation_log"))
        .containsEntry("screen_id", "SCR-112")
        .containsEntry("operation_type", "CREATE")
        .containsEntry("target_key", "HQ");

    assertThat(
            admin.submitAndRead(
                "/master/warehouses/new", "/master/warehouses", warehouse("HQ", "別倉庫")))
        .contains("倉庫コードは既に登録されています。");
  }

  @Test
  void 入力形式の誤りは項目の下に表示する() throws Exception {
    Map<String, String> params = warehouse("H Q", "");
    params.put("postalCode", "8120011");
    params.put("tel", "092(000)0000");
    String page = admin.submitAndRead("/master/warehouses/new", "/master/warehouses", params);
    assertThat(page)
        .contains(
            "倉庫コードの形式が正しくありません。",
            "倉庫名を入力してください。",
            "郵便番号の形式が正しくありません。",
            "電話番号の形式が正しくありません。");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM m_warehouse", Long.class)).isZero();
  }

  @Test
  void 編集では倉庫コードを変更せず無効にでき先に更新されていればE020() throws Exception {
    long id = MasterTestData.warehouse(jdbc, "HQ", "本社倉庫", true);
    Map<String, String> params = warehouse("CHANGED", "本社倉庫（新）");
    params.put("version", "0");
    params.put("enabled", "false");

    admin.submit("/master/warehouses/" + id + "/edit", "/master/warehouses/" + id, params);
    assertThat(jdbc.queryForMap("SELECT * FROM m_warehouse WHERE warehouse_id = ?", id))
        .containsEntry("warehouse_code", "HQ")
        .containsEntry("warehouse_name", "本社倉庫（新）")
        .containsEntry("enabled", false)
        .containsEntry("version", 1);
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .contains("倉庫名：本社倉庫→本社倉庫（新）", "状態：有効→無効");

    String conflict =
        admin.submitAndRead(
            "/master/warehouses/" + id + "/edit", "/master/warehouses/" + id, params);
    assertThat(conflict).contains("他のユーザーが先に更新しました。");
  }

  @Test
  void ユーザーの所属倉庫は有効な倉庫と現在の無効な倉庫を選択肢に表示し変えなければ更新できる() throws Exception {
    MasterTestData.warehouse(jdbc, "HQ", "本社倉庫", true);
    long old = MasterTestData.warehouse(jdbc, "OLD", "旧倉庫", false);
    MasterTestData.warehouse(jdbc, "GONE", "廃止倉庫", false);
    long userId = TestUsers.insert(jdbc, "yamamoto", Role.WAREHOUSE);
    jdbc.update("UPDATE m_user SET default_warehouse_id = ? WHERE user_id = ?", old, userId);

    String edit = admin.page(get("/admin/users/" + userId + "/edit"));
    assertThat(edit).contains("HQ 本社倉庫", "OLD 旧倉庫（無効）").doesNotContain("廃止倉庫");
    assertThat(admin.page(get("/admin/users/new"))).contains("HQ 本社倉庫").doesNotContain("旧倉庫");

    Map<String, String> params = new HashMap<>();
    params.put("version", "0");
    params.put("loginId", "yamamoto");
    params.put("userName", "山本 太郎");
    params.put("defaultWarehouseId", String.valueOf(old));
    params.put("roles", "WAREHOUSE");
    params.put("enabled", "true");
    admin.submit("/admin/users/" + userId + "/edit", "/admin/users/" + userId, params);
    assertThat(
            jdbc.queryForObject(
                "SELECT user_name FROM m_user WHERE user_id = ?", String.class, userId))
        .isEqualTo("山本 太郎");
  }
}
