# Steg 2.1 — Kartläggning: OBJECT-kolumner → Java-klasser → SQL-fält

Detta dokument beskriver alla 30 tabeller i `create_tables.sql` som innehåller
en `OBJECT`-kolumn med serialiserat Java-objekt, vilken Java-klass varje
kolumn lagrar, och vilka fält som måste bli riktiga SQL-kolumner i Steg 2.2.

Ingen bakåtkompatibilitet krävs — nytt schema är ett rent snitt.

---

## Konventioner

| Symbol | Betydelse |
|--------|-----------|
| `PK`   | Primary key i det nya schemat |
| `FK`   | Foreign key |
| `→ tbl` | Refererar till en annan tabell |
| ⭐ enkel | Platta fält; lättast att migrera |
| ⚙ medium | Innehåller inbäddade värde-objekt (SSAddress, SSCurrency m.fl.) |
| 🔧 komplex | Innehåller listor av rader (child-tabeller krävs) |
| 🔩 mycket komplex | Djupt nästlade strukturer med korsreferenser |

---

## Typkategori A — Referensdata (globala uppslagstabeller)

Dessa tabeller lagrar globala masterdata utan `companyid`. I det nya schemat
behåller de sin nuvarande primärnyckel (namn/kod) och får bara utplattade fält.

### tbl_currency ⭐
**Java-klass:** `SSCurrency`  
**Nuvarande PK:** `code VARCHAR(255)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `code` `PK`   | `iCode`   | `VARCHAR(10)` |
| `description` | `iDescription` | `VARCHAR(255)` |
| `exchange_rate` | `iExchangeRate` | `DECIMAL(18,6)` |

---

### tbl_unit ⭐
**Java-klass:** `SSUnit`  
**Nuvarande PK:** `name VARCHAR(255)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `name` `PK`   | `iName`   | `VARCHAR(100)` |
| `description` | (om fins) | `VARCHAR(255)` |

---

### tbl_deliveryway ⭐
**Java-klass:** `SSDeliveryWay`  
**Nuvarande PK:** `name VARCHAR(255)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `name` `PK`   | `iName`   | `VARCHAR(100)` |
| `description` | (om fins) | `VARCHAR(255)` |

---

### tbl_deliveryterm ⭐
**Java-klass:** `SSDeliveryTerm`  
**Nuvarande PK:** `name VARCHAR(255)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `name` `PK`   | `iName`   | `VARCHAR(100)` |
| `description` | (om fins) | `VARCHAR(255)` |

---

