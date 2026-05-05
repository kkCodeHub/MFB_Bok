# Session L — Accounting-Core Repository Layer (Steg 2.3 Phase 3)

**Status:** READY TO START  
**Date:** 2026-05-05  
**Estimated Effort:** 1–2 sessions (L, possibly M)  
**Risk Level:** MEDIUM-HIGH — vouchers depend on accounting-year and account-plan FK chains  

---

## Låsta beslut före kodstart (bindande för Session L)

1. **Scope för L är låst:** `AccountingYearRepository` i Session L omfattar endast
   `findAll`, `findCurrent`, `add`, `update`, `delete`.
   Mapping av `tbl_year_balance` och `tbl_budget_row` flyttas uttryckligen till Session M.
2. **Voucher-API är låst:** `VoucherRepository.add(SSVoucher voucher)` behålls i L.
   Accounting-year hämtas från voucher-objektet (inte via implicit "current year").
   Om year saknas i objektet ska implementationen faila tidigt med tydligt fel.
3. **Implementationsstil i L är låst:** V2-repositories delegerar till befintliga
   `SSDB`-metoder i detta steg (samma mönster som Session K). Ren JDBC/SQL-rewrite
   hanteras i senare refaktorsteg.
4. **Testnivå för L är låst:** Före commit krävs minst:
   - `SSAccountPlanV2RepositoryTest`
   - `SSVoucherV2RepositoryTest`
   - `SSAccountingYearV2RepositoryTest`
   - `SSAccountingCoreV2RepositoryTest`
   - `mvn clean test`

---

## Sammanhang / Background

Sessions A–K etablerade:

| Session | Resultat |
|---------|---------|
| A | V2-schema aktiveras i `SSDB.createNewTables()` via `-Dfribok.schema.version=v2` |
| B | Customer CRUD i `SSDB` migrerat till `tbl_customer` |
| C | Product + Supplier CRUD i `SSDB` migrerat till `tbl_product` / `tbl_supplier` |
| D | AccountingYear + Voucher CRUD-slice i `SSDB` mot `tbl_accountingyear` / `tbl_voucher` |
| E–I | Invoice, Order, Tender, CreditInvoice, PeriodicInvoice CRUD-slices i `SSDB` |
| J | `CustomerRepository`, `ProductRepository`, `SupplierRepository` interfaces + legacy adapters + `Repositories`-factory |
| K | `V2CustomerRepository`, `V2ProductRepository`, `V2SupplierRepository` inkopplade i `Repositories.init()` när `fribok.schema.version=v2`; `SSMasterdataV2RepositoryTest` passerar |

**Vad som saknas för Session L:**

- Masterdata-repositories är klara, men **ingen** repository finns ännu för bokföringskärnan.
- `SSDB` innehåller fortfarande direkta Voucher/AccountingYear/AccountPlan-metoder utan
  repository-abstraktion.
- Nästa steg i `STEP2_3_EXECUTION_PLAN.md` §3 är: "Migrera bokföringskärnan".

---

## Mål för Session L

Implementera repository-gränssnittet för bokföringskärnan på samma sätt som J/K
gjorde för masterdata — tre interface + legacy-adapter + V2-implementering per entitet,
inkopplat i `Repositories`-factory.

### Prioriterat scope

| Entitet | Interface | Legacy-adapter | V2-impl | V2-schema-tabell |
|---------|-----------|---------------|---------|-----------------|
| AccountPlan | `AccountPlanRepository` | `SSDBAccountPlanRepository` | `V2AccountPlanRepository` | `tbl_accountplan` + `tbl_account` |
| Voucher | `VoucherRepository` | `SSDBVoucherRepository` | `V2VoucherRepository` | `tbl_voucher` + `tbl_voucher_row` |
| AccountingYear | `AccountingYearRepository` | `SSDBAccountingYearRepository` | `V2AccountingYearRepository` | `tbl_accountingyear` |

