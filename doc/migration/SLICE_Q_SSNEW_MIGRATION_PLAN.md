# Slice Q – SSNew* Domain Migration (Actual Calls from Legacy to Modern APIs)

**Status**: Initiating  
**Start Date**: 2026-05-29  
**Phase After**: Slice P (V2 Persistence Cutover Complete)  
**Goal**: Replace active uses of legacy domain classes (SSInvoice, SSInventory, etc.) with SSNew* equivalents in application code

---

## Scope & Objectives

### What This Slice Does
- Migrates **application code** (dialogs, panels, services, business logic) from legacy SS* class usage to SSNew* equivalents
- **NOT about persistence** (Slice P already completed V2 storage)
- **IS about API contracts** — where code accepts/returns domain objects, prefer SSNew* types
- Covers all 5 domain categories:
  1. **Masterdata** (SSNewCustomer, SSNewSupplier, SSNewProduct, SSNewCompany)
  2. **Sales** (SSNewInvoice, SSNewOrder, SSNewTender, SSNewCreditInvoice, SSNewPeriodicInvoice)
  3. **Purchase** (SSNewSupplierInvoice, SSNewSupplierCreditInvoice, SSNewPurchaseOrder)
  4. **Inventory** (SSNewInventory, SSNewIndelivery, SSNewOutdelivery) ← **Punkt 4 — START HERE**
  5. **Accounting** (SSNewAccountingYear, SSNewVoucher, SSNewAccountPlan)

### Binding Constraints
- No API signature changes without explicit approval
- Maintain backward compatibility with legacy paths until cutover complete
- Each domain migrated independently
- Repository layer from Slice P used as boundary

---

## Execution Order (Locked)

Per domain priority/risk:

1. **Punkt 4 — Inventory** (F/G/H: Indelivery, Outdelivery, Inventory, AutoDist, Outpayment, Inpayment)
   - Relatively isolated from core accounting
   - Wide usage in panels and dialogs
   - Lowest risk of cascading failure

2. **Sales** (D: Invoice, Order, Tender, CreditInvoice, PeriodicInvoice)
   - Higher risk due to accounting integration
   - Priority after inventory proves pattern works

3. **Purchase** (E: SupplierInvoice, SupplierCreditInvoice, PurchaseOrder)
   - Companion to Sales

4. **Accounting** (C: AccountingYear, Voucher, AccountPlan)
   - Core business logic; migrate last
   - Requires careful testing

5. **Masterdata** (B: Customer, Supplier, Product, Company)
   - Cross-cutting; review after transaction domains

---

## Punkt 4 — Inventory Domain Detailed Plan

### Phase 1: Inventory Repository Verification

**Goal**: Confirm V2InventoryRepository covers all required operations

**Tasks**:
- [ ] Review `V2InventoryRepository` interface and implementation
- [ ] Verify `add()`, `update()`, `delete()`, `findByInventory()`, `findAll()` exist
- [ ] Check child-table handling (indelivery/outdelivery rows)
- [ ] Write integration test proving full CRUD works

**Files Involved**:
- `src/main/java/se/swedsoft/bookkeeping/persistence/InventoryRepository.java`
- `src/main/java/se/swedsoft/bookkeeping/persistence/v2/V2InventoryRepository.java`
- `src/test/java/se/swedsoft/bookkeeping/persistence/SSInventoryV2RepositoryTest.java`

### Phase 2: Identify Call Sites in Inventory Domain

**Goal**: Catalog all places where code constructs/uses SSInventory objects

**Key Classes/Packages to Scan**:
- `se.swedsoft.bookkeeping.gui.inventory.*` — dialogs, panels, frames
- `se.swedsoft.bookkeeping.data.system.SSDB` — inventory CRUD methods
- `se.swedsoft.bookkeeping.calc.math.SSInventoryMath` — calculation helpers
- `se.swedsoft.bookkeeping.print.report.*` — inventory list printer
- `se.swedsoft.bookkeeping.importexport.*` — inventory import/export

**Artifact**: Searchable list of SSInventory usage sites (template below)

| File | Method | Line | Usage Type | Needs SSNew? |
|------|--------|------|-----------|------------|
| `SSInventoryPanel.java` | `onAddInventory()` | 45 | Constructor `new SSInventory()` | YES |
| `SSInventoryListPrinter.java` | `print(List<SSInventory>)` | 120 | Parameter type | YES |

### Phase 3: Create SSNewInventory Bridge (if needed)

**Goal**: Ensure SSNewInventory class exists and covers SSInventory's public interface

**Decision Point**: Check if SSNewInventory already exists:
- If exists & complete → skip to Phase 4
- If partial → add missing accessors
- If missing → create new class

