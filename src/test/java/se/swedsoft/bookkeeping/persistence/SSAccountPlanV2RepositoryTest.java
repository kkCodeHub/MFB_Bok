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

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addUpdateDeleteAccountPlanViaRepository() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-L-001");
        plan.setBaseName("BAS95");
        plan.setAssessementYear("2026");
        plan.setExcelPath("PLAN-L-001.xlsx");
        plan.setDefaultPlan(false);

        Repositories.accountPlans().add(plan);

        assertThat(plan.getId()).isNotNull();

        Optional<SSAccountPlan> fetched = Repositories.accountPlans().findById(plan.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("PLAN-L-001");
        assertThat(fetched.get().getExcelPath()).isEqualTo("PLAN-L-001.xlsx");
        assertThat(fetched.get().isDefaultPlan()).isFalse();
        assertThat(fetched.get().getAccounts()).isEmpty();

        SSAccountPlan update = fetched.get();
        update.setName("PLAN-L-001-UPDATED");
        update.setExcelPath("PLAN-L-001-UPDATED.xlsx");
        Repositories.accountPlans().update(update);

        Optional<SSAccountPlan> reloaded = Repositories.accountPlans().findById(plan.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("PLAN-L-001-UPDATED");
        assertThat(reloaded.get().getExcelPath()).isEqualTo("PLAN-L-001-UPDATED.xlsx");

        Repositories.accountPlans().delete(reloaded.get());

        Optional<SSAccountPlan> removed = Repositories.accountPlans().findById(plan.getId());
        assertThat(removed).isEmpty();
    }

    @Test
    void repositoryDoesNotPersistTemplateAccounts() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-L-NULL-NUMBER");
        plan.setExcelPath("PLAN-L-NULL-NUMBER.xlsx");

        SSAccount validAccount = new SSAccount();
        validAccount.setNumber(2440);
        validAccount.setDescription("Leverantorsskulder");
        plan.addAccount(validAccount);

        Repositories.accountPlans().add(plan);
        assertThat(plan.getId()).isNotNull();

        Optional<SSAccountPlan> fetched = Repositories.accountPlans().findById(plan.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getAccounts()).isEmpty();
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}
