# Fribok V2 - Kodgranskning och förbättringsförslag

Datum: 2026-05-27

## Omfattning

Genomgången baseras på kodstruktur, beroenden, identifierade risker, moderniseringsstatus och generella förbättringsområden i V2-spåret.

## Projektöversikt

- Java-källfiler (main): cirka 702
- Testfiler (test): cirka 104
- Arkitektur: Swing-GUI + HSQLDB, monolitisk struktur med central databas-/domänhantering
- Pågående modernisering: Date/LocalDate-övergång, V2-schemaarbete, nyare teststruktur

## Säkerhetsstatus (beroenden)

Identifierade CVE-risker i beroenden:

1. `net.sf.jasperreports:jasperreports:6.21.5`
   - CVE: `CVE-2025-10492`
   - Risk: Hög
   - Rekommendation: uppgradera till minst `7.0.4`

2. `org.assertj:assertj-core:3.26.3    UPPGRADERAD` (testscope)
   - CVE: `CVE-2026-24400`
   - Risk: Hög
   - Rekommendation: uppgradera till minst `3.27.7`

3. `com.lowagie:itext:4.2.2                     BORTA`
   - CVE: `CVE-2017-9096`
   - Risk: Hög
   - Status: ingen direkt patch i samma artefaktlinje
   - Rekommendation: utred migrering till modern iText-linje (`com.itextpdf`)

4. `org.springframework:spring-core:6.1.14      BORTA`
   - CVE: `CVE-2025-41249`
   - Risk: Hög
   - Rekommendation: bevaka patch/fix-version och minska exponering

## Viktiga fynd

### 1) Oanvända Spring-beroenden i koden   BORTA

`spring-core` och `spring-beans` finns i `pom.xml`, men inga faktiska användningar hittades i Java-koden.

- Rekommendation: ta bort dem om de inte behövs transitivt.
- Effekt: mindre attackyta, snabbare build, färre uppgraderingsrisker.

### 2) `SSDB.java` är mycket stor och ansvarstung

`src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java` är mycket omfattande (kring 15k rader) och innehåller blandade ansvar:

- schema/migrering
- CRUD för många domänobjekt
- triggerhantering
- kopplingar till GUI-uppdateringar

Rekommendation:

- bryt ut funktionalitet till repository-/service-klasser per domän
- minimera koppling mellan persistence och GUI
- behåll `SSDB` som tunn fasad där möjligt

### 3) Blandad hantering av SQL-resurser

Kodbasen har både:

- modern `try-with-resources`
- äldre manuell `close()`-hantering

Rekommendation:

- standardisera på `try-with-resources`
- minska risk för resursläckage och förenkla felhantering

### 4) Legacy-spår kvar i domänmodellen

Klasser som `SSProject`/`SSResultUnit` samexisterar med `SSNewProject`/`SSNewResultUnit`.

Rekommendation:

- dokumentera tydlig målmodell för V2
- avveckla legacy-klasser stegvis när migration är verifierad
- undvik dubbla representationsmodeller längre än nödvändigt

### 5) `SSVoucherRowTableModelOld` används fortfarande  ÅTGÄRDAD (2026-05-29)

Namn och kommentering antyder utfasning, men klassen används i aktiv kod.

Status:

- Aktiv GUI-kod är migrerad till `SSVoucherRowTableModel`.
- `SSVoucherRowTableModelOld` är markerad `@Deprecated` och kvar endast som kompatibilitetsklass.
- Återstående referenser är kommentarer/loggsträngar i legacy-kod, inte aktiva beroenden.

Rekommendation:

- besluta: behåll och döp om, eller migrera bort
- minska teknisk skuld och förvirring i GUI-lagret

### 6) Deprecated API kvar i brukbar kod  ÅTGÄRDAD (2026-05-29)

Flera klasser/metoder är markerade `@Deprecated` (bl.a. datum- och kontogruppslogik).

Status:

- Inventering genomförd av aktiva anrop i `src/main/java` för kända V2-deprecated API-spår.
- Inga aktiva anrop hittades till `SSAccountGroupMath` eller legacy-domänklasserna
  `SSProject`/`SSResultUnit` i brukbar produktionskod.
- `SSDateChooser` har nu tydligt markerade `@Deprecated` Date-accessorer
  (`getDate`/`setDate`) för att styra ny kod mot `LocalDate`.
- `SSDateCellEditor` migrerad till `getLocalDate`/`setLocalDate` för att undvika
  fortsatt användning av Date-baserat GUI-API.
- Riktad sweep av kvarvarande `@Deprecated`-spår i `src/main/java` genomförd.
- TODO-matris per klass och planerad borttagning per release dokumenterad i
  `DEPRECATED_API_TODO_MATRIX.md`.
- Hård CI-gate tillagd i `.github/workflows/ci.yml`:
  `mvn clean -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.failOnWarning=true compile`
  (Linux-jobb), vilket blockerar nya compiler-varningar i main-kod.

Rekommendation (fortsatt):

- inventera faktiska anrop
- planera borttagning per release
- lås ny kod till modern API-variant

### 7) `equals()`/`hashCode()`-kontrakt bör ses över

Det finns klasser med egen `equals()` där `hashCode()`-konsistens behöver verifieras.

Rekommendation:

- gör en systematisk genomgång
- lägg till tester för likhet/hash i kritiska domänobjekt

### 8) Struktur- och kvalitetsregler kan skärpas

`checkstyle` körs men är inte blockerande (`failOnViolation=false`).

Rekommendation:

- överväg blockerande regler i CI för ny kod
- eventuellt gradvis införande med baseline

## Död kod och förenklingar

Bedömning:

- det finns tydliga kandidater till död/legacy-kod (särskilt gamla modellspår och "Old"-klasser)
- oanvända beroenden (Spring) är en konkret förenkling att göra direkt
- stora utility-/manager-klasser bör delas upp för enklare testning och underhåll

Föreslagen strategi:

1. börja med lågrisk-förbättringar (beroenden, naming, tydliga deprecated-anrop)
2. fortsätt med resurs- och felhanteringsstandardisering
3. ta större refaktorering i inkrementella steg per domän

## Prioriterad åtgärdslista (V2)

1. Ta bort oanvända Spring-beroenden från `pom.xml` (om verifierat säkert).
2. Uppgradera sårbara beroenden där fix finns (`jasperreports`, `assertj-core`).
3. Sätt plan för `itext`-migrering pga kvarvarande CVE-risk.
4. Standardisera SQL-kod till `try-with-resources`.
5. Påbörja uppdelning av `SSDB` i repository/service-klasser.
6. Definiera och exekvera avvecklingsplan för legacy-modeller (`SSProject` m.fl.).
7. Besluta framtid för `SSVoucherRowTableModelOld`.
8. Skärp CI-regler gradvis (checkstyle/spotbugs/test quality gates).

## Slutsats

V2 är på rätt väg, men projektet bär fortfarande tydlig historisk teknisk skuld. Den snabbaste nyttan fås genom:

- dependency hygiene (säkerhet + städning)
- mindre koppling mellan lager (DB/GUI)
- kontrollerad avveckling av legacy-kod

Detta ger lägre risk, bättre testbarhet och enklare vidareutveckling inför kommande releaser.
