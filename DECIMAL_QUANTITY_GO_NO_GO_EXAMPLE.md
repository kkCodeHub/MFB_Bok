# Exempel - Go/No-Go for Decimalantal (heltal*10)

Detta ar en **ifylld exempelversion** av `DECIMAL_QUANTITY_GO_NO_GO_TEMPLATE.md`.
Vardena nedan ar **realistiska platshallare** for planering och releasegenomgang.
De ar **inte** ett faktiskt verifierat produktionsbeslut.

- Releaseomrade: Decimalantal (`int*10`), alltid 1 decimal i visning
- Datum: 2026-06-15
- Miljo: Stage
- Motesledare: [Namn]
- Beslutsagare: [Produktagare / Teknisk ansvarig]
- Narvarande: [Dev], [QA], [Produkt], [Support], [Releaseansvarig]

## 1) Beslutsregel

- **GO**: Alla blockerande krav ar uppfyllda och inga oppna P1-risker kvarstar.
- **NO-GO**: Minst ett blockerande krav ar ej uppfyllt eller rollback-plan saknas.

## 2) Blockerande krav (exempelutvardering)

### A. Funktionella regler

- [x] Antal hanteras internt som `int*10` i hela kedjan.
- [x] Minsta steg `0,1` efterlevs i alla relevanta inmatningar.
- [x] Antal visas alltid med exakt 1 decimal i GUI och rapporter.
- [x] Prisregler oforandrade (2 decimaler, `HALF_UP`).
- [x] Produktregel **hela antal endast** fungerar i faktura, kredit, retur, lager.

Kommentar:
- Verifierat i testscenarier med `0,1`, `1,0`, `2,5`, `-1,5` lagerfall.
- Slutlig verifiering i produktion saknas annu i detta exempel.

### B. Databas och migrering

- [x] Migrering genomford i stage utan blockerande avvikelse.
- [x] Samtliga berorda tabeller verifierade fore/efter (`old * 10`).
- [x] Ny produktflagga for **hela antal endast** satt med beslutat default.
- [x] Datakontroller/logik visar enhetlig skala i persistenslager.

Kommentar:
- Berorda tabeller i detta exempel:
  - `tbl_product`
  - `tbl_invoice_row`
  - `tbl_creditinvoice_row`
  - `tbl_periodicinvoice_row`
  - `tbl_order_row`
  - `tbl_tender_row`
  - `tbl_purchaseorder_row`
  - `tbl_supplierinvoice_row`
  - `tbl_suppliercreditinvoice_row`
  - `tbl_inventory_row`
  - `tbl_indelivery_row`
  - `tbl_outdelivery_row`

### C. Kritiska floden

- [x] Faktura normal (inkl. rabatt/moms/frakt) godkand.
- [x] Kreditfaktura/retur godkand.
- [x] Periodfakturering godkand.
- [x] Lagerfloden (inleverans, utleverans, inventering) godkanda.
- [x] Negativt lager med decimalantal godkant scenario.

Kommentar:
- Exempelgodkannanden:
  - Produkt med antal `2,5`, pris med 2 decimaler
  - Kreditrad mot produkt som ar markerad **hela antal endast** -> decimal blockeras
  - Lagervara med decimalantal tillats ga negativ
  - Enhetsfrakt `20 kr` * antal `2,5` -> `50 kr`

### D. Utskrifter och rapporter

- [x] Faktura visar antal med 1 decimal.
- [x] Kreditfaktura visar antal med 1 decimal.
- [x] Foljesedel/plocklista visar antal samt korrekt vikt/volym.
- [x] Golden samples jamforda utan blockerande avvikelse.

Kommentar:
- Exempel pa kontrollerade utfall:
  - `2,0` visas inte som `2`
  - `2,5` visas konsekvent i GUI, PDF och rapport
  - Vikt: `5 kg * 2,5 = 12,5 kg`

### E. Integration/import/export

- [x] Berorda importer/exporter klarar decimalantal.
- [x] Externa formatandringar kommunicerade (om tillampligt).

Kommentar:
- Om integrationsformat inte andras i detta exempel ska release-notis anda beskriva intern skalflytt och visningsregel.

### F. Driftberedskap

- [x] Rollback-plan dokumenterad och testad i stage.
- [x] Hypercare-plan dag 0-7 faststalld (ansvariga och tider).
- [x] Supportinformation/publiceringsnotis klar.

Kommentar:
- Rollback omfattar backup/snapshot, stoppkriterier och beslutsgang.

## 3) Oppna risker och undantag

| ID | Risk/undantag | Allvar | Agare | Atgard | Deadline | Status |
|----|---------------|--------|-------|--------|----------|--------|
| R1 | Enstaka sallan anvand rapport kan sakna 1-decimalformat | Medel | QA/Reporting | Kontrollera mot golden sample | 2026-06-13 | Oppen |
| R2 | Historisk importfil kan innehalla antagande om heltal | Medel | Integration | Dokumentera formatforvantningar och testimport | 2026-06-12 | Pa gar |
| R3 | Anvandare kan uppfatta `2,0` som ny/inovan visning | Lag | Support | Kort releaseinfo med exempel | 2026-06-14 | Planerad |

## 4) Testsammanfattning

- Testomfattning: Doman, persistens, GUI, rapport, integration, migrering
- Totalt antal testfall: 68
- Godkanda: 66
- Underkanda: 0 blockerande
- Ej korda: 2 lage prioritet
- Kritiska avvikelser: 0

Kommentar:
- De 2 ej karda testen ar i detta exempel icke-kritiska regressionsfall utan koppling till antalmodell.

## 5) Migreringssammanfattning

- Kord migrering (version/id): `decimal-quantity-v1`
- Starttid: 19:00
- Sluttid: 19:11
- Faktisk tid vs plan: +1 minut mot plan
- Avvikelser: Ingen blockerande avvikelse
- Verifieringsresultat: Godkant i stage

Kommentar:
- Exempelkontroll:
  - gammalt antal `2` -> nytt internt varde `20`
  - gammalt bestallningsantal `5` -> nytt internt varde `50`

## 6) Beslut

- [x] **GO**
- [ ] **NO-GO**

Beslutskommentar:
- Stage-godkannande klart i detta exempel. Produktion kan planeras under forutsattning att oppna medelrisker stangs eller explicit accepteras.

Beslutsagare (namn/sign):
- [Namn]

Tidpunkt:
- 2026-06-15 14:30

## 7) Om GO - exekveringsplan (kort)

1. Start production releasefonster
2. Ta backup/snapshot
3. Kor migrering
4. Kor smoke-testpaket
5. Bekrafta affarskritiska floden
6. Aktivera hypercare

### Exempel pa smoke-testpaket

- Skapa faktura med antal `2,5`
- Skapa kreditrad for produkt med **hela antal endast** och verifiera blockering av decimal
- Gor lagerjustering `-1,5`
- Skriv ut faktura och foljesedel
- Verifiera enhetsfrakt for decimalantal

## 8) Om NO-GO - omplanering (kort)

- Orsak till NO-GO:
  - Exempel: blockerande avvikelse i rapport eller migrering
- Korrigerande aktiviteter:
  - Ratta avvikelse
  - Kor om verifiering i stage
  - Nytt Go/No-Go-mote
- Nytt maldatum:
  - 2026-06-22
- Ansvariga:
  - [Utveckling], [QA], [Releaseansvarig]

