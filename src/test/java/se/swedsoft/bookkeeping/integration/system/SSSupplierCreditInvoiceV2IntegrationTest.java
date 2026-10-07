package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSMonth;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
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

    @Test
    void cancellingSupplierCreditInvoiceRecalculatesSupplierInvoiceSaldoCache() {
        SSSupplierInvoice supplierInvoice = supplierInvoice("SUP-SALDO-001", "Supplier Saldo AB", "REF-SALDO-001");
        supplierInvoice.setTaxSum(BigDecimal.ZERO);
        supplierInvoice.setRoundingSum(BigDecimal.ZERO);
        supplierInvoice.setCurrencyRate(BigDecimal.ONE);
        supplierInvoice.getRows().add(invoiceRow("P-SINV-SALDO", "Supplier row", new BigDecimal("100.00"), 1, 4010));
        SSPurchaseContext.addSupplierInvoice(supplierInvoice);

        SSSupplierCreditInvoice supplierCreditInvoice = supplierCreditInvoice(
                "SUP-SALDO-001", "Supplier Saldo AB", "REF-CR-SALDO-001", supplierInvoice.getNumber());
        supplierCreditInvoice.setTaxSum(BigDecimal.ZERO);
        supplierCreditInvoice.setRoundingSum(BigDecimal.ZERO);
        supplierCreditInvoice.setCurrencyRate(BigDecimal.ONE);
        supplierCreditInvoice.getRows().add(
                invoiceRow("P-SCI-SALDO", "Supplier credit row", new BigDecimal("40.00"), 1, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(supplierCreditInvoice);

        SSSupplierInvoiceMath.iSaldoMap = null;
        SSSupplierInvoiceMath.calculateSaldos();
        BigDecimal iSaldoWithActiveCredit = SSSupplierInvoiceMath.getSaldo(supplierInvoice.getNumber());

        supplierCreditInvoice.setCancelled();
        SSPurchaseContext.updateSupplierCreditInvoice(supplierCreditInvoice);

        BigDecimal iSaldoAfterCancel = SSSupplierInvoiceMath.getSaldo(supplierInvoice.getNumber());
        assertThat(iSaldoAfterCancel).isGreaterThan(iSaldoWithActiveCredit);
        assertThat(iSaldoAfterCancel).isEqualByComparingTo(SSSupplierInvoiceMath.getTotalSum(supplierInvoice));

        SSPurchaseContext.deleteSupplierCreditInvoice(supplierCreditInvoice);
        SSPurchaseContext.deleteSupplierInvoice(supplierInvoice);
    }

    @Test
    void monthlyPurchaseValueIgnoresCancelledSupplierInvoicesAndCredits() {
        SSSupplierInvoice activeInvoice = supplierInvoice("SUP-MONTH-001", "Supplier Month AB", "REF-MONTH-001");
        activeInvoice.setTaxSum(BigDecimal.ZERO);
        activeInvoice.setRoundingSum(BigDecimal.ZERO);
        activeInvoice.setCurrencyRate(BigDecimal.ONE);
        activeInvoice.getRows().add(invoiceRow("P-MONTH-INV-A", "Active invoice row", new BigDecimal("100.00"), 10, 4010));
        SSPurchaseContext.addSupplierInvoice(activeInvoice);

        SSSupplierInvoice cancelledInvoice = supplierInvoice("SUP-MONTH-002", "Supplier Month AB", "REF-MONTH-002");
        cancelledInvoice.setTaxSum(BigDecimal.ZERO);
        cancelledInvoice.setRoundingSum(BigDecimal.ZERO);
        cancelledInvoice.setCurrencyRate(BigDecimal.ONE);
        cancelledInvoice.setCancelled();
        cancelledInvoice.getRows().add(invoiceRow("P-MONTH-INV-C", "Cancelled invoice row", new BigDecimal("50.00"), 10, 4010));
        SSPurchaseContext.addSupplierInvoice(cancelledInvoice);

        SSSupplierCreditInvoice activeCreditInvoice = supplierCreditInvoice(
                "SUP-MONTH-001", "Supplier Month AB", "REF-MONTH-CR-001", activeInvoice.getNumber());
        activeCreditInvoice.setTaxSum(BigDecimal.ZERO);
        activeCreditInvoice.setRoundingSum(BigDecimal.ZERO);
        activeCreditInvoice.setCurrencyRate(BigDecimal.ONE);
        activeCreditInvoice.getRows().add(
                invoiceRow("P-MONTH-CR-A", "Active credit row", new BigDecimal("40.00"), 10, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(activeCreditInvoice);

        SSSupplierCreditInvoice cancelledCreditInvoice = supplierCreditInvoice(
                "SUP-MONTH-001", "Supplier Month AB", "REF-MONTH-CR-002", activeInvoice.getNumber());
        cancelledCreditInvoice.setTaxSum(BigDecimal.ZERO);
        cancelledCreditInvoice.setRoundingSum(BigDecimal.ZERO);
        cancelledCreditInvoice.setCurrencyRate(BigDecimal.ONE);
        cancelledCreditInvoice.setCancelled();
        cancelledCreditInvoice.getRows().add(
                invoiceRow("P-MONTH-CR-C", "Cancelled credit row", new BigDecimal("20.00"), 10, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(cancelledCreditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        Double monthlyValue = SSDB.getInstance().getCurrentCompany().getSupplierInvoiceValueForMonth(
                new SSMonth(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31)));

        assertThat(monthlyValue).isEqualTo(60.0d);

        SSPurchaseContext.deleteSupplierCreditInvoice(cancelledCreditInvoice);
        SSPurchaseContext.deleteSupplierCreditInvoice(activeCreditInvoice);
        SSPurchaseContext.deleteSupplierInvoice(cancelledInvoice);
        SSPurchaseContext.deleteSupplierInvoice(activeInvoice);
    }

    @Test
    void supplierDebtIgnoresCancelledSupplierInvoicesAndCredits() {
        SSSupplierInvoice activeInvoice = supplierInvoice("SUP-DEBT-001", "Supplier Debt AB", "REF-DEBT-001");
        activeInvoice.setTaxSum(BigDecimal.ZERO);
        activeInvoice.setRoundingSum(BigDecimal.ZERO);
        activeInvoice.setCurrencyRate(BigDecimal.ONE);
        activeInvoice.getRows().add(invoiceRow("P-DEBT-INV-A", "Active invoice row", new BigDecimal("100.00"), 10, 4010));
        SSPurchaseContext.addSupplierInvoice(activeInvoice);

        SSSupplierInvoice cancelledInvoice = supplierInvoice("SUP-DEBT-002", "Supplier Debt AB", "REF-DEBT-002");
        cancelledInvoice.setTaxSum(BigDecimal.ZERO);
        cancelledInvoice.setRoundingSum(BigDecimal.ZERO);
        cancelledInvoice.setCurrencyRate(BigDecimal.ONE);
        cancelledInvoice.setCancelled();
        cancelledInvoice.getRows().add(invoiceRow("P-DEBT-INV-C", "Cancelled invoice row", new BigDecimal("50.00"), 10, 4010));
        SSPurchaseContext.addSupplierInvoice(cancelledInvoice);

        SSSupplierCreditInvoice activeCreditInvoice = supplierCreditInvoice(
                "SUP-DEBT-001", "Supplier Debt AB", "REF-DEBT-CR-001", activeInvoice.getNumber());
        activeCreditInvoice.setTaxSum(BigDecimal.ZERO);
        activeCreditInvoice.setRoundingSum(BigDecimal.ZERO);
        activeCreditInvoice.setCurrencyRate(BigDecimal.ONE);
        activeCreditInvoice.getRows().add(
                invoiceRow("P-DEBT-CR-A", "Active credit row", new BigDecimal("40.00"), 10, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(activeCreditInvoice);

        SSSupplierCreditInvoice cancelledCreditInvoice = supplierCreditInvoice(
                "SUP-DEBT-001", "Supplier Debt AB", "REF-DEBT-CR-002", activeInvoice.getNumber());
        cancelledCreditInvoice.setTaxSum(BigDecimal.ZERO);
        cancelledCreditInvoice.setRoundingSum(BigDecimal.ZERO);
        cancelledCreditInvoice.setCurrencyRate(BigDecimal.ONE);
        cancelledCreditInvoice.setCancelled();
        cancelledCreditInvoice.getRows().add(
                invoiceRow("P-DEBT-CR-C", "Cancelled credit row", new BigDecimal("20.00"), 10, 4010));
        SSPurchaseContext.addSupplierCreditInvoice(cancelledCreditInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSSupplierInvoiceMath.getSaldo(activeInvoice, LocalDate.of(2025, 7, 31)))
                .isEqualByComparingTo("60.00");
        assertThat(SSSupplierInvoiceMath.getSaldo(cancelledInvoice, LocalDate.of(2025, 7, 31)))
                .isEqualByComparingTo("0.00");
        assertThat(SSSupplierInvoiceMath.getSaldo(
                List.of(activeInvoice, cancelledInvoice), LocalDate.of(2025, 7, 31)))
                .containsOnlyKeys(activeInvoice);

        SSPurchaseContext.deleteSupplierCreditInvoice(cancelledCreditInvoice);
        SSPurchaseContext.deleteSupplierCreditInvoice(activeCreditInvoice);
        SSPurchaseContext.deleteSupplierInvoice(cancelledInvoice);
        SSPurchaseContext.deleteSupplierInvoice(activeInvoice);
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
