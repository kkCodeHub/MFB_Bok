# SIE-filparametrar - Värdens källor

## Arkitekturöversikt

SIE-filen genereras av `SSSIEExporter` som itererar genom en lista av `SIEEntry`-implementationer (definierat av `SIELabel` enum). Varje `SIEEntry` är ansvarig för att exportera en viss parameter.

```
SSSIEExporter
    ↓
SIEFactory (hämtar SIE-format specifika labels)
    ↓
SIELabel enum (definierar alla SIE-poster)
    ↓
SIEEntry implementationer (varje exporterar en parameter)
    ↓
Datakällor (SSNewCompany, SSNewAccountingYear, SSDB, etc.)
```

## Parametrars värdens ursprung

### **Identifikationsposter** (`#` prefix-poster)

| Parameter | Klass | Värdens källa | Hämtningsplats | Anteckningar |
|-----------|-------|---------------|----------------|--------------|
| **#FLAGGA** | `SIEEntryFlagga` | Hårdkodat | `"0"` | Flaggpost ignoreras vid import, exporteras alltid som 0 |
| **#PROGRAM** | `SIEEntryProgram` | Kod-konstanter | `Version.APP_TITLE` och `Version.APP_VERSION` | Programnamn och version från JarFile/Version.java |
| **#FORMAT** | `SIEEntryFormat` | Hårdkodat | `"PC8"` | Standard SIE-format |
| **#GEN** | `SIEEntryGenererat` | System-tid | `SSDateUtil.now()` | Exporttidpunkt när filen genereras |
| **#SIETYP** | `SIEEntryTyp` | Exportör-konfiguration | Från `SIEType` parameter i `SSSIEExporter` | Bestämmer SIE-format-typ (1, 2, 3, 4E, 4I) |
| **#PROSA** | `SIEEntryProsa` | Valfri parameter | Från `SSSIEExporter.iComment` | Tillhandahålls från användaren/API |
| **#FNR** | `SIEEntryForetagsid` | Databas | `SSNewCompany.getId()` | Företagets interna ID från SSDB |
| **#ORGNR** | `SIEEntryOrgnummer` | Databas | `SSNewCompany.getCorporateID()` | Organisationsnummer (ex: "556600-6699") |
| **#BKOD** | `SIEEntryBranschkod` | Hårdkodat | `0` | SNI-kod (branschkod) - lagras INTE i datamodellen |
| **#ADRESS** | `SIEEntryForetagsAdress` | Databas | `SSNewCompany.getAddress()` | Företagets adressuppgifter |
| **#FNAMN** | `SIEEntryFNamn` | Databas | `SSNewCompany.getName()` | Företagets namn (ex: "Min Företag AB") |
| **#RAR** | `SIEEntryRAR` | Databas | `SSNewAccountingYear.getLocalFrom()/.getLocalTo()` | Räkenskapsårets från/till datum (lagras i `tbl_accounting_year`) |
| **#TAXAR** | `SIEEntryTaxar` | Databas | `SSNewAccountingYear` | Taxeringsår för deklarationsinformation |
| **#OMFATTN** | `SIEEntryOmfattn` | Databas | `SSNewAccountingYear.getLocalTo()` | Periodsaldons omfattning (datum) |
| **#KPTYP** | `SIEEntryKontoplanTyp` | Databas | `SSNewAccountingYear.getAccountPlan().getType()` | Kontoplanstyp |

### **Kontoplansuppgifter**

| Parameter | Klass | Värdens källa | Hämtningsplats | Anteckningar |
|-----------|-------|---------------|----------------|--------------|
| **#KONTO** | `SIEEntryKonto` | Databas | `SSNewAccountingYear.getAccountPlan().getAccounts()` | Itererar alla konton i kontoplanen |
| **#KTYP** | `SIEEntryKontoTyp` | Databas | `SSNewAccountingYear.getAccountPlan().getAccounts()` | Kontotyp för varje konto |
| **#ENHET** | `SIEEntryEnhet` | Databas | `SSNewAccountingYear.getAccountPlan()` | Enheter för kvantitetsredovisning |
| **#SRU** | `SIEEntrySRU` | Databas | `SSNewAccountingYear` | RSV-Kod för standardiserat räkenskapsutdrag |
| **#DIM** | `SIEEntryDimension` | Databas | `SSNewAccountingYear` | Dimensioner (projekt, resultatenheter, etc.) |
| **#UNDERDIM** | `SIEEntryUnderDimension` | Databas | `SSNewAccountingYear` | Underdimensioner |
| **#OBJEKT** | `SIEEntryObjekt` | Databas | `SSNewAccountingYear` | Objekt (motsvarar dimensionsvärden) |

