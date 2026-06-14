package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSInventoryDeliveriesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.SSOutdeliveryRow;

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
 * Integration slice for outdelivery CRUD against schema V2.
 */
@Tag("integration")
class SSOutdeliveryV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-o-u-t-d-e-l-i-v-e-r-y-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Outdelivery Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Outdelivery Test Company AB");
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
    void addAndFetchOutdeliveryWithRowsInSchemaV2() {
        SSOutdelivery outdelivery = outdelivery("Initial outdelivery text");
        outdelivery.getRows().add(row("P-OUT-001", 2));
        outdelivery.getRows().add(row("P-OUT-002", 4));

        SSInventoryDeliveriesContext.addOutdelivery(outdelivery);

        assertThat(outdelivery.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutdelivery> fetched = SSInventoryDeliveriesContext.getOutdelivery(outdelivery);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 24));
        assertThat(fetched.get().getText()).isEqualTo("Initial outdelivery text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-OUT-001");
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(2);

        SSInventoryDeliveriesContext.deleteOutdelivery(outdelivery);
    }

    @Test
    void updateAndDeleteOutdeliveryInSchemaV2() {
        SSOutdelivery outdelivery = outdelivery("Before outdelivery update");
        outdelivery.getRows().add(row("P-OUT-003", 1));
        SSInventoryDeliveriesContext.addOutdelivery(outdelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutdelivery> fetched = SSInventoryDeliveriesContext.getOutdelivery(outdelivery);
        assertThat(fetched).isPresent();

        SSOutdelivery updatedOutdelivery = fetched.get();
        updatedOutdelivery.setText("After outdelivery update");
        updatedOutdelivery.setLocalDate(LocalDate.of(2025, 9, 14));
        updatedOutdelivery.getRows().clear();
        updatedOutdelivery.getRows().add(row("P-OUT-004", 6));
        SSInventoryDeliveriesContext.updateOutdelivery(updatedOutdelivery);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutdelivery> updated = SSInventoryDeliveriesContext.getOutdelivery(outdelivery);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After outdelivery update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 14));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-OUT-004");
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(6);

        Integer outdeliveryNumber = updated.get().getNumber();
        SSInventoryDeliveriesContext.deleteOutdelivery(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSOutdelivery> all = SSInventoryDeliveriesContext.getOutdeliveries();
        assertThat(all).extracting(SSOutdelivery::getNumber).doesNotContain(outdeliveryNumber);
    }

    private static SSOutdelivery outdelivery(String text) {
        SSOutdelivery outdelivery = new SSOutdelivery();
        outdelivery.setLocalDate(LocalDate.of(2025, 7, 24));
        outdelivery.setText(text);
        return outdelivery;
    }

    private static SSOutdeliveryRow row(String productNr, Integer change) {
        SSOutdeliveryRow row = new SSOutdeliveryRow();
        row.setProductNr(productNr);
        row.setChange(change);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create test company for schema V2 outdelivery test");
    }
}


