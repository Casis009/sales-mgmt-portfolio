package jp.co.hikari.hanbai.master.dto;

/**
 * 倉庫の登録・更新の内容（SCR-112）。
 *
 * @param warehouseId 倉庫ID（登録時は null）
 * @param version バージョン（登録時は 0）
 * @param warehouseCode 倉庫コード（更新時は変更しない）
 * @param warehouseName 倉庫名
 * @param postalCode 郵便番号
 * @param address 住所
 * @param tel 電話番号
 * @param enabled 有効フラグ
 */
public record WarehouseCommand(
    Long warehouseId,
    int version,
    String warehouseCode,
    String warehouseName,
    String postalCode,
    String address,
    String tel,
    boolean enabled) {}
