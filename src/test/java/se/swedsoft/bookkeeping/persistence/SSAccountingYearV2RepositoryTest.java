package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSAccountingYearV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_accountingyear_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 AccountingYear Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 AccountingYear Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

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
    }

    @Test
    void addUpdateDeleteAccountingYearViaRepository() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-YEAR-L-001");
        Repositories.accountPlans().add(plan);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2027, 1, 1));
        year.setLocalTo(LocalDate.of(2027, 12, 31));
        year.setAccountPlan(plan);

        Repositories.accountingYears().add(year);
        assertThat(year.getId()).isNotNull();

        SSDB.getInstance().setCurrentYear(year);
        Optional<SSNewAccountingYear> current = Repositories.accountingYears().findCurrent();
        assertThat(current).isPresent();
        assertThat(current.get().getId()).isEqualTo(year.getId());

        year.setLocalTo(LocalDate.of(2028, 1, 31));
        Repositories.accountingYears().update(year);

        Optional<SSNewAccountingYear> reloaded = SSDB.getInstance().getAccountingYear(year);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getLocalTo()).isEqualTo(LocalDate.of(2028, 1, 31));

        Repositories.accountingYears().delete(year);
        assertThat(SSDB.getInstance().getAccountingYear(year)).isEmpty();

        Repositories.accountPlans().delete(plan);
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
        throw new IllegalStateException("Could not create test company for accounting-year repository test");
    }
}

