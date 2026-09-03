package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OutpaymentRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the outpayment domain.
 */
class RepositoriesOutpaymentCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2OutpaymentRepository() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.outpayments()).isInstanceOf(V2OutpaymentRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2OutpaymentRepository() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.outpayments()).isInstanceOf(V2OutpaymentRepository.class);
    }
}

