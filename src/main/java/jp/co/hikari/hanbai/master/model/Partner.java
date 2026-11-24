package jp.co.hikari.hanbai.master.model;

/** 取引先（TBL-103 m_partner）の1行。得意先と仕入先をフラグで区別する（ER図 設計方針 No.3）。 */
public class Partner {

  private Long partnerId;
  private String partnerCode;
  private String partnerName;
  private String partnerNameKana;
  private boolean customer;
  private boolean supplier;
  private String postalCode;
  private String address;
  private String tel;
  private String fax;
  private String contactName;
  private Integer closingDay;
  private Integer paymentMonthOffset;
  private Integer paymentDay;
  private boolean enabled;
  private int version;

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
   * @return 1～28、99＝末日（得意先以外は null）
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
   * @return 0＝当月、1＝翌月、2＝翌々月（得意先以外は null）
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
   * @return 1～28、99＝末日（得意先以外は null）
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
}
