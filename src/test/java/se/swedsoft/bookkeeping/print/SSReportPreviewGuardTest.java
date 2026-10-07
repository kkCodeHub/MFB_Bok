package se.swedsoft.bookkeeping.print;

import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.frame.SSInternalFrame;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.print.view.SSPreviewFrame;
import sun.misc.Unsafe;

import javax.swing.JDesktopPane;
import javax.swing.SwingUtilities;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class SSReportPreviewGuardTest {

    @AfterEach
    void tearDown() throws Exception {
        runOnEdt(SSInternalFrame::closeAllFrames);
    }

    @Test
    void activatesExistingPreviewFrameForMatchingPreviewKey() throws Exception {
        TestPreviewFrame previewFrame = createPreviewFrame("report-preview-key");
        runOnEdt(() -> {
            previewFrame.setVisible(true);
            try {
                previewFrame.setIcon(true);
            } catch (PropertyVetoException e) {
                throw new RuntimeException(e);
            }
        });

        SSReport report = new SSReport();
        report.setPreviewKey("report-preview-key");

        boolean activated = activateExistingPreviewFrame(report);

        assertThat(activated).isTrue();
        assertThat(previewFrame.isIcon()).isFalse();
    }

    @Test
    void doesNotActivatePreviewFrameForDifferentPreviewKey() throws Exception {
        TestPreviewFrame previewFrame = createPreviewFrame("accounts-payable");
        runOnEdt(() -> previewFrame.setVisible(true));

        SSReport report = new SSReport();
        report.setPreviewKey("supplier-debt");

        boolean activated = activateExistingPreviewFrame(report);

        assertThat(activated).isFalse();
        assertThat(previewFrame.getPreviewKey()).isEqualTo("accounts-payable");
    }

    @Test
    void multiPrinterUsesFirstSubReportPreviewKey() {
        SSMultiPrinter printer = createMultiPrinter();

        printer.addReport(createStubPrinter("first-report-key", "First report"));
        printer.addReport(createStubPrinter("second-report-key", "Second report"));

        assertThat(printer.getPreviewKey())
                .isEqualTo(SSMultiPrinter.class.getName() + ":first-report-key");
    }

    @Test
    void multiPrinterSkipsNullPreviewKeysUntilItFindsFirstGuardedReport() {
        SSMultiPrinter printer = createMultiPrinter();

        printer.addReport(createStubPrinter(null, "Ungarded report"));
        printer.addReport(createStubPrinter("guarded-report-key", "Guarded report"));

        assertThat(printer.getPreviewKey())
                .isEqualTo(SSMultiPrinter.class.getName() + ":guarded-report-key");
    }

    private static boolean activateExistingPreviewFrame(SSReport report) throws Exception {
        Method method = SSReport.class.getDeclaredMethod("activateExistingPreviewFrame");
        method.setAccessible(true);
        return (Boolean) method.invoke(report);
    }

    private static TestPreviewFrame createPreviewFrame(String previewKey) throws Exception {
        TestPreviewFrame[] holder = new TestPreviewFrame[1];
        SSMainFrame mainFrame = createMainFrameStub();

        runOnEdt(() -> {
            TestPreviewFrame previewFrame = new TestPreviewFrame(mainFrame, previewKey);
            previewFrame.setPreviewKey(previewKey);
            holder[0] = previewFrame;
        });

        return holder[0];
    }

    private static SSMultiPrinter createMultiPrinter() {
        try {
            Unsafe unsafe = getUnsafe();
            SSMultiPrinter printer = (SSMultiPrinter) unsafe.allocateInstance(SSMultiPrinter.class);

            Field subReportsField = SSMultiPrinter.class.getDeclaredField("iSubReports");
            subReportsField.setAccessible(true);
            subReportsField.set(printer, new LinkedList<>());

            return printer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static StubPrinter createStubPrinter(String previewKey, String title) {
        try {
            Unsafe unsafe = getUnsafe();
            StubPrinter printer = (StubPrinter) unsafe.allocateInstance(StubPrinter.class);
            printer.previewKey = previewKey;
            printer.title = title;
            printer.model = new SSDefaultTableModel<>() {
                @Override
                public Class<?> getType() {
                    return String.class;
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    return getObject(rowIndex);
                }
            };
            printer.model.setObjects(Collections.singletonList(title));
            return printer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SSMainFrame createMainFrameStub() throws Exception {
        Unsafe unsafe = getUnsafe();
        SSMainFrame frame = (SSMainFrame) unsafe.allocateInstance(SSMainFrame.class);

        JDesktopPane desktopPane = new JDesktopPane();
        desktopPane.setSize(1200, 800);

        Field field = SSMainFrame.class.getDeclaredField("iDesktop");
        field.setAccessible(true);
        field.set(frame, desktopPane);
        return frame;
    }

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void runOnEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static class StubPrinter extends SSPrinter {
        private String previewKey;
        private String title;
        private SSDefaultTableModel<String> model;

        @Override
        public void generateReport() {
            // No-op for preview key tests.
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        protected SSDefaultTableModel getModel() {
            return model;
        }

        @Override
        protected String getPreviewKey() {
            return previewKey;
        }

        @Override
        public JasperReport getReport() {
            return null;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Collections.emptyMap();
        }

        @Override
        public ResourceBundle getBundle() {
            return iBundle;
        }
    }

    private static class TestPreviewFrame extends SSInternalFrame implements SSPreviewFrame {
        private String previewKey;

        private TestPreviewFrame(SSMainFrame mainFrame, String previewKey) {
            super(mainFrame, "Preview", 300, 200);
            this.previewKey = previewKey;
        }

        @Override
        public boolean isCompanyFrame() {
            return false;
        }

        @Override
        public boolean isYearDataFrame() {
            return false;
        }

        @Override
        public String getPreviewKey() {
            return previewKey;
        }

        private void setPreviewKey(String previewKey) {
            this.previewKey = previewKey;
        }
    }
}
