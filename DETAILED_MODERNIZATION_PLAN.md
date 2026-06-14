# Detailed modernization plan (post-hypercare)

Date: 2026-05-18
Status: Draft plan for execution in agreed order
Owner: Team

## Goal

Reduce deprecated API usage and technical debt in controlled batches, while keeping the application stable in production.

Agreed execution order:
1. P1 BigDecimal
2. P1 Date
3. P2 XML
4. P4 all
5. P5 dependency cleanup
6. P3 Jasper report template cleanup

## Working model

- Small, testable batches (one bounded area per PR).
- No cross-cutting mega-refactor.
- Build + tests after each batch.
- Manual smoke test after high-risk batches.
- Keep compatibility wrappers only when needed; remove later in cleanup PR.

## Global gate after each batch

1) Compile + tests
```powershell
mvn -q clean compile
mvn -q test
```

2) Optional full gate before merge to main
```powershell
mvn -q clean install
```

3) Manual smoke test (minimum)
- Open company, open accounting year.
- Create/edit/delete one record in core CRUD flows.
- Print one invoice/reminder report.
- Verify no new ERROR in runtime log.

## Phase 1 - P1 BigDecimal (lowest risk, start here)

### Scope
- Replace deprecated BigDecimal constants/method overloads in:
  - `src/main/java/se/swedsoft/bookkeeping/data/SSBudget.java`
  - `src/main/java/se/swedsoft/bookkeeping/data/SSProduct.java`

### Planned changes
- `BigDecimal.ROUND_*` -> `RoundingMode.*`
- `setScale(int, int)` -> `setScale(int, RoundingMode)`
- `divide(BigDecimal, int)` -> `divide(BigDecimal, RoundingMode)`

### Risks
- Changed rounding behavior if wrong enum selected.

### Validation
- Unit tests for budget distribution and product amount/tax calculations.
- Manual compare: old vs new sample values for edge inputs (negative values, zero, repeating decimals).

### Exit criteria
- No deprecation warnings for BigDecimal usages in these files.
- Numeric outputs unchanged for baseline scenarios.

## Phase 2 - P1 Date (core business logic)

### Scope
- Remove deprecated Date-based call paths in prioritized order:
  1. `src/main/java/se/swedsoft/bookkeeping/data/SSNewAccountingYear.java`
  2. `src/main/java/se/swedsoft/bookkeeping/data/SSInvoice.java`
  3. `src/main/java/se/swedsoft/bookkeeping/calc/SSResultCalculator.java`
- Then continue migration of remaining Date-based internal APIs where safe.

### Planned changes
- Prefer `LocalDate`/`LocalDateTime` APIs.
- Replace calls to deprecated methods:
  - `getFrom()/getTo()` -> local-date equivalents
  - `addDaysToDate(Date)` -> local-date variant
  - Date-based period functions -> LocalDate-based equivalents

### Risks
- Subtle date boundary drift (timezone/start-of-day conversions).
- Behavioral changes in due date and period filters.

### Validation
- Focused tests for:
  - due date calculation
  - accounting year boundaries
  - period inclusion/exclusion in reports/calculations
- Manual verification with real-like dates around month/year boundaries.

### Exit criteria
- Target P1 Date call sites no longer use deprecated Date APIs.
- No regressions in due-date and period behavior.

## Phase 3 - P2 XML (legacy parser/serializer APIs)

### Scope
- Replace deprecated XML APIs in:
  - `src/main/java/se/swedsoft/bookkeeping/data/system/SSDBConfig.java`
  - `src/main/java/se/swedsoft/bookkeeping/calc/data/SSAccountSchema.java`
  - `src/main/java/se/swedsoft/bookkeeping/gui/util/menu/SSMenuLoader.java`
  - XML writing blocks in:
    - `src/main/java/se/swedsoft/bookkeeping/importexport/excel/SSCustomerExporter.java`
    - `src/main/java/se/swedsoft/bookkeeping/importexport/excel/SSProductExporter.java`

### Planned changes
- Replace `org.apache.xml.serialize.*` usage with JAXP `Transformer`-based writing.
- Replace deprecated SAX helpers (`XMLReaderFactory`, `AttributeListImpl`) with modern SAX/JAXP setup.

### Risks
- XML formatting/output differences (indentation, declaration, encoding handling).

### Validation
- Golden-file compare on generated XML.
- Parse/read-back tests for generated files.

### Exit criteria
- Deprecated XML serializer/parser APIs removed from target files.
- Generated XML remains accepted by existing import paths.

## Phase 4 - P4 all (low-risk cleanup)

### Scope
- Locale constructor cleanup and other low-risk deprecations:
  - Excel import/export classes under `src/main/java/se/swedsoft/bookkeeping/importexport/excel/`
  - QR helper update where needed (`CreateQRCode`)

### Planned changes
- `new Locale("sv")` / `new Locale("sv", "SE")` -> `Locale.forLanguageTag("sv")` or equivalent explicit constants.
- Update deprecated library calls where direct replacements are available.

### Risks
- Locale formatting differences if replacement is wrong.

### Validation
- Regression checks for import/export formatting and decimal/date localization.

### Exit criteria
- No remaining P4 deprecated warnings in scoped files.

## Phase 5 - P5 dependency cleanup

### Scope
- Dependency-level modernization (without functional behavior changes where possible).
- Start with relocation/legacy warnings from Maven.

### Planned changes
- Evaluate `com.lowagie:itext` relocation warning and decide migration path.
- Reassess `xml-apis` relocation usage and remove/reduce unnecessary explicit deps.
- Confirm compatibility and licensing implications before final switch.

### Risks
- Transitive dependency conflicts.
- License/policy constraints for PDF stack changes.

### Validation
- `mvn dependency:tree` before/after.
- Full compile/test/install.
- Print/export smoke test.

### Exit criteria
- Dependency relocation warnings reduced or documented with approved rationale.

## Phase 6 - P3 Jasper report template cleanup (last)

### Scope
- `data/report/**/*.jrxml`
- Replace deprecated Jasper attributes:
  - `isSplitAllowed` -> `splitType`
  - `pen`, `borderColor`, `border*` legacy box style attributes -> `<pen>` model

### Planned changes
- Batch by report family (sales, VAT, accounting summaries, etc.).
- Keep visual output stable while modernizing schema usage.

### Risks
- Layout drift in printed/exported reports.

### Validation
- Visual compare (before/after PDF snapshots) for representative reports.
- Run critical report generation set (invoice, reminder, VAT, ledger-like reports).

### Exit criteria
- Deprecated Jasper template warnings significantly reduced.
- No major visual or pagination regressions in key reports.

## Suggested PR slicing

PR-1: P1 BigDecimal (`SSBudget`, `SSProduct`)
PR-2: P1 Date part A (`SSNewAccountingYear`)
PR-3: P1 Date part B (`SSInvoice`, `SSResultCalculator`)
PR-4: P2 XML config/menu/schema loaders
PR-5: P2 XML export writers
PR-6: P4 cleanup
PR-7: P5 dependencies
PR-8..N: P3 report template families

## Rollback strategy

- Keep each phase in separate PRs and tags.
- If regression detected, revert only the affected PR.
- Do not mix data model refactors with dependency upgrades in the same PR.

## Done definition for the full plan

- Ordered phases completed with passing build/test gates.
- No critical production regressions introduced.
- Deprecated warning count reduced in targeted areas.
- Final summary documented in `CHANGELOG.md` and release notes.

