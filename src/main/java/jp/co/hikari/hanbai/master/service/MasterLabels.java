package jp.co.hikari.hanbai.master.service;

import jp.co.hikari.hanbai.master.model.Partner;

/** マスタの表示名（画面の一覧・操作ログの操作内容で共通に使う）。 */
public final class MasterLabels {

  /** 締日・支払日の「末日」を表す値（コード定義書 11章）。 */
  public static final int END_OF_MONTH = 99;

  private static final String[] MONTH_OFFSETS = {"当月", "翌月", "翌々月"};

  private MasterLabels() {}

  /**
   * 有効フラグの表示名を返す。
   *
   * @param enabled 有効フラグ
   * @return 有効 または 無効
   */
  public static String enabled(boolean enabled) {
    return enabled ? "有効" : "無効";
  }

  /**
   * 締日・支払日の表示名を返す。
   *
   * @param day 1～28、99＝末日
   * @return 例：20日、末日（null は null）
   */
  public static String day(Integer day) {
    if (day == null) {
      return null;
    }
    return day == END_OF_MONTH ? "末日" : day + "日";
  }

  /**
   * 支払月区分の表示名を返す。
   *
   * @param offset 0＝当月、1＝翌月、2＝翌々月
   * @return 例：翌月（範囲外・null は null）
   */
  public static String monthOffset(Integer offset) {
    if (offset == null || offset < 0 || offset >= MONTH_OFFSETS.length) {
      return null;
    }
    return MONTH_OFFSETS[offset];
  }

  /**
   * 得意先の締日・支払条件の表示名を返す。
   *
   * @param partner 取引先
   * @return 例：末日締め 翌月末日払い（得意先でない場合は空文字）
   */
  public static String terms(Partner partner) {
    if (!partner.isCustomer() || partner.getClosingDay() == null) {
      return "";
    }
    return day(partner.getClosingDay())
        + "締め "
        + monthOffset(partner.getPaymentMonthOffset())
        + day(partner.getPaymentDay())
        + "払い";
  }

  /**
   * 取引先区分の表示名を返す。
   *
   * @param partner 取引先
   * @return 例：得意先・仕入先
   */
  public static String partyType(Partner partner) {
    if (partner.isCustomer() && partner.isSupplier()) {
      return "得意先・仕入先";
    }
    return partner.isCustomer() ? "得意先" : "仕入先";
  }
}
