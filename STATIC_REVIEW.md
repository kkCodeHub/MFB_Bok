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
5. **`java.util.Date`:** 103 förekomster. Gör klart övergången till `LocalDate` och behåll `Date` bara
   mot Jasper och Swing.
6. **`new BigDecimal(<literal>)`:** 248 förekomster. Ersätt med `BigDecimal.ZERO`, `BigDecimal.ONE` eller
   `valueOf`, som är tydligare och undviker fel med flyttal.
7. **Felaktig metod:** `SSReport.addParameters(Map)` lägger in parametern i sig själv, eftersom
   parametern skuggar fältet. Metoden gör därför ingenting.
8. **Kör `mvn checkstyle:check` och `spotbugs:check` regelbundet**, gärna i en CI-workflow, som saknas
   i dag.

## Troligen oanvända klasser (20)

Ingen annan källfil i `main` refererar till dessa klasser:

- **calc:** `SSSalesTaxCalculator`, `SSAccountGroupMath`
- **data:** `SSBackupData`, `SSCompanyConfig`, `SSDBState`, `SSDBThread`, `SSInventoryDeliveriesContext`
  (används i tester)
- **gui:** `SSValueListener`, `SSHelpButton`, `SSProgressBar`, `SSInputDialog`, `SSDBFileChooser`,
  `SSBooleanCellEditor`, `SSBooleanCellRenderer`, `SSVoucherEditors`
- **övrigt:**
  - `SSSchemaMigrationManager` (bara i tester)
  - `SSCreditinvoicejournalPrinter`, `SSSuppliercreditinvoicejournalPrinter`
  - `SSResourceBundle`

Kontrollera före borttagning om klasserna laddas via reflektion, serialisering (till exempel
`SSBackupData` i gamla säkerhetskopior) eller namn i konfiguration.

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
