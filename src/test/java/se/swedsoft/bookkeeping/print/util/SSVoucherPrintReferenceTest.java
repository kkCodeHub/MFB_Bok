package se.swedsoft.bookkeeping.print.util;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherPrintReferenceTest {

    @Test
    void toDisplayStringFormatsSeriesAndNumber() {
        assertThat(SSVoucherPrintReference.toDisplayString("A", 123)).isEqualTo("A 123");
        assertThat(SSVoucherPrintReference.toDisplayString("b", 7)).isEqualTo("B 7");
    }

    @Test
    void toDisplayStringHandlesMissingValues() {
        assertThat(SSVoucherPrintReference.toDisplayString(null, null)).isEqualTo("");
        assertThat(SSVoucherPrintReference.toDisplayString("A", null)).isEqualTo("A");
        assertThat(SSVoucherPrintReference.toDisplayString(null, 42)).isEqualTo("42");
    }

    @Test
    void fromVoucherFormatsVoucherReference() {
        SSVoucher voucher = new SSVoucher(123);
        voucher.setSeries("A");

        assertThat(SSVoucherPrintReference.toDisplayString(voucher)).isEqualTo("A 123");
    }
}