> **AccountingYear** är den mest komplexa (se OBJECT_COLUMN_MAPPING.md §C) och kan
> delas upp i ett eget LM-delsteg om den tar för lång tid i Session L.

---

## Task L.1 — `AccountPlanRepository`

### Syfte
Separera kontoplansläsning/-skrivning från `SSDB` God-objektet.

### Interface — `AccountPlanRepository.java`
Placeras i `se.swedsoft.bookkeeping.persistence`.

```java
public interface AccountPlanRepository {

    /** Returns all account plans available for the current company. */
    List<SSAccountPlan> findAll();

    /** Looks up a plan by its internal id. */
    Optional<SSAccountPlan> findById(int id);

    /** Persists a new account plan. */
    void add(SSAccountPlan plan);

    /** Updates an existing account plan. */
    void update(SSAccountPlan plan);

    /** Deletes an account plan. */
    void delete(SSAccountPlan plan);
}
```

### Legacy-adapter — `SSDBAccountPlanRepository.java`
Placeras i `se.swedsoft.bookkeeping.persistence.legacy`.
Delegerar till befintliga `SSDB.getAccountPlans()`, `SSDB.addAccountPlan()`, etc.

### V2-implementering — `V2AccountPlanRepository.java`
Placeras i `se.swedsoft.bookkeeping.persistence.v2`.
Delegerar (låst beslut för L) till `SSDB` precis som `V2CustomerRepository` gör.
Ren JDBC SQL-mappning är ett separat refaktoreringssteg.

### Berörda `SSDB`-metoder att identifiera
Sök i `SSDB.java` efter metoder som hanterar `SSAccountPlan` — troligen:
- `getAccountPlans()`
- `getAccountPlan(int id)`  *(kan behöva läggas till om den saknas)*
- `addAccountPlan(SSAccountPlan)`
- `updateAccountPlan(SSAccountPlan)`
- `deleteAccountPlan(SSAccountPlan)`

### Test
- `src/test/java/se/swedsoft/bookkeeping/persistence/SSAccountPlanV2RepositoryTest.java`
- CRUD-flöde: lägg till plan → hämta → uppdatera → ta bort.

---

## Task L.2 — `VoucherRepository`

### Syfte
Separera verifikationsläsning/-skrivning från `SSDB`.

### Interface — `VoucherRepository.java`

```java
public interface VoucherRepository {

    /** Returns all vouchers for the given accounting year. */
    List<SSVoucher> findByYear(SSAccountingYear year);

    /** Looks up a voucher by its integer number within the year. */
    Optional<SSVoucher> findByNumber(SSAccountingYear year, int number);

    /** Persists a new voucher. */
    void add(SSVoucher voucher);

    /** Updates an existing voucher. */
    void update(SSVoucher voucher);

    /** Deletes a voucher. */
    void delete(SSVoucher voucher);
}
```

### Legacy-adapter — `SSDBVoucherRepository.java`
Delegerar till befintliga `SSDB.getVouchers(year)`, `SSDB.addVoucher()`, etc.

### V2-implementering — `V2VoucherRepository.java`
Delegerar till SSDB precis som övriga V2-repon gör.
Säkerställer att `fribok.schema.version=v2` är satt vid instansiering.

**Låst regel för Session L:** `add(SSVoucher voucher)` använder year-referens
från voucher-objektet. Ingen fallback till implicit aktivt år.

### Berörda `SSDB`-metoder
- `getVouchers(SSAccountingYear)`
- `getVoucher(SSAccountingYear, int)`
- `addVoucher(SSVoucher)`
- `updateVoucher(SSVoucher)`
- `deleteVoucher(SSVoucher)`

### Test
- `src/test/java/se/swedsoft/bookkeeping/persistence/SSVoucherV2RepositoryTest.java`
- Kräver att en `SSAccountingYear` skapas i testet (FK-beroende).

---

