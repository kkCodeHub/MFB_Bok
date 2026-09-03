# Genomförandeplan: Kontrollerad seedning i multischema

## 1. Mål och avgränsning

### Mål
- Seedning av demodata ska endast kunna ske vid **första uppstart av en tom databas**.
- Seedning ska alltid ske i **co_0** när den väl sker.
- Aktivt företag (`is_active`) ska endast sättas till `co_0` i detta första-uppstart-fall.

### Icke-mål
- Ingen ändring av affärsdata i befintliga företagsscheman.
- Ingen automatisk migrering av historiska backupformat.

---

## 2. Problembild som ska lösas

Nuvarande beteende kopplar seed-kontroll till aktuellt schema (current schema), vilket gör att:
- seedning kan triggas i fel schema (exempelvis aktivt `co_2`) om demoföretaget saknas där,
- aktivt företag kan ändras oväntat vid fallback/bootstrapping.

Rotorsak är att seed-villkor inte är knutna till global databasstatus i `PUBLIC`, utan till schemakontext.

---

## 3. Ny målarkitektur för seed-trigger

### Princip
Inför en tydlig, global och idempotent seed-gate som läser från `PUBLIC` och aldrig från current schema.

### Init-definition (global)
Seed-init körs endast när `PUBLIC.tbl_company_catalog` innehåller 0 rader.

Vid denna init-situation gäller:
1. Om `co_0` redan finns: droppa `co_0`.
2. Skapa `co_0` på nytt.
3. Kör seed i `co_0`.
4. Skapa katalograd för `co_0` och sätt `is_active=TRUE`.
5. Sätt seed-markör (boolean) i `PUBLIC`.

### Seed-beteende
Vid init (katalog tom):
1. Hantera `co_0` enligt init-definitionen ovan.
2. Sätt schema explicit till `co_0`.
3. Kör demo-seed.
4. Skapa katalograd och sätt `is_active=TRUE`.
5. Skriv seed-markör (boolean) i `PUBLIC`.

Vid icke-init (katalog har rader):
- Seedning hoppas över helt.
- Aktivt schema styrs enbart av befintlig katalogstatus.

---

## 4. Konkreta implementationsteg

## Steg A: Centralisera seed-beslut
- Skapa en enda beslutsväg för seedning i startup-flödet.
- Avveckla direkta kontroller som läser `tbl_company` i aktuellt schema för att avgöra seed.
- Säkerställ att backup/restore-flöde inte återanvänder startup-seedbeslut okontrollerat.

### Leveranskrav
- Endast en kodväg får kunna trigga seed.
- Beslutet ska bygga på globala PUBLIC-kriterier.

## Steg B: Inför seed-markör i PUBLIC
- Lägg till metadata-tabell i `PUBLIC` (om sådan inte redan finns) för seed-state.
- Skriv markör efter lyckad seed-transaktion.
- Läs markör innan seedbeslut.

### Leveranskrav
- Seedning är idempotent över upprepade starter.
- Markör skrivs i samma transaktion som seed/kataloginit.

## Steg C: Tvinga schema-binding till co_0
- Kör seed-script endast med explicit schema `co_0`.
- Förhindra beroende av current schema vid seed.

### Leveranskrav
- Seed kan aldrig gå till `co_2`, `co_3`, etc. av misstag.

## Steg D: Aktivt företag-regler
- Sätt `is_active=TRUE` för `co_0` endast i init-fallet (katalog tom).
- I icke-init: respektera befintlig aktiv rad.
- Om aktiv rad saknas i icke-init: lämna “ingen aktiv” och kräv användarval (ingen automatisk aktivering).

### Leveranskrav
- Aktivt företag ändras inte som sidoeffekt av seedlogik i befintlig databas.

