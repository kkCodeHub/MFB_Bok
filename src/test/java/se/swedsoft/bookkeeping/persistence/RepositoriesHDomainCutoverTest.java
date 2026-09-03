package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2AutoDistRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2CustomerRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2OwnReportRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherTemplateRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Slice P cutover wiring for migrated H domains.
 */
class RepositoriesHDomainCutoverTest extends AbstractCutoverTest {

    @Test
    void initUsesV2RepositoriesForMigratedHDomains() {
        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.autoDists()).isInstanceOf(V2AutoDistRepository.class);
        assertThat(Repositories.voucherTemplates()).isInstanceOf(V2VoucherTemplateRepository.class);
        assertThat(Repositories.ownReports()).isInstanceOf(V2OwnReportRepository.class);
        assertThat(Repositories.customers()).isInstanceOf(V2CustomerRepository.class);
    }

    @Test
    void initStillUsesV2RepositoriesForMigratedHDomains() {

        Repositories.init(SSDB.getInstance());

        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.autoDists()).isInstanceOf(V2AutoDistRepository.class);
        assertThat(Repositories.voucherTemplates()).isInstanceOf(V2VoucherTemplateRepository.class);
        assertThat(Repositories.ownReports()).isInstanceOf(V2OwnReportRepository.class);
    }
}