### tbl_paymentterm ⭐
**Java-klass:** `SSPaymentTerm`  
**Nuvarande PK:** `name VARCHAR(255)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `name` `PK`   | `iName`   | `VARCHAR(100)` |
| `description` | `iDescription` | `VARCHAR(255)` |
| `days`        | (beräknat) | `INTEGER` |

---

## Typkategori B — Masterdata (per företag)

### tbl_company ⚙
**Java-klass:** `SSCompany`  
**Nuvarande PK:** `id INTEGER IDENTITY`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | `iId` (UID) | `INTEGER IDENTITY` |
| `name`        | `iName` | `VARCHAR(255)` |
| `phone`       | `iPhone` | `VARCHAR(50)` |
| `phone2`      | `iPhone2` | `VARCHAR(50)` |
| `telefax`     | `iTelefax` | `VARCHAR(50)` |
| `residence`   | `iResidence` | `VARCHAR(255)` |
| `web_address` | `iWebAddress` | `VARCHAR(255)` |
| `smtp_address`| `iSMTPAddress` | `VARCHAR(255)` |
| `email`       | `iEMail` | `VARCHAR(255)` |
| `contact_person` | `iContactPerson` | `VARCHAR(255)` |
| `tax_registered` | `iTaxRegistered` | `BOOLEAN` |
| `corporate_id`| `iCorporateID` | `VARCHAR(50)` |
| `logotype`    | `iLogotype` | `VARCHAR(500)` |
| `bank`        | `iBank` | `VARCHAR(255)` |
| `vat_number`  | `iVATNumber` | `VARCHAR(50)` |
| `bank_account`| `iBankAccountNumber` | `VARCHAR(50)` |
| `plusgiro`    | `iPlusAccountNumber` | `VARCHAR(50)` |
| `iban`        | `iIBAN` | `VARCHAR(50)` |
| `swift`       | `iSwift` | `VARCHAR(20)` |
| `currency_code` `FK` | `iCurrency.iCode` | `VARCHAR(10) → tbl_currency` |
| `invoice_addr_name` | `iInvoiceAddress.iName` | `VARCHAR(255)` |
| `invoice_addr_address` | `iInvoiceAddress.iAddress` | `VARCHAR(255)` |
| `invoice_addr_street` | `iInvoiceAddress.iStreet` | `VARCHAR(255)` |
| `invoice_addr_zipcode` | `iInvoiceAddress.iZipCode` | `VARCHAR(20)` |
| `invoice_addr_city` | `iInvoiceAddress.iCity` | `VARCHAR(100)` |
| `invoice_addr_country` | `iInvoiceAddress.iCountry` | `VARCHAR(100)` |

> **Not:** `SSCompany` har ytterligare fält (defaultkonton, m.m.) —
> komplett genomgång behövs i Steg 2.2.

---

### tbl_customer ⚙
**Java-klass:** `SSCustomer`  
**Nuvarande PK:** `(number, companyid)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | –          | `INTEGER IDENTITY` |
| `number`      | `iCustomerNr` | `VARCHAR(50)` |
| `companyid` `FK` | –      | `INTEGER → tbl_company` |
| `name`        | `iName` | `VARCHAR(255)` |
| `email`       | `iEMail` | `VARCHAR(255)` |
| `phone`       | `iPhone` | `VARCHAR(50)` |
| `phone2`      | `iPhone2` | `VARCHAR(50)` |
| `telefax`     | `iTelefax` | `VARCHAR(50)` |
| `registration_number` | `iRegistrationNumber` | `VARCHAR(50)` |
| `our_contact` | `iOurContactPerson` | `VARCHAR(255)` |
| `your_contact`| `iYourContactPerson` | `VARCHAR(255)` |
| `vat_number`  | `iVATNumber` | `VARCHAR(50)` |
| `bankgiro`    | `iBankAccountNumber` | `VARCHAR(50)` |
| `plusgiro`    | `iPlusAccountNumber` | `VARCHAR(50)` |
| `account_number` | `iAccountNumber` | `VARCHAR(50)` |
| `clearing_number` | `iClearingNumber` | `VARCHAR(20)` |
| `eu_sale_commodity` | `iEuSaleCommodity` | `BOOLEAN` |
| `eu_sale_third_part` | `iEuSaleYhirdPartCommodity` | `BOOLEAN` |
| `vat_free_sale` | `iVatFreeSale` | `BOOLEAN` |
| `hide_unitprice` | `iHideUnitprice` | `BOOLEAN` |
| `credit_limit`| `iCreditLimit` | `DECIMAL(18,2)` |
| `discount`    | `iDiscount` | `DECIMAL(5,2)` |
| `comment`     | `iComment` | `TEXT` |
| `currency_code` `FK` | `iInvoiceCurrency` | `VARCHAR(10) → tbl_currency` |
| `payment_term` `FK`  | `iPaymentTerm` | `VARCHAR(100) → tbl_paymentterm` |
| `delivery_term` `FK` | `iDeliveryTerm` | `VARCHAR(100) → tbl_deliveryterm` |
| `delivery_way` `FK`  | `iDeliveryWay` | `VARCHAR(100) → tbl_deliveryway` |
| `inv_addr_name` | `iInvoiceAddress.iName` | `VARCHAR(255)` |
| `inv_addr_address` | `iInvoiceAddress.iAddress` | `VARCHAR(255)` |
| `inv_addr_street` | `iInvoiceAddress.iStreet` | `VARCHAR(255)` |
| `inv_addr_zipcode` | `iInvoiceAddress.iZipCode` | `VARCHAR(20)` |
| `inv_addr_city` | `iInvoiceAddress.iCity` | `VARCHAR(100)` |
| `inv_addr_country` | `iInvoiceAddress.iCountry` | `VARCHAR(100)` |
| `del_addr_name` | `iDeliveryAddress.iName` | `VARCHAR(255)` |
| `del_addr_address` | `iDeliveryAddress.iAddress` | `VARCHAR(255)` |
| `del_addr_street` | `iDeliveryAddress.iStreet` | `VARCHAR(255)` |
| `del_addr_zipcode` | `iDeliveryAddress.iZipCode` | `VARCHAR(20)` |
| `del_addr_city` | `iDeliveryAddress.iCity` | `VARCHAR(100)` |
| `del_addr_country` | `iDeliveryAddress.iCountry` | `VARCHAR(100)` |

