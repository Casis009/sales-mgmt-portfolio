-- =====================================================================
-- V1：業務テーブルの作成（テーブル定義書 第1.0版、27テーブル）
--   命名規約：開発標準書 1.2（主キー pk_、外部キー fk_子_親、一意 uq_、CHECK ck_、索引 idx_）
--   PostgreSQL 固有構文は共通処理設計 9章の範囲に限る（IDENTITY、部分インデックス）
--   共通カラム（A）＝ created_at, created_by, updated_at, updated_by
--   共通カラム（B）＝ created_at, created_by
--   created_by / updated_by はバッチ・移行で 0 を使うため、外部キーを設定しない
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 共通
-- ---------------------------------------------------------------------

-- TBL-102 倉庫（m_user が参照するため先に作成する）
CREATE TABLE m_warehouse (
  warehouse_id    BIGINT       GENERATED ALWAYS AS IDENTITY,
  warehouse_code  VARCHAR(10)  NOT NULL,
  warehouse_name  VARCHAR(50)  NOT NULL,
  postal_code     VARCHAR(8),
  address         VARCHAR(200),
  tel             VARCHAR(20),
  enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
  version         INT          NOT NULL DEFAULT 0,
  created_at      TIMESTAMP    NOT NULL,
  created_by      BIGINT       NOT NULL,
  updated_at      TIMESTAMP    NOT NULL,
  updated_by      BIGINT       NOT NULL,
  CONSTRAINT pk_m_warehouse PRIMARY KEY (warehouse_id),
  CONSTRAINT uq_m_warehouse_warehouse_code UNIQUE (warehouse_code)
);
COMMENT ON TABLE m_warehouse IS 'TBL-102 倉庫';

