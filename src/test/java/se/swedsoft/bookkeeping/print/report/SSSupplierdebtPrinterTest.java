package se.swedsoft.bookkeeping.print.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierdebtPrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:supplier-debt-report-" + System.nanoTime() + ";sql.syntax_mys=true");
        Integer companyId = SSV2DatabaseFixture.createCompany(connection, "Testbolaget");
        SSV2DatabaseFixture.setCurrentCompany(companyId, "Testbolaget");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterEach
    void tearDown() throws Exception {
        SSV2DatabaseFixture.clearState();
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @Test
    void cancelledSupplierInvoicesAreExcludedFromSupplierDebtReportModel() {
        SSSupplierInvoice activeInvoice = supplierInvoice("SUP-DEBT-001", false);
        SSSupplierInvoice cancelledInvoice = supplierInvoice("SUP-DEBT-002", true);

        SSPurchaseContext.addSupplierInvoice(activeInvoice);
        SSPurchaseContext.addSupplierInvoice(cancelledInvoice);

        SSDefaultTableModel<?> model = new TestSupplierdebtPrinter(LocalDate.of(2025, 7, 31)).exposeModel();

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 0)).isEqualTo(activeInvoice.getNumber());
        assertThat(model.getValueAt(0, 1)).isEqualTo("SUP-DEBT-001");

        SSPurchaseContext.deleteSupplierInvoice(cancelledInvoice);
        SSPurchaseContext.deleteSupplierInvoice(activeInvoice);
    }

    private static SSSupplierInvoice supplierInvoice(String supplierNr, boolean cancelled) {
        SSSupplierInvoice supplierInvoice = new SSSupplierInvoice();
        supplierInvoice.setSupplierNr(supplierNr);
        supplierInvoice.setSupplierName("Supplier Debt AB");
        supplierInvoice.setReferencenumber("REF-" + supplierNr);
        supplierInvoice.setLocalDate(LocalDate.of(2025, 7, 10));
        supplierInvoice.setLocalDueDate(LocalDate.of(2025, 8, 9));
        supplierInvoice.setCurrencyRate(BigDecimal.ONE);
        supplierInvoice.setTaxSum(BigDecimal.ZERO);
        supplierInvoice.setRoundingSum(BigDecimal.ZERO);
        supplierInvoice.setEntered(false);
        supplierInvoice.setCancelled(cancelled);
        supplierInvoice.setStockInfluencing(true);
        supplierInvoice.setBGCEntered(false);
        supplierInvoice.getRows().add(invoiceRow("P-" + supplierNr, new BigDecimal("100.00"), 1));
        return supplierInvoice;
    }

    private static SSSupplierInvoiceRow invoiceRow(String productNr, BigDecimal unitPrice, int quantity) {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProductNr(productNr);
        row.setDescription("Supplier debt row");
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setAccountNr(4010);
        return row;
    }

    private static class TestSupplierdebtPrinter extends SSSupplierdebtPrinter {
        private TestSupplierdebtPrinter(LocalDate date) {
            super(date);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
