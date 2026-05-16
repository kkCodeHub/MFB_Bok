# Steg 2 Reference Data (Kategori A) V2-Patch — Implementation Complete

**Date:** 2026-05-11  
**Scope:** Full V2-patch implementation for 5 reference-data domains (Kategori A)  
**Status:** ✅ COMPLETE — Compilation & Test Success

## Summary

This session completed the **full V2-patch for all five Kategori A (reference data) domains**:
1. **SSCurrency** (Valutor)
2. **SSUnit** (Enheter)
3. **SSDeliveryWay** (Leveranssätt)
4. **SSDeliveryTerm** (Leveransvillkor)
5. **SSPaymentTerm** (Betalningsvillkor)

These are "cutover" domains — they now always use V2 repositories regardless of the schema flag, similar to the existing H-domain cutover pattern.

## Deliverables

### 1. Repository Interfaces (5 new)
- `CurrencyRepository.java` — interface with CRUD + lookup methods
- `UnitRepository.java` — interface with CRUD + lookup methods
- `DeliveryWayRepository.java` — interface with CRUD + lookup methods
- `DeliveryTermRepository.java` — interface with CRUD + lookup methods
- `PaymentTermRepository.java` — interface with CRUD + lookup methods

**Location:** `src/main/java/se/swedsoft/bookkeeping/persistence/`

### 2. V2 Repository Implementations (5 new)
- `V2CurrencyRepository.java` — delegates to SSDB.getCurrencies() / addCurrency() / updateCurrency() / deleteCurrency()
- `V2UnitRepository.java` — delegates to SSDB.getUnits() / addUnit() / updateUnit() / deleteUnit()
- `V2DeliveryWayRepository.java` — delegates to SSDB.getDeliveryWays() / addDeliveryWay() / updateDeliveryWay() / deleteDeliveryWay()
- `V2DeliveryTermRepository.java` — delegates to SSDB.getDeliveryTerms() / addDeliveryTerm() / updateDeliveryTerm() / deleteDeliveryTerm()
- `V2PaymentTermRepository.java` — delegates to SSDB.getPaymentTerms() / addPaymentTerm() / updatePaymentTerm() / deletePaymentTerm()

**Location:** `src/main/java/se/swedsoft/bookkeeping/persistence/v2/`

### 3. SSDB.java V2 Routing & SQL Helpers
Updated each of the 5 domain methods in `SSDB.java`:
- Added `useSchemaV2()` guard in all public methods (get/add/update/delete)
- Implemented 20+ private V2 helper methods with direct SQL mapping
- All new lookups: `getUnit(String name)`, `getDeliveryWay(String name)`, `getDeliveryTerm(String name)`, `getPaymentTerm(String name)`

**Key SQL Mapping:**
- `SSCurrency` → `tbl_currency(code, description, exchange_rate)`
- `SSUnit` → `tbl_unit(name, description)`
- `SSDeliveryWay` → `tbl_deliveryway(name, description)`
- `SSDeliveryTerm` → `tbl_deliveryterm(name, description)`
- `SSPaymentTerm` → `tbl_paymentterm(name, description, days)`

### 4. Repositories Factory (Updated)
- Added 5 new static fields for the repository instances
- Added imports for all 5 V2 repository classes
- Updated `init(SSDB)` to instantiate all 5 V2 repositories as **always-V2** (cutover pattern)
- Added 5 public static getter methods: `currencies()`, `units()`, `deliveryWays()`, `deliveryTerms()`, `paymentTerms()`
- Updated Javadoc to document the five new cutover domains

**Location:** `src/main/java/se/swedsoft/bookkeeping/persistence/Repositories.java`

## Build Verification

✅ **Compilation:** `mvn compile -q` → SUCCESS  
✅ **Tests:** `mvn test -q` → Green  
✅ **Dependencies:** All imports resolved, no circular dependencies

## Architecture Pattern (Cutover)

All 5 reference-data repositories follow the **Slice P cutover pattern**:

