package jp.co.hikari.hanbai.common.code;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 区分値の Enum と DB の値を相互変換する MyBatis の TypeHandler（ソフトウェア構成 5章 No.4）。
 *
 * <p>MyBatis の既定の Enum 用 TypeHandler として登録する（application.yml の {@code
 * mybatis.configuration.default-enum-type-handler}）。{@link CodeEnum} を実装した Enum はコードで、
 * 実装していない Enum は定数名で変換する。
 *
 * @param <E> Enum の型
 */
public class CodeEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  private final Class<E> type;

  /**
   * MyBatis が Enum の型ごとに生成する。
   *
   * @param type Enum の型
   */
  public CodeEnumTypeHandler(Class<E> type) {
    if (type == null) {
      throw new IllegalArgumentException("type は必須");
    }
    this.type = type;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, toCode(parameter));
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toEnum(rs.getString(columnName));
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toEnum(rs.getString(columnIndex));
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toEnum(cs.getString(columnIndex));
  }

  private String toCode(E value) {
    return value instanceof CodeEnum c ? c.getCode() : value.name();
  }

  private E toEnum(String code) {
    if (code == null) {
      return null;
    }
    // CHAR 型のカラムは末尾が空白で埋められるため除去する
    String trimmed = code.strip();
    for (E constant : type.getEnumConstants()) {
      if (toCode(constant).equals(trimmed)) {
        return constant;
      }
    }
    throw new IllegalArgumentException("未定義のコード: " + type.getSimpleName() + "=" + trimmed);
  }
}
