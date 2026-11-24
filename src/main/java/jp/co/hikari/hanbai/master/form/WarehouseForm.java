package jp.co.hikari.hanbai.master.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jp.co.hikari.hanbai.common.form.InputPatterns;
import jp.co.hikari.hanbai.master.dto.WarehouseCommand;

/** SCR-112 倉庫登録・編集の入力。倉庫コードは登録時のみ入力でき、編集時は変更しない。 */
public class WarehouseForm {

  private Long warehouseId;

  private int version;

  @NotBlank
  @Size(max = 10)
  @Pattern(regexp = InputPatterns.CODE)
  private String warehouseCode;

  @NotBlank
  @Size(max = 50)
  private String warehouseName;

  @Pattern(regexp = InputPatterns.POSTAL_CODE)
  private String postalCode;

  @Size(max = 200)
  private String address;

  @Size(max = 20)
  @Pattern(regexp = InputPatterns.TEL)
  private String tel;

  private boolean enabled = true;

  /**
   * Service に渡す登録・更新の内容に変換する。
   *
   * @return 登録・更新の内容
   */
  public WarehouseCommand toCommand() {
    return new WarehouseCommand(
        warehouseId, version, warehouseCode, warehouseName, postalCode, address, tel, enabled);
  }

  /**
   * 新規登録かを返す。
   *
   * @return 倉庫IDが未設定の場合 true
   */
  public boolean isNew() {
    return warehouseId == null;
  }

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
}
