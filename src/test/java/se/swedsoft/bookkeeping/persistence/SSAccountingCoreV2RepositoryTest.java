package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSAccountingCoreV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_accounting_core_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Database() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(JDBC_URL);
        Integer iCompanyId = SSV2DatabaseFixture.createCompany(connection, "V2 Accounting Core Repo Test AB");
        SSV2DatabaseFixture.setCurrentCompany(iCompanyId, "V2 Accounting Core Repo Test AB");

        Repositories.init(SSDB.getInstance());
    }

    @AfterAll
    static void teardownV2Database() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @BeforeEach
    void resetState() {
        SSV2DatabaseFixture.clearState();
    }

    @AfterEach
    void cleanupState() {
        SSV2DatabaseFixture.clearState();
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
        year.setInBalance(cash, new BigDecimal("5000.00"));
        year.getBudget().setSaldoForAccountAndMonth(cash, year.getBudget().getMonths().get(0),
                new BigDecimal("800.00"));
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
        assertThat(currentYear.get().getInBalance(new SSAccount(1910))).isEqualByComparingTo("5000.00");
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

}
