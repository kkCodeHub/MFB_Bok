package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSInventoryDeliveriesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSIndeliveryRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;

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
 * Integration slice for indelivery CRUD against schema V2.
 */
@Tag("integration")
class SSIndeliveryV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-i-n-d-e-l-i-v-e-r-y-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Indelivery Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Indelivery Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
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
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchIndeliveryWithRowsInSchemaV2() {
        SSIndelivery indelivery = indelivery("Initial indelivery text");
        indelivery.getRows().add(row("P-IND-001", 5));
        indelivery.getRows().add(row("P-IND-002", 7));

        SSInventoryDeliveriesContext.addIndelivery(indelivery);

        assertThat(indelivery.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> fetched = SSInventoryDeliveriesContext.getIndelivery(indelivery);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 22));
        assertThat(fetched.get().getText()).isEqualTo("Initial indelivery text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-001");
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(5);

        SSInventoryDeliveriesContext.deleteIndelivery(indelivery);
    }

    @Test
    void updateAndDeleteIndeliveryInSchemaV2() {
        SSIndelivery indelivery = indelivery("Before indelivery update");
        indelivery.getRows().add(row("P-IND-003", 1));
        SSInventoryDeliveriesContext.addIndelivery(indelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> fetched = SSInventoryDeliveriesContext.getIndelivery(indelivery);
        assertThat(fetched).isPresent();

        SSIndelivery updatedIndelivery = fetched.get();
        updatedIndelivery.setText("After indelivery update");
        updatedIndelivery.setLocalDate(LocalDate.of(2025, 9, 12));
        updatedIndelivery.getRows().clear();
        updatedIndelivery.getRows().add(row("P-IND-004", 9));
        SSInventoryDeliveriesContext.updateIndelivery(updatedIndelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSIndelivery> updated = SSInventoryDeliveriesContext.getIndelivery(indelivery);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After indelivery update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 12));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-004");
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(9);

        Integer indeliveryNumber = updated.get().getNumber();
        SSInventoryDeliveriesContext.deleteIndelivery(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSIndelivery> all = SSInventoryDeliveriesContext.getIndeliveries();
        assertThat(all).extracting(SSIndelivery::getNumber).doesNotContain(indeliveryNumber);
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