---

### tbl_supplier ⚙
**Java-klass:** `SSSupplier`  
**Nuvarande PK:** `(number, companyid)`

Liknar `SSCustomer`. Nyckellfält:

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | –          | `INTEGER IDENTITY` |
| `number`      | `iNumber` | `VARCHAR(50)` |
| `companyid` `FK` | –      | `INTEGER → tbl_company` |
| `name`        | `iName` | `VARCHAR(255)` |
| `phone`       | `iPhone` | `VARCHAR(50)` |
| `phone2`      | `iPhone2` | `VARCHAR(50)` |
| `telefax`     | `iTeleFax` | `VARCHAR(50)` |
| `email`       | `iEmail` | `VARCHAR(255)` |
| `homepage`    | `iHomepage` | `VARCHAR(255)` |
| `registration_number` | `iRegistrationNumber` | `VARCHAR(50)` |
| `your_contact`| `iYourContact` | `VARCHAR(255)` |
| `our_contact` | `iOurContact` | `VARCHAR(255)` |
| `our_customer_nr` | `iOurCustomerNr` | `VARCHAR(50)` |
| `bankgiro`    | `iBankAccountNumber` | `VARCHAR(50)` |
| `plusgiro`    | `iPlusAccountNumber` | `VARCHAR(50)` |
| `outpayment_number` | `iOutpaymentNumber` | `INTEGER` |
| `comment`     | `iComment` | `TEXT` |
| `currency_code` `FK` | `iCurrency` | `VARCHAR(10) → tbl_currency` |
| `payment_term` `FK`  | `iPaymentTerm` | `VARCHAR(100) → tbl_paymentterm` |
| `delivery_term` `FK` | `iDeliveryTerm` | `VARCHAR(100) → tbl_deliveryterm` |
| `delivery_way` `FK`  | `iDeliveryWay` | `VARCHAR(100) → tbl_deliveryway` |
| `addr_name` … `addr_country` | `iAddress.*` | Inbäddad adress (6 kolumner) |

---

### tbl_product ⚙
**Java-klass:** `SSProduct`  
**Nuvarande PK:** `(number, companyid)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | –          | `INTEGER IDENTITY` |
| `number`      | `iNumber` | `VARCHAR(50)` |
| `companyid` `FK` | –      | `INTEGER → tbl_company` |
| `description` | `iDescription` | `VARCHAR(500)` |
| `unitprice`   | `iUnitprice` | `DECIMAL(18,4)` |
| `tax_code`    | `iTaxCode` (enum) | `VARCHAR(10)` |
| `warehouse_location` | `iWarehouseLocation` | `VARCHAR(100)` |
| `orderpoint`  | `iOrderpoint` | `INTEGER` |
| `ordercount`  | `iOrdercount` | `INTEGER` |
| `purchase_price` | `iPurchasePrice` | `DECIMAL(18,4)` |
| `stock_price` | `iStockPrice` | `DECIMAL(18,4)` |
| `freight`     | `iFreight` | `DECIMAL(18,4)` |
| `supplier_nr` | `iSupplierNr` | `VARCHAR(50)` |
| `supplier_product_nr` | `iSupplierProductNr` | `VARCHAR(50)` |
| `expired`     | `iExpired` | `BOOLEAN` |
| `stock_goods` | `iStockGoods` | `BOOLEAN` |
| `unit` `FK`   | `iUnit` | `VARCHAR(100) → tbl_unit` |
| `weight`      | `iWeight` | `DECIMAL(10,3)` |
| `volume`      | `iVolume` | `DECIMAL(10,3)` |
| `project_number` | `iProjectNumber` | `VARCHAR(50)` |

> **Kommentar:** `Map<SSDefaultAccount, Integer> iDefaultAccounts` behöver
> en separat child-tabell `tbl_product_account`.

---

### tbl_project ⭐
**Java-klass:** `SSProject`  
**Nuvarande PK:** `(number, companyid)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `number` `PK` | `iNumber` | `VARCHAR(50)` |
| `companyid` `FK` `PK` | – | `INTEGER → tbl_company` |
| `name`        | `iName` | `VARCHAR(255)` |
| `description` | `iDescription` | `TEXT` |
| `concluded`   | `iConcluded` | `BOOLEAN` |
| `concluded_date` | `iConcludedDate` | `DATE` |

