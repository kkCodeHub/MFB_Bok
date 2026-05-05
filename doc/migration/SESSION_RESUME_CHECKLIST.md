# Session Resume Checklist (Steg 2)

Använd denna checklista när arbetet ska återupptas i en ny session.

## Planlåsning (godkänd 2026-05-05)

Denna plan är låst tills nytt explicit godkännande ges.

- Scope: Slice N, O, O.5, P.
- Ordning: N -> O -> O.5 -> P.
- Slice N: Försäljning/Inköp (D/E) - V2-repositories + relevanta tester passerar.
- Slice O: Betalning/Lager/Misc (F/G/H) - V2-repositories + relevanta tester passerar.
- Slice O.5: Bokföring (C) - `accountplan`, `accountingyear`, `voucher` i V2 + relevanta tester passerar.
- Slice P: Cutover/städning - inga aktiva `OBJECT`-paths i persistenslagret (V2-only).

Bindande guardrails:

- Ingen scope-utökning mitt i pågående slice.
- Ingen API-signaturändring utan nytt explicit godkännande.
- Nästa slice får starta först när föregående slice uppfyller sin DoD.
- Avvikelser dokumenteras i `doc/migration/STEP2_3_EXECUTION_PLAN.md` och kräver nytt godkännande.

## 1) Snabb läsordning

**För Session L (nuvarande sessionstart):**

1. `doc/migration/SESSION_L_ACCOUNTING_REPOSITORY.md` — detaljplan för sessionen
2. `doc/migration/STEP2_3_EXECUTION_PLAN.md` — övergripande arbetsordning (§3 är näst)
3. `OBJECT_COLUMN_MAPPING.md` — §C (Kategori C – Bokföring) för fältmappning
4. `src/main/java/se/swedsoft/bookkeeping/persistence/Repositories.java` — aktuell factory
5. `src/main/java/se/swedsoft/bookkeeping/persistence/v2/V2CustomerRepository.java` — mall för V2-impl
6. `src/main/java/se/swedsoft/bookkeeping/data/SSVoucher.java` — fältstruktur
7. `src/main/java/se/swedsoft/bookkeeping/data/SSAccountPlan.java` — fältstruktur
8. `src/main/java/se/swedsoft/bookkeeping/data/SSAccountingYear.java` — fältstruktur

**Allmän bakgrundsdokumentation:**

- `doc/migration/STEP2_STATUS_2026-05-04.md`
- `src/main/resources/sql/create_tables_v2.sql`

## 2) Snabb verifiering i terminal

```powershell
cd "E:\FB_update\fribok-master3\fribok-master"
git --no-pager log --oneline -8
mvn clean test "-Dtest=SSMasterdataV2RepositoryTest" "-Dfribok.schema.version=v2"
```

Förväntat resultat:
- `SSMasterdataV2RepositoryTest` passerar (3 CRUD-flows: kund, produkt, leverantör).
- Senaste commit(s) inkluderar Session K-arbetet (V2CustomerRepository, V2ProductRepository, V2SupplierRepository).

**Senast verifierad baseline före Session L:**
- Commit: `474a715` (`feat(sessions-E-to-K): add tender/credit-invoice/periodic-invoice CRUD, repository layer and integration tests`)

## 3) Starta Session L (första arbetsuppgift)

- Implementera `AccountPlanRepository` interface i `se.swedsoft.bookkeeping.persistence`.
- Lägg legacy-adapter `SSDBAccountPlanRepository` i `persistence.legacy`.
- Lägg V2-implementering `V2AccountPlanRepository` i `persistence.v2`.
- Uppdatera `Repositories.init()` med V2-grenen för AccountPlan.
- Följ sedan samma mönster för `VoucherRepository` och `AccountingYearRepository`.
- Se `doc/migration/SESSION_L_ACCOUNTING_REPOSITORY.md` för fullständig uppgiftslista.

## 4) Klartecken innan commit

- Berörda tester passerar lokalt.
- `mvn clean test` utan regressionsfel.
- Dokumentationen uppdaterad med:
  - vad som ändrats
  - vilka tabeller/symboler som migrerats
  - vilken del av Steg 2.3 som är klar

**Definition of Done för Session L (måste uppfyllas):**
- `AccountPlanRepository`, `VoucherRepository`, `AccountingYearRepository` finns (interface + legacy + V2).
- `Repositories.init()` wire:ar rätt implementation för både V1 och V2.
- `AccountingYearRepository` i L omfattar endast `findAll`, `findCurrent`, `add`, `update`, `delete`.
- Budget/in-balance (`tbl_budget_row`, `tbl_year_balance`) är explicit markerat som Session M.
- Följande tester passerar i V2-läge:
  - `SSAccountPlanV2RepositoryTest`
  - `SSVoucherV2RepositoryTest`
  - `SSAccountingYearV2RepositoryTest`
  - `SSAccountingCoreV2RepositoryTest`
