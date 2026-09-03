# PR: Lägg till persistenslager med repository-interfaces för masterdata

## Typ av förändring
- [x] Ny funktion / ny kod
- [ ] Buggfix
- [x] Brytande ändring (vid cutover till ny databasmodell)

---

## Bakgrund

`SSDB` är idag ett >7 700-radigt "God object" som blandar ihop
databasuppkoppling, cachning, affärslogik och GUI-notifieringar i en enda klass.
Samtliga delar av applikationen refererar direkt till `SSDB.getInstance()`, vilket
gör det i princip omöjligt att:

- byta databasmotor utan att ändra varje anropssitet
- skriva enhetstester utan en live HSQLDB-koppling
- gradvis migrera data-lagret till en normalized schema (inga `OBJECT`-kolumner)

Denna PR introducerar ett **tunt repository-lager** som bryter det direkta beroendet
för masterdatadomänen (kunder, produkter, leverantörer) utan att ändra ett enda
befintligt anropssitet.

**Beslut för moderniseringen:** migrationen är *forward-only cutover*.
Efter övergång till ny databasmodell finns inget krav på att kunna återläsa
äldre backupformat direkt i den nya databasen.

---

## Vad förändringen gör

### Nya filer

| Fil | Roll |
|-----|------|
| `persistence/CustomerRepository.java` | Interface: CRUD + sökmetoder för `SSCustomer` |
| `persistence/ProductRepository.java` | Interface: CRUD + sökmetoder för `SSProduct` |
| `persistence/SupplierRepository.java` | Interface: CRUD + sökmetoder för `SSSupplier` |
| `persistence/legacy/SSDBCustomerRepository.java` | Legacy-impl: delegerar till `SSDB` |
| `persistence/legacy/SSDBProductRepository.java` | Legacy-impl: delegerar till `SSDB` |
| `persistence/legacy/SSDBSupplierRepository.java` | Legacy-impl: delegerar till `SSDB` |
| `persistence/Repositories.java` | Fabrik/statisk locator; initieras med `Repositories.init(SSDB)` |
| `test/.../SSDBCustomerRepositoryTest.java` | Grundläggande konstruktortester (JUnit 5) |

### Senaste justering

- `SSDBCustomerRepositoryTest` migrerad till JUnit 5 (`org.junit.jupiter.api.Test`).
- Assertion för exception uppdaterad från JUnit 4-stil till `assertThrows(...)`.
- Matchar projektets befintliga testsetup med `junit-jupiter` i `pom.xml`.
- Valideringsunderlag för företagsinställningar dokumenterat i `doc/migration/COMPANY_SETTINGS_VALIDATION_MATRICES.md`.

### Principen
```
Befintlig kod          Ny kod
──────────────         ──────────────────────────────────────────────
SSDB (singleton)  ←── SSDBCustomerRepository (legacy-impl)
                       implements CustomerRepository (interface)
                            ↑
                       Repositories.customers()  ← anropare framöver
```

Legacy-implementationen **ändrar inget beteende** – den är en ren delegatadapter.
Befintliga direktanrop till `SSDB.getInstance().getCustomers()` etc. fortsätter
att fungera oförändrat i detta steg.

Vid senare cutover till ny databasimplementation är avsikten att beteende och
datamodell får förändras där det behövs, utan krav på restore av legacy-backuper.

---

## Hur man testar

```bash
# Kompilera
mvn clean compile

# Kör nya tester (JUnit 5)
mvn -Dtest=SSDBCustomerRepositoryTest test

# Kör hela testsviten (regressionstest)
mvn clean install
```

Manuellt: starta applikationen normalt och verifiera att kund-, produkt- och
leverantörslistorna visas korrekt.

---

## Nästa steg (inte i denna PR)

1. **Initiera `Repositories`** – anropa `Repositories.init(SSDB.getInstance())`
   i `Bookkeeping.main()` efter att databasen startats.
2. **Definiera målmodell** – ta fram normaliserat schema för ny databas
   (utan `OBJECT`-kolumner) och dokumentera brytande skillnader.
3. **Migrera ett anropsssite** – byt ut ett `SSDB.getInstance().getCustomers()`
   mot `Repositories.customers().findAll()` och verifiera funktionella
   acceptanskriterier i stället för full legacy-paritet.
4. **Lägg till fler domains** – enligt migrationsordning:
   säljflöde → inköpsflöde → lager → bokföring.
5. **Cutover-plan** – dokumentera att äldre backupformat inte stöds för restore
   efter övergång, samt hur användare uppgraderar säkert.

---

## Checklista

- [x] `mvn clean compile` passerar utan fel
- [x] `mvn -Dtest=SSDBCustomerRepositoryTest test` passerar
- [x] Inga befintliga anropssiter ändrade i denna PR
- [x] Javadoc på alla publika klasser och metoder
- [x] `CHANGELOG.md` uppdaterad
- [x] Inga wildcard-imports
- [x] Indragning 4 mellanslag, vänster klammerparentes på samma rad
- [x] Beslut dokumenterat: ingen restore av äldre backupformat efter cutover

