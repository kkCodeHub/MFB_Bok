# Copilot Start Context (2026-05-20)

Detta dokument sammanfattar aktuell projektstatus for snabb uppstart i en ny tråd.

## 1) Kort lägesbild

- Projektet är i en **mycket dirty worktree** med många pågående ändringar (kod, docs, test, datafiler).
- JasperReports har testats i 7.x under sessionen men är nu **rollbackad till 6.21.4**.
- Rapportcache-logik är förenklad: rapporter kompileras nu alltid från `.jrxml` i runtime (ingen disk-cache skrivs/läses).

## 2) Verifierat gjort i denna session

### JasperReports

- `pom.xml` använder nu:
  - `net.sf.jasperreports:jasperreports:6.21.4`
  - `net.sf.jasperreports:jasperreports-fonts:6.21.4`
- 7.x-specifikt dependency (`jasperreports-spring`) är borttaget.
- `src/main/java/se/swedsoft/bookkeeping/print/SSReport.java` är återställd till 6.x-anrop:
  - `band.getSplitTypeValue()`
- `src/main/resources/jasperreports_extension.properties` är återställd till 6.x-klass:
  - `net.sf.jasperreports.extensions.SpringExtensionsRegistryFactory`

### Rapportcache

- `src/main/java/se/swedsoft/bookkeeping/print/util/SSReportCache.java`:
  - `loadCompiledReport()` och `saveCompiledReport()` borttagna.
  - Laddning/sparning av prekompilerade `.jasperreport` används inte längre.
  - Rapport kompileras alltid från classpath-resurs (`/reports/report/*.jrxml`) och hålls i minnescache under körning.

### Verifiering körd

- Kompilering verifierad efter rollback:

```powershell
Set-Location "E:\FB_update\fribok-master3\fribok-master"
mvn -q -DskipTests compile
```

- Resultat: gick igenom utan kompileringsfel (endast generella Java/Maven-varningar i terminalen).

## 3) Rapportmallar / användning (kartlagt)

- Aktiva mallar laddas från `src/main/resources/reports/report/` (classpath), inte från `data/report/`.
- `vatreport2015.jrxml` används via `SSVATReport2015Printer` och menyaction `reportmenu.vatreport2015`.
- `vatreport2007.jrxml` och `vatreport.jrxml` ser ut att vara legacy i nuvarande flöde (ingen aktiv menykoppling hittad).

## 4) 7.x-teststatus (för historik)

- 7.x migration testades tillfälligt.
- En konverterad mall (`balance.jrxml`) kunde kompileras i testspår under 7.x.
- Icke-konverterade DTD-mallar (ex. `vatreport2015.jrxml`) gav `JRException: Unable to load report` i 7.x.
- Därefter gjordes rollback till 6.21.4.

## 5) Kvar att göra / planerat

### A. Om ni vill fortsätta på 6.21.4 (stabilt spår)

1. Behåll nuvarande dependency-läge (6.21.4).
2. Kör normal regressionskontroll för rapporter (UI-smoke).
3. Besluta om `data/report/compiled/` ska städas i repo (de används inte längre av runtime-koden).

### B. Om ni vill återuppta 7.x-spår senare

1. Skapa separat branch (rekommenderat).
2. Uppgradera dependencies till 7.x.
3. Konvertera `.jrxml` stegvis i `src/main/resources/reports/report/`.
4. Testa varje mall visuellt + compile-smoke per mall.
5. Mergas först när kritiska rapporter är verifierade.

## 6) Viktiga filer för nästa tråd

- `pom.xml`
- `src/main/java/se/swedsoft/bookkeeping/print/util/SSReportCache.java`
- `src/main/java/se/swedsoft/bookkeeping/print/SSReport.java`
- `src/main/resources/jasperreports_extension.properties`
- `src/main/resources/reports/report/` (aktiva jrxml)
- `data/report/` (legacy/dubletter + gamla compiled-filer)

## 7) Notering om repository-status

- Repon har ett stort antal redan existerande lokala ändringar i många moduler.
- Använd selektiva commits och undvik breda "commit all" för att inte blanda orelaterade spår.

