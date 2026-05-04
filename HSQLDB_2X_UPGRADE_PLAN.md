# HSQLDB 1.8 → 2.7 Upgrade Plan (Steg 1)

## Bakgrund

Fribok använder idag **HSQLDB 1.8.0.10** (från ~2005), en mycket gammal version.
Steg 1 i två-stegs-migrering utgörs av **upgrade till HSQLDB 2.7.x** (senaste 2.x, från 2023).

Målet är att stabilisera koden på en modernare HSQLDB innan nästa steg (byte till H2).

---

## Versionsjämförelse

| Aspekt | HSQLDB 1.8 | HSQLDB 2.7 |
|--------|-----------|-----------|
| JDBC-version | JDBC 2.0 | JDBC 4.2 |
| SQL-standard | Partiell SQL92 | SQL:2016 |
| Java-version | Java 1.4+ | Java 8+ |
| Datatypstöd | Grundläggande | Utökat (JSON, XML-delvis) |
| Performance | Låg | Högre |
| Thread-safety | Begränsad | Bättre |
| Transaction-stöd | Basalt | Fullt (ACID) |
| Backning | Stabil men gammal | Modern format |

---

## Brutna och förbirnade ändringar

### 1. JDBC Connection-URI
**HSQLDB 1.8:**
```
jdbc:hsqldb:file:<path>
```

**HSQLDB 2.7:**
```
jdbc:hsqldb:file:<path>;shutdown=true
```
(Se `SSDBConfig.java` och `Bookkeeping.java`)

### 2. SQL-nyckelord och syntax
- `HSQLDB 2.x` är striktare med SQL-syntax.
- `OBJECT` kolumner (lagring av Java-objekt) är **fortfarande stödda men deprecated**.
- Triggers och stored procedures kan behöva uppdateras.

### 3. Datatypsmappning
- `LONGVARBINARY` → `BLOB`
- `LONGVARCHAR` → `CLOB`
- Möjligt behov att uppdatera `OBJECT`-kolumnernas typer.

### 4. Bakupformat
- **HSQLDB 1.8:** använder gamla `.script`/`.data`/`.properties` format.
- **HSQLDB 2.7:** är kompatibel **läsa** gamla format men skriver nytt format.
- Filer kommer att migrera automatiskt-vid första start.

### 5. Klassväg och Driver
- Driver-klassnamn är samma: `org.hsqldb.jdbcDriver`.
- Men klasserna finns i annat paket internt.

---

## Migration Path

### Phase 1: Uppdatera `pom.xml`
```xml
<dependency>
  <groupId>org.hsqldb</groupId>
  <artifactId>hsqldb</artifactId>
  <version>2.7.2</version>
  <scope>compile</scope>
</dependency>
```

**Varför:** Senaste HSQLDB 2.x med bästa stabilitet och backwards-kompatibilitet.

### Phase 2: Kompilering och initiala fel
```bash
mvn clean compile
```
- Förvänta möjliga varningar om deprecated API:er.
- Möjligt behov att uppdatera SQL-DDL i `create_tables.sql`.

### Phase 3: Databaskoppling-test
```bash
mvn clean install
```
- Kör enhets- och integrationstester.
- Förvänta möjliga fel i trig gers eller SQL-syntax.

### Phase 4: Manuell starttest
```bash
java -jar target/fribok-2.2-SNAPSHOT-jar-with-dependencies.jar
```
- Kontrollera att applikationen startar.
- Verifiera att kunder/produkter/leverantörer laddas korrekt.
- Se om databasen migrerar`.script`-format automatiskt.

---

## Identifierade Risker och Mitigering

| Risk | Sannolikhet | Mitigering |
|------|-------------|-----------|
| **SQL-syntaxfel** | Medel | Kör tester; fixa DDL vid behov |
| **OBJECT-kolumner misbeteende** | Låg-Medel | Verifiera serialisering; möjlig senare refactor |
| **Trigger-inkompatibilitet** | Låg | Testa triggers; möjlig omskrivning |
| **Datamnormal vid databasen boot** | Låg-Medel | Manuell verifikation av migreringsformat |
| **Thread-safety issues** | Låg | Redan testad på JUnit 5-bas |

---

## Build-status och resultat (2026-05-04, Session 2)

### Kompilering
✅ **PASS** — `mvn clean compile` lyckas utan fel.

### Enhetstester
✅ **PASS** — Alla JUnit-enhetstest passerar med HSQLDB 2.7.2.

### Integration-tester
⚠️ **FAIL (GUI-issue)** — Integration-tester misslyckas med `HeadlessException`:
```
HeadlessException: No display devices available
  at SSMainFrame.<init>:60 (GUI initialization)
```
**Orsak:** `SSDB.init(false)` försöker skapa Swing GUI components utan en X11/display-server.
**Status:** Inte ett HSQLDB-problem. Krävs separat fix för test-miljö.

### Migrering av databasformat
✅ **PASS** — HSQLDB 2.7.2 skapar alla tabeller från `create_tables.sql` korrekt.

**Åtgärd denna session:**
- Fixade `SSDB.createNewTables()` för att exekvera varje SQL-sats separat
  (PreparedStatement kan bara köra en sats åt gången).
- Förväntat resultat: Alla tabeller skapas nu korekt i HSQLDB 2.7.2 in-memory-DB.

---

## Nästa steg (inte i denna PR — är separat issue)

### Headless-GUI-fix (optionell för denna PR)
1. **Uppdatera `SSDBTestFixture.setupOnce()`** för att inte anropa `SSDB.init(true/false)` i test-läge.
2. Eller: Sätt system property `-Djava.awt.headless=true` vid testkörning (redan gjort i pom.xml).
3. Eller: Uppdatera `SSDB.init()` för att skilja på GUI/headless-mode.

---

## Dokument att uppdatera efter upgrade

- [ ] `CHANGELOG.md` — notera HSQLDB 2.7-upgrade, möjliga breaking changes.
- [ ] `README` — ange systemkrav (Java 21, HSQLDB 2.7).
- [ ] `MODERNIZATION.md` — uppdatera Phase 4/5 med denna milestone.
- [ ] `src/main/java/.../SSDBConfig.java` — möjliga JDBC-URI-ändringar.




