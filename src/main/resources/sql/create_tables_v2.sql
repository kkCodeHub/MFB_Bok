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

CREATE TABLE IF NOT EXISTS tbl_currency (
    code               VARCHAR(10)   NOT NULL,
    description        VARCHAR(255),
    exchange_rate      DECIMAL(18,6) DEFAULT 1.000000 NOT NULL,
    CONSTRAINT pk_currency PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS tbl_unit (
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_unit PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS tbl_deliveryway (
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_deliveryway PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS tbl_deliveryterm (
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    CONSTRAINT pk_deliveryterm PRIMARY KEY (name)
);

-- SSPaymentTerm.days is not directly stored in Java but derivable from description;
-- kept here for query convenience.
CREATE TABLE IF NOT EXISTS tbl_paymentterm (
    name               VARCHAR(100)  NOT NULL,
    description        VARCHAR(255),
    days               INTEGER,
    CONSTRAINT pk_paymentterm PRIMARY KEY (name)
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
    CONSTRAINT pk_accountplan PRIMARY KEY (id)
);

-- Java class: SSAccount  (child of tbl_accountplan)
CREATE TABLE IF NOT EXISTS tbl_account (
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
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT fk_account_plan FOREIGN KEY (accountplan_id) REFERENCES tbl_accountplan(id)
);

-- =============================================================================
-- CATEGORY B : Master data â€” per company
--              Java classes: SSCompany, SSProject, SSResultUnit,
--                            SSProduct, SSCustomer, SSSupplier
-- =============================================================================

-- Java class: SSCompany
-- Embedded: SSAddress iAddress, SSAddress iDeliveryAddress
-- Child tables: tbl_company_standard_text, tbl_company_default_account
CREATE TABLE IF NOT EXISTS tbl_company (
    id                     INTEGER IDENTITY,
    name                   VARCHAR(255),
    phone                  VARCHAR(50),
    phone2                 VARCHAR(50),
    telefax                VARCHAR(50),
    residence              VARCHAR(255),
    web_address            VARCHAR(255),
    smtp_address           VARCHAR(255),
    email                  VARCHAR(255),
    contact_person         VARCHAR(255),
    tax_registered         BOOLEAN       DEFAULT FALSE,
    corporate_id           VARCHAR(50),
    logotype               VARCHAR(500),
    bank                   VARCHAR(255),
    vat_number             VARCHAR(50),
    bank_account           VARCHAR(50),
    plusgiro               VARCHAR(50),
    iban                   VARCHAR(50),
    swift                  VARCHAR(20),
    delay_interest         DECIMAL(10,4),
    reminder_fee           DECIMAL(10,2),
    estimated_delivery     VARCHAR(100),
    taxrate1               DECIMAL(5,2),
    taxrate2               DECIMAL(5,2),
    taxrate3               DECIMAL(5,2),
    weight_unit            VARCHAR(50),
    volume_unit            VARCHAR(50),
    -- Default references
    currency_code          VARCHAR(10),
    standard_unit          VARCHAR(100),
    default_payment_term   VARCHAR(100),
    default_delivery_term  VARCHAR(100),
    default_delivery_way   VARCHAR(100),
    -- Postal address (SSAddress iAddress)
    addr_name              VARCHAR(255),
    addr_address           VARCHAR(255),
    addr_street            VARCHAR(255),
    addr_zipcode           VARCHAR(20),
    addr_city              VARCHAR(100),
    addr_country           VARCHAR(100),
    -- Delivery address (SSAddress iDeliveryAddress)
    del_addr_name          VARCHAR(255),
    del_addr_address       VARCHAR(255),
    del_addr_street        VARCHAR(255),
    del_addr_zipcode       VARCHAR(20),
    del_addr_city          VARCHAR(100),
    del_addr_country       VARCHAR(100),
    CONSTRAINT pk_company        PRIMARY KEY (id),
    CONSTRAINT fk_co_currency    FOREIGN KEY (currency_code)         REFERENCES tbl_currency(code),
    CONSTRAINT fk_co_unit        FOREIGN KEY (standard_unit)         REFERENCES tbl_unit(name),
    CONSTRAINT fk_co_payterm     FOREIGN KEY (default_payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_co_delterm     FOREIGN KEY (default_delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_co_delway      FOREIGN KEY (default_delivery_way)  REFERENCES tbl_deliveryway(name)
);

-- Per-company standard texts (Map<SSStandardText, String>)
CREATE TABLE IF NOT EXISTS tbl_company_standard_text (
    company_id         INTEGER       NOT NULL,
    text_type          VARCHAR(50)   NOT NULL,
    text_value         CLOB,
    CONSTRAINT pk_co_std_text PRIMARY KEY (company_id, text_type),
    CONSTRAINT fk_cst_company FOREIGN KEY (company_id) REFERENCES tbl_company(id)
);

-- Per-company default account mappings (Map<SSDefaultAccount, Integer>)
CREATE TABLE IF NOT EXISTS tbl_company_default_account (
    company_id         INTEGER       NOT NULL,
    account_type       VARCHAR(50)   NOT NULL,
    account_nr         INTEGER       NOT NULL,
    CONSTRAINT pk_co_def_acc PRIMARY KEY (company_id, account_type),
    CONSTRAINT fk_cda_company FOREIGN KEY (company_id) REFERENCES tbl_company(id)
);

-- Java class: SSAccountingYear
-- iVouchers stored separately in tbl_voucher (FK yearid)
-- iBudget    stored in tbl_budget_row
-- iInBalance stored in tbl_year_balance
CREATE TABLE IF NOT EXISTS tbl_accountingyear (
    id                 INTEGER IDENTITY,
    companyid          INTEGER       NOT NULL,
    from_date          DATE          NOT NULL,
    to_date            DATE          NOT NULL,
    accountplan_id     INTEGER,
    CONSTRAINT pk_accountingyear PRIMARY KEY (id),
    CONSTRAINT fk_year_company   FOREIGN KEY (companyid)      REFERENCES tbl_company(id),
    CONSTRAINT fk_year_plan      FOREIGN KEY (accountplan_id) REFERENCES tbl_accountplan(id)
);

-- Opening balances per account (Map<SSAccount, BigDecimal> iInBalance)
CREATE TABLE IF NOT EXISTS tbl_year_balance (
    year_id            INTEGER       NOT NULL,
    account_nr         INTEGER       NOT NULL,
    balance            DECIMAL(18,2) DEFAULT 0 NOT NULL,
    CONSTRAINT pk_year_balance PRIMARY KEY (year_id, account_nr),
    CONSTRAINT fk_yb_year FOREIGN KEY (year_id) REFERENCES tbl_accountingyear(id)
);

-- Budget per account per month (Map<SSMonth, Map<SSAccount, BigDecimal>>)
CREATE TABLE IF NOT EXISTS tbl_budget_row (
    id                 INTEGER IDENTITY,
    year_id            INTEGER       NOT NULL,
    account_nr         INTEGER       NOT NULL,
    month              INTEGER       NOT NULL,  -- 1=Jan â€¦ 12=Dec
    amount             DECIMAL(18,2) DEFAULT 0 NOT NULL,
    CONSTRAINT pk_budget_row PRIMARY KEY (id),
    CONSTRAINT uq_budget_row UNIQUE (year_id, account_nr, month),
    CONSTRAINT fk_br_year FOREIGN KEY (year_id) REFERENCES tbl_accountingyear(id)
);

-- Java class: SSVoucher
-- iCorrects / iCorrectedBy are self-referential FKs
CREATE TABLE IF NOT EXISTS tbl_voucher (
    id                 INTEGER IDENTITY,
    number             INTEGER       NOT NULL,
    yearid             INTEGER       NOT NULL,
    vdate              DATE,
    description        VARCHAR(500),
    corrects_id        INTEGER,
    corrected_by_id    INTEGER,
    CONSTRAINT pk_voucher PRIMARY KEY (id),
    CONSTRAINT fk_voucher_year   FOREIGN KEY (yearid)          REFERENCES tbl_accountingyear(id),
    CONSTRAINT fk_voucher_corr   FOREIGN KEY (corrects_id)     REFERENCES tbl_voucher(id),
    CONSTRAINT fk_voucher_corrby FOREIGN KEY (corrected_by_id) REFERENCES tbl_voucher(id)
);

-- Java class: SSVoucherRow  (child of tbl_voucher)
CREATE TABLE IF NOT EXISTS tbl_voucher_row (
    id                 INTEGER IDENTITY,
    voucher_id         INTEGER       NOT NULL,
    account_nr         INTEGER       NOT NULL,
    project_number     VARCHAR(50),
    result_unit_number VARCHAR(50),
    debet              DECIMAL(18,2),
    credit             DECIMAL(18,2),
    edited_date        DATE,
    edited_signature   VARCHAR(100),
    crossed            BOOLEAN       DEFAULT FALSE,
    added              BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_voucher_row PRIMARY KEY (id),
    CONSTRAINT fk_vrow_voucher FOREIGN KEY (voucher_id) REFERENCES tbl_voucher(id)
);

-- Java class: SSProject
CREATE TABLE IF NOT EXISTS tbl_project (
    number             VARCHAR(50)   NOT NULL,
    companyid          INTEGER       NOT NULL,
    name               VARCHAR(255),
    description        CLOB,
    concluded          BOOLEAN       DEFAULT FALSE,
    concluded_date     DATE,
    CONSTRAINT pk_project PRIMARY KEY (number, companyid),
    CONSTRAINT fk_proj_company FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

-- Java class: SSResultUnit
CREATE TABLE IF NOT EXISTS tbl_resultunit (
    number             VARCHAR(50)   NOT NULL,
    companyid          INTEGER       NOT NULL,
    name               VARCHAR(255),
    description        CLOB,
    CONSTRAINT pk_resultunit PRIMARY KEY (number, companyid),
    CONSTRAINT fk_ru_company FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

-- Java class: SSProduct
-- iDefaultAccounts (Map<SSDefaultAccount, Integer>) â†’ tbl_product_account
CREATE TABLE IF NOT EXISTS tbl_product (
    id                   INTEGER IDENTITY,
    number               VARCHAR(50)   NOT NULL,
    companyid            INTEGER       NOT NULL,
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    tax_code             VARCHAR(10),
    warehouse_location   VARCHAR(100),
    orderpoint           INTEGER,
    ordercount           INTEGER,
    purchase_price       DECIMAL(18,4),
    stock_price          DECIMAL(18,4),
    freight              DECIMAL(18,4),
    supplier_nr          VARCHAR(50),
    supplier_product_nr  VARCHAR(50),
    expired              BOOLEAN       DEFAULT FALSE,
    stock_goods          BOOLEAN       DEFAULT FALSE,
    unit                 VARCHAR(100),
    weight               DECIMAL(10,3),
    volume               DECIMAL(10,3),
    project_number       VARCHAR(50),
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT fk_prod_company FOREIGN KEY (companyid) REFERENCES tbl_company(id),
    CONSTRAINT fk_prod_unit    FOREIGN KEY (unit)      REFERENCES tbl_unit(name)
);

-- Default account mappings per product (Map<SSDefaultAccount, Integer>)
CREATE TABLE IF NOT EXISTS tbl_product_account (
    product_id         INTEGER       NOT NULL,
    account_type       VARCHAR(50)   NOT NULL,
    account_nr         INTEGER       NOT NULL,
    CONSTRAINT pk_product_account PRIMARY KEY (product_id, account_type),
    CONSTRAINT fk_pa_product FOREIGN KEY (product_id) REFERENCES tbl_product(id)
);

-- Java class: SSCustomer
-- Embedded: SSAddress iInvoiceAddress, SSAddress iDeliveryAddress
CREATE TABLE IF NOT EXISTS tbl_customer (
    id                   INTEGER IDENTITY,
    number               VARCHAR(50)   NOT NULL,
    companyid            INTEGER       NOT NULL,
    name                 VARCHAR(255),
    email                VARCHAR(255),
    phone                VARCHAR(50),
    phone2               VARCHAR(50),
    telefax              VARCHAR(50),
    registration_number  VARCHAR(50),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    vat_number           VARCHAR(50),
    bankgiro             VARCHAR(50),
    plusgiro             VARCHAR(50),
    account_number       VARCHAR(50),
    clearing_number      VARCHAR(20),
    eu_sale_commodity    BOOLEAN       DEFAULT FALSE,
    eu_sale_third_part   BOOLEAN       DEFAULT FALSE,
    vat_free_sale        BOOLEAN       DEFAULT FALSE,
    hide_unitprice       BOOLEAN       DEFAULT FALSE,
    credit_limit         DECIMAL(18,2),
    discount             DECIMAL(5,2),
    comment              CLOB,
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    -- Invoice address (SSAddress)
    inv_addr_name        VARCHAR(255),
    inv_addr_address     VARCHAR(255),
    inv_addr_street      VARCHAR(255),
    inv_addr_zipcode     VARCHAR(20),
    inv_addr_city        VARCHAR(100),
    inv_addr_country     VARCHAR(100),
    -- Delivery address (SSAddress)
    del_addr_name        VARCHAR(255),
    del_addr_address     VARCHAR(255),
    del_addr_street      VARCHAR(255),
    del_addr_zipcode     VARCHAR(20),
    del_addr_city        VARCHAR(100),
    del_addr_country     VARCHAR(100),
    CONSTRAINT pk_customer      PRIMARY KEY (id),
    CONSTRAINT fk_cust_company  FOREIGN KEY (companyid)    REFERENCES tbl_company(id),
    CONSTRAINT fk_cust_currency FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_cust_payterm  FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_cust_delterm  FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_cust_delway   FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

-- Java class: SSSupplier
-- Embedded: SSAddress iAddress
CREATE TABLE IF NOT EXISTS tbl_supplier (
    id                   INTEGER IDENTITY,
    number               VARCHAR(50)   NOT NULL,
    companyid            INTEGER       NOT NULL,
    name                 VARCHAR(255),
    phone                VARCHAR(50),
    phone2               VARCHAR(50),
    telefax              VARCHAR(50),
    email                VARCHAR(255),
    homepage             VARCHAR(255),
    registration_number  VARCHAR(50),
    your_contact         VARCHAR(255),
    our_contact          VARCHAR(255),
    our_customer_nr      VARCHAR(50),
    bankgiro             VARCHAR(50),
    plusgiro             VARCHAR(50),
    outpayment_number    INTEGER,
    comment              CLOB,
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    -- Address (SSAddress)
    addr_name            VARCHAR(255),
    addr_address         VARCHAR(255),
    addr_street          VARCHAR(255),
    addr_zipcode         VARCHAR(20),
    addr_city            VARCHAR(100),
    addr_country         VARCHAR(100),
    CONSTRAINT pk_supplier      PRIMARY KEY (id),
    CONSTRAINT fk_supp_company  FOREIGN KEY (companyid)    REFERENCES tbl_company(id),
    CONSTRAINT fk_supp_currency FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_supp_payterm  FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_supp_delterm  FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_supp_delway   FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

-- =============================================================================
-- CATEGORY D : Sales transactions  (SSSale subclasses + row children)
--              Java classes: SSInvoice, SSCreditInvoice, SSPeriodicInvoice,
--                            SSOrder, SSTender
-- SSSale base fields are repeated in each table (flat denormalization).
-- =============================================================================

-- Java class: SSInvoice  (extends SSSale)
-- iVoucher â†’ FK to tbl_voucher; iRows â†’ tbl_invoice_row
CREATE TABLE IF NOT EXISTS tbl_invoice (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    -- SSSale base fields
    vdate                DATE,
    customer_nr          VARCHAR(50),
    customer_name        VARCHAR(255),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    delay_interest       DECIMAL(5,2),
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    tax_free             BOOLEAN       DEFAULT FALSE,
    sale_text            CLOB,
    eu_sale_commodity    BOOLEAN       DEFAULT FALSE,
    eu_sale_third_part   BOOLEAN       DEFAULT FALSE,
    printed              BOOLEAN       DEFAULT FALSE,
    -- SSInvoice specific
    invoice_type         VARCHAR(20),
    currency_rate        DECIMAL(10,6),
    payment_day          DATE,
    your_order_number    VARCHAR(100),
    ocr_number           VARCHAR(50),
    entered              BOOLEAN       DEFAULT FALSE,
    num_reminders        INTEGER       DEFAULT 0,
    interest_invoiced    BOOLEAN       DEFAULT FALSE,
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    order_numbers        VARCHAR(500),
    voucher_id           INTEGER,
    -- Invoice address (SSAddress)
    inv_addr_name        VARCHAR(255),
    inv_addr_address     VARCHAR(255),
    inv_addr_street      VARCHAR(255),
    inv_addr_zipcode     VARCHAR(20),
    inv_addr_city        VARCHAR(100),
    inv_addr_country     VARCHAR(100),
    -- Delivery address (SSAddress)
    del_addr_name        VARCHAR(255),
    del_addr_address     VARCHAR(255),
    del_addr_street      VARCHAR(255),
    del_addr_zipcode     VARCHAR(20),
    del_addr_city        VARCHAR(100),
    del_addr_country     VARCHAR(100),
    CONSTRAINT pk_invoice        PRIMARY KEY (id),
    CONSTRAINT fk_inv_company    FOREIGN KEY (companyid)   REFERENCES tbl_company(id),
    CONSTRAINT fk_inv_voucher    FOREIGN KEY (voucher_id)  REFERENCES tbl_voucher(id),
    CONSTRAINT fk_inv_currency   FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_inv_payterm    FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_inv_delterm    FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_inv_delway     FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

-- Java class: SSSaleRow  (child of tbl_invoice)
CREATE TABLE IF NOT EXISTS tbl_invoice_row (
    id                   INTEGER IDENTITY,
    invoice_id           INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    count                INTEGER,
    unit                 VARCHAR(100),
    discount             DECIMAL(5,2),
    tax_code             VARCHAR(10),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_invoice_row PRIMARY KEY (id),
    CONSTRAINT fk_inv_row_invoice FOREIGN KEY (invoice_id) REFERENCES tbl_invoice(id)
);

-- Java class: SSCreditInvoice  (extends SSInvoice â€” single extra field)
CREATE TABLE IF NOT EXISTS tbl_creditinvoice (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    crediting_nr         INTEGER,
    -- SSSale + SSInvoice fields (same as tbl_invoice)
    vdate                DATE,
    customer_nr          VARCHAR(50),
    customer_name        VARCHAR(255),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    delay_interest       DECIMAL(5,2),
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    tax_free             BOOLEAN       DEFAULT FALSE,
    sale_text            CLOB,
    eu_sale_commodity    BOOLEAN       DEFAULT FALSE,
    eu_sale_third_part   BOOLEAN       DEFAULT FALSE,
    printed              BOOLEAN       DEFAULT FALSE,
    invoice_type         VARCHAR(20),
    currency_rate        DECIMAL(10,6),
    payment_day          DATE,
    your_order_number    VARCHAR(100),
    ocr_number           VARCHAR(50),
    entered              BOOLEAN       DEFAULT FALSE,
    num_reminders        INTEGER       DEFAULT 0,
    interest_invoiced    BOOLEAN       DEFAULT FALSE,
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    order_numbers        VARCHAR(500),
    voucher_id           INTEGER,
    inv_addr_name        VARCHAR(255),  inv_addr_address   VARCHAR(255),
    inv_addr_street      VARCHAR(255),  inv_addr_zipcode   VARCHAR(20),
    inv_addr_city        VARCHAR(100),  inv_addr_country   VARCHAR(100),
    del_addr_name        VARCHAR(255),  del_addr_address   VARCHAR(255),
    del_addr_street      VARCHAR(255),  del_addr_zipcode   VARCHAR(20),
    del_addr_city        VARCHAR(100),  del_addr_country   VARCHAR(100),
    CONSTRAINT pk_creditinvoice     PRIMARY KEY (id),
    CONSTRAINT fk_cinv_company      FOREIGN KEY (companyid)   REFERENCES tbl_company(id),
    CONSTRAINT fk_cinv_voucher      FOREIGN KEY (voucher_id)  REFERENCES tbl_voucher(id),
    CONSTRAINT fk_cinv_currency     FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_cinv_payterm      FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_cinv_delterm      FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_cinv_delway       FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

CREATE TABLE IF NOT EXISTS tbl_creditinvoice_row (
    id                   INTEGER IDENTITY,
    creditinvoice_id     INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    count                INTEGER,
    unit                 VARCHAR(100),
    discount             DECIMAL(5,2),
    tax_code             VARCHAR(10),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_creditinvoice_row PRIMARY KEY (id),
    CONSTRAINT fk_cinv_row_cinv FOREIGN KEY (creditinvoice_id) REFERENCES tbl_creditinvoice(id)
);

-- Java class: SSPeriodicInvoice
-- iTemplate (SSInvoice) stored by FK; iInvoices (List<SSInvoice>) by FK back
-- iAdded (Map<Integer,Boolean>) â†’ tbl_periodicinvoice_added
CREATE TABLE IF NOT EXISTS tbl_periodicinvoice (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    count                INTEGER,
    period               INTEGER,      -- months
    description          VARCHAR(500),
    period_start         DATE,
    period_end           DATE,
    append_period        BOOLEAN       DEFAULT FALSE,
    append_information   BOOLEAN       DEFAULT FALSE,
    information          CLOB,
    -- Template invoice fields (denormalized from SSInvoice for the template)
    customer_nr          VARCHAR(50),
    customer_name        VARCHAR(255),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    delay_interest       DECIMAL(5,2),
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    tax_free             BOOLEAN       DEFAULT FALSE,
    sale_text            CLOB,
    printed              BOOLEAN       DEFAULT FALSE,
    currency_rate        DECIMAL(10,6),
    payment_day          DATE,
    your_order_number    VARCHAR(100),
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    inv_addr_name        VARCHAR(255),  inv_addr_address   VARCHAR(255),
    inv_addr_street      VARCHAR(255),  inv_addr_zipcode   VARCHAR(20),
    inv_addr_city        VARCHAR(100),  inv_addr_country   VARCHAR(100),
    del_addr_name        VARCHAR(255),  del_addr_address   VARCHAR(255),
    del_addr_street      VARCHAR(255),  del_addr_zipcode   VARCHAR(20),
    del_addr_city        VARCHAR(100),  del_addr_country   VARCHAR(100),
    CONSTRAINT pk_periodicinvoice PRIMARY KEY (id),
    CONSTRAINT fk_pinv_company    FOREIGN KEY (companyid)     REFERENCES tbl_company(id),
    CONSTRAINT fk_pinv_currency   FOREIGN KEY (currency_code)  REFERENCES tbl_currency(code),
    CONSTRAINT fk_pinv_payterm    FOREIGN KEY (payment_term)   REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_pinv_delterm    FOREIGN KEY (delivery_term)  REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_pinv_delway     FOREIGN KEY (delivery_way)   REFERENCES tbl_deliveryway(name)
);

CREATE TABLE IF NOT EXISTS tbl_periodicinvoice_row (
    id                   INTEGER IDENTITY,
    periodicinvoice_id   INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    count                INTEGER,
    unit                 VARCHAR(100),
    discount             DECIMAL(5,2),
    tax_code             VARCHAR(10),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_pinv_row PRIMARY KEY (id),
    CONSTRAINT fk_pinv_row_pinv FOREIGN KEY (periodicinvoice_id) REFERENCES tbl_periodicinvoice(id)
);

-- Generated invoices linked to periodic invoice (List<SSInvoice>)
-- Relationship: invoice.periodicinvoice_id references tbl_periodicinvoice
ALTER TABLE tbl_invoice ADD COLUMN periodicinvoice_id INTEGER;
ALTER TABLE tbl_invoice ADD CONSTRAINT fk_inv_periodic
    FOREIGN KEY (periodicinvoice_id) REFERENCES tbl_periodicinvoice(id);

-- iAdded map (Map<Integer invoiceNr, Boolean added>)
CREATE TABLE IF NOT EXISTS tbl_periodicinvoice_added (
    periodicinvoice_id   INTEGER       NOT NULL,
    invoice_nr           INTEGER       NOT NULL,
    added                BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_pinv_added PRIMARY KEY (periodicinvoice_id, invoice_nr),
    CONSTRAINT fk_pinv_added_pinv FOREIGN KEY (periodicinvoice_id) REFERENCES tbl_periodicinvoice(id)
);

-- Java class: SSOrder  (extends SSSale)
CREATE TABLE IF NOT EXISTS tbl_order (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    -- SSSale base
    vdate                DATE,
    customer_nr          VARCHAR(50),
    customer_name        VARCHAR(255),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    delay_interest       DECIMAL(5,2),
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    tax_free             BOOLEAN       DEFAULT FALSE,
    sale_text            CLOB,
    eu_sale_commodity    BOOLEAN       DEFAULT FALSE,
    eu_sale_third_part   BOOLEAN       DEFAULT FALSE,
    printed              BOOLEAN       DEFAULT FALSE,
    -- SSOrder specific
    your_order_number    VARCHAR(100),
    estimated_delivery   VARCHAR(100),
    invoice_nr           INTEGER,
    periodicinvoice_nr   INTEGER,
    purchaseorder_nr     INTEGER,
    hide_unitprice       BOOLEAN       DEFAULT FALSE,
    currency_rate        DECIMAL(10,6),
    inv_addr_name        VARCHAR(255),  inv_addr_address   VARCHAR(255),
    inv_addr_street      VARCHAR(255),  inv_addr_zipcode   VARCHAR(20),
    inv_addr_city        VARCHAR(100),  inv_addr_country   VARCHAR(100),
    del_addr_name        VARCHAR(255),  del_addr_address   VARCHAR(255),
    del_addr_street      VARCHAR(255),  del_addr_zipcode   VARCHAR(20),
    del_addr_city        VARCHAR(100),  del_addr_country   VARCHAR(100),
    CONSTRAINT pk_order       PRIMARY KEY (id),
    CONSTRAINT fk_ord_company FOREIGN KEY (companyid)    REFERENCES tbl_company(id),
    CONSTRAINT fk_ord_currency  FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_ord_payterm   FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_ord_delterm   FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_ord_delway    FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

CREATE TABLE IF NOT EXISTS tbl_order_row (
    id                   INTEGER IDENTITY,
    order_id             INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    count                INTEGER,
    unit                 VARCHAR(100),
    discount             DECIMAL(5,2),
    tax_code             VARCHAR(10),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_order_row PRIMARY KEY (id),
    CONSTRAINT fk_ord_row_order FOREIGN KEY (order_id) REFERENCES tbl_order(id)
);

-- Java class: SSTender  (extends SSSale)
CREATE TABLE IF NOT EXISTS tbl_tender (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    -- SSSale base
    vdate                DATE,
    customer_nr          VARCHAR(50),
    customer_name        VARCHAR(255),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    delay_interest       DECIMAL(5,2),
    currency_code        VARCHAR(10),
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    tax_free             BOOLEAN       DEFAULT FALSE,
    sale_text            CLOB,
    eu_sale_commodity    BOOLEAN       DEFAULT FALSE,
    eu_sale_third_part   BOOLEAN       DEFAULT FALSE,
    printed              BOOLEAN       DEFAULT FALSE,
    -- SSTender specific
    expires              DATE,
    order_nr             INTEGER,
    currency_rate        DECIMAL(10,6),
    inv_addr_name        VARCHAR(255),  inv_addr_address   VARCHAR(255),
    inv_addr_street      VARCHAR(255),  inv_addr_zipcode   VARCHAR(20),
    inv_addr_city        VARCHAR(100),  inv_addr_country   VARCHAR(100),
    del_addr_name        VARCHAR(255),  del_addr_address   VARCHAR(255),
    del_addr_street      VARCHAR(255),  del_addr_zipcode   VARCHAR(20),
    del_addr_city        VARCHAR(100),  del_addr_country   VARCHAR(100),
    CONSTRAINT pk_tender       PRIMARY KEY (id),
    CONSTRAINT fk_tend_company FOREIGN KEY (companyid)    REFERENCES tbl_company(id),
    CONSTRAINT fk_tend_currency  FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_tend_payterm   FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_tend_delterm   FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_tend_delway    FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

CREATE TABLE IF NOT EXISTS tbl_tender_row (
    id                   INTEGER IDENTITY,
    tender_id            INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    count                INTEGER,
    unit                 VARCHAR(100),
    discount             DECIMAL(5,2),
    tax_code             VARCHAR(10),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_tender_row PRIMARY KEY (id),
    CONSTRAINT fk_tend_row_tender FOREIGN KEY (tender_id) REFERENCES tbl_tender(id)
);

-- =============================================================================
-- CATEGORY E : Purchase transactions
--              Java classes: SSSupplierInvoice, SSSupplierCreditInvoice,
--                            SSPurchaseOrder
-- =============================================================================

-- Java class: SSSupplierInvoice
-- iVoucher, iCorrection â†’ tbl_voucher FKs
CREATE TABLE IF NOT EXISTS tbl_supplierinvoice (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    due_date             DATE,
    supplier_nr          VARCHAR(50),
    supplier_name        VARCHAR(255),
    reference_number     VARCHAR(100),
    currency_code        VARCHAR(10),
    currency_rate        DECIMAL(10,6),
    payment_term         VARCHAR(100),
    tax_sum              DECIMAL(18,2),
    rounding_sum         DECIMAL(18,2),
    entered              BOOLEAN       DEFAULT FALSE,
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    bgc_entered          BOOLEAN       DEFAULT FALSE,
    voucher_id           INTEGER,
    correction_voucher_id INTEGER,
    CONSTRAINT pk_supplierinvoice    PRIMARY KEY (id),
    CONSTRAINT fk_sinv_company       FOREIGN KEY (companyid)          REFERENCES tbl_company(id),
    CONSTRAINT fk_sinv_currency      FOREIGN KEY (currency_code)       REFERENCES tbl_currency(code),
    CONSTRAINT fk_sinv_payterm       FOREIGN KEY (payment_term)        REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_sinv_voucher       FOREIGN KEY (voucher_id)          REFERENCES tbl_voucher(id),
    CONSTRAINT fk_sinv_corr_voucher  FOREIGN KEY (correction_voucher_id) REFERENCES tbl_voucher(id)
);

-- Java class: SSSupplierInvoiceRow  (child of tbl_supplierinvoice)
CREATE TABLE IF NOT EXISTS tbl_supplierinvoice_row (
    id                   INTEGER IDENTITY,
    supplierinvoice_id   INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    unitprice            DECIMAL(18,4),
    quantity             INTEGER,
    unit                 VARCHAR(100),
    unit_freight         DECIMAL(18,4),
    account_nr           INTEGER,
    project_number       VARCHAR(50),
    result_unit_number   VARCHAR(50),
    CONSTRAINT pk_supplierinvoice_row PRIMARY KEY (id),
    CONSTRAINT fk_sinv_row_sinv FOREIGN KEY (supplierinvoice_id) REFERENCES tbl_supplierinvoice(id)
);

-- Java class: SSSupplierCreditInvoice  (extends SSSupplierInvoice)
CREATE TABLE IF NOT EXISTS tbl_suppliercreditinvoice (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    crediting_nr         INTEGER,
    vdate                DATE,
    due_date             DATE,
    supplier_nr          VARCHAR(50),
    supplier_name        VARCHAR(255),
    reference_number     VARCHAR(100),
    currency_code        VARCHAR(10),
    currency_rate        DECIMAL(10,6),
    payment_term         VARCHAR(100),
    tax_sum              DECIMAL(18,2),
    rounding_sum         DECIMAL(18,2),
    entered              BOOLEAN       DEFAULT FALSE,
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    bgc_entered          BOOLEAN       DEFAULT FALSE,
    voucher_id           INTEGER,
    correction_voucher_id INTEGER,
    CONSTRAINT pk_suppliercreditinvoice   PRIMARY KEY (id),
    CONSTRAINT fk_sci_company             FOREIGN KEY (companyid)             REFERENCES tbl_company(id),
    CONSTRAINT fk_sci_currency            FOREIGN KEY (currency_code)          REFERENCES tbl_currency(code),
    CONSTRAINT fk_sci_payterm             FOREIGN KEY (payment_term)           REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_sci_voucher             FOREIGN KEY (voucher_id)             REFERENCES tbl_voucher(id),
    CONSTRAINT fk_sci_corr_voucher        FOREIGN KEY (correction_voucher_id)  REFERENCES tbl_voucher(id)
);

CREATE TABLE IF NOT EXISTS tbl_suppliercreditinvoice_row (
    id                       INTEGER IDENTITY,
    suppliercreditinvoice_id INTEGER       NOT NULL,
    product_nr               VARCHAR(50),
    description              VARCHAR(500),
    unitprice                DECIMAL(18,4),
    quantity                 INTEGER,
    unit                     VARCHAR(100),
    unit_freight             DECIMAL(18,4),
    account_nr               INTEGER,
    project_number           VARCHAR(50),
    result_unit_number       VARCHAR(50),
    CONSTRAINT pk_sci_row PRIMARY KEY (id),
    CONSTRAINT fk_sci_row_sci FOREIGN KEY (suppliercreditinvoice_id) REFERENCES tbl_suppliercreditinvoice(id)
);

-- Java class: SSPurchaseOrder
-- iDefaultAccounts â†’ tbl_purchaseorder_account
CREATE TABLE IF NOT EXISTS tbl_purchaseorder (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    invoice_nr           INTEGER,
    vdate                DATE,
    supplier_nr          VARCHAR(50),
    supplier_name        VARCHAR(255),
    estimated_delivery   DATE,
    payment_term         VARCHAR(100),
    delivery_term        VARCHAR(100),
    delivery_way         VARCHAR(100),
    our_contact          VARCHAR(255),
    your_contact         VARCHAR(255),
    currency_code        VARCHAR(10),
    currency_rate        DECIMAL(10,6),
    sale_text            CLOB,
    printed              BOOLEAN       DEFAULT FALSE,
    stock_influencing    BOOLEAN       DEFAULT TRUE,
    -- Delivery address (SSAddress iDeliveryAddress)
    del_addr_name        VARCHAR(255),  del_addr_address   VARCHAR(255),
    del_addr_street      VARCHAR(255),  del_addr_zipcode   VARCHAR(20),
    del_addr_city        VARCHAR(100),  del_addr_country   VARCHAR(100),
    -- Supplier address (SSAddress iSupplierAddress)
    supp_addr_name       VARCHAR(255),  supp_addr_address  VARCHAR(255),
    supp_addr_street     VARCHAR(255),  supp_addr_zipcode  VARCHAR(20),
    supp_addr_city       VARCHAR(100),  supp_addr_country  VARCHAR(100),
    CONSTRAINT pk_purchaseorder    PRIMARY KEY (id),
    CONSTRAINT fk_po_company       FOREIGN KEY (companyid)    REFERENCES tbl_company(id),
    CONSTRAINT fk_po_currency      FOREIGN KEY (currency_code) REFERENCES tbl_currency(code),
    CONSTRAINT fk_po_payterm       FOREIGN KEY (payment_term)  REFERENCES tbl_paymentterm(name),
    CONSTRAINT fk_po_delterm       FOREIGN KEY (delivery_term) REFERENCES tbl_deliveryterm(name),
    CONSTRAINT fk_po_delway        FOREIGN KEY (delivery_way)  REFERENCES tbl_deliveryway(name)
);

CREATE TABLE IF NOT EXISTS tbl_purchaseorder_row (
    id                   INTEGER IDENTITY,
    purchaseorder_id     INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    description          VARCHAR(500),
    supplier_article_nr  VARCHAR(50),
    unitprice            DECIMAL(18,4),
    quantity             INTEGER,
    unit                 VARCHAR(100),
    account_nr           INTEGER,
    CONSTRAINT pk_po_row PRIMARY KEY (id),
    CONSTRAINT fk_po_row_po FOREIGN KEY (purchaseorder_id) REFERENCES tbl_purchaseorder(id)
);

CREATE TABLE IF NOT EXISTS tbl_purchaseorder_account (
    purchaseorder_id   INTEGER       NOT NULL,
    account_type       VARCHAR(50)   NOT NULL,
    account_nr         INTEGER       NOT NULL,
    CONSTRAINT pk_po_account PRIMARY KEY (purchaseorder_id, account_type),
    CONSTRAINT fk_poa_po FOREIGN KEY (purchaseorder_id) REFERENCES tbl_purchaseorder(id)
);

-- =============================================================================
-- CATEGORY F : Payments
--              Java classes: SSInpayment (+SSInpaymentRow),
--                            SSOutpayment (+SSOutpaymentRow)
-- =============================================================================

-- Java class: SSInpayment
CREATE TABLE IF NOT EXISTS tbl_inpayment (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    itext                VARCHAR(500),
    entered              BOOLEAN       DEFAULT FALSE,
    voucher_id           INTEGER,
    difference_voucher_id INTEGER,
    CONSTRAINT pk_inpayment     PRIMARY KEY (id),
    CONSTRAINT fk_inp_company   FOREIGN KEY (companyid)             REFERENCES tbl_company(id),
    CONSTRAINT fk_inp_voucher   FOREIGN KEY (voucher_id)            REFERENCES tbl_voucher(id),
    CONSTRAINT fk_inp_diff_vou  FOREIGN KEY (difference_voucher_id) REFERENCES tbl_voucher(id)
);

-- Java class: SSInpaymentRow  (child of tbl_inpayment)
CREATE TABLE IF NOT EXISTS tbl_inpayment_row (
    id                   INTEGER IDENTITY,
    inpayment_id         INTEGER       NOT NULL,
    invoice_nr           INTEGER,
    invoice_currency_code VARCHAR(10),
    invoice_currency_rate DECIMAL(10,6),
    value                DECIMAL(18,2),
    currency_rate        DECIMAL(10,6),
    CONSTRAINT pk_inpayment_row PRIMARY KEY (id),
    CONSTRAINT fk_inpr_payment  FOREIGN KEY (inpayment_id) REFERENCES tbl_inpayment(id)
);

-- Java class: SSOutpayment  (mirrors SSInpayment but for supplier invoices)
CREATE TABLE IF NOT EXISTS tbl_outpayment (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    itext                VARCHAR(500),
    entered              BOOLEAN       DEFAULT FALSE,
    voucher_id           INTEGER,
    difference_voucher_id INTEGER,
    CONSTRAINT pk_outpayment    PRIMARY KEY (id),
    CONSTRAINT fk_outp_company  FOREIGN KEY (companyid)             REFERENCES tbl_company(id),
    CONSTRAINT fk_outp_voucher  FOREIGN KEY (voucher_id)            REFERENCES tbl_voucher(id),
    CONSTRAINT fk_outp_diff_vou FOREIGN KEY (difference_voucher_id) REFERENCES tbl_voucher(id)
);

-- Java class: SSOutpaymentRow  (child of tbl_outpayment)
CREATE TABLE IF NOT EXISTS tbl_outpayment_row (
    id                   INTEGER IDENTITY,
    outpayment_id        INTEGER       NOT NULL,
    invoice_nr           INTEGER,
    invoice_currency_code VARCHAR(10),
    invoice_currency_rate DECIMAL(10,6),
    value                DECIMAL(18,2),
    currency_rate        DECIMAL(10,6),
    CONSTRAINT pk_outpayment_row PRIMARY KEY (id),
    CONSTRAINT fk_outpr_payment  FOREIGN KEY (outpayment_id) REFERENCES tbl_outpayment(id)
);

-- =============================================================================
-- CATEGORY G : Inventory / stock movements
--              Java classes: SSIndelivery (+SSIndeliveryRow),
--                            SSOutdelivery (+SSOutdeliveryRow),
--                            SSInventory (+SSInventoryRow)
-- =============================================================================

-- Java class: SSIndelivery  (goods received)
CREATE TABLE IF NOT EXISTS tbl_indelivery (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    itext                VARCHAR(500),
    CONSTRAINT pk_indelivery   PRIMARY KEY (id),
    CONSTRAINT fk_indel_company FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

CREATE TABLE IF NOT EXISTS tbl_indelivery_row (
    id                   INTEGER IDENTITY,
    indelivery_id        INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    change_qty           INTEGER,
    CONSTRAINT pk_indelivery_row PRIMARY KEY (id),
    CONSTRAINT fk_indr_del FOREIGN KEY (indelivery_id) REFERENCES tbl_indelivery(id)
);

-- Java class: SSOutdelivery  (goods dispatched)
CREATE TABLE IF NOT EXISTS tbl_outdelivery (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    itext                VARCHAR(500),
    CONSTRAINT pk_outdelivery    PRIMARY KEY (id),
    CONSTRAINT fk_outdel_company FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

CREATE TABLE IF NOT EXISTS tbl_outdelivery_row (
    id                   INTEGER IDENTITY,
    outdelivery_id       INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    change_qty           INTEGER,
    CONSTRAINT pk_outdelivery_row PRIMARY KEY (id),
    CONSTRAINT fk_outdr_del FOREIGN KEY (outdelivery_id) REFERENCES tbl_outdelivery(id)
);

-- Java class: SSInventory  (stock-take / count)
CREATE TABLE IF NOT EXISTS tbl_inventory (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    vdate                DATE,
    itext                VARCHAR(500),
    CONSTRAINT pk_inventory        PRIMARY KEY (id),
    CONSTRAINT fk_invent_company   FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

CREATE TABLE IF NOT EXISTS tbl_inventory_row (
    id                   INTEGER IDENTITY,
    inventory_id         INTEGER       NOT NULL,
    product_nr           VARCHAR(50),
    quantity             INTEGER,
    change_qty           INTEGER,
    CONSTRAINT pk_inventory_row PRIMARY KEY (id),
    CONSTRAINT fk_invr_inv FOREIGN KEY (inventory_id) REFERENCES tbl_inventory(id)
);

-- =============================================================================
-- CATEGORY H : Miscellaneous
--              Java classes: SSAutoDist (+SSAutoDistRow),
--                            SSVoucherTemplate (+SSVoucherTemplateRow),
--                            SSOwnReport (+SSOwnReportRow +SSOwnReportAccountRow)
-- =============================================================================

-- Java class: SSAutoDist  (automatic cost distribution / split postings)
CREATE TABLE IF NOT EXISTS tbl_autodist (
    id                   INTEGER IDENTITY,
    number               INTEGER       NOT NULL,
    companyid            INTEGER       NOT NULL,
    account_nr           INTEGER,
    description          VARCHAR(500),
    amount               DECIMAL(18,2),
    CONSTRAINT pk_autodist    PRIMARY KEY (id),
    CONSTRAINT fk_ad_company  FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

-- Java class: SSAutoDistRow  (child of tbl_autodist)
CREATE TABLE IF NOT EXISTS tbl_autodist_row (
    id                   INTEGER IDENTITY,
    autodist_id          INTEGER       NOT NULL,
    account_nr           INTEGER,
    description          VARCHAR(255),
    percentage           DECIMAL(7,4),
    debet                DECIMAL(18,2),
    credit               DECIMAL(18,2),
    project_nr           VARCHAR(50),
    result_unit_nr       VARCHAR(50),
    CONSTRAINT pk_autodist_row PRIMARY KEY (id),
    CONSTRAINT fk_adr_autodist FOREIGN KEY (autodist_id) REFERENCES tbl_autodist(id)
);

-- Java class: SSVoucherTemplate
CREATE TABLE IF NOT EXISTS tbl_vouchertemplate (
    id                   INTEGER IDENTITY,
    name                 VARCHAR(255)  NOT NULL,
    companyid            INTEGER       NOT NULL,
    description          VARCHAR(500),
    template_date        TIMESTAMP,
    CONSTRAINT pk_vouchertemplate    PRIMARY KEY (id),
    CONSTRAINT uq_vt_name_company    UNIQUE (name, companyid),
    CONSTRAINT fk_vt_company         FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

-- Java class: SSVoucherTemplateRow  (child of tbl_vouchertemplate)
CREATE TABLE IF NOT EXISTS tbl_vouchertemplate_row (
    id                   INTEGER IDENTITY,
    vouchertemplate_id   INTEGER       NOT NULL,
    account_nr           INTEGER,
    is_debet             BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_vt_row PRIMARY KEY (id),
    CONSTRAINT fk_vtr_template FOREIGN KEY (vouchertemplate_id) REFERENCES tbl_vouchertemplate(id)
);

-- Java class: SSOwnReport  (custom user-defined reports)
CREATE TABLE IF NOT EXISTS tbl_ownreport (
    id                   INTEGER IDENTITY,
    companyid            INTEGER       NOT NULL,
    name                 VARCHAR(255),
    project_nr           VARCHAR(50),
    result_unit_nr       VARCHAR(50),
    CONSTRAINT pk_ownreport   PRIMARY KEY (id),
    CONSTRAINT fk_or_company  FOREIGN KEY (companyid) REFERENCES tbl_company(id)
);

-- Java class: SSOwnReportRow  (heading row; child of tbl_ownreport)
CREATE TABLE IF NOT EXISTS tbl_ownreport_row (
    id                   INTEGER IDENTITY,
    ownreport_id         INTEGER       NOT NULL,
    row_order            INTEGER       DEFAULT 0 NOT NULL,
    heading_type         VARCHAR(50),
    heading              VARCHAR(255),
    CONSTRAINT pk_ownreport_row PRIMARY KEY (id),
    CONSTRAINT fk_orr_report FOREIGN KEY (ownreport_id) REFERENCES tbl_ownreport(id)
);

-- SSOwnReportAccountRow  (account entries under each heading row)
CREATE TABLE IF NOT EXISTS tbl_ownreport_account_row (
    id                   INTEGER IDENTITY,
    ownreport_row_id     INTEGER       NOT NULL,
    account_from         INTEGER,
    account_to           INTEGER,
    negate               BOOLEAN       DEFAULT FALSE,
    CONSTRAINT pk_ownreport_acc_row PRIMARY KEY (id),
    CONSTRAINT fk_orar_row FOREIGN KEY (ownreport_row_id) REFERENCES tbl_ownreport_row(id)
);

-- End of schema V2







