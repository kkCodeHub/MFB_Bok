package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplier;

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

    // ---- addSupplier / getSuppliers ----

    @Test
    void addedSupplierAppearsInGetSuppliers() {
        SSSupplier s = supplier("S-IT-001", "Integration Leverantör AB");
        SSDB.getInstance().addSupplier(s);

        try {
            SSDBTestFixture.resetCaches();
            List<SSSupplier> all = SSDB.getInstance().getSuppliers();

            assertThat(all).extracting(SSSupplier::getNumber).contains("S-IT-001");
        } finally {
            SSDB.getInstance().deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierNameRoundTrips() {
        SSSupplier s = supplier("S-IT-002", "Round-Trip Leverantör");
        SSDB.getInstance().addSupplier(s);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getName()).isEqualTo("Round-Trip Leverantör");
        } finally {
            SSDB.getInstance().deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierEmailRoundTrips() {
        SSSupplier s = supplier("S-IT-003", "Email Leverantör");
        s.setEMail("lev@example.se");
        SSDB.getInstance().addSupplier(s);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getEMail()).isEqualTo("lev@example.se");
        } finally {
            SSDB.getInstance().deleteSupplier(s);
        }
    }

    @Test
    void addedSupplierPhoneRoundTrips() {
        SSSupplier s = supplier("S-IT-004", "Phone Leverantör");
        s.setPhone1("08-999888");
        SSDB.getInstance().addSupplier(s);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(s);

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getPhone1()).isEqualTo("08-999888");
        } finally {
            SSDB.getInstance().deleteSupplier(s);
        }
    }

    // ---- deleteSupplier ----

    @Test
    void deletedSupplierDisappearsFromGetSuppliers() {
        SSSupplier s = supplier("S-IT-DEL-001", "Delete Me Leverantör");
        SSDB.getInstance().addSupplier(s);
        SSDB.getInstance().deleteSupplier(s);

        SSDBTestFixture.resetCaches();
        List<SSSupplier> all = SSDB.getInstance().getSuppliers();

        assertThat(all).extracting(SSSupplier::getNumber)
                .doesNotContain("S-IT-DEL-001");
    }

    @Test
    void deletedSupplierNotFoundByObject() {
        SSSupplier s = supplier("S-IT-DEL-002", "Also Delete Me Lev");
        SSDB.getInstance().addSupplier(s);
        SSDB.getInstance().deleteSupplier(s);

        SSDBTestFixture.resetCaches();
        Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(s);

        assertThat(fetched).isEmpty();
    }

    // ---- updateSupplier ----

    @Test
    void updatedSupplierNameRoundTrips() {
        SSSupplier s = supplier("S-IT-UPD-001", "Before Update Lev");
        SSDB.getInstance().addSupplier(s);

        try {
            SSDBTestFixture.resetCaches();
            Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(s);
            assertThat(fetched).isPresent();

            fetched.get().setName("After Update Lev");
            SSDB.getInstance().updateSupplier(fetched.get());

            SSDBTestFixture.resetCaches();
            Optional<SSSupplier> updated = SSDB.getInstance().getSupplier(s);

            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("After Update Lev");
        } finally {
            SSDB.getInstance().deleteSupplier(s);
        }
    }

    // ---- getSupplier returns empty for unknown ----

    @Test
    void getSupplierReturnsEmptyForUnknownNumber() {
        SSSupplier unknown = supplier("S-IT-UNKNOWN-999", "Ghost");
        Optional<SSSupplier> fetched = SSDB.getInstance().getSupplier(unknown);

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
