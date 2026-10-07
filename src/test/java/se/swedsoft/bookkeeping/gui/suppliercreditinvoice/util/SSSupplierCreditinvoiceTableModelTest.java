package se.swedsoft.bookkeeping.gui.suppliercreditinvoice.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierCreditinvoiceTableModelTest {

    @Test
    void stateColumnShouldShowCancelledIconForCancelledSupplierCreditInvoice() {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice();
        invoice.setCancelled();

        SSSupplierCreditinvoiceTableModel model = new SSSupplierCreditinvoiceTableModel();
        model.setObjects(List.of(invoice));
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_STATE);

        assertThat(model.getColumnCount()).isEqualTo(1);
        assertThat(model.getColumns().get(0)).isSameAs(SSSupplierCreditinvoiceTableModel.COLUMN_STATE);
        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_DELETE16", SSIcon.IconState.NORMAL));
    }

    @Test
    void stateColumnShouldShowEnteredIconForEnteredSupplierCreditInvoice() {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice();
        invoice.setEntered(true);

        SSSupplierCreditinvoiceTableModel model = new SSSupplierCreditinvoiceTableModel();
        model.setObjects(List.of(invoice));
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_STATE);

        assertThat(model.getValueAt(0, 0))
                .isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void frameColumnsShouldRemainUnchangedForSupplierCreditInvoices() {
        SSSupplierCreditinvoiceTableModel model = new SSSupplierCreditinvoiceTableModel();
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_STATE);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_NUMBER);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_CREDITNING);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_SUPPLIER_NUMBER);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_SUPPLIER_NAME);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_DATE);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_NETSUM);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_CURRENCY);
        model.addColumn(SSSupplierCreditinvoiceTableModel.COLUMN_TOTALSUM);

        assertThat(model.getColumnCount()).isEqualTo(9);
        assertThat(model.getColumns().get(0)).isSameAs(SSSupplierCreditinvoiceTableModel.COLUMN_STATE);
        assertThat(model.getColumnName(1)).isEqualTo("Fakturanr");
        assertThat(model.getColumnName(2)).isEqualTo("Krediterar");
        assertThat(model.getColumnName(3)).isEqualTo("Leverantörs-id");
        assertThat(model.getColumnName(4)).isEqualTo("Leverantörsnamn");
        assertThat(model.getColumnName(5)).isEqualTo("Fakturadatum");
        assertThat(model.getColumnName(6)).isEqualTo("Nettosumma");
        assertThat(model.getColumnName(7)).isEqualTo("Valuta");
        assertThat(model.getColumnName(8)).isEqualTo("Totalsumma");
    }
}
