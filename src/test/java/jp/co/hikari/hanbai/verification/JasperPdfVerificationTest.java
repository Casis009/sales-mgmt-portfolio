package jp.co.hikari.hanbai.verification;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
 * 技術検証 P-02：JasperReports 7 で、IPAexゴシックを埋め込んだ日本語 PDF を出力できること。
 *
 * <p>自動で確認すること：① PDF から抽出した文字列が入力と一致する（文字化けしない） ② フォントが PDF に埋め込まれている。
 * 見た目（字形・レイアウト）は出力した {@code target/verification/P-02_japanese.pdf} を目視で確認する。
 */
class JasperPdfVerificationTest {

  static final Path OUTPUT = Path.of("target", "verification", "P-02_japanese.pdf");

  // \u3000 は全角スペース（Checkstyle の規約により、ASCII 以外の空白はエスケープして書く）
  static final String CUSTOMER = "株式会社ひかり電設\u3000御中";
  static final String MESSAGE = "下記のとおりご請求申し上げます。髙橋様・﨑山様（異体字の確認）";
  static final String SYMBOLS = "全角記号：（）「」【】〒※①②③\u3000半角カナ：ｶﾞｲﾄﾞ\u3000ハイフン：－ー‐";
  static final String JRXML = "/reports/verification/p02_japanese.jrxml";
  static final String AMOUNT = "￥1,234,567-";

  @Test
  void 日本語を含むPDFを出力でき_文字化けせずフォントが埋め込まれる() throws Exception {
    JasperReport report;
    try (InputStream in = getClass().getResourceAsStream(JRXML)) {
      report = JasperCompileManager.compileReport(in);
    }
    Map<String, Object> params = new HashMap<>();
    params.put("customerName", CUSTOMER);
    params.put("message", MESSAGE);
    params.put("symbols", SYMBOLS);
    params.put("amount", AMOUNT);
    JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());

    Files.createDirectories(OUTPUT.getParent());
    JasperExportManager.exportReportToPdfFile(print, OUTPUT.toString());

    PdfReader reader = new PdfReader(OUTPUT.toString());
    try {
      // 抽出時に全角スペースの前へ半角スペースが入るため、空白を除いて比較する
      String text = removeSpaces(new PdfTextExtractor(reader).getTextFromPage(1));
      assertThat(text).contains("請求書");
      assertThat(text).contains(removeSpaces(CUSTOMER), removeSpaces(MESSAGE));
      assertThat(text).contains("髙橋", "﨑山");
      assertThat(text).contains("〒", "※", "①", "ｶﾞｲﾄﾞ");
      assertThat(text).contains("1,234,567");
      assertThat(embeddedFontNames(reader)).anyMatch(name -> name.contains("IPAexGothic"));
    } finally {
      reader.close();
    }
  }

  private static String removeSpaces(String s) {
    return s.replaceAll("[\\s\\u3000]", "");
  }

  /** 1ページ目で使用しているフォントのうち、埋め込まれているもの（FontFile2 を持つもの）の名前を返す。 */
  private static java.util.List<String> embeddedFontNames(PdfReader reader) {
    java.util.List<String> names = new java.util.ArrayList<>();
    PdfDictionary resources = reader.getPageN(1).getAsDict(PdfName.RESOURCES);
    PdfDictionary fonts = resources.getAsDict(PdfName.FONT);
    for (PdfName key : fonts.getKeys()) {
      PdfDictionary font = fonts.getAsDict(key);
      // Identity-H の場合は Type0 フォントの下位フォント（DescendantFonts）に記述子がある
      PdfDictionary descendant =
          font.getAsArray(PdfName.DESCENDANTFONTS) == null
              ? font
              : font.getAsArray(PdfName.DESCENDANTFONTS).getAsDict(0);
      PdfDictionary descriptor = descendant.getAsDict(PdfName.FONTDESCRIPTOR);
      if (descriptor != null && descriptor.get(PdfName.FONTFILE2) != null) {
        names.add(String.valueOf(font.get(PdfName.BASEFONT)));
      }
    }
    return names;
  }
}
