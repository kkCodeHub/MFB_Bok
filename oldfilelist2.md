# oldfilelist2

Skapad: 2026-07-31T01:48:19
Avgransning: Metoder i src/main/java som har verifierade direkta symbolanrop i src/test/java och inga verifierade direkta symbolanrop i src/main/java.
Metod: Semantisk Java-analys via javac/Trees API med upplosta method symbols, vilket ger hogre traffsakerhet an namn- eller regexsokning.

## `src\main\java\se\swedsoft\bookkeeping\calc\SSOCRNumber.java`
- rad 45: `getCheckSum(Integer iText)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSAccountMath.java`
- rad 184: `getAccounts(List<SSAccount> pAccounts, SSAccount pFrom, SSAccount pTo)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 355: `getNumAccountsBySRUCode(List<SSAccount> pAccounts, String... pSruCodes)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSDateMath.java`
- rad 53: `getMonthsBetween(LocalDate from, LocalDate to)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 67: `getDaysBetween(LocalDate from, LocalDate to)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSProductQuantityValidator.java`
- rad 55: `isWholeQuantity(Integer quantityTenths)` - 0 verifierade anrop i src/main/java, 8 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\math\SSVoucherMath.java`
- rad 454: `setCreditMinusDebet(SSVoucherRow iVoucherRow, BigDecimal iValue)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\calc\util\SSAutoIncrement.java`
- rad 61: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSAccount.java`
- rad 72: `copyFrom(SSAccount pAccount)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 209: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 218: `toRenderString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 240: `hashCode()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSAccountPlanType.java`
- rad 43: `get(String name)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 52: `getDefault()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSAddress.java`
- rad 44: `dispose()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 239: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSCustomer.java`
- rad 616: `toRenderString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 620: `hashCode()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 624: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSInvoice.java`
- rad 435: `hasCustomer(SSCustomer iCustomer)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 538: `equals(Object obj)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSMonth.java`
- rad 48: `getLocalTo()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 64: `equals(Object obj)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSNewAccountingYear.java`
- rad 270: `setInBalance(SSAccount pAccount, BigDecimal pAmount)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSProduct.java`
- rad 753: `hashCode()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSSupplier.java`
- rad 482: `hashCode()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 494: `toRenderString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 498: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSVoucher.java`
- rad 221: `addVoucherRow(SSAccount iAccount, BigDecimal iDebet, BigDecimal iCredit)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 236: `addVoucherRow(SSAccount iAccount, BigDecimal iValue)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 263: `removeVoucherRow(SSVoucherRow row)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 277: `equals(Object obj)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 313: `toRenderString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\SSVoucherRow.java`
- rad 492: `getValue()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 565: `isEmpty()` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 574: `hasAccount(SSAccount iAccount)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSAccountingContext.java`
- rad 205: `getLastVoucherNumber()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSCompanyYearContext.java`
- rad 166: `addAccountingYear(SSNewAccountingYear pYear)` - 0 verifierade anrop i src/main/java, 31 verifierade anrop i src/test/java
- rad 203: `getYearsForCompany(SSNewCompany pCompany)` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSDB.java`
- rad 579: `setCurrentCompany(SSNewCompany iCompany)` - 0 verifierade anrop i src/main/java, 47 verifierade anrop i src/test/java
- rad 590: `setCurrentYear(SSNewAccountingYear iYear)` - 0 verifierade anrop i src/main/java, 36 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSInventoryDeliveriesContext.java`
- rad 32: `getInventories()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 42: `getInventory(SSInventory pInventory)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 60: `addInventory(SSInventory pInventory)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 69: `updateInventory(SSInventory pInventory)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 78: `deleteInventory(SSInventory pInventory)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 91: `getIndeliveries()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 101: `getIndelivery(SSIndelivery pIndelivery)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 119: `addIndelivery(SSIndelivery pIndelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 128: `updateIndelivery(SSIndelivery pIndelivery)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 137: `deleteIndelivery(SSIndelivery pIndelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 150: `getOutdeliveries()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 160: `getOutdelivery(SSOutdelivery pOutdelivery)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 178: `addOutdelivery(SSOutdelivery pOutdelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 187: `updateOutdelivery(SSOutdelivery pOutdelivery)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 196: `deleteOutdelivery(SSOutdelivery pOutdelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSMasterdataContext.java`
- rad 26: `getCustomers()` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 37: `getCustomer(String pCustomerNumber)` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 45: `addCustomer(SSCustomer pCustomer)` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 49: `updateCustomer(SSCustomer pCustomer)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 53: `deleteCustomer(SSCustomer pCustomer)` - 0 verifierade anrop i src/main/java, 11 verifierade anrop i src/test/java
- rad 57: `getSuppliers()` - 0 verifierade anrop i src/main/java, 6 verifierade anrop i src/test/java
- rad 61: `getSupplier(SSSupplier pSupplier)` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 72: `addSupplier(SSSupplier pSupplier)` - 0 verifierade anrop i src/main/java, 9 verifierade anrop i src/test/java
- rad 76: `updateSupplier(SSSupplier pSupplier)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 80: `deleteSupplier(SSSupplier pSupplier)` - 0 verifierade anrop i src/main/java, 9 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSPaymentContext.java`
- rad 32: `getInpayments()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 42: `getInpayment(SSInpayment pInpayment)` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java
- rad 51: `addInpayment(SSInpayment pInpayment)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 60: `updateInpayment(SSInpayment pInpayment)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 69: `deleteInpayment(SSInpayment pInpayment)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 82: `getOutpayments()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 92: `getOutpayment(SSOutpayment pOutpayment)` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java
- rad 101: `addOutpayment(SSOutpayment pOutpayment)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 110: `updateOutpayment(SSOutpayment pOutpayment)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 119: `deleteOutpayment(SSOutpayment pOutpayment)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSPurchaseContext.java`
- rad 95: `getPurchaseOrders()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 115: `getPurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 124: `addPurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 133: `updatePurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 142: `deletePurchaseOrder(SSPurchaseOrder pPurchaseOrder)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 155: `getSupplierInvoices()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 175: `getSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 184: `addSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 193: `updateSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 202: `deleteSupplierInvoice(SSSupplierInvoice pSupplierInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 215: `getSupplierCreditInvoices()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 244: `getSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 254: `addSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 263: `updateSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 272: `deleteSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSSalesContext.java`
- rad 96: `getTenders()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 116: `getTender(SSTender pTender)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 125: `addTender(SSTender pTender)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 134: `updateTender(SSTender pTender)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 143: `deleteTender(SSTender pTender)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\data\system\SSSystemConfigContext.java`
- rad 13: `startupLocal(Connection pConnection)` - 0 verifierade anrop i src/main/java, 49 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\gui\product\util\SSProductRowTableModel.java`
- rad 101: `setValueAt(Object aValue, int rowIndex, int columnIndex)` - 0 verifierade anrop i src/main/java, 6 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\gui\util\SSQuantityPresentationUtil.java`
- rad 20: `toDisplayQuantity(Integer storedTenths)` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java
- rad 33: `toStoredTenths(Object uiValue)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 56: `toStoredTenths(BigDecimal uiQuantity)` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxAvsnitt.java`
- rad 42: `getBankgiroNummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 70: `getBankKontoNummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 112: `getBelopp()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 140: `getAntal()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxBetalning.java`
- rad 67: `getBankgiroNummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 104: `getBeloppRaw()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 118: `getReferensKod()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 132: `getBetalningsKanalKod()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 146: `getBGCLopnummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 160: `getAvibildmarkering()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxFile.java`
- rad 52: `getLayoutnamn()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 59: `getVersion()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 66: `getTidsstampel()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 80: `getAntalBetalningsPoster()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 87: `getAntalExtraReferensPoster()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 94: `getAntalAvdragsPoster()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 101: `getAntalInsattningsPoster()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxLine.java`
- rad 32: `getTransaktionsKod()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 41: `getField(int iStart)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 52: `getField(int iStart, int iEnd)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\bgmax\data\BgMaxReferens.java`
- rad 24: `getBankgiroNummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 52: `getBelopp()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 66: `getReferensKod()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 80: `getBetalningsKanalKod()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 94: `getBGCLopnummer()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 108: `getAvibildmarkering()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSAccountPlanDefaultResourceDiscovery.java`
- rad 29: `discoverDefaultExcelResources(ClassLoader classLoader)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSAccountPlanImporter.java`
- rad 99: `validateFileNameAgainstPlanName(String fileName, SSAccountPlan accountPlan)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSCustomerExporter.java`
- rad 99: `export()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 195: `doXMLExport()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSCustomerImporter.java`
- rad 423: `doImport()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSProductExporter.java`
- rad 200: `doXMLExport()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 367: `tenthsToDecimalString(Integer tenths)` - 0 verifierade anrop i src/main/java, 8 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSProductImporter.java`
- rad 376: `doXMLImport()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\SSVoucherImporter.java`
- rad 51: `Import()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 275: `buildImportReportText(List<SSVoucher> iImportedVouchers, List<Integer> iDuplicateNumbers)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\util\SSExcelCell.java`
- rad 41: `getString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\util\SSExcelRow.java`
- rad 41: `empty()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\excel\util\SSExcelWorkbookReader.java`
- rad 30: `openWorkbook(File pFile)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\sie\SSSIEImporter.java`
- rad 68: `doImport()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\sie\fields\SIEEntrySRU.java`
- rad 37: `importEntry(SSSIEImporter iImporter, SIEReader iReader, SSNewAccountingYear iYearData)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\sie\util\SIEReader.java`
- rad 76: `hasNextLine()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 89: `nextLine()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 119: `hasNext()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 124: `next()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 136: `peek()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 140: `remove()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 154: `hasFields(SIEDataType... pDataTypes)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 190: `hasNextString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 198: `hasNextInteger()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 206: `hasNextFloat()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 222: `hasNextBoolean()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 246: `hasNextArray()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 254: `hasNextDate()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 270: `nextString()` - 0 verifierade anrop i src/main/java, 8 verifierade anrop i src/test/java
- rad 278: `nextInteger()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 294: `nextDouble()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 302: `nextBoolean()` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 310: `nextBigInteger()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 318: `nextBigDecimal()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 326: `nextArray()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 334: `nextDate()` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\sie\util\SIEWriter.java`
- rad 40: `newLine()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 50: `newLine(String iLine)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 60: `append(String pValue)` - 0 verifierade anrop i src/main/java, 9 verifierade anrop i src/test/java
- rad 84: `append(Object... pValues)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 98: `append(List<Object> pValues)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 112: `append(SSMonth pValue)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 128: `append(SIELabel pValue)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 146: `append(Float pValue)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 174: `append(boolean pValue)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 183: `append(BigInteger pValue)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 192: `append(BigDecimal pValue)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 201: `append(LocalDate pValue)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 214: `getLine()` - 0 verifierade anrop i src/main/java, 29 verifierade anrop i src/test/java
- rad 222: `getLines()` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 230: `toString()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\xml\SSOrderExporter.java`
- rad 48: `doExport()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 393: `tenthsToDecimalString(Integer tenths)` - 0 verifierade anrop i src/main/java, 8 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\importexport\xml\SSOrderImporter.java`
- rad 1074: `parseQuantityToTenths(String iValue)` - 0 verifierade anrop i src/main/java, 11 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\Repositories.java`
- rad 104: `isSchemaV2()` - 0 verifierade anrop i src/main/java, 45 verifierade anrop i src/test/java
- rad 221: `customers()` - 0 verifierade anrop i src/main/java, 29 verifierade anrop i src/test/java
- rad 232: `autoDists()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 243: `currencies()` - 0 verifierade anrop i src/main/java, 11 verifierade anrop i src/test/java
- rad 254: `deliveryTerms()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 265: `deliveryWays()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 276: `paymentTerms()` - 0 verifierade anrop i src/main/java, 18 verifierade anrop i src/test/java
- rad 287: `units()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 298: `creditInvoices()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 348: `invoices()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 359: `ownReports()` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 370: `orders()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 407: `periodicInvoices()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java
- rad 418: `products()` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 486: `accountPlans()` - 0 verifierade anrop i src/main/java, 24 verifierade anrop i src/test/java
- rad 497: `vouchers()` - 0 verifierade anrop i src/main/java, 11 verifierade anrop i src/test/java
- rad 508: `voucherTemplates()` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 519: `accountingYears()` - 0 verifierade anrop i src/main/java, 22 verifierade anrop i src/test/java
- rad 541: `projects()` - 0 verifierade anrop i src/main/java, 15 verifierade anrop i src/test/java
- rad 552: `resultUnits()` - 0 verifierade anrop i src/main/java, 15 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2AccountPlanRepository.java`
- rad 59: `findById(int id)` - 0 verifierade anrop i src/main/java, 6 verifierade anrop i src/test/java
- rad 76: `add(SSAccountPlan plan)` - 0 verifierade anrop i src/main/java, 9 verifierade anrop i src/test/java
- rad 104: `update(SSAccountPlan plan)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 132: `delete(SSAccountPlan plan)` - 0 verifierade anrop i src/main/java, 8 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2AccountingYearRepository.java`
- rad 85: `findCurrent()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 89: `add(SSNewAccountingYear year)` - 0 verifierade anrop i src/main/java, 7 verifierade anrop i src/test/java
- rad 141: `update(SSNewAccountingYear year)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 196: `delete(SSNewAccountingYear year)` - 0 verifierade anrop i src/main/java, 7 verifierade anrop i src/test/java
- rad 271: `open(SSNewAccountingYear year)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2AutoDistRepository.java`
- rad 61: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 97: `findByAutoDist(SSAutoDist autoDist)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 152: `add(SSAutoDist autoDist)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 193: `update(SSAutoDist autoDist, SSAutoDist original)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 226: `delete(SSAutoDist autoDist)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2CreditInvoiceRepository.java`
- rad 71: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 107: `findByCreditInvoice(SSCreditInvoice creditInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 162: `add(SSCreditInvoice creditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 231: `update(SSCreditInvoice creditInvoice)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 270: `delete(SSCreditInvoice creditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2CurrencyRepository.java`
- rad 54: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 75: `findByCode(String code)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 100: `add(SSCurrency currency)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 118: `update(SSCurrency currency)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 136: `delete(SSCurrency currency)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2CustomerRepository.java`
- rad 74: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 113: `findByNumber(String customerNumber)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 175: `findAll(List<SSCustomer> customers)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 208: `add(SSCustomer customer)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 242: `update(SSCustomer customer)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 305: `delete(SSCustomer customer)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2DeliveryTermRepository.java`
- rad 53: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 72: `findByName(String name)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 95: `add(SSDeliveryTerm deliveryTerm)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 112: `update(SSDeliveryTerm deliveryTerm)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 129: `delete(SSDeliveryTerm deliveryTerm)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2DeliveryWayRepository.java`
- rad 53: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 72: `findByName(String name)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 95: `add(SSDeliveryWay deliveryWay)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 112: `update(SSDeliveryWay deliveryWay)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 129: `delete(SSDeliveryWay deliveryWay)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2IndeliveryRepository.java`
- rad 101: `findByIndelivery(SSIndelivery indelivery)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 133: `add(SSIndelivery indelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 191: `update(SSIndelivery indelivery)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 222: `delete(SSIndelivery indelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2InpaymentRepository.java`
- rad 101: `findByInpayment(SSInpayment inpayment)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 127: `add(SSInpayment inpayment)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 229: `delete(SSInpayment inpayment)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2InventoryRepository.java`
- rad 97: `findByInventory(SSInventory inventory)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 129: `add(SSInventory inventory)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 187: `update(SSInventory inventory)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 218: `delete(SSInventory inventory)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2InvoiceRepository.java`
- rad 71: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 107: `findByInvoice(SSInvoice invoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 162: `add(SSInvoice invoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 229: `update(SSInvoice invoice)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 267: `delete(SSInvoice invoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2OrderRepository.java`
- rad 70: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 106: `findByOrder(SSOrder order)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 161: `add(SSOrder order)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 227: `update(SSOrder order)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 265: `delete(SSOrder order)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2OutdeliveryRepository.java`
- rad 108: `findByOutdelivery(SSOutdelivery outdelivery)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 150: `add(SSOutdelivery outdelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 214: `update(SSOutdelivery outdelivery)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 251: `delete(SSOutdelivery outdelivery)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2OutpaymentRepository.java`
- rad 101: `findByOutpayment(SSOutpayment outpayment)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 127: `add(SSOutpayment outpayment)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 229: `delete(SSOutpayment outpayment)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2OwnReportRepository.java`
- rad 102: `findByOwnReport(SSOwnReport ownReport)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 166: `findAll(List<SSOwnReport> subset)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 200: `add(SSOwnReport ownReport)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 242: `update(SSOwnReport ownReport)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 277: `delete(SSOwnReport ownReport)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2PaymentTermRepository.java`
- rad 55: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 74: `findByName(String name)` - 0 verifierade anrop i src/main/java, 6 verifierade anrop i src/test/java
- rad 97: `add(SSPaymentTerm paymentTerm)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 115: `update(SSPaymentTerm paymentTerm)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 136: `delete(SSPaymentTerm paymentTerm)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2PeriodicInvoiceRepository.java`
- rad 63: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 99: `findByPeriodicInvoice(SSPeriodicInvoice periodicInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 125: `add(SSPeriodicInvoice periodicInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 194: `update(SSPeriodicInvoice periodicInvoice)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 234: `delete(SSPeriodicInvoice periodicInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2ProductRepository.java`
- rad 64: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 144: `add(SSProduct product)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 189: `update(SSProduct product)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 243: `delete(SSProduct product)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2ProjectRepository.java`
- rad 59: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 82: `findAll(List<SSNewProject> pProjects)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 121: `findByNumber(String pProjectNumber)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 147: `add(SSNewProject pProject)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 173: `update(SSNewProject pProject)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 199: `delete(SSNewProject pProject)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2PurchaseOrderRepository.java`
- rad 100: `findByPurchaseOrder(SSPurchaseOrder purchaseOrder)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 155: `add(SSPurchaseOrder purchaseOrder)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 261: `delete(SSPurchaseOrder purchaseOrder)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2ResultUnitRepository.java`
- rad 58: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 81: `findAll(List<SSNewResultUnit> pResultUnits)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 120: `findByNumber(String pResultUnitNumber)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 146: `add(SSNewResultUnit pResultUnit)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 170: `update(SSNewResultUnit pResultUnit)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 194: `delete(SSNewResultUnit pResultUnit)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2SupplierCreditInvoiceRepository.java`
- rad 96: `findBySupplierCreditInvoice(SSSupplierCreditInvoice supplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 122: `add(SSSupplierCreditInvoice supplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 222: `delete(SSSupplierCreditInvoice supplierCreditInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2SupplierInvoiceRepository.java`
- rad 96: `findBySupplierInvoice(SSSupplierInvoice supplierInvoice)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 151: `add(SSSupplierInvoice supplierInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 250: `delete(SSSupplierInvoice supplierInvoice)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2SupplierRepository.java`
- rad 112: `findBySupplier(SSSupplier supplier)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 143: `findAll(List<SSSupplier> suppliers)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 176: `add(SSSupplier supplier)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 208: `update(SSSupplier supplier)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 260: `delete(SSSupplier supplier)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2TenderRepository.java`
- rad 106: `findByTender(SSTender tender)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 161: `add(SSTender tender)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 263: `delete(SSTender tender)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2UnitRepository.java`
- rad 53: `findAll()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 72: `findByName(String name)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 95: `add(SSUnit unit)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 112: `update(SSUnit unit)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 129: `delete(SSUnit unit)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2VoucherRepository.java`
- rad 63: `findByYear(SSNewAccountingYear year)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 127: `add(SSVoucher voucher)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 131: `update(SSVoucher voucher)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 158: `delete(SSVoucher voucher)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\V2VoucherTemplateRepository.java`
- rad 55: `findAll()` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 78: `findAll(List<SSVoucherTemplate> subset)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java
- rad 110: `add(SSVoucherTemplate voucherTemplate)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 155: `delete(SSVoucherTemplate voucherTemplate)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\schema\SSSchemaBuilder.java`
- rad 45: `createBaseTables()` - 0 verifierade anrop i src/main/java, 13 verifierade anrop i src/test/java
- rad 99: `createLocalTriggers()` - 0 verifierade anrop i src/main/java, 7 verifierade anrop i src/test/java
- rad 181: `dropTriggers()` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\schema\SSSchemaEnsurer.java`
- rad 46: `ensureAllForwardCompatibility()` - 0 verifierade anrop i src/main/java, 13 verifierade anrop i src/test/java
- rad 305: `ensureColumnExists(String tableName, String columnName, String columnDefinition)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 351: `dropConstraintIfExists(String tableName, String constraintName)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\persistence\v2\schema\SSSchemaMigrationManager.java`
- rad 95: `ensureQuantityScaleMigration()` - 0 verifierade anrop i src/main/java, 12 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\print\report\sales\SSSalePrinterUtils.java`
- rad 93: `getImage(final File iImageFile)` - 0 verifierade anrop i src/main/java, 2 verifierade anrop i src/test/java
- rad 115: `getPrimaryPaymentMethod(final SSNewCompany iCompany)` - 0 verifierade anrop i src/main/java, 5 verifierade anrop i src/test/java
- rad 130: `getPrimaryPaymentAccount(final SSNewCompany iCompany)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\print\util\SSQuantityPrintUtil.java`
- rad 20: `toDisplay(Integer tenths)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\util\SSDateUtil.java`
- rad 34: `toLocalDate(Date date)` - 0 verifierade anrop i src/main/java, 10 verifierade anrop i src/test/java
- rad 52: `toLocalDateTime(Date date)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 77: `toDate(LocalDate localDate)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 90: `toDate(LocalDateTime localDateTime)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 109: `today()` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 120: `now()` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

## `src\main\java\se\swedsoft\bookkeeping\util\SSUtil.java`
- rad 18: `isNullOrEmpty(String s)` - 0 verifierade anrop i src/main/java, 4 verifierade anrop i src/test/java
- rad 28: `verifyNotNull(String message, Object... objs)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 36: `convertNullToEmpty(String s)` - 0 verifierade anrop i src/main/java, 3 verifierade anrop i src/test/java
- rad 40: `isInRage(int i, int low, int high)` - 0 verifierade anrop i src/main/java, 7 verifierade anrop i src/test/java
- rad 48: `readResourceToString(String name)` - 0 verifierade anrop i src/main/java, 1 verifierade anrop i src/test/java

