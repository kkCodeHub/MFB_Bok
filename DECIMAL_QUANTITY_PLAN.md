# Plan: antal med 1 decimal (heltal*10)

Detta dokument beskriver beslutad målbild innan implementation.

## 1) Beslutade principer

- Intern representation av antal: `int` i tiondelar (`heltal*10`).
- Minsta steg för antal: `0,1`.
- Antal visas alltid med exakt 1 decimal (exempel: `2,0`, `2,5`).
- Antal avrundas inte (valideras till steg om `0,1`).
- Pris fortsätter med 2 decimaler och nuvarande avrundning `HALF_UP`.

## 2) Produktregler

- Ny produktinställning: **hela antal endast**.
- Om **hela antal endast** är aktivt måste decimaldelen vara `0`.
- Regeln gäller i alla flöden, inklusive:
  - fakturarader
  - kreditrader
  - returflöden
  - lagerjustering/inventering
- Decimalprodukter kan vara lagervaror.
- Decimalprodukter kan ha negativt lager.

## 3) Lager, beställningspunkt och beställningsantal

- Lagerantal hanteras i tiondelar (`int*10`).
- Beställningspunkt och beställningsantal hanteras i tiondelar (`int*10`) och visas med 1 decimal.

## 4) Beräkningsregler

- Kärnlogik för antal bygger på heltal i tiondelar för att undvika flyttalsfel.
- Prisberäkning följer befintliga regler för prisfält (2 decimaler, `HALF_UP`).
- Vikt/volym beräknas proportionellt med antal:
  - Exempel: `5 kg * 2,5 = 12,5 kg`.
  - Används i följesedel och plocklista.
- Frakt:
  - Om enhetsfrakt används: frakt = `enhetsfrakt * antal`.
  - Om frakt faktureras frikopplat från enhetsfrakt: enhetsfrakt påverkar inte beräkningen.

## 5) Datamodell (målbild)

Följande typer av data ska hantera antal i tiondelar (`int*10`):

- Fakturarader (normal, kredit, period).
- Order-/leveransrader.
- Lagertransaktioner och inventering.
- Produktens beställningspunkt och beställningsantal.
- Historik/logg-tabeller där antal förekommer.

Samt:

- Produktflagga för **hela antal endast**.

## 6) UI och rapporter (målbild)

- Inmatning antal: 1 decimal, steg `0,1`.
- Visning antal: alltid 1 decimal.
- Inmatning pris: 2 decimaler.
- Validering ska vara konsekvent i alla berörda skärmar/flöden.
- Rapporter (faktura, kreditfaktura, följesedel, plocklista) ska visa antal med 1 decimal.

## 7) Migreringsprincip

- Befintliga heltalsantal migreras till tiondelar (`gammaltVarde * 10`).
- Konsistenskontroll före/efter migrering.
- Vid behov: bakåtkompatibel läsning under övergång; annars engångsmigrering i releasefönster.

## 8) Testmål

- Parser/formatter för antal (alltid 1 decimal, steg `0,1`).
- Affärsregler för **hela antal endast** i samtliga flöden (inkl. kredit/retur).
- Lagerfall med decimalantal och negativt lager.
- Prisavrundning oförändrad (`HALF_UP`).
- Rapportvalidering för antalformat och vikt/volym.

