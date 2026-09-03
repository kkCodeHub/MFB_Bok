# SSDB avveckling och 12 domaner - handoverstatus

## Syfte
Denna fil ar en kort overlamning for nya chattradar. Fokus ar avveckling av `SSDB`, etablering av 12 domankontext, och stegvis flytt av kod till context/repository-lagret.

## Nulage (sammanfattning)
- Arkitektursparet ar aktivt: `SSDB` ska bli tunn adapter.
- Domankontext och repositories finns etablerade for centrala delar.
- CRUD flyttas domanvis fran `SSDB` till context/repository.
- `SSDB` innehaller fortfarande legacy-ytor, men flera ar nu markerade `@Deprecated`.
- Forsta fysiska borttagning av deprecierade `SSDB`-metoder ar paborjad (voucher-relaterat).

## Genomfort (enligt senaste arbete i traden)
1. Steg 5-8 i planen har genomforts i praktiken:
   - nya/uppdaterade context-klasser
   - repository-first i fler domaner
   - `SSDB` markerad med `@Deprecated` pa migrerade publika metoder
   - UI/initialisering flyttad mot context-overloads
2. `SSCompanyYearContext` migrerad till repository-first for CRUD.
3. `SSAccountingContext` TODO-punkter stangda via repositories.
4. `Project` och `ResultUnit` flyttade till egna repositories/context-monstren.
5. Product/OwnReport-sparet delat och verifierat utan korsanrop.
6. Steg 2 (Lifecycle split) hanterat via:
   - `SSSystemConfigContext` (startup/shutdown/db-init/schema)
   - `SSEventTriggerSyncContext` (trigger/sync/event-routing)
7. Test-fixtures har migrerats fran direkta `SSDB.getInstance()`-anrop till context-lagret.
8. Forsta rensningsbatch i `SSDB` utford med borttagning av utvalda deprecierade voucher-metoder efter callsite-nollning.
9. Batch 1 (Masterdata) klar: callsites migrerade till context/repository, repository-adaptrar uppdaterade, och deprecierade kund/leverantor-metoder fysiskt borttagna ur `SSDB`.
10. Batch 2 (Project & ResultUnit) klar: context-klasser styr nu via repositories, repository-adaptrar uppdaterade, och deprecierade project/resultunit-metoder fysiskt borttagna ur `SSDB`.
11. Batch 3 (Product) klar: tests/callsites migrerade till `SSProductContext`, repository-adapter uppdaterad, och deprecierade product-metoder fysiskt borttagna ur `SSDB`.
12. Batch 4 (OwnReport) klar: OwnReport-callsites migrerade till `SSOwnReportContext`, repository-adapter uppdaterad, och deprecierade ownreport-metoder fysiskt borttagna ur `SSDB`.
13. Batch 5 (Inventory & Deliveries) klar: callsites migrerade till `SSInventoryDeliveriesContext`, repository-adaptrar uppdaterade, och deprecierade inventory/indelivery/outdelivery-metoder fysiskt borttagna ur `SSDB`.
14. Batch 6 (Sales) klar: callsites migrerade till `SSSalesContext`, repository-adaptrar uppdaterade, och deprecierade sales-metoder (tender/order/invoice/creditinvoice/periodicinvoice) fysiskt borttagna ur `SSDB`.
15. Batch 7 (Purchase) klar: callsites/tester migrerade till `SSPurchaseContext`, repository-adaptrar uppdaterade till icke-deprekerade `SSDB`-metoder, och deprecierade purchase-metoder (purchaseorder/supplierinvoice/suppliercreditinvoice) fysiskt borttagna ur `SSDB`.
16. Batch 8 (Payments) klar: callsites/tester migrerade till `SSPaymentContext`, repository-adaptrar uppdaterade, och deprecierade payments-metoder (inpayment/outpayment) fysiskt borttagna ur `SSDB`.
17. Batch 9 (Accounting Core) klar: callsites/tester migrerade till `SSAccountingContext`, repository-adaptrar uppdaterade, och deprecierade accounting core-metoder (voucher/autodist/vouchertemplate/accountplan) fysiskt borttagna ur `SSDB`.
18. Batch 10 (Company/Year) klar: callsites/tester migrerade till `SSCompanyYearContext`/repositories, V2-adaptrar uppdaterade, och deprecierade company/year-metoder fysiskt borttagna ur `SSDB`.
19. Batch 11 (Event/Trigger Sync) klar: callsites/tester migrerade till `SSEventTriggerSyncContext`, cache-sync API harmoniserad (`clearCachedLists`), och direkta testanrop till `SSDB.getInstance().clearLists()` borttagna.
20. Batch 12 (System/Config) klar: startup-lifecycle-callsites migrerade till `SSSystemConfigContext`, och system-API i `SSDB` harmoniserad med `deleteDatabaseFiles()`.

