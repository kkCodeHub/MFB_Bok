package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SSDB} voucher CRUD operations.
 *
 * <p>Vouchers are scoped to the current accounting year.  Each test uses a
 * unique voucher number (high, unlikely to clash with seed data) and removes
 * the voucher in a {@code finally} block.</p>
 *
 * <p>We use {@code addVoucher(voucher, true)} (iHasNumber = true) so we can
 * control the voucher number and target it for cleanup.</p>
 */
@Tag("integration")
class SSVoucherIntegrationTest {

    /** First test-specific voucher number — intentionally high to avoid clashing with seed data. */
    private static final int BASE_NUMBER = 90_000;

    @BeforeAll
    static void openDatabase() throws Exception {
        SSDBTestFixture.setupOnce();
    }

    @BeforeEach
    void clearCaches() {
        SSDBTestFixture.resetCaches();
    }

    @AfterEach
    void assertNoBackgroundErrors() {
        SSDBTestFixture.drainUncaughtExceptions();
    }

    // ---- addVoucher / getVouchers ----

    @Test
    void addedVoucherAppearsInGetVouchers() {
        SSVoucher v = voucher(BASE_NUMBER + 1);
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            List<SSVoucher> all = SSDB.getInstance().getVouchers();

            assertThat(all).extracting(SSVoucher::getNumber).contains(BASE_NUMBER + 1);
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherDescriptionRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 2);
        v.setDescription("Integration test voucher");
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 2);

            assertThat(fetched).isNotNull();
            assertThat(fetched.getDescription()).isEqualTo("Integration test voucher");
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherDateRoundTrips() {
        Date date = new Date(1_700_000_000_000L);
        SSVoucher v = voucher(BASE_NUMBER + 3);
        v.setLocalDate(se.swedsoft.bookkeeping.util.SSDateUtil.toLocalDate(date));
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 3);

            // Date fields are now LocalDate internally, so the time portion
            // is truncated to midnight on round-trip.
            LocalDate expectedLocalDate = se.swedsoft.bookkeeping.util.SSDateUtil.toLocalDate(date);
            assertThat(fetched).isNotNull();
            assertThat(fetched.getLocalDate()).isEqualTo(expectedLocalDate);
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherWithRowsPreservesRowCount() {
        SSVoucher v = voucher(BASE_NUMBER + 4);
        v.getRows().add(voucherRow(1000, new BigDecimal("500.00"), null));
        v.getRows().add(voucherRow(2000, null, new BigDecimal("500.00")));
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 4);

            assertThat(fetched).isNotNull();
            assertThat(fetched.getRows()).hasSize(2);
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherRowDebitRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 5);
        v.getRows().add(voucherRow(3000, new BigDecimal("1234.50"), null));
        v.getRows().add(voucherRow(4000, null, new BigDecimal("1234.50")));
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 5);
            assertThat(fetched).isNotNull();

            SSVoucherRow row = fetched.getRows().stream()
                    .filter(r -> r.getDebet() != null)
                    .findFirst()
                    .orElse(null);
            assertThat(row).isNotNull();
            assertThat(row.getDebet()).isEqualByComparingTo(new BigDecimal("1234.50"));
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    // ---- deleteVoucher ----

    @Test
    void deletedVoucherDisappearsFromGetVouchers() {
        SSVoucher v = voucher(BASE_NUMBER + 10);
        SSDB.getInstance().addVoucher(v, true);
        SSDB.getInstance().deleteVoucher(v);

        SSDBTestFixture.resetCaches();
        List<SSVoucher> all = SSDB.getInstance().getVouchers();

        assertThat(all).extracting(SSVoucher::getNumber)
                .doesNotContain(BASE_NUMBER + 10);
    }

    // ---- updateVoucher ----

    @Test
    void updatedVoucherDescriptionRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 20);
        v.setDescription("Original description");
        SSDB.getInstance().addVoucher(v, true);

        try {
            SSDBTestFixture.resetCaches();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 20);
            assertThat(fetched).isNotNull();

            fetched.setDescription("Updated description");
            SSDB.getInstance().updateVoucher(fetched);

            SSDBTestFixture.resetCaches();
            SSVoucher updated = findVoucherByNumber(BASE_NUMBER + 20);

            assertThat(updated).isNotNull();
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    // ---- getLastVoucherNumber ----

    @Test
    void getLastVoucherNumberIncludesAddedVoucher() {
        SSVoucher v = voucher(BASE_NUMBER + 30);
        SSDB.getInstance().addVoucher(v, true);

        try {
            int last = SSDB.getInstance().getLastVoucherNumber();

            assertThat(last).isGreaterThanOrEqualTo(BASE_NUMBER + 30);
        } finally {
            SSDB.getInstance().deleteVoucher(v);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SSVoucher voucher(int number) {
        SSVoucher v = new SSVoucher(number);
        v.setLocalDate(se.swedsoft.bookkeeping.util.SSDateUtil.today());
        return v;
    }

    private static SSVoucherRow voucherRow(int accountNumber,
            BigDecimal debet, BigDecimal credit) {
        SSAccount acc = new SSAccount();
        acc.setNumber(accountNumber);
        SSVoucherRow row = new SSVoucherRow();
        row.setAccount(acc);
        row.setDebet(debet);
        row.setCredit(credit);
        return row;
    }

    private static SSVoucher findVoucherByNumber(int number) {
        return SSDB.getInstance().getVouchers().stream()
                .filter(v -> v.getNumber() == number)
                .findFirst()
                .orElse(null);
    }
}