- `CHANGELOG.md` uppdaterad under `[Unreleased]`.

## 5) Uppdatera denna fil efter varje delsteg

Lägg till en kort changelog-rad med datum, commit och status, t.ex.:

- `2026-05-04`: Session A klar - schema wiring i `SSDB.createNewTables()` med `fribok.schema.version` (`v2` aktiverar `create_tables_v2.sql`).
- `2026-05-04`: Session B klar - kund-CRUD i `SSDB` migrerad till V2-tabell `tbl_customer` när `fribok.schema.version=v2`; ny testklass `SSCustomerV2IntegrationTest` passerar.
- `2026-05-04`: Session C klar - produkt- och leverantörs-CRUD i `SSDB` migrerad till V2-tabeller `tbl_product`/`tbl_supplier`; nya testklasser `SSProductV2IntegrationTest` och `SSSupplierV2IntegrationTest` passerar.
- `2026-05-04`: Session D klar - minimal V2-kärnslice för `tbl_accountingyear`, `tbl_voucher` och `tbl_voucher_row`; ny testklass `SSVoucherV2IntegrationTest` passerar.
- `2026-05-04`: Session E klar - minimal V2-kärnslice för `tbl_invoice` och `tbl_invoice_row`; ny testklass `SSInvoiceV2IntegrationTest` passerar.
- `2026-05-04`: Session F klar - minimal V2-kärnslice för `tbl_order` och `tbl_order_row`; ny testklass `SSOrderV2IntegrationTest` passerar.
- `2026-05-04`: Session G klar - minimal V2-kärnslice för `tbl_tender` och `tbl_tender_row`; ny testklass `SSTenderV2IntegrationTest` passerar.
- `2026-05-05`: Session H klar - minimal V2-kärnslice för `tbl_creditinvoice` och `tbl_creditinvoice_row`; ny testklass `SSCreditInvoiceV2IntegrationTest` passerar.
- `2026-05-05`: Session I klar - minimal V2-kärnslice för `tbl_periodicinvoice` och `tbl_periodicinvoice_row`; ny testklass `SSPeriodicInvoiceV2IntegrationTest` passerar.
- `2026-05-05`: Session J klar - repository interfaces + legacy adapters + `Repositories` factory etablerad för masterdata.
- `2026-05-05`: Session K klar - `persistence.v2` implementationer (`V2CustomerRepository`, `V2ProductRepository`, `V2SupplierRepository`) inkopplade via `Repositories.init()` när `fribok.schema.version=v2`; `SSMasterdataV2RepositoryTest` passerar.
- `2026-05-05`: Session L kickoff klar - scope/API/DoD låsta: AccountingYear-budget/in-balance flyttat till Session M; voucher-add använder year från voucher-objekt; baseline verifierad på commit `474a715`.
- `2026-05-05`: Session L klar - `AccountPlanRepository`, `VoucherRepository`, `AccountingYearRepository` (interface + legacy + V2) inkopplade i `Repositories`; V2-stöd tillagt i `SSDB` för account-plan mapping och year-baserad voucher lookup; tester `SSAccountPlanV2RepositoryTest`, `SSVoucherV2RepositoryTest`, `SSAccountingYearV2RepositoryTest`, `SSAccountingCoreV2RepositoryTest` passerar.
- `2026-05-05`: Session M steg 1 klar - `SSDB` persisterar och laddar `tbl_year_balance` + `tbl_budget_row` för V2 accounting year; berörda repositorytester passerar.
- `2026-05-05`: Session M steg 2 klar - null-normalisering i `SSNewAccountingYear` för budget/in-balance (`setBudget(null)` och `setInBalance(null)`) med verifierande integrationstester.
- `2026-05-05`: Session M steg 3 klar - budgetvärden bevaras vid ändrade year-boundaries via `SSBudget#setYear(...)`; nytt test `updateAccountingYearBoundaryPreservesBudgetRows` i `SSAccountingYearV2RepositoryTest` verifierar bevarade månadsrader i `tbl_budget_row`.
- `2026-05-05`: Planlåsning uppdaterad - exekveringsordning låst till N (D/E) -> O (F/G/H) -> O.5 (C bokföring) -> P (V2-only cutover), med bindande guardrails.
- `2026-05-05`: Slice N steg (supplier invoice) klar - `SSDB` V2-CRUD för `tbl_supplierinvoice`/`tbl_supplierinvoice_row` samt repositorylager (`SupplierInvoiceRepository`, `SSDBSupplierInvoiceRepository`, `V2SupplierInvoiceRepository`) inkopplat i `Repositories`; tester `SSSupplierInvoiceV2IntegrationTest` och `SSSupplierInvoiceV2RepositoryTest` passerar.