---

### tbl_resultunit ⭐
**Java-klass:** `SSResultUnit`  
**Nuvarande PK:** `(number, companyid)`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `number` `PK` | `iNumber` | `VARCHAR(50)` |
| `companyid` `FK` `PK` | – | `INTEGER → tbl_company` |
| `name`        | `iName` | `VARCHAR(255)` |
| `description` | `iDescription` | `TEXT` |

---

## Typkategori C — Bokföring (kärnan)

### tbl_accountplan 🔧
**Java-klass:** `SSAccountPlan`  
Innehåller en lista av `SSAccount`-objekt.

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | `iId`     | `INTEGER IDENTITY` |
| `name`        | `iName`   | `VARCHAR(255)` |
| `base_name`   | `iBaseName` | `VARCHAR(255)` |
| `assessment_year` | `iAssessementYear` | `VARCHAR(10)` |
| `type`        | `iType` (enum) | `VARCHAR(50)` |

> `List<SSAccount>` → ny child-tabell `tbl_account`:  
> `(id PK, accountplan_id FK, number INTEGER, name VARCHAR(255), active BOOLEAN, ...)`

---

### tbl_accountingyear 🔩
**Java-klass:** `SSAccountingYear`  
Den mest komplexa typen — innehåller:
- `SSAccountPlan` (FK)
- `Map<SSAccount, BigDecimal>` ingående balanser
- `List<SSVoucher>` (men lagras separat i `tbl_voucher`)
- `SSBudget`

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | `iId`     | `INTEGER IDENTITY` |
| `companyid` `FK` | –      | `INTEGER → tbl_company` |
| `from_date`   | `iFrom`   | `DATE` |
| `to_date`     | `iTo`     | `DATE` |
| `accountplan_id` `FK` | `iPlan` | `INTEGER → tbl_accountplan` |

> `iInBalance` → ny child-tabell `tbl_year_balance`:  
> `(year_id FK, account_nr INTEGER, balance DECIMAL(18,2))`
>
> `iBudget` (SSBudget) → inline-fält eller separat tabell;
> `SSBudget` i sin tur innehåller `Map<SSAccount, BigDecimal[]>` (per månad)
> → troligen `tbl_budget_row(year_id, account_nr, month INTEGER, amount DECIMAL)`.

---

### tbl_voucher 🔧
**Java-klass:** `SSVoucher`  
Inkluderar en lista `List<SSVoucherRow>`.

| Nytt SQL-fält | Java-fält | Typ |
|---------------|-----------|-----|
| `id` `PK`     | –         | `INTEGER IDENTITY` |
| `number`      | `iNumber` | `INTEGER` |
| `yearid` `FK` | –         | `INTEGER → tbl_accountingyear` |
| `date`        | `iDate`   | `DATE` |
| `description` | `iDescription` | `VARCHAR(500)` |
| `corrects_id` `FK` | `iCorrects` | `INTEGER → tbl_voucher` (self-ref) |
| `corrected_by_id` `FK` | `iCorrectedBy` | `INTEGER → tbl_voucher` (self-ref) |

