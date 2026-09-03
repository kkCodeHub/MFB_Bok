# Fribok — Förbättrings- och förenklingsförslag

## 🔴 Kritiska problem

### 1. SSDB → GUI: Cirkulärt beroende
`SSDB.java` (datapaketet) importerar 20+ GUI-klasser (`SSInvoiceFrame`, `SSVoucherFrame`, etc.). Datapaketet beror på GUI-paketet — gör det omöjligt att använda domänlagret fristående. Bryt beroendet med events/callbacks.

### 2. SSInvoice.generateVoucher() — 85 rader affärslogik i domänmodellen
Metoden anropar persistens, beräkningslager och GUI-resurser direkt inifrån domänklassen. Bör flyttas till en separat `SSVoucherGeneratorService`.

### 3. SSReportFactory — God-klass (98 KB, 2069 rader, 56 statiska metoder)
Varje metod kombinerar tre ansvarsområden: (a) dialog-visning, (b) datahämtning, (c) rapportgenerering. Bör delas upp i domänspecifika factories:
- `SSAccountingReportFactory` — voucher, balance, result, budget, VAT
- `SSSalesReportFactory` — faktura, order, kredit + e-post
- `SSPurchaseReportFactory` — leverantörsfaktura, inköpsorder
- `SSInventoryReportFactory` — lager, leveranser
- `SSJournalReportFactory` — alla journaler

Dessutom är ~45 av 56 metoder identiska boilerplate som kan ersättas med en generisk hjälpmetod.

---

## 🟠 Höga problem

### 4. SSInvoice importerar SSBundle (GUI-resurser)
Domänmodell beroende på GUI-lager. Eliminera genom att skicka lokaliserade strängar som parametrar.

### 5. Beräkningsklasser beroende av global state
`SSVoucherMath`, `SSResultCalculator`, `SSInvoiceMath` importerar `SSDB` och anropar `SSCompanyYearContext.getCurrentYear()` statiskt. Gör beräkningarna till rena funktioner som tar data som parametrar.

### 6. Dödkod
- Tomma if-satser i SSReportFactory (rad ~138, ~932): `if (isProjectSelected) {}` — ingen logik
- `SSResultPrinter.getSummaryGroup()` returnerar alltid `-1` — metoden är overksam
- `lockString` konstrueras men används aldrig (SSReportFactory)

---

## 🟡 Medelprioritet

### 7. switch(columnIndex) i ~50 printer-klasser
Alla printer-klasser upprepar identiska `switch`-block med hårda heltalsindex för kolumnmappning. Kan abstraheras med en `ColumnMapper<T>` strategi i basklassen.

### 8. SSCompanyYearContext — halvmigrering
Vissa metoder delegerar till `Repositories.*`, andra fortfarande till `SSDB.getInstance()`. Slutför migreringen.

### 9. Saknade enhetstester

| Komponent | Risk |
|---|---|
| `SSReportFactory` — 56 metoder | Hög |
| `SSDB` | Hög |
| `SSInvoice.generateVoucher()` | Hög |
| `SSResultCalculator`, `SSBalanceCalculator` | Medel |

---

## 🟢 Lågprioritet

### 10. Namnkonvention i SSReportFactory
Mix av `buildXxxReport()`, `XxxReport()`, `XxxList()`, `XxxJournal()` — standardisera till `buildXxxReport()`.

### 11. V2RepositoryHelpers
Konstruktorn är `public` men klassen ska vara icke-instantierbar. Gör konstruktorn `private`.

### 12. Hårdkodad svenska i källkod
`SSInvoice` rad 65: `iOrderNumbers = "Fakturan har inga ordrar"` — flytta till resursbuntar.
