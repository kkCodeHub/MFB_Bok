package se.swedsoft.bookkeeping.gui.suppliercreditinvoice.dialog;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSelectSupplierInvoiceDialogTest {

    @Test
    void shouldFilterOutCancelledSupplierInvoices() {
        SSSupplierInvoice active = supplierInvoice(1001, false);
        active.setEntered(true);
        SSSupplierInvoice cancelled = supplierInvoice(1002, true);
        cancelled.setEntered(true);

        List<SSSupplierInvoice> filtered = SSSelectSupplierInvoiceDialog
                .filterSelectableSupplierInvoices(List.of(active, cancelled));

        assertThat(filtered).containsExactly(active);
    }

    @Test
    void shouldFilterOutUnenteredSupplierInvoices() {
        SSSupplierInvoice entered = supplierInvoice(2001, false);
        entered.setEntered(true);
        SSSupplierInvoice unentered = supplierInvoice(2002, false);

        List<SSSupplierInvoice> filtered = SSSelectSupplierInvoiceDialog
                .filterSelectableSupplierInvoices(List.of(entered, unentered));

        assertThat(filtered).containsExactly(entered);
    }

    private static SSSupplierInvoice supplierInvoice(int pNumber, boolean pCancelled) {
        SSSupplierInvoice iInvoice = new SSSupplierInvoice(true);
        iInvoice.setNumber(pNumber);
        iInvoice.setCancelled(pCancelled);
        return iInvoice;
    }
}