> `List<SSVoucherRow>` → child-tabell `tbl_voucher_row`:  
> `(id PK, voucher_id FK, account_nr, project_number, result_unit_number,`  
> ` debet DECIMAL, credit DECIMAL, edited_date DATE, edited_signature VARCHAR,`  
> ` crossed BOOLEAN, added BOOLEAN)`

---

## Typkategori D — Försäljning (transaktioner med rader)

Alla dessa ärver från `SSSale` (med `List<SSSaleRow>`) eller liknar den strukturen.

### SSSale — gemensamma fält (ärvs av 6 klasser nedan)

| SQL-fält | Java-fält | Typ |
|----------|-----------|-----|
| `number` | `iNumber` | `INTEGER` |
| `date`   | `iDate`   | `DATE` |
| `customer_nr` | `iCustomerNr` | `VARCHAR(50)` |
| `customer_name` | `iCustomerName` | `VARCHAR(255)` |
| `our_contact` | `iOurContactPerson` | `VARCHAR(255)` |
| `your_contact` | `iYourContactPerson` | `VARCHAR(255)` |
| `delay_interest` | `iDelayInterest` | `DECIMAL(5,2)` |
| `currency_code` `FK` | `iCurrency` | `VARCHAR(10)` |
| `payment_term` `FK` | `iPaymentTerm` | `VARCHAR(100)` |
| `delivery_term` `FK` | `iDeliveryTerm` | `VARCHAR(100)` |
| `delivery_way` `FK` | `iDeliveryWay` | `VARCHAR(100)` |
| `tax_free` | `iTaxFree` | `BOOLEAN` |
| `text` | `iText` | `TEXT` |
| `eu_sale_commodity` | `iEuSaleCommodity` | `BOOLEAN` |
| `eu_sale_third_part` | `iEuSaleYhirdPartCommodity` | `BOOLEAN` |
| `printed`| `iPrinted` | `BOOLEAN` |
| inv/del addr (12 fält) | `iInvoiceAddress`, `iDeliveryAddress` | inbäddade |

> `List<SSSaleRow>` → child-tabell per sale-typ, t.ex. `tbl_invoice_row`:  
> `(id PK, invoice_id FK, product_nr, description, unitprice DECIMAL, count INTEGER,`  
> ` unit FK, discount DECIMAL, tax_code VARCHAR, account_nr INTEGER,`  
> ` project_number VARCHAR, result_unit_number VARCHAR)`

---

### tbl_invoice 🔧 (extends SSSale)
**Java-klass:** `SSInvoice`

Extra fält utöver SSSale:

| SQL-fält | Java-fält | Typ |
|----------|-----------|-----|
| `invoice_type` | `iType` (enum) | `VARCHAR(20)` |
| `currency_rate` | `iCurrencyRate` | `DECIMAL(10,6)` |
| `payment_day` | `iPaymentDay` | `DATE` |
| `your_order_number` | `iYourOrderNumber` | `VARCHAR(100)` |
| `ocr_number` | `iOCRNumber` | `VARCHAR(50)` |
| `entered` | `iEntered` | `BOOLEAN` |
| `num_reminders` | `iNumReminders` | `INTEGER` |
| `interest_invoiced` | `iInterestInvoiced` | `BOOLEAN` |
| `stock_influencing` | `iStockInfluencing` | `BOOLEAN` |
| `order_numbers` | `iOrderNumbers` | `VARCHAR(500)` |
| `voucher_id` `FK` | `iVoucher` | `INTEGER → tbl_voucher` |

---

### tbl_creditinvoice 🔧 (extends SSInvoice)
**Java-klass:** `SSCreditInvoice`

Extra fält utöver SSInvoice:

| SQL-fält | Java-fält | Typ |
|----------|-----------|-----|
| `crediting_nr` | `iCreditingNr` | `INTEGER` |

---

### tbl_periodicinvoice 🔧 (extends SSInvoice eller SSSale)
**Java-klass:** `SSPeriodicInvoice`

> Kräver genomgång av fälten — innehåller periodicitetslogik.

---

### tbl_order 🔧 (extends SSSale)
**Java-klass:** `SSOrder`

> Kräver genomgång av fälten.

---

### tbl_tender 🔧 (extends SSSale)
**Java-klass:** `SSTender`

