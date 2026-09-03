# Inventering: Globalt tillstånd och Cache-livscykel i SSDB

Detta dokument sammanfattar inventeringen av globalt tillstånd och cache-livscykel i `SSDB` (Delmoment 5, Delsteg 1).

## Globalt tillstånd (Session / Context)

- `iCurrentCompany` (SSNewCompany)
  - Aktuellt valt bolag.
  - Sätts via `setCurrentCompany()` / `setCurrentCompanyInternal()`.
  - Initialiseras i `prepareStartupConnection()` och `initializeCurrentCompanyAndYear()`.

- `iCurrentYear` (SSNewAccountingYear)
  - Aktuellt valt räkenskapsår.
  - Sätts via `setCurrentYear()` / `applyOpenedYearFromRepository()` eller `openYear()`.

- `iConnection` (Connection)
  - Databasanslutning som SSDB använder för alla repository-/schemaoperationer.
  - Sätts i `prepareStartupConnection()` eller `loadLocalDatabase()` och hålls under sessionens livstid.

## Event / Trigger - tillstånd

- `iEventBus` (SSDBEventBus)
  - Intern eventbus för property-change-notifieringar.
  - Används för att publicera `COMPANY`, `YEAR` och andra events till lyssnare.

- `iBypassTriggerDispatcher` (ThreadLocal<Boolean>)
  - Flagga för att kringgå trigger-dispatchern vid särskilda operationer.

- `iEventTriggerDispatcher` (SSEventTriggerDispatcher)
  - Dispatcher som delegarar trigger-händelser till kategori-handlers.
  - Innehåller handlers för masterdata, sales, payments, purchase/supplier, inventory, accounting och reports.

## Cache-listor (in-memory)

SSDB innehåller följande in-memory-cacher (lazy-loaded, initieras till `null`):

Masterdata:
- `iProducts` (List<SSProduct>)
- `iCustomers` (List<SSCustomer>)
- `iSuppliers` (List<SSSupplier>)
- `iAutoDists` (List<SSAutoDist>)

Sales / Transaktioner:
- `iInpayments` (List<SSInpayment>)
- `iTenders` (List<SSTender>)
- `iOrders` (List<SSOrder>)
- `iInvoices` (List<SSInvoice>)
- `iCreditInvoices` (List<SSCreditInvoice>)
- `iPeriodicInvoices` (List<SSPeriodicInvoice>)

Purchase / Supplier:
- `iOutpayments` (List<SSOutpayment>)
- `iPurchaseOrders` (List<SSPurchaseOrder>)
- `iSupplierInvoices` (List<SSSupplierInvoice>)
- `iSupplierCreditInvoices` (List<SSSupplierCreditInvoice>)

Inventory:
- `iInventories` (List<SSInventory>)
- `iIndeliveries` (List<SSIndelivery>)
- `iOutdeliveries` (List<SSOutdelivery>)

Accounting / Reports:
- `iVouchers` (List<SSVoucher>) — year-specifik
- `iOwnReports` (List<SSOwnReport>)

## Cache-livscykel

### Initialisering
- `prepareStartupConnection()` nollställer sessionstillstånd och anropar `clearCachedLists()`.
- `init()` laddar masterdata och transaktions-cacher via `load*()`-metoder.
- `initYear()` laddar year-specifika data (t.ex. vouchers).

### Invalidation
- `clearCachedLists()` — sätter alla cache-listor till `null`.
  - Anropas i `prepareStartupConnection()` och kan anropas via `SSEventTriggerSyncContext.clearCachedLists()`.

- `setCurrentCompanyInternal()` — vid bolagsbyte:
  - Nollställer masterdata och de flesta transaktionscacher (samma effekt som `clearCachedLists()` på många fält).
  - Publicerar `COMPANY`-event.

- `setCurrentYear()` / `applyOpenedYearFromRepository()` — vid år-byte:
  - Nollställer `iVouchers` (year-specifik cache).
  - Publicerar `YEAR`-event.

### Lazy-loading
- Varje `load*()` metod returnerar cachen om den är satt, annars läses färsk data via `Repositories.*().findAll()` och cachen fylls.

### Trigger-baserad uppdatering
- När DB-triggers inträffar anropas `triggerAction()` som dispatchar till `SSEventTriggerDispatcher`.
- Handlers gör in-place-uppdateringar (add/update/remove) i relevanta cache-listor och triggar UI-uppdateringar.

## Externa / statiska hjälpcacher
- `SSInvoiceMath.iSaldoMap`, `SSCustomerMath.iInvoicesForCustomers`, `SSSupplierInvoiceMath.iSaldoMap`, `SSSupplierMath.iInvoicesForSuppliers` etc.
  - Dessa är beroende av SSDB-cacher och återställs i `init()` genom att sättas `null` och beräknas om.

## Rekommendationer (hög nivå)
- Centralisera cache-invalidation i en `SessionStateManager` med tydliga metoder för company/year-byten och full/partial invalidation.
- Behåll lazy-loading som default, men erbjuda explicit `reload`-strategi för init/test/CI-scenarier.
- Publicera tydliga events för `COMPANY` och `YEAR` så UI och beräkningshjälpare kan reagera deterministiskt.

---

Fil skapad av automatiserad inventering.