-- TBL-001 ユーザー
CREATE TABLE m_user (
  user_id                   BIGINT        GENERATED ALWAYS AS IDENTITY,
  login_id                  VARCHAR(20)   NOT NULL,
  password_hash             VARCHAR(100)  NOT NULL,
  user_name                 VARCHAR(50)   NOT NULL,
  department                VARCHAR(50),
  default_warehouse_id      BIGINT,
  enabled                   BOOLEAN       NOT NULL DEFAULT TRUE,
  locked                    BOOLEAN       NOT NULL DEFAULT FALSE,
  failed_login_count        SMALLINT      NOT NULL DEFAULT 0,
  password_change_required  BOOLEAN       NOT NULL DEFAULT TRUE,
  last_login_at             TIMESTAMP,
  version                   INT           NOT NULL DEFAULT 0,
  created_at                TIMESTAMP     NOT NULL,
  created_by                BIGINT        NOT NULL,
  updated_at                TIMESTAMP     NOT NULL,
  updated_by                BIGINT        NOT NULL,
  CONSTRAINT pk_m_user PRIMARY KEY (user_id),
  CONSTRAINT uq_m_user_login_id UNIQUE (login_id),
  CONSTRAINT fk_m_user_m_warehouse FOREIGN KEY (default_warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT ck_m_user_failed_login_count CHECK (failed_login_count >= 0)
);
COMMENT ON TABLE m_user IS 'TBL-001 ユーザー';

-- TBL-002 ユーザーロール
CREATE TABLE m_user_role (
  user_id     BIGINT       NOT NULL,
  role_code   VARCHAR(20)  NOT NULL,
  created_at  TIMESTAMP    NOT NULL,
  created_by  BIGINT       NOT NULL,
  CONSTRAINT pk_m_user_role PRIMARY KEY (user_id, role_code),
  CONSTRAINT fk_m_user_role_m_user FOREIGN KEY (user_id) REFERENCES m_user (user_id)
);
COMMENT ON TABLE m_user_role IS 'TBL-002 ユーザーロール';

-- TBL-003 自社情報（1行のみ）
CREATE TABLE m_company (
  company_id               SMALLINT      NOT NULL,
  company_name             VARCHAR(100)  NOT NULL,
  postal_code              VARCHAR(8)    NOT NULL,
  address                  VARCHAR(200)  NOT NULL,
  tel                      VARCHAR(20)   NOT NULL,
  fax                      VARCHAR(20),
  invoice_registration_no  CHAR(14)      NOT NULL,
  bank_account_info        VARCHAR(200)  NOT NULL,
  created_at               TIMESTAMP     NOT NULL,
  created_by               BIGINT        NOT NULL,
  updated_at               TIMESTAMP     NOT NULL,
  updated_by               BIGINT        NOT NULL,
  CONSTRAINT pk_m_company PRIMARY KEY (company_id),
  CONSTRAINT ck_m_company_company_id CHECK (company_id = 1)
);
COMMENT ON TABLE m_company IS 'TBL-003 自社情報';

-- TBL-004 消費税率
CREATE TABLE m_tax_rate (
  tax_category  CHAR(1)       NOT NULL,
  valid_from    DATE          NOT NULL,
  rate          NUMERIC(4,3)  NOT NULL,
  created_at    TIMESTAMP     NOT NULL,
  created_by    BIGINT        NOT NULL,
  updated_at    TIMESTAMP     NOT NULL,
  updated_by    BIGINT        NOT NULL,
  CONSTRAINT pk_m_tax_rate PRIMARY KEY (tax_category, valid_from),
  CONSTRAINT ck_m_tax_rate_rate CHECK (rate >= 0 AND rate < 1)
);
COMMENT ON TABLE m_tax_rate IS 'TBL-004 消費税率';

-- TBL-005 採番
CREATE TABLE t_numbering (
  numbering_type  VARCHAR(2)  NOT NULL,
  year_month      CHAR(4)     NOT NULL,
  current_no      INT         NOT NULL DEFAULT 0,
  updated_at      TIMESTAMP   NOT NULL,
  CONSTRAINT pk_t_numbering PRIMARY KEY (numbering_type, year_month),
  CONSTRAINT ck_t_numbering_current_no CHECK (current_no BETWEEN 0 AND 9999)
);
COMMENT ON TABLE t_numbering IS 'TBL-005 採番';

-- TBL-006 操作ログ（アプリ用ユーザーの UPDATE・DELETE 権限は V3 で取り消す）
CREATE TABLE t_operation_log (
  operation_log_id  BIGINT       GENERATED ALWAYS AS IDENTITY,
  operated_at       TIMESTAMP    NOT NULL,
  user_id           BIGINT,
  login_id          VARCHAR(20),
  ip_address        VARCHAR(45),
  screen_id         VARCHAR(10),
  operation_type    VARCHAR(20)  NOT NULL,
  target_type       VARCHAR(30),
  target_key        VARCHAR(50),
  detail            TEXT,
  result            CHAR(1)      NOT NULL,
  CONSTRAINT pk_t_operation_log PRIMARY KEY (operation_log_id)
);
COMMENT ON TABLE t_operation_log IS 'TBL-006 操作ログ';
CREATE INDEX idx_t_operation_log_operated_at ON t_operation_log (operated_at);
CREATE INDEX idx_t_operation_log_user_id_operated_at ON t_operation_log (user_id, operated_at);

-- ---------------------------------------------------------------------
-- 2. マスタ管理
-- ---------------------------------------------------------------------

-- TBL-103 取引先
CREATE TABLE m_partner (
  partner_id            BIGINT        GENERATED ALWAYS AS IDENTITY,
  partner_code          VARCHAR(10)   NOT NULL,
  partner_name          VARCHAR(100)  NOT NULL,
  partner_name_kana     VARCHAR(100),
  is_customer           BOOLEAN       NOT NULL DEFAULT FALSE,
  is_supplier           BOOLEAN       NOT NULL DEFAULT FALSE,
  postal_code           VARCHAR(8),
  address               VARCHAR(200),
  tel                   VARCHAR(20),
  fax                   VARCHAR(20),
  contact_name          VARCHAR(50),
  closing_day           SMALLINT,
  payment_month_offset  SMALLINT,
  payment_day           SMALLINT,
  enabled               BOOLEAN       NOT NULL DEFAULT TRUE,
  version               INT           NOT NULL DEFAULT 0,
  created_at            TIMESTAMP     NOT NULL,
  created_by            BIGINT        NOT NULL,
  updated_at            TIMESTAMP     NOT NULL,
  updated_by            BIGINT        NOT NULL,
  CONSTRAINT pk_m_partner PRIMARY KEY (partner_id),
  CONSTRAINT uq_m_partner_partner_code UNIQUE (partner_code),
  -- 得意先・仕入先のいずれかであること
  CONSTRAINT ck_m_partner_partner_type CHECK (is_customer OR is_supplier),
  -- 締日・支払日は 1～28 または 99（末日）（コード定義書 11章）
  CONSTRAINT ck_m_partner_closing_day CHECK (closing_day BETWEEN 1 AND 28 OR closing_day = 99),
  CONSTRAINT ck_m_partner_payment_day CHECK (payment_day BETWEEN 1 AND 28 OR payment_day = 99),
  CONSTRAINT ck_m_partner_payment_month_offset CHECK (payment_month_offset BETWEEN 0 AND 2),
  -- 得意先の場合は締日・支払条件が必須（BR-32）
  CONSTRAINT ck_m_partner_customer_terms CHECK (
    NOT is_customer
    OR (closing_day IS NOT NULL AND payment_month_offset IS NOT NULL AND payment_day IS NOT NULL))
);
COMMENT ON TABLE m_partner IS 'TBL-103 取引先';
CREATE INDEX idx_m_partner_partner_name_kana ON m_partner (partner_name_kana);
CREATE INDEX idx_m_partner_is_customer_closing_day ON m_partner (is_customer, closing_day);

-- TBL-101 商品
CREATE TABLE m_product (
  product_id               BIGINT        GENERATED ALWAYS AS IDENTITY,
  product_code             VARCHAR(20)   NOT NULL,
  product_name             VARCHAR(100)  NOT NULL,
  model_number             VARCHAR(50),
  unit                     VARCHAR(10)   NOT NULL,
  standard_sales_price     NUMERIC(9,0)  NOT NULL DEFAULT 0,
  standard_purchase_price  NUMERIC(9,0)  NOT NULL DEFAULT 0,
  tax_category             CHAR(1)       NOT NULL DEFAULT '1',
  main_supplier_id         BIGINT,
  enabled                  BOOLEAN       NOT NULL DEFAULT TRUE,
  version                  INT           NOT NULL DEFAULT 0,
  created_at               TIMESTAMP     NOT NULL,
  created_by               BIGINT        NOT NULL,
  updated_at               TIMESTAMP     NOT NULL,
  updated_by               BIGINT        NOT NULL,
  CONSTRAINT pk_m_product PRIMARY KEY (product_id),
  CONSTRAINT uq_m_product_product_code UNIQUE (product_code),
  CONSTRAINT fk_m_product_m_partner FOREIGN KEY (main_supplier_id) REFERENCES m_partner (partner_id),
  CONSTRAINT ck_m_product_standard_sales_price CHECK (standard_sales_price >= 0),
  CONSTRAINT ck_m_product_standard_purchase_price CHECK (standard_purchase_price >= 0)
);
COMMENT ON TABLE m_product IS 'TBL-101 商品';
CREATE INDEX idx_m_product_product_name ON m_product (product_name);

-- TBL-104 商品倉庫設定
CREATE TABLE m_product_warehouse (
  product_id               BIGINT     NOT NULL,
  warehouse_id             BIGINT     NOT NULL,
  reorder_point            INT        NOT NULL DEFAULT 0,
  standard_order_quantity  INT        NOT NULL DEFAULT 0,
  created_at               TIMESTAMP  NOT NULL,
  created_by               BIGINT     NOT NULL,
  updated_at               TIMESTAMP  NOT NULL,
  updated_by               BIGINT     NOT NULL,
  CONSTRAINT pk_m_product_warehouse PRIMARY KEY (product_id, warehouse_id),
  CONSTRAINT fk_m_product_warehouse_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT fk_m_product_warehouse_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT ck_m_product_warehouse_reorder_point CHECK (reorder_point >= 0),
  CONSTRAINT ck_m_product_warehouse_standard_order_quantity CHECK (standard_order_quantity >= 0)
);
COMMENT ON TABLE m_product_warehouse IS 'TBL-104 商品倉庫設定';

-- ---------------------------------------------------------------------
-- 3. 在庫管理
-- ---------------------------------------------------------------------

-- TBL-201 在庫
CREATE TABLE t_stock (
  product_id          BIGINT     NOT NULL,
  warehouse_id        BIGINT     NOT NULL,
  quantity            INT        NOT NULL DEFAULT 0,
  allocated_quantity  INT        NOT NULL DEFAULT 0,
  created_at          TIMESTAMP  NOT NULL,
  created_by          BIGINT     NOT NULL,
  updated_at          TIMESTAMP  NOT NULL,
  updated_by          BIGINT     NOT NULL,
  CONSTRAINT pk_t_stock PRIMARY KEY (product_id, warehouse_id),
  CONSTRAINT fk_t_stock_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT fk_t_stock_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  -- 現在庫数はマイナス不可（BR-03）。有効在庫数（quantity − allocated_quantity）はマイナスを許容する（D-15）
  CONSTRAINT ck_t_stock_quantity CHECK (quantity >= 0),
  CONSTRAINT ck_t_stock_allocated_quantity CHECK (allocated_quantity >= 0)
);
COMMENT ON TABLE t_stock IS 'TBL-201 在庫';

-- TBL-202 入出庫履歴
CREATE TABLE t_stock_movement (
  stock_movement_id  BIGINT        GENERATED ALWAYS AS IDENTITY,
  movement_date      DATE          NOT NULL,
  product_id         BIGINT        NOT NULL,
  warehouse_id       BIGINT        NOT NULL,
  movement_type      CHAR(2)       NOT NULL,
  quantity           INT           NOT NULL,
  quantity_after     INT           NOT NULL,
  slip_type          VARCHAR(2)    NOT NULL,
  slip_no            VARCHAR(10)   NOT NULL,
  reason             VARCHAR(200),
  created_at         TIMESTAMP     NOT NULL,
  created_by         BIGINT        NOT NULL,
  CONSTRAINT pk_t_stock_movement PRIMARY KEY (stock_movement_id),
  CONSTRAINT fk_t_stock_movement_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT fk_t_stock_movement_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT ck_t_stock_movement_quantity CHECK (quantity <> 0),
  CONSTRAINT ck_t_stock_movement_quantity_after CHECK (quantity_after >= 0)
);
COMMENT ON TABLE t_stock_movement IS 'TBL-202 入出庫履歴';
CREATE INDEX idx_t_stock_movement_product_id_warehouse_id_created_at
  ON t_stock_movement (product_id, warehouse_id, created_at);
CREATE INDEX idx_t_stock_movement_slip_type_slip_no ON t_stock_movement (slip_type, slip_no);
CREATE INDEX idx_t_stock_movement_movement_date ON t_stock_movement (movement_date);

-- TBL-203 棚卸
CREATE TABLE t_stocktaking (
  stocktaking_id    BIGINT       GENERATED ALWAYS AS IDENTITY,
  stocktaking_no    VARCHAR(10)  NOT NULL,
  warehouse_id      BIGINT       NOT NULL,
  stocktaking_date  DATE         NOT NULL,
  status            CHAR(2)      NOT NULL DEFAULT '10',
  confirmed_by      BIGINT,
  confirmed_at      TIMESTAMP,
  version           INT          NOT NULL DEFAULT 0,
  created_at        TIMESTAMP    NOT NULL,
  created_by        BIGINT       NOT NULL,
  updated_at        TIMESTAMP    NOT NULL,
  updated_by        BIGINT       NOT NULL,
  CONSTRAINT pk_t_stocktaking PRIMARY KEY (stocktaking_id),
  CONSTRAINT uq_t_stocktaking_stocktaking_no UNIQUE (stocktaking_no),
  CONSTRAINT fk_t_stocktaking_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT fk_t_stocktaking_m_user FOREIGN KEY (confirmed_by) REFERENCES m_user (user_id)
);
COMMENT ON TABLE t_stocktaking IS 'TBL-203 棚卸';
-- 同じ倉庫で実施中（status = '10'）の棚卸は1件に限る（BR-05）。部分インデックス（共通処理設計 9章 No.4）
CREATE UNIQUE INDEX uq_t_stocktaking_warehouse_id_in_progress
  ON t_stocktaking (warehouse_id) WHERE status = '10';

-- TBL-204 棚卸明細
CREATE TABLE t_stocktaking_detail (
  stocktaking_id     BIGINT        NOT NULL,
  product_id         BIGINT        NOT NULL,
  book_quantity      INT           NOT NULL,
  actual_quantity    INT,
  difference_reason  VARCHAR(200),
  created_at         TIMESTAMP     NOT NULL,
  created_by         BIGINT        NOT NULL,
  updated_at         TIMESTAMP     NOT NULL,
  updated_by         BIGINT        NOT NULL,
  CONSTRAINT pk_t_stocktaking_detail PRIMARY KEY (stocktaking_id, product_id),
  CONSTRAINT fk_t_stocktaking_detail_t_stocktaking FOREIGN KEY (stocktaking_id) REFERENCES t_stocktaking (stocktaking_id),
  CONSTRAINT fk_t_stocktaking_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT ck_t_stocktaking_detail_actual_quantity CHECK (actual_quantity >= 0)
);
COMMENT ON TABLE t_stocktaking_detail IS 'TBL-204 棚卸明細';

-- TBL-205 月次在庫
CREATE TABLE t_stock_monthly (
  target_month        CHAR(6)    NOT NULL,
  product_id          BIGINT     NOT NULL,
  warehouse_id        BIGINT     NOT NULL,
  quantity            INT        NOT NULL,
  allocated_quantity  INT        NOT NULL,
  created_at          TIMESTAMP  NOT NULL,
  created_by          BIGINT     NOT NULL,
  CONSTRAINT pk_t_stock_monthly PRIMARY KEY (target_month, product_id, warehouse_id),
  CONSTRAINT fk_t_stock_monthly_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT fk_t_stock_monthly_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id)
);
COMMENT ON TABLE t_stock_monthly IS 'TBL-205 月次在庫';

