package jp.co.hikari.hanbai.common.code;

import java.util.Arrays;
import java.util.Optional;

/**
 * 区分値の Enum が実装するインターフェース（ソフトウェア構成 5章 No.4）。
 *
 * <p>DB に保存する値（コード）と画面に表示する名称を持つ。DB との相互変換は {@link CodeEnumTypeHandler} が行う。
 */
public interface CodeEnum {

  /**
   * DB に保存する値を返す。
   *
   * @return コード（コード定義書の「値」）
   */
  String getCode();

  /**
   * 画面に表示する名称を返す。
   *
   * @return 名称（コード定義書の「名称」）
   */
  String getLabel();

  /**
   * コードから Enum を取得する。
   *
   * @param type Enum の型
   * @param code コード
   * @param <E> Enum の型
   * @return 該当する定数。該当しない場合は空
   */
  static <E extends Enum<E> & CodeEnum> Optional<E> fromCode(Class<E> type, String code) {
    return Arrays.stream(type.getEnumConstants()).filter(e -> e.getCode().equals(code)).findFirst();
  }
}
