package jp.co.hikari.hanbai.master.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 起動時に、ユーザーが1人もいなければ初期のシステム管理者を作成する（セキュリティ設計 1章 No.4、決定 PD-19）。
 *
 * <p>初期パスワードは環境変数 {@code HANBAI_INITIAL_ADMIN_PASSWORD} で渡し、ソースコード・リポジトリに含めない（開発環境のみ
 * application-dev.yml に既定値を置く）。未設定の場合は作成しない。作成したユーザーは初回ログイン時にパスワード変更を求められる。
 */
@Component
public class InitialAdminInitializer implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(InitialAdminInitializer.class);

  private final UserService userService;
  private final String loginId;
  private final String password;

  /**
   * 依存部品と設定値を受け取る。
   *
   * @param userService ユーザー管理
   * @param loginId 初期管理者のログインID
   * @param password 初期管理者のパスワード（未設定は空）
   */
  public InitialAdminInitializer(
      UserService userService,
      @Value("${hanbai.initial-admin.login-id:admin}") String loginId,
      @Value("${hanbai.initial-admin.password:}") String password) {
    this.userService = userService;
    this.loginId = loginId;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (password == null || password.isBlank()) {
      LOG.info("初期管理者のパスワードが未設定のため、初期管理者の作成を確認しない");
      return;
    }
    if (userService.createInitialAdminIfNoUsers(loginId, password)) {
      // パスワードはログに出力しない
      LOG.info("初期管理者を作成した: loginId={}", loginId);
    }
  }
}
