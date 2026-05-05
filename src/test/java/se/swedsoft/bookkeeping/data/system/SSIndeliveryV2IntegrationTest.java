package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_indelivery";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Indelivery Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Indelivery Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
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
    void clearCaches() {
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void addAndFetchIndeliveryWithRowsInSchemaV2() {
        SSIndelivery indelivery = indelivery("Initial indelivery text");
        indelivery.getRows().add(row("P-IND-001", 5));
        indelivery.getRows().add(row("P-IND-002", 7));

        SSDB.getInstance().addIndelivery(indelivery);

        assertThat(indelivery.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSIndelivery> fetched = SSDB.getInstance().getIndelivery(indelivery);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 22));
        assertThat(fetched.get().getText()).isEqualTo("Initial indelivery text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-001");
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(5);

        SSDB.getInstance().deleteIndelivery(indelivery);
    }

    @Test
    void updateAndDeleteIndeliveryInSchemaV2() {
        SSIndelivery indelivery = indelivery("Before indelivery update");
        indelivery.getRows().add(row("P-IND-003", 1));
        SSDB.getInstance().addIndelivery(indelivery);

        SSDB.getInstance().clearLists();
        Optional<SSIndelivery> fetched = SSDB.getInstance().getIndelivery(indelivery);
        assertThat(fetched).isPresent();

        SSIndelivery updatedIndelivery = fetched.get();
        updatedIndelivery.setText("After indelivery update");
        updatedIndelivery.setLocalDate(LocalDate.of(2025, 9, 12));
        updatedIndelivery.getRows().clear();
        updatedIndelivery.getRows().add(row("P-IND-004", 9));
        SSDB.getInstance().updateIndelivery(updatedIndelivery);

        SSDB.getInstance().clearLists();
        Optional<SSIndelivery> updated = SSDB.getInstance().getIndelivery(indelivery);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After indelivery update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 12));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-IND-004");
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(9);

        Integer indeliveryNumber = updated.get().getNumber();
        SSDB.getInstance().deleteIndelivery(updated.get());
        SSDB.getInstance().clearLists();
        List<SSIndelivery> all = SSDB.getInstance().getIndeliveries();
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
        throw new IllegalStateException("Could not create test company for schema V2 indelivery test");
    }
}

