package jp.co.hikari.hanbai.master.dto;

import java.util.List;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.master.model.User;

/**
 * ユーザー編集画面（SCR-012）に表示するユーザーとロール。
 *
 * @param user ユーザー
 * @param roles ロール
 */
public record UserDetail(User user, List<Role> roles) {}
