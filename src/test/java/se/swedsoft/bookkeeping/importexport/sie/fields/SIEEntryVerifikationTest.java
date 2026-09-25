package se.swedsoft.bookkeeping.importexport.sie.fields;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SIEEntryVerifikationTest {

    @Test
    void getExportVoucherSeriesUsesVoucherSeries() {
        SSVoucher voucher = new SSVoucher(3);
        voucher.setSeries("b");

        assertThat(SIEEntryVerifikation.getExportVoucherSeries(voucher)).isEqualTo("B");
    }

    @Test
    void getExportVoucherSeriesDefaultsToAWhenMissing() {
        assertThat(SIEEntryVerifikation.getExportVoucherSeries(null)).isEqualTo("A");
    }

    @Test
    void normalizeVoucherSeriesUsesDefaultAForBlank() {
        assertThat(SIEEntryVerifikation.normalizeVoucherSeries(" ")).isEqualTo("A");
    }

    @Test
    void normalizeVoucherSeriesUppercasesSingleLetter() {
        assertThat(SIEEntryVerifikation.normalizeVoucherSeries("c")).isEqualTo("C");
    }

    @Test
    void normalizeVoucherSeriesRejectsInvalidSeries() {
        assertThatThrownBy(() -> SIEEntryVerifikation.normalizeVoucherSeries("12"))
                .isInstanceOf(RuntimeException.class);
    }
}
