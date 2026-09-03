# Plan för ren slutarkitektur: 12 domäner

## 1. Målbild (slutarkitektur: 12 domäner)

1. **Company/Year**  
2. **Accounting Core**  
3. **Masterdata**  
4. **Sales**  
5. **Purchase**  
6. **Payments**  
7. **Product**  
8. **OwnReport**  
9. **Project & ResultUnit**  
10. **Inventory & Deliveries**  
11. **Event/Trigger Sync**  
12. **System/Config**

**Princip:** Ingen ny affärslogik i `SSDB`. `SSDB` används endast som övergångsadapter tills varje domän är fysiskt flyttad.

---

## 2. Hur de 8 tidigare domänerna hanteras

Nuvarande 8 domäner används som **övergångscontainrar** och mappas till 12 enligt nedan:

- `SSDBLifecycleContext` → delas upp i:
  - **System/Config**
  - **Event/Trigger Sync**
- `SSCompanyYearContext` → **Company/Year**
- `SSMasterdataContext` → **Masterdata** + delar till **Project & ResultUnit**
- `SSProductOwnReportContext` → delas i:
  - **Product**
  - **OwnReport**
- `SSSalesContext` → **Sales** (ev. gränssnitt mot Inventory/Deliveries)
- `SSPurchaseContext` → **Purchase**
- `SSAccountingContext` / `SSDBAccountingContext` → **Accounting Core**
- `SSPaymentContext` → **Payments**

---

## 3. Skal-domäner (kortlivade)

Skapas tidigt som tunna kontrakt för att låsa slutarkitekturen direkt:

- **OwnReport** (separat från Product från dag 1)
- **Event/Trigger Sync**
- **System/Config**
- **Project & ResultUnit**
- **Inventory & Deliveries**

Dessa får först tunna API:er och delegerar till befintlig implementation, men fylls snabbt med fysisk kodflytt i nästa steg.

---

## 4. Genomförandeordning (minst problem först)

1. **Definiera 12-domäners API-kontrakt och ägarskap**  
   - Tydlig ägare per domän, inga överlapp.

2. **Bryt ut tekniska domäner först**  
   - `SSSystemConfigContext` etablerad för startup/shutdown/db-init/schema-relaterad livscykel.
   - `SSEventTriggerSyncContext` etablerad för trigger-hantering och cache/UI-sync.
   - `SSDBLifecycleContext` kvar som `@Deprecated` bakåtkompatibilitetslager som endast delegerar till de två nya domänerna.
   - Inga aktiva callsites kvar mot `SSDBLifecycleContext` i produktionskod.

3. ✅ **Dela Product/OwnReport tidigt**  
   - `SSProductContext` delegerar fullt till `Repositories.products()` — full CRUD + Javadoc.
   - `SSOwnReportContext` delegerar fullt till `Repositories.ownReports()` — full CRUD + Javadoc.
   - `SSOwnReportContext` utökad med `getOwnReports(List)` och `getOwnReport(Integer)`.
   - `OwnReportRepository` utökad med `findByNumber(Integer)`; implementerat i `V2OwnReportRepository`.
   - `SSProductOwnReportContext` är `@Deprecated` utan aktiva callsites; kvarstår som bakåtkompatibilitetsbrygga.
   - BUILD SUCCESS ✅

4. ✅ **Flytta stöddata**  
   - `ProjectRepository` interface + `V2ProjectRepository` skapade.
   - `ResultUnitRepository` interface + `V2ResultUnitRepository` skapade.
   - `SSProjectContext` och `SSResultUnitContext` migrerade: all CRUD via `Repositories.projects()` / `Repositories.resultUnits()`.
   - `Repositories` utökad med `projects()` och `resultUnits()` getters.
   - Inga `SSDBAccess.database().*`-anrop kvar i Project/ResultUnit-domänen.
   - BUILD SUCCESS ✅

5. ✅ **Etablera Inventory & Deliveries som egen domän**  
   - `SSInventoryDeliveriesContext` skapad med CRUD för Inventory, Indelivery och Outdelivery.
   - Delegerar till `Repositories.inventories()`, `indeliveries()`, `outdeliveries()`.

