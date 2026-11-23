package jp.co.hikari.hanbai.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ログインIDからユーザーとロールを取得する（セキュリティ設計 1章）。
 *
 * <p>ユーザーが存在しない場合の例外は、Spring Security がパスワード不一致と同じ例外に変換する
 * （E-010。ログインIDの存在有無を推測させない）。
 */
@Service
public class LoginUserDetailsService implements UserDetailsService {

  private final LoginUserMapper mapper;

  /**
   * 依存部品を受け取る。
   *
   * @param mapper 認証用の SQL
   */
  public LoginUserDetailsService(LoginUserMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String loginId) {
    // 前後の空白は除去する（SCR-001 項目定義）
    String trimmed = loginId == null ? "" : loginId.strip();
    return mapper
        .findByLoginId(trimmed)
        .map(LoginUserRow::toLoginUser)
        .orElseThrow(() -> new UsernameNotFoundException("ユーザーなし"));
  }
}