-- ---------------------------------------------------------------------
-- 4. 購買管理
-- ---------------------------------------------------------------------

-- TBL-301 発注
CREATE TABLE t_purchase_order (
  purchase_order_id      BIGINT         GENERATED ALWAYS AS IDENTITY,
  purchase_order_no      VARCHAR(10)    NOT NULL,
  order_date             DATE           NOT NULL,
  supplier_id            BIGINT         NOT NULL,
  warehouse_id           BIGINT         NOT NULL,
  expected_arrival_date  DATE           NOT NULL,
  status                 CHAR(2)        NOT NULL DEFAULT '10',
  total_amount           NUMERIC(12,0)  NOT NULL DEFAULT 0,
  applicant_id           BIGINT,
  applied_at             TIMESTAMP,
  approver_id            BIGINT,
  approved_at            TIMESTAMP,
  remaining_cancelled    BOOLEAN        NOT NULL DEFAULT FALSE,
  remarks                VARCHAR(200),
  version                INT            NOT NULL DEFAULT 0,
  created_at             TIMESTAMP      NOT NULL,
  created_by             BIGINT         NOT NULL,
  updated_at             TIMESTAMP      NOT NULL,
  updated_by             BIGINT         NOT NULL,
  CONSTRAINT pk_t_purchase_order PRIMARY KEY (purchase_order_id),
  CONSTRAINT uq_t_purchase_order_purchase_order_no UNIQUE (purchase_order_no),
  CONSTRAINT fk_t_purchase_order_m_partner FOREIGN KEY (supplier_id) REFERENCES m_partner (partner_id),
  CONSTRAINT fk_t_purchase_order_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  -- m_user を2回参照するため、末尾にカラムの役割を付けて区別する
  CONSTRAINT fk_t_purchase_order_m_user_applicant FOREIGN KEY (applicant_id) REFERENCES m_user (user_id),
  CONSTRAINT fk_t_purchase_order_m_user_approver FOREIGN KEY (approver_id) REFERENCES m_user (user_id)
);
COMMENT ON TABLE t_purchase_order IS 'TBL-301 発注';
CREATE INDEX idx_t_purchase_order_status_order_date ON t_purchase_order (status, order_date);
CREATE INDEX idx_t_purchase_order_supplier_id ON t_purchase_order (supplier_id);

