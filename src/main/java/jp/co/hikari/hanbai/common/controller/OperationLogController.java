package jp.co.hikari.hanbai.common.controller;

import java.util.List;
import jp.co.hikari.hanbai.common.code.OperationType;
import jp.co.hikari.hanbai.common.form.OperationLogSearchForm;
import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import jp.co.hikari.hanbai.common.message.UiMessage;
import jp.co.hikari.hanbai.common.operationlog.OperationLogCriteria;
import jp.co.hikari.hanbai.common.operationlog.OperationLogService;
import jp.co.hikari.hanbai.common.web.PageUrls;
import jp.co.hikari.hanbai.common.web.Pagination;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/** SCR-013 操作ログ照会（F-005）。システム管理者のみ。照会のみで、変更・削除の機能は設けない（NF-31）。 */
@Controller
public class OperationLogController {

  private static final String VIEW = "common/operation-log-list";

  private final OperationLogService operationLogService;
  private final MessageHelper messageHelper;

  /**
   * 依存部品を受け取る。
   *
   * @param operationLogService 操作ログ
   * @param messageHelper メッセージ
   */
  public OperationLogController(
      OperationLogService operationLogService, MessageHelper messageHelper) {
    this.operationLogService = operationLogService;
    this.messageHelper = messageHelper;
  }

  /**
   * 操作ログを検索する（操作日時の降順、1ページ50件）。初期表示では検索しない。
   *
   * @param form 検索条件
   * @param result 入力チェックの結果
   * @param model モデル
   * @return 画面
   */
  @GetMapping("/admin/operation-logs")
  public String list(
      @Validated @ModelAttribute("searchForm") OperationLogSearchForm form,
      BindingResult result,
      Model model) {
    model.addAttribute("operationTypes", List.of(OperationType.values()));
    if (!form.isSearchRequested()) {
      return VIEW;
    }
    if (form.getOperatedFrom() != null
        && form.getOperatedTo() != null
        && form.getOperatedTo().isBefore(form.getOperatedFrom())) {
      result.rejectValue(
          "operatedTo",
          MessageId.E_DATE_ORDER,
          new Object[] {MessageHelper.label("operatedTo"), MessageHelper.label("operatedFrom")},
          null);
    }
    if (result.hasErrors()) {
      return VIEW;
    }
    OperationLogCriteria criteria = form.toCriteria();
    long total = operationLogService.count(criteria);
    Pagination pagination = Pagination.of(form.getPage(), total, PageUrls.currentWithoutPage());
    model.addAttribute("pagination", pagination);
    model.addAttribute(
        "rows", operationLogService.search(criteria, pagination.limit(), pagination.offset()));
    if (total == 0) {
      messageHelper.add(model, UiMessage.Level.INFO, MessageId.I_NO_DATA);
    } else if (pagination.tooMany()) {
      messageHelper.add(
          model, UiMessage.Level.WARN, MessageId.W_TOO_MANY_RESULTS, Pagination.MAX_RESULTS);
    }
    return VIEW;
  }
}
