package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration slice for voucher core against schema V2.
 */
@Tag("integration")
class SSVoucherV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-v-o-u-c-h-e-r-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";
    private static final int BASE_NUMBER = 95_000;

    private static Connection connection;

    @BeforeAll
    static void setupV2Database() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(JDBC_URL);
        Integer iCompanyId = SSV2DatabaseFixture.createCompany(connection, "V2 Voucher Test Company AB");
        SSV2DatabaseFixture.setCurrentCompany(iCompanyId, "V2 Voucher Test Company AB");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterAll
    static void teardownV2Database() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @BeforeEach
    void resetState() {
        SSV2DatabaseFixture.clearState();
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void cleanupState() {
        SSV2DatabaseFixture.clearState();
    }

    @Test
    void accountingYearRoundTripsInSchemaV2() {
        List<SSNewAccountingYear> years = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYears();

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

        SSAccountingContext.addVoucher(voucher, true);

        Optional<SSVoucher> fetched = SSAccountingContext.getVoucher(new SSVoucher(BASE_NUMBER + 1));
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("V2 voucher");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDebet()).isEqualByComparingTo("500.00");

        SSAccountingContext.deleteVoucher(voucher);
    }

    @Test
    void updateDeleteAndLastNumberVoucherInSchemaV2() {
        SSVoucher voucher = voucher(BASE_NUMBER + 2);
        voucher.setDescription("Before voucher update");
        SSAccountingContext.addVoucher(voucher, true);

        Optional<SSVoucher> fetched = SSAccountingContext.getVoucher(new SSVoucher(BASE_NUMBER + 2));
        assertThat(fetched).isPresent();

        SSVoucher updatedVoucher = fetched.get();
        updatedVoucher.setDescription("After voucher update");
        updatedVoucher.getRows().add(voucherRow(1510, new BigDecimal("100.00"), null));
        updatedVoucher.getRows().add(voucherRow(2610, null, new BigDecimal("100.00")));
        SSAccountingContext.updateVoucher(updatedVoucher);

        SSV2DatabaseFixture.clearState();
        Optional<SSVoucher> updated = SSAccountingContext.getVoucher(new SSVoucher(BASE_NUMBER + 2));
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After voucher update");
        assertThat(updated.get().getRows()).hasSize(2);
        assertThat(SSAccountingContext.getLastVoucherNumber()).isGreaterThanOrEqualTo(BASE_NUMBER + 2);

        SSAccountingContext.deleteVoucher(updated.get());
        SSV2DatabaseFixture.clearState();
        assertThat(SSAccountingContext.getVouchers())
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

}


