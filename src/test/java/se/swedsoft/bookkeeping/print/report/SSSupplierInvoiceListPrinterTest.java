package se.swedsoft.bookkeeping.print.report;

import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.util.SSDefaultJasperDataSource;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierInvoiceListPrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:supplier-report-" + System.nanoTime() + ";sql.syntax_mys=true");
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
    void cancelledSupplierInvoiceShowsZeroCalculatedTotalsButKeepsRowAmount() throws Exception {
        SSSupplierInvoice invoice = supplierInvoice(true);

        SSDefaultTableModel<?> model = new TestSupplierInvoiceListPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 5)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 9)).isEqualByComparingTo("0.00");
        assertThat(firstRowSum(model, 0)).isEqualByComparingTo("100.00");
    }

    @Test
    void cancelledSupplierCreditInvoiceShowsZeroCalculatedTotalsButKeepsRowAmount() throws Exception {
        SSSupplierCreditInvoice invoice = supplierCreditInvoice(true);

        SSDefaultTableModel<?> model = new TestSupplierCreditInvoiceListPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 5)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 9)).isEqualByComparingTo("0.00");
        assertThat(firstRowSum(model, 0)).isEqualByComparingTo("100.00");
    }

    private static BigDecimal firstRowSum(SSDefaultTableModel<?> model, int rowIndex) throws Exception {
        SSDefaultJasperDataSource dataSource = (SSDefaultJasperDataSource) model.getValueAt(rowIndex, 7);
        JRDesignField field = new JRDesignField();
        field.setName("row.sum");

        assertThat(dataSource.next()).isTrue();
        return (BigDecimal) dataSource.getFieldValue(field);
    }

    private static SSSupplierInvoice supplierInvoice(boolean cancelled) {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);
        invoice.setCurrencyRate(new BigDecimal("2.00"));
        invoice.setTaxSum(new BigDecimal("25.00"));
        invoice.setRows(List.of(rowWithSum(new BigDecimal("100.00"))));
        invoice.setCancelled(cancelled);
        return invoice;
    }

    private static SSSupplierCreditInvoice supplierCreditInvoice(boolean cancelled) {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice(true);
        invoice.setCurrencyRate(new BigDecimal("2.00"));
        invoice.setTaxSum(new BigDecimal("25.00"));
        invoice.setRows(List.of(rowWithSum(new BigDecimal("100.00"))));
        invoice.setCancelled(cancelled);
        return invoice;
    }

    private static SSSupplierInvoiceRow rowWithSum(BigDecimal sum) {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setDescription("Test row");
        row.setQuantity(10);
        row.setUnitprice(sum);
        return row;
    }

    private static class TestSupplierInvoiceListPrinter extends SSSupplierInvoiceListPrinter {
        private TestSupplierInvoiceListPrinter(List<SSSupplierInvoice> invoices) {
            super(invoices);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }

    private static class TestSupplierCreditInvoiceListPrinter extends SSSupplierCreditInvoiceListPrinter {
        private TestSupplierCreditInvoiceListPrinter(List<SSSupplierCreditInvoice> invoices) {
            super(invoices);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
