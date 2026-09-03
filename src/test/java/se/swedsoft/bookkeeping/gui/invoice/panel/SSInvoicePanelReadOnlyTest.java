package se.swedsoft.bookkeeping.gui.invoice.panel;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoicePanelReadOnlyTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_gui_invoice_panel";

    private static Connection connection;

    @org.junit.jupiter.api.BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Invoice Panel Test AB");
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
    void readOnlyModeDisablesOkAndKeepsCancelEnabledAndLocksStatusFields() throws Exception {
        SSInvoicePanel panel = createPanelOnEdt();
        try {
            runOnEdt(() -> panel.setReadOnlyMode(true));

            SSButtonPanel buttonPanel = getField(panel, "iButtonPanel", SSButtonPanel.class);
            JCheckBox entered = getField(panel, "iEntered", JCheckBox.class);
            JCheckBox printed = getField(panel, "iPrinted", JCheckBox.class);

            assertThat(buttonPanel.getOkButton().isEnabled()).isFalse();
            assertThat(buttonPanel.getCancelButton().isEnabled()).isTrue();
            assertThat(entered.isEnabled()).isFalse();
            assertThat(printed.isEnabled()).isFalse();
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    void statusCheckboxesStayDisabledEvenInEditableMode() throws Exception {
        SSInvoicePanel panel = createPanelOnEdt();
        try {
            runOnEdt(() -> panel.setReadOnlyMode(false));

            JCheckBox entered = getField(panel, "iEntered", JCheckBox.class);
            JCheckBox printed = getField(panel, "iPrinted", JCheckBox.class);

            assertThat(entered.isEnabled()).isFalse();
            assertThat(printed.isEnabled()).isFalse();
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    private static SSInvoicePanel createPanelOnEdt() throws Exception {
        final SSInvoicePanel[] holder = new SSInvoicePanel[1];
        runOnEdt(() -> holder[0] = new SSInvoicePanel(null));
        return holder[0];
    }

    private static void runOnEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