-- TBL-302 発注明細
CREATE TABLE t_purchase_order_detail (
  purchase_order_id  BIGINT         NOT NULL,
  line_no            SMALLINT       NOT NULL,
  product_id         BIGINT         NOT NULL,
  quantity           INT            NOT NULL,
  unit_price         NUMERIC(9,0)   NOT NULL,
  amount             NUMERIC(12,0)  NOT NULL,
  tax_category       CHAR(1)        NOT NULL,
  received_quantity  INT            NOT NULL DEFAULT 0,
  created_at         TIMESTAMP      NOT NULL,
  created_by         BIGINT         NOT NULL,
  updated_at         TIMESTAMP      NOT NULL,
  updated_by         BIGINT         NOT NULL,
  CONSTRAINT pk_t_purchase_order_detail PRIMARY KEY (purchase_order_id, line_no),
  CONSTRAINT fk_t_purchase_order_detail_t_purchase_order
    FOREIGN KEY (purchase_order_id) REFERENCES t_purchase_order (purchase_order_id),
  CONSTRAINT fk_t_purchase_order_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT ck_t_purchase_order_detail_line_no CHECK (line_no BETWEEN 1 AND 50),
  CONSTRAINT ck_t_purchase_order_detail_quantity CHECK (quantity >= 1),
  -- 入荷済数量は発注数量を超えない（BR-26）
  CONSTRAINT ck_t_purchase_order_detail_received_quantity
    CHECK (received_quantity >= 0 AND received_quantity <= quantity)
);
COMMENT ON TABLE t_purchase_order_detail IS 'TBL-302 発注明細';
CREATE INDEX idx_t_purchase_order_detail_product_id ON t_purchase_order_detail (product_id);

