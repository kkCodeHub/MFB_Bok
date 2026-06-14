# Risk- och beroendelista per sprint (utan kod)

Detta dokument kompletterar:
- `DECIMAL_QUANTITY_PLAN.md`
- `DECIMAL_QUANTITY_SPRINT_CHANGE_LIST.md`

Syftet är att tydliggora risker, beroenden och vad som maste vara klart innan nasta sprint.

## Sprint 1 - Domankontrakt, format och validering

### Beroenden

- Beslutade regler ar lasta:
  - intern modell `int*10`
  - minsta steg `0,1`
  - visning alltid 1 decimal
  - pris fortsatt 2 decimaler med `HALF_UP`
  - produktflagga **hela antal endast**
- Gemensam tolkning av vad "quantity/count/change_qty" betyder i respektive flode.

### Huvudrisker

- **Semantisk sammanblandning**: vissa falt heter `count`, andra `quantity`.
- **Skugglogik i flera klasser**: lokala formatterings-/valideringsregler riskerar divergera.
- **Berakningsregression**: totalsummor kan bli fel om antal i tiondelar behandlas som heltal.

### Motatgarder

- Definiera ett centralt antal-kontrakt (in/ut/lagring) som alla team foljer.
- Identifiera alla berakningspunkter i `*Math`-klasser och markera riskniva.
- Skriv tidiga godkannandescenarier for kritiska affarsfall (faktura/kredit/lager).

### Exit-kriterier

- Ett forankrat kontraktsdokument finns och ar signerat av produkt/teknik.
- Alla kritiska berakningspunkter ar inventerade.

---

## Sprint 2 - Databas och persistens (V2)

### Beroenden

- Sprint 1-kontraktet ar faststallt.
- Migreringsstrategi ar vald (engangsmigrering eller overgangsperiod).

### Huvudrisker

- **Datamigreringsfel**: missad `*10` i en eller flera tabeller.
- **Inkonsekvent lagring**: vissa tabeller lagrar gamla enheter, andra nya.
- **Kompatibilitetsrisk**: gamla backups/importer kan antas vara i heltal.
- **Tyst datakorruption**: data ser rimlig ut men betyder fel skala.

### Motatgarder

- Tabell-for-tabell migreringschecklista med verifieringsfraga per tabell.
- Pre/post-kontroller med sample-data och totalsummeringar.
- Pilotmigrering pa kopia av produktionsdata innan releasefonster.

### Exit-kriterier

- Alla berorda tabeller verifierade med pre/post-resultat.
- Persistenslager (`SSDB`) har enhetlig skala i read/write-paths.

---

## Sprint 3 - UI/skarmar och anvandarvalidering

### Beroenden

- Databas/persistens ar stabil i testmiljo.
- Gemensamma formatterings-/valideringsregler fran Sprint 1 anvands.

### Huvudrisker

- **UI-inkonsistens**: vissa skarmar visar 1 decimal, andra inte.
- **Valideringsglapp**: "hela antal endast" blockeras i ett flode men inte i annat.
- **Konverteringsfel vid editering**: 2,5 visas korrekt men sparas som 2 eller 25 felaktigt.
- **Lagerfel i edge cases**: negativa saldon + decimalantal hanteras olika i dialoger.

### Motatgarder

- En gemensam UI-spec med exempel pa giltiga/ogiltiga inmatningar.
- Cross-screen testmatris (faktura, kredit, retur, order, lager, inventering).
- Valideringsmeddelanden standardiseras sa anvandaren forstar orsaken.

### Exit-kriterier

- Alla berorda skarmar passerar samma valideringsmatris.
- Antal visas konsekvent med exakt 1 decimal overallt.

---

## Sprint 4 - Rapporter, utskrifter och integrationer

### Beroenden

- UI och datamodell ar stabila.
- Beslutad regel for frakt (enhetsfrakt vs manuell frakt) ar slutligt dokumenterad.

### Huvudrisker

- **Rapportavvikelse**: utskrift visar annan skala an GUI.
- **Fysikberakning**: vikt/volym multipliceras med fel enhetsskala.
- **Integrationsbrott**: importer/exporter tolkar antal som gamla heltal.

### Motatgarder

- Golden-sample dokument (faktura, kredit, foljesedel, plocklista) med forvantat utfall.
- Separata testfall for fraktregeln (enhetsfrakt aktiv/inaktiv).
- Versionering eller tydlig release-notis for integrationsformat.

### Exit-kriterier

- Rapporter matchar GUI-varden for samma affarsfall.
- Import/export klarar decimalantal utan manuell efterkorrigering.

---

## Sprint 5 - Migrering, regression och release

### Beroenden

- Samtliga foregaende sprintar ar godkanda i test/stage.
- Go/No-Go-kriterier ar beslutade i forvag.

### Huvudrisker

- **Releasefonsersrisk**: migrering tar langre tid an planerat.
- **Driftregression**: dolda floden (sallan anvanda) bryts efter release.
- **Supportbelastning**: anvandare upplever att antal "ser annorlunda" ut.

### Motatgarder

- Tidsatt torrkorning av releaseplan inklusive rollback-plan.
- Prioriterad regressionssvit for kritiska floden dag 0-2 efter release.
- Kort anvandarinformation om 1-decimalvisning och nya produktregeln.

### Exit-kriterier

- Go/No-Go-checklista uppfylld.
- Hypercare-plan (ansvariga, tider, uppfoljning) ar aktiverad.

---

## Tvargaende beroenden (alla sprintar)

- **Datakontrakt**: en och samma definition av antal genom hela kedjan.
- **Beslutslogg**: alla regelbeslut dokumenteras med datum och agare.
- **Testdata**: gemensamma testfall med decimaler (`0,1`, `1,0`, `2,5`, negativa lagerfall).
- **Observabilitet**: kontroller/loggning som snabbt visar fel skala i produktion.

## Tvargaende riskindikatorer att folja veckovis

- Antal buggar kopplade till quantity/count/change_qty.
- Andel UI-skarmar klara enligt valideringsmatris.
- Andel rapportmallar validerade mot golden samples.
- Antal migreringsavvikelser i pilotkorning.

