package jp.co.hikari.hanbai.master.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import jp.co.hikari.hanbai.master.dto.ProductCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/** 倉庫ごとの発注点・標準発注数の検査（MS-06～MS-08：同値分割・境界値）。 */
class ProductFormValidatorTest {

  private final ProductFormValidator validator = new ProductFormValidator();

  private static ProductForm form(String point, String quantity) {
    ReorderSettingForm row = new ReorderSettingForm();
    row.setWarehouseId(10L);
    row.setWarehouseLabel("HQ 本社倉庫");
    row.setReorderPoint(point);
    row.setStandardOrderQuantity(quantity);
    ProductForm form = new ProductForm();
    form.setSettings(List.of(row));
    return form;
  }

  private record Result(List<ProductCommand.Setting> settings, BeanPropertyBindingResult errors) {
    String code(String field) {
      FieldError error = errors.getFieldError("settings[0]." + field);
      return error == null ? "OK" : error.getCode();
    }
  }

  private Result validate(String point, String quantity) {
    ProductForm form = form(point, quantity);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
    return new Result(validator.validate(form, errors), errors);
  }

  @Test
  void 両方空欄は発注点チェックの対象外() {
    Result result = validate("", " ");
    assertThat(result.errors().hasErrors()).isFalse();
    assertThat(result.settings())
        .containsExactly(new ProductCommand.Setting(10L, null, null))
        .allMatch(s -> !s.configured());
  }

  @Test
  void 発注点のみ入力は対象で標準発注数は未入力のまま渡す() {
    Result result = validate("10", null);
    assertThat(result.settings()).containsExactly(new ProductCommand.Setting(10L, 10, null));
    assertThat(result.settings().get(0).configured()).isTrue();
  }

  @Test
  void 標準発注数のみ入力は発注点が必須() {
    Result result = validate(null, "50");
    assertThat(result.code("reorderPoint")).isEqualTo("E-001");
    assertThat(result.errors().getFieldError("settings[0].reorderPoint").getArguments())
        .containsExactly("発注点（HQ 本社倉庫）");
    assertThat(result.settings()).isEmpty();
  }

  @ParameterizedTest(name = "{0} → {1}")
  @CsvSource({
    "0,        OK",
    "999999,   OK",
    "1000000,  E-003",
    "12345678, E-003",
    "-1,       E-004",
    "1.5,      E-004",
    "abc,      E-004",
  })
  void 発注点と標準発注数は0から999999の整数(String value, String expected) {
    assertThat(validate(value, "1").code("reorderPoint")).isEqualTo(expected);
    assertThat(validate("1", value).code("standardOrderQuantity")).isEqualTo(expected);
  }

  @Test
  void 倉庫IDのない行は無視する() {
    ProductForm form = form("10", "20");
    form.getSettings().get(0).setWarehouseId(null);
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
    assertThat(validator.validate(form, errors)).isEmpty();
  }
}
