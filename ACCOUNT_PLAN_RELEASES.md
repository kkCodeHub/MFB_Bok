# Account Plan Management - Three-Release Roadmap

**Status:** Release 1 & 2 completed; Release 3 in planning
**Last Updated:** 2026-05-11
**Documentation Date:** From sessions starting 2026-05-09

## Overview

This document describes a three-release implementation strategy for modernizing account plan management in Fribok. The goal is to separate template data (reusable account plans) from year-active data (year-specific account sets), enabling:

1. Independent year-to-year account management
2. Historical snapshot preservation
3. Clean deletion of obsolete templates without breaking active years
4. Write-back capability from active year changes to snapshots

---

## Release 1: Snapshot Infrastructure ✅ COMPLETED

### Objective
Add persistence layer for storing snapshots of account plans in each accounting year, enabling years to become independent of template definitions.

### What Was Changed

#### Schema Changes
- Added `TBL_AccountingYear.accountplan` column (CLOB type)
- Added `TBL_AccountingYear.accountplan_snapshot_timestamp` (metadata)
- These columns store a serialized copy of the account plan valid for that year

#### Code Changes
- **`SSDB.loadAccountPlanSnapshotV2()`**: Loads snapshot from CLOB string
- **`SSDB.mapAccountingYearV2()`**: Now attempts snapshot-first reading before fallback to `accountplan_id` FK
- **`SSDB.updateAccountingYear()`**: Persists snapshot CLOB when year is updated
- Resource bundle entries for error messages

#### Behavior
- When a user edits the account plan for an active year, a snapshot is written to the year's CLOB field
- The template `TBL_AccountPlan` table still exists and may be referenced; snapshot provides backup
- Years with snapshots can be restored even if template is deleted
- No FK constraint was removed at this stage; this is still hybrid model (snapshot + template reference coexist)

#### Test Results
- ✅ `mvn clean install` passes
- ✅ All 153 unit tests pass
- ✅ Integration tests pass in headless mode

---

## Release 2: Year-Based Active Account Set + Write-Back ✅ COMPLETED

### Objective
Fully decouple active year account data from template library. Each year owns its account roster independent of templates.

### Key Architectural Changes

#### 1. Schema Restructuring
**Old Model:**
```
TBL_AccountPlan (template library)
  ├─ accountplan_id → TBL_Account (accountplan_id FK)
  │
TBL_AccountingYear
  ├─ accountplan_id FK (referenced template)
  └─ accountplan CLOB (snapshot, optional)
```

**New Model:**
```
TBL_AccountPlan (template library only)
  │
  └─ (no FK to years)

TBL_AccountPlanAccount (NEW - template account library)
  ├─ accountplan_id FK → TBL_AccountPlan
  └─ stores: account_number, account_name, account_type, etc.

TBL_Account (year-active working set)
  ├─ accountingyear_id FK → TBL_AccountingYear (CHANGED from accountplan_id)
  └─ stores: account_number, account_name, account_type, etc.

TBL_AccountingYear
  ├─ accountplan CLOB (snapshot/archive)
  └─ (FK to template removed)
```

#### 2. Table Changes in `create_tables_v2.sql`
- **`tbl_account`**: Removed `accountplan_id` column; added `accountingyear_id` FK
- **`tbl_accountplan_account`**: NEW table to hold template accounts (replaces accounts being directly in `tbl_account`)
- **`tbl_accountingyear`**: Removed `fk_year_plan` FK constraint

#### 3. SSDB Method Updates
- **Account retrieval**: Changed from `SELECT * FROM tbl_account WHERE accountplan_id = ?` to `SELECT * FROM tbl_account WHERE accountingyear_id = ?`
- **Template accounts**: New methods accessing `tbl_accountplan_account` for template CRUD
- **Year-active accounts**: Updated to read/write via `accountingyear_id` relationship
- **`canOpenAccountingYear(...)`**: NEW guard method; validates snapshot exists before year can be opened
- **`ensureDropYearPlanFkV2()`**: Idempotent migration; removes old FK from existing databases

#### 4. GUI Dialog Changes
- **`SSAccountPlanDialog.editCurrentDialog()`**: Changed to update active year snapshot (via `updateAccountingYear()`) instead of creating new template post
- **`SSNewAccountingYearDialog`**: When year created with template selection, template accounts are COPIED to `tbl_account` with new year's `accountingyear_id`
- **`SSAccountingYearFrame`** / **`SSCompanyFrame`**: Added snapshot validation before opening year; blocks opening if snapshot missing
- Error dialogs inform user that missing snapshot years must be deleted and recreated

