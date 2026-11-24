package jp.co.hikari.hanbai.master.form;

import jp.co.hikari.hanbai.common.message.MessageHelper;
import jp.co.hikari.hanbai.common.message.MessageId;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * SCR-122 取引先の相関チェック（共通処理設計 6章）。
 *
 * <ul>
 *   <li>得意先・仕入先のいずれかを選択する（E-001。テーブルの CHECK 制約と同じ条件）
 *   <li>得意先の場合、締日・支払月・支払日を必須とする（E-001。BR-32）
 *   <li>締日・支払日は 1～28 または 99（末日）、支払月は 0～2（E-004。コード定義書 11・12章）
 * </ul>
 */
@Component
public class PartnerFormValidator implements Validator {

  private static final int MAX_DAY = 28;
  private static final int END_OF_MONTH = 99;
  private static final int MAX_MONTH_OFFSET = 2;

  @Override
  public boolean supports(Class<?> clazz) {
    return PartnerForm.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    PartnerForm form = (PartnerForm) target;
    if (!form.isCustomer() && !form.isSupplier()) {
      required(errors, "customer");
    }
    if (!form.isCustomer()) {
      return;
    }
    checkDay(errors, "closingDay", form.getClosingDay());
    checkDay(errors, "paymentDay", form.getPaymentDay());
    Integer offset = form.getPaymentMonthOffset();
    if (offset == null) {
      required(errors, "paymentMonthOffset");
    } else if (offset < 0 || offset > MAX_MONTH_OFFSET) {
      invalid(errors, "paymentMonthOffset");
    }
  }

  private static void checkDay(Errors errors, String field, Integer day) {
    if (day == null) {
      required(errors, field);
    } else if ((day < 1 || day > MAX_DAY) && day != END_OF_MONTH) {
      invalid(errors, field);
    }
  }

  private static void required(Errors errors, String field) {
    if (!errors.hasFieldErrors(field)) {
      errors.rejectValue(
          field, MessageId.E_REQUIRED, new Object[] {MessageHelper.label(field)}, null);
    }
  }

  private static void invalid(Errors errors, String field) {
    errors.rejectValue(field, MessageId.E_FORMAT, new Object[] {MessageHelper.label(field)}, null);
  }
}
