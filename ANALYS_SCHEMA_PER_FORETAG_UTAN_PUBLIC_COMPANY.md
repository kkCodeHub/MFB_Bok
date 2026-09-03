# Analys: Företag per eget schema (utan `tbl_company` i `PUBLIC`)

## 1. Syfte

Ta fram en genomförbar målbild och ändringsanalys för att köra varje företag i eget databasschema, med:

- ingen bakåtkompatibilitet/migrering av gamla databaser
- ingen `tbl_company` i `PUBLIC`
- strikt isolering per företag
- befintlig tabell-design i övrigt bevarad

## 2. Fastslagna förutsättningar

1. Vid skapande av företag ska nytt schema skapas.
2. Schemanamn ska bestå av co_ följt av de fyra första boksäverna i företagets namn som uppercase. Andra tecken är bokstäver ska bortses från. Svenska tecken ÅÄÖ ska omvandlas till AAO. Om skapat schemanamn redan finns ska ett löpnummer läggas till (_x). Om företagsnamnet inte innhåller fyra bokstäver sker utfyllnad med bokstaven X.
3. Vid uppstart mot tom databas ska Demoföretag skapas i eget schema.
4. Följande ska ligga kvar i `PUBLIC` och delas av alla företag:
   - Category A-tabeller
   - `tbl_license`
   - `tbl_accountplan`
   - `tbl_accountplan_account`
5. `PUBLIC` ska ha en katalogtabell (tbl_company_catalog) som håller kolumner (catalog_id (integer), schema_name (varchar), company_name(varchar), is_activ (Boolean)). company_name ska var synkad med tbl_company.name i respektive förtags schema.
6. Aktivt företag styrs av aktivt schema (`SET SCHEMA ...`).
7. När företag tas bort ska företagets schema tas bort samt tbl_company_catalog uppdateras.
8. Ingen migrering av befintliga databaser krävs.

## 3. Målarkitektur (databas)

## `PUBLIC` (globalt)

- Delade referens-/malltabeller:
  - `tbl_currency`, `tbl_unit`, `tbl_deliveryway`, `tbl_deliveryterm`, `tbl_paymentterm`
  - `tbl_license`
  - `tbl_accountplan`, `tbl_accountplan_account`
- Ny katalogtabell, t.ex. `tbl_company_catalog`:
  - `catalog_id` (IDENTITY/PK)
  - `schema_name` (varchar)
  - `company_name` (varchar) (för visning vid uppstart/val, synkat mot tbl_company.name)
  - `is_active` (endast en rad aktiv åt gången)

## Företagsschema `<schema_name>`

- Innehåller `tbl_company` + övriga företagsspecifika tabeller från company-delen av V2.
- Inga data för andra företag får ligga här.

## 4. Konsekvenser av att `tbl_company` inte finns i `PUBLIC`

- All logik som idag förutsätter global `tbl_company` måste flyttas till:
  1. katalog i `PUBLIC` för bootstrap/listning/val
  2. aktivt företagsschema för faktisk företagsdata
- Listning av företag i UI/startläge ska läsa från katalogtabellen i `PUBLIC`, inte från `tbl_company`.
- När ett företag aktiveras:
  - `SET SCHEMA` till schema_name
  - därefter kan `tbl_company` läsas lokalt i aktivt schema.

## 5. Flöden som måste stödjas

## 5.1 Uppstart mot tom databas

1. Skapa `PUBLIC`-struktur (delade tabeller + katalogtabell).
2. Importera standardkontoplan i `PUBLIC` vid behov.
3. Skapa katalograd för Demoföretaget i tbl_company_catalog.
4. Skapa schema för Demoföretag.
5. Skapa företagstabeller i schemat för Demoföretaget.
6. Sätt aktiv-rad i katalogen.
7. `SET SCHEMA` till Demoföretagets schema.
8. Kör demo-seed mot aktivt schema.

## 5.2 Skapa företag

1. Lägg in rad i katalogtabellen (`PUBLIC`) och skapa schema_name.
2. Skapa schema `<schema_name>`.
3. Skapa företagstabeller i det schemat.
4. Sätt schema aktivt och skriv företagets `tbl_company` i företagsschemat.
5. Skriv tbl_company.name till tbl_company_catalog.company_name.
6. Återställ/behåll aktivt schema enligt vald UX-regel.

## 5.3 Aktivera företag

1. Läs `company_name` från katalogtabellen i `PUBLIC`.
2. Nollställ tidigare aktiv-rad.
3. Kör `SET SCHEMA` till valt schema_name.
4. Uppdatera is_active för företaget till true.

## 5.4 Ta bort företag

1. Läs `company_name` från katalogtabellen.
2. Kör `DROP SCHEMA <schema_name> CASCADE`.
3. Ta bort katalograd.
4. Om borttaget företag var aktivt: välj fallback-företag eller markera inget aktivt.

## 6. Kodytor som påverkas

