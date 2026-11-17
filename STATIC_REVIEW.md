# Statisk genomgång av projektet

Datum: 2026-11-16. Genomgången är gjord utan kodändringar. Kompilering med
`-Xlint:deprecation` lyckades.

## Uppbyggnad

- **Storlek:** 733 källfiler (cirka 141 000 rader) och 175 testfiler.
- **Lager:** `data` (domän och kontexter), `persistence/v2` (repositories), `calc`, `gui`, `print` och
  `importexport`.
- **Styrkor:**
  - Ett nyare V2-persistenslager med repositories och schemamigrering.
  - Integrationstester med delade fixtures.
  - Checkstyle och SpotBugs är konfigurerade.
  - Kompilatorn kör med `-Xlint:deprecation/unchecked`.
- **Svagheter:**
  - **Gudklasser:** `SSReportFactory` är 107 KB och samlar all rapport- och mejllogik. `SSMainMenu` är
    55 KB, `V2CompanyRepository` 57 KB och `SSCustomerImporter` och `SSOrderImporter` cirka 55 KB vardera.
  - **Globalt tillstånd:** statiska `*Context.get...`-anrop förekommer på cirka 770 ställen, vilket ger
    stark koppling och gör det svårt att testa.
  - **GUI och logik blandas:** paneler som `SSInvoicePanel` och `SSVoucherPanel` är 35–48 KB.
  - **Tunn testtäckning för gui, print och import/export:** testerna ligger mest i persistence, data och
    integration.

## Förbättringar med enkla metoder

1. **Dela upp `SSReportFactory`** per dokumenttyp: försäljning, inköp, journaler och mejl.
   Mejlflödena (faktura, kreditfaktura, order, offert med flera) är nästan kopior och kan ersättas av en
   gemensam hjälpmetod.
2. **Ta bort död kod:**
   - 256 bortkommenterade kodrader.
   - 118 `$Id$`-huvuden.
   - Cirka 1 180 tomma `@return`-rader i Javadoc.
3. **Wildcard-importer:** `import java.util.*` förekommer i 68 filer och strider mot stilreglerna. Det
   åtgärdas automatiskt med IDE:ns funktion för att organisera importer.
4. **`catch (Exception)`:** 14 ställen, som bör smalnas av till specifika undantag.
   **Åtgärdat (2026-11-17):** alla 14 ställen i produktionskoden är avsmalnade till specifika undantag
   (`DateTimeParseException`, `IOException`, `SQLException`, `EvaluationException`/`ParseException`,
   `RuntimeException`). Checkstyle-regeln `IllegalCatch` i `checkstyle.xml` varnar för nya. De fem
   `catch (Throwable)` i `SSReport` (Jasper) och `catch (Exception)` i tester ingick inte.
5. **`java.util.Date`:** 103 förekomster av den explicita importraden `import java.util.Date` (2026-11-16).
   **Obs:** den siffran missar användning via wildcard-importen `import java.util.*` (se punkt 3 ovan),
   som flera centrala beräkningsklasser använder. En bredare sökning (typ-matchning på `Date`, exklusive
   `LocalDate`/`LocalDateTime`) visar att minst följande affärslogik-klasser i `calc/` och `data/` också
   har kvarvarande `Date`-parametrar/fält trots wildcard-importen och alltså inte syntes i den ursprungliga
   räkningen: `SSInvoiceMath`, `SSSupplierInvoiceMath`, `SSCreditInvoiceMath`, `SSSupplierCreditInvoiceMath`,
   `SSProductMath`, `SSBalanceCalculator`, `SSMainBookCalculator`, `SSOwnReportCalculator`,
   `SSResultCalculator`, `SSBudget`, `SSStock`. Tillsammans med de 15 ursprungligen hittade filerna
   (`SSCreditInvoice`, `SSIndelivery`, `SSNewProject`, `SSOutdelivery`, `SSTender`, `SSVoucherRow`,
   `SSVoucherTemplate`, `SSBackupFactory`, `SSSystemYear`, `SSIndeliveryMath`, `SSInpaymentMath`,
   `SSInventoryMath`, `SSOutdeliveryMath`, `SSOutpaymentMath`, `SSVATUtil`) ger det minst 24 filer i
   kärndomän-/beräkningslagret som behöver färdigmigreras, inte 15. Flera av dem (t.ex. `SSInvoiceMath`,
   `SSMainBookCalculator`) har breda anropsytor mot `gui`/`print` och bör migreras i mindre, testade
   omgångar snarare än i en enda stor ändring.

   Gör klart övergången till `LocalDate` i `data/`, `calc/` och `persistence/` — inklusive att ta bort
   kvarvarande bakåtkompatibla `Date`-wrappers/overloads (t.ex. `getX()/setX(Date)` bredvid
   `getLocalX()/setLocalX(LocalDate)`) och uppdatera deras anropare att konvertera med `SSDateUtil`
   precis vid gränsen. Behåll `Date` bara mot Jasper, Swing och externa API:er som inte själva förstår
   `LocalDate` (t.ex. Apache POI, SIE-filskrivning).
   **Åtgärdat (2026-11-17):** `data/`, `calc/` och `persistence/` innehåller inget `java.util.Date`
   längre (JDBC använder `java.sql.Date` i `persistence/v2`). Alla `Date`-wrappers och -overloads är
   borttagna, `importexport` (SIE, Excel, leverantörsbetalningar) använder `LocalDate` och `Date`
   finns bara kvar mot Swing, Jasper och Apache POI. Regeln vaktas av `DateBoundaryRuleTest` och av
   Checkstyle-regeln `IllegalImport` i `checkstyle.xml`.
