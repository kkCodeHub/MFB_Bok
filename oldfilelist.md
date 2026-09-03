# oldfilelist

Skapad: 2026-07-31 01:22:25
Avgransning: Metoder i src/main/java som saknar direkta Java-anrop i src/main/java men har direkta Java-anrop i src/test/java.
Not: Statisk namn-baserad sokning. Overlagrade metoder och indirekta anrop kan missas.

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSAccountMath.java`
- rad 355: `getNumAccountsBySRUCode(List<SSAccount> pAccounts, String... pSruCodes)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSDateMath.java`
- rad 53: `getMonthsBetween(LocalDate from, LocalDate to)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 67: `getDaysBetween(LocalDate from, LocalDate to)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSProductQuantityValidator.java`
- rad 55: `isWholeQuantity(Integer quantityTenths)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSVoucherMath.java`
- rad 454: `setCreditMinusDebet(SSVoucherRow iVoucherRow, BigDecimal iValue)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSVoucher.java`
- rad 263: `removeVoucherRow(SSVoucherRow row)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSCompanyYearContext.java`
- rad 83: `deleteCompany(SSNewCompany pCompany)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 138: `getCompanies()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 203: `getYearsForCompany(SSNewCompany pCompany)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSInventoryDeliveriesContext.java`
- rad 32: `getInventories()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 60: `addInventory(SSInventory pInventory)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 69: `updateInventory(SSInventory pInventory)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 78: `deleteInventory(SSInventory pInventory)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 91: `getIndeliveries()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 119: `addIndelivery(SSIndelivery pIndelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 128: `updateIndelivery(SSIndelivery pIndelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 137: `deleteIndelivery(SSIndelivery pIndelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 150: `getOutdeliveries()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 178: `addOutdelivery(SSOutdelivery pOutdelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 187: `updateOutdelivery(SSOutdelivery pOutdelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 196: `deleteOutdelivery(SSOutdelivery pOutdelivery)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSPaymentContext.java`
- rad 51: `addInpayment(SSInpayment pInpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 60: `updateInpayment(SSInpayment pInpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 69: `deleteInpayment(SSInpayment pInpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 101: `addOutpayment(SSOutpayment pOutpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 110: `updateOutpayment(SSOutpayment pOutpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 119: `deleteOutpayment(SSOutpayment pOutpayment)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSPurchaseContext.java`
- rad 124: `addPurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 133: `updatePurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 142: `deletePurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 184: `addSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 193: `updateSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 202: `deleteSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 254: `addSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 263: `updateSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 272: `deleteSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSSalesContext.java`
- rad 125: `addTender(SSTender pTender)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 134: `updateTender(SSTender pTender)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 143: `deleteTender(SSTender pTender)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 203: `deleteOrder(SSOrder pOrder)` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxAvsnitt.java`
- rad 70: `getBankKontoNummer()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 140: `getAntal()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxBetalning.java`
- rad 104: `getBeloppRaw()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 211: `getBetalarensNamn()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 239: `getBetalarensAdress()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 253: `getBetalarensPostnummer()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 267: `getBetalarensOrt()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 309: `getBetalarensOrganisationsnr()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxFile.java`
- rad 52: `getLayoutnamn()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 59: `getVersion()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 66: `getTidsstampel()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 80: `getAntalBetalningsPoster()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 87: `getAntalExtraReferensPoster()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 94: `getAntalAvdragsPoster()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java
- rad 101: `getAntalInsattningsPoster()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\sie\util\SIEWriter.java`
- rad 214: `getLine()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\Repositories.java`
- rad 104: `isSchemaV2()` - Inga direkta anrop i src/main/java, men direkta anrop i src/test/java



