package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SSDB} customer CRUD operations.
 *
 * <p>Each test operates against an in-memory HSQLDB database opened via
 * {@link SSDBTestFixture}.  Tests are independent: each adds its own customer
 * with a unique number and cleans up after itself so that ordering does not
 * matter.</p>
 */
@Tag("integration")
class SSCustomerIntegrationTest {

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

    // ---- addCustomer / getCustomers ----

    @Test
    void addedCustomerAppearsInGetCustomers() {
        SSCustomer c = customer("C-IT-001", "Integration AB");
        SSDB.getInstance().addCustomer(c);

        try {
            SSDBTestFixture.resetCaches();
            List<SSCustomer> all = SSDB.getInstance().getCustomers();

            assertThat(all).extracting(SSCustomer::getNumber).contains("C-IT-001");
        } finally {
            SSDB.getInstance().deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerNameRoundTrips() {
        SSCustomer c = customer("C-IT-002", "Round-Trip AB");
        SSDB.getInstance().addCustomer(c);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-002");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getName()).isEqualTo("Round-Trip AB");
        } finally {
            SSDB.getInstance().deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerEmailRoundTrips() {
        SSCustomer c = customer("C-IT-003", "Email Test AB");
        c.setEMail("test@roundtrip.se");
        SSDB.getInstance().addCustomer(c);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-003");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getEMail()).isEqualTo("test@roundtrip.se");
        } finally {
            SSDB.getInstance().deleteCustomer(c);
        }
    }

    @Test
    void addedCustomerPhoneRoundTrips() {
        SSCustomer c = customer("C-IT-004", "Phone Test AB");
        c.setPhone1("08-123456");
        SSDB.getInstance().addCustomer(c);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-004");

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPhone1()).isEqualTo("08-123456");
        } finally {
            SSDB.getInstance().deleteCustomer(c);
        }
    }

    // ---- deleteCustomer ----

    @Test
    void deletedCustomerDisappearsFromGetCustomers() {
        SSCustomer c = customer("C-IT-DEL-001", "Delete Me AB");
        SSDB.getInstance().addCustomer(c);
        SSDB.getInstance().deleteCustomer(c);

        SSDBTestFixture.resetCaches();
        List<SSCustomer> all = SSDB.getInstance().getCustomers();

        assertThat(all).extracting(SSCustomer::getNumber)
                .doesNotContain("C-IT-DEL-001");
    }

    @Test
    void deletedCustomerNotFoundByNumber() {
        SSCustomer c = customer("C-IT-DEL-002", "Also Delete Me");
        SSDB.getInstance().addCustomer(c);
        SSDB.getInstance().deleteCustomer(c);

        Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-DEL-002");

        assertThat(fetched).isEmpty();
    }

    // ---- updateCustomer ----

    @Test
    void updatedCustomerNameRoundTrips() {
        SSCustomer c = customer("C-IT-UPD-001", "Before Update");
        SSDB.getInstance().addCustomer(c);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-UPD-001");
            assertThat(fetched).isPresent();

            fetched.get().setName("After Update");
            SSDB.getInstance().updateCustomer(fetched.get());

            SSDBTestFixture.resetCaches();
            Optional<SSCustomer> updated = SSDB.getInstance().getCustomer("C-IT-UPD-001");

            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("After Update");
        } finally {
            SSDB.getInstance().deleteCustomer(c);
        }
    }

    // ---- getCustomer by number ----

    @Test
    void getCustomerByNumberReturnsEmptyForUnknownNumber() {
        Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-IT-DOES-NOT-EXIST");

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
