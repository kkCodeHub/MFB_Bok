# SQL-stängning i SSDB.java — Ursprung och Modernisering

## Kort Svar
**SQL-stängningen är ett ARV FRÅN V1.** Det uppstod inte vid V2-migreringen. 

De ursprungliga manuella `iResultSet.close()` och `iStatement.close()` callsens kom från V1-kodbasen och är redan närvarand i `InitialSnapshot` (commit 497beec från 2026-05-04).

---

## Evidens

### 1. InitialSnapshot — Källan till V2-kodbasen
- **Commit:** `497beec791644c9881a2b3e5e63c67544b1bcd67` 
- **Datum:** 2026-05-04 19:42:25 UTC+2
- **Beskrivning:** "InitialSnapshot"
- **Status:** Första commiten för SSDB.java i denna Git-historik

I InitialSnapshot ser vi redan det V1-typiska mönstret:
```java
iResultSet.close();
iStatement.close();
```

Till exempel i `startupLocal()` metoden (ursprunglig linjer 131-140 i InitialSnapshot):
```java
if (iLastCompany != null) {
    iStatement = iConnection.prepareStatement(
            "SELECT * FROM tbl_company WHERE id=?");
    iStatement.setObject(1, iLastCompany);
    iResultSet = iStatement.executeQuery();
    if (iResultSet.next()) {
        SSNewCompany iCompany = (SSNewCompany) iResultSet.getObject("company");
        setCurrentCompany(iCompany);
    }
    iResultSet.close();       // ← V1-mönster
    iStatement.close();       // ← V1-mönster
}
```

### 2. Progressiv Modernisering Under Maj 2026

Från den senaste Git-historiken ser vi att try-with-resources redan började introduceras i senare commits:

| Commit | Datum | Mönster |
|--------|-------|---------|
| 577732a (wire schema selection) | 2026-05-04 ~20:00 | Introducerar try-with-resources i vissa nya V2-metoder |
| 118b4de (customer CRUD) | 2026-05-05 | Blandade mönster — nya metoder använder try-with-resources |
| 59a72ff (product/supplier) | 2026-05-05 | Blandade mönster |
| ... många domain-migration commits ... | 2026-05-06 till 2026-05-07 | Progressiv adoption |
| 5d8d9c8 (Invoice cutover) | 2026-05-07 20:47 | Senaste ändring — blandad stil |

**Exempel från nyare V2-metoderna** (try-with-resources redan etablerat):
```java
// Från shouldSeedV2DemoDataV2() — redan V2-style:
try (PreparedStatement iStatement = iConnection.prepareStatement(
        "SELECT 1 FROM tbl_company WHERE name=?")) {
    iStatement.setObject(1, "Demoföretaget");
    try (ResultSet iResultSet = iStatement.executeQuery()) {
        return !iResultSet.next();
    }
}
```

### 3. Current State — Blandad Still

SSDB.java har **två konkurrerande mönster**:

| Stil | Förekomst | Exempel | Status |
|------|-----------|---------|--------|
| V1 — Manuell stängning | ~70 % av metoderna | `iResultSet.close()` + `iStatement.close()` | Arv från V1 |
| V2 — Try-with-resources | ~30 % av metoderna | `try (PreparedStatement ... )` | Nyare V2-metoder |

**Exempel på V1-metoder (fortfarande i original-state):**
- `startupLocal()` — linjer 202-241
- `checkCreateExampleCompany()` — linjer 406-428
- `hasAccountRowsForYear()` — linjer 733-745
- `getCompanies()` — linjer 897-905
- … många fler …

**Exempel på V2-metoder (redan moderniserade):**
- `shouldSeedV2DemoDataV2()` — linjer 442-448
- `getCompanyByNameV2()` — linjer 466-475
- `getAccountingYearByRangeV2()` — linjer 490-501
- `executeSqlScriptResource()` — linjer 526-527
- `logV2DemoSeedSummary()` — linjer 545-561

---

## Konklusion

### Ursprung
**SQL-stängningen är inte en rest från V2-migreringen. Det är arv från V1-kodbasen.**

De manuella close-calls som finns i SSDB.java:
1. **Lämnades kvar från V1** när InitialSnapshot skapades
2. **Är klassiskt JDBC-mönster** för före-Java 7-tider (före try-with-resources)
3. **Migreras gradvis** när nya V2-metoder skrivs eller gamla metoder refaktoreras

### Rekommendation: Try-with-Resources Migration

Eftersom vi redan har precedent för try-with-resources i nyare V2-metoder, bör vi:

1. **Modernisera alla V1-arv-metoder** till try-with-resources systematiskt
2. **Prioritera metoder** som:
   - Läses/modifieras ofta
   - Är känsliga för resource leak (exception-vägar)
   - Är stora eller komplexabbda
3. **Testerna är redan på plats** (headless HSQLDB + JUnit 5)

Denna modernisering:
- ✅ Förbättrar resource management (auto-close på exception)
- ✅ Förbättrar läsbarhet (reducerar boilerplate)
- ✅ Överensstämmer med AGENTS.md och kodstilguider
- ✅ Är låg risk (try-with-resources är välkänd sedan Java 7, 2011)

### Inte V2-design-artefakt
Det är viktigt att notera: **SQL-stängningen är INTE ett resultat av V2-migeringens nya dataubaser-design.** Det är helt enkelt gammalt V1-kod som ännu inte refactorerades.

---

## Se även

- `MODERNIZATION.md` — Moderniseringsserien (section 3 och 5)
- `AGENTS.md` — Kodstilguider, Java 21, try-with-resources som best practice
- Git history: `git log --all --oneline -- src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`

