package se.swedsoft.bookkeeping.gui.creditinvoice.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSCreditInvoiceTableModelTest {

    @Test
    void dropdownModelShouldKeepCreditInvoiceColumnsUnchanged() {
        SSCreditInvoiceTableModel model = new SSCreditInvoiceTableModel(List.of(new SSCreditInvoice()));
        model.addColumn(SSCreditInvoiceTableModel.COLUMN_NUMBER);
        model.addColumn(SSCreditInvoiceTableModel.COLUMN_CUSTOMER_NR);
        model.addColumn(SSCreditInvoiceTableModel.COLUMN_CUSTOMER_NAME);

        assertThat(model.getColumns()).containsExactly(
                SSCreditInvoiceTableModel.COLUMN_NUMBER,
                SSCreditInvoiceTableModel.COLUMN_CUSTOMER_NR,
                SSCreditInvoiceTableModel.COLUMN_CUSTOMER_NAME);
    }

    @Test
    void printedColumnShouldStillBeFirstStatusColumn() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setCancelled();

        SSCreditInvoiceTableModel model = new SSCreditInvoiceTableModel(List.of(invoice));
        model.addColumn(SSCreditInvoiceTableModel.COLUMN_PRINTED);

        assertThat(model.getColumns()).containsExactly(SSCreditInvoiceTableModel.COLUMN_PRINTED);
    }
}
