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

## 2) Verifierad teststatus

Senast verifierat gront:

```powershell
mvn test "-Dtest=RepositoriesSupplierInvoiceCutoverTest,SSSupplierInvoiceV2IntegrationTest,SSSupplierInvoiceV2RepositoryTest,RepositoriesHDomainCutoverTest,SSDBCustomerRepositoryTest"
```

```powershell
mvn test "-Dtest=RepositoriesTenderCutoverTest,SSTenderV2IntegrationTest,SSTenderV2RepositoryTest,RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,SSDBCustomerRepositoryTest"
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
2. Valj en pilotdoman med lagst risk (kandidater: `CreditInvoice`, `PeriodicInvoice`, `Order`).
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

Fore forsta kodandring, kor:

```powershell
mvn test "-Dtest=RepositoriesHDomainCutoverTest,RepositoriesSupplierInvoiceCutoverTest,RepositoriesTenderCutoverTest,SSDBCustomerRepositoryTest"
```

