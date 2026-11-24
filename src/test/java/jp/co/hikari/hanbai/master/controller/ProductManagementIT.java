package jp.co.hikari.hanbai.master.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import jp.co.hikari.hanbai.common.code.Role;
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

/** 商品マスタ（F-101、SCR-101・SCR-102）と発注点設定（F-104）。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductManagementIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private FormSubmitter warehouseStaff;
  private long hq;
  private long kitakyushu;
  private long supplier;

  @BeforeEach
  void setUp() {
    MasterTestData.clean(jdbc);
    warehouseStaff = new FormSubmitter(mvc, TestUsers.of(Role.WAREHOUSE));
    hq = MasterTestData.warehouse(jdbc, "HQ", "本社倉庫", true);
    kitakyushu = MasterTestData.warehouse(jdbc, "KK", "北九州倉庫", true);
    supplier = MasterTestData.supplier(jdbc, "S001", "九州照明工業", true);
  }

  private Map<String, String> product(String code) {
    Map<String, String> params = new HashMap<>();
    params.put("productCode", code);
    params.put("productName", "LEDダウンライト");
    params.put("modelNumber", "DL-100");
    params.put("unit", "台");
    params.put("standardSalesPrice", "3000");
    params.put("standardPurchasePrice", "2000");
    params.put("taxCategory", "STANDARD");
    params.put("mainSupplierId", String.valueOf(supplier));
    params.put("enabled", "true");
    params.put("settings[0].warehouseId", String.valueOf(hq));
    params.put("settings[0].warehouseLabel", "HQ 本社倉庫");
    params.put("settings[0].reorderPoint", "10");
    params.put("settings[0].standardOrderQuantity", "50");
    params.put("settings[1].warehouseId", String.valueOf(kitakyushu));
    params.put("settings[1].warehouseLabel", "KK 北九州倉庫");
    params.put("settings[1].reorderPoint", "");
    params.put("settings[1].standardOrderQuantity", "");
    return params;
  }

  private long productId(String code) {
    return jdbc.queryForObject(
        "SELECT product_id FROM m_product WHERE product_code = ?", Long.class, code);
  }

  private List<String> settings(long productId) {
    return jdbc.queryForList(
        "SELECT w.warehouse_code || ':' || pw.reorder_point || '/' || pw.standard_order_quantity"
            + " FROM m_product_warehouse pw JOIN m_warehouse w USING (warehouse_id)"
            + " WHERE pw.product_id = ? ORDER BY w.warehouse_code",
        String.class,
        productId);
  }

  @Test
  void 登録画面には有効な倉庫ごとの発注点欄を表示する() throws Exception {
    MasterTestData.warehouse(jdbc, "OLD", "旧倉庫", false);
    String page = warehouseStaff.page(get("/master/products/new"));
    assertThat(page).contains("HQ 本社倉庫", "KK 北九州倉庫", "S001 九州照明工業").doesNotContain("旧倉庫");
  }

  @Test
  void 商品を登録すると発注点を入力した倉庫だけ設定を作り操作ログに記録する() throws Exception {
    assertThat(
            warehouseStaff
                .submit("/master/products/new", "/master/products", product("LED-001"))
                .getResponse()
                .getRedirectedUrl())
        .isEqualTo("/master/products?search=1");

    long id = productId("LED-001");
    assertThat(jdbc.queryForMap("SELECT * FROM m_product WHERE product_id = ?", id))
        .containsEntry("tax_category", "1")
        .containsEntry("main_supplier_id", supplier);
    assertThat(
            (BigDecimal)
                jdbc.queryForObject(
                    "SELECT standard_sales_price FROM m_product WHERE product_id = ?",
                    BigDecimal.class,
                    id))
        .isEqualByComparingTo("3000");
    assertThat(settings(id)).containsExactly("HQ:10/50");
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .isEqualTo("品名：LEDダウンライト、発注点・標準発注数（HQ）：なし→10・50");
  }

  @Test
  void 編集で発注点の追加と変更と削除をまとめて反映する() throws Exception {
    warehouseStaff.submit("/master/products/new", "/master/products", product("LED-001"));
    final long id = productId("LED-001");
    jdbc.update("DELETE FROM t_operation_log");

    Map<String, String> params = product("LED-001");
    params.put("version", "0");
    params.put("productName", "LEDダウンライト 60W形");
    params.put("settings[0].reorderPoint", "");
    params.put("settings[0].standardOrderQuantity", "");
    params.put("settings[1].reorderPoint", "5");
    warehouseStaff.submit("/master/products/" + id + "/edit", "/master/products/" + id, params);

    assertThat(settings(id)).containsExactly("KK:5/0");
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .isEqualTo(
            "品名：LEDダウンライト→LEDダウンライト 60W形、"
                + "発注点・標準発注数（HQ）：10・50→なし、発注点・標準発注数（KK）：なし→5・0");
  }

  @Test
  void 無効な仕入先と単価の範囲外と発注点の誤りはエラーで登録しない() throws Exception {
    long disabled = MasterTestData.supplier(jdbc, "S999", "廃業商事", false);
    Map<String, String> badSupplier = product("LED-002");
    badSupplier.put("mainSupplierId", String.valueOf(disabled));
    assertThat(
            warehouseStaff.submitAndRead("/master/products/new", "/master/products", badSupplier))
        .contains("主仕入先が存在しないか、無効になっています。");

    Map<String, String> badValues = product("LED-003");
    badValues.put("standardSalesPrice", "1000000000");
    badValues.put("standardPurchasePrice", "10.5");
    badValues.put("settings[0].reorderPoint", "1000000");
    String page =
        warehouseStaff.submitAndRead("/master/products/new", "/master/products", badValues);
    assertThat(page)
        .contains(
            "標準販売単価は0以上999,999,999以下の数値で入力してください。",
            "標準仕入単価の形式が正しくありません。",
            "発注点（HQ 本社倉庫）は0以上999,999以下の数値で入力してください。");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM m_product", Long.class)).isZero();
  }

  @Test
  void 一覧は全員が照会でき営業担当は登録できない() throws Exception {
    warehouseStaff.submit("/master/products/new", "/master/products", product("LED-001"));
    FormSubmitter sales = new FormSubmitter(mvc, TestUsers.of(Role.SALES));

    String list =
        sales.page(
            get("/master/products")
                .param("search", "1")
                .param("supplierId", String.valueOf(supplier)));
    assertThat(list).contains("LED-001", "3,000", "九州照明工業").doesNotContain("新規登録");

    int status =
        mvc.perform(
                get("/master/products/new")
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user(TestUsers.of(Role.SALES))))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(403);
  }
}