## Task L.3 — `AccountingYearRepository`

> ⚠️ **Komplex** — `SSAccountingYear` innehåller `SSAccountPlan`, in-balance-map och
> budget. Scope i Session L är låst till: `findAll`, `findCurrent`, `add`, `update`,
> `delete`. Budget och in-balances migreras i Session M.

### Interface — `AccountingYearRepository.java`

```java
public interface AccountingYearRepository {

    /** Returns all accounting years for the current company. */
    List<SSAccountingYear> findAll();

    /** Returns the currently active year, or empty if none is open. */
    Optional<SSAccountingYear> findCurrent();

    /** Persists a new accounting year. */
    void add(SSAccountingYear year);

    /** Updates an accounting year record. */
    void update(SSAccountingYear year);

    /** Deletes an accounting year. */
    void delete(SSAccountingYear year);
}
```

### Legacy-adapter — `SSDBAccountingYearRepository.java`

### V2-implementering — `V2AccountingYearRepository.java`

### Test
- `src/test/java/se/swedsoft/bookkeeping/persistence/SSAccountingYearV2RepositoryTest.java`

---

## Task L.4 — Koppla in i `Repositories`

Utöka `Repositories.java` med tre nya fält och getters:

```java
private static AccountPlanRepository accountPlanRepository;
private static VoucherRepository voucherRepository;
private static AccountingYearRepository accountingYearRepository;
```

Utöka `Repositories.init(SSDB db)` med V2-grenarna:

```java
if (isSchemaV2()) {
    // ...befintlig masterdata...
    accountPlanRepository   = new V2AccountPlanRepository(db);
    voucherRepository       = new V2VoucherRepository(db);
    accountingYearRepository = new V2AccountingYearRepository(db);
} else {
    // ...befintlig masterdata...
    accountPlanRepository   = new SSDBAccountPlanRepository(db);
    voucherRepository       = new SSDBVoucherRepository(db);
    accountingYearRepository = new SSDBAccountingYearRepository(db);
}
```

Lägg till getters `Repositories.accountPlans()`, `Repositories.vouchers()`,
`Repositories.accountingYears()`.

---

## Task L.5 — Integrationstester

- `src/test/java/se/swedsoft/bookkeeping/persistence/SSAccountingCoreV2RepositoryTest.java`
- Verifierar att alla tre nya repositories fungerar end-to-end i V2-läge:
  - Skapa ett räkenskapsår → lägg till kontoplan → lägg till verifikation → hämta → ta bort.
- Kör med: `mvn test -Dtest=SSAccountingCoreV2RepositoryTest -Dfribok.schema.version=v2`

---

## Filförteckning — nya filer att skapa

```
src/main/java/se/swedsoft/bookkeeping/persistence/
├── AccountPlanRepository.java
├── VoucherRepository.java
└── AccountingYearRepository.java

src/main/java/se/swedsoft/bookkeeping/persistence/legacy/
├── SSDBAccountPlanRepository.java
├── SSDBVoucherRepository.java
└── SSDBAccountingYearRepository.java

src/main/java/se/swedsoft/bookkeeping/persistence/v2/
├── V2AccountPlanRepository.java
├── V2VoucherRepository.java
└── V2AccountingYearRepository.java

src/test/java/se/swedsoft/bookkeeping/persistence/
├── SSAccountPlanV2RepositoryTest.java
├── SSVoucherV2RepositoryTest.java
├── SSAccountingYearV2RepositoryTest.java
└── SSAccountingCoreV2RepositoryTest.java
```

## Filer att modifiera

```
src/main/java/se/swedsoft/bookkeeping/persistence/Repositories.java
  - Lägg till tre nya fält, init-grenar och getters
```

---

## Förutsättningar / Snabb läsordning vid sessionstart

