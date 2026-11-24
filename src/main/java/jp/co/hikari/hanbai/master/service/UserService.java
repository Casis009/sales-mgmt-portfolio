package jp.co.hikari.hanbai.master.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.OptimisticLockException;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.operationlog.ChangeDetail;
import jp.co.hikari.hanbai.common.operationlog.OperationLog;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import jp.co.hikari.hanbai.common.operationlog.OperationLogTarget;
import jp.co.hikari.hanbai.common.operationlog.OperationLogged;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.master.dto.UserCommand;
import jp.co.hikari.hanbai.master.dto.UserCriteria;
import jp.co.hikari.hanbai.master.dto.UserDetail;
import jp.co.hikari.hanbai.master.dto.UserListRow;
import jp.co.hikari.hanbai.master.dto.UserRole;
import jp.co.hikari.hanbai.master.mapper.UserMapper;
import jp.co.hikari.hanbai.master.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザー管理（F-003、SCR-011・SCR-012）。ユーザーの登録・変更・無効化、権限の付与、ロック解除を行う。
 *
 * <p>更新処理はシステム管理者のみが実行できる（権限マトリクス 2.1。セキュリティ設計 3章 ②）。
 * ユーザーは物理削除せず、無効にする（画面一覧 5.2 No.12）。
 */
@Service
public class UserService {

  /** 操作ログの画面ID。 */
  public static final String SCREEN_ID = "SCR-012";

  /** システム（初期管理者の作成）の操作者ID（テーブル定義書 0章：バッチ・移行は 0）。 */
  public static final long SYSTEM_USER_ID = 0L;

  private static final String TARGET_TYPE = "m_user";
  private static final String INITIAL_ADMIN_NAME = "システム管理者";

