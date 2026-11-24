package jp.co.hikari.hanbai.common.operationlog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 操作ログの「操作内容」に記録する、主要な項目の変更前後（共通処理設計 7.1）。
 *
 * <p>変更のない項目は記録しない。例：「品名：LED電球→LED電球 60W形、発注点（HQ）：10→20」
 */
public final class ChangeDetail {

  private static final String NONE = "なし";

  private final List<String> items = new ArrayList<>();

  /**
   * 項目の変更前後を追加する。値が同じ場合は追加しない（金額は小数点以下の桁数の違いを無視する）。
   *
   * @param name 項目名
   * @param before 変更前
   * @param after 変更後
   * @return このオブジェクト
   */
  public ChangeDetail change(String name, Object before, Object after) {
    if (!same(before, after)) {
      items.add(name + "：" + text(before) + "→" + text(after));
    }
    return this;
  }

  /**
   * 変更前後の形にならない操作（例：ロック解除）を追加する。
   *
   * @param text 内容
   * @return このオブジェクト
   */
  public ChangeDetail note(String text) {
    items.add(text);
    return this;
  }

  /**
   * 記録する文字列を返す。
   *
   * @return 項目を「、」で連結した文字列。変更がない場合は null
   */
  public String build() {
    return items.isEmpty() ? null : String.join("、", items);
  }

  private static boolean same(Object before, Object after) {
    if (before instanceof BigDecimal b && after instanceof BigDecimal a) {
      return b.compareTo(a) == 0;
    }
    return Objects.equals(before, after);
  }

  private static String text(Object value) {
    if (value instanceof BigDecimal d) {
      return d.toPlainString();
    }
    return Objects.toString(value, NONE);
  }
}