## Steg E: Skydd i restore/backup-kedja
- Efter restore: klassificera DB som befintlig om katalog/scheman finns.
- Förhindra att startup tolkar återställd databas som fresh p.g.a. lokala saknade företagsrader i aktivt schema.
- Om katalogen är tom efter restore: init-regeln gäller (återskapa `co_0`, seeda, aktivera `co_0`).

### Leveranskrav
- Restore av en DB med flera företag ger ingen ny demo-seed.

---

## 5. Testplan (måste vara grön innan release)

## A. Nya/uppdaterade integrationstester
1. **Fresh startup**  
   - Katalog tom och `co_0` saknas => `co_0` skapas, seed körs en gång, `co_0` aktiv.
2. **Init med befintlig `co_0` men tom katalog**  
   - `co_0` droppas, skapas på nytt, seed körs i ny `co_0`, `co_0` aktiv.
3. **Restart same DB**  
   - Ingen ny seed, data oförändrad.
4. **Multi-schema existing DB**  
   - Aktivt `co_2`, demo finns i `co_0` => ingen seed, aktivt förblir `co_2`.
5. **Multi-schema där demo saknas i aktivt schema**  
   - Ingen seed får köras, eftersom katalogen inte är tom.
6. **Restore-scenario med befintlig katalog + flera scheman**  
   - Ingen seed vid efterföljande startup.
7. **Katalog saknar aktiv rad (icke-init)**  
   - Lämna ingen aktiv rad, kräv användarval, ingen seed.
8. **Seed-markör TRUE + katalog tom**  
   - Init-regeln styr ändå (katalog tom vinner), `co_0` hanteras enligt initflöde.

## B. Regressionskontroller
- Företagsbyte (`activateAndApplySchema`) påverkar inte seedbeslut.
- Backup/export påverkar inte `is_active`.

---

## 6. Observability och felsökning

- Lägg tydliga loggrader för:
  - seed-gate input (katalograder, `co_0`-existens, markörstatus),
  - beslut: `SEED_EXECUTED` eller `SEED_SKIPPED`,
  - orsakskod (ex. `INIT_CATALOG_EMPTY`, `SKIP_CATALOG_NOT_EMPTY`, `INIT_CO0_RECREATED`).
- Logga schema som seed faktiskt körs mot (ska alltid vara `co_0`).

---

## 7. Säker releaseordning

1. Implementera seed-gate + boolean-markör + schema-binding.
2. Lägg/uppdatera integrationstester.
3. Kör full testsvit.
4. Verifiera manuellt med scenario från buggrapporten (3 företag, aktivt `co_2`, backup, restart).
5. Uppdatera changelog med beteendeförändring och tydlig regel: seed sker endast på fresh DB.

---

## 8. Risker och mitigering

- **Risk:** Felaktig fresh-detektering vid delvis korrupt databas.  
  **Mitigering:** Låt tom katalog vara enda init-trigger, och logga explicit när `co_0` återskapas.

- **Risk:** Oavsiktlig ändring av aktivt företag vid recovery.  
  **Mitigering:** Isolera recovery-logik från seedlogik och testa explicit.

- **Risk:** Miljöer utan metadata-tabell efter uppgradering.  
  **Mitigering:** Skapa tabellen idempotent i `PUBLIC` via startup DDL.

---

## 9. Fastställda beslut

1. I icke-init där aktiv rad saknas: lämna “ingen aktiv” och kräv användarval.
2. Seed-markör implementeras som enkel boolean (`seed_done`).
3. Om katalog är tom:
   - finns `co_0`: droppa `co_0`, skapa `co_0` på nytt, seeda till `co_0`,
   - saknas `co_0`: skapa `co_0`, seeda till `co_0`.

---

## 10. Definition of Done

- Seedning sker endast när katalogen är tom (init-fall).
- Seedning sker alltid i `co_0`.
- `is_active` ändras inte i befintliga databaser som sidoeffekt av startup/backup/restore.
- Testfall ovan passerar.
- Dokumentation/changelog uppdaterad.
