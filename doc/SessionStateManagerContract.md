# SessionStateManager - Kontrakt

Syfte
- Centralisera hantering av sessionstillstånd (current company & current year) och cache-livscykel.
- Ersätta direkta state-/cache-manipulationer i `SSDB` med ett tydligt, testbart API.

Designprinciper
- Enkel, välavgränsad API utan DB-implementation; SSDB orkestrerar bakom en adapter.
- Trådsäker: klienter kan anropa från UI- och bakgrundstrådar.
- Explicit invalidation: inga dolda side-effects.
- Notifiering: property-change events för "COMPANY" och "YEAR".

Ansvarsområden (översikt)
- Hålla och exponera aktuell company och year.
- Hantera cache-clear / invalidate för alla SSDB-cacher.
- Erbjuda strategier för att ladda/omladda cache (reload-strategi).
- Publicera events/lyssnare vid förändringar.

API-kontrakt (metodbeskrivningar)
- getCurrentCompany(): Optional<SSNewCompany>
  - Returnerar nuvarande company eller tom Optional.
  - Ej ändrande; ska vara snabb (ingen implicit DB-laddning).

- setCurrentCompany(SSNewCompany company): void
  - Sätter current company, utför nödvändig invalidation enligt policy.
  - Ska validera argument (icke-null) och kasta IllegalArgumentException om fel.
  - Efter lyckad ändring ska ett "COMPANY" event publiceras med gammalt och nytt värde.

- getCurrentYear(): Optional<SSNewAccountingYear>
  - Returnerar nuvarande year eller tom Optional.

- setCurrentYear(SSNewAccountingYear year): void
  - Sätter current year; utför year-specifik invalidation (t.ex. vouchers).
  - Publicerar "YEAR" event efter lyckad ändring.

- clearAllCaches(): void
  - Sätter alla in-memory entity-cacher till tomt/null state.
  - Anropas vid connection-reset eller explicit reload.
  - Bör vara idempotent och snabb.

- invalidateCachesForCompanyChange(): void
  - Specificerad invalidation som körs internt av `setCurrentCompany`.
  - Nollställer masterdata + transaktionscacher men behåll ej-year-specifika data.

- invalidateCachesForYearChange(): void
  - Specificerad invalidation som körs internt av `setCurrentYear`.
  - Nollställer endast year-specifika caches (exempel: vouchers).

- reloadCaches(ReloadStrategy strategy): ReloadResult
  - Kör en load/refresh enligt vald strategi (se nedan).
  - Ska kunna köras asynkront eller synkront beroende på strategy.
  - Returnerar en enkel result-objekt med status och eventuella fel.

Reload-strategier (förslag)
- SYNC_FULL: synkron, ladda alla caches omedelbart (blocking). Används i tester eller init.
- ASYNC_BACKGROUND: kickoff asynkront, publicera completion-event. UI kan visa loader.
- LAZY_ON_DEMAND: ingen förhandsladdning, ladda när en specifik load*() anropas.
- SELECTIVE(List<ResourceType>): ladda en specifik uppsättning caches.

Notifikation & events
- Publicera property-change events via existerande `SSDBEventBus`-liknande mekanism:
  - "COMPANY" med old/new
  - "YEAR" med old/new
  - "CACHE_RELOAD_STARTED" / "CACHE_RELOAD_COMPLETED" (valfritt)
- Events ska vara lätta att lyssna på från UI och testkod.

Transaktions- och felhantering
- `setCurrentCompany`/`setCurrentYear` bör avsluta i ett konsistent tillstånd:
  - Om reload eller cache-laddning misslyckas, bör state antingen rollbackas eller tydligt markeras som delvis initialiserad.
- Fel bör bubbla upp som tydliga undantag (t.ex. IllegalStateException) så att anropare kan hantera dem.

Trådsäkerhet
- Alla publika metoder måste vara säkra att anropa från flera trådar.
- Intern synkronisering eller immutabla snapshots rekommenderas.
- Event-publicering ska ske efter att intern state stabiliserats.

Prestanda och latens
- Standardbeteende ska vara "lazy" för att undvika dyra startsekvenser.
- `reloadCaches(SYNC_FULL)` ska finnas för scenarier där konsistens krävs före fortsättning.

Testbarhet
- Kontraktsdefinitionen måste möjliggöra enhetstester för:
  - company- och year-byten (events och cache-invalidation)
  - reload-strategier (sync vs async)
  - felvägar (misslyckad load där rollback/markering sker)

Backwards compatibility / migrering
- Under migrationen ska `SSDB` kunna orkestrera anrop till `SessionStateManager` så att befintlig logik i tests och bootstrapping fungerar oförändrat.

Dokumentation och exempel
- Tillhandahåll korta exempel i README: hur byta company, hur trigga en synkron reload.

---

Filer att skapa/uppdatera i nästa steg (för implementering)
- `doc/SessionStateManagerContract.md` (denna)
- `src/main/java/.../SessionStateManager.java` (interface/kontrakt - ej implementerad nu)
- `src/test/.../SessionStateManagerTest.java` (enhetstester för kontrakt)



