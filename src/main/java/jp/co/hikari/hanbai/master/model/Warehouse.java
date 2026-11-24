package jp.co.hikari.hanbai.master.model;

/** 倉庫（TBL-102 m_warehouse）の1行。 */
public class Warehouse {

  private Long warehouseId;
  private String warehouseCode;
  private String warehouseName;
  private String postalCode;
  private String address;
  private String tel;
  private boolean enabled;
  private int version;

  /**
   * 倉庫IDを返す。
   *
   * @return 倉庫ID
   */
  public Long getWarehouseId() {
    return warehouseId;
  }

  /**
   * 倉庫IDを設定する。
   *
   * @param warehouseId 倉庫ID
   */
  public void setWarehouseId(Long warehouseId) {
    this.warehouseId = warehouseId;
  }

  /**
   * 倉庫コードを返す。
   *
   * @return 倉庫コード
   */
  public String getWarehouseCode() {
    return warehouseCode;
  }

  /**
   * 倉庫コードを設定する。
   *
   * @param warehouseCode 倉庫コード
   */
  public void setWarehouseCode(String warehouseCode) {
    this.warehouseCode = warehouseCode;
  }

  /**
   * 倉庫名を返す。
   *
   * @return 倉庫名
   */
  public String getWarehouseName() {
    return warehouseName;
  }

  /**
   * 倉庫名を設定する。
   *
   * @param warehouseName 倉庫名
   */
  public void setWarehouseName(String warehouseName) {
    this.warehouseName = warehouseName;
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