### **Saldoposter**

| Parameter | Klass | Värdens källa | Hämtningsplats | Anteckningar |
|-----------|-------|---------------|----------------|--------------|
| **#IB** | `SIEEntryInBalance` | Databas | `tbl_balances` | Ingående balans för balanskonto |
| **#UB** | `SIEEntryOutBalance` | Databas | `tbl_balances` | Utgående balans för balanskonto |
| **#RES** | `SIEEntryResult` | Databas | `tbl_balances` | Saldo för resultatkonto |
| **#PSALDO** | `SIEEntryPeriodSaldo` | Databas | `tbl_balances` | Periodens saldo för ett visst konto |
| **#PBUDGET** | `SIEEntryPeriodBudget` | Databas | `tbl_balances` | Periodens budget för ett visst konto |

### **Verifikationsposter**

| Parameter | Klass | Värdens källa | Hämtningsplats | Anteckningar |
|-----------|-------|---------------|----------------|--------------|
| **#VER** | `SIEEntryVerifikation` | Databas | `tbl_verification` | Verifikationspost (serie, nummer, datum) |
| **#TRANS** | `SIEEntryTransaktion` | Databas | `tbl_transaction` | Transaktionspost (konto, belopp, text) |

---

## Databaskällor

### Huvudtabeller:

```
tbl_company
├── id (FNR)
├── name (FNAMN)
├── corporate_id (ORGNR)
├── phone, phone2, telefax, email, etc. (ADRESS)
└── ...

tbl_accounting_year
├── id
├── company_id (FK)
├── date_from (RAR)
├── date_to (RAR)
├── account_plan_id (KPTYP, KONTO)
├── tax_year (TAXAR)
└── ...

tbl_account
├── id
├── accounting_year_id (FK)
├── account_number (KONTO)
├── description (KONTO)
└── type (KTYP)

tbl_balances
├── id
├── accounting_year_id (FK)
├── account_id (FK)
├── year_number (IB, UB, RES)
├── opening_balance (IB)
├── closing_balance (UB)
├── result_balance (RES)
└── ...

tbl_verification
├── id
├── accounting_year_id (FK)
├── series (VER)
├── number (VER)
├── date (VER)
└── ...

tbl_transaction
├── id
├── verification_id (FK)
├── account_id (FK)
├── amount (TRANS)
├── description (TRANS)
└── ...
```

---

## Datahämtning - Detaljerat exempel: BKOD

### Källkod för **#BKOD** (Branschkod):

```java
// SIEEntryBranschkod.java
public boolean exportEntry(SSSIEExporter iExporter, SIEWriter iWriter, SSNewAccountingYear iCurrentYearData) throws SSExportException {
    // SNI code (branschkod) is not stored in the company data model;
    // export 0 as a placeholder per SIE specification.
    iWriter.append(SIELabel.SIE_BKOD);
    iWriter.append(0);  // <- HÅRDKODAT VÄRDE
    iWriter.newLine();
    return true;
}
```

**Värdens ursprung:** `0` (hårdkodat)
**Anledning:** Branschkod (SNI-kod) är inte implementerad i datamodellen
**Möjlig framtida källa:** `SSNewCompany.getBranchCode()` (om implementerad)

---

## Datahämtning - Detaljerat exempel: ORGNR

### Källkod för **#ORGNR** (Organisationsnummer):