6. ✅ **Stabilisera kärndomäner**  
   - Sales, Purchase, Payments, Accounting Core, Company/Year.
   - Explicit imports, full Javadoc, komplett CRUD i alla fem context-klasser.
   - `SSSalesContext`: lagt till Tender CRUD (offert/säljflöde).
   - `SSPurchaseContext`: lagt till `getSupplierCreditInvoices(List)`.
   - `SSPaymentContext`: utökad med full CRUD för inpayments och outpayments.
   - `SSAccountingContext`: `getCurrentYear()` via repository; kvarvarande SSDB-anrop märkta med TODO.
   - `SSCompanyYearContext`: `openYear/closeYear` och `getYears()` via repository.

7. ✅ **Fysisk rensning i SSDB per domän**  
   - Domän-CRUD/query-metoder i `SSDB` markeras löpande `@Deprecated` per batch (inkl. Sales, Purchase, Payments, Product, OwnReport, Inventory/Deliveries, Masterdata, Company/Year och Accounting Core).
   - Ingen ny anropare utanför SSDB.java → säkert att ta bort i nästa batch när alla callsites migrerats.
   - BOM-problematik löst: filen sparad UTF-8 utan BOM.
   - BUILD SUCCESS ✅

8. ✅ **Slutsteg**  
   - `SSDB` reducerad till tunn adapter-yta för init/lifecycle och intern kompatibilitet.
   - `SSDBUiInitializer` har fått context-baserade API:er (`init(boolean)`, `initYear(boolean)`).
   - UI/import-callsites migrerade bort från `SSCompanyYearContext.getDatabase()` / `SSAccountingContext.getDatabase()`.
   - Kvarvarande `SSDB`-exponering är centraliserad till adapterlager (`SSDBUiInitializer`, `Repositories.ensureInitialized`).
   - `SSCompanyYearContext` CRUD (company + year) migrerat till `Repositories.companies()` / `Repositories.accountingYears()`.
   - `CompanyRepository` interface + `V2CompanyRepository` implementation skapade.
   - `AccountingYearRepository` utökad med `findForCompany`, `findById`, `findPrevious`, `findLast`.
   - Session-state, `canOpen`, event-bus kvar i SSDB per §5 (tillåtet).
   - Första fysiska rensningsbatch genomförd: borttagna deprecierade kompatibilitetsmetoder i voucher-domänen (`getVoucher(year,number)`, `getVouchers(List)`, `getVoucherTemplates(List)`) efter callsite-migrering.
   - BUILD SUCCESS ✅

---

## 5. Hantering av kvarvarande kod i SSDB under övergång

Tillåtet temporärt i `SSDB`:
- teknisk bootstrap-adapter
- anslutnings- och schemahookar som ännu inte flyttats

Ej tillåtet:
- ny affärslogik
- nya domän-CRUD-metoder
- nya cross-domain-switchar

När en domän är flyttad:
- motsvarande `SSDB`-metoder markeras för borttagning och rensas i samma eller nästa batch.

---

## 6. Kvalitetsgrindar per batch

- Inga nya direkta singleton- eller facade-callsites.
- Full build/test grön mellan varje domänbatch.
- Gränstester skärps löpande (tak för kvarvarande SSDB-ytor sänks).
- Triggerflöden verifieras särskilt när Event/Trigger Sync bryts ut.
- UI- och import/export-flöden verifieras vid varje domän med hög integration.

---

## 7. Definition av klart

- Alla 12 domäner är aktiva med egen fysisk implementation.
- `OwnReport` är helt separerad från Product.
- Lifecycle-ansvar ligger uttryckligen i **System/Config** + **Event/Trigger Sync**.
- `SSDB` är antingen:
  - borttagen, eller
  - strikt minimal teknisk adapter utan affärslogik.

---

## 8. API-kontrakt och ägarskap (genomfört steg 1)

### 8.1 Tvingande kontraktsregler (inga överlapp)

1. **En skrivägare per dataobjekt**  
   - Endast en domän får skapa/uppdatera/radera sitt objekt.
2. **Läsning via query-API, aldrig via främmande repository**  
   - Domän A får inte läsa Domän B:s tabeller/DAO direkt.
3. **Cross-domain via kontrakt**  
   - Synk/asynk sker via definierade tjänstegränssnitt eller events, inte via `SSDB`-genvägar.
4. **Inga nya delade DTO:er som blandar ansvar**  
   - DTO:er versioneras per domänkontrakt.
5. **Konfliktregel**  
   - Vid oklar ägare gäller: "närmast affärsbeslutet äger skrivning".

