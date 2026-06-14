# Deprecated API TODO Matrix (src/main/java)

Datum: 2026-05-29

Denna matris bygger pa en riktad sweep av `@Deprecated` i `src/main/java` samt en ren kompilering:

```text
mvn clean -DskipTests compile
```

Resultat i sweepen: inga aktiva deprecation-varningar i main-kod vid kompilering.

## Matris per klass

| Klass | Deprecated-omrade | Ersattning | Planerad borttagning | Forutsattning |
|---|---|---|---|---|
| `SSDateMath` | Date-overloads (`floor/ceil/...`) | `LocalDate`-overloads | `3.3` | Bekrafta att externa integrationer inte anropar Date-overloads |
| `SSAccountGroupMath` | Hela utility-klassen (legacy kontogrupp) | Ny kontogruppering i V2-kodvag | `3.4` | Verifiera fortsatt 0 anrop i appkod |
| `SSVoucherMath` | `inPeriodPrevYear(SSVoucher, Date, Date)` | `inPeriodPrevYear(SSVoucher, LocalDate, LocalDate)` | `3.3` | Behall endast LocalDate-signatur |
| `SSDateChooser` | `getDate()` / `setDate(Date)` | `getLocalDate()` / `setLocalDate(LocalDate)` | `3.4` | Migrera kvarvarande externa/legacy-anrop |
| `SSCalendar` | `getDate()` / `setDate(Date)` | `getLocalDate()` / `setLocalDate(LocalDate)` | `3.4` | Samordna med datechooser-paneler |
| `SSDayChooser` | Date-baserade accessorer | LocalDate-baserade accessorer | `3.4` | GUI-regressionstest for datumvaljare |
| `SSMonthChooser` | Date-baserade accessorer | LocalDate-baserade accessorer | `3.4` | GUI-regressionstest for datumvaljare |
| `SSYearChooser` | Date-baserade accessorer | LocalDate-baserade accessorer | `3.4` | GUI-regressionstest for datumvaljare |
| `SSQuarterChooser` | Date-baserade accessorer | LocalDate-baserade accessorer | `3.4` | GUI/regression for kvartalsrapporter |
| `SSQuarterReportDialog` | `getDate()` / `getEndDate()` | `getLocalDate()` / `getLocalEndDate()` | `3.3` | Uppdatera eventuella kvarvarande kallare |
| `SSSaleReportDialog` | Date-baserade accessorbroar | LocalDate-baserad periodlogik | `3.3` | Verifiera utskriftsfloden |
| `SSMonth` | Date-konstruktorer + Date-getters | LocalDate-konstruktorer/getters | `3.4` | Serialization/backward-compat test |
| `SSPaymentTerm` | `addDaysToDate(Date)` | `addDaysToLocalDate(LocalDate)` | `3.3` | Validera legacy-bug-kompatibilitet i overgangen |
| `SSInvoice` | Date-get/set-broar | LocalDate-get/set | `3.4` | Export/import- och rapporttester |
| `SSInpayment` | Date-get/set-broar | LocalDate-get/set | `3.4` | Rapport- och betalflodestest |
| `SSOutpayment` | Date-get/set-broar | LocalDate-get/set | `3.4` | Rapport- och betalflodestest |
| `SSInventory` | Date-get/set-broar | LocalDate-get/set | `3.4` | Lager-/inventeringsrapporttest |
| `SSPeriodicInvoice` | Date-get/set + Date-hjalpmetoder | LocalDate API | `3.4` | Full periodfaktura-regression |
| `SSPurchaseOrder` | Date-get/set-broar | LocalDate-get/set | `3.4` | Inkop/order-regression |
| `SSSupplierInvoice` | Date-get/set-broar + legacy-datumhjalpare | LocalDate API | `3.4` | Leverantorsfaktura/utbetalning-regression |
| `SSTender` | Date-get/set-broar | LocalDate API | `3.4` | Kassaflodes-/betaltest |
| `SSCompany` | Hela legacy-klassen (serialization stub) | `SSNewCompany` | `4.0` | Beslut om slutlig serializationstrategi |
| `SSAccountingYear` | Hela legacy-klassen (serialization stub) | `SSNewAccountingYear` | `4.0` | Beslut om slutlig serializationstrategi |
| `SSProject` | Hela legacy-klassen (compat shim) | `SSNewProject` | `4.0` | Ta bort kvarvarande shim-behov + suppressions |
| `SSResultUnit` | Hela legacy-klassen (compat shim) | `SSNewResultUnit` | `4.0` | Ta bort kvarvarande shim-behov |

