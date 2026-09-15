# Genomförandeplan – FAS 1: Multipla verifikatserier

## Avgränsning
Denna fas omfattar endast:
- nya tabeller och migrationssteg
- repository för databasaccess
- UI i Företagsinställningar: **Val 8 Verifikatserie**
- lista/tabell för knytning mellan verifikatserie och händelsekod

Ingår inte i denna fas:
- full koppling i alla bokföringsflöden (kundfaktura, in/utbetalning m.fl.) vid verifikatskapande
- ny nummersättningsmotor i drift

---

## 1. Datamodell

### 1.1 PUBLIC-tabell: `tbl_voucher_event_type`
Syfte: gemensam masterdata för fasta händelsekoder.

Kolumner:
- `id` (PK, identity)
- `event_code` (unik, t.ex. KF, FJ, IB, UB)
- `event_name` (visningsnamn)
- `default_series_code` (A–Z, defaultförslag)
- `active` (boolean)

Regler:
- unik constraint på `event_code`
- validering att `default_series_code` är en bokstav A–Z

### 1.2 Årsbunden tabell: `tbl_year_voucher_event_series_map`
Syfte: faktisk konfiguration per bokföringsår för Val 8.

Kolumner:
- `id` (PK, identity)
- `year_id` (FK mot `tbl_accountingyear`)
- `event_code` (FK mot `PUBLIC.tbl_voucher_event_type.event_code`, nullable för custom-rad)
- `custom_event_name` (nullable, används för manuellt tillagd rad)
- `series_code` (A–Z)
- `is_custom` (boolean)

Regler:
- systemrad: `is_custom = false`, `event_code` måste finnas, `custom_event_name` ska vara null
- custom-rad: `is_custom = true`, `custom_event_name` måste finnas, `event_code` kan vara null
- unik constraint för systemrader: `(year_id, event_code)`
- validering att `series_code` är A–Z

### 1.3 Räknartabell (förbereds i fas 1): `tbl_voucher_series_counter`
Syfte: hålla senaste/nästa nummer per serie och år.

Kolumner:
- `year_id`
- `series_code` (A–Z)
- `last_number` (eller `next_number`, välj en modell)

Regler:
- unik constraint på `(year_id, series_code)`

Not: denna tabell används fullt ut i nästa fas när nummersättning kopplas in i bokföringsflöden.

---

## 2. Seedning

### 2.1 Seed av `tbl_voucher_event_type`
Skapa idempotent seed (startup/migration):
- läs källa från `Seed_Public.json` under noden `"Verifikatserie"` (array)
- varje objekt mappar så här:
  - `"Händelsekod"` -> `event_code`
  - `"Händelsenamn"` -> `event_name`
  - `"Verifikatkod"` -> `default_series_code`
- sätt `active = true` för seedade poster om inget annat anges i seedformatet
- stöd flera objekt i arrayen (framtida utökning utan kodändring)
- uppdatera befintliga poster vid namn/default-ändring utan dubbletter
- skapa nya poster som saknas

### 2.2 Initiering vid nytt bokföringsår
När nytt år skapas:
- skapa systemrader i `tbl_year_voucher_event_series_map` från aktiva poster i PUBLIC
- sätt `series_code = default_series_code`
- sätt `is_custom = false`

---

## 3. Repository-lager

### 3.1 Public repository
`V2VoucherEventTypeRepository`:
- `findAllActiveOrderedById()`
- `findByCode(code)`
- `upsertFromPublicSeed(...)` (anropas via seed service som läser `Seed_Public.json`)

### 3.2 Årsmapping repository
`V2YearVoucherEventSeriesMapRepository`:
- `findByYearOrderedById(yearId)`
- `upsertSystemMapping(yearId, eventCode, seriesCode)`
- `addCustomMapping(yearId, customEventName, seriesCode)`
- `updateCustomMapping(id, customEventName, seriesCode)`
- `deleteCustomMapping(id)`

### 3.3 Series counter repository (förbereds)
`V2VoucherSeriesCounterRepository`:
- `findByYearAndSeries(yearId, seriesCode)`
- `createIfMissing(yearId, seriesCode, startValue)`

---

## 4. UI – Företagsinställningar, Val 8 Verifikatserie

## 4.1 Navigering
- Nytt val i Företagsinställningar: **8. Verifikatserie**

## 4.2 Tabellinnehåll
- Ingen separat knapp för “visa systemrader för året”.
- Vid öppning laddas tabellen direkt för valt bokföringsår från `tbl_year_voucher_event_series_map`.

Kolumner:
- **Kolumn 1: Verifikatserie** (A–Z, redigerbar dropdown)
- **Kolumn 2: Händelsekod** (read-only för systemrad; tom/ej relevant för custom-rad)
- **Kolumn 3: Händelsenamn** (read-only för systemrad; skrivbar för custom-rad)
- **Kolumn 4: Typ** (SYSTEM/CUSTOM, read-only)

Standardordning:
- rader visas i `id`-ordning (ORDER BY id)
- användaren kan därefter sortera genom kolumnhuvud i UI

## 4.3 Radhantering
- Systemrader:
  - kan ändra `series_code`
  - kan inte ändra `event_code` eller systemnamn
- Custom-rader:
  - kan läggas till manuellt
  - `custom_event_name` skrivbar
  - `series_code` väljs A–Z
  - kan uppdateras/ta bort

---

## 5. Validering
- `series_code` måste vara A–Z (UI + DB)
- custom-rad kräver `custom_event_name`
- systemrad kräver giltig `event_code`
- inga dubbletter av systemkod per år (`year_id + event_code`)

---

## 6. Leveransordning
1. DDL/migration för tre tabeller + constraints
2. Seed service för PUBLIC-händelser från `Seed_Public.json` ("Verifikatserie")
3. Initiering vid årsskapande (defaultmapping)
4. Repositories
5. Service/facade för Val 8
6. UI för Val 8 (tabell + CRUD för custom-rader)
7. Tester (repository + seed + initiering + UI-integrationsfall)

---

## 7. Acceptanskriterier för FAS 1
- PUBLIC-händelser finns seedade med default-serier
- seedning hämtar data från `Seed_Public.json` -> `"Verifikatserie"`
- nytt bokföringsår får automatiskt systemrader i mappingtabellen
- Val 8 visar rader utan extra knapp
- Kolumn 1 (verifikatserie) kan ändras A–Z
- custom-rad kan läggas till och sparas
- tabellen öppnar i `id`-ordning och kan sorteras via kolumnhuvud
