package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;

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

/**
 * Integration slice for voucher core against schema V2.
 */
@Tag("integration")
class SSVoucherV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_voucher";
    private static final int BASE_NUMBER = 95_000;

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Voucher Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Voucher Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
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
    void accountingYearRoundTripsInSchemaV2() {
        List<SSNewAccountingYear> years = SSDB.getInstance().getYears();

        assertThat(years).isNotEmpty();
        assertThat(years.get(0).getLocalFrom()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(years.get(0).getLocalTo()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void addAndFetchVoucherWithRowsInSchemaV2() {
        SSVoucher voucher = voucher(BASE_NUMBER + 1);
        voucher.setDescription("V2 voucher");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("500.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("500.00")));

        SSDB.getInstance().addVoucher(voucher, true);

        Optional<SSVoucher> fetched = SSDB.getInstance().getVoucher(new SSVoucher(BASE_NUMBER + 1));
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("V2 voucher");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDebet()).isEqualByComparingTo("500.00");

        SSDB.getInstance().deleteVoucher(voucher);
    }

    @Test
    void updateDeleteAndLastNumberVoucherInSchemaV2() {
        SSVoucher voucher = voucher(BASE_NUMBER + 2);
        voucher.setDescription("Before voucher update");
        SSDB.getInstance().addVoucher(voucher, true);

        Optional<SSVoucher> fetched = SSDB.getInstance().getVoucher(new SSVoucher(BASE_NUMBER + 2));
        assertThat(fetched).isPresent();

        SSVoucher updatedVoucher = fetched.get();
        updatedVoucher.setDescription("After voucher update");
        updatedVoucher.getRows().add(voucherRow(1510, new BigDecimal("100.00"), null));
        updatedVoucher.getRows().add(voucherRow(2610, null, new BigDecimal("100.00")));
        SSDB.getInstance().updateVoucher(updatedVoucher);

        SSDB.getInstance().clearLists();
        Optional<SSVoucher> updated = SSDB.getInstance().getVoucher(new SSVoucher(BASE_NUMBER + 2));
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After voucher update");
        assertThat(updated.get().getRows()).hasSize(2);
        assertThat(SSDB.getInstance().getLastVoucherNumber()).isGreaterThanOrEqualTo(BASE_NUMBER + 2);

        SSDB.getInstance().deleteVoucher(updated.get());
        SSDB.getInstance().clearLists();
        assertThat(SSDB.getInstance().getVouchers())
                .extracting(SSVoucher::getNumber)
                .doesNotContain(BASE_NUMBER + 2);
    }

    private static SSVoucher voucher(int number) {
        SSVoucher voucher = new SSVoucher(number);
        voucher.setLocalDate(LocalDate.of(2025, 6, 15));
        return voucher;
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
        throw new IllegalStateException("Could not create test company for schema V2 voucher test");
    }
}

