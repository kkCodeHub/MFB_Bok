package se.swedsoft.bookkeeping.gui.customer.panel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SSCustomerPanelSubtextDefaultsTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_gui_customer_panel";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("GUI Customer Panel Test AB");
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
    static void teardown() {
        System.clearProperty("fribok.schema.version");
    }

    @Test
    void newCustomerUsesAllowedSubtextSizesAndDefaultsToOne() throws Exception {
        SSCustomerPanel panel = createPanelOnEdt();
        runOnEdt(() -> panel.setCustomer(new SSCustomer()));

        JComboBox<Integer> subtextSize = getField(panel, "iSubtextSize", JComboBox.class);
        assertThat(subtextSize.getItemCount()).isEqualTo(4);
        assertThat(subtextSize.getItemAt(0)).isEqualTo(1);
        assertThat(subtextSize.getItemAt(1)).isEqualTo(5);
        assertThat(subtextSize.getItemAt(2)).isEqualTo(10);
        assertThat(subtextSize.getItemAt(3)).isEqualTo(19);
        assertThat(subtextSize.getSelectedItem()).isEqualTo(1);
    }

    @Test
    void fileSubtextComboUsesInvoiceTextFileNamesAndSelectsFirst() throws Exception {
        SSCustomerPanel panel = createPanelOnEdt();
        runOnEdt(() -> panel.setCustomer(new SSCustomer()));

        JComboBox<String> fileSubtext = getField(panel, "iFileSubtext", JComboBox.class);

        File directory = new File(Path.get(Path.APP_DATA), "invoiceText");
        File[] files = directory.listFiles(File::isFile);
        if (files == null) {
            files = new File[0];
        }

        Collator collator = Collator.getInstance(new Locale("sv", "SE"));
        Arrays.sort(files, (left, right) -> collator.compare(left.getName(), right.getName()));
        List<String> expected = Arrays.stream(files).map(File::getName).toList();
        List<String> actual = IntStream.range(0, fileSubtext.getItemCount())
                .mapToObj(fileSubtext::getItemAt)
                .toList();

        assertThat(actual).isEqualTo(expected);
        assertThat(actual).doesNotContain("", "Faktura", "Kundfaktura", "E-post");
        if (!expected.isEmpty()) {
            assertThat(fileSubtext.getSelectedItem()).isEqualTo(expected.get(0));
        }
    }

    private static SSCustomerPanel createPanelOnEdt() throws Exception {
        final SSCustomerPanel[] holder = new SSCustomerPanel[1];
        runOnEdt(() -> holder[0] = new SSCustomerPanel(null, false));
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
