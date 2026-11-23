package jp.co.hikari.hanbai.common.operationlog;

/**
 * 操作ログの対象キーと操作内容を返す戻り値（{@link OperationLogged} のメソッドが返す）。
 *
 * <p>伝票番号などの対象キーに加えて、主要な項目の変更内容を記録する場合に実装する（共通処理設計 7.1）。
 */
public interface OperationLogTarget {

  /**
   * 対象キーを返す。
   *
   * @return 伝票番号・コードなど
   */
  String targetKey();

  /**
   * 操作内容を返す。
   *
   * @return 主要な項目の変更内容（パスワードを含めない）。なしは null
   */
  String detail();
}