-- TBL-303 発注承認履歴
CREATE TABLE t_purchase_order_approval (
  approval_id        BIGINT        GENERATED ALWAYS AS IDENTITY,
  purchase_order_id  BIGINT        NOT NULL,
  action             CHAR(2)       NOT NULL,
  comment            VARCHAR(200),
  created_at         TIMESTAMP     NOT NULL,
  created_by         BIGINT        NOT NULL,
  CONSTRAINT pk_t_purchase_order_approval PRIMARY KEY (approval_id),
  CONSTRAINT fk_t_purchase_order_approval_t_purchase_order
    FOREIGN KEY (purchase_order_id) REFERENCES t_purchase_order (purchase_order_id)
);
COMMENT ON TABLE t_purchase_order_approval IS 'TBL-303 発注承認履歴';

-- TBL-304 入荷
CREATE TABLE t_receiving (
  receiving_id       BIGINT        GENERATED ALWAYS AS IDENTITY,
  receiving_no       VARCHAR(10)   NOT NULL,
  purchase_order_id  BIGINT        NOT NULL,
  receiving_date     DATE          NOT NULL,
  remarks            VARCHAR(200),
  created_at         TIMESTAMP     NOT NULL,
  created_by         BIGINT        NOT NULL,
  CONSTRAINT pk_t_receiving PRIMARY KEY (receiving_id),
  CONSTRAINT uq_t_receiving_receiving_no UNIQUE (receiving_no),
  CONSTRAINT fk_t_receiving_t_purchase_order
    FOREIGN KEY (purchase_order_id) REFERENCES t_purchase_order (purchase_order_id)
);
COMMENT ON TABLE t_receiving IS 'TBL-304 入荷';