## Viktig princip
- `@Deprecated` ar primart for publika API-metoder i `SSDB`.
- Privata helper-metoder i `SSDB` markas normalt inte; de tas bort vid intern refaktorering.

## Vad som aterstar
1. Fortsatt domanvis rensning i `SSDB`:
   - migrera kvarvarande callsites (prod + test/hjalp)
   - verifiera noll callsites
   - ta bort deprecierade metoder sakert per doman
2. Hall `System/Config` och `Event/Trigger Sync` som tillaten teknisk adapteryta tills slutcutover.
3. Uppdatera plan/statusdokument efter varje batch for spårbarhet.

## Rekommenderad korordning for nasta batchar
Klar (inga fler batchar kvar i rekommenderad korordning).

## Definition of done per doman
- Inga direkta callsites till de deprecierade `SSDB`-metoderna.
- Context/repository ar enda anvandningsvag.
- Bygg + relevanta tester passerar.
- Deprekerade metoder borttagna i `SSDB` for den domanen.
- Plan/statusdokument uppdaterat.

## Praktisk start i ny trad
- Las denna fil + `SSDB_12_DOMAIN_ARCHITECTURE_PLAN.md`.
- Valj nasta doman enligt korordningen.
- Kor: callsite-sok -> migration -> verifiering -> borttagning -> dokumentuppdatering.

## Kod som kan raderas ur SSDB.java efter 12 domän-migrationer

### Säkerhetskopior: Cache-listfält (lines 126-148)
Dessa fält är endast för lokal caching av gamla API; repositories hanterar lagring:
- `List<SSProduct> iProducts` (line 126)
- `List<SSCustomer> iCustomers` (line 127)
- `List<SSSupplier> iSuppliers` (line 128)
- `List<SSAutoDist> iAutoDists` (line 129)
- `List<SSInpayment> iInpayments` (line 131)
- `List<SSTender> iTenders` (line 132)
- `List<SSOrder> iOrders` (line 133)
- `List<SSInvoice> iInvoices` (line 134)
- `List<SSCreditInvoice> iCreditInvoices` (line 135)
- `List<SSPeriodicInvoice> iPeriodicInvoices` (line 136)
- `List<SSOutpayment> iOutpayments` (line 138)
- `List<SSPurchaseOrder> iPurchaseOrders` (line 139)
- `List<SSSupplierInvoice> iSupplierInvoices` (line 140)
- `List<SSSupplierCreditInvoice> iSupplierCreditInvoices` (line 141)
- `List<SSInventory> iInventories` (line 143)
- `List<SSIndelivery> iIndeliveries` (line 144)
- `List<SSOutdelivery> iOutdeliveries` (line 145)
- `List<SSVoucher> iVouchers` (line 147)
- `List<SSOwnReport> iOwnReports` (line 148)
**Status**: Säkra att alla getter-metoder använder repositories istället (done).
**Action**: Removable när ingen fallback krävs.

### Masterdata-metoder (Batch 1) - redan borttagna
- ~~`getCustomers()`, `getCustomer()`, `addCustomer()`, `updateCustomer()`, `deleteCustomer()`~~
- ~~`getSuppliers()`, `getSupplier()`, `addSupplier()`, `updateSupplier()`, `deleteSupplier()`~~
- ~~`getProducts()`, `getProduct()`, `addProduct()`, `updateProduct()`, `deleteProduct()`~~

