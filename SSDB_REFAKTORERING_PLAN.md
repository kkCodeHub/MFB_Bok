# SSDB refaktorering - genomforandeplan

## Ramar och avgransning
- Ordning ar fast och ska foljas exakt:
  1. Mapper-metoder
  2. Schema/migrationslogik (med tester)
  3. Saldo-delta-logik
  4. triggerAction-uppdelning
  5. Session/state-manager
- Delmomenten genomfors separat (en i taget, egen verifiering, egen PR).
- Mapper-metoder ska ligga i `V2RepositoryHelpers` (nu public).
- SQL/script/seed-hjalpmetoder ligger kvar i `SSDB`.
- Ingen funktionsandring far introduceras oavsiktligt.

---

## Delmoment 1: Mapper-metoder (forst)

### Mal
Flytta och samla mappningsansvar fran `SSDB` till `V2RepositoryHelpers` utan beteendeforandring.

### Delsteg
1. Inventera alla `map*`-metoder och nara helper-metoder i `SSDB`.
2. Gruppera mapper-floden per entitet (Company, AccountingYear, osv).
3. Flytta en entitetsgrupp i taget till `V2RepositoryHelpers`.
4. Uppdatera anrop i `SSDB` till nya publika helper-metoder.
5. Ta bort dublettlogik i `SSDB` efter varje flytt.
6. Verifiera efter varje gruppflytt med compile + relevanta tester.

### Leverabler
- `SSDB` innehaller inte langre entitetsmappning som tillhor helpers.
- `V2RepositoryHelpers` ar enda plats for dessa mappers.

### Klar-kriterier
- Samma testutfall som fore flytt.
- Inga nya varningar/fel i berorda filer.

---

## Delmoment 2: Schema/migrationslogik (med tester)

### Mal
Separera schema/migrationsansvar fran `SSDB` till dedikerad komponent, men behall SQL/script/seed-hjalpmetoder i `SSDB`.

### Delsteg
1. Avgransa vilka metoder som ar migration/schema (ej seed/script).
2. Skapa ny struktur for schema/migration (klass/klasser) och definiera tydligt API.
3. Flytta migration i liten batch:
   - 3.1 tabell-/kolumn-sakerstallning
   - 3.2 index/constraint
   - 3.3 versions-/idempotenslogik
4. Lat `SSDB` endast orkestrera anrop till nya komponenten.
5. Behall `executeSqlScriptResource` och ovriga SQL/script/seed-hjalpmetoder i `SSDB`.
6. Lagga till/uppdatera tester for:
   - idempotens (kor migration flera ganger)
   - startup pa tom databas
   - startup pa redan migrerad databas
   - bakatkompatibla scenarier enligt befintliga testfixtures
7. Ko full testsvit for berorda integrationstester.

### Leverabler
- Schema/migration ar brutet ut.
- SQL/script/seed-hjalpmetoder kvar i `SSDB`.
- Tester som verifierar migrationssakerhet.

### Klar-kriterier
- Migration passerar pa ny och befintlig DB.
- Regressionsfritt i integrationstester.

---

## Delmoment 3: Saldo-delta-logik

### Mal
Flytta saldo-delta-ansvar fran `SSDB` till tydlig doman-/berakningskomponent.

### Delsteg
1. Identifiera all saldo-delta-logik och alla anropspunkter.
2. Definiera ny ansvarspunkt (t.ex. kalkyl/service) med tydlig in-/utdata.
3. Flytta logiken stegvis med adapter i `SSDB` under overgangen.
4. Ersatt direkta uppdateringar i `SSDB` med anrop till ny komponent.
5. Verifiera edge cases:
   - partiella betalningar
   - kreditfakturor
   - borttag/andring av betalning
   - nyskapad faktura

### Leverabler
- `SSDB` innehaller inte affarsregler for saldo-delta.
- Enhetlig saldo-uppdateringsvag.

### Klar-kriterier
- Saldo-kolumner och saldo-relaterade floden beter sig oforandrat.
- Relevanta tester passerar.

---

## Delmoment 4: triggerAction-uppdelning

### Mal
Dela upp stor triggerhantering i mindre och testbara delar.

### Delsteg
1. Kartlagg triggerfall och kategorisera (faktura, betalning, lager, UI-refresh, etc.).
2. Skapa dispatcher-struktur med separata handlers per kategori.
3. Migrera triggerfall i batcher (en kategori i taget).
4. Verifiera efter varje batch med funktionella regressionstester.
5. Behall fallback/loggning under overgangen for enklare felsokning.

### Leverabler
- `triggerAction` reducerad till routing/delegering.
- Handler-klasser med tydligt ansvar.

### Klar-kriterier
- Samma handelser ger samma utfall som tidigare.
- Inga tappade UI-/cache-refresh-signaler.

---

## Delmoment 5: Session/state-manager (sist)

### Mal
Flytta company/year-session och cache-invalidation till dedikerad manager.

### Delsteg
1. Inventera globalt tillstand och cachelivscykel i `SSDB`.
2. Definiera `SessionStateManager`-kontrakt:
   - current company
   - current year
   - cache clear/invalidate
   - reload-strategi
3. Flytta read-paths for tillstand forst, write-paths sedan.
4. Flytta invalidation stegvis med verifiering per flode.
5. Avsluta med att tunna ut `SSDB` till orkestrering.

### Leverabler
- Session/state ansvaret ligger utanfor `SSDB`.
- Tydlig och centraliserad invalidation.

### Klar-kriterier
- Inga stale-cache symptom i UI/floden.
- Stabilt beteende vid byte av bolag/ar.

---

## Genomforandemodell (gemensam for alla 5)
- En del i taget (ingen overlap mellan delmoment).
- Egen branch/PR per delmoment.
- Verifiering per delmoment:
  - compile
  - riktade tester
  - relevanta integrationstester
- Dokumentera risker och fallback i varje PR.
- Fortsatt till nasta delmoment forst efter godkant resultat.

## Riskstyrning
- Hogst regressionsrisk: delmoment 4 och 5.
- Medelrisk: delmoment 2 och 3.
- Lagst risk: delmoment 1.
- Vid avvikelse: stoppa, aterga till senaste stabila steg och korrigera innan fortsattning.

## Beslut som galler i hela planen
- Mapper-metoder centraliseras i `V2RepositoryHelpers`.
- SQL/script/seed-hjalpmetoder ligger kvar i `SSDB`.
- Ingen kodandring utanfor aktivt delmoment.

