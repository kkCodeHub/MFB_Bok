package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technical verification of invoice lifecycle policy rules.
 *
 * <p>Covers lock reasons for edit/cancel/delete/uncancel and side-flow permissions
 * for journal, reminder, interest, inpayment, crediting, print and e-mail.</p>
 */
@Tag("integration")
class SSInvoiceActionPolicyV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_invoice_policy";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Invoice Policy Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Invoice Policy Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
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
    void clearCaches() {
        SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void printedInvoiceCannotBeEdited() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(true);

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();
    }

    @Test
    void enteredInvoiceCannotBeEdited() {
        SSInvoice invoice = new SSInvoice();
        invoice.setEntered(true);

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();
    }

    @Test
    void creditedInvoiceCannotBeEdited() {
        SSInvoice invoice = persistInvoice("POL-CR-001", "Policy Credited Customer");
        SSCreditInvoice creditInvoice = new SSCreditInvoice(invoice);

        Repositories.creditInvoices().add(creditInvoice);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();

        Repositories.creditInvoices().delete(creditInvoice);
        Repositories.invoices().delete(invoice);
    }

    @Test
    void paidInvoiceCannotBeEdited() {
        SSInvoice invoice = persistInvoice("POL-PD-001", "Policy Paid Customer");

        SSInpayment inpayment = new SSInpayment();
        inpayment.setLocalDate(LocalDate.of(2025, 8, 1));
        SSInpaymentRow row = new SSInpaymentRow();
        row.setInvoiceNr(invoice.getNumber());
        row.setValue(new BigDecimal("100.00"));
        row.setCurrencyRate(BigDecimal.ONE);
        inpayment.getRows().add(row);

        Repositories.inpayments().add(inpayment);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();

        Repositories.inpayments().delete(inpayment);
        Repositories.invoices().delete(invoice);
    }

    @Test
    void cancelledInvoiceCannotBeEdited() {
        SSInvoice invoice = new SSInvoice();
        invoice.setCancelled();

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();
    }

    @Test
    void cancelledInvoiceCannotBeCancelledAgain() {
        SSInvoice invoice = new SSInvoice();
        invoice.setCancelled();

        assertThat(SSInvoiceActionPolicy.canCancel(invoice)).isFalse();
    }

    @Test
    void deletingLastInvoiceRequiresUnlockedState() {
        SSInvoice first = invoiceTemplate("POL-DL-001", "Delete Rule 1");
        first.setNumber(1000);
        SSInvoice last = invoiceTemplate("POL-DL-002", "Delete Rule 2");
        last.setNumber(1001);
        List<SSInvoice> series = List.of(first, last);

        assertThat(SSInvoiceActionPolicy.canDeletePhysically(last, series)).isTrue();

        last.setPrinted(true);
        assertThat(SSInvoiceActionPolicy.canDeletePhysically(last, series)).isFalse();
    }

    @Test
    void deletingNonLastInvoiceIsBlocked() {
        SSInvoice first = invoiceTemplate("POL-DN-001", "Delete Non-last 1");
        first.setNumber(2000);
        SSInvoice last = invoiceTemplate("POL-DN-002", "Delete Non-last 2");
        last.setNumber(2001);
        List<SSInvoice> series = List.of(first, last);

        assertThat(SSInvoiceActionPolicy.canDeletePhysically(first, series)).isFalse();
    }

    @Test
    void cancelledLastInvoiceCanBeUncancelled() {
        SSInvoice first = invoiceTemplate("POL-UL-001", "Uncancel Last 1");
        first.setNumber(3000);
        SSInvoice last = invoiceTemplate("POL-UL-002", "Uncancel Last 2");
        last.setNumber(3001);
        last.setCancelled();
        List<SSInvoice> series = List.of(first, last);

        assertThat(SSInvoiceActionPolicy.canUncancel(last, series)).isTrue();
    }

    @Test
    void cancelledNonLastInvoiceCannotBeUncancelled() {
        SSInvoice first = invoiceTemplate("POL-UN-001", "Uncancel Non-last 1");
        first.setNumber(4000);
        first.setCancelled();
        SSInvoice last = invoiceTemplate("POL-UN-002", "Uncancel Non-last 2");
        last.setNumber(4001);
        List<SSInvoice> series = List.of(first, last);

        assertThat(SSInvoiceActionPolicy.canUncancel(first, series)).isFalse();
    }

    @Test
    void journalExcludesCancelledInvoices() {
        SSInvoice allowed = new SSInvoice();
        allowed.setEntered(false);

        SSInvoice cancelled = new SSInvoice();
        cancelled.setCancelled();
        cancelled.setEntered(false);

        assertThat(SSInvoiceActionPolicy.canPostToJournal(allowed)).isTrue();
        assertThat(SSInvoiceActionPolicy.canPostToJournal(cancelled)).isFalse();
    }

    @Test
    void reminderAndInterestExcludeCancelledInvoices() {
        SSInvoice cancelled = new SSInvoice();
        cancelled.setCancelled();

        assertThat(SSInvoiceActionPolicy.canSelectForReminder(cancelled)).isFalse();
        assertThat(SSInvoiceActionPolicy.canSelectForInterestInvoicing(cancelled)).isFalse();
    }

    @Test
    void cancelledInvoiceIsBlockedInSideFlows() {
        SSInvoice cancelled = new SSInvoice();
        cancelled.setCancelled();

        assertThat(SSInvoiceActionPolicy.canRegisterInpayment(cancelled)).isFalse();
        assertThat(SSInvoiceActionPolicy.canCreateCreditInvoice(cancelled)).isFalse();
        assertThat(SSInvoiceActionPolicy.canPrint(cancelled)).isFalse();
        assertThat(SSInvoiceActionPolicy.canSendByEmail(cancelled)).isFalse();
    }

    private static SSInvoice persistInvoice(String customerNr, String customerName) {
        SSInvoice invoice = invoiceTemplate(customerNr, customerName);
        invoice.getRows().add(invoiceRow("P-" + customerNr, "Policy row", new BigDecimal("500.00"), 1, 3010));
        Repositories.invoices().add(invoice);
        return invoice;
    }

    private static SSInvoice invoiceTemplate(String customerNr, String customerName) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerName);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setType(SSInvoiceType.CASH);
        return invoice;
    }

    private static SSSaleRow invoiceRow(String productNr, String description, BigDecimal unitPrice, int quantity,
                                        int accountNumber) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(accountNumber);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        SSNewCompany company = new SSNewCompany();
        company.setName(name);
        SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}
