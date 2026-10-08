package se.swedsoft.bookkeeping.print.report.journals;

import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
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

class SSInvoicejournalPrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:invoice-journal-" + System.nanoTime() + ";sql.syntax_mys=true");
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
    void cancelledInvoiceShowsVoucherRowsAndZeroTotals() throws Exception {
        SSInvoice invoice = invoice();

        SSDefaultTableModel<?> model = new TestInvoicejournalPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(0, 9)).isEqualTo(Boolean.TRUE);

        List<JournalRow> rows = rows((SSDefaultJasperDataSource) model.getValueAt(0, 10));
        assertThat(rows).isNotEmpty();
        assertThat(rows).allMatch(row -> Boolean.TRUE.equals(row.cancelled()));
        assertThat(rows.stream().map(JournalRow::account)).contains(3010);
    }

    @Test
    void cancelledCreditInvoiceShowsVoucherRowsAndZeroTotals() throws Exception {
        SSCreditInvoice invoice = creditInvoice();

        SSDefaultTableModel<?> model = new TestInvoicejournalPrinter(
                new ArrayList<>(List.of(invoice))).exposeModel();

        assertThat((BigDecimal) model.getValueAt(0, 6)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 7)).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) model.getValueAt(0, 8)).isEqualByComparingTo("0.00");
        assertThat(model.getValueAt(0, 9)).isEqualTo(Boolean.TRUE);

        List<JournalRow> rows = rows((SSDefaultJasperDataSource) model.getValueAt(0, 10));
        assertThat(rows).isNotEmpty();
        assertThat(rows).allMatch(row -> Boolean.TRUE.equals(row.cancelled()));
        assertThat(rows.stream().map(JournalRow::account)).contains(3010);
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

    private static SSAccountPlan accountPlan() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.addAccount(account(1510, "Kundfordringar"));
        plan.addAccount(account(2611, "Utgående moms"));
        plan.addAccount(account(3010, "Försäljning"));
        plan.addAccount(account(3740, "Oresutjamning"));
        return plan;
    }

    private static SSAccount account(int number, String description) {
        SSAccount account = new SSAccount(number);
        account.setDescription(description);
        return account;
    }

    private static SSInvoice invoice() {
        SSInvoice invoice = new SSInvoice();
        invoice.setNumber(11);
        invoice.setLocalDate(LocalDate.of(2025, 1, 15));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setRows(List.of(row()));
        invoice.setCancelled(true);
        invoice.setVoucher(new SSVoucher(11, true));
        return invoice;
    }

    private static SSCreditInvoice creditInvoice() {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setNumber(12);
        invoice.setLocalDate(LocalDate.of(2025, 1, 16));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setRows(List.of(row()));
        invoice.setCancelled(true);
        invoice.setVoucher(new SSVoucher(12, true));
        return invoice;
    }

    private static SSSaleRow row() {
        SSSaleRow row = new SSSaleRow();
        row.setDescription("Test row");
        row.setQuantity(10);
        row.setUnitprice(new BigDecimal("1000.00"));
        row.setAccountNr(3010);
        return row;
    }

    private record JournalRow(Integer account, BigDecimal debet, BigDecimal credet, Boolean cancelled) {}

    private static class TestInvoicejournalPrinter extends SSInvoicejournalPrinter {
        private TestInvoicejournalPrinter(List<SSInvoice> invoices) {
            super(invoices, 1, LocalDate.of(2025, 1, 31));
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }
    }
}