-- TBL-305 入荷明細
CREATE TABLE t_receiving_detail (
  receiving_id            BIGINT     NOT NULL,
  line_no                 SMALLINT   NOT NULL,
  purchase_order_line_no  SMALLINT   NOT NULL,
  product_id              BIGINT     NOT NULL,
  quantity                INT        NOT NULL,
  created_at              TIMESTAMP  NOT NULL,
  created_by              BIGINT     NOT NULL,
  CONSTRAINT pk_t_receiving_detail PRIMARY KEY (receiving_id, line_no),
  CONSTRAINT fk_t_receiving_detail_t_receiving FOREIGN KEY (receiving_id) REFERENCES t_receiving (receiving_id),
  CONSTRAINT fk_t_receiving_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT ck_t_receiving_detail_quantity CHECK (quantity >= 1)
);
COMMENT ON TABLE t_receiving_detail IS 'TBL-305 入荷明細';

-- TBL-306 発注アラート（BAT-01 が毎回洗い替える）
CREATE TABLE t_reorder_alert (
  reorder_alert_id         BIGINT     GENERATED ALWAYS AS IDENTITY,
  alert_date               DATE       NOT NULL,
  product_id               BIGINT     NOT NULL,
  warehouse_id             BIGINT     NOT NULL,
  quantity                 INT        NOT NULL,
  allocated_quantity       INT        NOT NULL,
  on_order_quantity        INT        NOT NULL,
  reorder_point            INT        NOT NULL,
  standard_order_quantity  INT        NOT NULL,
  created_at               TIMESTAMP  NOT NULL,
  created_by               BIGINT     NOT NULL,
  CONSTRAINT pk_t_reorder_alert PRIMARY KEY (reorder_alert_id),
  CONSTRAINT fk_t_reorder_alert_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT fk_t_reorder_alert_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id)
);
COMMENT ON TABLE t_reorder_alert IS 'TBL-306 発注アラート';

-- ---------------------------------------------------------------------
-- 5. 販売管理
-- ---------------------------------------------------------------------

-- TBL-401 受注
CREATE TABLE t_sales_order (
  sales_order_id       BIGINT         GENERATED ALWAYS AS IDENTITY,
  sales_order_no       VARCHAR(10)    NOT NULL,
  order_date           DATE           NOT NULL,
  customer_id          BIGINT         NOT NULL,
  warehouse_id         BIGINT         NOT NULL,
  delivery_date        DATE           NOT NULL,
  customer_order_no    VARCHAR(30),
  status               CHAR(2)        NOT NULL DEFAULT '10',
  total_amount         NUMERIC(12,0)  NOT NULL DEFAULT 0,
  sales_user_id        BIGINT         NOT NULL,
  remaining_cancelled  BOOLEAN        NOT NULL DEFAULT FALSE,
  remarks              VARCHAR(200),
  version              INT            NOT NULL DEFAULT 0,
  created_at           TIMESTAMP      NOT NULL,
  created_by           BIGINT         NOT NULL,
  updated_at           TIMESTAMP      NOT NULL,
  updated_by           BIGINT         NOT NULL,
  CONSTRAINT pk_t_sales_order PRIMARY KEY (sales_order_id),
  CONSTRAINT uq_t_sales_order_sales_order_no UNIQUE (sales_order_no),
  CONSTRAINT fk_t_sales_order_m_partner FOREIGN KEY (customer_id) REFERENCES m_partner (partner_id),
  CONSTRAINT fk_t_sales_order_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT fk_t_sales_order_m_user FOREIGN KEY (sales_user_id) REFERENCES m_user (user_id)
);
COMMENT ON TABLE t_sales_order IS 'TBL-401 受注';
CREATE INDEX idx_t_sales_order_status_delivery_date ON t_sales_order (status, delivery_date);
CREATE INDEX idx_t_sales_order_customer_id_order_date ON t_sales_order (customer_id, order_date);
CREATE INDEX idx_t_sales_order_order_date ON t_sales_order (order_date);

