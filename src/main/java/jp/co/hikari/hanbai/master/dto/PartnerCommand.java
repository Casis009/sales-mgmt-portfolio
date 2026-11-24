package jp.co.hikari.hanbai.master.dto;

/**
 * 取引先の登録・更新の内容（SCR-122）。得意先でない場合、締日・支払条件は null とする。
 *
 * @param partnerId 取引先ID（登録時は null）
 * @param version バージョン（登録時は 0）
 * @param partnerCode 取引先コード（更新時は変更しない）
 * @param partnerName 取引先名
 * @param partnerNameKana 取引先名カナ
 * @param customer 得意先の場合 true
 * @param supplier 仕入先の場合 true
 * @param postalCode 郵便番号
 * @param address 住所
 * @param tel 電話番号
 * @param fax FAX番号
 * @param contactName 担当者名
 * @param closingDay 締日
 * @param paymentMonthOffset 支払月区分
 * @param paymentDay 支払日
 * @param enabled 有効フラグ
 */
public record PartnerCommand(
    Long partnerId,
    int version,
    String partnerCode,
    String partnerName,
    String partnerNameKana,
    boolean customer,
    boolean supplier,
    String postalCode,
    String address,
    String tel,
    String fax,
    String contactName,
    Integer closingDay,
    Integer paymentMonthOffset,
    Integer paymentDay,
    boolean enabled) {}
