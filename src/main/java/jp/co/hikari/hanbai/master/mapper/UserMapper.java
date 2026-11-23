package jp.co.hikari.hanbai.master.mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.master.dto.UserCriteria;
import jp.co.hikari.hanbai.master.dto.UserRole;
import jp.co.hikari.hanbai.master.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** ユーザー（m_user）・ユーザーロール（m_user_role）の SQL（F-003）。 */
@Mapper
public interface UserMapper {

  /**
   * 登録済みのユーザーの件数を返す（初期管理者の作成要否の判定）。
   *
   * @return 件数
   */
  long countAll();

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  long count(@Param("c") UserCriteria criteria);

  /**
   * 検索条件に該当するユーザーを、ログインIDの昇順で返す。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return ユーザー
   */
  List<User> search(
      @Param("c") UserCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") long offset);

  /**
   * 指定のユーザーのロールを返す。
   *
   * @param userIds ユーザーID（1件以上）
   * @return ロール
   */
  List<UserRole> findRoles(@Param("userIds") Collection<Long> userIds);

  /**
   * ユーザーを取得する。
   *
   * @param userId ユーザーID
   * @return ユーザー
   */
  Optional<User> findById(@Param("userId") long userId);

  /**
   * ログインIDが登録済みかを返す。
   *
   * @param loginId ログインID
   * @return 登録済みの場合 true
   */
  boolean existsByLoginId(@Param("loginId") String loginId);

  /**
   * ユーザーを登録する。採番したユーザーIDを {@code user.userId} に設定する。
   *
   * @param user ユーザー
   * @param passwordHash 初期パスワードのハッシュ
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insert(
      @Param("u") User user,
      @Param("passwordHash") String passwordHash,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * ユーザーを更新する（楽観的排他。共通処理設計 2.2）。
   *
   * @param user ユーザー（version は画面表示時の値）
   * @param unlock ロックを解除する場合 true（失敗回数も 0 に戻す）
   * @param now 現在日時
   * @param operatorId 更新者
   * @return 更新件数（version が一致しない場合は 0）
   */
  int update(
      @Param("u") User user,
      @Param("unlock") boolean unlock,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);

  /**
   * 指定のロールを持つ有効なユーザーを、ユーザーIDの昇順に行ロックして返す。
   *
   * <p>システム管理者が0人になる変更を、同時に実行された別の変更とすれ違わずに判定するために使う。
   *
   * @param role ロール
   * @return ユーザーID
   */
  List<Long> lockEnabledUsersWithRole(@Param("role") Role role);

  /**
   * ユーザーのロールをすべて削除する。
   *
   * @param userId ユーザーID
   * @return 削除件数
   */
  int deleteRoles(@Param("userId") long userId);

  /**
   * ユーザーにロールを付与する。
   *
   * @param userId ユーザーID
   * @param role ロール
   * @param now 現在日時
   * @param operatorId 登録者
   * @return 登録件数
   */
  int insertRole(
      @Param("userId") long userId,
      @Param("role") Role role,
      @Param("now") LocalDateTime now,
      @Param("operatorId") long operatorId);
}