```java
// SIEEntryOrgnummer.java
public boolean exportEntry(SSSIEExporter iExporter, SIEWriter iWriter, SSNewAccountingYear iCurrentYearData) throws SSExportException {
    SSNewCompany iCompany = SSDB.getInstance().getCurrentCompany();
    
    iWriter.append(SIELabel.SIE_ORGNR);
    iWriter.append(iCompany.getCorporateID());  // <- HÄMTAS FRÅN DATABASE
    iWriter.newLine();
    return true;
}
```

### Spårning av värdet:

1. **Exportör anropar:** `SSDB.getInstance().getCurrentCompany()`
2. **SSDB returnerar:** `SSNewCompany` objekt från aktuell session
3. **SSNewCompany.getCorporateID() hämtar:** Värde från privat fält `iCorporateID`
4. **Ursprunglig källa:** `tbl_company.corporate_id` i databasen
5. **Lagring:** Sätts via `SSNewCompany.setCorporateID(String)` när företag läses från DB
6. **SQL-fråga:** 
   ```sql
   SELECT corporate_id FROM tbl_company WHERE id = ?
   ```

### Användargränssnitt för redigering:

```java
// SSCompanyPageGeneral.java
public SSNewCompany getCompany() {
    iCompany.setCorporateID(iCorporateID.getText());  // <- FRÅN GUI-TEXTFÄLT
    // ...
    return iCompany;
}
```

---

## Datahämtning - Detaljerat exempel: RAR

### Källkod för **#RAR** (Räkenskapsår):

```java
// SIEEntryRAR.java
public boolean exportEntry(SSSIEExporter iExporter, SIEWriter iWriter, SSNewAccountingYear iCurrentYearData) throws SSExportException {
    SSNewAccountingYear iPreviousYearData = SSDB.getInstance().getPreviousYear().orElse(null);

    if (iPreviousYearData != null) {
        iWriter.append(SIELabel.SIE_RAR);
        iWriter.append("-1");  // År -1 = föregående år
        iWriter.append(iPreviousYearData.getLocalFrom());     // <- FRÅN DATABASE
        iWriter.append(iPreviousYearData.getLocalTo());       // <- FRÅN DATABASE
        iWriter.newLine();
    }

    if (iCurrentYearData != null) {
        iWriter.append(SIELabel.SIE_RAR);
        iWriter.append("0");   // År 0 = aktuellt år
        iWriter.append(iCurrentYearData.getLocalFrom());      // <- FRÅN DATABASE
        iWriter.append(iCurrentYearData.getLocalTo());        // <- FRÅN DATABASE
        iWriter.newLine();
    }

    return iPreviousYearData != null || iCurrentYearData != null;
}
```

### Database-lagring:

```java
// SSDB.java mapCompanyV2()
iAccYear.setLocalFrom(iResultSet.getLocalDateTime("date_from").toLocalDate());
iAccYear.setLocalTo(iResultSet.getLocalDateTime("date_to").toLocalDate());
```

---

## Datahämtning - Detaljerat exempel: KONTO

### Källkod för **#KONTO** (Kontoplansuppgifter):

```java
// SIEEntryKonto.java
public boolean exportEntry(SSSIEExporter iExporter, SIEWriter iWriter, SSNewAccountingYear iYearData) throws SSExportException {
    SSAccountPlan iAccountPlan = iYearData.getAccountPlan();

    for (SSAccount iAccount : iAccountPlan.getAccounts()) {
        iWriter.append(SIELabel.SIE_KONTO);
        iWriter.append(iAccount.getNumber());        // <- FRÅN DATABASE: tbl_account.account_number
        iWriter.append(iAccount.getDescription());   // <- FRÅN DATABASE: tbl_account.description
        iWriter.newLine();
    }

    return !iAccountPlan.getAccounts().isEmpty();
}
```

### Loop-struktur:
- Itererar genom **alla konton** i det aktiva räkenskapsårets kontoplan
- För varje konto exporteras:
  - Kontonummer (t.ex. `1000`, `2000`, `3000`)
  - Kontobeskrivning (t.ex. `"Kas och bank"`, `"Leverantörskulder"`)

---

