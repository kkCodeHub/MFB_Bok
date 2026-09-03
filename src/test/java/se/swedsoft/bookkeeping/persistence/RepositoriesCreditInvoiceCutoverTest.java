package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2CreditInvoiceRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the credit-invoice domain.
 */
class RepositoriesCreditInvoiceCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2CreditInvoiceRepository() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.creditInvoices()).isInstanceOf(V2CreditInvoiceRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2CreditInvoiceRepository() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.creditInvoices()).isInstanceOf(V2CreditInvoiceRepository.class);
    }
}

