package jp.co.hikari.hanbai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 結合テスト（*IT）で使用する PostgreSQL。本番と同じ PostgreSQL 17 をコンテナで起動する。
 *
 * <p>{@link ServiceConnection} により、データソースの接続先が自動でこのコンテナに設定される。
 * 同じ設定のテストクラス間では Spring のコンテキストとともにコンテナが再利用される。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /** 本番と同じメジャーバージョン（技術選定書 2.2）。 */
  public static final String POSTGRES_IMAGE = "postgres:17";

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(POSTGRES_IMAGE);
  }
}
