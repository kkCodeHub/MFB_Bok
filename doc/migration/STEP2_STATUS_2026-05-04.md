# Steg 2 status (2026-05-04)

Detta dokument sammanfattar exakt var vi står i tvåstegs-migreringen och vad som är nästa praktiska steg.

## Målbild för Steg 2

- Ersätta serialiseringsbaserad persistens (`OBJECT`-kolumner) med normaliserat relationsschema.
- Göra en framåtriktad cutover utan bakåtkompatibilitet för gamla databasformat.
- Förbereda byte till H2 efter att persistensmodellen är stabil i ny struktur.

## Slutfört i denna fas

### Steg 2.1 (kartläggning) - klar

- `OBJECT`-kolumner och Java-klasskopplingar kartlagda i `OBJECT_COLUMN_MAPPING.md`.
- Tabellerna grupperade per domän och komplexitet (A-H).
- Migreringsordning definierad (A -> B -> C -> D+E -> F+G+H).

**Commit:** `a7c9986`

### Steg 2.2 (schema-design) - klar

- Nytt normaliserat schema skapat i `src/main/resources/sql/create_tables_v2.sql`.
- `OBJECT`-kolumner ersatta med explicita kolumner + child-tabeller för rader/listor/mappar.
- Syntaxvalidering lagd i `src/test/java/se/swedsoft/bookkeeping/data/system/SchemaV2ValidatorTest.java`.
- Validering körd lokalt och passerade.

**Commit:** `7218afc`

## Verifierad status

- `SchemaV2ValidatorTest`: passerar mot HSQLDB in-memory.
- Senaste verifiering: **59 statements executed successfully**.
- Steg 2.2 är därför dokumenterat och tekniskt validerat på DDL-nivå.

## Beslut som gäller framåt

- Ingen bakåtkompatibilitet mot V1-datamodell i måldesignen.
- Ingen automatisk V1->V2-migrering behövs för denna uppgradering.
- Fokus i Steg 2.3 är att byta runtime-kod till V2-schemat (inte att stödja dubbla format).

## Kvar till Steg 2.3

- `SSDB` använder fortfarande V1-lagring (serialisering/OBJECT).
- `create_tables_v2.sql` är ännu inte inkopplad i runtime-boot.
- CRUD-paths måste flyttas tabell för tabell till V2.

Följ `doc/migration/STEP2_3_EXECUTION_PLAN.md` för konkret arbetsordning.

