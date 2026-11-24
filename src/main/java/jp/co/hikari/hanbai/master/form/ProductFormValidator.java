package jp.co.hikari.hanbai.master.form;

import java.util.ArrayList;
import java.util.List;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.master.dto.ProductCommand;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * SCR-102 の倉庫ごとの発注点・標準発注数の検査と変換（F-104。画面一覧 5.4）。
 *
 * <ul>
 *   <li>発注点・標準発注数とも空欄：その倉庫は発注点チェックの対象外（行を作らない）
 *   <li>発注点のみ入力：標準発注数は 0
 *   <li>標準発注数のみ入力：発注点を必須とする（E-001）
 *   <li>整数でない場合は E-004、0～999,999 の範囲外は E-003
 * </ul>
 */
@Component
public class ProductFormValidator {

  /** 発注点・標準発注数の上限（数量の上限と同じ）。 */
  public static final int MAX_QUANTITY = 999_999;

  private static final String DIGITS = "[0-9]+";
  private static final int MAX_DIGITS = 7;

  /**
   * 倉庫ごとの設定を検査し、数値に変換する。エラーは明細行の項目に設定する。
   *
   * @param form 入力
   * @param errors 入力チェックの結果
   * @return 変換した設定（エラーがある行は含まない）
   */
  public List<ProductCommand.Setting> validate(ProductForm form, Errors errors) {
    List<ProductCommand.Setting> result = new ArrayList<>();
    for (int i = 0; i < form.getSettings().size(); i++) {
      ReorderSettingForm row = form.getSettings().get(i);
      if (row.getWarehouseId() == null) {
        continue;
      }
      String prefix = "settings[" + i + "].";
      String where = row.getWarehouseLabel() == null ? "" : "（" + row.getWarehouseLabel() + "）";
      Integer point = parse(errors, prefix + "reorderPoint", "発注点" + where, row.getReorderPoint());
      Integer quantity =
          parse(
              errors,
              prefix + "standardOrderQuantity",
              "標準発注数" + where,
              row.getStandardOrderQuantity());
      if (errors.hasFieldErrors(prefix + "reorderPoint")
          || errors.hasFieldErrors(prefix + "standardOrderQuantity")) {
        continue;
      }
      if (point == null && quantity != null) {
        errors.rejectValue(
            prefix + "reorderPoint", MessageId.E_REQUIRED, new Object[] {"発注点" + where}, null);
        continue;
      }
      result.add(new ProductCommand.Setting(row.getWarehouseId(), point, quantity));
    }
    return result;
  }

  /** 空欄は null。整数でなければ E-004、範囲外は E-003 をエラーに設定して null を返す。 */
  private static Integer parse(Errors errors, String field, String label, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String text = value.strip();
    if (!text.matches(DIGITS) || text.length() > MAX_DIGITS) {
      if (text.matches(DIGITS)) {
        rejectRange(errors, field, label);
      } else {
        errors.rejectValue(field, MessageId.E_FORMAT, new Object[] {label}, null);
      }
      return null;
    }
    int number = Integer.parseInt(text);
    if (number > MAX_QUANTITY) {
      rejectRange(errors, field, label);
      return null;
    }
    return number;
  }

  private static void rejectRange(Errors errors, String field, String label) {
    errors.rejectValue(field, MessageId.E_RANGE, new Object[] {label, 0, MAX_QUANTITY}, null);
  }
}
