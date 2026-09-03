package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.data.system.SSInvoiceActionPolicy;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technical verification of invoice number-series routines.
 *
 * <p>Covers physical delete sequence for highest invoice, exact counter decrement,
 * next-invoice number reuse/continuation and uncancel sequence without number collisions.</p>
 */
@Tag("integration")
class SSInvoiceNumberSeriesV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_invoice_number_series";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Invoice Number Series Test AB");
        SSCompanyYearContext.addCompany(company);
        SSDB.getInstance().setCurrentCompany(company);

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
    void deletingLastInvoiceAndDecrementingCounterReusesNumberForNextInvoice() {
        SSInvoice first = persistInvoice("SER-DEL-001", "Series Delete 1");
        SSInvoice last = persistInvoice("SER-DEL-002", "Series Delete 2");

        int counterBefore = SSCompanyYearContext.getCurrentCompany().getAutoIncrement().getNumber("invoice");
        SSCompanyYearContext.getCurrentCompany().getAutoIncrement().setNumber("invoice", last.getNumber());
        SSCompanyYearContext.updateCompany(SSCompanyYearContext.getCurrentCompany());

        assertThat(SSInvoiceActionPolicy.canDeletePhysically(last, SSSalesContext.getInvoices())).isTrue();

        applyPhysicalDeleteCounterStep();
        SSSalesContext.deleteInvoice(last);

        int counterAfter = SSCompanyYearContext.getCurrentCompany().getAutoIncrement().getNumber("invoice");
        assertThat(counterAfter).isEqualTo(last.getNumber() - 1);
        assertThat(counterAfter).isEqualTo(Math.max(counterBefore, last.getNumber()) - 1);

        SSInvoice next = persistInvoice("SER-DEL-003", "Series Delete 3");
        assertThat(next.getNumber()).isEqualTo(last.getNumber());

        cleanupInvoices(next, first);
    }

    @Test
    void uncancellingLastCancelledInvoiceDoesNotCreateNumberCollision() {
        SSInvoice first = persistInvoice("SER-UNC-001", "Series Uncancel 1");
        SSInvoice last = persistInvoice("SER-UNC-002", "Series Uncancel 2");

        last.setCancelled();
        SSSalesContext.updateInvoice(last);
        SSEventTriggerSyncContext.clearCachedLists();

        assertThat(SSInvoiceActionPolicy.canUncancel(last, SSSalesContext.getInvoices())).isTrue();

        last.clearCancelled();
        SSSalesContext.updateInvoice(last);
        SSEventTriggerSyncContext.clearCachedLists();

        SSInvoice afterUncancel = persistInvoice("SER-UNC-003", "Series Uncancel 3");
        assertThat(afterUncancel.getNumber()).isGreaterThan(last.getNumber());

        List<SSInvoice> all = SSSalesContext.getInvoices();
        Set<Integer> numbers = all.stream()
                .map(SSInvoice::getNumber)
                .collect(Collectors.toSet());
        assertThat(numbers.size()).isEqualTo(all.size());

        cleanupInvoices(afterUncancel, last, first);
    }

    private static void applyPhysicalDeleteCounterStep() {
        SSNewCompany company = SSCompanyYearContext.getCurrentCompany();
        int current = company.getAutoIncrement().getNumber("invoice");
        company.getAutoIncrement().setNumber("invoice", Math.max(0, current - 1));
        SSCompanyYearContext.updateCompany(company);
    }

    private static SSInvoice persistInvoice(String customerNr, String customerName) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerName);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        invoice.setCurrencyRate(BigDecimal.ONE);
        invoice.setType(SSInvoiceType.CASH);
        invoice.getRows().add(invoiceRow("P-" + customerNr));
        SSSalesContext.addInvoice(invoice);
        return invoice;
    }

    private static SSSaleRow invoiceRow(String productNr) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription("Series test row");
        row.setUnitprice(new BigDecimal("100.00"));
        row.setQuantity(1);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(3010);
        return row;
    }

    private static void cleanupInvoices(SSInvoice... invoices) {
        for (SSInvoice invoice : invoices) {
            SSSalesContext.deleteInvoice(invoice);
        }
    }
}
