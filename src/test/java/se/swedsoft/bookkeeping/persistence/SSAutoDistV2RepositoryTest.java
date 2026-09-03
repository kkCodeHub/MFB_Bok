package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSAutoDistRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2AutoDistRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for auto-distribution repository wiring in schema V2.
 */
@Tag("integration")
class SSAutoDistV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_autodist_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 AutoDist Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 AutoDist Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void clearCaches() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void repositoriesInitUsesV2AutoDistRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.autoDists()).isInstanceOf(V2AutoDistRepository.class);
    }

    @Test
    void addAndFetchAutoDistViaRepository() {
        SSAutoDist autoDist = autoDist(5110, "Repo autodist");
        autoDist.getRows().add(autoDistRow(2610, "Debet row", new BigDecimal("60.00"), null));
        autoDist.getRows().add(autoDistRow(3010, "Credit row", null, new BigDecimal("60.00")));

        Repositories.autoDists().add(autoDist);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSAutoDist> fetched = Repositories.autoDists().findByAutoDist(new SSAutoDist(autoDist, 5110));
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getNumber()).isEqualTo(5110);
        assertThat(fetched.get().getDescription()).isEqualTo("Repo autodist");
        assertThat(fetched.get().getRows()).hasSize(2);

        Repositories.autoDists().delete(fetched.get());
    }

    @Test
    void updateAndDeleteAutoDistViaRepository() {
        SSAutoDist autoDist = autoDist(5120, "Before autodist update");
        autoDist.getRows().add(autoDistRow(2610, "Before row", new BigDecimal("75.00"), null));
        Repositories.autoDists().add(autoDist);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSAutoDist> fetched = Repositories.autoDists().findByAutoDist(new SSAutoDist(autoDist, 5120));
        assertThat(fetched).isPresent();

        SSAutoDist original = fetched.get();
        SSAutoDist updated = new SSAutoDist(original);
        updated.setDescrition("After autodist update");
        updated.setAmount(new BigDecimal("90.00"));
        updated.getRows().clear();
        updated.getRows().add(autoDistRow(1930, "After row", new BigDecimal("90.00"), null));

        Repositories.autoDists().update(updated, original);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSAutoDist> reloaded = Repositories.autoDists().findByAutoDist(new SSAutoDist(updated, 5120));
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("After autodist update");
        assertThat(reloaded.get().getAmount()).isEqualByComparingTo("90.00");
        assertThat(reloaded.get().getRows()).hasSize(1);
        assertThat(reloaded.get().getRows().get(0).getAccountNr()).isEqualTo(1930);

        Repositories.autoDists().delete(reloaded.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSAutoDist> all = Repositories.autoDists().findAll();
        assertThat(all).extracting(SSAutoDist::getNumber).doesNotContain(5120);
    }

    private static SSAutoDist autoDist(int number, String description) {
        SSAutoDist autoDist = new SSAutoDist();
        autoDist.setAccountNumber(number);
        autoDist.setDescrition(description);
        autoDist.setAmount(new BigDecimal("120.00"));
        return autoDist;
    }

    private static SSAutoDistRow autoDistRow(
            int accountNumber,
            String description,
            BigDecimal debet,
            BigDecimal credit) {
        SSAutoDistRow row = new SSAutoDistRow();
        row.setAccountNr(accountNumber);
        row.setDescription(description);
        row.setPercentage(new BigDecimal("50.0000"));
        row.setDebet(debet);
        row.setCredit(credit);
        row.setProjectNr("PRJ-1");
        row.setResultUnitNr("RES-1");
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}

