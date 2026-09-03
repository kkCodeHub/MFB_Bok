# Detaljerad genomförandeplan: Multi-schema (företag per schema)

## 1. Syfte och mål

Detta dokument konkretiserar implementationen av multi-schema enligt:

- `Analys_Schema.md` (gällande styrdokument)
- `ANALYS_SCHEMA_PER_FORETAG_UTAN_PUBLIC_COMPANY.md`
- `Analys_schema_kommentarer.md`
- `Analys_Schema_Comment.md` (historik/underlag)

Målet är att införa företag-per-schema utan migrering av gamla databaser, med `tbl_company_catalog` i `PUBLIC`, och `tbl_company` i respektive företagsschema.

---

## 2. Fastlåsta designbeslut (ska inte omtolkas under implementation)

1. `tbl_company` ligger **inte** i `PUBLIC`.
2. `PUBLIC` innehåller delade tabeller + `tbl_company_catalog`.
3. `schema_name` = `co_` + löpnummer, `VARCHAR(20)`, genereras av programmet.
4. `schema_name` ändras inte vid namnbyte.
5. `is_active` är korrekt kolumnnamn överallt.
6. Tillståndet “ingen aktiv” (alla `is_active = FALSE`) är tillåtet.
7. Om ingen aktiv finns vid start: visa företagsval i UI.
8. Vid namnavvikelse mellan katalog och lokal `tbl_company.name`: visa varning i UI **varje gång** avvikelsen upptäcks.
9. `tbl_company_catalog.company_name` är styrande (källvärde i katalogen).
10. `tbl_company_autoincrement` ligger kvar i företagsschemat.
11. Triggerhantering stannar i Java (`createLocalTriggers`/`dropTriggers`), alla triggers skapas samlat.
12. Ingen migrering av gamla databaser; gammalt format detekteras och avbryts med felmeddelande.

---

## 3. Scope

## In scope

- SQL-struktur för katalogtabell i `PUBLIC`
- startup-/bootstrapflöde för tom och befintlig DB
- schema-livscykel (create/activate/delete)
- repository-anpassning för PUBLIC vs company-schema
- explicit felåterställning (cleanup) vid DDL/DML-fel
- testanpassning + nya testfall för multi-schema

## Out of scope

- migrering av befintliga gamla databaser
- fallback till single-schema-modell
- omskrivning av triggerarkitektur till SQL-baserad modell

---

## 4. Målarkitektur (operativt)

1. `PUBLIC`:
   - delade tabeller (`Category A`, `tbl_license`, `tbl_accountplan`, `tbl_accountplan_account`)
   - `tbl_company_catalog(catalog_id, schema_name, company_name, is_active)`
2. Företagsschema (`co_<n>`):
   - `tbl_company`
   - alla företagsspecifika V2-tabeller
3. Aktivt företag:
   - bestäms via katalogens `is_active`
   - realiseras via `SET SCHEMA <schema_name>`

---

## 5. Implementationsfaser med leverabler

## Fas 0 – Förberedelse och baseline

### Aktiviteter
1. Lås branch/arbetsgren för multi-schema.
2. Baslinjekörning: `mvn clean install` för nuläge.
3. Inventera exakta anropspunkter för:
   - `SSDB.startupLocal`, `createNewTables`, `initializeCurrentCompanyAndYear`
   - `V2CompanyRepository`
   - `SSSchemaBuilder`, `SSSchemaEnsurer`

### Leverabler
- Baseline-logg (lokalt arbetsunderlag)
- Lista över berörda klasser och testklasser

### Exit-kriterium
- Baseline passerad och påverkningslista fastställd.

---

## Fas 1 – SQL och datamodell i PUBLIC

### Aktiviteter
1. Inför `tbl_company_catalog` i `create_tables_v2_Public.sql`.
2. Sätt constraints/index:
   - PK på `catalog_id`
   - unik `schema_name`
   - index för snabb lookup av `is_active` och `company_name`
3. Säkerställ att `create_tables_v2_Company.sql` fortsätter skapa `tbl_company` i aktivt schema.
4. Säkerställ att `seed_v2_demo.sql` inte förutsätter `PUBLIC.tbl_company`.

### Leverabler
- Uppdaterad public/company SQL-init

### Exit-kriterium
- Tom DB kan initiera både PUBLIC-struktur och company-struktur utan SQL-fel.

---

## Fas 2 – Schema-bootstrapping och ansvarsfördelning

### Aktiviteter
1. Dela initansvar till PUBLIC-init respektive COMPANY-init.
2. Säkerställ att `SSSchemaEnsurer` alltid kör i avsett schema (PUBLIC eller valt företagsschema).
3. Behåll idempotens i schema-init/ensure-flöden.

### Leverabler
- Tydlig initsekvens med schema-styrning

### Exit-kriterium
- Upprepad startup på samma DB är stabil och idempotent.

---

## Fas 3 – Startupflöde och aktivt företag

### Aktiviteter
1. Ny startupordning:
   - init PUBLIC
   - läs katalog
   - om ingen aktiv: visa företagsval i UI
   - sätt aktiv och `SET SCHEMA`
2. Inför formatdetektion av gammal DB och avbryt med tydligt fel.
3. Namn-synkkontroll vid öppning:
   - upptäckt avvikelse => UI-varning varje gång

### Leverabler
- Uppdaterat startupflöde med robust företagval

### Exit-kriterium
- Startup fungerar för:
  - tom DB
  - DB med aktivt företag
  - DB utan aktivt företag
  - gammalt format (avbryts korrekt)

---

## Fas 4 – Företagslivscykel (create/activate/delete)

