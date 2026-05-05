package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.SSOutdeliveryRow;
import se.swedsoft.bookkeeping.data.system.SSDB;

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
 * Integration tests for outdelivery repository wiring in schema V2.
 */
@Tag("integration")
class SSOutdeliveryV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_outdelivery_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Outdelivery Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Outdelivery Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
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
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2OutdeliveryRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.outdeliveries()).isNotNull();
    }

    @Test
    void addAndFetchOutdeliveryViaRepository() {
        SSOutdelivery outdelivery = outdelivery("Repo outdelivery text");
        outdelivery.getRows().add(row("P-OUT-REPO-001", 2));

        Repositories.outdeliveries().add(outdelivery);
        assertThat(outdelivery.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSOutdelivery> fetched = Repositories.outdeliveries().findByOutdelivery(outdelivery);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getText()).isEqualTo("Repo outdelivery text");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-OUT-REPO-001");
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(2);

        Repositories.outdeliveries().delete(fetched.get());
    }

    @Test
    void updateAndDeleteOutdeliveryViaRepository() {
        SSOutdelivery outdelivery = outdelivery("Before outdelivery repo update");
        outdelivery.getRows().add(row("P-OUT-REPO-002", 1));
        Repositories.outdeliveries().add(outdelivery);

        SSDB.getInstance().clearLists();
        Optional<SSOutdelivery> fetched = Repositories.outdeliveries().findByOutdelivery(outdelivery);
        assertThat(fetched).isPresent();

        SSOutdelivery updatedOutdelivery = fetched.get();
        updatedOutdelivery.setText("After outdelivery repo update");
        updatedOutdelivery.setLocalDate(LocalDate.of(2025, 9, 14));
        updatedOutdelivery.getRows().clear();
        updatedOutdelivery.getRows().add(row("P-OUT-REPO-003", 6));
        Repositories.outdeliveries().update(updatedOutdelivery);

        SSDB.getInstance().clearLists();
        Optional<SSOutdelivery> updated = Repositories.outdeliveries().findByOutdelivery(outdelivery);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After outdelivery repo update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 14));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-OUT-REPO-003");
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(6);

        Integer number = updated.get().getNumber();
        Repositories.outdeliveries().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSOutdelivery> all = Repositories.outdeliveries().findAll();
        assertThat(all).extracting(SSOutdelivery::getNumber).doesNotContain(number);
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
        throw new IllegalStateException("Could not create test company for outdelivery repository V2 integration test");
    }
}