### 8.2 Domänkontrakt: ägare, write-scope, API-yta, explicit exkludering

| Domän | Ägare | Äger (write-scope) | Publicerar API-kontrakt | Får inte äga |
|---|---|---|---|---|
| Company/Year | Company/Year-ansvarig | företag, räkenskapsår, årstatus, årspecifika metadata | `CompanyYearCommandApi`, `CompanyYearQueryApi`, events: `FiscalYearOpened/Closed` | kontoplantransaktioner, verifikat, betalningsflöden |
| Accounting Core | Bokföringsansvarig | verifikat, kontering, bokföringsregler, saldobildning | `AccountingCommandApi`, `AccountingQueryApi`, events: `VoucherPosted` | kund/leverantörsregister, order, produktkatalog |
| Masterdata | Masterdata-ansvarig | kund, leverantör, basregister (ej Project/ResultUnit) | `MasterdataCommandApi`, `MasterdataQueryApi`, events: `CustomerChanged`,`SupplierChanged` | projekt/resultatenheter, lagertransaktioner |
| Sales | Sales-ansvarig | offert/order/faktura/kreditfaktura (säljflöde) | `SalesCommandApi`, `SalesQueryApi`, events: `SalesDocumentBooked` | inköpsflöde, leverantörsbetalning |
| Purchase | Purchase-ansvarig | förfrågan/inköpsorder/leverantörsfaktura (inköpsflöde) | `PurchaseCommandApi`, `PurchaseQueryApi`, events: `PurchaseDocumentBooked` | säljfakturering, kundreskontra |
| Payments | Payments-ansvarig | kund- och leverantörsbetalningar, avprickning, betalstatus | `PaymentCommandApi`, `PaymentQueryApi`, events: `PaymentRegistered`,`PaymentMatched` | verifikatskapande utanför bokförings-API |
| Product | Product-ansvarig | artiklar, prislistor, enhet/produktattribut | `ProductCommandApi`, `ProductQueryApi`, events: `ProductChanged` | rapportdefinitioner (OwnReport) |
| OwnReport | Reporting-ansvarig | egna rapportmallar, rapportdefinitioner, rapportparametrar | `OwnReportCommandApi`, `OwnReportQueryApi`, events: `ReportDefinitionChanged` | produktregister |
| Project & ResultUnit | Projektansvarig | projekt, resultatenheter, kopplingsregler | `ProjectResultUnitCommandApi`, `ProjectResultUnitQueryApi`, events: `ProjectChanged`,`ResultUnitChanged` | kund/leverantörs-masterdata |
| Inventory & Deliveries | Lager/Logistik-ansvarig | lagerhändelser, in/utleverans, lagersaldologik | `InventoryCommandApi`, `InventoryQueryApi`, events: `StockAdjusted`,`DeliveryRegistered` | orderägarskap (Sales/Purchase) |
| Event/Trigger Sync | Integrationsansvarig | triggerdefinitioner, synkpolicy, eventrouting | `EventSyncCommandApi`, `EventSyncQueryApi`, events: `TriggerExecuted`,`SyncFailed` | affärsobjektens primärdata |
| System/Config | Plattformsansvarig | startup/shutdown, db-init, schema/config, teknisk bootstrap | `SystemConfigCommandApi`, `SystemConfigQueryApi`, events: `SystemConfigured` | affärslogik och domän-CRUD |

### 8.3 Överlappsfria gränser mellan kritiska domänpar

- **Product vs OwnReport**: Product äger artikeldata; OwnReport äger endast rapportdefinitioner.
- **Masterdata vs Project & ResultUnit**: Masterdata äger kund/leverantör; Project/ResultUnit äger projektstrukturer.
- **Sales/Purchase vs Inventory & Deliveries**: Sales/Purchase äger dokumentlivscykel; Inventory äger lagerrörelser.
- **Accounting Core vs Payments**: Payments registrerar betalning; Accounting Core äger bokföringspostning.
- **System/Config + Event/Trigger Sync vs övriga**: endast tekniskt ansvar, ingen affärsdata.

### 8.4 Definition of Done för steg 1

- Samtliga nya use-cases är mappade till exakt en `*CommandApi`-ägare.
- Samtliga läsbehov mellan domäner går via `*QueryApi` eller events.
- Inga nya metoder i `SSDB` som kringgår tabellen ovan.
