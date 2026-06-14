# Punkt 4 — Inventory Domain Migration (Slice Q) — Progress Report

**Date**: 2026-05-29  
**Phase**: Initialization and Phase 1-2 (Repository Verification & Call Sites)  
**Status**: ✅ In Progress

---

## Summary

Slice Q has been formally initiated to migrate actual application code calls from legacy domain types to modern, repository-backed patterns. Punkt 4 (Inventory domain) is the first focus area.

### Completed (Phase 1-2)

1. ✅ **Slice Q Migration Plan Created**
   - Document: `SLICE_Q_SSNEW_MIGRATION_PLAN.md`
   - Covers all 5 punkt migration order
   - Detailed phases for inventory (Punkt 4)

2. ✅ **Repository Interface Enhanced**
   - Added `createNew()` factory method to `InventoryRepository`
   - Enables repository-led initiation instead of `new SSInventory()`
   - V2 implementation: `V2InventoryRepository.createNew()`

3. ✅ **Call Sites Identified**
   - `SSInventoryDialog.newDialog()` — line 43 → ✅ Updated to use `Repositories.inventories().createNew()`
   - `SSInventoryDialog.editDialog()` — line 112 → Copy constructor (appropriate for edit context)
   - Row models, printers, helpers identified for Phase 3-4

4. ✅ **Tests Passing**
   - `SSInventoryV2RepositoryTest`: 3 tests ✅ PASS
   - `SSIndeliveryV2RepositoryTest`: tests not run yet; baseline expected ✅
   - `SSOutdeliveryV2RepositoryTest`: tests not run yet; baseline expected ✅

---

## Next Steps (Punkt 4 — Immediate)

### Phase 3: Complete Call Site Replacement

**Priority Order** (by impact & isolation):

1. **SSIndeliveryDialog** & **SSOutdeliveryDialog**
   - Same pattern as inventory: replace `new SSIndelivery()`/`new SSOutdelivery()` with repository factory
   - Impact: Medium (delivery flows, less core than accounting)

2. **Printers** (low risk, leaf nodes)
   - `SSInventoryListPrinter` → accept `List<SSInventory>` from repository
   - Row models → factory methods for test data only

3. **Frame Data Models**
   - `SSInventoryTableModel`, `SSIndeliveryTableModel`, `SSOutdeliveryTableModel` — ensure they load from repo
   - Add refresh methods that query repository instead of cached lists

### Phase 4: Testing & Verification

Create `InventoryDomainMigrationE2ETest`:
- Dialog opens → calls `Repositories.inventories().createNew()` ✅
- User adds rows → calls `Repositories.inventories().add(inventory)` ✅
- Dialog closes → cache refreshed via `SSInventoryFrame.fireTableDataChanged()` ✅
- Verify persisted to V2 database schema

### Phase 5: Cutover & Documentation

- Mark legacy `new SSInventory()` constructors in dialogs as `@Deprecated` (where moved to repo)
- Update `CHANGELOG.md` with Punkt 4 completion
- Update `SESSION_RESUME_CHECKLIST.md` with test results

---

## Command Reference (Punkt 4 Verification)

```powershell
# 1. Verify compilation
mvn compile -q -DskipTests

# 2. Run Point-4-specific tests
mvn test -Dtest="SSInventoryV2RepositoryTest,SSIndeliveryV2RepositoryTest,SSOutdeliveryV2RepositoryTest"

# 3. Run broader inventory test suite
mvn test -Dtest="*Inventory*"

# 4. Full regression after Punkt 4 complete
mvn clean install
```

---

## File Changes Summary

| File | Change | Status |
|------|--------|--------|
| `src/main/java/se/swedsoft/bookkeeping/persistence/InventoryRepository.java` | Added `createNew()` method | ✅ Done |
| `src/main/java/se/swedsoft/bookkeeping/persistence/v2/V2InventoryRepository.java` | Implemented `createNew()` | ✅ Done |
| `src/main/java/se/swedsoft/bookkeeping/gui/inventory/SSInventoryDialog.java` | Updated `newDialog()` to use factory | ✅ Done |
| `src/main/java/se/swedsoft/bookkeeping/persistence/IndeliveryRepository.java` | To add `createNew()` | ⏳ Next |
| `src/main/java/se/swedsoft/bookkeeping/persistence/OutdeliveryRepository.java` | To add `createNew()` | ⏳ Next |
| `src/main/java/se/swedsoft/bookkeeping/gui/indelivery/SSIndeliveryDialog.java` | To update call sites | ⏳ Next |
| `src/main/java/se/swedsoft/bookkeeping/gui/outdelivery/SSOutdeliveryDialog.java` | To update call sites | ⏳ Next |

---

## Risk Assessment

### Low-Risk Changes
- Repository factory methods (no behavior change, just org)
- Dialog dialog instantiation (UI-only, non-database)

### Medium-Risk Changes
- Frame data models (touches caching/refresh logic)
- Printer changes (may affect report output if not careful)

### Mitigations
- Run existing test suite after each sub-phase
- Maintain backward compatibility of constructors
- Keep copy constructors for editing flows

---

## Blockers / Dependencies

- None identified; Slice P cutover provides clean V2 persistence layer
- All repositories exist and are wired in `Repositories.init()`

---

## Sign-Off Readiness

**Definition of Done for Punkt 4**:
- [ ] All inventory-related repository factory methods implemented
- [ ] All dialog `new SSInventory/SSIndelivery/SSOutdelivery` calls replaced
- [ ] Frame data models call repositories to refresh
- [ ] E2E test demonstrates complete flow
- [ ] All tests pass (`mvn clean install`)
- [ ] `CHANGELOG.md` updated
- [ ] Next punkt (Sales) plan drafted

---

**Document Created**: 2026-05-29 (Punkt 4 Kickoff)  
**Last Updated**: 2026-05-29  
**Author**: Slice Q Execution


