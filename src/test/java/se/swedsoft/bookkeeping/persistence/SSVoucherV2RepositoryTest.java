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

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Voucher Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Voucher Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2026, 1, 1));
        year.setLocalTo(LocalDate.of(2026, 12, 31));
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
        SSDB.getInstance().setCurrentYear(year);
    }

    @Test
    void addUpdateDeleteVoucherViaRepository() {
        SSVoucher voucher = new SSVoucher(61001);
        voucher.setLocalDate(LocalDate.of(2026, 6, 15));
        voucher.setDescription("Voucher repository test");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("500.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("500.00")));

        Repositories.vouchers().add(voucher);

        Optional<SSVoucher> fetched = Repositories.vouchers().findByNumber(year, 61001);
        assertThat(fetched).isPresent();
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
        throw new IllegalStateException("Could not create test company for voucher repository test");
    }
}