> Kräver genomgång av fälten.

---

## Typkategori E — Inköp (leverantörssidan)

### tbl_supplierinvoice 🔧
**Java-klass:** `SSSupplierInvoice`

Extra fält:

| SQL-fält | Java-fält | Typ |
|----------|-----------|-----|
| `supplier_nr` | `iSupplierNr` | `VARCHAR(50)` |
| `supplier_name` | `iSupplierName` | `VARCHAR(255)` |
| `reference_number` | `iReferencenumber` | `VARCHAR(100)` |
| `stock_influencing` | `iStockInfluencing` | `BOOLEAN` |
| `bgc_entered` | `iBGCEntered` | `BOOLEAN` |

> Innehåller också rader — child-tabell `tbl_supplierinvoice_row`.

---

### tbl_suppliercreditinvoice 🔧
**Java-klass:** `SSSupplierCreditInvoice`

> Liknar `SSSupplierInvoice` — kräver genomgång.

---

### tbl_purchaseorder 🔧
**Java-klass:** `SSPurchaseOrder`

> Kräver genomgång av fälten.

---

## Typkategori F — Betalningar

### tbl_inpayment 🔧
**Java-klass:** `SSInpayment`

> Kräver genomgång — innehåller betalningsrader (`SSInpaymentRow`).

---

### tbl_outpayment 🔧
**Java-klass:** `SSOutpayment`

> Kräver genomgång — innehåller `SSOutpaymentRow`.

---

## Typkategori G — Lager

### tbl_indelivery 🔧
**Java-klass:** `SSIndelivery`

> Kräver genomgång — innehåller `SSIndeliveryRow`.

---

### tbl_outdelivery 🔧
**Java-klass:** `SSOutdelivery`

> Kräver genomgång — innehåller `SSOutdeliveryRow`.

---

### tbl_inventory 🔧
**Java-klass:** `SSInventory`

> Kräver genomgång — innehåller `SSInventoryRow`.

---

### tbl_autodist 🔧
**Java-klass:** `SSAutoDist`

> Automatisk distribution av kostnader — kräver genomgång.

---

## Typkategori H — Misc

### tbl_vouchertemplate 🔧
**Java-klass:** `SSVoucherTemplate`

> Innehåller `List<SSVoucherTemplateRow>` — child-tabell krävs.

---

### tbl_ownreport 🔧
**Java-klass:** `SSOwnReport`

> Egna rapportmallar — kräver genomgång.

---

## Sammanfattning

| Kategori | Tabeller | Komplexitet | Kommentar |
|----------|----------|-------------|-----------|
| A: Referensdata | 5 | ⭐ Enkel | Starta här — inga FK-cykler |
| B: Masterdata | 6 | ⚙ Medium | Inbäddade adresser och value-objects |
| C: Bokföring | 3 | 🔩 Mycket komplex | SSAccountingYear innehåller hela år:s data |
| D: Försäljning | 5 | 🔧 Komplex | Gemensam bas SSSale + child-rader |
| E: Inköp | 3 | 🔧 Komplex | Liknar D men leverantörssidan |
| F: Betalningar | 2 | 🔧 Komplex | Betalningsrader |
| G: Lager | 3 | 🔧 Komplex | Lagerrörelsrader |
| H: Misc | 3 | 🔧 Komplex | Mallar och rapporter |
| **Totalt** | **30** | | |

### Antal Serializable-klasser
- **46** klasser implementerar `Serializable` (varav ~30 är direkta OBJECT-kolumntyper
  och resten är inbäddade value-objects eller backup-strukturer).

### Rekommenderad migreringsordning (Steg 2.2→2.3)
1. **Kategori A** (referensdata) — inga beroenden
2. **Kategori B masterdata** (customer, supplier, product, project, resultunit)
3. **Kategori C** (accountplan, accountingyear, voucher) — kärnan i bokföringen
4. **Kategori D+E** (försäljning + inköp) — beror på customer/supplier/product
5. **Kategori F+G+H** (betalningar, lager, misc)

---

*Dokument skapat: 2026-05-04 (Steg 2.1)*

