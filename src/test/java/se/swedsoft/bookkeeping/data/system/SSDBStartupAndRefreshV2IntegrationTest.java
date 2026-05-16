package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for schema V2 startup seeding and synchronous cache refresh.
 */
@Tag("integration")
class SSDBStartupAndRefreshV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_startup_refresh";

    private static Connection connection;
    private static SSNewCompany exampleCompany;
    private static SSNewAccountingYear accountingYear;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        List<SSNewCompany> companies = SSDB.getInstance().getCompanies();
        assertThat(companies).hasSize(1);
        exampleCompany = companies.get(0);
        SSDB.getInstance().setCurrentCompany(exampleCompany);

        accountingYear = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(accountingYear);
        SSDB.getInstance().setCurrentYear(accountingYear);
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
    void resetState() {
        SSDB.getInstance().setCurrentCompany(exampleCompany);
        SSDB.getInstance().setCurrentYear(accountingYear);
        SSDB.getInstance().clearLists();
    }

    @Test
    void startupLocalCreatesExampleCompanyInSchemaV2WhenDatabaseIsEmpty() {
        List<SSNewCompany> companies = SSDB.getInstance().getCompanies();

        assertThat(companies).hasSize(1);
        assertThat(companies.get(0).getName()).isEqualTo("Demoföretaget");
    }

    @Test
    void addingCustomerUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSDB.getInstance().getCustomers().size();

        SSCustomer customer = new SSCustomer();
        customer.setNumber("C-REFRESH-001");
        customer.setName("Refresh Customer AB");

        SSDB.getInstance().addCustomer(customer);

        assertThat(SSDB.getInstance().getCustomers())
                .extracting(SSCustomer::getNumber)
                .contains("C-REFRESH-001");
        assertThat(SSDB.getInstance().getCustomers()).hasSize(initialSize + 1);

        SSDB.getInstance().deleteCustomer(customer);
        assertThat(SSDB.getInstance().getCustomers())
                .extracting(SSCustomer::getNumber)
                .doesNotContain("C-REFRESH-001");
    }

    @Test
    void addingProductUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSDB.getInstance().getProducts().size();

        SSProduct product = new SSProduct();
        product.setNumber("P-REFRESH-001");
        product.setDescription("Refresh Product");
        product.setSellingPrice(new BigDecimal("42.00"));

        SSDB.getInstance().addProduct(product);

        assertThat(SSDB.getInstance().getProducts())
                .extracting(SSProduct::getNumber)
                .contains("P-REFRESH-001");
        assertThat(SSDB.getInstance().getProducts()).hasSize(initialSize + 1);

        SSDB.getInstance().deleteProduct(product);
        assertThat(SSDB.getInstance().getProducts())
                .extracting(SSProduct::getNumber)
                .doesNotContain("P-REFRESH-001");
    }

    @Test
    void addingVoucherUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSDB.getInstance().getVouchers().size();

        SSVoucher voucher = new SSVoucher(98_001);
        voucher.setLocalDate(LocalDate.of(2025, 6, 15));
        voucher.setDescription("Refresh voucher");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("250.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("250.00")));

        SSDB.getInstance().addVoucher(voucher, true);

        assertThat(SSDB.getInstance().getVouchers())
                .extracting(SSVoucher::getNumber)
                .contains(98_001);
        assertThat(SSDB.getInstance().getVouchers()).hasSize(initialSize + 1);

        SSDB.getInstance().deleteVoucher(voucher);
        assertThat(SSDB.getInstance().getVouchers())
                .extracting(SSVoucher::getNumber)
                .doesNotContain(98_001);
    }

    private static SSVoucherRow voucherRow(int accountNumber, BigDecimal debet, BigDecimal credit) {
        SSVoucherRow row = new SSVoucherRow();
        row.setAccountNr(accountNumber);
        row.setDebet(debet);
        row.setCredit(credit);
        return row;
    }
}