1. `doc/migration/SESSION_RESUME_CHECKLIST.md` — verifiera senaste commit-hash
2. `doc/migration/STEP2_3_EXECUTION_PLAN.md` — bekräfta att §2 (masterdata) är klar
3. `OBJECT_COLUMN_MAPPING.md` — §C (Kategori C – Bokföring) för fältmappning
4. `src/main/java/se/swedsoft/bookkeeping/persistence/Repositories.java` — aktuell factory
5. `src/main/java/se/swedsoft/bookkeeping/persistence/v2/V2CustomerRepository.java` — mall för V2-implementering
6. `src/main/java/se/swedsoft/bookkeeping/data/SSVoucher.java` — fältstruktur för Voucher
7. `src/main/java/se/swedsoft/bookkeeping/data/SSAccountPlan.java` — fältstruktur för AccountPlan
8. `src/main/java/se/swedsoft/bookkeeping/data/SSAccountingYear.java` — fältstruktur för AccountingYear

### Snabb terminalverifiering

```powershell
cd "E:\FB_update\fribok-master3\fribok-master"
git --no-pager log --oneline -8
mvn clean test "-Dtest=SSMasterdataV2RepositoryTest" "-Dfribok.schema.version=v2"
```

Förväntat resultat:
- Senaste commit innehåller Session K-arbetet
- `SSMasterdataV2RepositoryTest` passerar (3 CRUD-flows: kund, produkt, leverantör)

---

## Risker och hantering

| Risk | Sannolikhet | Hantering |
|------|------------|-----------|
| `SSAccountingYear` FK-kedja (company → year → plan → voucher) kräver strikt insättningsordning | Hög | Skapa company → accountplan → accountingyear → voucher i testerna; aldrig hoppa steg |
| `SSDB.getAccountPlans()` returnerar `List<SSAccountPlan>` där plan-id är ett Integer-identity — kontrollera om `id`-getter finns | Medium | Granska `SSAccountPlan.java` innan L.1; lägg till getter om den saknas |
| `SSAccountingYear` innehåller budget-map och in-balance-map som ännu inte är V2-mappade | Hög | Håll `AccountingYearRepository`-scope smalt (se L.3); budget och in-balance migreras i M |
| `V2VoucherRepository` behöver `SSAccountingYear`-referens vid insättning | Medium | Skicka med `SSAccountingYear` i `add()`-signaturen eller använd aktiv instans från `Repositories.accountingYears().findCurrent()` |

---

## Koppling till övergripande plan

```
STEP2_3_EXECUTION_PLAN.md
  §1 Aktivera V2-schema          ✅ Session A
  §2 V2 repository masterdata    ✅ Sessions J, K
  §3 Migrera bokföringskärnan    ← SESSION L STARTAR HÄR
  §4 Migrera transaktioner       (Sessions M, N, ...)
  §5 Städa bort V1-paths         (sista steget)
```

---

## Acceptanskriterier

- ✅ `mvn clean compile` — inga nya kompileringsfel
- ✅ `mvn test` — inga regressioner i befintliga tester
- ✅ `mvn test -Dfribok.schema.version=v2 -Dtest=SSAccountingCoreV2RepositoryTest` passerar
- ✅ `mvn test -Dfribok.schema.version=v2 -Dtest=SSAccountPlanV2RepositoryTest,SSVoucherV2RepositoryTest,SSAccountingYearV2RepositoryTest` passerar
- ✅ `Repositories.accountPlans()`, `Repositories.vouchers()`, `Repositories.accountingYears()` returnerar icke-null efter `Repositories.init()`
- ✅ `SESSION_RESUME_CHECKLIST.md` uppdaterad med: "Session L klar"
- ✅ `CHANGELOG.md` uppdaterad med ny rad under `[Unreleased]`

---

**Sessionsstart:** Redo  
**Beroenden:** Sessions A–K (alla klara)  
**Nästa session (M):** Utöka `AccountingYearRepository` med in-balance och budget, eller starta transaktions-repositories (Invoice, Order, etc.)