-- TBL-402 受注明細
CREATE TABLE t_sales_order_detail (
  sales_order_id    BIGINT         NOT NULL,
  line_no           SMALLINT       NOT NULL,
  product_id        BIGINT         NOT NULL,
  quantity          INT            NOT NULL,
  unit_price        NUMERIC(9,0)   NOT NULL,
  amount            NUMERIC(12,0)  NOT NULL,
  tax_category      CHAR(1)        NOT NULL,
  shipped_quantity  INT            NOT NULL DEFAULT 0,
  created_at        TIMESTAMP      NOT NULL,
  created_by        BIGINT         NOT NULL,
  updated_at        TIMESTAMP      NOT NULL,
  updated_by        BIGINT         NOT NULL,
  CONSTRAINT pk_t_sales_order_detail PRIMARY KEY (sales_order_id, line_no),
  CONSTRAINT fk_t_sales_order_detail_t_sales_order
    FOREIGN KEY (sales_order_id) REFERENCES t_sales_order (sales_order_id),
  CONSTRAINT fk_t_sales_order_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT ck_t_sales_order_detail_line_no CHECK (line_no BETWEEN 1 AND 50),
  CONSTRAINT ck_t_sales_order_detail_quantity CHECK (quantity >= 1),
  -- 出荷済数量は受注数量を超えない（BR-14）
  CONSTRAINT ck_t_sales_order_detail_shipped_quantity
    CHECK (shipped_quantity >= 0 AND shipped_quantity <= quantity)
);
COMMENT ON TABLE t_sales_order_detail IS 'TBL-402 受注明細';
CREATE INDEX idx_t_sales_order_detail_product_id ON t_sales_order_detail (product_id);

-- ---------------------------------------------------------------------
-- 6. 請求管理（t_shipment が参照するため、請求を出荷より先に作成する）
-- ---------------------------------------------------------------------

-- TBL-501 請求
CREATE TABLE t_invoice (
  invoice_id         BIGINT         GENERATED ALWAYS AS IDENTITY,
  invoice_no         VARCHAR(10)    NOT NULL,
  customer_id        BIGINT         NOT NULL,
  closing_date       DATE           NOT NULL,
  period_from        DATE           NOT NULL,
  period_to          DATE           NOT NULL,
  taxable_amount_10  NUMERIC(12,0)  NOT NULL DEFAULT 0,
  tax_amount_10      NUMERIC(12,0)  NOT NULL DEFAULT 0,
  taxable_amount_8   NUMERIC(12,0)  NOT NULL DEFAULT 0,
  tax_amount_8       NUMERIC(12,0)  NOT NULL DEFAULT 0,
  total_amount       NUMERIC(12,0)  NOT NULL DEFAULT 0,
  payment_due_date   DATE           NOT NULL,
  status             CHAR(2)        NOT NULL DEFAULT '10',
  issued_at          TIMESTAMP,
  issue_count        SMALLINT       NOT NULL DEFAULT 0,
  version            INT            NOT NULL DEFAULT 0,
  created_at         TIMESTAMP      NOT NULL,
  created_by         BIGINT         NOT NULL,
  updated_at         TIMESTAMP      NOT NULL,
  updated_by         BIGINT         NOT NULL,
  CONSTRAINT pk_t_invoice PRIMARY KEY (invoice_id),
  CONSTRAINT uq_t_invoice_invoice_no UNIQUE (invoice_no),
  CONSTRAINT fk_t_invoice_m_partner FOREIGN KEY (customer_id) REFERENCES m_partner (partner_id),
  CONSTRAINT ck_t_invoice_period CHECK (period_from <= period_to AND period_to = closing_date),
  CONSTRAINT ck_t_invoice_issue_count CHECK (issue_count >= 0)
);
COMMENT ON TABLE t_invoice IS 'TBL-501 請求';
CREATE INDEX idx_t_invoice_customer_id_closing_date ON t_invoice (customer_id, closing_date);
CREATE INDEX idx_t_invoice_closing_date ON t_invoice (closing_date);

