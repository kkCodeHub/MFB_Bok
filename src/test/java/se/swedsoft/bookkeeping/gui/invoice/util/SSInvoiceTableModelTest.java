package se.swedsoft.bookkeeping.gui.invoice.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoiceTableModelTest {

    @Test
    void dropdownModelShouldKeepCustomerColumnsUnchanged() {
        SSInvoiceTableModel model = SSInvoiceTableModel.getDropDownModel(List.of(new SSInvoice()));

        assertThat(model.getColumns()).containsExactly(
                SSInvoiceTableModel.COLUMN_NUMBER,
                SSInvoiceTableModel.COLUMN_CUSTOMER_NR,
                SSInvoiceTableModel.COLUMN_CUSTOMER_NAME);
    }
}
