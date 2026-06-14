# Go/No-Go-mall - Decimalantal (heltal*10)

Anvand denna mall i release-motet for beslut om driftsattning.

- Releaseomrade: Decimalantal (`int*10`), alltid 1 decimal i visning
- Datum:
- Miljo: Test / Stage / Produktion
- Motesledare:
- Beslutsagare:
- Narvarande:

## 1) Beslutsregel

- **GO**: Alla blockerande krav ar uppfyllda och inga oppna P1-risker kvarstar.
- **NO-GO**: Minst ett blockerande krav ar ej uppfyllt eller rollback-plan saknas.

## 2) Blockerande krav (maste vara Ja)

### A. Funktionella regler

- [ ] Antal hanteras internt som `int*10` i hela kedjan.
- [ ] Minsta steg `0,1` efterlevs i alla relevanta inmatningar.
- [ ] Antal visas alltid med exakt 1 decimal i GUI och rapporter.
- [ ] Prisregler oforandrade (2 decimaler, `HALF_UP`).
- [ ] Produktregel **hela antal endast** fungerar i faktura, kredit, retur, lager.

### B. Databas och migrering

- [ ] Migrering genomford i stage utan avvikelse.
- [ ] Samtliga berorda tabeller verifierade fore/efter (`old * 10`).
- [ ] Ny produktflagga for **hela antal endast** satt med beslutat default.
- [ ] Datakontroller/logik visar enhetlig skala i persistenslager.

### C. Kritiska floden

- [ ] Faktura normal (inkl. rabatt/moms/frakt) godkand.
- [ ] Kreditfaktura/retur godkand.
- [ ] Periodfakturering godkand.
- [ ] Lagerfloden (inleverans, utleverans, inventering) godkanda.
- [ ] Negativt lager med decimalantal godkant scenario.

### D. Utskrifter och rapporter

- [ ] Faktura visar antal med 1 decimal.
- [ ] Kreditfaktura visar antal med 1 decimal.
- [ ] Foljesedel/plocklista visar antal samt korrekt vikt/volym.
- [ ] Golden samples jamforda utan avvikelse.

### E. Integration/import/export

- [ ] Berorda importer/exporter klarar decimalantal.
- [ ] Externa formatandringar kommunicerade (om tillampligt).

### F. Driftberedskap

- [ ] Rollback-plan dokumenterad och testad i stage.
- [ ] Hypercare-plan dag 0-7 faststalld (ansvariga och tider).
- [ ] Supportinformation/publiceringsnotis klar.

## 3) Oppna risker och undantag

| ID | Risk/undantag | Allvar | Agare | Atgard | Deadline | Status |
|----|---------------|--------|-------|--------|----------|--------|
| R1 |               |        |       |        |          |        |
| R2 |               |        |       |        |          |        |
| R3 |               |        |       |        |          |        |

## 4) Testsammanfattning

- Testomfattning:
- Totalt antal testfall:
- Godkanda:
- Underkanda:
- Ej kord:
- Kritiska avvikelser:

## 5) Migreringssammanfattning

- Kord migrering (version/id):
- Starttid:
- Sluttid:
- Faktisk tid vs plan:
- Avvikelser:
- Verifieringsresultat:

## 6) Beslut

- [ ] **GO**
- [ ] **NO-GO**

Beslutskommentar:

Beslutsagare (namn/sign):

Tidpunkt:

## 7) Om GO - exekveringsplan (kort)

1. Start production releasefonstrer
2. Ta backup/snapshot
3. Kor migrering
4. Kor smoke-testpaket
5. Bekrafta affarskritiska floden
6. Aktivera hypercare

## 8) Om NO-GO - omplanering (kort)

- Orsak till NO-GO:
- Korrigerande aktiviteter:
- Nytt maldatum:
- Ansvariga:

