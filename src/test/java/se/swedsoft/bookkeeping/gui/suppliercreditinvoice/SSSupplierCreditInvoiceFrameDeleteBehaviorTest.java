package se.swedsoft.bookkeeping.gui.suppliercreditinvoice;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSSupplierCreditInvoiceFrameDeleteBehaviorTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_supplier_credit_delete_frame";
    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("Supplier Credit Frame Test AB");
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
    void shouldResolvePhysicalDeleteWhenInvoiceIsLastAndUnlocked() {
        SSSupplierCreditInvoice first = creditInvoice(1001);
        SSSupplierCreditInvoice last = creditInvoice(1002);

        assertThat(SSSupplierCreditInvoiceFrame.resolveDeleteAction(last, List.of(first, last)))
                .isEqualTo(SSSupplierCreditInvoiceFrame.DeleteAction.DELETE_PHYSICALLY);
    }

    @Test
    void shouldResolveUncancelForCancelledLastInvoice() {
        SSSupplierCreditInvoice first = creditInvoice(2001);
        SSSupplierCreditInvoice last = creditInvoice(2002);
        last.setCancelled();

        assertThat(SSSupplierCreditInvoiceFrame.resolveDeleteAction(last, List.of(first, last)))
                .isEqualTo(SSSupplierCreditInvoiceFrame.DeleteAction.UNCANCEL);
    }

    @Test
    void shouldResolveCancelForUnlockedNonLastInvoice() {
        SSSupplierCreditInvoice first = creditInvoice(3001);
        SSSupplierCreditInvoice last = creditInvoice(3002);

        assertThat(SSSupplierCreditInvoiceFrame.resolveDeleteAction(first, List.of(first, last)))
                .isEqualTo(SSSupplierCreditInvoiceFrame.DeleteAction.CANCEL);
    }

    @Test
    void shouldResolveNoneForEnteredInvoice() {
        SSSupplierCreditInvoice invoice = creditInvoice(4001);
        invoice.setEntered(true);

        assertThat(SSSupplierCreditInvoiceFrame.resolveDeleteAction(invoice, List.of(invoice)))
                .isEqualTo(SSSupplierCreditInvoiceFrame.DeleteAction.NONE);
    }

    private static SSSupplierCreditInvoice creditInvoice(int pNumber) {
        SSSupplierCreditInvoice iInvoice = new SSSupplierCreditInvoice();
        iInvoice.setNumber(pNumber);
        return iInvoice;
    }
}
