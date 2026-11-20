-- =====================================================================
-- V3：アプリケーション用 DB ユーザーの権限（セキュリティ設計 5章、NF-31）
--   DML のみ許可し、操作ログ（t_operation_log）は INSERT・SELECT のみとする。
--   ユーザー名は Flyway のプレースホルダ appDbUser（application*.yml）で渡す。
--   テスト（Testcontainers）など、アプリ用ユーザーが存在しない環境では何もしない。
-- =====================================================================
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${appDbUser}') THEN
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${appDbUser};
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${appDbUser};
    REVOKE UPDATE, DELETE, TRUNCATE ON t_operation_log FROM ${appDbUser};
    -- Flyway の管理テーブルはアプリから変更させない
    REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON flyway_schema_history FROM ${appDbUser};
  END IF;
END
$$;
