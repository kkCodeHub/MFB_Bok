package se.swedsoft.bookkeeping.persistence.legacy;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link V2CustomerRepository} constructor contract.
 */
public class SSDBCustomerRepositoryTest {

    @Test
    public void constructorRejectsNullConnection() {
        assertThrows(NullPointerException.class,
                () -> new V2CustomerRepository(null, () -> null, () -> {}));
    }

    @Test
    public void constructorRejectsNullCurrentCompany() {
        assertThrows(NullPointerException.class,
                () -> new V2CustomerRepository(null, null, () -> {}));
    }

    @Test
    public void constructorRejectsNullRollbackHandler() {
        assertThrows(NullPointerException.class,
                () -> new V2CustomerRepository(null, () -> null, null));
    }
}
