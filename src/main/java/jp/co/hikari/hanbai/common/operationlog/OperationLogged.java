package jp.co.hikari.hanbai.common.operationlog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jp.co.hikari.hanbai.common.code.OperationType;

/**
 * 操作ログを記録する Service メソッドに付ける（共通処理設計 7.1）。
 *
 * <p>メソッドが正常終了した後、同じトランザクションで操作ログ（結果＝成功）を登録する。 業務エラーなどの例外で終了した場合は記録しない（ロールバックされるため）。
 *
 * <ul>
 *   <li>対象キー：戻り値。戻り値が {@link OperationLogTarget} の場合は、その対象キー・操作内容を使う
 *   <li>操作者：引数の {@code LoginUser}。引数にない場合はログイン中のユーザー
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogged {

  /**
   * 操作種別を返す。
   *
   * @return 操作種別
   */
  OperationType type();

  /**
   * 画面IDを返す。
   *
   * @return 画面ID（例：SCR-405）
   */
  String screenId();

  /**
   * 対象種別を返す。
   *
   * @return テーブルの物理名（例：t_shipment）
   */
  String targetType();
}