**Expected SSNewInventory API**:
```java
public class SSNewInventory {
    int getNumber();
    LocalDate getLocalDate();
    String getText();
    List<SSNewInventoryRow> getRows();
    
    void setNumber(int);
    void setLocalDate(LocalDate);
    void setText(String);
    void setRows(List<SSNewInventoryRow>);
}
```

### Phase 4: Gradual Replacement in GUI & Business Logic

**Approach**: Replace progressively from leaf to root

**Priority Order**:
1. **Dialogs** (leaf): `SSInventoryDialog`
   - Replace `SSInventory` constructor with repository method
   - Update handler methods to work with SSNewInventory

2. **Panels** (mid): `SSInventoryPanel`
   - Update list model to return SSNewInventory
   - Replace `add/update/delete` calls to use repositories

3. **Frames** (mid): `SSInventoryFrame`
   - Update table/list cache to SSNewInventory
   - Add refresh methods that reload from repository

4. **Printers** (leaf): `SSInventoryListPrinter`
   - Add overload accepting `List<SSNewInventory>`
   - Gradually migrate call sites to new signature

5. **Math Helpers** (isolated): `SSInventoryMath`
   - Create SSNewInventory-based calculation methods
   - Deprecate old SSInventory versions

6. **SSDB Methods** (boundary): Inventory CRUD
   - Keep both `add(SSInventory)` and `add(SSNewInventory)`
   - Mark legacy versions `@Deprecated`

### Phase 5: Testing Strategy

**Test Classes to Create/Update**:

1. `InventoryDomainMigrationTest` (new)
   - E2E: Dialog → Panel → Repository → V2DB
   - Verify SSNewInventory flows end-to-end

2. `SSInventoryPrinterSSNewCompatTest` (new)
   - Confirm SSNewInventory produces same output

3. Existing tests remain green:
   - `SSInventoryV2RepositoryTest`
   - `RepositoriesInventoryCutoverTest`
   - Any GUI tests referencing inventory

**Commands to Verify**:
```powershell
# Phase 4 focused test
mvn test -Dtest=InventoryDomainMigrationTest

# Regression on all inventory tests
mvn test -Dtest=*Inventory*

# Full build
mvn clean install
```

---

## Definition of Done (Punkt 4 — Inventory)

- [ ] `SSNewInventory` class (or equivalent) exists and matches SSInventory public API
- [ ] All leaf call sites (dialogs, printers, helpers) accept SSNewInventory
- [ ] `SSInventoryPanel` and `SSInventoryFrame` work with SSNewInventory
- [ ] Repository methods are the primary way to load/store inventory
- [ ] `SSInventoryDialog` no longer directly instantiates SSInventory
- [ ] New test class `InventoryDomainMigrationTest` passes
- [ ] All existing inventory tests pass (regression clean)
- [ ] `mvn clean install` passes
- [ ] `CHANGELOG.md` updated with Punkt 4 completion
- [ ] Documentation updated in `doc/migration/SESSION_RESUME_CHECKLIST.md`

---

## Punkt 5+ — Sales, Purchase, Accounting Order

### Punkt 5 — Sales (Post-Inventory)
- Parallel phases to inventory, starting after punkt 4 approved
- Domains: Invoice, Order, Tender, CreditInvoice, PeriodicInvoice
- Higher complexity due to voucher integration

### Punkt 6 — Purchase (Post-Sales)
- SupplierInvoice, SupplierCreditInvoice, PurchaseOrder
- Leverages sales pattern

### Punkt 7 — Accounting (Post-Purchase)
- AccountingYear, Voucher, AccountPlan
- Highest-risk; requires extensive test coverage

### Punkt 8 — Masterdata (Post-Accounting)
- Company, Customer, Supplier, Product
- Review for cross-domain impacts

---

## Guardrails & Rollback

### No-Go Conditions
- Regression in any test suite
- Data loss or corruption in integration test
- Payment-related flows broken
- Accounting year boundaries affected

### Rollback Procedure
- Revert last commit(s) in Slice Q
- Return to Slice P baseline (last passing invoice/inventory cut over)
- Document issue in `doc/migration/SESSION_RESUME_CHECKLIST.md`
- Schedule explicit re-review before retry

---

## Files to Read First (Session Start)

1. `doc/migration/SLICE_Q_SSNEW_MIGRATION_PLAN.md` (this file)
2. `V2_DOMAIN_MODEL_TARGET.md`
3. `src/main/java/se/swedsoft/bookkeeping/data/SSNewInventory.java`
4. `src/main/java/se/swedsoft/bookkeeping/gui/inventory/SSInventoryPanel.java`
5. `src/main/java/se/swedsoft/bookkeeping/persistence/v2/V2InventoryRepository.java`
6. `CHANGELOG.md` (check recent entries)

---

**Document Created**: 2026-05-29  
**Phase**: Punkt 4 Initiation  
**Next Review**: After Phase 1 verification (repository confirmation)


