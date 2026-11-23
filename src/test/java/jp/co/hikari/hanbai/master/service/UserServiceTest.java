package jp.co.hikari.hanbai.master.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import jp.co.hikari.hanbai.common.code.Role;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.exception.OptimisticLockException;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import jp.co.hikari.hanbai.common.operationlog.OperationLogTarget;
import jp.co.hikari.hanbai.common.security.LoginUser;
import jp.co.hikari.hanbai.master.dto.UserCommand;
import jp.co.hikari.hanbai.master.dto.UserRole;
import jp.co.hikari.hanbai.master.mapper.UserMapper;
import jp.co.hikari.hanbai.master.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock UserMapper mapper;
  @Mock WarehouseService warehouseService;
  @Mock OperationLogService operationLogService;
  @Mock PasswordEncoder passwordEncoder;

  private final Clock clock =
      Clock.fixed(Instant.parse("2027-01-18T00:00:00Z"), ZoneId.of("Asia/Tokyo"));
  private final LoginUser admin =
      new LoginUser(1L, "admin", "管理者", null, List.of(Role.ADMIN), true, false, false, null);

  private UserService service() {
    return new UserService(mapper, warehouseService, operationLogService, passwordEncoder, clock);
  }

  /** DB の自動採番と同じように、登録時にユーザーIDを設定する。 */
  private void insertAssignsId() {
    org.mockito.Mockito.doAnswer(
            inv -> {
              inv.<User>getArgument(0).setUserId(30L);
              return 1;
            })
        .when(mapper)
        .insert(any(), anyString(), any(), anyLong());
  }

  private static UserCommand command(Long userId, int version, Long warehouseId, boolean unlock) {
    return new UserCommand(
        userId,
        version,
        "suzuki",
        "鈴木 一郎",
        "営業部",
        warehouseId,
        List.of(Role.SALES, Role.APPROVER),
        true,
        unlock,
        "Init1234");
  }

  private static User existing(boolean locked) {
    User user = new User();
    user.setUserId(20L);
    user.setLoginId("suzuki");
    user.setUserName("鈴木");
    user.setDepartment("営業部");
    user.setEnabled(true);
    user.setLocked(locked);
    user.setVersion(3);
    return user;
  }

  @Test
  void 登録済みのログインIDはE005() {
    when(mapper.existsByLoginId("suzuki")).thenReturn(true);
    assertThatThrownBy(() -> service().create(command(null, 0, null, false), admin))
        .isInstanceOf(BusinessException.class)
        .extracting("messageId")
        .isEqualTo("E-005");
    verify(mapper, never()).insert(any(), anyString(), any(), anyLong());
  }

  @Test
  void 同時登録で一意制約に違反した場合もE005() {
    when(passwordEncoder.encode("Init1234")).thenReturn("hash");
    doThrow(new DuplicateKeyException("dup")).when(mapper).insert(any(), eq("hash"), any(), eq(1L));
    assertThatThrownBy(() -> service().create(command(null, 0, null, false), admin))
        .extracting("messageId")
        .isEqualTo("E-005");
  }

  @Test
  void 無効な所属倉庫はE006() {
    when(warehouseService.isEnabled(9L)).thenReturn(false);
    assertThatThrownBy(() -> service().create(command(null, 0, 9L, false), admin))
        .extracting("messageId")
        .isEqualTo("E-006");
  }

  @Test
  void 登録するとパスワードをハッシュ化しロールを付与し権限を操作内容に記録する() {
    when(warehouseService.isEnabled(2L)).thenReturn(true);
    when(passwordEncoder.encode("Init1234")).thenReturn("hash");
    insertAssignsId();

    OperationLogTarget target = service().create(command(null, 0, 2L, false), admin);

    verify(mapper).insert(any(User.class), eq("hash"), any(), eq(1L));
    verify(mapper).insertRole(eq(30L), eq(Role.SALES), any(), eq(1L));
    verify(mapper).insertRole(eq(30L), eq(Role.APPROVER), any(), eq(1L));
    assertThat(target.targetKey()).isEqualTo("suzuki");
    assertThat(target.detail()).isEqualTo("権限：営業担当・承認者、状態：有効");
  }

  @Test
  void versionが一致せず更新件数が0の場合はE020() {
    when(mapper.findById(20L)).thenReturn(Optional.of(existing(false)));
    when(mapper.update(any(), eq(false), any(), eq(1L))).thenReturn(0);
    assertThatThrownBy(() -> service().update(command(20L, 2, null, false), admin))
        .isInstanceOf(OptimisticLockException.class);
    verify(mapper, never()).deleteRoles(anyLong());
  }

  @Test
  void 存在しないユーザーの更新もE020() {
    when(mapper.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service().update(command(99L, 0, null, false), admin))
        .isInstanceOf(OptimisticLockException.class);
  }

  @Test
  void 更新すると変更前後とロック解除を操作内容に記録する() {
    when(mapper.findById(20L)).thenReturn(Optional.of(existing(true)));
    when(mapper.findRoles(List.of(20L))).thenReturn(List.of(new UserRole(20L, Role.SALES)));
    when(mapper.update(any(), eq(true), any(), eq(1L))).thenReturn(1);

    OperationLogTarget target = service().update(command(20L, 3, null, true), admin);

    verify(mapper).deleteRoles(20L);
    assertThat(target.targetKey()).isEqualTo("suzuki");
    assertThat(target.detail())
        .isEqualTo("氏名：鈴木→鈴木 一郎、権限：営業担当→営業担当・承認者、ロック解除");
  }

  @Test
  void ロックされていないユーザーのロック解除指定は無視し変更がなければ操作内容なし() {
    User user = existing(false);
    user.setUserName("鈴木 一郎");
    when(mapper.findById(20L)).thenReturn(Optional.of(user));
    when(mapper.findRoles(List.of(20L)))
        .thenReturn(List.of(new UserRole(20L, Role.SALES), new UserRole(20L, Role.APPROVER)));
    when(mapper.update(any(), eq(false), any(), eq(1L))).thenReturn(1);

    assertThat(service().update(command(20L, 3, null, true), admin).detail()).isNull();
  }

  @Test
  void ユーザーがいなければ初期管理者を作成しいれば作成しない() {
    when(mapper.countAll()).thenReturn(0L, 1L);
    when(passwordEncoder.encode("Admin2027")).thenReturn("hash");
    insertAssignsId();

    assertThat(service().createInitialAdminIfNoUsers("admin", "Admin2027")).isTrue();
    verify(mapper).insertRole(eq(30L), eq(Role.ADMIN), any(), eq(0L));
    verify(operationLogService).record(any());

    assertThat(service().createInitialAdminIfNoUsers("admin", "Admin2027")).isFalse();
  }

  @Test
  void 初期管理者のパスワードが未設定なら作成しない() {
    new InitialAdminInitializer(service(), "admin", "").run(null);
    new InitialAdminInitializer(service(), "admin", null).run(null);
    verify(mapper, never()).countAll();
  }
}
