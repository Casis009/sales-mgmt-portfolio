package jp.co.hikari.hanbai.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaginationTest {

  @Test
  void 件数0は1ページで前後なし() {
    Pagination p = Pagination.of(null, 0, "/a");
    assertThat(p.page()).isEqualTo(1);
    assertThat(p.totalPages()).isEqualTo(1);
    assertThat(p.hasPrevious()).isFalse();
    assertThat(p.hasNext()).isFalse();
    assertThat(p.fromIndex()).isZero();
    assertThat(p.toIndex()).isZero();
  }

  @Test
  void ページ数の境界は50件と51件() {
    assertThat(Pagination.of(1, 50, "/a").totalPages()).isEqualTo(1);
    assertThat(Pagination.of(1, 51, "/a").totalPages()).isEqualTo(2);
  }

  @Test
  void OFFSETとLIMITと表示範囲を計算する() {
    Pagination p = Pagination.of(3, 120, "/a");
    assertThat(p.offset()).isEqualTo(100);
    assertThat(p.limit()).isEqualTo(50);
    assertThat(p.fromIndex()).isEqualTo(101);
    assertThat(p.toIndex()).isEqualTo(120);
    assertThat(p.hasPrevious()).isTrue();
    assertThat(p.hasNext()).isFalse();
  }

  @Test
  void 範囲外のページは最初または最後に補正する() {
    assertThat(Pagination.of(0, 120, "/a").page()).isEqualTo(1);
    assertThat(Pagination.of(-5, 120, "/a").page()).isEqualTo(1);
    assertThat(Pagination.of(99, 120, "/a").page()).isEqualTo(3);
  }

  @Test
  void 前後4ページの番号を表示する() {
    assertThat(Pagination.of(10, 1000, "/a").pageNumbers())
        .containsExactly(6, 7, 8, 9, 10, 11, 12, 13, 14);
    assertThat(Pagination.of(1, 120, "/a").pageNumbers()).containsExactly(1, 2, 3);
  }

  @Test
  void 検索条件を保持したままページのURLを作る() {
    assertThat(Pagination.of(1, 120, "/admin/users").url(2)).isEqualTo("/admin/users?page=2");
    assertThat(Pagination.of(1, 120, "/admin/users?loginId=a").url(3))
        .isEqualTo("/admin/users?loginId=a&page=3");
  }

  @Test
  void 件数が5000件を超えると警告対象() {
    assertThat(Pagination.of(1, 5000, "/a").tooMany()).isFalse();
    assertThat(Pagination.of(1, 5001, "/a").tooMany()).isTrue();
  }
}
