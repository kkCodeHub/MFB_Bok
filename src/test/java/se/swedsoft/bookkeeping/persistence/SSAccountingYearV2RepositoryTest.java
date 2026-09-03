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

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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
        assertThat(countAccountRowsForYear(year.getId())).isZero();

        Repositories.accountingYears().open(year);
        assertThat(countAccountRowsForYear(year.getId())).isEqualTo(1);
        Optional<SSNewAccountingYear> current = Repositories.accountingYears().findCurrent();
        assertThat(current).isPresent();
        assertThat(current.get().getId()).isEqualTo(year.getId());

        year.setInBalance(cash, new BigDecimal("1500.00"));
        Repositories.accountingYears().update(year);
        assertThat(countAccountRowsForYear(year.getId())).isEqualTo(1);

        Optional<SSNewAccountingYear> reloaded = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getAccountingYear(year);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getLocalTo()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(reloaded.get().getInBalance(new SSAccount(1910))).isEqualByComparingTo("1500.00");
        assertThat(reloaded.get().getBudget().getValueForAccountAndMonth(cash, january))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("300.00"));

        Repositories.accountingYears().delete(year);
        assertThat(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getAccountingYear(year)).isEmpty();
        assertThat(countChildRows("tbl_year_balance", year.getId())).isZero();
        assertThat(countChildRows("tbl_budget_row", year.getId())).isZero();
        assertThat(countAccountRowsForYear(year.getId())).isZero();

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

    @Test
    void updateAccountingYearBoundaryPreservesBudgetRows() throws Exception {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-YEAR-M-BOUNDARY");

        SSAccount account = new SSAccount();
        account.setNumber(1930);
        account.setDescription("Operating account");
        plan.addAccount(account);
        Repositories.accountPlans().add(plan);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2031, 1, 1));
        year.setLocalTo(LocalDate.of(2031, 12, 31));
        year.setAccountPlan(plan);

        SSMonth january = findMonth(year, 1);
        SSMonth february = findMonth(year, 2);
        year.getBudget().setSaldoForAccountAndMonth(account, january, new BigDecimal("100.00"));
        year.getBudget().setSaldoForAccountAndMonth(account, february, new BigDecimal("250.00"));

        Repositories.accountingYears().add(year);

        year.setLocalFrom(LocalDate.of(2031, 7, 1));
        year.setLocalTo(LocalDate.of(2032, 6, 30));
        Repositories.accountingYears().update(year);

        Optional<SSNewAccountingYear> reloaded = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getAccountingYear(year);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getLocalFrom()).isEqualTo(LocalDate.of(2031, 7, 1));
        assertThat(reloaded.get().getLocalTo()).isEqualTo(LocalDate.of(2032, 6, 30));

        SSMonth reloadedJanuary = findMonth(reloaded.get(), 1);
        SSMonth reloadedFebruary = findMonth(reloaded.get(), 2);
        SSAccount reloadedAccount = new SSAccount(1930);
        assertThat(reloaded.get().getBudget().getValueForAccountAndMonth(reloadedAccount, reloadedJanuary))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("100.00"));
        assertThat(reloaded.get().getBudget().getValueForAccountAndMonth(reloadedAccount, reloadedFebruary))
                .hasValueSatisfying(value -> assertThat(value).isEqualByComparingTo("250.00"));
        assertThat(countChildRows("tbl_budget_row", year.getId())).isEqualTo(2);

        Repositories.accountingYears().delete(year);
        Repositories.accountPlans().delete(plan);
    }

    @Test
    void openingYearWithoutSnapshotIsBlocked() throws Exception {
        Integer yearId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_accountingyear(companyid, from_date, to_date, accountplan_id) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, SSDB.getInstance().getCurrentCompany().getId());
            statement.setDate(2, java.sql.Date.valueOf(LocalDate.of(2040, 1, 1)));
            statement.setDate(3, java.sql.Date.valueOf(LocalDate.of(2040, 12, 31)));
            statement.setObject(4, null);
            statement.executeUpdate();
            connection.commit();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                yearId = keys.getInt(1);
            }
        }

        SSNewAccountingYear yearProbe = new SSNewAccountingYear();
        yearProbe.setId(yearId);

        assertThat(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.canOpenAccountingYear(yearProbe)).isFalse();
    }

    @Test
    void openingSecondYearReplacesAccountWorkspaceRows() throws Exception {
        SSAccountPlan planA = new SSAccountPlan();
        planA.setName("PLAN-YEAR-N-SWITCH-A");
        SSAccount accountA = new SSAccount();
        accountA.setNumber(1910);
        accountA.setDescription("Cash");
        planA.addAccount(accountA);
        Repositories.accountPlans().add(planA);

        SSAccountPlan planB = new SSAccountPlan();
        planB.setName("PLAN-YEAR-N-SWITCH-B");
        SSAccount accountB = new SSAccount();
        accountB.setNumber(1930);
        accountB.setDescription("Bank");
        planB.addAccount(accountB);
        Repositories.accountPlans().add(planB);

        SSNewAccountingYear yearA = new SSNewAccountingYear();
        yearA.setLocalFrom(LocalDate.of(2033, 1, 1));
        yearA.setLocalTo(LocalDate.of(2033, 12, 31));
        yearA.setAccountPlan(planA);
        Repositories.accountingYears().add(yearA);

        SSNewAccountingYear yearB = new SSNewAccountingYear();
        yearB.setLocalFrom(LocalDate.of(2034, 1, 1));
        yearB.setLocalTo(LocalDate.of(2034, 12, 31));
        yearB.setAccountPlan(planB);
        Repositories.accountingYears().add(yearB);

        Repositories.accountingYears().open(yearA);
        assertThat(countDistinctYearIdsInAccountTable()).isEqualTo(1);
        assertThat(countAccountRowsForYear(yearA.getId())).isEqualTo(1);

        Repositories.accountingYears().open(yearB);
        assertThat(countDistinctYearIdsInAccountTable()).isEqualTo(1);
        assertThat(countAccountRowsForYear(yearA.getId())).isZero();
        assertThat(countAccountRowsForYear(yearB.getId())).isEqualTo(1);

        Repositories.accountingYears().delete(yearA);
        Repositories.accountingYears().delete(yearB);
        Repositories.accountPlans().delete(planA);
        Repositories.accountPlans().delete(planB);
    }

    private static SSMonth findMonth(SSNewAccountingYear year, int monthNumber) {
        return year.getBudget().getMonths().stream()
                .filter(month -> month.getLocalFrom() != null && month.getLocalFrom().getMonthValue() == monthNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing month " + monthNumber + " in budget"));
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

    private static int countAccountRowsForYear(Integer yearId) throws Exception {
        String sql = "SELECT COUNT(*) FROM tbl_account WHERE accountingyear_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, yearId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countDistinctYearIdsInAccountTable() throws Exception {
        String sql = "SELECT COUNT(DISTINCT accountingyear_id) FROM tbl_account";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}

