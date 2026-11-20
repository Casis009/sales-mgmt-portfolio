package jp.co.hikari.hanbai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** アプリケーションが起動し、Flyway で全テーブルが作成されることを確認する。 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HanbaiApplicationIT {

  @Autowired JdbcTemplate jdbc;

  @Test
  void 起動時にFlywayで業務テーブル27件とバッチ管理テーブル6件が作成される() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables"
                + " WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
            String.class);

    assertThat(tables.stream().filter(t -> t.startsWith("m_") || t.startsWith("t_")))
        .hasSize(27);
    assertThat(tables.stream().filter(t -> t.startsWith("batch_"))).hasSize(6);
  }

  @Test
  void Flywayのマイグレーションがすべて成功している() {
    List<String> versions =
        jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
            String.class);

    assertThat(versions).containsExactly("1", "2", "3");
  }
}
