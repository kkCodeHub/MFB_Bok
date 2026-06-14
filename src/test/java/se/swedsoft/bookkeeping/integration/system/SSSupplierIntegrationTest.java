package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SSDB} supplier CRUD operations.
 *
 * <p>Each test is self-contained: it inserts its own supplier with a unique
 * number and removes it in a {@code finally} block.  Tests can therefore run in
 * any order without interfering with each other.</p>
 */
@Tag("integration")
class SSSupplierIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_ss-supplier-integration-test";

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

    // ---- addSupplier / getSuppliers ----

    @Test
    void addedSupplierAppearsInGetSuppliers() {
        SSSupplier s = supplier("S-IT-001", "Integration LeverantÃ¶r AB");
        SSMasterdataContext.addSupplier(s);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            List<SSSupplier> all = SSMasterdataContext.getSuppliers();

            assertThat(all).extracting(SSSupplier::getNumber).contains("S-IT-001");
        } finally {
            SSMasterdataContext.deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierNameRoundTrips() {
        SSSupplier s = supplier("S-IT-002", "Round-Trip LeverantÃ¶r");
        SSMasterdataContext.addSupplier(s);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getName()).isEqualTo("Round-Trip LeverantÃ¶r");
        } finally {
            SSMasterdataContext.deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierEmailRoundTrips() {
        SSSupplier s = supplier("S-IT-003", "Email LeverantÃ¶r");
        s.setEMail("lev@example.se");
        SSMasterdataContext.addSupplier(s);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getEMail()).isEqualTo("lev@example.se");
        } finally {
            SSMasterdataContext.deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierPhoneRoundTrips() {
        SSSupplier s = supplier("S-IT-004", "Phone LeverantÃ¶r");
        s.setPhone1("08-999888");
        SSMasterdataContext.addSupplier(s);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPhone1()).isEqualTo("08-999888");
        } finally {
            SSMasterdataContext.deleteSupplier(s);
        }
    }

    // ---- deleteSupplier ----

    @Test
    void deletedSupplierDisappearsFromGetSuppliers() {
        SSSupplier s = supplier("S-IT-DEL-001", "Delete Me LeverantÃ¶r");
        SSMasterdataContext.addSupplier(s);
        SSMasterdataContext.deleteSupplier(s);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSSupplier> all = SSMasterdataContext.getSuppliers();

        assertThat(all).extracting(SSSupplier::getNumber)
                .doesNotContain("S-IT-DEL-001");
    }

    @Test
    void deletedSupplierNotFoundByObject() {
        SSSupplier s = supplier("S-IT-DEL-002", "Also Delete Me Lev");
        SSMasterdataContext.addSupplier(s);
        SSMasterdataContext.deleteSupplier(s);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);

        assertThat(fetched).isEmpty();
    }

    // ---- updateSupplier ----

    @Test
    void updatedSupplierNameRoundTrips() {
        SSSupplier s = supplier("S-IT-UPD-001", "Before Update Lev");
        SSMasterdataContext.addSupplier(s);

        try {
            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);
            assertThat(fetched).isPresent();

            fetched.get().setName("After Update Lev");
            SSMasterdataContext.updateSupplier(fetched.get());

            se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
            Optional<SSSupplier> updated = SSMasterdataContext.getSupplier(s);

            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("After Update Lev");
        } finally {
            SSMasterdataContext.deleteSupplier(s);
        }
    }

    // ---- getSupplier returns empty for unknown ----

    @Test
    void getSupplierReturnsEmptyForUnknownNumber() {
        SSSupplier unknown = supplier("S-IT-UNKNOWN-999", "Ghost");
        Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(unknown);

        assertThat(fetched).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SSSupplier supplier(String number, String name) {
        SSSupplier s = new SSSupplier();
        s.setNumber(number);
        s.setName(name);
        return s;
    }
}

