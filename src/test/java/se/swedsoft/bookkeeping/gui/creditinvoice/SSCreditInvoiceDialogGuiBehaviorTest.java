package se.swedsoft.bookkeeping.gui.creditinvoice;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.gui.creditinvoice.util.SSCreditInvoiceTableModel;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;

import static org.assertj.core.api.Assertions.assertThat;

class SSCreditInvoiceDialogGuiBehaviorTest {

    @Test
    void cancelledCreditInvoiceShouldShowCancelledIconInList() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setCancelled();

        Object icon = SSCreditInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_DELETE16", SSIcon.IconState.NORMAL));
    }

    @Test
    void printedButNotEnteredCreditInvoiceShouldShowPrintedIconInList() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setPrinted(true);
        invoice.setEntered(false);

        Object icon = SSCreditInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_PRINTED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void enteredButNotPrintedCreditInvoiceShouldShowEnteredIconInList() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setPrinted(false);
        invoice.setEntered(true);

        Object icon = SSCreditInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void printedAndEnteredCreditInvoiceShouldShowPropertiesIconInList() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setPrinted(true);
        invoice.setEntered(true);

        Object icon = SSCreditInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_PROPERTIES16", SSIcon.IconState.NORMAL));
    }

    @Test
    void newCreditInvoiceShouldShowNoIconInList() {
        SSCreditInvoice invoice = new SSCreditInvoice();

        Object icon = SSCreditInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isNull();
    }

    @Test
    void unprintedAndUnenteredCreditInvoiceShouldNotOpenLocked() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setNumber(10);
        invoice.setCreditingNr(10);
        invoice.setPrinted(false);
        invoice.setEntered(false);

        assertThat(SSCreditInvoiceDialog.shouldShowEditLockedInfo(invoice)).isFalse();
        assertThat(SSCreditInvoiceDialog.shouldOpenReadOnly(invoice)).isFalse();
    }

    @Test
    void printedCreditInvoiceShouldOpenInKonteringPossibleMode() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setPrinted(true);
        invoice.setEntered(false);

        assertThat(SSCreditInvoiceDialog.shouldOpenReadOnly(invoice)).isTrue();
        assertThat(SSCreditInvoiceDialog.shouldOpenKonteringPossibleMode(invoice)).isTrue();
    }
}
