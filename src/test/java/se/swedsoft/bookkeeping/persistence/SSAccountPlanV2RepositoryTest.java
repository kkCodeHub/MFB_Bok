package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSAccountPlanV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_accountplan_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 AccountPlan Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 AccountPlan Repo Test AB");
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
    void addUpdateDeleteAccountPlanViaRepository() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-L-001");
        plan.setBaseName("BAS95");
        plan.setAssessementYear("2026");

        SSAccount account = new SSAccount();
        account.setNumber(1910);
        account.setDescription("Cash account");
        plan.addAccount(account);

        Repositories.accountPlans().add(plan);

        assertThat(plan.getId()).isNotNull();

        Optional<SSAccountPlan> fetched = Repositories.accountPlans().findById(plan.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("PLAN-L-001");
        assertThat(fetched.get().getAccounts()).hasSize(1);

        SSAccountPlan update = fetched.get();
        update.setName("PLAN-L-001-UPDATED");
        update.getAccounts().get(0).setDescription("Updated cash account");
        Repositories.accountPlans().update(update);

        Optional<SSAccountPlan> reloaded = Repositories.accountPlans().findById(plan.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("PLAN-L-001-UPDATED");
        assertThat(reloaded.get().getAccounts().get(0).getDescription()).isEqualTo("Updated cash account");

        Repositories.accountPlans().delete(reloaded.get());

        Optional<SSAccountPlan> removed = Repositories.accountPlans().findById(plan.getId());
        assertThat(removed).isEmpty();
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
        throw new IllegalStateException("Could not create test company for account-plan repository test");
    }
}

