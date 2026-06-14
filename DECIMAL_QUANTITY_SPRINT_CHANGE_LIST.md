# Konkret ändringslista: sprint-för-sprint (antal som heltal*10)

Detta dokument bryter ned implementationen i konkret ordning per tabell/klass/skärm.
Ingen kod ingår här.

## Sprint 1 - Domänkontrakt, format och valideringsregler

### Mål

Lås intern kvantitetsmodell (`int*10`), visning (alltid 1 decimal), steg (`0,1`),
produktregel **hela antal endast**, samt konsekvent validering i alla flöden.

### Klasser (domän + beräkning)

- `src/main/java/se/swedsoft/bookkeeping/data/base/SSSaleRow.java`
  - Semantik: `getQuantity()/setQuantity()` representerar tiondelar internt.
  - Beräkning: radsumma använder kvantitet i tiondelar (inte helt antal).
- `src/main/java/se/swedsoft/bookkeeping/data/SSProduct.java`
  - `orderpoint` och `ordercount` hanteras som tiondelar.
  - Ny produktflagga: **hela antal endast**.
- `src/main/java/se/swedsoft/bookkeeping/data/SSPurchaseOrderRow.java`
- `src/main/java/se/swedsoft/bookkeeping/data/SSSupplierInvoiceRow.java`
- `src/main/java/se/swedsoft/bookkeeping/data/SSInventoryRow.java`
- `src/main/java/se/swedsoft/bookkeeping/data/SSIndeliveryRow.java`
- `src/main/java/se/swedsoft/bookkeeping/data/SSOutdeliveryRow.java`
- `src/main/java/se/swedsoft/bookkeeping/data/SSPeriodicInvoice.java`
  - Fält `count` (huvud/antal perioder eller antal enheter) ska följa beslutad semantik där relevant.

### Beräkningsklasser (review och justering)

- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSInvoiceMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSCreditInvoiceMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSPeriodicInvoiceMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSOrderMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSTenderMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSPurchaseOrderMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSSupplierInvoiceMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSSupplierCreditInvoiceMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSInventoryMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSIndeliveryMath.java`
- `src/main/java/se/swedsoft/bookkeeping/calc/math/SSOutdeliveryMath.java`

### Leverabel

- Gemensamma regler för parsing/formattering/validering av antal dokumenterade och förankrade.

---

## Sprint 2 - Databas och persistens (V2)

### Mål

Inför datamodellstöd i V2 med `int*10` och produktflagga för **hela antal endast**.

### Tabeller och kolumner (konkret scope)

- `tbl_product`
  - Befintliga: `orderpoint`, `ordercount` (tolkas/lagras som tiondelar).
  - Ny kolumn: flagga för **hela antal endast**.
- `tbl_invoice_row`
  - Kolumn: `count` (tiondelar).
- `tbl_creditinvoice_row`
  - Kolumn: `count` (tiondelar).
- `tbl_periodicinvoice_row`
  - Kolumn: `count` (tiondelar).
- `tbl_order_row`
  - Kolumn: `count` (tiondelar).
- `tbl_tender_row`
  - Kolumn: `count` (tiondelar).
- `tbl_purchaseorder_row`
  - Kolumn: `quantity` (tiondelar).
- `tbl_supplierinvoice_row`
  - Kolumn: `quantity` (tiondelar).
- `tbl_suppliercreditinvoice_row`
  - Kolumn: `quantity` (tiondelar).
- `tbl_inventory_row`
  - Kolumner: `quantity`, `change_qty` (tiondelar).
- `tbl_indelivery_row`
  - Kolumn: `change_qty` (tiondelar).
- `tbl_outdelivery_row`
  - Kolumn: `change_qty` (tiondelar).

### Persistensklass

- `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`
  - Alla read/write-paths för ovan tabeller ska behandla antal som tiondelar.
  - Migreringssteg: gamla heltal konverteras till `*10`.

### Leverabel

- Migreringsplan + migreringsskript/checklista (utan att ändra affärsflöden i GUI ännu).

---

## Sprint 3 - UI/skärmar och användarvalidering

### Mål

Användaren arbetar med 1 decimal i alla relevanta skärmar; pris kvar på 2 decimaler.

### Produkt

- `src/main/java/se/swedsoft/bookkeeping/gui/product/SSProductDialog.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/product/panel/SSProductPanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/product/util/SSProductRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/product/util/SSProductTableModel.java`

Ändringar:
- Ny checkbox/fält för **hela antal endast**.
- `orderpoint/ordercount` visas/editeras med exakt 1 decimal.

### Försäljning/inköp (radrutnät)

- `src/main/java/se/swedsoft/bookkeeping/gui/invoice/panel/SSInvoicePanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/creditinvoice/panel/SSCreditInvoicePanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/periodicinvoice/panel/SSPeriodicInvoicePanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/order/panel/SSOrderPanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/purchaseorder/panel/SSPurchaseOrderPanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/supplierinvoice/panel/SSSupplierInvoicePanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/suppliercreditinvoice/panel/SSSupplierCreditInvoicePanel.java`

Ändringar:
- Antalskolumn: alltid 1 decimal, steg `0,1`.
- Prisfält: fortsatt 2 decimaler.
- Blockera decimaldel för produkter med **hela antal endast** i alla flöden, inkl. kredit/retur.

### Lager

- `src/main/java/se/swedsoft/bookkeeping/gui/inventory/panel/SSInventoryPanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/indelivery/panel/SSIndeliveryPanel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/outdelivery/panel/SSOutdeliveryPanel.java`

Ändringar:
- Lagerförändring och saldo visas/editeras med 1 decimal.
- Negativt lager tillåtet enligt beslutad regel.

### Tabellmodeller/editorer

- `src/main/java/se/swedsoft/bookkeeping/gui/invoice/util/SSInvoiceRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/supplierinvoice/util/SSSupplierInvoiceRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/purchaseorder/util/SSPurchaseOrderRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/inventory/util/SSInventoryRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/indelivery/util/SSIndeliveryRowTableModel.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/outdelivery/util/SSOutdeliveryRowTableModel.java`

### Leverabel

- Full UI-validering och enhetlig antalpresentation med 1 decimal.

---

## Sprint 4 - Rapporter, utskrifter och integrationer

### Mål

Samma antalmodell i utskrifter, rapporter, export/import.

### Utskriftsklasser (exempel i kärnflöde)

- `src/main/java/se/swedsoft/bookkeeping/print/report/sales/SSInvoicePrinter.java`
- `src/main/java/se/swedsoft/bookkeeping/print/report/sales/SSCreditinvoicePrinter.java`
- `src/main/java/se/swedsoft/bookkeeping/print/report/sales/SSOrderPrinter.java`
- `src/main/java/se/swedsoft/bookkeeping/print/report/sales/SSDeliverynotePrinter.java`
- `src/main/java/se/swedsoft/bookkeeping/print/report/sales/SSPickingslipPrinter.java`

Ändringar:
- Antal formateras alltid med 1 decimal.
- Vikt/volym räknas med decimalantal (t.ex. 2,5).
- Fraktlogik: enhetsfrakt används endast när det valet är aktivt.

### JRXML-mallar (data/report)

- `data/report/invoicelist*.jrxml`
- `data/report/creditinvoicelist*.jrxml`
- `data/report/indeliverylist*.jrxml`
- `data/report/inventorylist*.jrxml`
- `data/report/outdeliverylist*.jrxml`

Ändringar:
- Formatmasker för antal till 1 decimal.

### Import/export

- `src/main/java/se/swedsoft/bookkeeping/importexport/xml/SSOrderExporter.java`
- `src/main/java/se/swedsoft/bookkeeping/importexport/xml/SSOrderImporter.java`
- Motsvarande exporter/importer för faktura/kredit/period/lager där antal förekommer.

### Leverabel

- Utskrifter/rapporter/integrationer konsekventa med intern modell `int*10`.

---

## Sprint 5 - Migrering, regression och release

### Mål

Säker driftsättning med migrering och full regression.

### Datamigrering

- Konvertera gamla heltalsantal till tiondelar (`old * 10`) i berörda tabeller.
- Initiera ny produktflagga **hela antal endast** med säkert defaultvärde enligt releasebeslut.
- Kör verifieringsfrågor före/efter migrering.

### Testpaket (måste-pass)

- Enhetstester: parsing/formattering/validering (1 decimal, steg `0,1`).
- Affärsregler: **hela antal endast** i faktura, kredit, retur, lager.
- Beräkningstester: prisavrundning (`HALF_UP`) oförändrad.
- Lagerfall: decimalantal + negativt lager.
- Rapporttester: antal, vikt/volym och fraktutfall.
- Regressionskörning: utvalda integrationsflöden i V2.

### Leverabel

- Go/No-Go-checklista + release notes för ändrad antalsmodell.

