package jp.co.hikari.hanbai.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jp.co.hikari.hanbai.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 技術検証 P-01：MyBatis（mybatis-spring-boot-starter 4.1.0）が Spring Boot 4.1 で動作すること。
 *
 * <p>あわせて P-04（Testcontainers）をローカルの Docker で確認する。テーブルはテスト専用の DDL ではなく、
 * Flyway が作成した本番と同じ t_stock を使用する。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MyBatisVerificationIT {

  private static final LocalDateTime NOW = LocalDateTime.of(2027, 4, 1, 9, 0);

  @Autowired StockProbeMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate tx;

  private long productId;
  private long warehouseId;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM t_stock");
    jdbc.update("DELETE FROM m_product WHERE product_code = 'P-TEST-001'");
    jdbc.update("DELETE FROM m_warehouse WHERE warehouse_code = 'W-TEST'");
    warehouseId =
        jdbc.queryForObject(
            "INSERT INTO m_warehouse (warehouse_code, warehouse_name, created_at, created_by,"
                + " updated_at, updated_by) VALUES ('W-TEST', 'テスト倉庫', ?, 0, ?, 0)"
                + " RETURNING warehouse_id",
            Long.class,
            NOW,
            NOW);
    productId =
        jdbc.queryForObject(
            "INSERT INTO m_product (product_code, product_name, unit, created_at, created_by,"
                + " updated_at, updated_by) VALUES ('P-TEST-001', 'LEDダウンライト', '台', ?, 0, ?, 0)"
                + " RETURNING product_id",
            Long.class,
            NOW,
            NOW);
    jdbc.update(
        "INSERT INTO t_stock (product_id, warehouse_id, quantity, allocated_quantity, created_at,"
            + " created_by, updated_at, updated_by) VALUES (?, ?, 10, 0, ?, 0, ?, 0)",
        productId,
        warehouseId,
        NOW,
        NOW);
  }

  @Test
  void 行ロック付きで取得し_増減値で更新できる() {
    Integer after =
        tx.execute(
            status -> {
              assertThat(mapper.selectQuantityForUpdate(productId, warehouseId)).contains(10);
              assertThat(mapper.addQuantity(productId, warehouseId, -3, NOW)).isEqualTo(1);
              return mapper.selectQuantityForUpdate(productId, warehouseId).orElseThrow();
            });
    assertThat(after).isEqualTo(7);
  }

  @Test
  void 行が存在しない場合_Optionalが空になる() {
    assertThat(mapper.selectQuantityForUpdate(productId + 999, warehouseId)).isEmpty();
  }

  @Test
  void 現在庫数が0未満になる更新_CHECK制約違反がDataIntegrityViolationExceptionになる() {
    assertThatThrownBy(() -> mapper.addQuantity(productId, warehouseId, -11, NOW))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_t_stock_quantity");
  }

  @Test
  void ON_CONFLICT_DO_NOTHING_既存行があれば0件_なければ1件作成する() {
    assertThat(mapper.insertIfAbsent(productId, warehouseId, NOW)).isZero();
    jdbc.update("DELETE FROM t_stock");
    assertThat(mapper.insertIfAbsent(productId, warehouseId, NOW)).isEqualTo(1);
  }

  @Test
  void 行ロック中の行は_別トランザクションがロック解放まで待たされる() throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    final CompletableFuture<Void> holder =
        CompletableFuture.runAsync(
            () ->
                tx.executeWithoutResult(
                    status -> {
                      mapper.selectQuantityForUpdate(productId, warehouseId);
                      locked.countDown();
                      await(release);
                    }));
    assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

    CompletableFuture<Integer> waiter =
        CompletableFuture.supplyAsync(
            () -> tx.execute(status -> selectForUpdate().orElseThrow()));

    // 1つ目のトランザクションがロックを保持している間は、2つ目は取得できない
    Thread.sleep(1000);
    assertThat(waiter).isNotDone();

    release.countDown();
    holder.get(10, TimeUnit.SECONDS);
    assertThat(waiter.get(10, TimeUnit.SECONDS)).isEqualTo(10);
  }

  private java.util.Optional<Integer> selectForUpdate() {
    return mapper.selectQuantityForUpdate(productId, warehouseId);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