6. **`new BigDecimal(<literal>)`:** 248 förekomster. Ersätt med `BigDecimal.ZERO`, `BigDecimal.ONE` eller
   `valueOf`, som är tydligare och undviker fel med flyttal.
7. **Felaktig metod:** `SSReport.addParameters(Map)` lägger in parametern i sig själv, eftersom
   parametern skuggar fältet. Metoden gör därför ingenting.
8. **Kör `mvn checkstyle:check` och `spotbugs:check` regelbundet**, gärna i en CI-workflow, som saknas
   i dag.

## Troligen oanvända klasser (11)

Ingen annan källfil i `main` refererar till dessa klasser:

- **gui:** `SSValueListener`, `SSHelpButton`, `SSProgressBar`, `SSInputDialog`, `SSDBFileChooser`,
  `SSBooleanCellEditor`, `SSBooleanCellRenderer`, `SSVoucherEditors`
- **övrigt:**
  - `SSCreditinvoicejournalPrinter`, `SSSuppliercreditinvoicejournalPrinter` — medvetet behållna
    (2026-11-16) som reserv för kreditfaktura-journalmallarna (`creditinvoicejournal*.jrxml`,
    `suppliercreditinvoicejournal*.jrxml`), som inte används av något annat.

Åtgärdat (2026-11-16): `SSSalesTaxCalculator`, `SSAccountGroupMath`, `SSBackupData`,
`SSCompanyConfig`, `SSDBState`, `SSDBThread`, `SSResourceBundle`, `SSInventoryDeliveriesContext`
(+ tillhörande integrationstester) och `SSSchemaMigrationManager` (+ tillhörande tester) har
tagits bort eftersom de var helt orefererade utanför sig själva.

Kontrollera före borttagning av återstående klasser om de laddas via reflektion, serialisering
eller namn i konfiguration.

## Deprecated

- **Externa API:er:** endast 1 varning från kompilatorn, `new Locale(String, String)` i
  `SSInvoiceTextFileOptions`. Den ersätts med `Locale.of(...)`.
- **Egna `@Deprecated`:** 49 markeringar. 28 av dem finns i `SSMasterdataContext` och gäller projekt och
  resultatenheter. Övriga finns i `SSDBUiInitializer`, `SSMainBookCalculator`, `SSResultCalculator`,
  `SSOwnReportCalculator`, `SSAccountGroupMath` och `SSAccountingContext.getDatabase`.
  - Nästan alla har fortfarande anropare. En namnsökning ger träffar som `getProject` (cirka 75) och
    `getResultUnit` (cirka 73). Siffrorna är ungefärliga, eftersom icke-deprecated överlagringar med
    samma namn också räknas.
  - Migrera anroparna till de nya signaturerna som anges i `@deprecated`-texten och ta sedan bort de
    gamla metoderna. Det kan göras klass för klass.

## Föreslagen ordning

1. Ta bort oanvända klasser.
2. Rensa importer och kommentarer.
3. Åtgärda `Locale` och `addParameters`.
4. Dela upp `SSReportFactory`.
5. Migrera de egna deprecated-anropen stegvis.
