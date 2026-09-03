package se.swedsoft.bookkeeping.gui.invoice;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSInvoiceActionPolicy;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInvoiceTableModel;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.ImageIcon;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoiceDialogGuiBehaviorTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_gui_invoice_dialog";

    private static Connection connection;

    @org.junit.jupiter.api.BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Invoice Dialog Test AB");
        SSCompanyYearContext.addCompany(company);
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @org.junit.jupiter.api.AfterAll
    static void teardown() throws Exception {
        System.clearProperty("fribok.schema.version");
    }

    @Test
    void lockedInvoiceShouldShowInfoAndOpenReadOnly() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(true);

        assertThat(SSInvoiceDialog.shouldShowEditLockedInfo(invoice)).isTrue();
        assertThat(SSInvoiceDialog.shouldOpenReadOnly(invoice)).isTrue();
    }

    @Test
    void cancelledInvoiceShouldShowCancelledIconInList() {
        SSInvoice invoice = new SSInvoice();
        invoice.setCancelled();

        Object icon = SSInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_DELETE16", SSIcon.IconState.NORMAL));
    }

    @Test
    void printedButNotEnteredInvoiceShouldShowPrintedIconInList() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(true);
        invoice.setEntered(false);

        Object icon = SSInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_PRINTED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void enteredButNotPrintedInvoiceShouldShowEnteredIconInList() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(false);
        invoice.setEntered(true);

        Object icon = SSInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_ENTERED16", SSIcon.IconState.NORMAL));
    }

    @Test
    void printedAndEnteredInvoiceShouldShowPropertiesIconInList() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(true);
        invoice.setEntered(true);

        Object icon = SSInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isSameAs(SSIcon.getIcon("ICON_PROPERTIES16", SSIcon.IconState.NORMAL));
    }

    @Test
    void newInvoiceShouldShowNoIconInList() {
        SSInvoice invoice = new SSInvoice();

        Object icon = SSInvoiceTableModel.COLUMN_PRINTED.getValue(invoice);

        assertThat(icon).isNull();
    }

    @Test
    void lockedInvoiceCanStillOfferPrintAndEmailWhenNotCancelled() {
        SSInvoice invoice = new SSInvoice();
        invoice.setPrinted(true);

        assertThat(SSInvoiceActionPolicy.canEdit(invoice)).isFalse();
        assertThat(SSInvoiceActionPolicy.canPrint(invoice)).isTrue();
        assertThat(SSInvoiceActionPolicy.canSendByEmail(invoice)).isTrue();
    }
}