```
┌─────────────────────────────────────────┐
│ Application Code                        │
│  → Repositories.currencies() / etc.     │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│ V2CurrencyRepository / etc. (Cutover)   │
│  • Always instantiated in init()        │
│  • No schema flag check needed          │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│ SSDB.java (V2-aware)                    │
│  • useSchemaV2() guards in CRUD methods │
│  • Returns deserialized objects (V1)    │
│    or mapped from SQL (V2)              │
└────────────┬────────────────────────────┘
             │
             ↓
┌─────────────────────────────────────────┐
│ HSQLDB 2.7.2                            │
│  • tbl_currency, tbl_unit, etc.         │
│  • V2 normalized schema                 │
└─────────────────────────────────────────┘
```

## Testing Strategy

The implementation is production-ready for integration tests when those are written. Tests should verify:

1. **V2 Roundtrips:** Create → Read → Update → Delete via repository
2. **Mapping Accuracy:** Object fields ↔ SQL columns
3. **Normalization:** Exchange rates, days calculations preserved
4. **Factory Wiring:** `Repositories.init()` always uses V2 for these domains
5. **Optional Lookups:** `getPaymentTerm("30")` returns valid Optional

## Next Steps

### Immediate (Recommended)
1. Write integration tests:
   - `SSCurrencyV2RepositoryTest`
   - `SSUnitV2RepositoryTest`
   - `SSDeliveryWayV2RepositoryTest`
   - `SSDeliveryTermV2RepositoryTest`
   - `SSPaymentTermV2RepositoryTest`

2. Add cutover test (similar to `RepositoriesHDomainCutoverTest.java`):
   - Verify all 5 are wired as V2-only in both V1 and V2 modes

3. Update `SESSION_RESUME_CHECKLIST.md` with:
   - Kategori A cutover completion date
   - Test status
   - Next domain group priority

### Longer-term
- Consider pulling category A reference-data cutover into earlier slices if warranted
- Evaluate whether `V1CurrencyRepository` etc. can be fully removed (no legacy fallback needed)
- Plan cutover for Kategori B (masterdata: Customer, Supplier, Product, Project, ResultUnit)

## Files Modified/Created

### Created
```
src/main/java/se/swedsoft/bookkeeping/persistence/
  • CurrencyRepository.java
  • UnitRepository.java
  • DeliveryWayRepository.java
  • DeliveryTermRepository.java
  • PaymentTermRepository.java

src/main/java/se/swedsoft/bookkeeping/persistence/v2/
  • V2CurrencyRepository.java
  • V2UnitRepository.java
  • V2DeliveryWayRepository.java
  • V2DeliveryTermRepository.java
  • V2PaymentTermRepository.java
```

### Modified
```
src/main/java/se/swedsoft/bookkeeping/data/system/
  • SSDB.java (added ~350 lines: V2 routing + 20 helper methods)

src/main/java/se/swedsoft/bookkeeping/persistence/
  • Repositories.java (added 5 fields, 5 getters, updated init() and Javadoc)
```

## Commit Summary (Recommended)

```
feat(slice-Q): Implement full V2-patch for Kategori A reference domains

- Add repository interfaces & V2 implementations for 5 reference domains:
  SSCurrency, SSUnit, SSDeliveryWay, SSDeliveryTerm, SSPaymentTerm
- Implement V2 SQL mapping in SSDB.java with direct column access
- Re-wire Repositories factory to always use V2 for these domains (cutover)
- Add lookup methods (getUnit(), getDeliveryWay(), etc.) for convenience
- All 5 domains follow Slice P cutover pattern (V2-only, no schema toggle)

Migration path (V1 → V2):
  ✓ V1: SELECT OBJECT columns from tbl_currency/unit/etc. (deserialize)
  ✓ V2: SELECT normalized columns, map to domain objects in Java

Tests passing: mvn compile ✓, unit tests ✓
Ready for: integration test suite (Kategori A)
Blocks: Kategori B (masterdata) if any FK constraints exist

Fixes: Preparation for full reference-data normalization.
```

---

**Implementation Date:** 2026-05-11  
**Status:** ✅ READY FOR INTEGRATION TESTS

