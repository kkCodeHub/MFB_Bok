package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-d-b-s-t-a-r-t-u-p-a-n-d-r-e-f-r-e-s-h-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;
    private static SSNewCompany exampleCompany;
    private static SSNewAccountingYear accountingYear;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        List<SSNewCompany> companies = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies();
        assertThat(companies).hasSize(1);
        exampleCompany = companies.get(0);
        assertThat(SSDB.getInstance().getCurrentCompany()).isNotNull();
        assertThat(SSDB.getInstance().getCurrentCompany().getId()).isEqualTo(exampleCompany.getId());
        SSDB.getInstance().setCurrentCompany(exampleCompany);

        List<SSNewAccountingYear> years = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYearsForCompany(exampleCompany);
        assertThat(years).isNotEmpty();
        accountingYear = years.get(0);
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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void startupLocalCreatesExampleCompanyInSchemaV2WhenDatabaseIsEmpty() {
        List<SSNewCompany> companies = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies();

        assertThat(companies).hasSize(1);
        assertThat(companies.get(0).getName())
                .isIn("Demoföretaget", "Demoföretaget AB", "DemofÃ¶retaget", "DemofÃ¶retaget AB",
                      "Demof├╢retaget", "Demof├╢retaget AB", "Demof├┬╢retaget");
    }

    @Test
    void startupLocalImportsDefaultAccountPlansInSchemaV2() {
        assertThat(SSAccountingContext.getAccountPlans()).isNotEmpty();
    }

    @Test
    void startupLocalSetsCurrentCompanyInSchemaV2() {
        SSNewCompany current = SSDB.getInstance().getCurrentCompany();

        assertThat(current).isNotNull();
        assertThat(current.getName())
                .isIn("Demoföretaget", "Demoföretaget AB", "DemofÃ¶retaget", "DemofÃ¶retaget AB",
                      "Demof├╢retaget", "Demof├╢retaget AB");
    }

    @Test
    void startupLocalCreatesInitialAccountingYearInSchemaV2() {
        List<SSNewAccountingYear> years = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYearsForCompany(exampleCompany);

        assertThat(years).hasSize(1);
        assertThat(SSDB.getInstance().getCurrentYear()).isNotNull();
        assertThat(SSDB.getInstance().getCurrentYear().getAccountPlan()).isNotNull();
        assertThat(SSDB.getInstance().getCurrentYear().getLocalFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(SSDB.getInstance().getCurrentYear().getLocalTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void startupLocalSeedsRequestedDemoEntitiesInSchemaV2() {
        // Customers, suppliers and products are seeded from Seed_Demo.json
        assertThat(SSMasterdataContext.getCustomers())
                .extracting(SSCustomer::getNumber)
                .containsExactlyInAnyOrder("K001", "K002");

        assertThat(SSProductContext.getProducts())
                .extracting(SSProduct::getNumber)
                .containsExactlyInAnyOrder("P001", "P002", "P003");

        assertThat(SSMasterdataContext.getSuppliers())
                .extracting(se.swedsoft.bookkeeping.data.SSSupplier::getNumber)
                .containsExactlyInAnyOrder("L001", "L002");

        // Vouchers are seeded from Seed_Demo_VerFakt.json with auto-assigned numbers
        assertThat(SSAccountingContext.getVouchers()).hasSize(2);
    }

    @Test
    void startupLocalDoesNotReseedWhenDatabaseAlreadyExists() throws Exception {
        int companyCountBefore = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies().size();
        int yearCountBefore = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYearsForCompany(exampleCompany).size();
        int customerCountBefore = SSMasterdataContext.getCustomers().size();
        int productCountBefore = SSProductContext.getProducts().size();
        int supplierCountBefore = SSMasterdataContext.getSuppliers().size();
        int voucherCountBefore = SSAccountingContext.getVouchers().size();

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);
        SSDB.getInstance().setCurrentCompany(exampleCompany);
        SSDB.getInstance().setCurrentYear(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYearsForCompany(exampleCompany).get(0));

        assertThat(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies()).hasSize(companyCountBefore);
        assertThat(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getYearsForCompany(exampleCompany)).hasSize(yearCountBefore);
        assertThat(SSMasterdataContext.getCustomers()).hasSize(customerCountBefore);
        assertThat(SSProductContext.getProducts()).hasSize(productCountBefore);
        assertThat(SSMasterdataContext.getSuppliers()).hasSize(supplierCountBefore);
        assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBefore);
    }

    @Test
    void addingCustomerUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSMasterdataContext.getCustomers().size();

        SSCustomer customer = new SSCustomer();
        customer.setNumber("C-REFRESH-001");
        customer.setName("Refresh Customer AB");

        SSMasterdataContext.addCustomer(customer);

        assertThat(SSMasterdataContext.getCustomers())
                .extracting(SSCustomer::getNumber)
                .contains("C-REFRESH-001");
        assertThat(SSMasterdataContext.getCustomers()).hasSize(initialSize + 1);

        SSMasterdataContext.deleteCustomer(customer);
        assertThat(SSMasterdataContext.getCustomers())
                .extracting(SSCustomer::getNumber)
                .doesNotContain("C-REFRESH-001");
    }

    @Test
    void addingProductUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSProductContext.getProducts().size();

        SSProduct product = new SSProduct();
        product.setNumber("P-REFRESH-001");
        product.setDescription("Refresh Product");
        product.setSellingPrice(new BigDecimal("42.00"));

        SSProductContext.addProduct(product);

        assertThat(SSProductContext.getProducts())
                .extracting(SSProduct::getNumber)
                .contains("P-REFRESH-001");
        assertThat(SSProductContext.getProducts()).hasSize(initialSize + 1);

        SSProductContext.deleteProduct(product);
        assertThat(SSProductContext.getProducts())
                .extracting(SSProduct::getNumber)
                .doesNotContain("P-REFRESH-001");
    }

    @Test
    void addingVoucherUpdatesWarmCacheImmediatelyInSchemaV2() {
        int initialSize = SSAccountingContext.getVouchers().size();

        SSVoucher voucher = new SSVoucher(98_001);
        voucher.setLocalDate(accountingYear.getLocalFrom().plusDays(10));
        voucher.setDescription("Refresh voucher");
        voucher.getRows().add(voucherRow(1910, new BigDecimal("250.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("250.00")));

        SSAccountingContext.addVoucher(voucher, true);

        assertThat(SSAccountingContext.getVouchers())
                .extracting(SSVoucher::getNumber)
                .contains(98_001);
        assertThat(SSAccountingContext.getVouchers()).hasSize(initialSize + 1);

        SSAccountingContext.deleteVoucher(voucher);
        assertThat(SSAccountingContext.getVouchers())
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

