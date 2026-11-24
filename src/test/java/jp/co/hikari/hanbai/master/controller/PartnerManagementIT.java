package jp.co.hikari.hanbai.master.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.HashMap;
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

/** 取引先マスタ（F-103、SCR-121・SCR-122）。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PartnerManagementIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private FormSubmitter sales;

  @BeforeEach
  void setUp() {
    MasterTestData.clean(jdbc);
    sales = new FormSubmitter(mvc, TestUsers.of(Role.SALES));
  }

  private static Map<String, String> customer(String code) {
    Map<String, String> params = new HashMap<>();
    params.put("partnerCode", code);
    params.put("partnerName", "株式会社ひかり電設");
    params.put("partnerNameKana", "ヒカリデンセツ");
    params.put("customer", "true");
    params.put("closingDay", "20");
    params.put("paymentMonthOffset", "1");
    params.put("paymentDay", "99");
    params.put("enabled", "true");
    return params;
  }

  @Test
  void 得意先を登録すると締日と支払条件を保存し操作ログに記録する() throws Exception {
    assertThat(
            sales
                .submit("/master/partners/new", "/master/partners", customer("C001"))
                .getResponse()
                .getRedirectedUrl())
        .isEqualTo("/master/partners?search=1");
    assertThat(jdbc.queryForMap("SELECT * FROM m_partner WHERE partner_code = 'C001'"))
        .containsEntry("is_customer", true)
        .containsEntry("is_supplier", false)
        .containsEntry("closing_day", 20)
        .containsEntry("payment_day", 99);
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .isEqualTo("区分：得意先、20日締め 翌月末日払い");
  }

  @Test
  void 仕入先のみの場合は送信された締日と支払条件を保存しない() throws Exception {
    Map<String, String> params = customer("S001");
    params.remove("customer");
    params.put("supplier", "true");
    sales.submit("/master/partners/new", "/master/partners", params);
    assertThat(jdbc.queryForMap("SELECT * FROM m_partner WHERE partner_code = 'S001'"))
        .containsEntry("is_supplier", true)
        .containsEntry("closing_day", null)
        .containsEntry("payment_month_offset", null);
  }

  @Test
  void 区分なしと得意先の締日なしとカナの誤りは項目の下に表示する() throws Exception {
    Map<String, String> noType = customer("X001");
    noType.remove("customer");
    noType.put("partnerNameKana", "ひかり");
    String page = sales.submitAndRead("/master/partners/new", "/master/partners", noType);
    assertThat(page).contains("取引先区分を入力してください。", "取引先名カナの形式が正しくありません。");

    Map<String, String> noTerms = customer("X002");
    noTerms.remove("closingDay");
    assertThat(sales.submitAndRead("/master/partners/new", "/master/partners", noTerms))
        .contains("締日を入力してください。");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM m_partner", Long.class)).isZero();
  }

  @Test
  void 一覧はキーワードとカナと区分で検索できる() throws Exception {
    MasterTestData.customer(jdbc, "C001", "株式会社ひかり電設");
    MasterTestData.supplier(jdbc, "S001", "九州照明工業", true);
    jdbc.update(
        "UPDATE m_partner SET partner_name_kana = ? WHERE partner_code = 'S001'", "キュウシュウショウメイ");

    String byKana =
        sales.page(get("/master/partners").param("search", "1").param("keyword", "キュウシュウ"));
    assertThat(byKana).contains("九州照明工業").doesNotContain("ひかり電設");

    String customers =
        sales.page(get("/master/partners").param("search", "1").param("partyType", "CUSTOMER"));
    assertThat(customers).contains("ひかり電設", "末日締め 翌月末日払い").doesNotContain("九州照明工業");
  }

  @Test
  void 編集では取引先コードを変更せず得意先をやめると締日を消し物流担当は登録できない() throws Exception {
    final long id = MasterTestData.customer(jdbc, "C001", "株式会社ひかり電設");
    Map<String, String> params = customer("CHANGED");
    params.put("version", "0");
    params.remove("customer");
    params.put("supplier", "true");

    sales.submit("/master/partners/" + id + "/edit", "/master/partners/" + id, params);
    assertThat(jdbc.queryForMap("SELECT * FROM m_partner WHERE partner_id = ?", id))
        .containsEntry("partner_code", "C001")
        .containsEntry("is_customer", false)
        .containsEntry("closing_day", null);
    assertThat(jdbc.queryForObject("SELECT detail FROM t_operation_log", String.class))
        .contains("区分：得意先→仕入先", "締日・支払条件：末日締め 翌月末日払い→");

    int status =
        mvc.perform(
                get("/master/partners/new")
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user(
                                TestUsers.of(Role.WAREHOUSE))))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isEqualTo(403);
  }
}
