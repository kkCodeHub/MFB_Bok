# Phase 3 TWR Memory List

_Last updated: 2026-05-28_

## Purpose

This file is a handoff/memory list for finishing the remaining try-with-resources (TWR)
cleanup in `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`.

Use this file as the resume point for the next session.

---

## Current Verified Snapshot

Scope: `SSDB.java`

- Total remaining `.close()` hits: **194**
- Intentional `iConnection.close()` calls: **4**
- Remaining non-connection manual closes: **190**
- Remaining TWR clusters (method-level buckets): **91**

Important: **Phase 3 is not complete in the current workspace state.**

---

## Intentional Close Calls That Must Remain

These are lifecycle connection shutdown calls and should **not** be migrated away as part
of the TWR cleanup goal:

- `shutdown()`
  - keeps `iConnection.close()`
- `shutdownCompact()`
  - keeps `iConnection.close()`
- `loadLocalDatabase()`
  - keeps `iConnection.close()` before reopening the local DB
- `delete()`
  - keeps `iConnection.close()`

Notes:
- `shutdown()` and `shutdownCompact()` still contain statement-level manual close logic that
  **can** be migrated to TWR.
- The earlier assumption that only `shutdown()` / `shutdownCompact()` / `delete()` remained
  was incomplete because `loadLocalDatabase()` also intentionally closes the connection.

---

## Definition of Done for Phase 3

Phase 3 is done when all of the following are true:

1. `SSDB.java` has **no manual close calls** for:
   - `Statement`
   - `PreparedStatement`
   - `ResultSet`
2. The **only** remaining `.close()` calls in `SSDB.java` are these four intentional
   connection closes:
   - `shutdown()` → `iConnection.close()`
   - `shutdownCompact()` → `iConnection.close()`
   - `loadLocalDatabase()` → `iConnection.close()`
   - `delete()` → `iConnection.close()`
3. Compile and test stay green.

---

## Remaining Clusters by Area

### AutoDist
- `getAutoDist` (line ~7948)
- `getAutoDists` (line ~7982)

### Tender
- `getTenderRowsV2` (line ~8172)
- `replaceTenderRowsV2` (line ~8211)
- `getTenderIdV2` (line ~8237)
- `getTenders` paginated variant (line ~8355)
- `getTender` (line ~8403)
- `getTenders` list variant (line ~8437)
- `addTender` (line ~8485)

### Order
- `getOrderRowsV2` (line ~8668)
- `replaceOrderRowsV2` (line ~8707)
- `getOrderIdV2` (line ~8733)
- `getOrders` paginated variant (line ~8851)
- `getOrder` (line ~8899)
- `getOrders` list variant (line ~8933)
- `addOrder` (line ~8980)

### Invoice
- `getInvoiceRowsV2` (line ~9183)
- `replaceInvoiceRowsV2` (line ~9222)
- `getInvoiceIdV2` (line ~9248)
- `getInvoices` paginated variant (line ~9379)
- `getInvoice` (line ~9427)
- `getInvoices` list variant (line ~9461)
- `addInvoice` (line ~9508)

### Inpayment
- `getInpaymentRowsV2` (line ~9704)
- `replaceInpaymentRowsV2` (line ~9729)
- `getInpaymentIdV2` (line ~9750)
- `getInpayments` (line ~9804)
- `getInpayment` (line ~9852)
- `addInpayment` (line ~9935)

### Outpayment
- `getOutpaymentRowsV2` (line ~10110)
- `replaceOutpaymentRowsV2` (line ~10135)
- `getOutpaymentIdV2` (line ~10156)
- `getOutpayments` (line ~10210)
- `getOutpayment` (line ~10258)
- `addOutpayment` (line ~10341)

### CreditInvoice
- `getCreditInvoiceRowsV2` (line ~10518)
- `replaceCreditInvoiceRowsV2` (line ~10557)
- `getCreditInvoiceIdV2` (line ~10584)
- `getCreditInvoices` paginated variant (line ~10723)
- `getCreditInvoice` (line ~10771)
- `getCreditInvoices` list variant (line ~10805)
- `addCreditInvoice` (line ~10852)

### PeriodicInvoice
- `getPeriodicInvoices` (line ~11108)
- `getPeriodicInvoice` (line ~11156)
- `addPeriodicInvoice` (line ~11190)

### PurchaseOrder
- `getPurchaseOrders` paginated variant (line ~11395)
- `getPurchaseOrder` (line ~11443)
- `getPurchaseOrders` list variant (line ~11477)
- `addPurchaseOrder` (line ~11524)

