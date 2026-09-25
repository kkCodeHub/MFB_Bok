package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSVoucherV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_voucher_repo";

    private static Connection connection;
    private static SSNewAccountingYear year;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Voucher Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Voucher Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2026, 1, 1));
        year.setLocalTo(LocalDate.of(2026, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().setCurrentYear(year);
    }

    @Test
    void addUpdateDeleteVoucherViaRepository() {
        SSVoucher voucher = new SSVoucher(61001);
        voucher.setSeries("B");
        voucher.setLocalDate(LocalDate.of(2026, 6, 15));
        voucher.setDescription("Voucher repository test");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("500.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("500.00")));

        Repositories.vouchers().add(voucher);

        Optional<SSVoucher> fetched = Repositories.vouchers().findByNumber(year, 61001);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSeries()).isEqualTo("B");
        assertThat(fetched.get().getDescription()).isEqualTo("Voucher repository test");
        assertThat(fetched.get().getRows()).hasSize(2);

        SSVoucher updated = fetched.get();
        updated.setDescription("Voucher repository updated");
        Repositories.vouchers().update(updated);

        Optional<SSVoucher> reloaded = Repositories.vouchers().findByNumber(year, 61001);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Voucher repository updated");

        List<SSVoucher> byYear = Repositories.vouchers().findByYear(year);
        assertThat(byYear).extracting(SSVoucher::getNumber).contains(61001);

        Repositories.vouchers().delete(reloaded.get());
        assertThat(Repositories.vouchers().findByNumber(year, 61001)).isEmpty();
    }

    @Test
    void resolveUpdateAndDeleteBySeriesWhenNumbersOverlap() {
        SSVoucher voucherA = new SSVoucher(61002);
        voucherA.setSeries("A");
        voucherA.setLocalDate(LocalDate.of(2026, 6, 20));
        voucherA.setDescription("Series A voucher");
        voucherA.getRows().add(voucherRow(1910, new BigDecimal("100.00"), null));
        voucherA.getRows().add(voucherRow(3010, null, new BigDecimal("100.00")));

        SSVoucher voucherB = new SSVoucher(61002);
        voucherB.setSeries("B");
        voucherB.setLocalDate(LocalDate.of(2026, 6, 21));
        voucherB.setDescription("Series B voucher");
        voucherB.getRows().add(voucherRow(1910, new BigDecimal("200.00"), null));
        voucherB.getRows().add(voucherRow(3010, null, new BigDecimal("200.00")));

        Repositories.vouchers().add(voucherA);
        Repositories.vouchers().add(voucherB);

        Optional<SSVoucher> fetchedA = Repositories.vouchers().findBySeriesAndNumber(year, "A", 61002);
        Optional<SSVoucher> fetchedB = Repositories.vouchers().findBySeriesAndNumber(year, "B", 61002);
        assertThat(fetchedA).isPresent();
        assertThat(fetchedB).isPresent();
        assertThat(fetchedA.get().getDescription()).isEqualTo("Series A voucher");
        assertThat(fetchedB.get().getDescription()).isEqualTo("Series B voucher");

        SSVoucher updatedB = fetchedB.get();
        updatedB.setDescription("Series B voucher updated");
        Repositories.vouchers().update(updatedB);

        Optional<SSVoucher> reloadedA = Repositories.vouchers().findBySeriesAndNumber(year, "A", 61002);
        Optional<SSVoucher> reloadedB = Repositories.vouchers().findBySeriesAndNumber(year, "B", 61002);
        assertThat(reloadedA).isPresent();
        assertThat(reloadedB).isPresent();
        assertThat(reloadedA.get().getDescription()).isEqualTo("Series A voucher");
        assertThat(reloadedB.get().getDescription()).isEqualTo("Series B voucher updated");

        Repositories.vouchers().delete(reloadedB.get());
        assertThat(Repositories.vouchers().findBySeriesAndNumber(year, "A", 61002)).isPresent();
        assertThat(Repositories.vouchers().findBySeriesAndNumber(year, "B", 61002)).isEmpty();

        Repositories.vouchers().delete(reloadedA.get());
    }

    private static SSVoucherRow voucherRow(int accountNumber, BigDecimal debet, BigDecimal credit) {
        SSVoucherRow row = new SSVoucherRow();
        row.setAccountNr(accountNumber);
        row.setDebet(debet);
        row.setCredit(credit);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}
