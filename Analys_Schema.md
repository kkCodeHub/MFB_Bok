# Analys: Företag per eget schema (utan `tbl_company` i `PUBLIC`)

## 1. Syfte

Ta fram en genomförbar målbild och genomförandeplan för att köra varje företag i eget databasschema, med:

- ingen migrering av gamla databaser
- ingen `tbl_company` i `PUBLIC`
- strikt isolering per företag
- delade referenstabeller kvar i `PUBLIC`

## 2. Fastslagna förutsättningar

1. Vid skapande av företag ska nytt schema skapas.
2. `schema_name` ska vara `co_` + löpnummer, typ `VARCHAR(20)`.
3. Programmet genererar `schema_name`.
4. `schema_name` ändras inte vid namnbyte på företag.
5. Vid tom databas ska Demoföretag skapas i eget schema.
6. Följande ligger kvar i `PUBLIC`:
   - Category A-tabeller
   - `tbl_license`
   - `tbl_accountplan`
   - `tbl_accountplan_account` (kvar tills vidare, används inte aktivt)
7. `PUBLIC` ska ha `tbl_company_catalog` med:
   - `catalog_id` (IDENTITY/PK)
   - `schema_name` (VARCHAR, unik)
   - `company_name` (VARCHAR)
   - `is_active` (BOOLEAN)
8. `company_name` i katalogen ska synkas med `tbl_company.name`.
9. Vid varje upptäckt av namn-avvikelse mellan katalog och lokal `tbl_company` ska varning visas i UI.
10. Aktivt företag styrs av aktivt schema (`SET SCHEMA ...`).
11. När företag tas bort ska schema tas bort och katalog uppdateras.
12. Ingen åtgärd för att hindra äldre programversioner ska införas, men formatdetektion ska ge fel och avbryt.
13. `tbl_company_autoincrement` ligger kvar i företagsschemat.

## 3. Målarkitektur (databas)

## `PUBLIC` (globalt)

- Delade referens-/malltabeller:
  - `tbl_currency`, `tbl_unit`, `tbl_deliveryway`, `tbl_deliveryterm`, `tbl_paymentterm`
  - `tbl_license`
  - `tbl_accountplan`, `tbl_accountplan_account`
- `tbl_company_catalog`:
  - `catalog_id` (IDENTITY/PK)
  - `schema_name` (unik)
  - `company_name` (för visning/val)
  - `is_active` (0..1 aktiva rader tillåts, dvs ingen aktiv är tillåten)

## Företagsschema `<schema_name>`

- Innehåller `tbl_company` + övriga företagsspecifika tabeller från V2 company-del.
- Inga data för andra företag får finnas där.

## 4. Konsekvenser av att `tbl_company` inte finns i `PUBLIC`

- Företagslistning/startval läser från `PUBLIC.tbl_company_catalog`.
- Efter val av företag:
  - `SET SCHEMA <schema_name>`
  - därefter används lokala tabeller i valt företagsschema.
- Namnkonsekvens mellan katalog och lokal `tbl_company` måste kontrolleras vid definierade punkter.

## 5. Flöden som måste stödjas

## 5.1 Uppstart mot tom databas

1. Skapa `PUBLIC`-struktur (delade tabeller + katalogtabell).
2. Importera standardkontoplan i `PUBLIC` vid behov.
3. Skapa katalograd för Demoföretag (`schema_name = co_<n>`).
4. Skapa företagsschema.
5. Skapa företagstabeller i företagsschemat.
6. Skriv `tbl_company` i företagsschemat.
7. Synkkontroll `catalog.company_name == tbl_company.name`.
8. Sätt `is_active = TRUE` för Demoföretag.
9. `SET SCHEMA` till Demoföretagets schema.
10. Kör demo-seed i aktivt företagsschema.

## 5.2 Skapa företag

1. Användare fyller företagsdialog (`tbl_company`-uppgifter).
2. Skapa katalograd i `PUBLIC.tbl_company_catalog` med `company_name`.
3. Generera `schema_name = co_<löpnummer>`.
4. Skapa schema `<schema_name>`.
5. Skapa företagstabeller i schemat.
6. Skriv företagets data i lokal `tbl_company`.
7. Synkkontroll namn mellan katalog och lokal `tbl_company`.
8. Sätt aktiv-rad enligt UX-regel.
9. Vid fel: kör explicit cleanup i `catch` (schema/katalog).

## 5.3 Aktivera företag

1. Läs vald rad från `tbl_company_catalog`.
2. Nollställ tidigare aktiv-rad (eller lämna alla FALSE om inget val görs).
3. `SET SCHEMA <schema_name>`.
4. Sätt vald rad `is_active = TRUE`.
5. Kontrollera namn-synk och visa UI-varning vid avvikelse.
6. Om ingen aktiv finns: visa företagsval vid startup.

## 5.4 Ta bort företag

1. Läs `schema_name` från katalograd.
2. Kontrollera/varna om namn-synk avviker.
3. `DROP SCHEMA <schema_name> CASCADE`.
4. Ta bort katalograd.
5. Om borttaget företag var aktivt: sätt ingen aktiv (alla `is_active = FALSE`).

## 6. Kodytor som påverkas

## 6.1 SQL-resurser

- `create_tables_v2_Public.sql`
  - lägg till `tbl_company_catalog` + unika/index/regel för aktivrad.
- `create_tables_v2_Company.sql`
  - ska fortsatt innehålla `tbl_company` + företagstabeller.
- `seed_v2_demo.sql`
  - ska köras i aktivt företagsschema.

