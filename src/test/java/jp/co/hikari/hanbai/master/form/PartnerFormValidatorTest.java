package jp.co.hikari.hanbai.master.form;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/** 取引先の相関チェック（MS-04・MS-05：同値分割・境界値）。 */
class PartnerFormValidatorTest {

  private final PartnerFormValidator validator = new PartnerFormValidator();

  private static PartnerForm customer(Integer closingDay, Integer offset, Integer paymentDay) {
    PartnerForm form = new PartnerForm();
    form.setCustomer(true);
    form.setClosingDay(closingDay);
    form.setPaymentMonthOffset(offset);
    form.setPaymentDay(paymentDay);
    return form;
  }

  private static BeanPropertyBindingResult validate(PartnerForm form) {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
    new PartnerFormValidator().validate(form, errors);
    return errors;
  }

  private static String code(BeanPropertyBindingResult errors, String field) {
    FieldError error = errors.getFieldError(field);
    return error == null ? "OK" : error.getCode();
  }

  @Test
  void 得意先も仕入先も選択しない場合はE001() {
    BeanPropertyBindingResult errors = validate(new PartnerForm());
    assertThat(code(errors, "customer")).isEqualTo("E-001");
  }

  @Test
  void 仕入先のみの場合は締日と支払条件を確認しない() {
    PartnerForm form = new PartnerForm();
    form.setSupplier(true);
    assertThat(validate(form).hasErrors()).isFalse();
    assertThat(validator.supports(PartnerForm.class)).isTrue();
    assertThat(validator.supports(String.class)).isFalse();
  }

  @Test
  void 得意先の場合は締日と支払月と支払日が必須() {
    BeanPropertyBindingResult errors = validate(customer(null, null, null));
    assertThat(code(errors, "closingDay")).isEqualTo("E-001");
    assertThat(code(errors, "paymentMonthOffset")).isEqualTo("E-001");
    assertThat(code(errors, "paymentDay")).isEqualTo("E-001");
  }

  @ParameterizedTest(name = "締日 {0} → {1}")
  @CsvSource({"0, E-004", "1, OK", "28, OK", "29, E-004", "98, E-004", "99, OK", "100, E-004"})
  void 締日と支払日は1から28または99(int day, String expected) {
    assertThat(code(validate(customer(day, 1, 10)), "closingDay")).isEqualTo(expected);
    assertThat(code(validate(customer(20, 1, day)), "paymentDay")).isEqualTo(expected);
  }

  @ParameterizedTest(name = "支払月 {0} → {1}")
  @CsvSource({"-1, E-004", "0, OK", "2, OK", "3, E-004"})
  void 支払月は0から2(int offset, String expected) {
    assertThat(code(validate(customer(99, offset, 99)), "paymentMonthOffset"))
        .isEqualTo(expected);
  }
}
