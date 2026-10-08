package se.swedsoft.bookkeeping.print.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.calc.math.SSCustomerMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSAccountsRecievablePrinterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:accounts-recievable-report-" + System.nanoTime() + ";sql.syntax_mys=true");
        Integer companyId = SSV2DatabaseFixture.createCompany(connection, "Testbolaget");
        SSV2DatabaseFixture.setCurrentCompany(companyId, "Testbolaget");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterEach
    void tearDown() throws Exception {
        SSV2DatabaseFixture.clearState();
        SSCustomerMath.iInvoicesForCustomers = null;
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @Test
    void cancelledInvoicesAreExcludedFromAccountsReceivableTotalsAndList() {
        SSCustomer customer = customer("CUST-REC-001", "Customer Recievable AB");
        SSInvoice activeInvoice = invoice(customer, false, "100.00", LocalDate.of(2025, 7, 10));
        SSInvoice cancelledInvoice = invoice(customer, true, "50.00", LocalDate.of(2025, 7, 11));

        SSSalesContext.addCustomer(customer);
        SSSalesContext.addInvoice(activeInvoice);
        SSSalesContext.addInvoice(cancelledInvoice);

        SSCreditInvoice activeCreditInvoice = creditInvoice(customer, false, activeInvoice.getNumber(), "20.00",
                LocalDate.of(2025, 7, 12));
        SSCreditInvoice cancelledCreditInvoice = creditInvoice(customer, true, activeInvoice.getNumber(), "30.00",
                LocalDate.of(2025, 7, 13));

        SSSalesContext.addCreditInvoice(activeCreditInvoice);
        SSSalesContext.addCreditInvoice(cancelledCreditInvoice);

        SSCustomerMath.iInvoicesForCustomers = null;
        SSCustomerMath.getInvoicesForCustomers();

        TestAccountsRecievablePrinter printer = new TestAccountsRecievablePrinter(
                LocalDate.of(2025, 7, 31), new ArrayList<>(List.of(customer)));
        SSDefaultTableModel<?> model = printer.exposeModel();

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 0)).isEqualTo(customer.getNumber());

        BigDecimal expectedSaldoLocal = SSInvoiceMath.convertToLocal(activeInvoice,
                SSInvoiceMath.getTotalSum(activeInvoice).subtract(SSInvoiceMath.getTotalSum(activeCreditInvoice)));
        assertThat((BigDecimal) model.getValueAt(0, 2)).isEqualByComparingTo(expectedSaldoLocal);
        assertThat(printer.invoicesForCustomer(customer.getNumber()))
                .extracting(SSInvoice::getNumber)
                .containsExactly(activeInvoice.getNumber());

        SSSalesContext.deleteCreditInvoice(cancelledCreditInvoice);
        SSSalesContext.deleteCreditInvoice(activeCreditInvoice);
        SSSalesContext.deleteInvoice(cancelledInvoice);
        SSSalesContext.deleteInvoice(activeInvoice);
        SSSalesContext.deleteCustomer(customer);
    }

    private static SSCustomer customer(String number, String name) {
        SSCustomer customer = new SSCustomer();
        customer.setNumber(number);
        customer.setName(name);
        return customer;
    }

    private static SSInvoice invoice(SSCustomer customer, boolean cancelled, String unitPrice, LocalDate date) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customer.getNumber());
        invoice.setCustomerName(customer.getName());
        invoice.setLocalDate(date);
        invoice.setLocalDueDate(date.plusDays(30));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(unitPrice));
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(SSCustomer customer, boolean cancelled, Integer creditingNr,
                                                 String unitPrice, LocalDate date) {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setCustomerNr(customer.getNumber());
        invoice.setCustomerName(customer.getName());
        invoice.setLocalDate(date);
        invoice.setLocalDueDate(date.plusDays(30));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setCreditingNr(creditingNr);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(unitPrice));
        return invoice;
    }

    private static SSSaleRow row(String unitPrice) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr("P-REC-1");
        row.setDescription("Accounts receivable row");
        row.setQuantity(1);
        row.setUnitprice(new BigDecimal(unitPrice));
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(3010);
        return row;
    }

    private static class TestAccountsRecievablePrinter extends SSAccountsRecievablePrinter {
        private TestAccountsRecievablePrinter(LocalDate date, List<SSCustomer> customers) {
            super(date, customers);
        }

        private SSDefaultTableModel<?> exposeModel() {
            return super.getModel();
        }

        private List<SSInvoice> invoicesForCustomer(String customerNumber) {
            return iCustomerInvoicesMap.get(customerNumber);
        }
    }
}
