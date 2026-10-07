package se.swedsoft.bookkeeping.print.report.journals;

import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.util.SSDefaultJasperDataSource;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSSupplierInvoicejournalPrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:supplier-journal-" + System.nanoTime() + ";sql.syntax_mys=true");
        Integer companyId = SSV2DatabaseFixture.createCompany(connection, "Testbolaget");
        SSV2DatabaseFixture.setCurrentCompany(companyId, "Testbolaget");
        SSNewAccountingYear year = SSV2DatabaseFixture.createAndSetCurrentYear(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        year.setAccountPlan(accountPlan());
        SSAccountingContext.updateAccountingYear(year);
    }

    @AfterEach
    void tearDown() throws Exception {
        SSV2DatabaseFixture.clearState();
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @Test
    void cancelledSupplierInvoiceKeepsGeneratedVoucherRowsWhenReferenceVoucherIsEmpty() throws Exception {
        SSSupplierInvoice invoice = supplierInvoice();

        SSDefaultTableModel<?> model = new TestSupplierInvoicejournalPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(0, 9)).isEqualTo(Boolean.TRUE);

        List<JournalRow> rows = rows((SSDefaultJasperDataSource) model.getValueAt(0, 10));
        assertThat(rowSummaries(rows)).contains(
                "2440|-|1250|true",
                "2641|250|-|true",
                "4010|1000|-|true");
    }

    @Test
    void cancelledSupplierCreditInvoiceKeepsGeneratedVoucherRowsWhenReferenceVoucherIsEmpty() throws Exception {
        SSSupplierCreditInvoice invoice = supplierCreditInvoice();

        SSDefaultTableModel<?> model = new TestSupplierInvoicejournalPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(0, 9)).isEqualTo(Boolean.TRUE);

        List<JournalRow> rows = rows((SSDefaultJasperDataSource) model.getValueAt(0, 10));
        assertThat(rowSummaries(rows)).contains(
                "2440|1250|-|true",
                "2641|-|250|true",
                "4010|-|1000|true");
    }

    private static List<JournalRow> rows(SSDefaultJasperDataSource dataSource) throws Exception {
        JRDesignField accountField = field("row.account");
        JRDesignField debetField = field("row.debet");
        JRDesignField credetField = field("row.credet");
        JRDesignField cancelledField = field("row.cancelled");
        List<JournalRow> rows = new ArrayList<>();
        dataSource.moveFirst();
        while (dataSource.next()) {
            rows.add(new JournalRow(
                    (Integer) dataSource.getFieldValue(accountField),
                    (BigDecimal) dataSource.getFieldValue(debetField),
                    (BigDecimal) dataSource.getFieldValue(credetField),
                    (Boolean) dataSource.getFieldValue(cancelledField)));
        }
        return rows;
    }

    private static JRDesignField field(String name) {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        return field;
    }

    private static List<String> rowSummaries(List<JournalRow> rows) {
        return rows.stream()
                .map(row -> row.account() + "|" + amountText(row.debet()) + "|" + amountText(row.credet()) + "|"
                        + row.cancelled())
                .toList();
    }

    private static String amountText(BigDecimal amount) {
        return amount == null ? "-" : amount.stripTrailingZeros().toPlainString();
    }

    private static SSAccountPlan accountPlan() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.addAccount(account(2440, "Leverantorsskulder"));
        plan.addAccount(account(2641, "Ingaende moms"));
        plan.addAccount(account(4010, "Varuinkop"));
        plan.addAccount(account(3740, "Oresutjamning"));
        return plan;
    }

    private static SSAccount account(int number, String description) {
        SSAccount account = new SSAccount(number);
        account.setDescription(description);
        return account;
    }

    private static SSSupplierInvoice supplierInvoice() {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);
        invoice.setNumber(11);
        invoice.setLocalDate(LocalDate.of(2025, 1, 15));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setTaxSum(new BigDecimal("250.00"));
        invoice.setRows(List.of(row()));
        invoice.setCancelled(true);
        invoice.setVoucher(new SSVoucher(11, true));
        return invoice;
    }

    private static SSSupplierCreditInvoice supplierCreditInvoice() {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice(true);
        invoice.setNumber(12);
        invoice.setLocalDate(LocalDate.of(2025, 1, 16));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setTaxSum(new BigDecimal("250.00"));
        invoice.setRows(List.of(row()));
        invoice.setCancelled(true);
        invoice.setVoucher(new SSVoucher(12, true));
        return invoice;
    }

    private static SSSupplierInvoiceRow row() {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setDescription("Test row");
        row.setQuantity(10);
        row.setUnitprice(new BigDecimal("1000.00"));
        row.setAccountNr(4010);
        return row;
    }

    private record JournalRow(Integer account, BigDecimal debet, BigDecimal credet, Boolean cancelled) {}

    private static class TestSupplierInvoicejournalPrinter extends SSSupplierInvoicejournalPrinter {
        private TestSupplierInvoicejournalPrinter(List<SSSupplierInvoice> invoices) {
            super(invoices, 1, LocalDate.of(2025, 1, 31));
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
