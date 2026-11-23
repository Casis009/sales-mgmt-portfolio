package jp.co.hikari.hanbai.common.operationlog;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 操作ログ（t_operation_log）の SQL。UPDATE・DELETE は定義しない（NF-31）。 */
@Mapper
public interface OperationLogMapper {

  /**
   * 操作ログを登録する。
   *
   * @param log 操作ログ
   * @return 登録件数
   */
  int insert(OperationLog log);

  /**
   * 検索条件に該当する件数を返す（SCR-013）。
   *
   * @param criteria 検索条件
   * @return 件数
   */
  long count(@Param("c") OperationLogCriteria criteria);

  /**
   * 検索条件に該当する操作ログを、操作日時の降順で返す（SCR-013）。
   *
   * @param criteria 検索条件
   * @param limit 取得件数
   * @param offset 読み飛ばす件数
   * @return 操作ログ
   */
  List<OperationLog> search(
      @Param("c") OperationLogCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") long offset);
}
