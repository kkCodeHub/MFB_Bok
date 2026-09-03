package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
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
 * Integration slice for supplier-credit-invoice CRUD against schema V2.
 */
@Tag("integration")
class SSSupplierCreditInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-s-u-p-p-l-i-e-r-c-r-e-d-i-t-i-n-v-o-i-c-e-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Supplier Credit Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Supplier Credit Invoice Test Company AB");
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
    void addAndFetchSupplierCreditInvoiceWithRowsInSchemaV2() {
        SSSupplierCreditInvoice supplierCreditInvoice = supplierCreditInvoice(
                "SUP-CR-001", "Supplier Credit V2 AB", "REF-CR-001", 1001);
        supplierCreditInvoice.getRows().add(invoiceRow("P-SCI-001", "Returned hardware", new BigDecimal("125.00"), 2, 4010));
        supplierCreditInvoice.getRows().add(invoiceRow("P-SCI-002", "Returned service", new BigDecimal("500.00"), 1, 4041));

        SSPurchaseContext.addSupplierCreditInvoice(supplierCreditInvoice);

        assertThat(supplierCreditInvoice.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierCreditInvoice> fetched = SSPurchaseContext.getSupplierCreditInvoice(supplierCreditInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCreditingNr()).isEqualTo(1001);
        assertThat(fetched.get().getSupplierNr()).isEqualTo("SUP-CR-001");
        assertThat(fetched.get().getSupplierName()).isEqualTo("Supplier Credit V2 AB");
        assertThat(fetched.get().getReferencenumber()).isEqualTo("REF-CR-001");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 15));
        assertThat(fetched.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 8, 14));
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.00");
        assertThat(fetched.get().getTaxSum()).isEqualByComparingTo("100.00");
        assertThat(fetched.get().getRoundingSum()).isEqualByComparingTo("0.25");
        assertThat(fetched.get().isStockInfluencing()).isTrue();
        assertThat(fetched.get().isBGCEntered()).isFalse();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Returned hardware");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("125.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(2);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(4010);

        SSPurchaseContext.deleteSupplierCreditInvoice(supplierCreditInvoice);
    }

    @Test
    void updateAndDeleteSupplierCreditInvoiceInSchemaV2() {
        SSSupplierCreditInvoice supplierCreditInvoice = supplierCreditInvoice(
                "SUP-CR-002", "Before Supplier Credit Update", "REF-CR-002", 1002);
        supplierCreditInvoice.getRows().add(invoiceRow("P-SCI-003", "Initial row", new BigDecimal("100.00"), 1, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(supplierCreditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierCreditInvoice> fetched = SSPurchaseContext.getSupplierCreditInvoice(supplierCreditInvoice);
        assertThat(fetched).isPresent();

        SSSupplierCreditInvoice updatedSupplierCreditInvoice = fetched.get();
        updatedSupplierCreditInvoice.setSupplierName("After Supplier Credit Update");
        updatedSupplierCreditInvoice.setReferencenumber("REF-CR-UPDATED");
        updatedSupplierCreditInvoice.setCreditingNr(2002);
        updatedSupplierCreditInvoice.setLocalDueDate(LocalDate.of(2025, 9, 5));
        updatedSupplierCreditInvoice.setBGCEntered(true);
        updatedSupplierCreditInvoice.setStockInfluencing(false);
        updatedSupplierCreditInvoice.getRows().clear();
        updatedSupplierCreditInvoice.getRows().add(
                invoiceRow("P-SCI-004", "Updated row", new BigDecimal("750.00"), 3, 4041));
        SSPurchaseContext.updateSupplierCreditInvoice(updatedSupplierCreditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierCreditInvoice> updated = SSPurchaseContext.getSupplierCreditInvoice(supplierCreditInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After Supplier Credit Update");
        assertThat(updated.get().getReferencenumber()).isEqualTo("REF-CR-UPDATED");
        assertThat(updated.get().getCreditingNr()).isEqualTo(2002);
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 5));
        assertThat(updated.get().isBGCEntered()).isTrue();
        assertThat(updated.get().isStockInfluencing()).isFalse();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer supplierCreditInvoiceNumber = updated.get().getNumber();
        SSPurchaseContext.deleteSupplierCreditInvoice(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSSupplierCreditInvoice> all = SSPurchaseContext.getSupplierCreditInvoices();
        assertThat(all).extracting(SSSupplierCreditInvoice::getNumber).doesNotContain(supplierCreditInvoiceNumber);
    }

    private static SSSupplierCreditInvoice supplierCreditInvoice(
            String supplierNr,
            String supplierName,
            String referenceNumber,
            Integer creditingNr) {
        SSSupplierCreditInvoice supplierCreditInvoice = new SSSupplierCreditInvoice();
        supplierCreditInvoice.setCreditingNr(creditingNr);
        supplierCreditInvoice.setSupplierNr(supplierNr);
        supplierCreditInvoice.setSupplierName(supplierName);
        supplierCreditInvoice.setReferencenumber(referenceNumber);
        supplierCreditInvoice.setLocalDate(LocalDate.of(2025, 7, 15));
        supplierCreditInvoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        supplierCreditInvoice.setCurrencyRate(new BigDecimal("10.00"));
        supplierCreditInvoice.setTaxSum(new BigDecimal("100.00"));
        supplierCreditInvoice.setRoundingSum(new BigDecimal("0.25"));
        supplierCreditInvoice.setEntered(false);
        supplierCreditInvoice.setStockInfluencing(true);
        supplierCreditInvoice.setBGCEntered(false);
        return supplierCreditInvoice;
    }

    private static SSSupplierInvoiceRow invoiceRow(
            String productNr,
            String description,
            BigDecimal unitPrice,
            int quantity,
            int accountNumber) {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setUnitFreight(new BigDecimal("3.50"));
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


