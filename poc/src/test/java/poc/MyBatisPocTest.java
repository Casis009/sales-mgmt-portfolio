package poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * P-01：MyBatis が Spring Boot 4.1 で動作すること。 P-04：Testcontainers（PostgreSQL）で実行できること。
 *
 * <p>未実行の草案。Testcontainers のバージョンによっては PostgreSQLContainer の パッケージが
 * org.testcontainers.postgresql に変わっている。
 */
@SpringBootTest
@Testcontainers
class MyBatisPocTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @Autowired StockMapper stockMapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    jdbc.execute("DROP TABLE IF EXISTS t_stock");
    jdbc.execute(
        """
        CREATE TABLE t_stock (
          product_id   BIGINT NOT NULL,
          warehouse_id BIGINT NOT NULL,
          quantity     INT    NOT NULL DEFAULT 0 CHECK (quantity >= 0),
          PRIMARY KEY (product_id, warehouse_id)
        )
        """);
    jdbc.update("INSERT INTO t_stock VALUES (1, 1, 10)");
  }

  @Test
  @Transactional
  void 行ロック付きで取得し_更新できる() {
    assertThat(stockMapper.selectQuantityForUpdate(1, 1)).contains(10);
    assertThat(stockMapper.addQuantity(1, 1, -3)).isEqualTo(1);
    assertThat(stockMapper.selectQuantityForUpdate(1, 1)).contains(7);
  }

  @Test
  @Transactional
  void 行が存在しない場合_Optionalが空になる() {
    assertThat(stockMapper.selectQuantityForUpdate(99, 1)).isEmpty();
  }

  @Test
  void 現在庫数が0未満になる更新_CHECK制約でエラーになる() {
    assertThatThrownBy(() -> stockMapper.addQuantity(1, 1, -11))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