## 6.1 SQL-resurser

- `create_tables_v2_Public.sql`
  - kompletteras med katalogtabellen + constraints för “en aktiv”.
- `create_tables_v2_Company.sql`
  - ska innehålla `tbl_company` och övriga företagstabeller.
- `seed_v2_demo.sql`
  - får inte skapa företag i `PUBLIC.tbl_company`; ska köras i aktivt företagsschema.

## 6.2 Schema-bootstrapping

- `SSSchemaBuilder`
  - idag laddas `create_tables_v2.sql` (allt i ett).
  - behöver delas upp i public-init + company-init per schema.
- `SSSchemaEnsurer` / `SSSchemaMigrationManager`
  - kontrollera vilka “ensure”-regler som hör till `PUBLIC` respektive företagsschema.
  - ingen legacy-migrering behövs, men idempotens behövs fortsatt.

## 6.3 Startup och aktivt företag

- `SSDB.startupLocal`, `createNewTables`, `seedDemoDataIfNeeded`, `initializeCurrentCompanyAndYear`, `setCurrentCompany`
  - ny ordning: init `PUBLIC` → katalogval → `SET SCHEMA` → företagsladdning.
  - `setCurrentCompany` ska alltid koppla till `SET SCHEMA`.

## 6.4 Företagsrepository

- `V2CompanyRepository`
  - kan inte längre använda `tbl_company` globalt.
  - ska hantera katalog + schema-livscykel + lokalt `tbl_company` i aktivt schema.
  - delete-flöde förenklas till schema-drop istället för lång delete-kedja.

## 6.5 Övriga repositoryn

- Repositoryn med delade tabeller (`Category A`, `tbl_accountplan*`) ska uttryckligen arbeta mot `PUBLIC`.
- Företagsspecifika repositoryn kan fortsätta med okvalificerade tabellnamn när aktivt schema är satt korrekt.

## 7. Testpåverkan

Stor påverkan i testkod:

- Fixtures som idag gör `INSERT INTO tbl_company(name)` i defaultschema måste ändras till katalog + schema-create + `SET SCHEMA`.
- Integrationstester som antar enskilt schema (`PUBLIC`) måste uppdateras.
- Nya tester behövs för:
  - katalogens aktiv-rad
  - schema-skap vid add
  - schema-drop vid delete
  - demo-start från tom DB
  - korrekt `SET SCHEMA` vid byte

## 8. Risker och viktiga designbeslut

1. **Numeriska schemanamn**  
   Kräver konsekvent quoting/hantering i SQL-utförande och helpermetoder.
   Rättning: Schemanamn ska inte vara numeriska. Ska vara av typen VARCHAR.

2. **Transaktionsgränser över DDL + DML**  
   Fel under företagsskapande får inte lämna halvt skapad katalog/scheman.
   Komplettering: Städrutin, kompensationslogik eller liknande ska finnas.

3. **Aktiv-flagga i katalog**  
   Constraint behövs så högst en aktiv post kan finnas.

4. **Triggerhantering**  
   Triggers måste finnas i rätt schema och skapas idempotent.

5. **Felåterställning**  
   Skapa företag/delete företag måste vara robusta vid avbrutna operationer.

## 9. Rekommenderad genomförandeordning (detaljplan-underlag)

## Fas A: Databasmodell

1. Slå fast slutlig katalogtabell i `PUBLIC`.
2. Dela SQL-init i public/company och säkerställ idempotens.

## Fas B: Startup/Bootstrap

1. Implementera ny startsekvens för tom DB.
2. Säkerställ demo-skapande i företagsschema.

## Fas C: Företagslivscykel

1. Implementera add: katalog → schema → company-tabeller → lokal `tbl_company`.
2. Implementera activate: katalog + `SET SCHEMA`.
3. Implementera delete: `DROP SCHEMA CASCADE` + katalogstädning.

## Fas D: Repository-anpassning

1. Separera delade vs företagsspecifika repositoryn.
2. Säkerställ att delade tabeller alltid nås via `PUBLIC`.

## Fas E: Testanpassning

1. Uppdatera fixtures.
2. Uppdatera befintliga integration/repositorytester.
3. Lägg till nya tester för schema-livscykel.

## 10. Acceptanskriterier

1. Ny tom DB startar och skapar Demoföretag i eget schema.
2. `PUBLIC` innehåller inte `tbl_company`.
3. Alla företagsspecifika data ligger i respektive företagsschema.
4. Delade tabeller ligger endast i `PUBLIC`.
5. Företagsskapande skapar schema med namn = ID.Rättelse: schemanan = tbl_company_catalog.schema_name.
6. Företagsborttagning droppar företagsschemat.
7. Företagsbyte sätter aktivt schema korrekt.
8. Testsvit uppdaterad och grön för ny arkitektur.

## 11. Avgränsning

- Ingen migrering av gamla databaser.
- Ingen fallback till gammal single-schema-modell.