### Project & ResultUnit-metoder (Batch 2) - redan borttagna
- ~~`getProjects()`, `getProject()`, `addProject()`, `updateProject()`, `deleteProject()`~~
- ~~`getResultUnits()`, `getResultUnit()`, `addResultUnit()`, `updateResultUnit()`, `deleteResultUnit()`~~

### OwnReport-metoder (Batch 4) - redan borttagna
- ~~`getOwnReports()`, `getOwnReport()`, `addOwnReport()`, `updateOwnReport()`, `deleteOwnReport()`~~

### Inventory & Deliveries-metoder (Batch 5) - redan borttagna
- ~~`getInventories()`, `getInventory()`, `addInventory()`, `updateInventory()`, `deleteInventory()`~~
- ~~`getIndeliveries()`, `getIndelivery()`, `addIndelivery()`, `updateIndelivery()`, `deleteIndelivery()`~~
- ~~`getOutdeliveries()`, `getOutdelivery()`, `addOutdelivery()`, `updateOutdelivery()`, `deleteOutdelivery()`~~

### Sales-metoder (Batch 6) - redan borttagna
- ~~`getTenders()`, `getTender()`, `addTender()`, `updateTender()`, `deleteTender()`~~
- ~~`getOrders()`, `getOrder()`, `addOrder()`, `updateOrder()`, `deleteOrder()`~~
- ~~`getInvoices()`, `getInvoice()`, `addInvoice()`, `updateInvoice()`, `deleteInvoice()`~~
- ~~`getCreditInvoices()`, `getCreditInvoice()`, `addCreditInvoice()`, `updateCreditInvoice()`, `deleteCreditInvoice()`~~
- ~~`getPeriodicInvoices()`, `getPeriodicInvoice()`, `addPeriodicInvoice()`, `updatePeriodicInvoice()`, `deletePeriodicInvoice()`, `getPeriodicInvoiceRows()`~~

### Purchase-metoder (Batch 7) - redan borttagna
- ~~`getPurchaseOrders()`, `getPurchaseOrder()`, `addPurchaseOrder()`, `updatePurchaseOrder()`, `deletePurchaseOrder()`~~
- ~~`getSupplierInvoices()`, `getSupplierInvoice()`, `addSupplierInvoice()`, `updateSupplierInvoice()`, `deleteSupplierInvoice()`~~
- ~~`getSupplierCreditInvoices()`, `getSupplierCreditInvoice()`, `addSupplierCreditInvoice()`, `updateSupplierCreditInvoice()`, `deleteSupplierCreditInvoice()`~~

### Payments-metoder (Batch 8) - redan borttagna
- ~~`getInpayments()`, `getInpayment()`, `addInpayment()`, `updateInpayment()`, `deleteInpayment()`~~
- ~~`getOutpayments()`, `getOutpayment()`, `addOutpayment()`, `updateOutpayment()`, `deleteOutpayment()`~~

### Accounting Core-metoder (Batch 9) - redan borttagna
- ~~`getVouchers()`, `getVoucher()`, `addVoucher()`, `updateVoucher()`, `deleteVoucher()`, `getLastVoucherNumber()`~~
- ~~`getVoucherTemplates()`, `addVoucherTemplate()`, `deleteVoucherTemplate()`~~
- ~~`getAutoDists()`, `getAutoDist()`, `addAutoDist()`, `updateAutoDist()`, `deleteAutoDist()`~~
- ~~`getAccountPlans()`, `getAccountPlan()`, `addAccountPlan()`, `updateAccountPlan()`, `deleteAccountPlan()`, `getCurrentAccountPlan()`, `getAccounts()`~~

