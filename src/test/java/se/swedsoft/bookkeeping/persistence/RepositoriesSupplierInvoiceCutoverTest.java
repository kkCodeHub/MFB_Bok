package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2SupplierInvoiceRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the supplier-invoice domain.
 */
class RepositoriesSupplierInvoiceCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2SupplierInvoiceRepository() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.supplierInvoices()).isInstanceOf(V2SupplierInvoiceRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2SupplierInvoiceRepository() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.supplierInvoices()).isInstanceOf(V2SupplierInvoiceRepository.class);
    }
}

