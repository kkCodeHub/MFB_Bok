package se.swedsoft.bookkeeping.print.dialog;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherListDialogTest {

    @Test
    void buildSeriesOptionsShouldIncludeControlOptionsAndUsedSeriesSorted() {
        List<SSVoucher> vouchers = Arrays.asList(voucher("b", 3), voucher("A", 1), voucher("B", 2), voucher("c", 4));

        List<String> options = SSVoucherListDialog.buildSeriesOptions(vouchers, "Alla");

        assertThat(options).containsExactly("Alla", "System", "Egna", "A", "B", "C");
    }

    @Test
    void filterBySeriesSelectionShouldHandleAllSystemCustomAndSpecificSeries() {
        List<SSVoucher> vouchers = Arrays.asList(voucher("A", 1), voucher("B", 2), voucher("C", 3));
        Set<String> systemSeries = Set.of("A", "C");
        Set<String> customSeries = Set.of("B");

        assertThat(SSVoucherListDialog.filterBySeriesSelection(vouchers, "Alla", "Alla", systemSeries, customSeries))
                .hasSize(3);
        assertThat(SSVoucherListDialog.filterBySeriesSelection(vouchers, "System", "Alla", systemSeries, customSeries))
                .extracting(SSVoucher::getSeries).containsExactlyInAnyOrder("A", "C");
        assertThat(SSVoucherListDialog.filterBySeriesSelection(vouchers, "Egna", "Alla", systemSeries, customSeries))
                .extracting(SSVoucher::getSeries).containsExactly("B");
        assertThat(SSVoucherListDialog.filterBySeriesSelection(vouchers, "C", "Alla", systemSeries, customSeries))
                .extracting(SSVoucher::getSeries).containsExactly("C");
    }

    @Test
    void resolveMaxNumberShouldUseAtLeastOneAndHighestVoucherNumber() {
        assertThat(SSVoucherListDialog.resolveMaxNumber(Collections.emptyList())).isEqualTo(1);
        assertThat(SSVoucherListDialog.resolveMaxNumber(Arrays.asList(voucher("A", 2), voucher("A", 9), voucher("B", 5))))
                .isEqualTo(9);
    }

    @Test
    void isValidDateIntervalShouldValidateFromBeforeOrEqualToTo() {
        assertThat(SSVoucherListDialog.isValidDateInterval(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(SSVoucherListDialog.isValidDateInterval(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(SSVoucherListDialog.isValidDateInterval(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1))).isFalse();
    }

    private static SSVoucher voucher(String series, int number) {
        SSVoucher voucher = new SSVoucher(number);
        voucher.setSeries(series);
        voucher.setNumber(number);
        return voucher;
    }
}
