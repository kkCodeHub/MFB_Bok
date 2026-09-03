Inventering av Globalt Tillstånd och Cache-Livscykel i SSDB
Resultatet av Delsteg 1 för Delmoment 5 är följande:
GLOBALT TILLSTÅND (Global State)
Session/Kontext-Tillstånd
iCurrentCompany (typ: SSNewCompany) – Aktuellt valt bolag
Sätts via: setCurrentCompany() eller setCurrentCompanyInternal()
Initialiseras i: prepareStartupConnection(), initializeCurrentCompanyAndYear()
Invalideras när: bolag byter
iCurrentYear (typ: SSNewAccountingYear) – Aktuellt valt räkenskapsår
Sätts via: setCurrentYear(), applyOpenedYearFromRepository()
Initialiseras i: initializeCurrentCompanyAndYear(), openYear()
Invalideras när: år byter, år öppnas
iConnection (typ: Connection) – Databasanslutning
Sätts i: prepareStartupConnection(), loadLocalDatabase()
Livslängd: hela applikationens session
Event/Trigger-Tillstånd
iEventBus (typ: SSDBEventBus) – Property-change eventbus
Ansvar: publicerar "COMPANY" och "YEAR"-events till lyssnare
Initialiseras: på SSDB-instansiering
Events:
"COMPANY" – vidarebefordras när setCurrentCompanyInternal() anropas
"YEAR" – vidarebefordras när setCurrentYear() eller applyOpenedYearFromRepository() anropas
"IO" – reserverat för I/O-events
iBypassTriggerDispatcher (typ: ThreadLocal<Boolean>) – Bypass-flagga för trigger-hantering
Standardvärde: FALSE
Syfte: möjliggöra att hoppa över dispatcher vid testning/speciella fall
iEventTriggerDispatcher (typ: SSEventTriggerDispatcher) – Trigger-dispatcher
Initialiseras: på SSDB-instansiering
Dispatcher-hanterare (7 stycken):
MasterdataTriggerHandler (projekt, resultatenhet, vouchermallar)
SalesTriggerHandler (anbud, order, fakturor, kreditfakturor, periodiska fakturor)
CustomerPaymentTriggerHandler (kundbetalningar)
PurchaseSupplierTriggerHandler (inköpsorder, leverantörkreditfakturor, leverantörbetalningar)
InventoryTriggerHandler (inventarier, in-/utleveranser)
AccountingTriggerHandler (verifikationer)
ReportTriggerHandler (egna rapporter)

CACHE LISTS (Entity Caches)
18 cache-listor, alla initialiseras till null och fylls on-demand:
Masterdata Cache:
iProducts (typ: List<SSProduct>)
iCustomers (typ: List<SSCustomer>)
iSuppliers (typ: List<SSSupplier>)
iAutoDists (typ: List<SSAutoDist>)
Sales/Inbound Cache:
iInpayments (typ: List<SSInpayment>)
iTenders (typ: List<SSTender>)
iOrders (typ: List<SSOrder>)
iInvoices (typ: List<SSInvoice>)
iCreditInvoices (typ: List<SSCreditInvoice>)
iPeriodicInvoices (typ: List<SSPeriodicInvoice>)
Purchase/Outbound Cache:
iOutpayments (typ: List<SSOutpayment>)
iPurchaseOrders (typ: List<SSPurchaseOrder>)
iSupplierInvoices (typ: List<SSSupplierInvoice>)
iSupplierCreditInvoices (typ: List<SSSupplierCreditInvoice>)
Inventory Cache:
iInventories (typ: List<SSInventory>)
iIndeliveries (typ: List<SSIndelivery>)
iOutdeliveries (typ: List<SSOutdelivery>)
Accounting/Report Cache:
iVouchers (typ: List<SSVoucher>) – Är year-specifik
iOwnReports (typ: List<SSOwnReport>)

CACHE-LIVSCYKEL (Cache Lifecycle)
Initialisering (Initialization)
På startup: prepareStartupConnection() → nollställer all state
Efter anslutning: init(Boolean showDialog) → laddar alla masterdata och sales-/purchase-cacher
Year-spesifik: initYear(Boolean showLoadingDialog) → laddar vochers-cache
Invalidation-mekanismer
Full cache-invalidering:
clearCachedLists() – sätter ALLA 18 listor till null
Anropas från:
prepareStartupConnection() (vid ny databasanslutning)
setCurrentCompanyInternal() (vid byte av bolag)
SSEventTriggerSyncContext.clearCachedLists() (extern facade)
Bolag-byte – partial invalidering:
setCurrentCompanyInternal() nollställer:
iCurrentCompany (uppdateras via repo)
ALLA masterdata och transaktionscacher (17 listor)
Meddelar alla lyssnare via iEventBus för property "COMPANY"
År-byte – year-spesifik invalidering:
setCurrentYear() / applyOpenedYearFromRepository() nollställer:
iCurrentYear (uppdateras)
ONLY iVouchers (year-specifik)
Meddelar alla lyssnare via iEventBus för property "YEAR"
Lazy Loading-strategi
Varje load*() metod följer pattern:
if (cache != null) return cache;
cache = new LinkedList<>(Repositories.*.findAll());
return cache;
On-demand: cachen fylls först när den efterfrågas
Trigger-baserad Uppdatering
Triggers från databasen anropar triggerAction() → iEventTriggerDispatcher.dispatch(...)
Dispatcher delegerar till lämplig *TriggerHandler
Handler uppdaterar relevant cache-lista in-place (add/update/remove-operation)
Sedan uppdateras UI-ramarna via *.getInstance().updateFrame()

DYNAMISKA CACHER (External Static Caches)
Utanför SSDB finns ytterligare externala statiska cacher som är beroende av SSDB-tillståndet:
SSInvoiceMath.iSaldoMap – saldo-beräkningar för fakturor
SSCustomerMath.iInvoicesForCustomers – mappning kund → fakturor
SSSupplierInvoiceMath.iSaldoMap – saldo-beräkningar för leverantörsfakturor
SSSupplierMath.iInvoicesForSuppliers – mappning leverantör → fakturor
Dessa återställs i init() via:
