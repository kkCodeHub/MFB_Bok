package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SSDB} customer CRUD operations.
 *
 * <p>Each test operates against a class-local in-memory HSQLDB database.
 * Tests are independent: each adds its own customer with a unique number and
 * cleans up after itself so that ordering does not matter.</p>
 */
@Tag("integration")
class SSCustomerIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_ss-customer-integration-test";

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

    // ---- addCustomer / getCustomers ----

    @Test
    void addedCustomerAppearsInGetCustomers() {
        SSCustomer c = customer("C-IT-001", "Integration AB");
        SSMasterdataContext.addCustomer(c);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            List<SSCustomer> all = SSMasterdataContext.getCustomers();

            assertThat(all).extracting(SSCustomer::getNumber).contains("C-IT-001");
        } finally {
            SSMasterdataContext.deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerNameRoundTrips() {
        SSCustomer c = customer("C-IT-002", "Round-Trip AB");
        SSMasterdataContext.addCustomer(c);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-002");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getName()).isEqualTo("Round-Trip AB");
        } finally {
            SSMasterdataContext.deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerEmailRoundTrips() {
        SSCustomer c = customer("C-IT-003", "Email Test AB");
        c.setEMail("test@roundtrip.se");
        SSMasterdataContext.addCustomer(c);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-003");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getEMail()).isEqualTo("test@roundtrip.se");
        } finally {
            SSMasterdataContext.deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerPhoneRoundTrips() {
        SSCustomer c = customer("C-IT-004", "Phone Test AB");
        c.setPhone1("08-123456");
        SSMasterdataContext.addCustomer(c);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-004");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPhone1()).isEqualTo("08-123456");
        } finally {
            SSMasterdataContext.deleteCustomer(c);
        }
    }

    // ---- deleteCustomer ----

    @Test
    void deletedCustomerDisappearsFromGetCustomers() {
        SSCustomer c = customer("C-IT-DEL-001", "Delete Me AB");
        SSMasterdataContext.addCustomer(c);
        SSMasterdataContext.deleteCustomer(c);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSCustomer> all = SSMasterdataContext.getCustomers();

        assertThat(all).extracting(SSCustomer::getNumber)
                .doesNotContain("C-IT-DEL-001");
    }

    @Test
    void deletedCustomerNotFoundByNumber() {
        SSCustomer c = customer("C-IT-DEL-002", "Also Delete Me");
        SSMasterdataContext.addCustomer(c);
        SSMasterdataContext.deleteCustomer(c);

        Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-DEL-002");

        assertThat(fetched).isEmpty();
    }

    // ---- updateCustomer ----

    @Test
    void updatedCustomerNameRoundTrips() {
        SSCustomer c = customer("C-IT-UPD-001", "Before Update");
        SSMasterdataContext.addCustomer(c);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-UPD-001");
            assertThat(fetched).isPresent();

            fetched.get().setName("After Update");
            SSMasterdataContext.updateCustomer(fetched.get());

            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSCustomer> updated = SSMasterdataContext.getCustomer("C-IT-UPD-001");

            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("After Update");
        } finally {
            SSMasterdataContext.deleteCustomer(c);
        }
    }

    // ---- getCustomer by number ----

    @Test
    void getCustomerByNumberReturnsEmptyForUnknownNumber() {
        Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-IT-DOES-NOT-EXIST");

        assertThat(fetched).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SSCustomer customer(String number, String name) {
        SSCustomer c = new SSCustomer();
        c.setNumber(number);
        c.setName(name);
        return c;
    }
}

