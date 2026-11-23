package jp.co.hikari.hanbai.master.dto;

import jp.co.hikari.hanbai.common.code.Role;

/**
 * ユーザーロール（TBL-002 m_user_role）の1行。
 *
 * @param userId ユーザーID
 * @param roleCode ロール
 */
public record UserRole(long userId, Role roleCode) {}
