package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;

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
 * Integration slice for credit invoice CRUD against schema V2.
 */
@Tag("integration")
class SSCreditInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-c-r-e-d-i-t-i-n-v-o-i-c-e-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Credit Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Credit Invoice Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
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
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchCreditInvoiceWithRowsInSchemaV2() {
        SSCreditInvoice creditInvoice = creditInvoice("CINV-V2-CUST-001", "V2 Credit Invoice Customer AB", 91001);
        creditInvoice.getRows().add(invoiceRow("P-CINV-001", "Consulting", new BigDecimal("1250.00"), 2, 3010));
        creditInvoice.getRows().add(invoiceRow("P-CINV-002", "Support", new BigDecimal("500.00"), 1, 3041));

        SSSalesContext.addCreditInvoice(creditInvoice);

        assertThat(creditInvoice.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSCreditInvoice> fetched = SSSalesContext.getCreditInvoice(creditInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerNr()).isEqualTo("CINV-V2-CUST-001");
        assertThat(fetched.get().getCustomerName()).isEqualTo("V2 Credit Invoice Customer AB");
        assertThat(fetched.get().getCreditingNr()).isEqualTo(91001);
        assertThat(fetched.get().getType()).isEqualTo(SSInvoiceType.CASH);
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.50");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 15));
        assertThat(fetched.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 8, 14));
        assertThat(fetched.get().getText()).isEqualTo("Credit invoice text in V2");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Consulting");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("1250.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(2);
        assertThat(fetched.get().getRows().get(0).getTaxCode()).isEqualTo(SSTaxCode.TAXRATE_1);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(3010);

        SSSalesContext.deleteCreditInvoice(creditInvoice);
    }

    @Test
    void updateAndDeleteCreditInvoiceInSchemaV2() {
        SSCreditInvoice creditInvoice = creditInvoice("CINV-V2-CUST-002", "Before V2 Credit Invoice Update", 91002);
        creditInvoice.getRows().add(invoiceRow("P-CINV-003", "Initial row", new BigDecimal("100.00"), 1, 3010));
        SSSalesContext.addCreditInvoice(creditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSCreditInvoice> fetched = SSSalesContext.getCreditInvoice(creditInvoice);
        assertThat(fetched).isPresent();

        SSCreditInvoice updatedCreditInvoice = fetched.get();
        updatedCreditInvoice.setCustomerName("After V2 Credit Invoice Update");
        updatedCreditInvoice.setText("Updated credit invoice text in V2");
        updatedCreditInvoice.setLocalDueDate(LocalDate.of(2025, 9, 1));
        updatedCreditInvoice.setCreditingNr(91099);
        updatedCreditInvoice.getRows().clear();
        updatedCreditInvoice.getRows().add(
                invoiceRow("P-CINV-004", "Updated row", new BigDecimal("750.00"), 3, 3041));
        SSSalesContext.updateCreditInvoice(updatedCreditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSCreditInvoice> updated = SSSalesContext.getCreditInvoice(creditInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After V2 Credit Invoice Update");
        assertThat(updated.get().getText()).isEqualTo("Updated credit invoice text in V2");
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(updated.get().getCreditingNr()).isEqualTo(91099);
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer creditInvoiceNumber = updated.get().getNumber();
        SSSalesContext.deleteCreditInvoice(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSCreditInvoice> all = SSSalesContext.getCreditInvoices();
        assertThat(all).extracting(SSCreditInvoice::getNumber).doesNotContain(creditInvoiceNumber);
    }

    private static SSCreditInvoice creditInvoice(String customerNr, String customerName, Integer creditingNr) {
        SSCreditInvoice creditInvoice = new SSCreditInvoice();
        creditInvoice.setCustomerNr(customerNr);
        creditInvoice.setCustomerName(customerName);
        creditInvoice.setCreditingNr(creditingNr);
        creditInvoice.setLocalDate(LocalDate.of(2025, 7, 15));
        creditInvoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        creditInvoice.setCurrencyRate(new BigDecimal("10.50"));
        creditInvoice.setType(SSInvoiceType.CASH);
        creditInvoice.setText("Credit invoice text in V2");
        creditInvoice.setYourOrderNumber("ORDER-V2-CREDIT-001");
        return creditInvoice;
    }

    private static SSSaleRow invoiceRow(String productNr, String description, BigDecimal unitPrice, int quantity,
                                        int accountNumber) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(accountNumber);
        row.setProjectNr("PRJ-1");
        row.setResultUnitNr("RES-1");
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


