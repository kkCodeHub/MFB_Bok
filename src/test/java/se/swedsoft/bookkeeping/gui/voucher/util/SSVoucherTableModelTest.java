package se.swedsoft.bookkeeping.gui.voucher.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherTableModelTest {

    @Test
    void voucherListIncludesSeriesAsFirstColumn() {
        SSVoucher voucher = new SSVoucher(10);
        voucher.setSeries("C");

        SSVoucherTableModel model = new SSVoucherTableModel(List.of(voucher));
        model.addColumn(SSVoucherTableModel.COLUMN_SERIES);
        model.addColumn(SSVoucherTableModel.COLUMN_NUMBER);
        model.addColumn(SSVoucherTableModel.COLUMN_DATE);
        model.addColumn(SSVoucherTableModel.COLUMN_DESCRIPTION);
        model.addColumn(SSVoucherTableModel.COLUMN_SUM);
        model.addColumn(SSVoucherTableModel.COLUMN_CORRECTS);
        model.addColumn(SSVoucherTableModel.COLUMN_CORRECTEDBY);

        assertThat(model.getColumnName(0)).isEqualTo("Serie");
        assertThat(model.getColumnName(1)).isEqualTo("Nummer");
        assertThat(model.getValueAt(0, 0)).isEqualTo("C");
        assertThat(model.getValueAt(0, 1)).isEqualTo(10);
        assertThat(SSVoucherTableModel.COLUMN_SERIES.getDefaultWidth())
                .isEqualTo(SSVoucherTableModel.COLUMN_NUMBER.getDefaultWidth());
    }
}
