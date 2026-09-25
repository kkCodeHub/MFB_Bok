package se.swedsoft.bookkeeping.gui.voucher;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherDialogTest {

    @Test
    void formatVoucherReferenceIncludesSeriesAndNumber() {
        SSVoucher voucher = new SSVoucher(3);
        voucher.setSeries("a");

        assertThat(SSVoucherDialog.formatVoucherReference(voucher)).isEqualTo("A3");
    }

    @Test
    void formatVoucherReferenceNormalizesSeriesToUppercase() {
        SSVoucher voucher = new SSVoucher(7);
        voucher.setSeries("b");

        assertThat(SSVoucherDialog.formatVoucherReference(voucher)).isEqualTo("B7");
    }
}
