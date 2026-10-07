package se.swedsoft.bookkeeping.gui.supplierinvoice;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierInvoiceDialogGuiBehaviorTest {

    @Test
    void enteredSupplierInvoiceShouldOpenReadOnly() {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);
        invoice.setEntered(true);

        assertThat(SSSupplierInvoiceDialog.shouldShowEditLockedInfo(invoice)).isTrue();
        assertThat(SSSupplierInvoiceDialog.shouldOpenReadOnly(invoice)).isTrue();
    }

    @Test
    void cancelledSupplierInvoiceShouldOpenReadOnly() {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);
        invoice.setCancelled();

        assertThat(SSSupplierInvoiceDialog.shouldShowEditLockedInfo(invoice)).isTrue();
        assertThat(SSSupplierInvoiceDialog.shouldOpenReadOnly(invoice)).isTrue();
    }

    @Test
    void unlockedSupplierInvoiceShouldOpenEditable() {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);

        assertThat(SSSupplierInvoiceDialog.shouldShowEditLockedInfo(invoice)).isFalse();
        assertThat(SSSupplierInvoiceDialog.shouldOpenReadOnly(invoice)).isFalse();
    }
}
