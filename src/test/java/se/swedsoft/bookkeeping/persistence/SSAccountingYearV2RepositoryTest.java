package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSMonth;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.math.BigDecimal;
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
    void addUpdateDeleteAccountingYearViaRepository() throws Exception {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-YEAR-L-001");

        SSAccount cash = new SSAccount();
        cash.setNumber(1910);
        cash.setDescription("Cash");
        plan.addAccount(cash);

        Repositories.accountPlans().add(plan);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2027, 1, 1));
        year.setLocalTo(LocalDate.of(2027, 12, 31));
        year.setAccountPlan(plan);
        year.setInBalance(cash, new BigDecimal("1250.00"));

        SSMonth january = year.getBudget().getMonths().get(0);
        year.getBudget().setSaldoForAccountAndMonth(cash, january, new BigDecimal("300.00"));

        Repositories.accountingYears().add(year);
        assertThat(year.getId()).isNotNull();

        SSDB.getInstance().setCurrentYear(year);
        Optional<SSNewAccountingYear> current = Repositories.accountingYears().findCurrent();
        assertThat(current).isPresent();
        assertThat(current.get().getId()).isEqualTo(year.getId());

        year.setInBalance(cash, new BigDecimal("1500.00"));
        Repositories.accountingYears().update(year);

        Optional<SSNewAccountingYear> reloaded = SSDB.getInstance().getAccountingYear(year);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getLocalTo()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(reloaded.get().getInBalance(new SSAccount(1910))).isEqualByComparingTo("1500.00");
        assertThat(reloaded.get().getBudget().getValueForAccountAndMonth(cash, january))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("300.00"));

        Repositories.accountingYears().delete(year);
        assertThat(SSDB.getInstance().getAccountingYear(year)).isEmpty();
        assertThat(countChildRows("tbl_year_balance", year.getId())).isZero();
        assertThat(countChildRows("tbl_budget_row", year.getId())).isZero();

        Repositories.accountPlans().delete(plan);
    }

    @Test
    void addAccountingYearWithNullBudgetStoresNoBudgetRows() throws Exception {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-YEAR-M-NULL-BUDGET");
        Repositories.accountPlans().add(plan);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2029, 1, 1));
        year.setLocalTo(LocalDate.of(2029, 12, 31));
        year.setAccountPlan(plan);
        year.setBudget(null);

        Repositories.accountingYears().add(year);

        assertThat(year.getId()).isNotNull();
        assertThat(countChildRows("tbl_budget_row", year.getId())).isZero();

        Repositories.accountingYears().delete(year);
        Repositories.accountPlans().delete(plan);
    }

    @Test
    void updateAccountingYearWithNullBudgetAndInBalanceClearsChildRows() throws Exception {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-YEAR-M-NULL-UPDATE");

        SSAccount account = new SSAccount();
        account.setNumber(1940);
        account.setDescription("Bank");
        plan.addAccount(account);
        Repositories.accountPlans().add(plan);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2030, 1, 1));
        year.setLocalTo(LocalDate.of(2030, 12, 31));
        year.setAccountPlan(plan);
        year.setInBalance(account, new BigDecimal("999.00"));
        year.getBudget().setSaldoForAccountAndMonth(account, year.getBudget().getMonths().get(0),
                new BigDecimal("123.00"));

        Repositories.accountingYears().add(year);
        assertThat(countChildRows("tbl_year_balance", year.getId())).isGreaterThan(0);
        assertThat(countChildRows("tbl_budget_row", year.getId())).isGreaterThan(0);

        year.setInBalance(null);
        year.setBudget(null);
        Repositories.accountingYears().update(year);

        assertThat(countChildRows("tbl_year_balance", year.getId())).isZero();
        assertThat(countChildRows("tbl_budget_row", year.getId())).isZero();

        Repositories.accountingYears().delete(year);
        Repositories.accountPlans().delete(plan);
    }

    private static int countChildRows(String tableName, Integer yearId) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE year_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, yearId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
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