### Aktiviteter
1. **Create företag** (`V2CompanyRepository.add()` ansvarig):
   - skapa katalograd (`company_name`, `schema_name`)
   - skapa schema
   - skapa company-tabeller i schema
   - skapa lokal `tbl_company`
   - sätt `is_active` enligt UI-regel
   - `SET SCHEMA` till aktivt företag
2. **Activate företag**:
   - nollställ tidigare aktivrad(er)
   - sätt vald rad `is_active = TRUE`
   - `SET SCHEMA`
   - namn-synkkontroll + UI-varning vid avvikelse
3. **Delete företag**:
   - namn-synkkontroll + UI-varning vid avvikelse
   - `DROP SCHEMA <schema_name> CASCADE`
   - radera katalograd
   - om aktivt företag togs bort: lämna ingen aktiv
4. **Felåterställning/cleanup**:
   - explicit cleanup i `catch` vid create/delete-delsteg
   - användarinfo: försök skapa företaget igen

### Leverabler
- Full schema-livscykel med kompensationslogik

### Exit-kriterium
- Create/activate/delete fungerar repeterbart utan orphan state.

---

## Fas 5 – Triggerflöde

### Aktiviteter
1. Behåll triggerflöde i Java.
2. Säkerställ att triggers skapas samtidigt via befintligt samlat metodflöde.
3. Behåll `dropTriggers()`.
4. Verifiera triggerflöde vid:
   - startup utan DB
   - import (SIE)
   - restore

### Leverabler
- Stabilt triggerflöde utan uppdelning statisk/dynamisk

### Exit-kriterium
- Triggerflöde fungerar i alla definierade startvägar.

---

## Fas 6 – Repository-anpassning (PUBLIC vs company)

### Aktiviteter
1. Säkerställ att delade repositoryn arbetar explicit mot `PUBLIC`.
2. Säkerställ att företagsspecifika repositoryn använder aktivt schema.
3. Minska direktberoenden i SSDB genom centraliserad schema-orchestrering.

### Leverabler
- Repositorylager med tydlig schema-separation

### Exit-kriterium
- CRUD i delade och företagsspecifika domäner fungerar utan schemasammanblandning.

---

## Fas 7 – Testanpassning och regressionspaket

### Aktiviteter
1. Skapa/inför `CompanySchemaFixture`.
2. Uppdatera tester som gör `INSERT INTO tbl_company` i defaultschema.
3. Lägg till tester för:
   - startup med/utan aktiv rad
   - create/activate/delete
   - namn-synk UI-varning vid varje avvikelse
   - cleanup vid fel under create/delete
   - korrekt `SET SCHEMA` vid företagsbyte
4. Kör full testsvit.

### Leverabler
- Uppdaterad testsvit för multi-schema

### Exit-kriterium
- Relevanta testsviter gröna.

---

## 6. Detaljerad arbetsordning (beroenden)

1. Fas 1 måste vara klar före Fas 3 och 4.
2. Fas 2 måste vara klar före Fas 3.
3. Fas 3 måste vara klar före full validering av Fas 4.
4. Fas 4 och Fas 6 kan delvis parallelliseras efter att Fas 2 är klar.
5. Fas 7 görs iterativt men avslutas sist.

---

## 7. DoD per delområde

## Databas
- Katalogtabell finns och fungerar.
- Schema skapas/raderas korrekt.

## Runtime
- Aktivt schema sätts konsekvent vid företagsbyte/startup.

## Datakonsistens
- Nammansynk kontrolleras i open/delete och varning visas i UI varje gång avvikelse upptäcks.

## Felhantering
- Inga halvskapade företag lämnas kvar efter fel.

## Test
- Nya och uppdaterade tester gröna.

---

## 8. Testmatris (minimikrav)

1. Startup tom DB => Demoföretag + aktivt schema sätts.
2. Startup med 1 aktiv => öppnar utan valdialog.
3. Startup utan aktiv => valdialog visas.
4. Create företag => schema + tabeller + katalograd + `tbl_company`.
5. Create avbryts mitt i => cleanup fungerar.
6. Activate företag => `is_active` uppdateras + `SET SCHEMA`.
7. Delete aktivt företag => schema droppas + ingen aktiv kvar.
8. Name mismatch open/delete => UI-varning visas varje gång.
9. Triggerflöde efter startup/import/restore => fungerar.
10. Full regression i repositorylager => ingen cross-schema data.

---

## 9. Riskhantering under implementation

1. **DDL/DML-fel i create-flöde**  
   Motåtgärd: explicit cleanup och tydlig användarinformation.

2. **Fel schema aktivt vid repository-anrop**  
   Motåtgärd: central schema-orchestrering + testfall för `SET SCHEMA`.

3. **Namn-synk driver isär**  
   Motåtgärd: transaktionsuppdatering av båda namn + UI-varning vid avvikelse.

4. **Stora testanpassningar**  
   Motåtgärd: gemensam fixture först, därefter bulk-uppdatering av tester.

---

## 10. Milstolpar

1. **M1:** SQL-modell och bootstrap klara.
2. **M2:** Startup + aktivt företag klart.
3. **M3:** Create/activate/delete klart med cleanup.
4. **M4:** Repository-separation klar.
5. **M5:** Testsvit uppdaterad och stabil.
6. **M6:** Go/No-Go för implementation i huvudgren.

---

## 11. Optioner efter genomförd plan

1. Fördjupad health-check vid startup (katalog/schema-konsistens).
2. Utökad verifiering för backup/restore-kedjan.
3. Förstärkt SIE-importvalidering av aktivt schema.
4. Extra idempotenshärdning i trigger/create/drop-sekvenser.

