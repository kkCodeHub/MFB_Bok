Här följer svar på frågor och förslag samt kommentarer till Risker och observationer som finns i filen Analys_Schema_Comment.md.
Analysera dessa samt vad som anges i filerna Analys_Schema_Comment.md och ANALYS_SCHEMA_PER_FORETAG_UTAN_PUBLIC_COMPANY.md.
Sammanställ sedan en ny analysfil Analys_Schema.md enligt samma upplägg som ANALYS_SCHEMA_PER_FORETAG_UTAN_PUBLIC_COMPANY.md.
Ge mer detaljerat förslag på de frågeställningar som ges nedan under Risker och Observationer punkt 3 till 7.
Innan sammaställningen görs så ställ kompletterande frågor och ge eventuella kompletterande förslag till lösning.
Skapa ingen kod.

Svar på Frågor i Analys_Schema_Comment.md:
1. Poster i `tbl_accountplan` i PUBLIC ska kunna hanteras enligt de funktioner som finns i menyn för dialogrutan där inlästa kontoplaner finns. `tbl_accountplan_account` i PUBLIC används inte längre men finns kvar oanvänd tills vidare. 
2. Det är tänkt att ett HSQLDB-fil-format ska användas (single-user, lokalt) inte som en server-instans.
3. `tbl_company_autoincrement` (fakturanummer, ordernummer etc.) ska ligga i företagsschemat som idag i det nya upplägget, schema-id ska inte finnas.
4. Ingen hänsyn eller åtgärd ska tas eller implementeras för att hindra en äldre version av programmet att försöka ansluta till databasen.
5. Schemanamnet ska inte längre vara numeriskt och inte motsvara företagets ID.

Svar på Förslag i Analys_Schema_Comment.md:
Tabellen ska heta tbl_company_catalog. Tabellens innehåll finns nu beskrivet i ANALYS_SCHEMA_PER_FORETAG_UTAN_PUBLIC_COMPANY.md.
Kolumnen company_name är tillagt tbl_company_catalog och ska vara synkad med tbl_company.name.
En `CompanySchemaFixture`** för testmiljön kommer med stor annolikhet att tas fram.

Risker och Observationer i Analys_Schema_Comment.md:
1. Schemanamnen ska inte längre vara numeriska.
2. Testfiler behöver skapas.
3. Ge förslag på hur frågan runt SSSchemaEnsurer ska hanteras och lösas.
4. Ge förslag på hur frågan runt Aktiv-flagga ska hanteras, partiellt index, en trigger eller annat?
5. Ge förslag på hur frågan kan lösas, explicit städrutin, kompensationslogik eller annat?
6. Ge förslag på hur frågan runt SSSchemabuilder kan lösas utan att risk för att förändringar sprider sig okontrollerat uppstår.
7. Ge förslag på hur frågan runt triggers kan hanteras när scheman för företag skapas och deletas.

Kontrollera svaren nedan och återkom med eventuellt ytterligare frågor och förslag. Vänta sedan innan den nya anlysen skrivs.
Svar kompletterande frågor:
A.1 Ändrad strategi. Schema_name ska nu sättas till co_ plus ett löpnummer. Programmet genererar namnet. När man valt att skapa nytt företag så visas dialogrutam för tbl_company och där anges föetagsnamnet. Vid submit skapas post i tbl_company_catalog där också company_name sätts till vad företaget är namngivet till. Efter det skapas schema med namn enligt tbl_company_catalog.schema_name. Efter att schemat skapats läggs alla företagsuppgifter in i tbl_company som har skapats i schemat. Därefter sätts is_activ i tbl_company_catalog. Kontroll görs då också att tbl_company_catalog.company_name = tbl_company.name. Kontroll tbl_company_catalog.company_name = tbl_company.name ska sedan göras när företaget öppnas samt när det ska deletas.
A.2 Schemanamnet är tänkt att vara av typen VARCHAR 20 tecken lång.
B.3 Schemanamnet är nu ändrat till co_ plus löpnummer och schemanamnet behålls även om företaget byter namn.
Risk 3. Använd Förslag A.
Risk 4. Använd Förslag B. Ja man ska kunna ha "ingen aktiv" (alla is_active = False).
Risk 5. Använd Förslag A. Java-kod (V2CompanyRepository.add()) är ansvarig för att anropa denna procedur.
Risk 6. Använd Förslag A + C.
Risk 7. Använd Förslag C (A + B). Vet inte om det finns andra triggers än SSEventTriggerSyncContext. Metoden DropTriggers() ska inte tas bort även om den inte behövs för tillfället.

Svar på Ytterligare klargörande frågor.
1. tbl_company_catalog struktur och IDENTITY
   Antagande är rätt.
   Följ rekommendation, välj alternativ A.
2. Validering av name-synk — vilken är "källan till sanningen"?
   Både tbl_company.name OCH tbl_company_catalog.company_name uppdateras i samma transaktion.
   Följ rekommendation, välj alternativ B, tbl_company_catalog.company_name vinner (är källan). Ge varning att namnen inte ställer överens samt att övrig data bör kontrolleras av användare.
3. Fallback vid borttag av aktivt företag
   Följ rekommendation, välj alternativ B.
4. Startup när ingen är aktiv (is_active = FALSE för alla)
   Följ rekommendation, välj alternativ A.
5. SSEventTriggerSyncContext — var skapas triggers idag?
   Sql-satser för att skapa triggers i databasen finns i SSSchemaBuilder.createLocalTriggers(). Denna metod anroppas vid start utan databas, Import frånSIE-fil samt vid Restore databas.
6. Validering av name-synk — frekvens
   Följ rekommendation, välj alternativ A. Logga varning, men låt användaren arbeta. Synkroniseringen kan lagas senare.
   Se fråga 2, samma varning till användare.
7. Schema-skapande med två-fas status (Risk 5)
   Följ rekommendation. Explicit cleanup i catch-block räcker. Ge info till användare att skapa företaget på nytt. 
8. Migrering från gammalt system — ej-definiert läge
   Följ rekommendation, välj alternativ A. Detektera gamla formatet och krascha med felmeddelande.










