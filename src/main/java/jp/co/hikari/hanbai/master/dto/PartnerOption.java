package jp.co.hikari.hanbai.master.dto;

/**
 * 画面の取引先（仕入先）の選択肢。
 *
 * @param partnerId 取引先ID
 * @param partnerCode 取引先コード
 * @param partnerName 取引先名
 * @param selectable 新しく選択できる場合 true（有効な仕入先）。false は現在の値として表示する場合のみ
 */
public record PartnerOption(
    long partnerId, String partnerCode, String partnerName, boolean selectable) {

  /**
   * 選択肢の表示名を返す。
   *
   * @return 例：S001 株式会社〇〇電機（選択できない場合は末尾に「（無効）」）
   */
  public String label() {
    return partnerCode + " " + partnerName + (selectable ? "" : "（無効）");
  }
}
