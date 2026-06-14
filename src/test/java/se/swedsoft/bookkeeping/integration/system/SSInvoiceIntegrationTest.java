package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SSDB} invoice CRUD operations.
 *
 * <p>{@code addInvoice} automatically assigns the invoice number, so after
 * calling it the number is available via {@code invoice.getNumber()}.  Each
 * test records that number and deletes the invoice in a {@code finally} block
 * to keep the DB clean.</p>
 */
@Tag("integration")
class SSInvoiceIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_ss-invoice-integration-test";

    private static Connection connection;

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

    // ---- addInvoice / getInvoices ----

    @Test
    void addedInvoiceAppearsInGetInvoices() {
        SSInvoice inv = invoice("INV-IT-CUST-001", "Invoice IT Customer");
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            List<SSInvoice> all = SSSalesContext.getInvoices();

            assertThat(all).extracting(SSInvoice::getNumber).contains(inv.getNumber());
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    @Test
    void addedInvoiceNumberIsPositive() {
        SSInvoice inv = invoice("INV-IT-CUST-002", "Invoice IT Customer 2");
        SSSalesContext.addInvoice(inv);

        try {
            assertThat(inv.getNumber()).isGreaterThan(0);
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    @Test
    void addedInvoiceCustomerNrRoundTrips() {
        SSInvoice inv = invoice("INV-IT-CUST-003", "Round-Trip Customer");
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getCustomerNr()).isEqualTo("INV-IT-CUST-003");
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    @Test
    void addedInvoiceCustomerNameRoundTrips() {
        SSInvoice inv = invoice("INV-IT-CUST-004", "Name Round-Trip AB");
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getCustomerName()).isEqualTo("Name Round-Trip AB");
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    @Test
    void addedInvoiceDateRoundTrips() {
        Date date = new Date(1_700_000_000_000L);
        SSInvoice inv = invoice("INV-IT-CUST-005", "Date Customer");
        inv.setLocalDate(se.swedsoft.bookkeeping.util.SSDateUtil.toLocalDate(date));
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);

            // Date fields are now LocalDate internally, so the time portion
            // is truncated to midnight on round-trip.
            LocalDate expectedLocalDate = se.swedsoft.bookkeeping.util.SSDateUtil.toLocalDate(date);
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getLocalDate()).isEqualTo(expectedLocalDate);
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    @Test
    void addedInvoiceCurrencyRateRoundTrips() {
        SSInvoice inv = invoice("INV-IT-CUST-006", "Currency Customer");
        inv.setCurrencyRate(new BigDecimal("10.50"));
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getCurrencyRate())
                    .isEqualByComparingTo(new BigDecimal("10.50"));
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    // ---- deleteInvoice ----

    @Test
    void deletedInvoiceDisappearsFromGetInvoices() {
        SSInvoice inv = invoice("INV-IT-DEL-001", "Delete Me Customer");
        SSSalesContext.addInvoice(inv);
        Integer number = inv.getNumber();
        SSSalesContext.deleteInvoice(inv);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSInvoice> all = SSSalesContext.getInvoices();

        assertThat(all).extracting(SSInvoice::getNumber).doesNotContain(number);
    }

    @Test
    void deletedInvoiceNotFoundByObject() {
        SSInvoice inv = invoice("INV-IT-DEL-002", "Also Delete Me");
        SSSalesContext.addInvoice(inv);
        SSSalesContext.deleteInvoice(inv);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);

        assertThat(fetched).isEmpty();
    }

    // ---- updateInvoice ----

    @Test
    void updatedInvoiceCustomerNameRoundTrips() {
        SSInvoice inv = invoice("INV-IT-UPD-001", "Before Update");
        SSSalesContext.addInvoice(inv);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> fetched = SSSalesContext.getInvoice(inv);
            assertThat(fetched).isPresent();

            fetched.get().setCustomerName("After Update");
            SSSalesContext.updateInvoice(fetched.get());

            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSInvoice> updated = SSSalesContext.getInvoice(inv);

            assertThat(updated).isPresent();
            assertThat(updated.get().getCustomerName()).isEqualTo("After Update");
        } finally {
            SSSalesContext.deleteInvoice(inv);
        }
    }

    // ---- getInvoice returns empty for unknown ----

    @Test
    void getInvoiceReturnsEmptyForNonExistentNumber() {
        SSInvoice ghost = new SSInvoice();
        ghost.setNumber(Integer.MAX_VALUE);

        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(ghost);

        assertThat(fetched).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a minimal {@link SSInvoice} using the default constructor (which
     * does not call SSDB) and wires the customer number and name directly.
     */
    private static SSInvoice invoice(String customerNr, String customerName) {
        SSInvoice inv = new SSInvoice();
        inv.setCustomerNr(customerNr);
        inv.setCustomerName(customerName);
        inv.setLocalDate(se.swedsoft.bookkeeping.util.SSDateUtil.today());
        inv.setCurrencyRate(new BigDecimal("1"));
        return inv;
    }
}

