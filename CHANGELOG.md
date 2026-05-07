# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This project is a fork of
[JFS Accounting](https://sourceforge.net/projects/jfsaccounting/),
diverging from upstream version 2.2-SNAPSHOT.

## [Unreleased]

### Added
- Slice P (steg 7) credit-invoice cutover:
  - `CreditInvoice` har nu cutover-mönster i linje med tidigare domäner:
    `Repositories.init(SSDB)` wire:ar alltid `V2CreditInvoiceRepository`,
    aktiva V1 `OBJECT`-paths för credit invoices är avaktiverade i `SSDB`, och
    den döda legacy-adaptern `SSDBCreditInvoiceRepository` har tagits bort.
  - Nytt test `RepositoriesCreditInvoiceCutoverTest` verifierar att
    credit-invoice-domänen går via V2 i både V1- och V2-läge, medan omigrerade
    domäner fortsatt kan använda legacy-wiring.
  - `V2CreditInvoiceRepository` kan nu konstrueras oberoende av schemaflagga
    för att stödja Slice P-cutovern.
  - Verifierat med fokuserad testsvit:
    `RepositoriesCreditInvoiceCutoverTest`,
    `SSCreditInvoiceV2IntegrationTest`, `SSCreditInvoiceV2RepositoryTest`,
    `RepositoriesHDomainCutoverTest`, `RepositoriesSupplierInvoiceCutoverTest`,
    `RepositoriesTenderCutoverTest`, `RepositoriesPeriodicInvoiceCutoverTest`,
    `SSDBCustomerRepositoryTest`.
- Slice P (steg 6) periodic-invoice cutover:
  - `PeriodicInvoice` har nu cutover-mönster i linje med tidigare domäner:
    `Repositories.init(SSDB)` wire:ar alltid `V2PeriodicInvoiceRepository`,
    aktiva V1 `OBJECT`-paths för periodic invoices är avaktiverade i `SSDB`,
    och den döda legacy-adaptern `SSDBPeriodicInvoiceRepository` har tagits
    bort.
  - Nytt test `RepositoriesPeriodicInvoiceCutoverTest` verifierar att
    periodic-invoice-domänen går via V2 i både V1- och V2-läge, medan
    omigrerade domäner fortsatt kan använda legacy-wiring.
  - `V2PeriodicInvoiceRepository` kan nu konstrueras oberoende av
    schemaflagga för att stödja Slice P-cutovern.
  - Verifierat med fokuserad testsvit:
    `RepositoriesPeriodicInvoiceCutoverTest`,
    `SSPeriodicInvoiceV2IntegrationTest`, `SSPeriodicInvoiceV2RepositoryTest`,
    `RepositoriesHDomainCutoverTest`, `RepositoriesSupplierInvoiceCutoverTest`,
    `RepositoriesTenderCutoverTest`, `SSDBCustomerRepositoryTest`.
- Slice P (steg 5) tender cutover:
  - `Tender` har nu cutover-mönster i linje med tidigare domäner:
    `Repositories.init(SSDB)` wire:ar alltid `V2TenderRepository`, aktiva V1
    `OBJECT`-paths för tender är avaktiverade i `SSDB`, och den döda
    legacy-adaptern `SSDBTenderRepository` har tagits bort.
  - Nytt test `RepositoriesTenderCutoverTest` verifierar att tender-domänen
    går via V2 i både V1- och V2-läge, medan omigrerade domäner fortsatt kan
    använda legacy-wiring.
  - `V2TenderRepository` kan nu konstrueras oberoende av schemaflagga för att
    stödja Slice P-cutovern.
  - Verifierat med fokuserad testsvit:
    `RepositoriesTenderCutoverTest`, `SSTenderV2IntegrationTest`,
    `SSTenderV2RepositoryTest`, `RepositoriesHDomainCutoverTest`,
    `RepositoriesSupplierInvoiceCutoverTest`, `SSDBCustomerRepositoryTest`.
- Slice P (steg 4) supplier-invoice cutover:
  - `SupplierInvoice` har nu samma cutover-mönster som H-domänerna:
    `Repositories.init(SSDB)` wire:ar alltid `V2SupplierInvoiceRepository`,
    `SSDB` blockerar aktiva V1 `OBJECT`-paths för supplier invoices, och den
    döda legacy-adaptern `SSDBSupplierInvoiceRepository` har tagits bort.
  - Nytt test `RepositoriesSupplierInvoiceCutoverTest` verifierar att
    supplier-invoice-domänen går via V2 även i V1-läge, medan omigrerade
    domäner fortsatt kan använda legacy-wiring.
  - `V2SupplierInvoiceRepository` kan nu konstrueras oberoende av schemaflagga
    för att stödja Slice P-cutovern.
  - Bred regression verifierad för migrerade V2-slicar och cutover-smokes
    (`SSMasterdataV2RepositoryTest`, `SSAccountingCoreV2RepositoryTest`,
    `SSInvoiceV2RepositoryTest`, `SSOrderV2RepositoryTest`,
    `SSTenderV2RepositoryTest`, `SSCreditInvoiceV2RepositoryTest`,
    `SSPeriodicInvoiceV2RepositoryTest`, `SSSupplierInvoiceV2RepositoryTest`,
    `SSAutoDistV2RepositoryTest`, `SSVoucherTemplateV2IntegrationTest`,
    `SSVoucherTemplateV2RepositoryTest`, `SSOwnReportV2IntegrationTest`,
    `SSOwnReportV2RepositoryTest`, `RepositoriesHDomainCutoverTest`,
    `RepositoriesSupplierInvoiceCutoverTest`, `SSDBCustomerRepositoryTest`).
- Slice P (steg 3) finaliserad H-domain cutover:
  - Oanvanda legacy-adapters for `AutoDist`, `VoucherTemplate` och
    `OwnReport` har tagits bort
    (`SSDBAutoDistRepository`, `SSDBVoucherTemplateRepository`,
    `SSDBOwnReportRepository`).
  - Nytt test `RepositoriesHDomainCutoverTest` verifierar att
    `Repositories.init(SSDB)` fortsatt wire:ar H-domainerna till V2 medan en
    omigrerad doman (`Customer`) fortfarande far legacy-repository i V1-lage.
  - H-domainernas V2-repositories kan nu konstrueras oberoende av schemaflagga,
    sa att Slice P-cutovern fungerar utan latent init-fel i V1-lage.
- Slice P (steg 2) repository wiring cleanup for migrerade H-domainer:
  - `Repositories.init(SSDB)` wire:ar nu alltid V2-adapters for `AutoDist`,
    `VoucherTemplate` och `OwnReport` (ingen aktiv legacy-adapter-wire for
    dessa domainer).
  - V2-repositorytester hardenades med konkreta type-checks for
    `V2AutoDistRepository` och `V2VoucherTemplateRepository`.
  - Verifierat med fokuserad V2-regression samt V1-smoke
    (`SSDBCustomerRepositoryTest`).
- Slice P (steg 1) cutover/stadning for migrerade H-domainer:
  - `SSDB` har nu schema-V2-guard for `AutoDist`, `VoucherTemplate` och
    `OwnReport` i read/write-metoder sa att inga aktiva V1 `OBJECT`-paths
    anvands for dessa domainer.
  - V1-forsok for dessa domainer avbryts tidigt med varningslogg i stallet
    for att ga via serialiserade `OBJECT`-kolumner.
  - Fokusregressioner i V2-lage verifierade: `SSAutoDistV2RepositoryTest`,
    `SSVoucherTemplateV2IntegrationTest`, `SSVoucherTemplateV2RepositoryTest`,
    `SSOwnReportV2IntegrationTest`, `SSOwnReportV2RepositoryTest`.
- Slice O (H5) own-report repository slice:
  - New repository interface `OwnReportRepository` with legacy and V2
    adapters (`SSDBOwnReportRepository`, `V2OwnReportRepository`).
  - `Repositories.init(SSDB)` now wires own-report repositories and exposes
    `Repositories.ownReports()`.
  - New integration test `SSOwnReportV2RepositoryTest` validating
    repository-level V2 add/find/subset/update/delete flow.
- Slice O (H4) own-report V2 migration:
  - `SSDB` now supports V2 loading and persistence for own reports
    (`getOwnReports`, `getOwnReport`, `getOwnReports(List)`, `addOwnReport`,
    `updateOwnReport`, `deleteOwnReport`) against `tbl_ownreport`,
    `tbl_ownreport_row`, and `tbl_ownreport_account_row` behind
    `fribok.schema.version=v2`.
  - New integration test `SSOwnReportV2IntegrationTest` validating own-report
    V2 add/fetch/update/delete flow including heading and account-row
    round-trip mapping.
- Slice O (H3) voucher-template repository slice:
  - New repository interface `VoucherTemplateRepository` with legacy and V2
    adapters (`SSDBVoucherTemplateRepository`, `V2VoucherTemplateRepository`).
  - `Repositories.init(SSDB)` now wires voucher-template repositories and
    exposes `Repositories.voucherTemplates()`.
  - New integration test `SSVoucherTemplateV2RepositoryTest` validating
    repository-level V2 add/find/find-subset/delete flow.
- Slice O (H2) voucher-template V2 migration:
  - `SSDB` now supports V2 loading and persistence for voucher templates
    (`getVoucherTemplates`, `getVoucherTemplates(List)`, `addVoucherTemplate`,
    `deleteVoucherTemplate`) against `tbl_vouchertemplate` and
    `tbl_vouchertemplate_row` behind `fribok.schema.version=v2`.
  - New integration test `SSVoucherTemplateV2IntegrationTest` validating
    voucher-template V2 add/fetch/subset/delete flow including row
    round-trip mapping.
- Slice O (H) AutoDist repository slice:
  - `SSDB` V2 support for `SSAutoDist` now includes row mapping (`mapAutoDistV2`) and
    V2 CRUD persistence for `tbl_autodist` + `tbl_autodist_row` behind
    `fribok.schema.version=v2`.
  - New repository interface `AutoDistRepository` with legacy and V2 adapters
    (`SSDBAutoDistRepository`, `V2AutoDistRepository`).
  - `Repositories.init(SSDB)` now wires autodist repositories and exposes
    `Repositories.autoDists()`.
  - Integration test: `SSAutoDistV2RepositoryTest` validating repository-level
    V2 CRUD flow including row round-trip mapping.
- Slice N (D/E) supplier invoice V2 migration:
  - `SSDB` now supports V2 CRUD for supplier invoices (`getSupplierInvoices`,
    `getSupplierInvoice`, `addSupplierInvoice`, `updateSupplierInvoice`,
    `deleteSupplierInvoice`) against `tbl_supplierinvoice` and
    `tbl_supplierinvoice_row` behind `fribok.schema.version=v2`.
  - New repository interface `SupplierInvoiceRepository` with legacy and V2
    adapters (`SSDBSupplierInvoiceRepository`, `V2SupplierInvoiceRepository`).
  - `Repositories.init(SSDB)` now wires supplier-invoice repositories and
    exposes `Repositories.supplierInvoices()`.
  - Integration tests: `SSSupplierInvoiceV2IntegrationTest` (SSDB layer) and
    `SSSupplierInvoiceV2RepositoryTest` (repository wiring).
- Accounting-core repository layer (Session L):
  - New repository interfaces: `AccountPlanRepository`, `VoucherRepository`,
    and `AccountingYearRepository`.
  - Legacy adapters: `SSDBAccountPlanRepository`, `SSDBVoucherRepository`,
    and `SSDBAccountingYearRepository`.
  - V2 adapters: `V2AccountPlanRepository`, `V2VoucherRepository`,
    and `V2AccountingYearRepository`.
  - `Repositories.init(SSDB)` now wires these repositories for both V1 and V2,
    and new getters are available via `Repositories.accountPlans()`,
    `Repositories.vouchers()`, and `Repositories.accountingYears()`.
  - Integration tests: `SSAccountPlanV2RepositoryTest`,
    `SSVoucherV2RepositoryTest`, `SSAccountingYearV2RepositoryTest`, and
    `SSAccountingCoreV2RepositoryTest`.
- Schema V2 invoice core slice (Session E): `SSDB` now supports minimal
  invoice CRUD (`getInvoices`, `getInvoice`, `addInvoice`, `updateInvoice`,
  `deleteInvoice`) against `tbl_invoice` and `tbl_invoice_row` behind
  `fribok.schema.version=v2`.
- Integration test `SSInvoiceV2IntegrationTest` validating V2 invoice
  add/fetch/update/delete flow including row round-trip mapping.
- Schema V2 order core slice (Session F): `SSDB` now supports minimal
  order CRUD (`getOrders`, `getOrder`, `addOrder`, `updateOrder`,
  `deleteOrder`) against `tbl_order` and `tbl_order_row` behind
  `fribok.schema.version=v2`.
- Integration test `SSOrderV2IntegrationTest` validating V2 order
  add/fetch/update/delete flow including row round-trip mapping.
- Schema V2 tender core slice (Session G): `SSDB` now supports minimal
  tender CRUD (`getTenders`, `getTender`, `addTender`, `updateTender`,
  `deleteTender`) against `tbl_tender` and `tbl_tender_row` behind
  `fribok.schema.version=v2`.
- Integration test `SSTenderV2IntegrationTest` validating V2 tender
  add/fetch/update/delete flow including row round-trip mapping.
- Schema V2 credit invoice core slice (Session H): `SSDB` now supports
  minimal credit invoice CRUD (`getCreditInvoices`, `getCreditInvoice`,
  `addCreditInvoice`, `updateCreditInvoice`, `deleteCreditInvoice`) against
  `tbl_creditinvoice` and `tbl_creditinvoice_row` behind
  `fribok.schema.version=v2`.
- Integration test `SSCreditInvoiceV2IntegrationTest` validating V2 credit
  invoice add/fetch/update/delete flow including row round-trip mapping.
- Schema V2 periodic invoice core slice (Session I): `SSDB` now supports
  minimal periodic invoice CRUD (`getPeriodicInvoices`, `getPeriodicInvoice`,
  `addPeriodicInvoice`, `updatePeriodicInvoice`, `deletePeriodicInvoice`)
  against `tbl_periodicinvoice` and `tbl_periodicinvoice_row` behind
  `fribok.schema.version=v2`.
- Integration test `SSPeriodicInvoiceV2IntegrationTest` validating V2
  periodic invoice add/fetch/update/delete flow including template row
  round-trip mapping.
- Integration tests now pass in headless (CI) environments:
  - `SSDB.init()` skips the `SSInitDialog` popup when
    `GraphicsEnvironment.isHeadless()` returns true.
  - `SSMainFrame.getInstance()` returns `null` in headless mode instead of
    throwing `HeadlessException`.
  - `SSErrorDialog.showDialog()` accepts a `null` frame and silently skips
    the dialog, so callers work correctly in headless contexts.
  - `SSDB.createNewTables()` falls back from `CREATE CACHED TABLE` (file-backed)
    to `CREATE TABLE IF NOT EXISTS` (in-memory) when the primary DDL statement
    fails; required for HSQLDB in-memory databases used in integration tests.
  - `create_tables.sql` now uses explicit `VARCHAR(255)` instead of bare
    `VARCHAR` throughout; HSQLDB 2.7 requires an explicit length in DDL.
  - `SSDB.java` collapses redundant `ClassCastException | RuntimeException`
    multi-catch arms (the former is a subclass of the latter; JDK 21 rejects
    the redundant union).
- Repository abstraction layer (`se.swedsoft.bookkeeping.persistence`) introducing
  `CustomerRepository`, `ProductRepository` and `SupplierRepository` interfaces
  for the masterdata domain.  Legacy implementations in the `persistence.legacy`
  subpackage delegate to the existing `SSDB` singleton.  A `Repositories` factory
  class provides the single wiring point.  No existing call sites were changed;
  this is the foundation for future incremental migration away from the SSDB
  God-object (Phase 1 of the persistence modernization plan).
- Session K repository V2 wiring: added `persistence.v2` implementations
  `V2CustomerRepository`, `V2ProductRepository`, and `V2SupplierRepository`.
  `Repositories.init(SSDB)` now selects V2 implementations when
  `fribok.schema.version=v2`, otherwise keeps legacy adapters.
- Integration test `SSMasterdataV2RepositoryTest` validating repository-level
  V2 CRUD flows (customer, product, supplier) plus subset lookups through
  `Repositories.customers()`, `Repositories.products()`, and
  `Repositories.suppliers()`.
- Unit test `SSDBCustomerRepositoryTest` covering constructor contract of the
  customer repository adapter.
- PR_DRAFT.md with detailed description of the repository-layer pull request.
- HSQLDB 2.7.2 upgrade (Phase 5 Step 41): migrated from ancient HSQLDB 1.8.0.10
  (from ~2005) to modern HSQLDB 2.7.2 (2023). Compilation succeeds; unit tests pass
  with HSQLDB 2.7.2. Fixed `SSDB.createNewTables()` to execute SQL statements
  separately (PreparedStatement limitation). Includes `HSQLDB_2X_UPGRADE_PLAN.md`
  integration tests have separate headless/GUI issue
  (not a database problem). (Headless/in-memory issue subsequently fixed.)
- Modernization plan (`MODERNIZATION.md`) documenting a phased approach to
  bring the codebase from Java 5/6-era style to modern Java.
- `AGENTS.md` with build, test, lint commands and code style guidelines for
  AI-assisted development.
- Checkstyle configuration (`checkstyle.xml`) enforcing project code style
  guidelines (Phase 7 Step 34).
- SpotBugs static analysis replacing abandoned FindBugs (Phase 7 Step 35).
- JaCoCo code coverage reporting with 5.3% baseline (Phase 7 Step 36).
- CI quality gates: Checkstyle and coverage report upload on PRs (Phase 7
  Step 37).
- GitHub Actions CI/CD workflow (`ci.yml`):
  - Pull request builds with `mvn clean install` on Ubuntu/JDK 21.
  - Release builds on push to master for Linux, Windows, and macOS.
  - Native installer creation via jpackage (AppImage, MSI, DMG).
  - Smoke tests for all three platform installers.
- AppImage build support for Linux distribution.
- JUnit 5 test foundation with Maven Surefire integration, test infrastructure
  utilities (`TestDBHelper`, `TestLauncher`), and initial core tests for
  `SSNewCompany`, `SSDB`, and `SSVoucher` (PR #3).
- Core business logic tests for `SSAccountPlan`, `SSNewAccountingYear`,
  `SSVoucherMath`, and `SSBudget` (PR #5).
- Database integration tests for SSDB CRUD operations covering invoices,
  suppliers, customers, products, and vouchers (PR #6, #7).
- `SSDateUtil` adapter class bridging `java.util.Date` and `java.time`
  (Phase 3 Step 15) (PR #9).

### Changed
- Session M step 3: accounting-year boundary updates now preserve existing
  monthly budget values by month number instead of flattening distribution
  during `SSBudget#setYear(...)` remapping.
- `SSDB` account-plan persistence now supports schema V2 mapping against
  `tbl_accountplan` and `tbl_account` (read/add/update/delete) while keeping
  legacy object-column behaviour for V1.
- `SSDB` now exposes voucher lookup by explicit year and number:
  `getVoucher(SSNewAccountingYear, int)`.
- `SSDB` accounting-year V2 mapping now hydrates `accountplan_id` into
  `SSNewAccountingYear#setAccountPlan(...)` when available.
- `SSVoucher` gained a mapping-safe constructor used by V2 hydration to avoid
  recursive voucher-list lookups during `SSDB.mapVoucherV2(...)`.
- Session L migration documentation is now locked for implementation start:
  - `doc/migration/SESSION_L_ACCOUNTING_REPOSITORY.md` defines binding scope for
    `AccountingYearRepository` in L (`findAll/findCurrent/add/update/delete` only),
    defers budget/year-balance mapping to Session M, and locks voucher-add behavior
    to require explicit accounting-year on the voucher object.
  - `doc/migration/SESSION_RESUME_CHECKLIST.md` now includes a verified baseline
    commit (`474a715`) and an explicit Definition of Done checklist for Session L.
- Persistence modernization policy: target database migration is now defined as
  a forward-only cutover. After cutover, restoring older legacy backup formats
  directly into the new database model is not supported.
- Modernized Java syntax (Phase 1): replaced anonymous inner classes with
  lambdas, added diamond operator, converted loops to streams, adopted
  try-with-resources for I/O (PR #4).
- Replaced `System.out`/`System.err`/`printStackTrace` calls with SLF4J
  logging backed by Logback (Phase 2) (PR #8).
- Updated `MODERNIZATION.md` to reflect current progress through Phase 3.5
  (PR #13).
- Migrated domain model date fields from `java.util.Date` to `LocalDate`
  (Phase 3 Step 16) (PR #14).
- Replaced `SimpleDateFormat` usage with `DateTimeFormatter` throughout the
  codebase (Phase 3 Step 17) (PR #15).
- Eliminated all `java.util.Calendar` usage from the codebase, migrating GUI
  date components, print reports, table renderers, calc utilities, and data
  classes to `java.time.LocalDate`/`ChronoUnit` (Phase 3 Step 18).
- Continued the date migration in GUI workflows by replacing more
  `new Date()` defaults with `SSDateUtil.today()` and `LocalDate` setters in
  invoice, order, purchase order, periodic invoice, tender, and credit invoice
  dialogs plus related invoice date chooser/table logic.
- Continued the date migration across calculations, import/export, backup,
  voucher editing, and report cache code so production `new Date()` runtime
  calls are eliminated in favor of `SSDateUtil` and `java.time` comparisons.
- Continued the date API cleanup by switching `SSDateMath`, `SSMonth`,
  `SSVoucher`, `SSInvoice`, and `SSSupplierInvoice` workflows and focused tests
  to prefer `LocalDate` accessors over deprecated `Date` bridges.
- Continued the date migration in payment, credit-note, and periodic-invoice
  calculations by replacing more legacy `Date` comparisons with `LocalDate`
  logic in in/out-payment math and period boundary handling.
- Continued the date migration in revenue, receivable/payable, budget, and
  value report flows by replacing more report-period comparisons and month
  splitting logic with `LocalDate`-based boundaries.
- Continued the date migration in stock-related math by replacing remaining
  purchase order, inventory, and in/out-delivery period checks with
  `LocalDate`-based comparisons.
- Continued the date migration in accounting-year and report setup flows by
  preferring `LocalDate` year boundaries and converting back to `Date` only at
  dialog and Jasper parameter boundaries.
- Continued the date migration in payment, inventory, and periodic-invoice UI
  panels by preferring `LocalDate` chooser accessors and only bridging back to
  `Date` for legacy stock-update and table-rendering APIs.
- Continued the date migration in company, customer, supplier, product,
  project, and result-unit monthly aggregates by switching more month-membership
  checks from deprecated `Date` accessors to `LocalDate` values.
- Continued the date migration in product pricing, inpayment lookup, and main
  book calculations by comparing `LocalDate` values directly and only bridging
  back to `Date` for legacy method contracts.
- Continued the date migration in periodic-invoice generation and pending
  invoice flows by keeping schedule calculations and next-invoice dates as
  `LocalDate` values internally.
- Continued the date migration in invoice due-date table and sales print flows
  by using `LocalDate` accessors directly and only converting to `Date` at
  report and table boundaries.
- Continued the date migration in list, journal, and debt printers by reading
  local date accessors directly and only bridging to `Date` for final display
  formatting.
- Continued the date migration in import flows by storing parsed BGMax,
  supplier-payment, voucher-import, and SIE voucher dates through `LocalDate`
  setters instead of deprecated `Date` setters.
- Continued the date migration in in- and out-delivery domain, table, panel,
  and list-printer flows by adding `LocalDate` accessors and removing immediate
  `Date` bridge round-trips.
- Continued the date migration in order, tender, purchase-order, and inventory
  report/import flows by using `LocalDate` accessors directly and limiting
  `Date` bridges to XML and Jasper boundaries.
- Continued the date migration in payment journal, reminder, main-book, and
  transaction-cleanup flows by reading local dates directly and only bridging
  to `Date` where report rendering still requires it.
- Continued the date migration in supplier-payment export flows by reading
  `LocalDate` values directly from payment models and only bridging back to
  `Date` for persisted config values.
- Continued the date migration in supplier-payment LB export posts by taking
  `LocalDate` values from payment models and only bridging to `Date` at the
  file-format boundary.
- Continued the date migration in Excel voucher export by letting writable row
  helpers accept `LocalDate` values directly instead of formatting through
  deprecated voucher `Date` accessors.
- Continued the date migration in app dialogs by exposing `LocalDate` values
  directly where menu flows immediately convert legacy `Date` selections back
  into local dates for processing.
- Continued the date migration in report dialogs by exposing `LocalDate`
  values directly for single-date reports and reading local date ranges
  directly from chooser widgets in list dialogs.
- Dropped the legacy pre-HSQL `bookkeeper.db` import path and its archived
  `db/databas_v1.zip` handoff, requiring very old installations to migrate via
  historical Fribok releases before using this fork.
- Encapsulated 53 public mutable fields across 7 classes with proper
  getters/setters (Phase 4 Step 19).
- Introduced `Optional<T>` for ~100 public API methods across SSDB lookups,
  calc/math search methods, data model getters, and parser/decoder methods;
  reduced `return null` sites from ~419 to ~212 (Phase 4 Step 20).

### Fixed
- CI: use `target/dist` for AppImage build output.
- CI: use bash shell for Maven build and fix installer test paths.
- CI: install jpackage dependencies on Linux runner.
- CI: upgrade deprecated GitHub Actions from v3 to v4 (PR #2).
- Excluded build artifacts from git tracking.
- CI: fail `build_and_publish` job when tests fail (PR #10).
- Resolved database path issue: use per-user directories on Windows and
  macOS instead of hard-coded paths (PR #11).
- Caught `NullPointerException` in voucher comparator, fixed PR CI coverage
  reporting, and fixed Linux resource loading paths (PR #12).
- Added null guards in `SSTriggerHandler.triggerAction` and improved
  background-thread error detection in tests (PR #16).
- Fixed buggy delayed-days calculation in `SSReminderPrinter.getNumDelayedDays()`
  and `SSInvoiceMath.getNumDelayedDays()`: replaced epoch-based Calendar
  arithmetic with `ChronoUnit.DAYS.between()`.
- Fixed thread-safety issues: removed shared mutable `static Calendar` fields
  in `SSVoucherMath` and `SSBudget`.

### Removed
- Dead multi-user/server mode code (Phase 3.5): removed `SSPostLock`,
  `SSCompanyLock`, `SSYearLock`, and all lock acquisition/release calls
  across 54+ GUI files. Simplified `SSTriggerHandler` to a direct
  `Trigger.fire()` call. Reduced `SSDB` by ~1,300 lines (PR #17).
- Duplicate legacy entry point `SSBookkeeping.java` and 5 orphaned test data
  files (Phase 4 Step 21). Resolved all TODOs and converted remaining
  `System.out.printf` calls to SLF4J logging.
