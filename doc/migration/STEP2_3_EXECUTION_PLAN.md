# Steg 2.3 - Exekveringsplan

Detta är arbetsplanen för att koppla applikationskoden till schema V2.

## Definition of Done (Steg 2.3)

- Runtime init skapar V2-tabeller (inte V1).
- Minst kärnflödena CRUD fungerar mot V2:
  - kund (`tbl_customer`)
  - produkt (`tbl_product`)
  - leverantör (`tbl_supplier`)
  - verifikation (`tbl_voucher` + `tbl_voucher_row`)
- Integrationstester för ovan passerar utan `OBJECT`-kolumner.

## Prioriterad ordning

## 1) Aktivera V2-schema i startup

- Lokalisera schema-laddning i `SSDB.createNewTables()`.
- Introducera tydlig växling till `create_tables_v2.sql`.
- Behall V1-kod endast tillfälligt bakom feature-flag under migrering.

**Mål:** Ny databas bootar med V2-tabeller.

## 2) Bygg V2-repository för masterdata (låg risk)

- Börja med:
  - `CustomerRepository`
  - `ProductRepository`
  - `SupplierRepository`
- Implementera SQL-baserad mapping utan Java-serialisering.
- Mappa inbäddade adresser/value objects explicit.

**Mål:** Masterdata kan läsas/sparas via V2-tabeller.

## 3) Migrera bokföringskärnan

- `tbl_accountplan` + `tbl_account`
- `tbl_accountingyear` + `tbl_year_balance` + `tbl_budget_row`
- `tbl_voucher` + `tbl_voucher_row`

**Mål:** Verifikationsflöden fungerar end-to-end mot V2.

## 4) Migrera transaktioner (sales/purchase/payment/inventory)

- Sales: invoice/order/tender/periodic + row-tabeller.
- Purchase: supplierinvoice/purchaseorder + row-tabeller.
- Payment: in/outpayment + row-tabeller.
- Inventory: in/outdelivery, inventory + row-tabeller.

**Mål:** Inga kvarvarande V1-objektlagringar i affärsflöden.

## 5) Stada bort V1-paths

- Ta bort OBJECT-baserad SQL och serialiseringsberoenden i persistenslagret.
- Uppdatera tester så de validerar V2-only.

**Mål:** Endast V2-lagring används i kodbasen.

## Rekommenderad arbetsmetod per delsteg

1. Implementera en liten vertikal slice.
2. Lägg/uppdatera integrationstester först.
3. Kör:
   - berört testklass
   - `mvn clean test`
4. Commita i små, reversibla steg.

## Risker att hantera tidigt

- FK-ordning vid inserts (speciellt voucher/year/company).
- Null/Optional i gamla API-kontrakt.
- Dubbelkällor för default-konton (`Map<SSDefaultAccount, Integer>`).
- Datumkonvertering (`Date`/`LocalDate`) i boundary-kod.

## Filer att alltid läsa först vid återupptag

- `doc/migration/STEP2_STATUS_2026-05-04.md`
- `OBJECT_COLUMN_MAPPING.md`
- `src/main/resources/sql/create_tables_v2.sql`
- `src/test/java/se/swedsoft/bookkeeping/data/system/SchemaV2ValidatorTest.java`

