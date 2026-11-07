package se.swedsoft.bookkeeping.gui.invoice.panel;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.gui.util.SSInvoiceTextFileOptions;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;

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

    @Test
    void subtextAndSwishFieldsCanBeEditedBeforeSave() throws Exception {
        SSInvoicePanel panel = createPanelOnEdt();
        try {
            runOnEdt(() -> panel.setInvoice(new SSInvoice()));

            JCheckBox printSwish = getField(panel, "iPrintSwish", JCheckBox.class);
            JCheckBox printSubtext = getField(panel, "iPrintSubtext", JCheckBox.class);
            JComboBox<String> fileSubtext = getField(panel, "iFileSubtext", JComboBox.class);
            JComboBox<Integer> subtextSize = getField(panel, "iSubtextSize", JComboBox.class);
            String selectedFileSubtext = fileSubtext.getItemCount() > 0
                    ? fileSubtext.getItemAt(0)
                    : null;

            runOnEdt(() -> {
                printSwish.setSelected(true);
                printSubtext.setSelected(true);
                if (selectedFileSubtext != null) {
                    fileSubtext.setSelectedItem(selectedFileSubtext);
                }
                subtextSize.setSelectedItem(19);
            });

            SSInvoice[] holder = new SSInvoice[1];
            runOnEdt(() -> holder[0] = panel.getInvoice());

            assertThat(holder[0].isPrintSwish()).isTrue();
            assertThat(holder[0].isPrintSubtext()).isTrue();
            assertThat(holder[0].getFileSubtext()).isEqualTo(selectedFileSubtext);
            assertThat(holder[0].getSubtextSize()).isEqualTo(19);
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    void selectedCustomerSubtextSizeIsPreselectedAndOtherCustomerValuesAreSelectable() throws Exception {
        SSInvoicePanel panel = createPanelOnEdt();
        try {
            SSCustomer customer = new SSCustomer();
            customer.setNumber("C-200");
            customer.setName("Kund 200");
            customer.setSubtextSize(10);

            SSInvoice invoice = new SSInvoice();
            invoice.setCustomer(customer);

            runOnEdt(() -> panel.setInvoice(invoice));

            JComboBox<Integer> subtextSize = getField(panel, "iSubtextSize", JComboBox.class);
            assertThat(subtextSize.getItemCount()).isEqualTo(4);
            assertThat(subtextSize.getItemAt(0)).isEqualTo(1);
            assertThat(subtextSize.getItemAt(1)).isEqualTo(5);
            assertThat(subtextSize.getItemAt(2)).isEqualTo(10);
            assertThat(subtextSize.getItemAt(3)).isEqualTo(19);
            assertThat(subtextSize.getSelectedItem()).isEqualTo(10);

            runOnEdt(() -> subtextSize.setSelectedItem(19));
            SSInvoice[] holder = new SSInvoice[1];
            runOnEdt(() -> holder[0] = panel.getInvoice());

            assertThat(holder[0].getSubtextSize()).isEqualTo(19);
        } finally {
            runOnEdt(panel::dispose);
        }
    }

    @Test
    void selectedCustomerTransfersSubtextAndFileSubtextValuesToInvoicePanel() throws Exception {
        SSInvoicePanel panel = createPanelOnEdt();
        try {
            List<String> expectedFileOptions = SSInvoiceTextFileOptions.getInvoiceTextFileNames();
            assertThat(expectedFileOptions).isNotEmpty();

            String selectedFile = expectedFileOptions.size() > 1
                    ? expectedFileOptions.get(1)
                    : expectedFileOptions.get(0);

            SSCustomer customer = new SSCustomer();
            customer.setNumber("C-201");
            customer.setName("Kund 201");
            customer.setPrintSwish(true);
            customer.setPrintSubtext(true);
            customer.setSubtextSize(10);
            customer.setFileSubtext(selectedFile);

            SSInvoice invoice = new SSInvoice();
            invoice.setCustomer(customer);

            runOnEdt(() -> panel.setInvoice(invoice));

            JCheckBox printSwish = getField(panel, "iPrintSwish", JCheckBox.class);
            JCheckBox printSubtext = getField(panel, "iPrintSubtext", JCheckBox.class);
            JComboBox<String> fileSubtext = getField(panel, "iFileSubtext", JComboBox.class);
            JComboBox<Integer> subtextSize = getField(panel, "iSubtextSize", JComboBox.class);

            assertThat(printSwish.isSelected()).isTrue();
            assertThat(printSubtext.isSelected()).isTrue();
            assertThat(subtextSize.getSelectedItem()).isEqualTo(10);
            assertThat(fileSubtext.getSelectedItem()).isEqualTo(selectedFile);

            for (int i = 0; i < expectedFileOptions.size(); i++) {
                assertThat(fileSubtext.getItemAt(i)).isEqualTo(expectedFileOptions.get(i));
            }

            SSInvoice[] holder = new SSInvoice[1];
            runOnEdt(() -> holder[0] = panel.getInvoice());

            assertThat(holder[0].isPrintSwish()).isTrue();
            assertThat(holder[0].isPrintSubtext()).isTrue();
            assertThat(holder[0].getSubtextSize()).isEqualTo(10);
            assertThat(holder[0].getFileSubtext()).isEqualTo(selectedFile);
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
