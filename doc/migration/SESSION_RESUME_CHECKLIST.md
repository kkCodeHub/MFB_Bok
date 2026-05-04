# Session Resume Checklist (Steg 2)

Använd denna checklista när arbetet ska återupptas i en ny session.

## 1) Snabb läsordning

1. `doc/migration/STEP2_STATUS_2026-05-04.md`
2. `doc/migration/STEP2_3_EXECUTION_PLAN.md`
3. `OBJECT_COLUMN_MAPPING.md`
4. `src/main/resources/sql/create_tables_v2.sql`

## 2) Snabb verifiering i terminal

```powershell
cd "E:\FB_update\fribok-master3\fribok-master"
git --no-pager log --oneline -5
mvn clean test "-Dtest=SchemaV2ValidatorTest"
```

Forvantad signal:
- `SchemaV2ValidatorTest` passerar.
- Loggen innehaller commits `a7c9986` och `7218afc`.

## 3) Starta Steg 2.3 (forsta arbetsuppgift)

- Hitta schema-boot i `SSDB.createNewTables()`.
- Trada in V2-schema (`create_tables_v2.sql`) for ny databas via system property:
  - `-Dfribok.schema.version=v2`
- Validera med en liten integrationstest-slice (kunder/produkter/leverantorer).

## 4) Klartecken innan commit

- Berorda tester passerar lokalt.
- `mvn clean test` utan regressionsfel.
- Dokumentationen uppdaterad med:
  - vad som andrats
  - vilka tabeller/symboler som migrerats
  - vilken del av Steg 2.3 som ar klar

## 5) Uppdatera denna fil efter varje delsteg

Lagg till en kort changelog-rad med datum, commit och status, t.ex.:

- `2026-05-04`: Session A klar - schema wiring i `SSDB.createNewTables()` med `fribok.schema.version` (`v2` aktiverar `create_tables_v2.sql`).
- `YYYY-MM-DD`: Migrerade `tbl_customer` path till V2, commit `<hash>`.

