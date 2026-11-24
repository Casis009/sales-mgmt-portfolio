package jp.co.hikari.hanbai.master.controller;

import java.util.List;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.message.UiMessage;
import jp.co.hikari.hanbai.common.web.PageUrls;
import jp.co.hikari.hanbai.common.web.Pagination;
import jp.co.hikari.hanbai.master.dto.MasterCriteria;
import org.springframework.ui.Model;

/** マスタ一覧画面の共通処理（件数取得、ページング、該当なし・件数超過のメッセージ）。 */
final class MasterPages {

  private MasterPages() {}

  /**
   * 検索して一覧画面のモデルに設定する。
   *
   * @param criteria 検索条件
   * @param page ページ番号
   * @param counter 件数を返す処理
   * @param searcher 1ページ分を返す処理（引数は offset。件数は 50）
   * @param model モデル
   * @param messageHelper メッセージ
   * @param <T> 一覧の行
   */
  static <T> void search(
      MasterCriteria criteria,
      Integer page,
      ToLongFunction<MasterCriteria> counter,
      LongFunction<List<T>> searcher,
      Model model,
      MessageHelper messageHelper) {
    long total = counter.applyAsLong(criteria);
    Pagination pagination = Pagination.of(page, total, PageUrls.currentWithoutPage());
    model.addAttribute("pagination", pagination);
    model.addAttribute("rows", searcher.apply(pagination.offset()));
    if (total == 0) {
      messageHelper.add(model, UiMessage.Level.INFO, MessageId.I_NO_DATA);
    } else if (pagination.tooMany()) {
      messageHelper.add(
          model, UiMessage.Level.WARN, MessageId.W_TOO_MANY_RESULTS, Pagination.MAX_RESULTS);
    }
  }
}
