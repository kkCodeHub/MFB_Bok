# Session Resume Snapshot - 2026-05-07 (Slice P)

Detta dokument ar en fast resume-punkt for fortsatt arbete i Slice P.

## 1) Resume-lage just nu

Foljande cutover ar klara och verifierade:

- H-domainer: `AutoDist`, `VoucherTemplate`, `OwnReport`
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB` for dessa domainer.
  - `Repositories.init(SSDB)` wire:ar alltid V2-repositories for dessa domainer.
  - Doda legacy-adapters ar borttagna.
- `SupplierInvoice`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2SupplierInvoiceRepository`.
  - Legacy-adapter `SSDBSupplierInvoiceRepository` ar borttagen.
- `Tender`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2TenderRepository`.
  - Legacy-adapter `SSDBTenderRepository` ar borttagen.
- `PeriodicInvoice`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2PeriodicInvoiceRepository`.
  - Legacy-adapter `SSDBPeriodicInvoiceRepository` ar borttagen.
- `CreditInvoice`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2CreditInvoiceRepository`.
  - Legacy-adapter `SSDBCreditInvoiceRepository` ar borttagen.
- `SupplierCreditInvoice`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2SupplierCreditInvoiceRepository`.
  - Legacy-adapter `SSDBSupplierCreditInvoiceRepository` ar borttagen.
- `PurchaseOrder`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2PurchaseOrderRepository`.
  - Legacy-adapter `SSDBPurchaseOrderRepository` ar borttagen.
- `Order`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2OrderRepository`.
  - Legacy-adapter `SSDBOrderRepository` ar borttagen.
- `Indelivery`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2IndeliveryRepository`.
  - Legacy-adapter `SSDBIndeliveryRepository` ar borttagen.
- `Outdelivery`
  - Samma cutover-monster som ovan ar genomfort.
  - Aktiv V1 `OBJECT`-persistens ar avaktiverad i `SSDB`.
  - `Repositories.init(SSDB)` wire:ar alltid `V2OutdeliveryRepository`.
  - Legacy-adapter `SSDBOutdeliveryRepository` ar borttagen.

## 2) Verifierad teststatus

Senast verifierat gront:

```powershell
mvn test "-Dtest=RepositoriesSupplierInvoiceCutoverTest,SSSupplierInvoiceV2IntegrationTest,SSSupplierInvoiceV2RepositoryTest,RepositoriesHDomainCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesTenderCutoverTest,SSTenderV2IntegrationTest,SSTenderV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesPeriodicInvoiceCutoverTest,SSPeriodicInvoiceV2IntegrationTest,SSPeriodicInvoiceV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesCreditInvoiceCutoverTest,SSCreditInvoiceV2IntegrationTest,SSCreditInvoiceV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesSupplierCreditInvoiceCutoverTest,SSSupplierCreditInvoiceV2IntegrationTest,SSSupplierCreditInvoiceV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesPurchaseOrderCutoverTest,SSPurchaseOrderV2IntegrationTest,SSPurchaseOrderV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,RepositoriesSupplierCreditInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesOrderCutoverTest,SSOrderV2IntegrationTest,SSOrderV2RepositoryTest,RepositoriesPurchaseOrderCutoverTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,RepositoriesSupplierCreditInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesIndeliveryCutoverTest,SSIndeliveryV2IntegrationTest,SSIndeliveryV2RepositoryTest,RepositoriesOrderCutoverTest,RepositoriesPurchaseOrderCutoverTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,RepositoriesSupplierCreditInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesOutdeliveryCutoverTest,SSOutdeliveryV2IntegrationTest,SSOutdeliveryV2RepositoryTest,RepositoriesIndeliveryCutoverTest,RepositoriesOrderCutoverTest,RepositoriesPurchaseOrderCutoverTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,RepositoriesSupplierCreditInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=SSMasterdataV2RepositoryTest,SSAccountingCoreV2RepositoryTest,SSInvoiceV2RepositoryTest,SSOrderV2RepositoryTest,SSTenderV2RepositoryTest,SSCreditInvoiceV2RepositoryTest,SSPeriodicInvoiceV2RepositoryTest,SSSupplierInvoiceV2RepositoryTest,SSAutoDistV2RepositoryTest,SSVoucherTemplateV2IntegrationTest,SSVoucherTemplateV2RepositoryTest,SSOwnReportV2IntegrationTest,SSOwnReportV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,SSDBCustomerRepositoryTest"
```

Resultat vid senaste korning: `BUILD SUCCESS` (40 tester, 0 failures, 0 errors).

## 3) Viktiga guardrails att behalla

- En doman i taget i Slice P (lag-risk pilot).
- Stoppa direkt vid forsta fel; fortsatt inte till nasta doman.
- Behall V1-smoke for omigrerade domainer (t.ex. `SSDBCustomerRepositoryTest`).
- Cutover-test maste visa V2-wiring i bade V1- och V2-lage.

## 4) Nasta steg (rekommenderad ordning)

1. Inventera kvarvarande migrerade domainer med aktiv V1-vag.
2. Valj en pilotdoman med lagst risk (kandidater: `Invoice`, `Inpayment`).
3. Genomfor samma monster:
   - avaktivera aktiv V1-vag i `SSDB`,
   - lasa V2-wiring i `Repositories`,
   - ta bort dod legacy-adapter,
   - lagg till/uppdatera cutover-test.
4. Kor fokuserade tester, sedan bred regression.
5. Uppdatera `CHANGELOG.md` och `doc/migration/SESSION_RESUME_CHECKLIST.md`.

## 5) Snabbstart i nasta session

Las i denna ordning:

1. `doc/migration/SESSION_RESUME_2026-05-07_SLICE_P.md`
2. `doc/migration/SESSION_RESUME_CHECKLIST.md`
3. `src/main/java/se/swedsoft/bookkeeping/persistence/Repositories.java`
4. `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`
5. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesHDomainCutoverTest.java`
6. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesSupplierInvoiceCutoverTest.java`
7. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesTenderCutoverTest.java`
8. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesPeriodicInvoiceCutoverTest.java`
9. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesCreditInvoiceCutoverTest.java`
10. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesSupplierCreditInvoiceCutoverTest.java`
11. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesPurchaseOrderCutoverTest.java`
12. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesOrderCutoverTest.java`
13. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesIndeliveryCutoverTest.java`
14. `src/test/java/se/swedsoft/bookkeeping/persistence/RepositoriesOutdeliveryCutoverTest.java`

Fore forsta kodandring, kor:

```powershell
mvn test "-Dtest=RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,RepositoriesPeriodicInvoiceCutoverTest,RepositoriesCreditInvoiceCutoverTest,RepositoriesSupplierCreditInvoiceCutoverTest,RepositoriesPurchaseOrderCutoverTest,RepositoriesOrderCutoverTest,RepositoriesIndeliveryCutoverTest,RepositoriesOutdeliveryCutoverTest,SSDBCustomerRepositoryTest"
```