## Rekommenderad exekveringsordning

1. `3.3`: rensa metodnivaa-deprecations med tydliga LocalDate-ersattare.
2. `3.4`: ta bort kvarvarande Date-broar i doman/GUI efter regressionstest.
3. `4.0`: avveckla legacy-klass-shims (`SSCompany`, `SSAccountingYear`, `SSProject`, `SSResultUnit`).

## Konkret 3.3-exekveringslista (fil/metod + PR-batchordning)

### Batch 3.3-A: ta bort oanropade Date-overloads i `SSDateMath` ✅ DONE (2026-05-30)

**Fil:** `src/main/java/se/swedsoft/bookkeeping/calc/math/SSDateMath.java`

**Borttagna metoder:**
- `public static Date floor(Date pDate)` — ✅ borttagen
- `public static Date ceil(Date pDate)` — ✅ borttagen
- `public static Date getFirstDayInMonth(Date pDate)` — ✅ borttagen
- `public static Date getLastDayMonth(Date pDate)` — ✅ borttagen
- `public static int getMonthsBetween(Date iFrom, Date iTo)` — ✅ borttagen
- `public static int getDaysBetween(Date iFrom, Date iTo)` — ✅ borttagen
- `public static Date addMonths(Date iDate, Integer iCount)` — ✅ borttagen

**Borttagna importer:** `java.util.Calendar`, `java.util.Date`, `SSDateUtil`
**Test:** `SSDateMathTest` uppdaterad — legacy Date-tester borttagna.
**Build:** Grön (168 tester, 0 fel).

### Batch 3.3-B: ta bort Date-overload i `SSVoucherMath` ✅ ALREADY DONE

**Fil:** `src/main/java/se/swedsoft/bookkeeping/calc/math/SSVoucherMath.java`

**Metod:** `public static boolean inPeriodPrevYear(SSVoucher iVoucher, Date pFrom, Date pTo)` — redan borttagen i tidigare session.

**Behalls:**
- `public static boolean inPeriodPrevYear(SSVoucher iVoucher, LocalDate pFrom, LocalDate pTo)`


### Batch 3.3-C: ta bort Date-brygga i `SSPaymentTerm` ✅ ALREADY DONE

**Fil:** `src/main/java/se/swedsoft/bookkeeping/data/common/SSPaymentTerm.java`

**Metod:** `public Date addDaysToDate(Date iDate)` — redan borttagen i tidigare session.

**Behalls:**
- `public LocalDate addDaysToLocalDate(LocalDate iDate)`


### Batch 3.3-D: ta bort Date-bryggor i rapportdialoger ✅ ALREADY DONE

**Filer:** `SSQuarterReportDialog.java` och `SSSaleReportDialog.java`

Date-bridge-metoderna (`getDate()`, `getEndDate()`, `getFrom()`, `getTo()`) finns inte längre — redan borttagna i tidigare session. Kvarvarande metoder är `getLocalDate()`, `getLocalEndDate()`, `getLocalFrom()`, `getLocalTo()`.

## Verifiering per PR-batch

Kors efter varje batch:

1. Kompilering
   - `mvn -DskipTests compile`
2. Relevant testsubset (om test finns i batchens omrade)
   - `mvn -Dtest=*Date*,*Voucher*,*Report* test`
3. Ingen ny deprecation i main
   - `mvn clean -DskipTests compile`

## Foreslagen PR-ordning

1. PR-3.3-A: `SSDateMath` Date-overloads
2. PR-3.3-B: `SSVoucherMath.inPeriodPrevYear(Date, Date)`
3. PR-3.3-C: `SSPaymentTerm.addDaysToDate(Date)`
4. PR-3.3-D: `SSQuarterReportDialog` + `SSSaleReportDialog` Date-bryggor

Denna ordning minimerar risk genom att ta utility-overloads forst och UI-broar sist.


