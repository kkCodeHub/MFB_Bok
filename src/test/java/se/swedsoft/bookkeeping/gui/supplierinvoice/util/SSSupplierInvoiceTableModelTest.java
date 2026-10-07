package se.swedsoft.bookkeeping.gui.supplierinvoice.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierInvoiceTableModelTest {

    @Test
    void stateColumnShouldShowCancelledIconForCancelledSupplierInvoice() {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setCancelled(true);
        invoice.setEntered(true);

        SSSupplierInvoiceTableModel model = new SSSupplierInvoiceTableModel(List.of(invoice));
        model.addColumn(SSSupplierInvoiceTableModel.COLUMN_STATE);

        assertThat(model.getColumns()).containsExactly(SSSupplierInvoiceTableModel.COLUMN_STATE);
        assertThat(model.getColumnName(0)).isEmpty();
        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_DELETE16", SSIcon.IconState.NORMAL));
    }

    @Test
    void stateColumnShouldShowEnteredIconForEnteredSupplierInvoice() {
        SSSupplierInvoice invoice = new SSSupplierInvoice();
        invoice.setEntered(true);

        SSSupplierInvoiceTableModel model = new SSSupplierInvoiceTableModel(List.of(invoice));
        model.addColumn(SSSupplierInvoiceTableModel.COLUMN_STATE);

        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void dropdownModelShouldNotIncludeStateColumn() {
        SSTableModel<SSSupplierInvoice> model = SSSupplierInvoiceTableModel.getDropDownModel(
                List.of(new SSSupplierInvoice()));

        assertThat(model.getColumns()).containsExactly(
                SSSupplierInvoiceTableModel.COLUMN_NUMBER,
                SSSupplierInvoiceTableModel.COLUMN_REFERENCE_NUMBER,
                SSSupplierInvoiceTableModel.COLUMN_SUPPLIER_NUMBER,
                SSSupplierInvoiceTableModel.COLUMN_SUPPLIER_NAME);
    }
}