#### 5. Copy-on-Create Pattern
When a new accounting year is created:
1. User selects a template (from `TBL_AccountPlan`)
2. All template accounts from `tbl_accountplan_account` are copied into `tbl_account`
3. Each copied account gets the new year's `accountingyear_id`
4. Snapshot is generated and saved to `TBL_AccountingYear.accountplan` CLOB
5. Year is now **independent** of template; template can be deleted without affecting this year

#### 6. Schema Migration
- Runtime detection via `INFORMATION_SCHEMA.COLUMNS` checks if database is V1 or V2
- Existing V1 databases get `fk_year_plan` FK dropped via `ensureDropYearPlanFkV2()`
- Idempotent migrations prevent errors on repeated execution

### Key Behaviors

| Action | Before Release 2 | After Release 2 |
|--------|------------------|-----------------|
| Edit active year's plan | Creates new post in `TBL_AccountPlan` (template) | Updates snapshot in `TBL_AccountingYear` |
| Create new year | Links to template via FK | Copies template accounts to year; owns independent set |
| Delete template | Blocked by FK (years still reference it) | Allowed; years have independent snapshot copies |
| Open existing year | Reads from template via FK | Reads snapshot; template is fallback only |
| Modify template accounts | Affects all years using that template | Does NOT affect years (they have copies) |

### Test Results
- ✅ `mvn clean compile` passes
- ✅ `mvn clean install`: 153 tests pass
- ✅ No schema validation errors
- ✅ GUI account plan edits correctly update snapshots, not templates

### Files Modified in Release 2
- `src/main/resources/sql/create_tables_v2.sql` — schema changes
- `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java` — method routing & snapshot handling
- `src/main/java/se/swedsoft/bookkeeping/gui/accountplans/SSAccountPlanDialog.java` — GUI write-back
- `src/main/java/se/swedsoft/bookkeeping/gui/accountingyear/dialog/SSNewAccountingYearDialog.java` — copy-on-create
- `src/main/java/se/swedsoft/bookkeeping/gui/accountingyear/SSAccountingYearFrame.java` — snapshot validation
- `src/main/java/se/swedsoft/bookkeeping/gui/company/SSCompanyFrame.java` — snapshot check on year switch
- `src/main/resources/book.properties` — error messages

---

## Release 3: (Planning) ❓ IN PLANNING

### Scope (Not Yet Finalized)

Potential areas for Release 3, pending user decision:

#### Option A: Snapshot Format Standardization
- Convert CLOB serialized format to structured format (JSON or XML)
- Enable human-readable snapshot inspection
- Improve interoperability with external tools
- Version snapshot format to support schema evolution

