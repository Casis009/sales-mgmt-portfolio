package jp.co.hikari.hanbai.common.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

class CodeEnumTypeHandlerTest {

  @Test
  void CodeEnumはコードでDBに保存される() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    new CodeEnumTypeHandler<>(OperationResult.class)
        .setNonNullParameter(ps, 1, OperationResult.FAILURE, null);
    verify(ps).setString(1, "9");
  }

  @Test
  void CHAR型の末尾空白を除いてEnumに変換する() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("result")).thenReturn("1 ");
    assertThat(new CodeEnumTypeHandler<>(OperationResult.class).getNullableResult(rs, "result"))
        .isEqualTo(OperationResult.SUCCESS);
  }

  @Test
  void 列番号とCallableStatementからも変換できる() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(2)).thenReturn("ADMIN");
    CallableStatement cs = mock(CallableStatement.class);
    when(cs.getString(3)).thenReturn("LOGIN");

    assertThat(new CodeEnumTypeHandler<>(Role.class).getNullableResult(rs, 2))
        .isEqualTo(Role.ADMIN);
    assertThat(new CodeEnumTypeHandler<>(OperationType.class).getNullableResult(cs, 3))
        .isEqualTo(OperationType.LOGIN);
  }

  @Test
  void NULLはnullに変換する() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    assertThat(new CodeEnumTypeHandler<>(Role.class).getNullableResult(rs, "role_code")).isNull();
  }

  @Test
  void 未定義のコードは例外になる() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("result")).thenReturn("5");
    CodeEnumTypeHandler<OperationResult> handler = new CodeEnumTypeHandler<>(OperationResult.class);
    assertThatThrownBy(() -> handler.getNullableResult(rs, "result"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void CodeEnumでないEnumは定数名で変換する() throws SQLException {
    PreparedStatement ps = mock(PreparedStatement.class);
    CodeEnumTypeHandler<DayOfWeek> handler = new CodeEnumTypeHandler<>(DayOfWeek.class);
    handler.setNonNullParameter(ps, 1, DayOfWeek.MONDAY, null);
    verify(ps).setString(1, "MONDAY");
  }

  @Test
  void 型の指定は必須() {
    assertThatThrownBy(() -> new CodeEnumTypeHandler<>(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void コードからEnumを取得できる() {
    assertThat(CodeEnum.fromCode(OperationResult.class, "9")).contains(OperationResult.FAILURE);
    assertThat(CodeEnum.fromCode(OperationResult.class, "0")).isEmpty();
    assertThat(Role.WAREHOUSE.authority()).isEqualTo("ROLE_WAREHOUSE");
    assertThat(Role.WAREHOUSE.getLabel()).isEqualTo("物流担当");
    assertThat(OperationType.EXPORT.getCode()).isEqualTo("EXPORT");
    assertThat(OperationType.EXPORT.getLabel()).isEqualTo("出力");
    assertThat(OperationResult.SUCCESS.getLabel()).isEqualTo("成功");
  }
}
