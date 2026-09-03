# Bedömning av upplägget

## Generellt omdöme
Planen är välstrukturerad och genomtänkt. Fas-indelningen är logisk och acceptanskriterierna är tydliga. Databasdelningen i `create_tables_v2_Public.sql` och `create_tables_v2_Company.sql` är redan gjord på SQL-nivå — det är en bra grund. Det saknas dock `tbl_company_catalog` i Public-filen ännu, så det steget återstår.

---

## Positiva styrkor i upplägget

- **`DROP SCHEMA CASCADE`** vid borttagning av företag är elegant — ersätter den nuvarande långa delete-kedjan i `V2CompanyRepository` (rad 296–306) med ett enda kommando.
- Att låta aktivt schema styra SQL-routing utan att behöva ändra frågor i repositories är en stor fördel — befintliga `SELECT * FROM tbl_voucher`-frågor fungerar oförändrade.
- Ingen migrering av gamla databaser förenklar implementationen avsevärt.

---

## Risker och observationer

**1. Numeriska schemanamn i HSQLDB kräver konsekvent quoting**
HSQLDB tillåter numeriska schemanamn men de måste alltid citeras: `SET SCHEMA "1"`, `CREATE SCHEMA "1"`, `DROP SCHEMA "1" CASCADE`. Ett enda ställe som glömmer citattecken ger ett svårspårat fel. Överväg om ett prefix som `co_1`, `co_2` vore säkrare och mer läsbart i loggar och debuggning.

**2. Testkodsomfattningen är mycket stor**
Sökning visar att minst **35 testklasser** gör direkta `INSERT INTO tbl_company(name) VALUES (?)` utan schema-hantering. Dessutom kontrollerar `SSSchemaBuilderTest` och `SSSchemaInitializationIntegrationTest` explicit att `tbl_company` existerar i defaultschemat. Testanpassningen (Fas E) är sannolikt det mest tidskrävande steget.

**3. `SSSchemaEnsurer` vet inte vilket schema den är i**
Idag kör `SSSchemaEnsurer` `ensureColumnExists("tbl_company", ...)` med okvalificerade tabellnamn. Med schema-per-företag måste ensurer veta om den opererar mot PUBLIC eller mot ett företagsschema — annars riskerar den att köra mot fel schema beroende på vad `SET SCHEMA` råkar vara satt till vid tillfället.

**4. Aktiv-flagga — constraint saknas**
Planen nämner att en constraint behövs för "högst en aktiv post", men `tbl_company_catalog` är ännu inte skapad. I HSQLDB kan detta lösas med ett partiellt index eller en trigger. Hur det implementeras bör beslutas tidigt eftersom det påverkar activate-flödet (Fas C).

**5. Transaktionsgränser vid schema-skapande**
DDL (CREATE SCHEMA, CREATE TABLE) i HSQLDB är inte transaktionsskyddat på samma sätt som DML. Om ett fel uppstår efter `INSERT` i katalogen men innan alla företagstabeller skapats kan man få en katalogpost utan tillhörande schema. En explicit städrutin eller kompensationslogik behövs.

**6. `SSSchemaBuilder` anropas på tre ställen i SSDB**
Raderna 138, 2246, 2259 och 2271 i `SSDB.java` anropar `SSSchemaBuilder` direkt. Med den nya arkitekturen behöver dessa anrop delas upp i public-init och company-init — men SSDB är redan en God-klass. Risk för att förändringar sprider sig okontrollerat.

**7. Trigger-hantering**
Planen nämner triggers som en risk. Idag skapas triggers av `SSSchemaBuilder`. Med schema-per-företag måste triggers skapas per företagsschema vid schema-skapande och droppas med `DROP SCHEMA CASCADE`. Kontrollera att `SSEventTriggerSyncContext.dropTriggers()` (som kallas i Rensa transaktioner-flödet) fungerar korrekt med schema-routing.

---

## Förslag

- **Namnge katalogtabellen** `tbl_company_directory` istället för `tbl_company_catalog` för att tydligare signalera att det är en katalog/förteckning, inte ett konfigurationsobjekt.
- **Lägg till `company_name` i katalogtabellen** (planen anger det redan) men säkerställ att den hålls synkroniserad med `tbl_company.name` i företagsschemat — annars visar uppstartslistan inaktuella namn.
- **Skapa ett dedikerat `SchemaLifecycleService`** som äger CREATE/DROP/SET SCHEMA-operationerna, istället för att sprida det i SSDB och V2CompanyRepository.
- **Skriv en `CompanySchemaFixture`** för testmiljön som ersätter alla direkta `INSERT INTO tbl_company` — annars riskerar testkoden att bli inkonsekvent under migreringen.

---

## Frågor

1. Ska `tbl_accountplan` och `tbl_accountplan_account` i PUBLIC vara skrivskyddade för företagsscheman (enbart mallar), eller ska företag kunna modifiera dem? Det påverkar om foreign keys från företagstabeller till dessa är möjliga.

2. Är det tänkt att ett HSQLDB-fil-format används (single-user, lokalt) eller en server-instans? Det påverkar om `SET SCHEMA` per connection är hanterbart vid eventuell framtida flertrådig åtkomst.

3. Hur hanteras `tbl_company_autoincrement` (fakturanummer, ordernummer etc.) i det nya upplägget — ligger den i företagsschemat som idag, eller finns det en risk att nummerserien krockar om schema-id används som primärnyckel i PUBLIC-katalogen?

4. Vad händer om databasen öppnas av en äldre version av programmet som inte känner till schema-per-företag-modellen? Ska det finnas någon versionsmarkering i PUBLIC för att blockera det?

5. Planen anger att schemanamnet ska vara numeriskt och motsvara företagets ID. Vad händer om ett företag raderas och ett nytt skapas — återanvänds det gamla schema-id:t, eller är ID:n alltid sekventiellt ökande (IDENTITY)?