## 6.2 Schema-bootstrapping

- Dela ansvar i separat PUBLIC-init och COMPANY-init.
- `SSSchemaEnsurer` delas enligt vald modell (Public/Company).
- Idempotens kvarstår som krav.

## 6.3 Startup och aktivt företag

- `SSDB.startupLocal`, `createNewTables`, `seedDemoDataIfNeeded`,
  `initializeCurrentCompanyAndYear`, `setCurrentCompany`.
- Ny ordning: init `PUBLIC` -> läs katalog -> företagsval vid behov ->
  `SET SCHEMA` -> företagsladdning.

## 6.4 Företagsrepository

- `V2CompanyRepository` ska hantera:
  - katalog + schema-livscykel
  - lokalt `tbl_company` i aktivt schema
  - explicit cleanup vid create-fel
  - delete via `DROP SCHEMA CASCADE` + katalogstädning

## 6.5 Övriga repositoryn

- Delade tabeller nås uttryckligen via `PUBLIC`.
- Företagsspecifika repositoryn kör mot aktivt schema.

## 7. Testpåverkan

- Inför `CompanySchemaFixture` för schema-aware testsetup.
- Uppdatera tester som idag gör direkt `INSERT INTO tbl_company` i defaultschema.
- Lägg till tester för:
  - create/activate/delete
  - startup med ingen aktiv rad
  - namn-synkvarning i UI vid varje avvikelse
  - cleanup vid DDL/DML-fel
  - korrekt `SET SCHEMA` vid byte

## 8. Risker och designbeslut (punkt 3–7 fördjupat)

1. **SSSchemaEnsurer (vald: Förslag A)**  
   Två separata flöden/instanser: PUBLIC och COMPANY. All körning schema-styrs explicit.

2. **Aktiv-flagga (vald: Förslag B)**  
   Triggerbaserad hantering i katalogen för att säkra max en aktiv rad när aktiv sätts.  
   Tillståndet "ingen aktiv" ska vara tillåtet.

3. **Transaktionsgränser DDL/DML (vald: Förslag A)**  
   `V2CompanyRepository.add()` ansvarar för create-proceduren. Vid fel används explicit cleanup i `catch` och användaren får info att försöka igen.

4. **SSSchemaBuilder (vald: Förslag A + C)**  
   Ansvar delas upp för att minimera spridning i SSDB och centralisera schema-livscykel.

5. **Triggers vid create/delete av schema (vald: Förslag C med förtydligande)**  
   Alla triggers behandlas som en enhet och skapas samtidigt via befintlig metodkedja i Java.  
   Ingen uppdelning i statiska/dynamiska i implementationen.

## 9. Genomförandeordning

## Fas A: Databasmodell

1. Lås slutlig definition av `tbl_company_catalog`.
2. Uppdatera SQL-init för `PUBLIC` (katalog + regler).
3. Verifiera att COMPANY-skript innehåller rätt företagstabeller.

## Fas B: Startup/Bootstrap

1. Implementera ny startsekvens med katalogval och schemaaktivering.
2. Implementera fallback: ingen aktiv => visa företagsval.
3. Detektera gammalt format och avbryt med felmeddelande.

## Fas C: Företagslivscykel

1. Create: katalog -> schema -> tabeller -> lokal `tbl_company` -> synkkontroll -> aktiv.
2. Activate: kataloguppdatering + `SET SCHEMA`.
3. Delete: `DROP SCHEMA CASCADE` + katalogstädning + ingen aktiv fallback.
4. Implementera explicit cleanup i create/delete-felvägar.

## Fas D: Repository och servicegränser

1. Avgränsa PUBLIC/COMPANY i repository-anrop.
2. Flytta schema-livscykel till central orchestration/service.
3. Minimera direktkoppling i SSDB.

## Fas E: Triggerstrategi

1. Behåll triggerhantering i Java (`createLocalTriggers`/`dropTriggers`).
2. Flytta inte triggers till SQL.
3. Alla triggers skapas samtidigt i samma metodflöde.
4. `dropTriggers()` behålls.

## Fas F: Testanpassning

1. Inför `CompanySchemaFixture`.
2. Uppdatera berörda integration/repositorytester.
3. Lägg till regressionsfall för schema-livscykel och felåterställning.

## 10. Acceptanskriterier

1. Ny tom DB skapar Demoföretag i eget schema.
2. `PUBLIC` innehåller inte `tbl_company`.
3. Alla företagsspecifika data ligger i respektive schema.
4. Delade tabeller ligger i `PUBLIC`.
5. Företagsskapande ger `schema_name = co_<n>` i katalog.
6. Företagsborttagning droppar schema och städar katalog.
7. Företagsbyte sätter aktivt schema korrekt.
8. Ingen aktiv rad hanteras via företagsval i startup.
9. Testsvit uppdaterad och grön för ny arkitektur.

## 11. Avgränsning

- Ingen migrering av gamla databaser.
- Ingen fallback till gammal single-schema-modell.

## 12. Optioner efter genomförd plan

1. SIE-import: verifiera aktivt schema före import och trigger/cachescope per företag.
2. Backup: säkerställ att alla företagsscheman + `PUBLIC` följer med.
3. Restore: validera katalog kontra faktiska scheman efter återläsning.
4. Startup health check: kontrollera schema-existens, aktivrad (0..1), namn-synk.
5. Trigger-idempotens: robust hantering om trigger redan finns/saknas.
6. Delete-robusthet: extra verifiering av cleanup vid avbrott.
