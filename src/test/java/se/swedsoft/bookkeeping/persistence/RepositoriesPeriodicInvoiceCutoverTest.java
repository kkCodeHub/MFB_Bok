package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.legacy.SSDBCustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the periodic-invoice domain.
 */
class RepositoriesPeriodicInvoiceCutoverTest {

    @AfterEach
    void clearSchemaFlag() {
        System.clearProperty("fribok.schema.version");
    }

    @Test
    void initUsesV2PeriodicInvoiceRepositoryInV1Mode() {
        System.clearProperty("fribok.schema.version");

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isFalse();
        assertThat(Repositories.periodicInvoices()).isInstanceOf(V2PeriodicInvoiceRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(SSDBCustomerRepository.class);
    }

    @Test
    void initUsesV2PeriodicInvoiceRepositoryInV2Mode() {
        System.setProperty("fribok.schema.version", "v2");

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.periodicInvoices()).isInstanceOf(V2PeriodicInvoiceRepository.class);
    }
}

