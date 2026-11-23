package jp.co.hikari.hanbai.common.security;

import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 認証・パスワード変更に使う m_user の SQL（F-001、F-002）。
 *
 * <p>ユーザーの登録・編集（F-003）は master の UserMapper が担当する。
 * 本 Mapper は認証に関わる項目（失敗回数・ロック・最終ログイン日時・パスワード）のみを扱う。
 */
@Mapper
public interface LoginUserMapper {

  /**
   * ログインIDでユーザーとロールを取得する。
   *
   * @param loginId ログインID
   * @return ユーザー
   */
  Optional<LoginUserRow> findByLoginId(@Param("loginId") String loginId);

  /**
   * ログインIDでユーザーを行ロックして取得する（失敗回数の加算を直列化する）。ロールは取得しない。
   *
   * @param loginId ログインID
   * @return ユーザー
   */
  Optional<LoginUserRow> findByLoginIdForUpdate(@Param("loginId") String loginId);

  /**
   * ユーザーIDでユーザーを行ロックして取得する。ロールは取得しない。
   *
   * @param userId ユーザーID
   * @return ユーザー
   */
  Optional<LoginUserRow> findByIdForUpdate(@Param("userId") long userId);

  /**
   * 認証成功を記録する（失敗回数を 0、最終ログイン日時を更新）。
   *
   * @param userId ユーザーID
   * @param now 現在日時
   * @return 更新件数
   */
  int recordSuccess(@Param("userId") long userId, @Param("now") LocalDateTime now);

  /**
   * 認証失敗を記録する。
   *
   * @param userId ユーザーID
   * @param failedLoginCount 加算後の失敗回数
   * @param locked ロックする場合 true
   * @return 更新件数
   */
  int recordFailure(
      @Param("userId") long userId,
      @Param("failedLoginCount") int failedLoginCount,
      @Param("locked") boolean locked);

  /**
   * パスワードを変更し、パスワード変更要求フラグを解除する。
   *
   * @param userId ユーザーID
   * @param passwordHash 新しいパスワードハッシュ
   * @param now 現在日時
   * @return 更新件数
   */
  int updatePassword(
      @Param("userId") long userId,
      @Param("passwordHash") String passwordHash,
      @Param("now") LocalDateTime now);
}
