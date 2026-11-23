package jp.co.hikari.hanbai.master.dto;

/**
 * 画面の倉庫の選択肢。
 *
 * @param warehouseId 倉庫ID
 * @param warehouseCode 倉庫コード
 * @param warehouseName 倉庫名
 */
public record WarehouseOption(long warehouseId, String warehouseCode, String warehouseName) {}