### Company/Year-metoder (Batch 10) - redan borttagna
- ~~`getCompanies()`, `getCompany()`, `addCompany()`, `updateCompany()`, `deleteCompany()`~~
- ~~`getYears()`, `getYearsForCompany()`, `getAccountingYear()`, `addAccountingYear()`, `updateAccountingYear()`, `deleteAccountingYear()`~~
- ~~`canOpenAccountingYear()`, `hasAccountRowsForYear()`, `openYear()`, `closeYear()`~~

### Event/Trigger Sync-metoder (Batch 11 & 12) - potentiellt borttagbar
Delegeras via `SSEventTriggerSyncContext` och `SSSystemConfigContext`. Kan bevaras för intern bruk men är inte publika API:
- `clearCachedLists()` (line 623) - delegeras, kan sparas för intern reset
- `createTriggers()` (line 13014) - delegeras via context, kan behöllas som intern
- `dropTriggers()` (line 13018) - delegeras via context, kan behållas som intern
- `triggerAction()` (line 4863) - delegeras via context, kan behållas för event-hantering

System/Config-metoder som kan behållas men kan göras private:
- `startupLocal()` (line 180) - delegeras via `SSSystemConfigContext.startupLocal()`
- `shutdown()` (line 299) - delegeras via `SSSystemConfigContext.shutdown()`
- `shutdownCompact()` (line 312) - delegeras via `SSSystemConfigContext.shutdownCompact()`
- `loadLocalDatabase()` (line 323) - delegeras via `SSSystemConfigContext.loadLocalDatabase()`
- `deleteDatabaseFiles()` (line 585) - delegeras via `SSSystemConfigContext.deleteDatabaseFiles()`

### Internal init-metoder (Batch 12) - potentiellt borttagbar eller privat
- `prepareStartupConnection()` (line 184) - private, kan sparas
- `initializeSchemaRuntime()` (line 200) - package-private, kan sparas
- `seedDemoDataIfNeeded()` (line 207) - package-private, kan sparas
- `initializeCurrentCompanyAndYear()` (line 219) - package-private, kan sparas
- `logStartupV2Mode()` (line 243) - private, kan sparas

### ~~Klasskommentar-rester~~ - åtgärdat
Klasskommentaren i `SSDB.java` är uppdaterad: den gamla migrationslistan är borttagen och
ersatt med en ren lista över alla 10 context-klasser samt Stoppregel-stycket.

### Helper-metoder som måste bevaras för intern bruk
- `setCurrentCompanyInternal()` (line 648) - priv, init-path
- `openYear()` (line ~not explicitly listed) - intern year-state
- `getCurrentCompany()` (line 671) - public, för UI/test-context
- `getCurrentYear()` (line 851) - public, för UI/test-context
- `findCurrentAccountPlan()` (line 2733) - public, används av context
- Repository-lookup metoder: `getCompanyByNameV2()`, `getAccountingYearByRangeV2()`, etc. (internal)

### Gamla SQL/schema-metoder
- `createNewTables()` - kan sparas, delegeras via `SSDBBootstrap`
- `createLocalTriggers()` (line ~12970s) - kan sparas, delegeras via context
- `checkCreateExampleCompany()` (line 354) - kan sparas för legacy

---

### Sammanfattning elimineringslista
**Redan fysiskt borttagna** (12 batches): ~200+ CRUD-metoder för alla domäner
**Potentiellt borttagbar** (med försiktighet):
1. Cache-listfälten (iProducts, iCustomers, ..., iVouchers, iOwnReports)
2. Gamla kommentarer i klassheader om borttagna metoder
3. Obsolete init-rutiner om de är helt encapsulerade i `SSDBBootstrap`

**Måste sparas**:
- Interna state-fields: `iCurrentCompany`, `iCurrentYear`, `iConnection`
- Lifecycle del-metoder: `startupLocal()`, `shutdown()`, etc. (kan göras private sedan)
- Current-getter: `getCurrentCompany()`, `getCurrentYear()`
- Internal init: `prepareStartupConnection()`, `initializeSchemaRuntime()`, etc.
- Trigger-/sync-handling för events

**Nästa steg**: Begär explicit removal av items på denna lista i framtida sessioner.