### SupplierInvoice
- `getSupplierInvoiceRowsV2` (line ~11711)
- `replaceSupplierInvoiceRowsV2` (line ~11740)
- `getSupplierInvoiceIdV2` (line ~11766)
- `getSupplierInvoices` paginated variant (line ~11867)
- `getSupplierInvoice` (line ~11915)
- `getSupplierInvoices` list variant (line ~11949)
- `addSupplierInvoice` (line ~11996)

### SupplierCreditInvoice
- `getSupplierCreditInvoices` (line ~12256)
- `getSupplierCreditInvoice` (line ~12304)
- `addSupplierCreditInvoice` (line ~12338)

### Indelivery / Inventory / Outdelivery
- `getIndeliveryRowsV2` (line ~12602)
- `replaceIndeliveryRowsV2` (line ~12619)
- `getIndeliveryIdV2` (line ~12637)
- `getInventoryRowsV2` (line ~12672)
- `replaceInventoryRowsV2` (line ~12690)
- `getInventoryIdV2` (line ~12709)
- `getInventories` (line ~12748)
- `getInventory` (line ~12796)
- `addInventory` (line ~12830)
- `getOutdeliveryRowsV2` (line ~12999)
- `replaceOutdeliveryRowsV2` (line ~13016)
- `getOutdeliveryIdV2` (line ~13034)
- `getIndeliveries` (line ~13073)
- `getIndelivery` (line ~13121)
- `addIndelivery` (line ~13155)
- `getOutdeliveries` (line ~13328)
- `getOutdelivery` (line ~13376)
- `addOutdelivery` (line ~13410)

### OwnReport
- `getOwnReportAccountRowsV2` (line ~13579)
- `getOwnReportRowsV2` (line ~13605)
- `replaceOwnReportAccountRowsV2` (line ~13641)
- `replaceOwnReportRowsV2` (line ~13665)
- `getOwnReports` paginated variant (line ~13702)
- `getOwnReport` overload 1 (line ~13750)
- `getOwnReport` overload 2 (line ~13787)
- `getOwnReports` list variant (line ~13824)
- `addOwnReport` (line ~13872)
- `updateOwnReport` (line ~13914)
- `deleteOwnReport` (line ~13947)

### Bootstrap / schema / trigger helpers
- `createLocalTriggers` (line ~13987)
- `dropTriggers` (line ~14070)
- `createNewTables` (line ~14144)

---

## Suggested Resume Order

Recommended order for the next session:

1. **Small tail first**
   - `shutdown()` statement close
   - `shutdownCompact()` statement close
   - `createLocalTriggers`
   - `dropTriggers`
   - `createNewTables`
2. **Repeatable CRUD blocks**
   - Tender
   - Order
   - Invoice
   - Inpayment
   - Outpayment
   - CreditInvoice
3. **Later document groups**
   - PeriodicInvoice
   - PurchaseOrder
   - SupplierInvoice
   - SupplierCreditInvoice
4. **Largest tail**
   - Indelivery / Inventory / Outdelivery
   - OwnReport

Reason: the repeating CRUD patterns make safe batching easier once one domain pattern is proven.

---

## Migration Patterns to Apply

Typical conversions still needed:

### Query methods
Convert from manual:
- create statement
- execute query
- iterate result set
- manual `iResultSet.close()` / `iStatement.close()`

To nested try-with-resources:
- `try (PreparedStatement iStatement = ...) { ... }`
- `try (ResultSet iResultSet = iStatement.executeQuery()) { ... }`

### Delete + insert replacement methods
Convert from manual:
- `PreparedStatement iDelete = ...`
- `iDelete.close()`
- inside loop: `PreparedStatement iInsert = ...`
- `iInsert.close()`

To TWR:
- one TWR for delete statement
- one TWR for insert statement reused inside the loop when possible

### ID lookup helpers
Convert from manual/finally:
- `PreparedStatement iStatement = ...`
- `ResultSet iResultSet = ...`
- `finally { iResultSet.close(); iStatement.close(); }`

To TWR:
- `try (PreparedStatement ...) { try (ResultSet ...) { ... } }`

---

## Verification Commands

Run after each batch:

```powershell
mvn clean compile -q
mvn test -q
```

Quick close-scan for `SSDB.java`:

```powershell
Select-String -Path "src\main\java\se\swedsoft\bookkeeping\data\system\SSDB.java" -Pattern "\.close\s*\("
```

Final success condition for the grep scan:
- only the four intentional `iConnection.close()` lines remain

---

## Practical Reminder for Next Session

When continuing later:

1. Open `PHASE3_TWR_MEMORY_LIST.md`
2. Open `SSDB.java`
3. Start with the small tail (`shutdown*`, triggers, table creation)
4. Migrate one domain batch at a time
5. Compile and test after each batch
6. Stop only when the `.close()` grep scan shows only the four intentional
   `iConnection.close()` calls

