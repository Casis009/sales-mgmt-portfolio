package jp.co.hikari.hanbai.common.web;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 一覧画面のページング（共通処理設計 10章、画面一覧 5.2 No.1）。
 *
 * <p>1ページ {@value #PAGE_SIZE} 件。ページ番号は1始まり。リンクの URL は、現在の検索条件を保持した URL（{@code page}
 * パラメータを除く）に {@code page} を付けて作る。
 *
 * @param page 表示中のページ（1始まり。範囲外は補正済み）
 * @param total 件数
 * @param baseUrl {@code page} を除いた現在の URL
 */
public record Pagination(int page, long total, String baseUrl) {

  /** 1ページの件数（NF-07）。 */
  public static final int PAGE_SIZE = 50;

  /** これを超える件数の場合に W-002 を表示する。 */
  public static final int MAX_RESULTS = 5_000;

  /** ページ番号のリンクを前後に表示する数。 */
  private static final int WINDOW = 4;

  /**
   * ページングを作る。ページ番号が範囲外の場合は、最初または最後のページに補正する。
   *
   * @param requestedPage 指定されたページ（null は1）
   * @param total 件数
   * @param baseUrl {@code page} を除いた現在の URL
   * @return ページング
   */
  public static Pagination of(Integer requestedPage, long total, String baseUrl) {
    int last = lastPage(total);
    int page = requestedPage == null ? 1 : Math.clamp(requestedPage, 1, last);
    return new Pagination(page, total, baseUrl);
  }

  private static int lastPage(long total) {
    return (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
  }

  /**
   * SQL の OFFSET を返す。
   *
   * @return (page − 1) × 50
   */
  public long offset() {
    return (long) (page - 1) * PAGE_SIZE;
  }

  /**
   * SQL の LIMIT を返す。
   *
   * @return 50
   */
  public int limit() {
    return PAGE_SIZE;
  }

  /**
   * 最後のページ番号を返す。
   *
   * @return 最後のページ番号（0件の場合は1）
   */
  public int totalPages() {
    return lastPage(total);
  }

  /**
   * 前のページがあるかを返す。
   *
   * @return ある場合 true
   */
  public boolean hasPrevious() {
    return page > 1;
  }

  /**
   * 次のページがあるかを返す。
   *
   * @return ある場合 true
   */
  public boolean hasNext() {
    return page < totalPages();
  }

  /**
   * 件数が上限を超えているか（W-002 を表示するか）を返す。
   *
   * @return 超えている場合 true
   */
  public boolean tooMany() {
    return total > MAX_RESULTS;
  }

  /**
   * 表示中のページの前後に表示するページ番号を返す。
   *
   * @return ページ番号
   */
  public List<Integer> pageNumbers() {
    int from = Math.max(1, page - WINDOW);
    int to = Math.min(totalPages(), page + WINDOW);
    return IntStream.rangeClosed(from, to).boxed().toList();
  }

  /**
   * 表示中のページの開始件数を返す（例：51～100件目の 51）。
   *
   * @return 開始件数（0件の場合は0）
   */
  public long fromIndex() {
    return total == 0 ? 0 : offset() + 1;
  }

  /**
   * 表示中のページの終了件数を返す。
   *
   * @return 終了件数
   */
  public long toIndex() {
    return Math.min(total, offset() + PAGE_SIZE);
  }

  /**
   * 指定のページの URL を返す。
   *
   * @param target ページ番号
   * @return URL
   */
  public String url(int target) {
    return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "page=" + target;
  }
}