  private final UserMapper mapper;
  private final WarehouseService warehouseService;
  private final OperationLogService operationLogService;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper ユーザーの SQL
   * @param warehouseService 倉庫マスタ
   * @param operationLogService 操作ログ
   * @param passwordEncoder パスワードのハッシュ化
   * @param clock 時計
   */
  public UserService(
      UserMapper mapper,
      WarehouseService warehouseService,
      OperationLogService operationLogService,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.mapper = mapper;
    this.warehouseService = warehouseService;
    this.operationLogService = operationLogService;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * 検索条件に該当する件数を返す。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  @Transactional(readOnly = true)
  public long count(UserCriteria criteria) {
    return mapper.count(criteria);
  }

  /**
   * ユーザーを検索する（ログインIDの昇順）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 一覧の行
   */
  @Transactional(readOnly = true)
  public List<UserListRow> search(UserCriteria criteria, int limit, long offset) {
    List<User> users = mapper.search(criteria, limit, offset);
    if (users.isEmpty()) {
      return List.of();
    }
    Map<Long, List<Role>> roles = rolesOf(users.stream().map(User::getUserId).toList());
    return users.stream()
        .map(
            u ->
                new UserListRow(
                        u.getUserId(),
                        u.getLoginId(),
                        u.getUserName(),
                        u.getDepartment(),
                        u.isEnabled(),
                        u.isLocked(),
                        u.getLastLoginAt(),
                        List.of())
                    .withRoles(roles.getOrDefault(u.getUserId(), List.of())))
        .toList();
  }

  /**
   * 編集画面に表示するユーザーを取得する。
   *
   * @param userId ユーザーID
   * @return ユーザーとロール
   */
  @Transactional(readOnly = true)
  public Optional<UserDetail> find(long userId) {
    return mapper.findById(userId).map(u -> new UserDetail(u, rolesOf(userId)));
  }

  /**
   * ユーザーを登録する。初期パスワードのため、初回ログイン時にパスワード変更を求める。
   *
   * @param command 登録内容（初期パスワードは入力チェックで条件を確認済み）
   * @param operator 操作者
   * @return 操作ログの対象（ログインID、権限など）
   * @throws BusinessException ログインIDが登録済み（E-005）、所属倉庫が無効（E-006）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @OperationLogged(type = OperationType.CREATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget create(UserCommand command, LoginUser operator) {
    if (mapper.existsByLoginId(command.loginId())) {
      throw duplicateLoginId();
    }
    checkWarehouse(command.defaultWarehouseId());
    User user = new User();
    user.setLoginId(command.loginId());
    apply(command, user);
    LocalDateTime now = LocalDateTime.now(clock);
    String passwordHash = passwordEncoder.encode(command.initialPassword());
    try {
      mapper.insert(user, passwordHash, now, operator.getUserId());
    } catch (DuplicateKeyException e) {
      // 同時に同じログインIDが登録された場合（存在確認とすれ違い）
      throw duplicateLoginId();
    }
    insertRoles(user.getUserId(), command.roles(), now, operator.getUserId());
    String detail = "権限：" + labels(command.roles()) + "、状態：" + enabledLabel(command.enabled());
    return OperationLogTarget.of(command.loginId(), detail);
  }

  /**
   * ユーザーを更新する。ログインIDは変更しない。ロック解除を指定した場合は、ロックと認証失敗回数を解除する。
   *
   * @param command 更新内容（version は画面表示時の値）
   * @param operator 操作者
   * @return 操作ログの対象（ログインID、変更内容）
   * @throws BusinessException 他の利用者が先に更新した（E-020）、所属倉庫が無効（E-006）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @OperationLogged(type = OperationType.UPDATE, screenId = SCREEN_ID, targetType = TARGET_TYPE)
  @Transactional
  public OperationLogTarget update(UserCommand command, LoginUser operator) {
    User before = mapper.findById(command.userId()).orElseThrow(OptimisticLockException::new);
    // 変更前のロールは、ロールを置き換える前に取得しておく（操作内容の記録に使う）
    final List<Role> rolesBefore = rolesOf(before.getUserId());
    // 所属倉庫を変えない場合は、その倉庫が後から無効になっていても更新できるようにする
    if (!java.util.Objects.equals(before.getDefaultWarehouseId(), command.defaultWarehouseId())) {
      checkWarehouse(command.defaultWarehouseId());
    }
    checkAdminRemains(before, rolesBefore, command, operator);

    User after = new User();
    after.setUserId(before.getUserId());
    after.setLoginId(before.getLoginId());
    after.setVersion(command.version());
    apply(command, after);
    boolean unlock = command.unlock() && before.isLocked();
    LocalDateTime now = LocalDateTime.now(clock);
    if (mapper.update(after, unlock, now, operator.getUserId()) == 0) {
      throw new OptimisticLockException();
    }
    mapper.deleteRoles(before.getUserId());
    insertRoles(before.getUserId(), command.roles(), now, operator.getUserId());
    return OperationLogTarget.of(
        before.getLoginId(), changes(before, rolesBefore, command, unlock));
  }

  /**
   * ユーザーが1人も登録されていない場合に、初期のシステム管理者を作成する（移行時の最初のログイン用）。
   *
   * <p>初期パスワードのため、初回ログイン時にパスワード変更を求める。以降のユーザーは画面から登録する（データ移行方針）。
   *
   * @param loginId ログインID
   * @param rawPassword 初期パスワード
   * @return 作成した場合 true（既にユーザーが存在する場合 false）
   */
  @Transactional
  public boolean createInitialAdminIfNoUsers(String loginId, String rawPassword) {
    if (mapper.countAll() > 0) {
      return false;
    }
    User admin = new User();
    admin.setLoginId(loginId);
    admin.setUserName(INITIAL_ADMIN_NAME);
    admin.setEnabled(true);
    LocalDateTime now = LocalDateTime.now(clock);
    mapper.insert(admin, passwordEncoder.encode(rawPassword), now, SYSTEM_USER_ID);
    mapper.insertRole(admin.getUserId(), Role.ADMIN, now, SYSTEM_USER_ID);

    OperationLog log = OperationLog.of(OperationType.CREATE, OperationResult.SUCCESS);
    log.setTargetType(TARGET_TYPE);
    log.setTargetKey(loginId);
    log.setDetail("初期管理者の作成（システム起動時）");
    operationLogService.record(log);
    return true;
  }

  private void apply(UserCommand command, User user) {
    user.setUserName(command.userName());
    user.setDepartment(command.department());
    user.setDefaultWarehouseId(command.defaultWarehouseId());
    user.setEnabled(command.enabled());
  }

  private void checkWarehouse(Long warehouseId) {
    if (warehouseId != null && !warehouseService.isEnabled(warehouseId)) {
      throw new BusinessException(MessageId.E_NOT_FOUND, MessageHelper.label("defaultWarehouseId"));
    }
  }

  /**
   * システム管理者がいなくなる変更を防ぐ（決定 PD-23）。
   *
   * <ul>
   *   <li>自分自身の無効化・自分の ADMIN ロールの削除は不可（E-018）
   *   <li>変更の結果、有効な ADMIN が0人になる場合は不可（E-019）。同時に別の管理者を変更した場合にすれ違わないよう、
   *       有効な ADMIN の行をロックしてから判定する
   * </ul>
   */
  private void checkAdminRemains(
      User before, List<Role> rolesBefore, UserCommand command, LoginUser operator) {
    boolean keepsAdmin = command.enabled() && command.roles().contains(Role.ADMIN);
    if (keepsAdmin) {
      return;
    }
    if (before.getUserId() == operator.getUserId()) {
      throw new BusinessException(MessageId.E_SELF_ADMIN_CHANGE);
    }
    boolean wasAdmin = before.isEnabled() && rolesBefore.contains(Role.ADMIN);
    if (!wasAdmin) {
      return;
    }
    List<Long> admins = mapper.lockEnabledUsersWithRole(Role.ADMIN);
    if (admins.stream().allMatch(id -> id.equals(before.getUserId()))) {
      throw new BusinessException(MessageId.E_NO_ADMIN_LEFT);
    }
  }

  private void insertRoles(long userId, List<Role> roles, LocalDateTime now, long operatorId) {
    for (Role role : EnumSet.copyOf(roles)) {
      mapper.insertRole(userId, role, now, operatorId);
    }
  }

  private List<Role> rolesOf(long userId) {
    return rolesOf(List.of(userId)).getOrDefault(userId, List.of());
  }

  private Map<Long, List<Role>> rolesOf(List<Long> userIds) {
    return mapper.findRoles(userIds).stream()
        .collect(
            Collectors.groupingBy(
                UserRole::userId,
                Collectors.mapping(UserRole::roleCode, Collectors.toList())));
  }

  private static BusinessException duplicateLoginId() {
    return new BusinessException(MessageId.E_DUPLICATE, MessageHelper.label("loginId"));
  }

  /** 主要な項目の変更前後（共通処理設計 7.1）。変更のない項目は記録しない。 */
  private static String changes(
      User before, List<Role> rolesBefore, UserCommand command, boolean unlock) {
    ChangeDetail detail =
        new ChangeDetail()
            .change("氏名", before.getUserName(), command.userName())
            .change("所属", before.getDepartment(), command.department())
            .change("所属倉庫ID", before.getDefaultWarehouseId(), command.defaultWarehouseId())
            .change("権限", labels(rolesBefore), labels(command.roles()))
            .change("状態", enabledLabel(before.isEnabled()), enabledLabel(command.enabled()));
    if (unlock) {
      detail.note("ロック解除");
    }
    return detail.build();
  }

  private static String labels(List<Role> roles) {
    return roles.stream().sorted().distinct().map(Role::getLabel).collect(Collectors.joining("・"));
  }

  private static String enabledLabel(boolean enabled) {
    return enabled ? "有効" : "無効";
  }
}
