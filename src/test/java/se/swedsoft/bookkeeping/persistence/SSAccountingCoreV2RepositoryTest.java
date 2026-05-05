package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
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
class SSAccountingCoreV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_accounting_core_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Accounting Core Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Accounting Core Repo Test AB");
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
    void endToEndAccountingCoreFlowViaRepositories() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("PLAN-CORE-L-001");
        plan.setBaseName("BAS95");
        plan.setAssessementYear("2026");

        SSAccount cash = new SSAccount();
        cash.setNumber(1910);
        cash.setDescription("Cash");
        plan.addAccount(cash);

        Repositories.accountPlans().add(plan);
        assertThat(plan.getId()).isNotNull();

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2026, 1, 1));
        year.setLocalTo(LocalDate.of(2026, 12, 31));
        year.setAccountPlan(plan);
        Repositories.accountingYears().add(year);
        SSDB.getInstance().setCurrentYear(year);

        SSVoucher voucher = new SSVoucher(62001);
        voucher.setLocalDate(LocalDate.of(2026, 2, 10));
        voucher.setDescription("Core repository voucher");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("250.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("250.00")));
        Repositories.vouchers().add(voucher);

        Optional<SSAccountPlan> fetchedPlan = Repositories.accountPlans().findById(plan.getId());
        Optional<SSVoucher> fetchedVoucher = Repositories.vouchers().findByNumber(year, 62001);
        Optional<SSNewAccountingYear> currentYear = Repositories.accountingYears().findCurrent();

        assertThat(fetchedPlan).isPresent();
        assertThat(fetchedVoucher).isPresent();
        assertThat(currentYear).isPresent();
        assertThat(fetchedVoucher.get().getRows()).hasSize(2);

        Repositories.vouchers().delete(fetchedVoucher.get());
        Repositories.accountingYears().delete(year);
        Repositories.accountPlans().delete(plan);

        assertThat(Repositories.vouchers().findByNumber(year, 62001)).isEmpty();
        assertThat(Repositories.accountPlans().findById(plan.getId())).isEmpty();
    }

    private static SSVoucherRow voucherRow(int accountNumber, BigDecimal debet, BigDecimal credit) {
        SSVoucherRow row = new SSVoucherRow();
        row.setAccountNr(accountNumber);
        row.setDebet(debet);
        row.setCredit(credit);
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
        throw new IllegalStateException("Could not create test company for accounting-core repository test");
    }
}

