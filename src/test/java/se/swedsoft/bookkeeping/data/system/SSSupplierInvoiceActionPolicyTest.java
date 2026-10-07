package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSSupplierInvoiceActionPolicyTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_supplier_delete";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("Supplier Delete Policy Test AB");
        SSCompanyYearContext.addCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @AfterAll
    static void teardown() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void clearCaches() {
        SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void supplierInvoiceCanBeDeletedOnlyWhenItIsLastInSeries() {
        SSSupplierInvoice first = supplierInvoice(1001, false);
        SSSupplierInvoice last = supplierInvoice(1002, false);

        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(last, List.of(first, last))).isTrue();
        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(first, List.of(first, last))).isFalse();
    }

    @Test
    void supplierInvoiceCannotBeDeletedWhenEntered() {
        SSSupplierInvoice invoice = supplierInvoice(2001, true);

        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(invoice, List.of(invoice))).isFalse();
        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isFalse();
    }

    @Test
    void supplierCreditInvoiceUsesTheSameDeleteRule() {
        SSSupplierCreditInvoice first = new SSSupplierCreditInvoice();
        first.setNumber(3001);
        SSSupplierCreditInvoice last = new SSSupplierCreditInvoice();
        last.setNumber(3002);

        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(last, List.of(first, last))).isTrue();
        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(first, List.of(first, last))).isFalse();
    }

    @Test
    void supplierCreditInvoiceCannotBeDeletedWhenEntered() {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice();
        invoice.setNumber(4001);
        invoice.setEntered(true);

        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(invoice, List.of(invoice))).isFalse();
        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isFalse();
    }

    @Test
    void supplierCreditInvoiceCanBeCancelledWhenUnlocked() {
        SSSupplierCreditInvoice invoice = new SSSupplierCreditInvoice();
        invoice.setNumber(4101);

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isTrue();
        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(invoice, List.of(invoice))).isTrue();
    }

    @Test
    void supplierCreditInvoiceCanBeUncancelledOnlyWhenLastInSeries() {
        SSSupplierCreditInvoice first = new SSSupplierCreditInvoice();
        first.setNumber(4201);
        first.setCancelled();

        SSSupplierCreditInvoice last = new SSSupplierCreditInvoice();
        last.setNumber(4202);
        last.setCancelled();

        assertThat(SSSupplierInvoiceActionPolicy.canUncancel(last, List.of(first, last))).isTrue();
        assertThat(SSSupplierInvoiceActionPolicy.canUncancel(first, List.of(first, last))).isFalse();
    }

    @Test
    void supplierInvoiceCanBeCancelledWhenUnlocked() {
        SSSupplierInvoice invoice = supplierInvoice(5001, false);

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isTrue();
        assertThat(SSSupplierInvoiceActionPolicy.canDeletePhysically(invoice, List.of(invoice))).isTrue();
    }

    @Test
    void cancelledSupplierInvoiceCannotBeSelectedForCreditCreation() {
        SSSupplierInvoice invoice = supplierInvoice(5101, false);
        invoice.setCancelled();

        assertThat(SSSupplierInvoiceActionPolicy.canCreateCreditInvoice(invoice)).isFalse();
    }

    @Test
    void creditedSupplierInvoiceCannotBeCancelled() {
        SSSupplierInvoice invoice = supplierInvoice(6001, false);
        Repositories.supplierInvoices().add(invoice);

        SSSupplierCreditInvoice creditInvoice = creditedSupplierInvoice(invoice, 6002);
        Repositories.supplierCreditInvoices().add(creditInvoice);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isFalse();

        Repositories.supplierCreditInvoices().delete(creditInvoice);
        Repositories.supplierInvoices().delete(invoice);
    }

    @Test
    void paidSupplierInvoiceCannotBeCancelled() {
        SSSupplierInvoice invoice = supplierInvoice(7001, false);
        Repositories.supplierInvoices().add(invoice);

        SSOutpayment outpayment = paidSupplierInvoice(invoice);
        SSPaymentContext.addOutpayment(outpayment);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isFalse();

        SSPaymentContext.deleteOutpayment(outpayment);
        Repositories.supplierInvoices().delete(invoice);
    }

    @Test
    void supplierCreditInvoiceCanBeCancelledEvenWhenCreditedByAnotherCreditInvoice() {
        SSSupplierCreditInvoice invoice = unlockedSupplierCreditInvoice(8101);
        Repositories.supplierCreditInvoices().add(invoice);

        SSSupplierCreditInvoice creditingInvoice = unlockedSupplierCreditInvoice(8102);
        creditingInvoice.setCreditingNr(invoice.getNumber());
        Repositories.supplierCreditInvoices().add(creditingInvoice);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isTrue();

        Repositories.supplierCreditInvoices().delete(creditingInvoice);
        Repositories.supplierCreditInvoices().delete(invoice);
    }

    @Test
    void cancelledSupplierCreditInvoiceDoesNotBlockSupplierInvoiceCancellation() {
        SSSupplierInvoice invoice = supplierInvoice(8201, false);
        Repositories.supplierInvoices().add(invoice);

        SSSupplierCreditInvoice cancelledCredit = creditedSupplierInvoice(invoice, 8202);
        cancelledCredit.setCancelled();
        Repositories.supplierCreditInvoices().add(cancelledCredit);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSSupplierInvoiceActionPolicy.canCancel(invoice)).isTrue();

        Repositories.supplierCreditInvoices().delete(cancelledCredit);
        Repositories.supplierInvoices().delete(invoice);
    }

    private static SSSupplierInvoice supplierInvoice(int number, boolean entered) {
        SSSupplierInvoice invoice = new SSSupplierInvoice(true);
        invoice.setNumber(number);
        invoice.setEntered(entered);
        return invoice;
    }

    private static SSSupplierCreditInvoice creditedSupplierInvoice(SSSupplierInvoice invoice, int number) {
        SSSupplierCreditInvoice creditInvoice = new SSSupplierCreditInvoice();
        creditInvoice.setNumber(number);
        creditInvoice.setCreditingNr(invoice.getNumber());
        creditInvoice.setSupplierNr("SUP-1");
        creditInvoice.setSupplierName("Supplier");
        creditInvoice.setLocalDate(LocalDate.of(2025, 7, 1));
        creditInvoice.setLocalDueDate(LocalDate.of(2025, 7, 31));
        creditInvoice.setCurrencyRate(BigDecimal.ONE);
        creditInvoice.setTaxSum(BigDecimal.ZERO);
        creditInvoice.setRoundingSum(BigDecimal.ZERO);
        creditInvoice.setEntered(true);
        creditInvoice.getRows().add(creditedRow());
        return creditInvoice;
    }

    private static SSSupplierCreditInvoice unlockedSupplierCreditInvoice(int number) {
        SSSupplierCreditInvoice creditInvoice = new SSSupplierCreditInvoice();
        creditInvoice.setNumber(number);
        creditInvoice.setSupplierNr("SUP-1");
        creditInvoice.setSupplierName("Supplier");
        creditInvoice.setLocalDate(LocalDate.of(2025, 7, 1));
        creditInvoice.setLocalDueDate(LocalDate.of(2025, 7, 31));
        creditInvoice.setCurrencyRate(BigDecimal.ONE);
        creditInvoice.setTaxSum(BigDecimal.ZERO);
        creditInvoice.setRoundingSum(BigDecimal.ZERO);
        creditInvoice.setEntered(false);
        creditInvoice.getRows().add(creditedRow());
        return creditInvoice;
    }

    private static SSSupplierInvoiceRow creditedRow() {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setDescription("Credit row");
        row.setProductNr("P-1");
        row.setUnitprice(new BigDecimal("100.00"));
        row.setQuantity(1);
        row.setAccountNr(4010);
        return row;
    }

    private static SSOutpayment paidSupplierInvoice(SSSupplierInvoice invoice) {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setNumber(8001);
        outpayment.setLocalDate(LocalDate.of(2025, 7, 2));
        outpayment.setText("Payment");
        outpayment.setEntered(true);

        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setInvoiceNr(invoice.getNumber());
        row.setValue(new BigDecimal("100.00"));
        row.setCurrencyRate(BigDecimal.ONE);
        outpayment.getRows().add(row);
        return outpayment;
    }
}
