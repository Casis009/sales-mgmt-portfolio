package jp.co.hikari.hanbai.common.security;

import java.time.Clock;
import java.time.LocalDateTime;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.exception.BusinessException;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.operationlog.OperationLogged;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * パスワード変更（F-002、SCR-003）。新しいパスワードの条件は、入力チェック（{@link PasswordPolicy}）で確認済みとする。
 */
@Service
public class PasswordService {

  private final LoginUserMapper mapper;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 認証用の SQL
   * @param passwordEncoder パスワードのハッシュ化
   * @param clock 時計
   */
  public PasswordService(LoginUserMapper mapper, PasswordEncoder passwordEncoder, Clock clock) {
    this.mapper = mapper;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * ログイン中のユーザーのパスワードを変更し、パスワード変更要求フラグを解除する。
   *
   * @param user ログインユーザー
   * @param currentPassword 現在のパスワード
   * @param newPassword 新しいパスワード
   * @return ログインID（操作ログの対象キー。パスワードは記録しない）
   * @throws BusinessException 現在のパスワードが正しくない場合（E-014）
   */
  @OperationLogged(type = OperationType.UPDATE, screenId = "SCR-003", targetType = "m_user")
  @Transactional
  public String changePassword(LoginUser user, String currentPassword, String newPassword) {
    LoginUserRow row =
        mapper
            .findByIdForUpdate(user.getUserId())
            .orElseThrow(() -> new IllegalStateException("ログイン中のユーザーが存在しない"));
    if (!passwordEncoder.matches(currentPassword, row.getPasswordHash())) {
      throw new BusinessException(MessageId.E_CURRENT_PASSWORD);
    }
    mapper.updatePassword(
        user.getUserId(), passwordEncoder.encode(newPassword), LocalDateTime.now(clock));
    return user.getLoginId();
  }
}
