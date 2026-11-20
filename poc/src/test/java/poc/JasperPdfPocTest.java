package poc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

/**
 * P-02：JasperReports で日本語（IPAexゴシック）を含む PDF を文字化けなく出力できること。
 *
 * <p>未実行の草案。出力された target/poc.pdf を目視で確認する（自動テストでは文字化けを判定できないため）。
 * JasperReports 7 は jrxml の形式が 6 以前と異なる。読み込みでエラーになる場合は、 Jaspersoft Studio 7 で
 * poc.jrxml を作り直す。
 */
class JasperPdfPocTest {

  @Test
  void 日本語を含むPDFを出力できる() throws Exception {
    JasperReport report;
    try (InputStream in = getClass().getResourceAsStream("/reports/poc.jrxml")) {
      report = JasperCompileManager.compileReport(in);
    }

    Map<String, Object> params = new HashMap<>();
    params.put("customerName", "株式会社ひかり電設　御中");
    params.put("message", "下記のとおりご請求申し上げます。髙橋・﨑山（外字の確認）");
    params.put("amount", "￥1,234,567-");

    JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());

    Path out = Path.of("target", "poc.pdf");
    Files.createDirectories(out.getParent());
    JasperExportManager.exportReportToPdfFile(print, out.toString());

    assertThat(Files.size(out)).isGreaterThan(1000L);
  }
}
