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
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
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
 * Integration slice for supplier invoice CRUD against schema V2.
 */
@Tag("integration")
class SSSupplierInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-s-u-p-p-l-i-e-r-i-n-v-o-i-c-e-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Supplier Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Supplier Invoice Test Company AB");
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
    void addAndFetchSupplierInvoiceWithRowsInSchemaV2() {
        SSSupplierInvoice supplierInvoice = supplierInvoice("SUP-001", "Supplier V2 AB", "REF-001");
        supplierInvoice.getRows().add(invoiceRow("P-SINV-001", "Hardware", new BigDecimal("125.00"), 4, 4010));
        supplierInvoice.getRows().add(invoiceRow("P-SINV-002", "Service", new BigDecimal("500.00"), 1, 4041));

        SSPurchaseContext.addSupplierInvoice(supplierInvoice);

        assertThat(supplierInvoice.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierInvoice> fetched = SSPurchaseContext.getSupplierInvoice(supplierInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierNr()).isEqualTo("SUP-001");
        assertThat(fetched.get().getSupplierName()).isEqualTo("Supplier V2 AB");
        assertThat(fetched.get().getReferencenumber()).isEqualTo("REF-001");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 10));
        assertThat(fetched.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 8, 9));
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.00");
        assertThat(fetched.get().getTaxSum()).isEqualByComparingTo("100.00");
        assertThat(fetched.get().getRoundingSum()).isEqualByComparingTo("0.25");
        assertThat(fetched.get().isStockInfluencing()).isTrue();
        assertThat(fetched.get().isBGCEntered()).isFalse();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Hardware");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("125.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(4);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(4010);

        SSPurchaseContext.deleteSupplierInvoice(supplierInvoice);
    }

    @Test
    void updateAndDeleteSupplierInvoiceInSchemaV2() {
        SSSupplierInvoice supplierInvoice = supplierInvoice("SUP-002", "Before Supplier Update", "REF-002");
        supplierInvoice.getRows().add(invoiceRow("P-SINV-003", "Initial row", new BigDecimal("100.00"), 1, 4010));
        SSPurchaseContext.addSupplierInvoice(supplierInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierInvoice> fetched = SSPurchaseContext.getSupplierInvoice(supplierInvoice);
        assertThat(fetched).isPresent();

        SSSupplierInvoice updatedSupplierInvoice = fetched.get();
        updatedSupplierInvoice.setSupplierName("After Supplier Update");
        updatedSupplierInvoice.setReferencenumber("REF-UPDATED");
        updatedSupplierInvoice.setLocalDueDate(LocalDate.of(2025, 9, 1));
        updatedSupplierInvoice.setBGCEntered(true);
        updatedSupplierInvoice.setStockInfluencing(false);
        updatedSupplierInvoice.getRows().clear();
        updatedSupplierInvoice.getRows().add(
                invoiceRow("P-SINV-004", "Updated row", new BigDecimal("750.00"), 3, 4041));
        SSPurchaseContext.updateSupplierInvoice(updatedSupplierInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplierInvoice> updated = SSPurchaseContext.getSupplierInvoice(supplierInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After Supplier Update");
        assertThat(updated.get().getReferencenumber()).isEqualTo("REF-UPDATED");
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(updated.get().isBGCEntered()).isTrue();
        assertThat(updated.get().isStockInfluencing()).isFalse();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer supplierInvoiceNumber = updated.get().getNumber();
        SSPurchaseContext.deleteSupplierInvoice(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSSupplierInvoice> all = SSPurchaseContext.getSupplierInvoices();
        assertThat(all).extracting(SSSupplierInvoice::getNumber).doesNotContain(supplierInvoiceNumber);
    }

    private static SSSupplierInvoice supplierInvoice(String supplierNr, String supplierName, String referenceNumber) {
        SSSupplierInvoice supplierInvoice = new SSSupplierInvoice();
        supplierInvoice.setSupplierNr(supplierNr);
        supplierInvoice.setSupplierName(supplierName);
        supplierInvoice.setReferencenumber(referenceNumber);
        supplierInvoice.setLocalDate(LocalDate.of(2025, 7, 10));
        supplierInvoice.setLocalDueDate(LocalDate.of(2025, 8, 9));
        supplierInvoice.setCurrencyRate(new BigDecimal("10.00"));
        supplierInvoice.setTaxSum(new BigDecimal("100.00"));
        supplierInvoice.setRoundingSum(new BigDecimal("0.25"));
        supplierInvoice.setEntered(false);
        supplierInvoice.setStockInfluencing(true);
        supplierInvoice.setBGCEntered(false);
        return supplierInvoice;
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



