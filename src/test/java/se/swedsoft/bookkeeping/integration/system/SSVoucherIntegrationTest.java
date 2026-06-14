package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_ss-voucher-integration-test";

    private static Connection connection;

    /** First test-specific voucher number â€” intentionally high to avoid clashing with seed data. */
    private static final int BASE_NUMBER = 90_000;

    @BeforeAll
    static void openDatabase() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);
    }

    @AfterAll
    static void closeDatabase() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @AfterEach
    void assertNoBackgroundErrors() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    // ---- addVoucher / getVouchers ----

    @Test
    void addedVoucherAppearsInGetVouchers() {
        SSVoucher v = voucher(BASE_NUMBER + 1);
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            List<SSVoucher> all = SSAccountingContext.getVouchers();

            assertThat(all).extracting(SSVoucher::getNumber).contains(BASE_NUMBER + 1);
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherDescriptionRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 2);
        v.setDescription("Integration test voucher");
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 2);

            assertThat(fetched).isNotNull();
            assertThat(fetched.getDescription()).isEqualTo("Integration test voucher");
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherDateRoundTrips() {
        LocalDate date = currentYearDate(20);
        SSVoucher v = voucher(BASE_NUMBER + 3);
        v.setLocalDate(date);
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 3);

            assertThat(fetched).isNotNull();
            assertThat(fetched.getLocalDate()).isEqualTo(date);
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherWithRowsPreservesRowCount() {
        SSVoucher v = voucher(BASE_NUMBER + 4);
        v.getRows().add(voucherRow(1000, new BigDecimal("500.00"), null));
        v.getRows().add(voucherRow(2000, null, new BigDecimal("500.00")));
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 4);

            assertThat(fetched).isNotNull();
            assertThat(fetched.getRows()).hasSize(2);
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    @Test
    void addedVoucherRowDebitRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 5);
        v.getRows().add(voucherRow(3000, new BigDecimal("1234.50"), null));
        v.getRows().add(voucherRow(4000, null, new BigDecimal("1234.50")));
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 5);
            assertThat(fetched).isNotNull();

            SSVoucherRow row = fetched.getRows().stream()
                    .filter(r -> r.getDebet() != null)
                    .findFirst()
                    .orElse(null);
            assertThat(row).isNotNull();
            assertThat(row.getDebet()).isEqualByComparingTo(new BigDecimal("1234.50"));
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    // ---- deleteVoucher ----

    @Test
    void deletedVoucherDisappearsFromGetVouchers() {
        SSVoucher v = voucher(BASE_NUMBER + 10);
        SSAccountingContext.addVoucher(v, true);
        SSAccountingContext.deleteVoucher(v);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSVoucher> all = SSAccountingContext.getVouchers();

        assertThat(all).extracting(SSVoucher::getNumber)
                .doesNotContain(BASE_NUMBER + 10);
    }

    // ---- updateVoucher ----

    @Test
    void updatedVoucherDescriptionRoundTrips() {
        SSVoucher v = voucher(BASE_NUMBER + 20);
        v.setDescription("Original description");
        SSAccountingContext.addVoucher(v, true);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher fetched = findVoucherByNumber(BASE_NUMBER + 20);
            assertThat(fetched).isNotNull();

            fetched.setDescription("Updated description");
            SSAccountingContext.updateVoucher(fetched);

            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            SSVoucher updated = findVoucherByNumber(BASE_NUMBER + 20);

            assertThat(updated).isNotNull();
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    // ---- getLastVoucherNumber ----

    @Test
    void getLastVoucherNumberIncludesAddedVoucher() {
        SSVoucher v = voucher(BASE_NUMBER + 30);
        SSAccountingContext.addVoucher(v, true);

        try {
            int last = SSAccountingContext.getLastVoucherNumber();

            assertThat(last).isGreaterThanOrEqualTo(BASE_NUMBER + 30);
        } finally {
            SSAccountingContext.deleteVoucher(v);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SSVoucher voucher(int number) {
        SSVoucher v = new SSVoucher(number);
        v.setLocalDate(currentYearDate(1));
        return v;
    }

    private static LocalDate currentYearDate(int dayOffset) {
        LocalDate from = SSDB.getInstance().getCurrentYear() == null
                ? null
                : SSDB.getInstance().getCurrentYear().getLocalFrom();
        if (from != null) {
            return from.plusDays(dayOffset);
        }
        return LocalDate.now();
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
        return SSAccountingContext.getVouchers().stream()
                .filter(v -> v.getNumber() == number)
                .findFirst()
                .orElse(null);
    }
}

