package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2InventoryRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for the inventory domain.
 */
class RepositoriesInventoryCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2InventoryRepository() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.inventories()).isInstanceOf(V2InventoryRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2InventoryRepository() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.inventories()).isInstanceOf(V2InventoryRepository.class);
    }
}

