package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jp.co.hikari.hanbai.common.form.InputPatterns;
import jp.co.hikari.hanbai.master.dto.PartnerCommand;

/**
 * SCR-122 取引先登録・編集の入力。取引先コードは登録時のみ入力でき、編集時は変更しない。
 *
 * <p>区分（得意先・仕入先のいずれか）と、得意先の締日・支払条件は相関チェック（{@link PartnerFormValidator}）で確認する。
 */
public class PartnerForm {

  private Long partnerId;

  private int version;

  @NotBlank
  @Size(max = 10)
  @Pattern(regexp = InputPatterns.CODE)
  private String partnerCode;

  @NotBlank
  @Size(max = 100)
  private String partnerName;

  @Size(max = 100)
  @Pattern(regexp = InputPatterns.KANA)
  private String partnerNameKana;

  private boolean customer;

  private boolean supplier;

  @Pattern(regexp = InputPatterns.POSTAL_CODE)
  private String postalCode;

  @Size(max = 200)
  private String address;

  @Size(max = 20)
  @Pattern(regexp = InputPatterns.TEL)
  private String tel;

  @Size(max = 20)
  @Pattern(regexp = InputPatterns.TEL)
  private String fax;

  @Size(max = 50)
  private String contactName;

  private Integer closingDay;

  private Integer paymentMonthOffset;

  private Integer paymentDay;

  private boolean enabled = true;

  /**
   * Service に渡す登録・更新の内容に変換する。
   *
   * @return 登録・更新の内容
   */
  public PartnerCommand toCommand() {
    return new PartnerCommand(
        partnerId,
        version,
        partnerCode,
        partnerName,
        partnerNameKana,
        customer,
        supplier,
        postalCode,
        address,
        tel,
        fax,
        contactName,
        closingDay,
        paymentMonthOffset,
        paymentDay,
        enabled);
  }

  /**
   * 新規登録かを返す。
   *
   * @return 取引先IDが未設定の場合 true
   */
  public boolean isNew() {
    return partnerId == null;
  }

  /**
   * 取引先IDを返す。
   *
   * @return 取引先ID
   */
  public Long getPartnerId() {
    return partnerId;
  }

  /**
   * 取引先IDを設定する。
   *
   * @param partnerId 取引先ID
   */
  public void setPartnerId(Long partnerId) {
    this.partnerId = partnerId;
  }

  /**
   * バージョンを返す。
   *
   * @return バージョン
   */
  public int getVersion() {
    return version;
  }

  /**
   * バージョンを設定する。
   *
   * @param version バージョン
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * 取引先コードを返す。
   *
   * @return 取引先コード
   */
  public String getPartnerCode() {
    return partnerCode;
  }

  /**
   * 取引先コードを設定する。
   *
   * @param partnerCode 取引先コード
   */
  public void setPartnerCode(String partnerCode) {
    this.partnerCode = partnerCode;
  }

  /**
   * 取引先名を返す。
   *
   * @return 取引先名
   */
  public String getPartnerName() {
    return partnerName;
  }

  /**
   * 取引先名を設定する。
   *
   * @param partnerName 取引先名
   */
  public void setPartnerName(String partnerName) {
    this.partnerName = partnerName;
  }

  /**
   * 取引先名カナを返す。
   *
   * @return 取引先名カナ
   */
  public String getPartnerNameKana() {
    return partnerNameKana;
  }

  /**
   * 取引先名カナを設定する。
   *
   * @param partnerNameKana 取引先名カナ
   */
  public void setPartnerNameKana(String partnerNameKana) {
    this.partnerNameKana = partnerNameKana;
  }

  /**
   * 得意先フラグを返す。
   *
   * @return 得意先の場合 true
   */
  public boolean isCustomer() {
    return customer;
  }

  /**
   * 得意先フラグを設定する。
   *
   * @param customer 得意先の場合 true
   */
  public void setCustomer(boolean customer) {
    this.customer = customer;
  }

  /**
   * 仕入先フラグを返す。
   *
   * @return 仕入先の場合 true
   */
  public boolean isSupplier() {
    return supplier;
  }

  /**
   * 仕入先フラグを設定する。
   *
   * @param supplier 仕入先の場合 true
   */
  public void setSupplier(boolean supplier) {
    this.supplier = supplier;
  }

  /**
   * 郵便番号を返す。
   *
   * @return 郵便番号
   */
  public String getPostalCode() {
    return postalCode;
  }

  /**
   * 郵便番号を設定する。
   *
   * @param postalCode 郵便番号
   */
  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  /**
   * 住所を返す。
   *
   * @return 住所
   */
  public String getAddress() {
    return address;
  }

  /**
   * 住所を設定する。
   *
   * @param address 住所
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * 電話番号を返す。
   *
   * @return 電話番号
   */
  public String getTel() {
    return tel;
  }

  /**
   * 電話番号を設定する。
   *
   * @param tel 電話番号
   */
  public void setTel(String tel) {
    this.tel = tel;
  }

  /**
   * FAX番号を返す。
   *
   * @return FAX番号
   */
  public String getFax() {
    return fax;
  }

  /**
   * FAX番号を設定する。
   *
   * @param fax FAX番号
   */
  public void setFax(String fax) {
    this.fax = fax;
  }

  /**
   * 担当者名を返す。
   *
   * @return 担当者名
   */
  public String getContactName() {
    return contactName;
  }

  /**
   * 担当者名を設定する。
   *
   * @param contactName 担当者名
   */
  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  /**
   * 締日を返す。
   *
   * @return 締日
   */
  public Integer getClosingDay() {
    return closingDay;
  }

  /**
   * 締日を設定する。
   *
   * @param closingDay 締日
   */
  public void setClosingDay(Integer closingDay) {
    this.closingDay = closingDay;
  }

  /**
   * 支払月区分を返す。
   *
   * @return 支払月区分
   */
  public Integer getPaymentMonthOffset() {
    return paymentMonthOffset;
  }

  /**
   * 支払月区分を設定する。
   *
   * @param paymentMonthOffset 支払月区分
   */
  public void setPaymentMonthOffset(Integer paymentMonthOffset) {
    this.paymentMonthOffset = paymentMonthOffset;
  }

  /**
   * 支払日を返す。
   *
   * @return 支払日
   */
  public Integer getPaymentDay() {
    return paymentDay;
  }

  /**
   * 支払日を設定する。
   *
   * @param paymentDay 支払日
   */
  public void setPaymentDay(Integer paymentDay) {
    this.paymentDay = paymentDay;
  }

  /**
   * 有効フラグを返す。
   *
   * @return 有効フラグ
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * 有効フラグを設定する。
   *
   * @param enabled 有効フラグ
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