#### Option B: Advanced Snapshot Management
- UI for viewing/comparing snapshots across years
- Snapshot rollback capability (restore year's plan to previous snapshot)
- Snapshot history log (who changed it, when, what changed)
- Diff viewer between template and year's snapshot

#### Option C: Template-to-Year Sync Controls
- Selective audit: which accounts differ between template and year's snapshot
- Semi-automatic template update propagation (notify when template changes relevant accounts)
- Batch account plan edits across multiple years (apply same changes to multiple years' snapshots)

#### Option D: Performance & Integrity
- Index optimization for year-sorted account queries
- Constraints to ensure account consistency (no orphaned accounts post-copy)
- Audit trail for template deletions and their impact
- Cleanup of obsolete templates and their snapshots

#### Option E: Reporting & Analytics
- Account plan change history (track when snapshots were created/modified)
- Template usage report (which years use which templates)
- Account consistency report (detect drift between years and templates)
- Migration statistics (how many accounts per year vs. template)

---

## Critical Design Decisions

### 1. Copy-on-Create vs. Reference Strategy
**Decision:** Copy template accounts when year is created (not permanent FK reference)

**Rationale:**
- Each year has complete independence
- No cascade-delete fragility
- Storage is explicit and transparent
- No surprise changes if template is edited

**Trade-off:** Uses more storage space; duplicates account definitions across years

### 2. Snapshot as Authoritative Source
**Decision:** CLOB snapshot in `TBL_AccountingYear` is source of truth for that year's plan

**Rationale:**
- Survives template deletion
- Prevents accidental cross-year contamination
- Clear boundaries between year data and template library
- Solves the "which version applies to this year?" ambiguity

**Trade-off:** Takes storage space; requires synchronization if template is updated post-year-creation

### 3. No FK from Years to Templates
**Decision:** Remove `fk_year_plan` FK constraint; years and templates are completely decoupled

**Rationale:**
- Template can be deleted without impacting years
- Years don't accidentally export changes upward to template
- Clear ownership: year owns its accounts, template owns its library

**Trade-off:** Templatization is explicit via copy, not implicit via FK

### 4. Validation on Year Open
**Decision:** Every year open checks for valid snapshot; blocks if missing

**Rationale:**
- Prevents silent fallback to wrong data
- Forces explicit remediation (delete & recreate year if snapshot lost)
- No ambiguity about which data is active

**Trade-off:** More rigid (no fallback recovery); user must handle manually

---

## Migration Pattern for Existing Databases

1. **Runtime Detection** (`detectSchemaVersion()`)
   - Checks `INFORMATION_SCHEMA.COLUMNS` for V2 columns
   - Fresh databases get V2 automatically
   - Existing V1 databases detected and kept as-is

2. **Lazy Migration** (`ensureDropYearPlanFkV2()`)
   - Runs once per session if V1 database detected
   - Drops obsolete `fk_year_plan` FK if it exists
   - Idempotent (checks before dropping)

3. **No Data Loss**
   - FK removal does not alter `tbl_accountingyear` or `tbl_account` rows
   - Existing snapshots in CLOB remain unchanged
   - Data is preserved; relationship interpretation changes

---

## Testing Strategy

### Unit Tests
- Account plan snapshot serialization/deserialization
- Copy-on-create account population
- Year open validation (snapshot present/absent)

### Integration Tests
- Full year lifecycle (create → open → edit accounts → snapshot → close)
- Template update does not affect year's snapshot
- Template deletion doesn't block year access
- Template account copy completeness

### Manual Tests
1. Create new year with template selection; verify accounts are copied
2. Edit account plan; verify snapshot is updated, not template
3. Delete template; verify year still opens with snapshot
4. Open year without snapshot; verify error dialog and remediation message
5. Switch between years; verify each year's plan loads correctly

---

## Known Limitations & Future Work

### Current Limitations (Release 2)
1. **Snapshot Format**: Serialized Java object (not human-readable)
   - Blocks in Release 3?
2. **No Rollback**: Once snapshot written, cannot restore to previous version
   - Possible Release 3 feature
3. **No Sync UI**: No way to compare or sync template changes to year snapshot
   - Possible Release 3 feature
4. **Deletion Sensitivity**: Years with missing snapshots cannot be opened
   - By design; prevents silent data corruption

### Future Improvements
- Export/import snapshots in standard format (JSON/XML)
- Template versioning and year-specific overrides
- Account plan audit trail and change attribution
- Parallel template management (draft → staging → live)

---

## Checklist for Release 3 Planning

Before starting Release 3, answer:

- [ ] What is the primary pain point to solve in Release 3?
- [ ] Is it template management, snapshot visibility, sync controls, performance, or something else?
- [ ] Who are the end users affected, and what's their workflow?
- [ ] How does Release 3 integrate with existing year/template lifecycle?
- [ ] Are there breaking schema changes, or purely new features?
- [ ] What's the rollback strategy if Release 3 is problematic?

---

## Regression Testing Across All Releases

```bash
# Full suite (includes all three releases' tests)
mvn clean install

# Focus on account plan changes
mvn test -Dtest=*AccountPlan* -DfailIfNoTests=false

# Focus on year lifecycle
mvn test -Dtest=*AccountingYear* -DfailIfNoTests=false

# Check schema migration paths
mvn test -Dtest=SchemaV2* -DfailIfNoTests=false
```

---

## References

- **Code**: `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`
- **Schema**: `src/main/resources/sql/create_tables_v2.sql`
- **GUI**: `src/main/java/se/swedsoft/bookkeeping/gui/accountplans/SSAccountPlanDialog.java`
- **Tests**: `src/test/java/se/swedsoft/bookkeeping/data/system/` (integration tests)
- **Changelog**: `CHANGELOG.md` (entries for Releases 1 & 2)


