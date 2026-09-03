-- =============================================================================
-- FRIBOK DATABASE SCHEMA V2
-- Normalized relational schema â€” replaces Java-serialization OBJECT columns.
--
-- Target DB  : HSQLDB 2.7+ (for H2 2.x swap IDENTITY to AUTO_INCREMENT)
-- Predecessor: create_tables.sql (V1, OBJECT-column-per-row approach)
-- No backward compatibility with V1 data.
-- Java class mappings documented in OBJECT_COLUMN_MAPPING.md.
--
-- Table count : ~50 tables (30 original + ~20 child tables for rows/maps)
-- Generated   : 2026-05-04 (Steg 2.2)
-- =============================================================================

-- =============================================================================
-- CATEGORY A : Reference data â€” global lookup tables (no companyid)
--              Java classes: SSCurrency, SSUnit, SSDeliveryWay,
--                            SSDeliveryTerm, SSPaymentTerm
-- =============================================================================
-- CREATE SCHEMA IF NOT EXISTS PUBLIC;
-- 
-- SET SCHEMA PUBLIC;
--
CREATE TABLE IF NOT EXISTS tbl_currency (
    id                 INTEGER IDENTITY,
    code               VARCHAR(10)   NOT NULL,
    description        VARCHAR(255),
    exchange_rate      DECIMAL(18,6) DEFAULT 1.000000 NOT NULL,
    CONSTRAINT pk_currency PRIMARY KEY (id),
    CONSTRAINT uq_currency_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS tbl_unit (
    id                 INTEGER IDENTITY,
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_unit PRIMARY KEY (id),
    CONSTRAINT uq_unit_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS tbl_deliveryway (
    id                 INTEGER IDENTITY,
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_deliveryway PRIMARY KEY (id),
    CONSTRAINT uq_deliveryway_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS tbl_deliveryterm (
    id                 INTEGER IDENTITY,
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_deliveryterm PRIMARY KEY (id),
    CONSTRAINT uq_deliveryterm_name UNIQUE (name)
);

-- SSPaymentTerm.days is explicitly stored and used in the application.
CREATE TABLE IF NOT EXISTS tbl_paymentterm (
    id                 INTEGER IDENTITY,
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    days               INTEGER,
    CONSTRAINT pk_paymentterm PRIMARY KEY (id),
    CONSTRAINT uq_paymentterm_name UNIQUE (name)
);

-- =============================================================================
-- LICENSE (unchanged from V1)
-- =============================================================================

CREATE TABLE IF NOT EXISTS tbl_license (
    licensekey         VARCHAR(255)  NOT NULL,
    CONSTRAINT pk_license PRIMARY KEY (licensekey)
);

-- =============================================================================
-- ACCOUNTING CORE â€” must exist before all tables that reference years/plans
-- Java classes: SSAccountPlan, SSAccount, SSAccountingYear,
--               SSBudget (via tbl_budget_row), SSVoucher, SSVoucherRow
-- =============================================================================

-- Java class: SSAccountPlan
-- child rows in tbl_account below
CREATE TABLE IF NOT EXISTS tbl_accountplan (
    id                 INTEGER IDENTITY,
    name               VARCHAR(255),
    base_name          VARCHAR(255),
    assessment_year    VARCHAR(10),
    plan_type          VARCHAR(50),
    excel_path         VARCHAR(512),
    is_default         BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_accountplan PRIMARY KEY (id)
);

-- Template accounts (child of tbl_accountplan)
CREATE TABLE IF NOT EXISTS tbl_accountplan_account (
    id                   INTEGER IDENTITY,
    accountplan_id       INTEGER       NOT NULL,
    number               INTEGER       NOT NULL,
    description          VARCHAR(255),
    sru_code             VARCHAR(20),
    vat_code             VARCHAR(20),
    report_code          VARCHAR(20),
    active               BOOLEAN       DEFAULT TRUE,
    project_required     BOOLEAN       DEFAULT FALSE,
    result_unit_required BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_accountplan_account PRIMARY KEY (id),
    CONSTRAINT fk_accountplan_account_plan FOREIGN KEY (accountplan_id) REFERENCES tbl_accountplan(id) ON DELETE CASCADE
);

-- =============================================================================
-- COMPANY CATALOG (global company index for multi-schema runtime)
-- =============================================================================
CREATE TABLE IF NOT EXISTS tbl_company_catalog (
    catalog_id          INTEGER IDENTITY,
    schema_name         VARCHAR(20)   NOT NULL,
    company_name        VARCHAR(255)  NOT NULL,
    is_active           BOOLEAN       DEFAULT FALSE NOT NULL,
    CONSTRAINT pk_company_catalog PRIMARY KEY (catalog_id),
    CONSTRAINT uq_company_catalog_schema_name UNIQUE (schema_name)
);

CREATE INDEX IF NOT EXISTS ix_company_catalog_is_active
    ON tbl_company_catalog (is_active);

CREATE INDEX IF NOT EXISTS ix_company_catalog_company_name
    ON tbl_company_catalog (company_name);

-- End of schema V2 PUBLIC