-- TBL-403 出荷
CREATE TABLE t_shipment (
  shipment_id     BIGINT        GENERATED ALWAYS AS IDENTITY,
  shipment_no     VARCHAR(10)   NOT NULL,
  sales_order_id  BIGINT        NOT NULL,
  customer_id     BIGINT        NOT NULL,
  warehouse_id    BIGINT        NOT NULL,
  shipment_date   DATE          NOT NULL,
  status          CHAR(2)       NOT NULL DEFAULT '10',
  invoice_id      BIGINT,
  remarks         VARCHAR(200),
  version         INT           NOT NULL DEFAULT 0,
  created_at      TIMESTAMP     NOT NULL,
  created_by      BIGINT        NOT NULL,
  updated_at      TIMESTAMP     NOT NULL,
  updated_by      BIGINT        NOT NULL,
  CONSTRAINT pk_t_shipment PRIMARY KEY (shipment_id),
  CONSTRAINT uq_t_shipment_shipment_no UNIQUE (shipment_no),
  CONSTRAINT fk_t_shipment_t_sales_order FOREIGN KEY (sales_order_id) REFERENCES t_sales_order (sales_order_id),
  CONSTRAINT fk_t_shipment_m_partner FOREIGN KEY (customer_id) REFERENCES m_partner (partner_id),
  CONSTRAINT fk_t_shipment_m_warehouse FOREIGN KEY (warehouse_id) REFERENCES m_warehouse (warehouse_id),
  CONSTRAINT fk_t_shipment_t_invoice FOREIGN KEY (invoice_id) REFERENCES t_invoice (invoice_id)
);
COMMENT ON TABLE t_shipment IS 'TBL-403 出荷';
-- 請求締めの対象抽出（未請求・出荷済）を高速化する部分インデックス（共通処理設計 9章 No.4）
CREATE INDEX idx_t_shipment_customer_id_shipment_date
  ON t_shipment (customer_id, shipment_date) WHERE invoice_id IS NULL AND status = '10';
CREATE INDEX idx_t_shipment_sales_order_id ON t_shipment (sales_order_id);
CREATE INDEX idx_t_shipment_invoice_id ON t_shipment (invoice_id);

-- TBL-404 出荷明細
CREATE TABLE t_shipment_detail (
  shipment_id          BIGINT         NOT NULL,
  line_no              SMALLINT       NOT NULL,
  sales_order_line_no  SMALLINT       NOT NULL,
  product_id           BIGINT         NOT NULL,
  quantity             INT            NOT NULL,
  unit_price           NUMERIC(9,0)   NOT NULL,
  amount               NUMERIC(12,0)  NOT NULL,
  tax_category         CHAR(1)        NOT NULL,
  created_at           TIMESTAMP      NOT NULL,
  created_by           BIGINT         NOT NULL,
  CONSTRAINT pk_t_shipment_detail PRIMARY KEY (shipment_id, line_no),
  CONSTRAINT fk_t_shipment_detail_t_shipment FOREIGN KEY (shipment_id) REFERENCES t_shipment (shipment_id),
  CONSTRAINT fk_t_shipment_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id),
  CONSTRAINT ck_t_shipment_detail_quantity CHECK (quantity >= 1)
);
COMMENT ON TABLE t_shipment_detail IS 'TBL-404 出荷明細';

-- TBL-502 請求明細
CREATE TABLE t_invoice_detail (
  invoice_id        BIGINT         NOT NULL,
  line_no           INT            NOT NULL,
  shipment_id       BIGINT         NOT NULL,
  shipment_line_no  SMALLINT       NOT NULL,
  shipment_date     DATE           NOT NULL,
  shipment_no       VARCHAR(10)    NOT NULL,
  product_id        BIGINT         NOT NULL,
  product_name      VARCHAR(100)   NOT NULL,
  quantity          INT            NOT NULL,
  unit              VARCHAR(10)    NOT NULL,
  unit_price        NUMERIC(9,0)   NOT NULL,
  amount            NUMERIC(12,0)  NOT NULL,
  tax_category      CHAR(1)        NOT NULL,
  created_at        TIMESTAMP      NOT NULL,
  created_by        BIGINT         NOT NULL,
  CONSTRAINT pk_t_invoice_detail PRIMARY KEY (invoice_id, line_no),
  CONSTRAINT fk_t_invoice_detail_t_invoice FOREIGN KEY (invoice_id) REFERENCES t_invoice (invoice_id),
  CONSTRAINT fk_t_invoice_detail_t_shipment FOREIGN KEY (shipment_id) REFERENCES t_shipment (shipment_id),
  CONSTRAINT fk_t_invoice_detail_m_product FOREIGN KEY (product_id) REFERENCES m_product (product_id)
);
COMMENT ON TABLE t_invoice_detail IS 'TBL-502 請求明細';
