package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSIndeliveryRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2IndeliveryRepository;

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
 * Integration tests for indelivery repository wiring in schema V2.
 */
@Tag("integration")
class SSIndeliveryV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_indelivery_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Indelivery Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Indelivery Repo Test AB");
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
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2IndeliveryRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.indeliveries()).isInstanceOf(V2IndeliveryRepository.class);
    }

    @Test
    void addAndFetchIndeliveryViaRepository() {
        SSIndelivery indelivery = indelivery("Repo indelivery text");
        indelivery.getRows().add(row("P-IND-REPO-001", 5));

        Repositories.indeliveries().add(indelivery);
        assertThat(indelivery.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> fetched = Repositories.indeliveries().findByIndelivery(indelivery);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getText()).isEqualTo("Repo indelivery text");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-REPO-001");
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(5);

        Repositories.indeliveries().delete(fetched.get());
    }

    @Test
    void updateAndDeleteIndeliveryViaRepository() {
        SSIndelivery indelivery = indelivery("Before indelivery repo update");
        indelivery.getRows().add(row("P-IND-REPO-002", 1));
        Repositories.indeliveries().add(indelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> fetched = Repositories.indeliveries().findByIndelivery(indelivery);
        assertThat(fetched).isPresent();

        SSIndelivery updatedIndelivery = fetched.get();
        updatedIndelivery.setText("After indelivery repo update");
        updatedIndelivery.setLocalDate(LocalDate.of(2025, 9, 12));
        updatedIndelivery.getRows().clear();
        updatedIndelivery.getRows().add(row("P-IND-REPO-003", 9));
        Repositories.indeliveries().update(updatedIndelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> updated = Repositories.indeliveries().findByIndelivery(indelivery);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After indelivery repo update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 12));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-REPO-003");
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(9);

        Integer number = updated.get().getNumber();
        Repositories.indeliveries().delete(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSIndelivery> all = Repositories.indeliveries().findAll();
        assertThat(all).extracting(SSIndelivery::getNumber).doesNotContain(number);
    }

    private static SSIndelivery indelivery(String text) {
        SSIndelivery indelivery = new SSIndelivery();
        indelivery.setLocalDate(LocalDate.of(2025, 7, 22));
        indelivery.setText(text);
        return indelivery;
    }

    private static SSIndeliveryRow row(String productNr, Integer change) {
        SSIndeliveryRow row = new SSIndeliveryRow();
        row.setProductNr(productNr);
        row.setChange(change);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}

