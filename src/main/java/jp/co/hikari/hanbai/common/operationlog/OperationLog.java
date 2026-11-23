package jp.co.hikari.hanbai.common.operationlog;

import java.time.LocalDateTime;
import jp.co.hikari.hanbai.common.code.OperationResult;
import jp.co.hikari.hanbai.common.code.OperationType;

/** 操作ログ（TBL-006 t_operation_log）の1行。 */
public class OperationLog {

  private Long operationLogId;
  private LocalDateTime operatedAt;
  private Long userId;
  private String loginId;
  private String ipAddress;
  private String screenId;
  private OperationType operationType;
  private String targetType;
  private String targetKey;
  private String detail;
  private OperationResult result;

  /**
   * 操作ログを生成する。
   *
   * @param operationType 操作種別
   * @param result 操作結果
   * @return 操作ログ（他の項目は setter で設定する）
   */
  public static OperationLog of(OperationType operationType, OperationResult result) {
    OperationLog log = new OperationLog();
    log.operationType = operationType;
    log.result = result;
    return log;
  }

  /**
   * 操作ログIDを返す。
   *
   * @return 操作ログID
   */
  public Long getOperationLogId() {
    return operationLogId;
  }

  /**
   * 操作ログIDを設定する。
   *
   * @param operationLogId 操作ログID
   */
  public void setOperationLogId(Long operationLogId) {
    this.operationLogId = operationLogId;
  }

  /**
   * 操作日時を返す。
   *
   * @return 操作日時
   */
  public LocalDateTime getOperatedAt() {
    return operatedAt;
  }

  /**
   * 操作日時を設定する。
   *
   * @param operatedAt 操作日時
   */
  public void setOperatedAt(LocalDateTime operatedAt) {
    this.operatedAt = operatedAt;
  }

  /**
   * ユーザーIDを返す。
   *
   * @return ユーザーID（ログイン失敗・バッチは null の場合あり）
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * ユーザーIDを設定する。
   *
   * @param userId ユーザーID
   */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /**
   * ログインIDを返す。
   *
   * @return ログインID
   */
  public String getLoginId() {
    return loginId;
  }

  /**
   * ログインIDを設定する。
   *
   * @param loginId ログインID
   */
  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  /**
   * IPアドレスを返す。
   *
   * @return IPアドレス
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * IPアドレスを設定する。
   *
   * @param ipAddress IPアドレス
   */
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * 画面IDを返す。
   *
   * @return 画面ID（例：SCR-405、BAT-01）
   */
  public String getScreenId() {
    return screenId;
  }

  /**
   * 画面IDを設定する。
   *
   * @param screenId 画面ID
   */
  public void setScreenId(String screenId) {
    this.screenId = screenId;
  }

  /**
   * 操作種別を返す。
   *
   * @return 操作種別
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * 操作種別を設定する。
   *
   * @param operationType 操作種別
   */
  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  /**
   * 対象種別を返す。
   *
   * @return 対象種別（テーブルの物理名）
   */
  public String getTargetType() {
    return targetType;
  }

  /**
   * 対象種別を設定する。
   *
   * @param targetType 対象種別
   */
  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  /**
   * 対象キーを返す。
   *
   * @return 対象キー（伝票番号・コードなど）
   */
  public String getTargetKey() {
    return targetKey;
  }

  /**
   * 対象キーを設定する。
   *
   * @param targetKey 対象キー
   */
  public void setTargetKey(String targetKey) {
    this.targetKey = targetKey;
  }

  /**
   * 操作内容を返す。
   *
   * @return 操作内容
   */
  public String getDetail() {
    return detail;
  }

  /**
   * 操作内容を設定する。
   *
   * @param detail 操作内容（パスワードを含めない）
   */
  public void setDetail(String detail) {
    this.detail = detail;
  }

  /**
   * 操作結果を返す。
   *
   * @return 操作結果
   */
  public OperationResult getResult() {
    return result;
  }

  /**
   * 操作結果を設定する。
   *
   * @param result 操作結果
   */
  public void setResult(OperationResult result) {
    this.result = result;
  }
}
