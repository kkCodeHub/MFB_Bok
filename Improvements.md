# Fribok — Förbättringslista (ny genomgång 2026-09-08)

Den här listan ersätter tidigare version och prioriterar kvarvarande arbete utifrån nuvarande kodläge.

## 🔴 Kritiska problem

### 1. API-servern har osäker standardautentisering och loggar inkommande API-nyckel
`CompanyApiServer` startar med hårdkodad fallback-nyckel (`minhemliganyckel`) när `FRIBOK_API_KEY` saknas, och `CompanyApiHandler` loggar dessutom inkommande `X-API-Key` i klartext.

**Rekommenderad åtgärd**
- Ta bort fallback-nyckeln och kräv explicit konfiguration i alla miljöer.
- Sluta logga hemliga headers; maskera eller utelämna `X-API-Key` helt.

### 2. SpotBugs kör nu, men visar stor faktisk buggskuld
`mvn spotbugs:check` fungerar i nuvarande miljö, men rapporten innehåller **347 fynd**. De största grupperna är `MS_SHOULD_BE_FINAL` (**286**), `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` (**33**) och `HE_EQUALS_USE_HASHCODE` (**24**).

**Rekommenderad åtgärd**
- Prioritera `equals/hashCode`-bristerna först eftersom de riskerar fel i samlingar och identitetsjämförelser.
- Gör en kontrollerad plan för att minska muterbar global/static state, med start i `SSDB` och GUI-ramklasser.

---

## 🟠 Höga problem

### 3. Checkstyle-skuld är fortfarande mycket stor
`mvn checkstyle:checkstyle` ger **2468 varningar**. Största grupperna är oanvända importer (**593**), wildcard-importer (**567**), radlängd (**544**), whitespace-problem (**172**) och saknade `default` i `switch` (**143**).

**Rekommenderad åtgärd**
- Rensa i batchar per paket i stället för filvis.
- Börja med filer som koncentrerar många fel, särskilt `SSAccountdiagramPrinter`, `SSMainMenu`, `SSReportFactory` och `SSSchemaBuilder`.

### 4. SSReportFactory är fortfarande en god-klass
`SSReportFactory` är fortfarande **1972 rader** lång och har kvar tomma villkor som `if (isProjectSelected) {}` och `if (isDateSelected) {}`.

**Rekommenderad åtgärd**
- Dela upp rapportskapandet per rapportdomän eller utskriftstyp.
- Ta bort tomma villkor och extrahera gemensamma dialog-/previewflöden.

### 5. SSDB har kvar stark GUI- och global-state-koppling
`SSDB` är fortfarande **1791 rader** lång, importerar GUI-klasser (`SSMainFrame`, `SSErrorDialog`) och skriver fel direkt till dialoglager. SpotBugs visar också flera skrivningar till statiskt tillstånd från instansmetoder.

**Rekommenderad åtgärd**
- Flytta användarfeedback till ett notifieringslager utanför databasklassen.
- Minska statiskt delat tillstånd och låt beroenden injiceras tydligare.

### 6. SSInvoice.generateVoucher() blandar fortfarande domän, kontext och presentation
`SSInvoice.generateVoucher()` hämtar fortfarande bundle-text, global kontext och bokföringsdata direkt i samma metod.

**Rekommenderad åtgärd**
- Flytta voucher-byggandet till en dedikerad tjänst med explicita inparametrar.
- Låt modellen beskriva data, inte slå upp runtime-kontext själv.

---

## 🟡 Medelprioritet

### 7. Breda `catch (Exception)` finns kvar i produktionskod
Det finns fortfarande träffar i bland annat `SSDB`, `V2CompanyRepository`, `AccountPlanSnapshot`, `SSVoucherExporter` och `SSSupplierExporter`.

**Rekommenderad åtgärd**
- Smalna av till specifika undantag per kodväg.
- Dokumentera vilka fel som är förväntade respektive programmeringsfel.

### 8. Aktiv kommentar-/underhållsskuld finns kvar
Det finns fortfarande **9** `TODO`/`FIXME`/`XXX`-markeringar i `src/main/java`.

**Rekommenderad åtgärd**
- Koppla varje markering till issue eller åtgärda direkt.
- Avsluta med att förbjuda nya ospårade flaggor i huvudkod.

### 9. Oanvänd kod finns kvar
`SSResourceBundle` saknar fortfarande referenser i produktionskod och tester.

**Rekommenderad åtgärd**
- Ta bort klassen om den inte längre behövs.
- Annars återintroducera den via tydlig och testad användning.

---

## 🟢 Lågprioritet

### 10. Projektroten innehåller fortfarande manuella artefakter
Projektroten innehåller fortfarande `checkstyle_fresh.log` och `Hypercare_checklista.txt`, vilket skapar brus i repot.

**Rekommenderad åtgärd**
- Flytta till extern arbetsyta eller sessionsartefakter.
- Uppdatera `.gitignore` om den typen av filer ska fortsätta skapas lokalt.

---

## Åtgärdade eller förbättrade punkter jämfört med tidigare lista

- SpotBugs är inte längre blockerad av Java-versionen i nuvarande miljö; analysen kör och producerar rapport.
- Kvarvarande `TODO/FIXME/XXX`-skuld är nu liten i absoluta tal (**9** träffar), även om den fortfarande bör stängas.
- Projektroten innehåller färre tillfälliga text/loggfiler än tidigare; kvar är två tydliga manuella artefakter.
- Tidigare förbättringar kring serialisering, V2-migrering och bundle-flyttar gäller fortfarande och behöver inte längre prioriteras i denna lista.
