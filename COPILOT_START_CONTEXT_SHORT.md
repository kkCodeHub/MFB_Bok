# Copilot Start Context - Short (2026-05-20)

Snabb one-pager for ny tråd eller statusgenomgang.

## Nulage

- Projektet har manga parallella andringar (dirty worktree).
- JasperReports ar tillbaka pa **6.21.4** (rollback klar och verifierad).
- Rapportcache ar forenklad: rapporter kompileras alltid fran `.jrxml` i runtime.

## Gjort

- Aterstallt JasperReports till 6.21.4 i `pom.xml`:
  - `jasperreports:6.21.4`
  - `jasperreports-fonts:6.21.4`
- Tagit bort 7.x-specifikt dependency (`jasperreports-spring`).
- Aterstallt 6.x-kompatibilitet i:
  - `src/main/java/se/swedsoft/bookkeeping/print/SSReport.java`
  - `src/main/resources/jasperreports_extension.properties`
- Rensat `SSReportCache`:
  - inga disk-cacheade `.jasperreport` laddas/sparas langre
  - endast kompilering fran classpath-resurser

## Verifiering

Kord och godkand:

```powershell
Set-Location "E:\FB_update\fribok-master3\fribok-master"
mvn -q -DskipTests compile
```

## Rapportstatus (viktig)

- Aktiva mallar laddas fran `src/main/resources/reports/report/`.
- `data/report/` ser ut att vara legacy/dublett for flera mallar.
- `vatreport2015` ar aktiv i nuvarande menyflode.
- `vatreport2007` och `vatreport` ser ut att vara legacy i nuvarande flode.

## Risker just nu

- Stor mangd orelaterade lokala andringar i repot gor commits kansliga.
- Rapportmallar i blandat format (aldre DTD + nyare Studio-format) okar migreringsrisk till 7.x.

## Rekommenderad nasta plan

1. Fortsatt stabilt pa 6.21.4 och gor rapport-smoke i UI.
2. Hall 7.x-migrering i separat branch.
3. Konvertera jrxml stegvis och verifiera per rapport (kompilering + visuell kontroll).
4. Anvand selektiva commits (inga breda "commit all").

## Startfiler i nasta trad

- `COPILOT_START_CONTEXT.md` (full)
- `COPILOT_START_CONTEXT_SHORT.md` (denna)
- `pom.xml`
- `src/main/java/se/swedsoft/bookkeeping/print/util/SSReportCache.java`
- `src/main/java/se/swedsoft/bookkeeping/print/SSReport.java`
- `src/main/resources/jasperreports_extension.properties`

