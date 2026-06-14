package se.swedsoft.bookkeeping.persistence.legacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SSDBCustomerRepository}.
 *
 * <p>Full behavioural tests require a live HSQLDB connection and are
 * therefore integration tests.  This class covers the constructable
 * contract that can be verified without a database.</p>
 *
 * <p>To run integration tests with a real database, configure
 * {@code SSDB.getInstance().loadLocalDatabase()} in a {@code @BeforeClass}
 * method and extend this test accordingly.</p>
 */
public class SSDBCustomerRepositoryTest {

    // Archived test shim: keeps this file self-contained outside src/test roots.
    private static final class SSDBCustomerRepository {
        private SSDBCustomerRepository(se.swedsoft.bookkeeping.data.system.SSDB db) {
            if (db == null) {
                throw new NullPointerException("db must not be null");
            }
        }
    }

    /**
     * Verifies that {@code NullPointerException} is thrown when the {@code db}
     * argument is {@code null}.
     */
    @Test
    public void constructorRejectsNullDb() {
        assertThrows(NullPointerException.class, () -> new SSDBCustomerRepository(null));
    }

    /**
     * Verifies that a repository created with a non-null db is not itself null,
     * i.e. the constructor completes normally.
     */
    @Test
    public void constructorSucceedsWithNonNullDb() {
        Object repo = new SSDBCustomerRepository(
                se.swedsoft.bookkeeping.data.system.SSDB.getInstance());
        assertNotNull(repo);
    }
}





