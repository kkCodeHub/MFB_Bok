package se.swedsoft.bookkeeping.print.report;

import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.calc.math.SSCreditInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.util.SSDefaultJasperDataSource;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoiceListPrinterTest {
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:invoice-list-report-" + System.nanoTime() + ";sql.syntax_mys=true");
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
    void cancelledCustomerInvoiceShowsZeroCalculatedTotalsAndExcludedFromLocalCurrencyTotals() throws Exception {
        SSInvoice activeInvoice = invoice(1001, false);
        SSInvoice cancelledInvoice = invoice(1002, true);

        SSDefaultTableModel<?> model = new TestInvoiceListPrinter(
                new ArrayList<>(List.of(activeInvoice, cancelledInvoice))).exposeModel();

        int activeRow = rowByNumber(model, 1001);
        int cancelledRow = rowByNumber(model, 1002);

        assertThat((BigDecimal) model.getValueAt(cancelledRow, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 8)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 9)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(cancelledRow, 10)).isEqualTo(true);
        BigDecimal cancelledRowSum = firstRowSum(model, cancelledRow, 4);
        BigDecimal activeRowSum = firstRowSum(model, activeRow, 4);
        assertThat(cancelledRowSum).isEqualByComparingTo(activeRowSum);
        assertThat(cancelledRowSum.signum()).isPositive();

        BigDecimal activeLocalSum = SSInvoiceMath.getTotalSum(activeInvoice).multiply(activeInvoice.getCurrencyRate());
        BigDecimal activeLocalTax = SSInvoiceMath.getTotalTaxSum(activeInvoice).multiply(activeInvoice.getCurrencyRate());

        assertThat((BigDecimal) model.getValueAt(activeRow, 8)).isEqualByComparingTo(activeLocalSum);
        assertThat((BigDecimal) model.getValueAt(activeRow, 9)).isEqualByComparingTo(activeLocalTax);
        assertThat(sumColumn(model, 8)).isEqualByComparingTo(activeLocalSum);
        assertThat(sumColumn(model, 9)).isEqualByComparingTo(activeLocalTax);
    }

    @Test
    void cancelledCustomerCreditInvoiceShowsZeroCalculatedTotalsAndExcludedFromLocalCurrencyTotals() throws Exception {
        SSCreditInvoice activeInvoice = creditInvoice(2001, false);
        SSCreditInvoice cancelledInvoice = creditInvoice(2002, true);

        SSDefaultTableModel<?> model = new TestCreditInvoiceListPrinter(
                new ArrayList<>(List.of(activeInvoice, cancelledInvoice))).exposeModel();

        int activeRow = rowByNumber(model, 2001);
        int cancelledRow = rowByNumber(model, 2002);

        assertThat((BigDecimal) model.getValueAt(cancelledRow, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 8)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(cancelledRow, 9)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(cancelledRow, 10)).isEqualTo(true);
        BigDecimal cancelledRowSum = firstRowSum(model, cancelledRow, 4);
        BigDecimal activeRowSum = firstRowSum(model, activeRow, 4);
        assertThat(cancelledRowSum).isEqualByComparingTo(activeRowSum);
        assertThat(cancelledRowSum.signum()).isPositive();

        BigDecimal activeLocalSum = SSCreditInvoiceMath.getTotalSum(activeInvoice).multiply(activeInvoice.getCurrencyRate());
        BigDecimal activeLocalTax = SSCreditInvoiceMath.getTotalTaxSum(activeInvoice).multiply(activeInvoice.getCurrencyRate());

        assertThat((BigDecimal) model.getValueAt(activeRow, 8)).isEqualByComparingTo(activeLocalSum);
        assertThat((BigDecimal) model.getValueAt(activeRow, 9)).isEqualByComparingTo(activeLocalTax);
        assertThat(sumColumn(model, 8)).isEqualByComparingTo(activeLocalSum);
        assertThat(sumColumn(model, 9)).isEqualByComparingTo(activeLocalTax);
    }

    @Test
    void invoiceListTemplatesContainCancelledMarker() throws Exception {
        assertThat(readResource("reports/report/invoicelist.jrxml"))
                .contains("<field name=\"invoice.cancelled\" class=\"java.lang.Boolean\"/>")
                .contains("<printWhenExpression><![CDATA[$F{invoice.cancelled}]]></printWhenExpression>")
                .contains("<text><![CDATA[M]]></text>");

        assertThat(readResource("reports/report/creditinvoicelist.jrxml"))
                .contains("<field name=\"invoice.cancelled\" class=\"java.lang.Boolean\"/>")
                .contains("<printWhenExpression><![CDATA[$F{invoice.cancelled}]]></printWhenExpression>")
                .contains("<text><![CDATA[M]]></text>");
    }

    private static SSInvoice invoice(int number, boolean cancelled) {
        SSInvoice invoice = new SSInvoice();
        invoice.setNumber(number);
        invoice.setCurrencyRate(new BigDecimal("2.00"));
        invoice.setRows(List.of(rowWithSum(new BigDecimal("100.00"))));
        invoice.setCancelled(cancelled);
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(int number, boolean cancelled) {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setNumber(number);
        invoice.setCurrencyRate(new BigDecimal("2.00"));
        invoice.setRows(List.of(rowWithSum(new BigDecimal("100.00"))));
        invoice.setCancelled(cancelled);
        return invoice;
    }

    private static SSSaleRow rowWithSum(BigDecimal unitPrice) {
        SSSaleRow row = new SSSaleRow();
        row.setDescription("Test row");
        row.setQuantity(1);
        row.setUnitprice(unitPrice);
        return row;
    }

    private static int rowByNumber(SSDefaultTableModel<?> model, int invoiceNumber) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (Integer.valueOf(invoiceNumber).equals(model.getValueAt(i, 0))) {
                return i;
            }
        }
        throw new IllegalStateException("Invoice row not found: " + invoiceNumber);
    }

    private static BigDecimal firstRowSum(SSDefaultTableModel<?> model, int rowIndex, int rowDataSourceColumn)
            throws Exception {
        SSDefaultJasperDataSource dataSource = (SSDefaultJasperDataSource) model.getValueAt(rowIndex, rowDataSourceColumn);
        JRDesignField field = new JRDesignField();
        field.setName("row.sum");

        assertThat(dataSource.next()).isTrue();
        return (BigDecimal) dataSource.getFieldValue(field);
    }

    private static BigDecimal sumColumn(SSDefaultTableModel<?> model, int columnIndex) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < model.getRowCount(); i++) {
            BigDecimal value = (BigDecimal) model.getValueAt(i, columnIndex);
            sum = sum.add(value);
        }
        return sum;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = SSInvoiceListPrinterTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static class TestInvoiceListPrinter extends SSInvoiceListPrinter {
        private TestInvoiceListPrinter(List<SSInvoice> invoices) {
            super(invoices);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }

    private static class TestCreditInvoiceListPrinter extends SSCreditInvoiceListPrinter {
        private TestCreditInvoiceListPrinter(List<SSCreditInvoice> invoices) {
            super(invoices);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