## Flödesschema: Från GUI till SIE-fil

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. ANVÄNDARE FYLLER I GUI (SSCompanyPageGeneral)                │
│    └─ Namn, ORGNR, Adress, etc.                                 │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. GUI LAGRAR I DATABAS (SSDB.updateCompany())                  │
│    └─ INSERT/UPDATE tbl_company                                 │
│       └─ corporate_id, name, phone, etc.                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. EXPORTÖR INITIERAS (SSSIEExporter.exportSIE())               │
│    └─ Hämtar aktuellt företag: SSDB.getInstance().getCurrentCompany()
│    └─ Hämtar aktuellt år: SSDB.getInstance().getCurrentYear()  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. FACTORY SKAPAR LABEL-LISTA (SIEFactory.getLabels())          │
│    └─ Lista av alla SIE-poster för vald format                  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. FÖR VARJE LABEL - ENTRY EXPORTERAS                           │
│    └─ SIEEntryXxx.exportEntry() anropas                         │
│       └─ Hämtar värden från SSDB eller hårdkodade              │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. WRITER SKRIVER TILL FIL (SIEWriter.append())                 │
│    └─ Formaterar som SIE-text: "#LABEL värde1 värde2"          │
│    └─ Sparas i StringBuilder (iLines)                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. FIL SKRIVS TILL DISK (SSSIEExporter.writeFile())             │
│    └─ Alla linjer från StringBuilder skrivs till fil            │
│    └─ Resultat: xxx.sie                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Källfiler och klassrelationer

```
se.swedsoft.bookkeeping
├── importexport/sie/
│   ├── SSSIEExporter.java              [MAIN ORCHESTRATOR]
│   ├── SSSIEImporter.java
│   ├── fields/
│   │   ├── SIEEntry.java               [INTERFACE]
│   │   ├── SIEEntryBranschkod.java     [BKOD export]
│   │   ├── SIEEntryOrgnummer.java      [ORGNR export]
│   │   ├── SIEEntryFNamn.java          [FNAMN export]
│   │   ├── SIEEntryRAR.java            [RAR export]
│   │   ├── SIEEntryKonto.java          [KONTO export]
│   │   ├── SIEEntryVerifikation.java   [VER export]
│   │   ├── SIEEntryTransaktion.java    [TRANS export]
│   │   ├── SIEEntryGenererat.java      [GEN export]
│   │   ├── SIEEntryProgram.java        [PROGRAM export]
│   │   └── ... (fler entries)
│   └── util/
│       ├── SIELabel.java               [ENUM: alla labels]
│       ├── SIEFactory.java             [CREATE LABEL LISTS]
│       ├── SIEWriter.java              [WRITE FORMATTED OUTPUT]
│       ├── SIEReader.java              [PARSE SIE FILES]
│       ├── SIEType.java                [ENUM: 1,2,3,4E,4I]
│       └── SIEFile.java
├── data/
│   ├── SSNewCompany.java               [DATA MODEL]
│   ├── SSNewAccountingYear.java        [DATA MODEL]
│   ├── SSAccount.java
│   └── SSAccountPlan.java
└── system/
    └── SSDB.java                       [DATABASE ACCESS]
```

---

## Sammanfattning

**Huvudkällor för SIE-parametervärden:**

| Källa | Exempel | Implementering |
|-------|---------|-----------------|
| **Databas** | ORGNR, FNAMN, RAR, KONTO | `SSDB.getInstance()` → `tbl_company`, `tbl_accounting_year`, etc. |
| **Aktuell session** | Inloggad användare, aktuellt företag | `SSDB.getInstance().getCurrentCompany()` |
| **Systemtid** | #GEN (exportdatum) | `SSDateUtil.now()` |
| **Programkonstanter** | #PROGRAM, #FORMAT | `Version.APP_TITLE`, `Version.APP_VERSION` |
| **Hårdkodad** | #BKOD (branschkod), #FLAGGA | Statiska värden i SIEEntry-klasser |
| **Ingångsparameter** | #PROSA (kommentar) | `SSSIEExporter` konstruktor parameter |

Varje SIE-parameter är tilldelad en specifik `SIEEntry`-implementering som är ansvarig för att hämta sitt värde från lämplig källa.

