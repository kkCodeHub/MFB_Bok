# Fribok — Förbättringslista (ny genomgång 2026-09-05)

Den här listan ersätter tidigare version och prioriterar kvarvarande arbete.

## 🔴 Kritiska problem

### 1. SpotBugs är inte körbar i nuvarande miljö
`mvn spotbugs:check` faller med `Unsupported class file major version 69` (Java 25), vilket gör att den statiska buggrapporteringen saknas helt.

**Rekommenderad åtgärd**
- Lås analyskörning till JDK 21 (samma som projektets mål), eller
- uppgradera SpotBugs/ASM-kedjan så Java 25 stöds.

---

## 🟠 Höga problem

### 2. SSReportFactory är fortfarande en god-klass
Klassen är fortsatt mycket stor och bär både UI-flöden och rapportlogik. Checkstyle-varningar och tomma if-satser finns kvar (t.ex. `if (isProjectSelected) {}` och `if (isDateSelected) {}`).

**Rekommenderad åtgärd**
- Dela upp i mindre factories per rapportdomän.
- Ta bort tomma villkor och extrahera gemensam boilerplate.

### 3. SSDB har kvar lagerkoppling mot GUI
`SSDB` importerar fortfarande GUI-klasser (`SSMainFrame`, `SSErrorDialog`), vilket försvårar ren testbarhet och separering av datalager.

**Rekommenderad åtgärd**
- Flytta GUI-återkoppling till notifierings-/eventlager utanför `SSDB`.

### 4. SSInvoice.generateVoucher() blandar domän och infrastruktur
Metoden är stor och hämtar både GUI-resurs (`SSBundle`) och global kontext (`SSAccountingContext`, `SSCompanyYearContext`) direkt.

**Rekommenderad åtgärd**
- Flytta voucher-byggandet till dedikerad tjänst med inparametrar i stället för global state.

### 5. Breda `catch (Exception)` finns kvar i produktionskod
Det finns kvar i bland annat `SSDB`, `V2CompanyRepository`, `AccountPlanSnapshot`, `SSVoucherExporter`, `SSSupplierExporter`.

**Rekommenderad åtgärd**
- Smalna av till specifika undantag per kodväg.

---

## 🟡 Medelprioritet

### 6. Stor stilskuld i imports och formattering
Kodbasen har mycket wildcard-importer (567 träffar i `src/main/java`) samt flera checkstyle-varningar (line length, namngivning, tabbar, trailing whitespace).

**Rekommenderad åtgärd**
- Rensa per paket i batchar (börja med `print/*` och `util/*` där varningar koncentreras).

### 7. Mycket stora kärnklasser bör brytas upp stegvis
Exempel: `SSReportFactory`, `SSDB`, `V2CompanyRepository`, `SSInvoicePanel`.

**Rekommenderad åtgärd**
- Sätt maxstorlek per klass/metod för ny kod och bryt upp de största i kontrollerade delsteg.

### 8. Aktiv TODO/FIXME-skuld finns kvar
Kommentarflaggor (`TODO/FIXME/XXX`) finns fortfarande i huvudkod.

**Rekommenderad åtgärd**
- Koppla varje flagga till issue eller åtgärda direkt.

---

## 🟢 Lågprioritet

### 9. Oanvänd kod kan städas
`SSResourceBundle` har inga referenser i kod eller tester.

**Rekommenderad åtgärd**
- Ta bort eller återintroducera den via tydlig användning.

### 10. Projektroten innehåller temporära analys-/loggfiler
Flera `*.txt`/`*.log` i roten är historiska körresultat och skapar brus.

**Rekommenderad åtgärd**
- Arkivera utanför repo eller rensa och uppdatera `.gitignore`.

---

## Åtgärdade eller förbättrade punkter jämfört med tidigare lista

- Tidigare hårdkodad svensk fallbacktext i `SSInvoice` är flyttad till bundle-nyckel.
- Området kring V2-migrering har avancerat enligt `CHANGELOG.md` (flera legacy-shims och migreringssteg slutförda).
