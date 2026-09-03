package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the periodic-invoice domain.
 */
class RepositoriesPeriodicInvoiceCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2PeriodicInvoiceRepository() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.periodicInvoices()).isInstanceOf(V2PeriodicInvoiceRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2PeriodicInvoiceRepository() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.periodicInvoices()).isInstanceOf(V2PeriodicInvoiceRepository.class);
    }
}

