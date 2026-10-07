package se.swedsoft.bookkeeping.print.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.calc.math.SSSupplierInvoiceMath;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSAccountsPayablePrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:accounts-payable-report-" + System.nanoTime() + ";sql.syntax_mys=true");
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
    void cancelledSupplierInvoicesAreExcludedFromAccountsPayableReport() {
        SSSupplier supplier = supplier("SUP-PAY-001", "Supplier Payable AB");
        SSSupplierInvoice activeInvoice = supplierInvoice(1001, supplier, false);
        SSSupplierInvoice cancelledInvoice = supplierInvoice(1002, supplier, true);

        SSPurchaseContext.addSupplier(supplier);
        SSPurchaseContext.addSupplierInvoice(activeInvoice);
        SSPurchaseContext.addSupplierInvoice(cancelledInvoice);

        TestAccountsPayablePrinter printer = new TestAccountsPayablePrinter(LocalDate.of(2025, 7, 31));
        SSDefaultTableModel<?> model = printer.exposeModel();

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 0)).isEqualTo("SUP-PAY-001");
        assertThat((BigDecimal) model.getValueAt(0, 2)).isEqualByComparingTo(SSSupplierInvoiceMath.getTotalSum(activeInvoice));
        assertThat(printer.invoicesForSupplier("SUP-PAY-001"))
                .extracting(SSSupplierInvoice::getNumber)
                .containsExactly(activeInvoice.getNumber());

        SSPurchaseContext.deleteSupplierInvoice(cancelledInvoice);
        SSPurchaseContext.deleteSupplierInvoice(activeInvoice);
        SSPurchaseContext.deleteSupplier(supplier);
    }

    private static SSSupplier supplier(String number, String name) {
        SSSupplier supplier = new SSSupplier();
        supplier.setNumber(number);
        supplier.setName(name);
        return supplier;
    }

    private static SSSupplierInvoice supplierInvoice(Integer invoiceNumber, SSSupplier supplier, boolean cancelled) {
        SSSupplierInvoice supplierInvoice = new SSSupplierInvoice();
        supplierInvoice.setNumber(invoiceNumber);
        supplierInvoice.setSupplierNr(supplier.getNumber());
        supplierInvoice.setSupplierName(supplier.getName());
        supplierInvoice.setReferencenumber("REF-" + invoiceNumber);
        supplierInvoice.setLocalDate(LocalDate.of(2025, 7, 10));
        supplierInvoice.setLocalDueDate(LocalDate.of(2025, 8, 9));
        supplierInvoice.setCurrencyRate(BigDecimal.ONE);
        supplierInvoice.setTaxSum(BigDecimal.ZERO);
        supplierInvoice.setRoundingSum(BigDecimal.ZERO);
        supplierInvoice.setEntered(false);
        supplierInvoice.setCancelled(cancelled);
        supplierInvoice.setStockInfluencing(true);
        supplierInvoice.setBGCEntered(false);
        supplierInvoice.getRows().add(invoiceRow("P-" + invoiceNumber, new BigDecimal("100.00"), 1));
        return supplierInvoice;
    }

    private static SSSupplierInvoiceRow invoiceRow(String productNr, BigDecimal unitPrice, int quantity) {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProductNr(productNr);
        row.setDescription("Accounts payable row");
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setAccountNr(4010);
        return row;
    }

    private static class TestAccountsPayablePrinter extends SSAccountsPayablePrinter {
        private TestAccountsPayablePrinter(LocalDate date) {
            super(date);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }

        private List<SSSupplierInvoice> invoicesForSupplier(String supplierNumber) {
            return iSupplierInvoicesMap.get(supplierNumber);
        }
    }
}
