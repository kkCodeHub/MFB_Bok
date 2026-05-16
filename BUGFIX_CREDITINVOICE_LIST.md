# Bug Fix: Credit Invoices Not Displayed in List

## Problem
Created credit invoices were not appearing in the credit invoices list (in `SSCreditInvoiceFrame`) even though they existed in the database.

## Root Cause
The issue was in `SSCreditInvoiceDialog.java`. When a new credit invoice was created or edited, the dialog code was:
1. Saving the credit invoice to the database via `SSDB.getInstance().addCreditInvoice()` or `SSDB.getInstance().updateCreditInvoice()`
2. Calling `SSInvoiceFrame.getInstance().updateFrame()` to refresh the invoices list
3. **BUT NOT** calling `SSCreditInvoiceFrame.getInstance().updateFrame()` to refresh the credit invoices list

This caused the newly created credit invoice to exist in the database but not be displayed in the credit invoices frame until the frame was manually refreshed (e.g., by pressing F5 or reopening it).

## Solution
Added the missing call to update `SSCreditInvoiceFrame` in three dialog methods:

### Changed Methods in `SSCreditInvoiceDialog.java`:

1. **editDialog()** - for editing existing credit invoices
2. **newDialog()** - for creating new credit invoices  
3. **copyDialog()** - for copying existing credit invoices

In each method, after updating the invoice frame, the code now also updates the credit invoice frame:

```java
if (SSCreditInvoiceFrame.getInstance() != null) {
    SSCreditInvoiceFrame.getInstance().updateFrame();
}
```

This ensures that:
- The credit invoices list is refreshed immediately after save
- The search panel applies the filter to reload the data from SSDB
- The newly created/edited credit invoice appears instantly in the list

## Testing
After applying this fix:
1. Create a new credit invoice and save it
2. The credit invoice should immediately appear in the credit invoices list
3. No need to manually refresh the list

## Files Modified
- `src/main/java/se/swedsoft/bookkeeping/gui/creditinvoice/SSCreditInvoiceDialog.java`

## Note
A similar issue may exist in `SSSupplierCreditInvoiceDialog.java` which uses the Repositories pattern instead of direct SSDB calls. This should be investigated separately.

