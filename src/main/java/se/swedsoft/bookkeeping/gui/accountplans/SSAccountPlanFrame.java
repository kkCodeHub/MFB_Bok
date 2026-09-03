package se.swedsoft.bookkeeping.gui.accountplans;

import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.accountplans.util.SSAccountPlanTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSButton;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSProgressDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSExcelFileChooser;
import se.swedsoft.bookkeeping.gui.util.frame.SSDefaultTableFrame;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanExporter;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.print.report.SSAccountPlanPrinter;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Account plan frame.
 */
public class SSAccountPlanFrame extends SSDefaultTableFrame {

    private static SSAccountPlanFrame cInstance;

    public static void showFrame(SSMainFrame pMainFrame, int pWidth, int pHeight) {
        if (cInstance == null || cInstance.isClosed()) {
            cInstance = new SSAccountPlanFrame(pMainFrame, pWidth, pHeight);
        }
        cInstance.setVisible(true);
        cInstance.deIconize();
    }

    public static SSAccountPlanFrame getInstance() {
        return cInstance;
    }

    private SSTable iTable;
    private SSAccountPlanTableModel iModel;

    private SSAccountPlanFrame(SSMainFrame pMainFrame, int width, int height) {
        super(pMainFrame, SSBundle.getBundle().getString("accountplanframe.title"), width, height);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                updateFrame();
            }
        });
    }

    @Override
    public JToolBar getToolBar() {
        JToolBar toolBar = new JToolBar();

        SSButton button = new SSButton("ICON_NEWITEM", "accountplanframe.newbutton", e -> {
            updateFrame();
            SSAccountPlanDialog.newDialog(getMainFrame());
            updateFrame();
        });
        toolBar.add(button);

        button = new SSButton("ICON_EDITITEM", "accountplanframe.editbutton", e -> editSelectedAccountPlan());
        iTable.addSelectionDependentComponent(button);
        toolBar.add(button);
        toolBar.addSeparator();

        button = new SSButton("ICON_COPYITEM", "accountplanframe.copybutton", e -> copySelectedAccountPlan());
        iTable.addSelectionDependentComponent(button);
        toolBar.add(button);

        button = new SSButton("ICON_DELETEITEM", "accountplanframe.deletebutton", e -> deleteSelectedAccountPlan());
        iTable.addSelectionDependentComponent(button);
        toolBar.add(button);
        toolBar.addSeparator();

        button = new SSButton("ICON_IMPORT", "accountplanframe.importbutton", e -> {
            SSAccountPlanDialog.newDialog(getMainFrame());
            updateFrame();
        });
        toolBar.add(button);

        button = new SSButton("ICON_EXPORT", "accountplanframe.exportbutton", e -> exportSelectedAccountPlan());
        iTable.addSelectionDependentComponent(button);
        toolBar.add(button);
        toolBar.addSeparator();

        button = new SSButton("ICON_PRINT", "accountplanframe.printbutton", e -> printSelectedAccountPlan());
        iTable.addSelectionDependentComponent(button);
        toolBar.add(button);

        return toolBar;
    }

    @Override
    public JComponent getMainContent() {
        iTable = new SSTable();

        iModel = new SSAccountPlanTableModel();
        iModel.addColumn(SSAccountPlanTableModel.COLUMN_NAME);
        iModel.addColumn(SSAccountPlanTableModel.COLUMN_TYPE);
        iModel.addColumn(SSAccountPlanTableModel.COLUMN_ASSESSMENTYEAR);
        iModel.addColumn(SSAccountPlanTableModel.COLUMN_DEFAULTPLAN);
//        iModel.addColumn(SSAccountPlanTableModel.COLUMN_EXCELPATH);
        iModel.setObjects(SSAccountingContext.getAccountPlans());
        iModel.setupTable(iTable);

        iTable.addDblClickListener(e -> ShowSelectedAccountPlan());   //editSelectedAccountPlan());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JScrollPane(iTable), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return panel;
    }

    @Override
    public JComponent getStatusBar() {
        return null;
    }

    @Override
    public boolean isCompanyFrame() {
        return true;
    }

    @Override
    public boolean isYearDataFrame() {
        return false;
    }

    private SSAccountPlan getSelected() {
        int selected = iTable.getSelectedRow();
        if (selected >= 0) {
            return iModel.getObject(selected);
        }
        return null;
    }

    private void ShowSelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            return;
        }
        if (selected.isTemplatePlan()) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.show");
            return;
        }
    }

    private void editSelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.selectone");
            return;
        }
        if (selected.isDefaultPlan()) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.editDEFblocked");
            return;
        }

        if (!selected.isDefaultPlan()) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.editblocked");
            return;
        }

     //   String name = selected.getName();
     //   SSAccountPlan accountPlan = getAccountPlan(selected);
     //   if (accountPlan != null) {
     //       SSAccountPlanDialog.editDialog(getMainFrame(), accountPlan, iModel);
     //   } else {
     //       new SSErrorDialog(getMainFrame(), "accountplanframe.accountplangone", name);
     //   }
     //   updateFrame();
    }

    private void copySelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.selectone");
            return;
        }

        if (selected.isTemplatePlan()) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.copyblocked");
            return;
        }

//        String name = selected.getName();
//        SSAccountPlan accountPlan = getAccountPlan(selected);
//        if (accountPlan != null) {
//            SSAccountPlanDialog.copyDialog(getMainFrame(), accountPlan);
//        } else {
//            new SSErrorDialog(getMainFrame(), "accountplanframe.accountplangone", name);
//        }
//        updateFrame();
    }

    private void deleteSelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.selectone");
            return;
        }
        if (selected.isDefaultPlan()) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.deleteblocked");  //"Default account plans cannot be deleted.");
            return;
        }

        SSQueryDialog dialog = new SSQueryDialog(getMainFrame(), SSBundle.getBundle(), "accountplanframe.delete", selected.getName());
        int response = dialog.getResponce();
        updateFrame();
        if (response != JOptionPane.YES_OPTION) {
            return;
        }
        SSAccountingContext.deleteAccountPlan(selected);
        fireTableDataChanged();
    }

    private void exportSelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.selectone");
            return;
        }

        String name = selected.getName();
        SSAccountPlan accountPlan = getAccountPlan(selected);
        if (accountPlan == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.accountplangone", name);
            return;
        }

        SSExcelFileChooser chooser = SSExcelFileChooser.getInstance();
        File dataDir = Path.get(Path.USER_DATA);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        chooser.setCurrentDirectory(dataDir);
        chooser.setSelectedFile(new File(dataDir, accountPlan.getName() + ".xlsx"));

        if (chooser.showSaveDialog(getMainFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        SSAccountPlanExporter exporter = new SSAccountPlanExporter(chooser.getSelectedFile());
        try {
            exporter.doExport(accountPlan);
        } catch (IOException ex) {
            SSErrorDialog.showDialog(getMainFrame(), "", ex.getLocalizedMessage());
        } catch (SSExportException ex) {
            SSErrorDialog.showDialog(getMainFrame(), "", ex.getLocalizedMessage());
        }
    }

    private void printSelectedAccountPlan() {
        SSAccountPlan selected = getSelected();
        if (selected == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.selectone");
            return;
        }

        String name = selected.getName();
        SSAccountPlan accountPlan = getAccountPlan(selected);
        if (accountPlan == null) {
            new SSErrorDialog(getMainFrame(), "accountplanframe.accountplangone", name);
            return;
        }

        SSProgressDialog.runProgress(getMainFrame(), () -> {
            SSAccountPlanPrinter printer = new SSAccountPlanPrinter(accountPlan);
            printer.preview(getMainFrame());
        });
    }

    public SSAccountPlan getAccountPlan(SSAccountPlan iAccountPlan) {
        if (iAccountPlan == null) {
            return null;
        }

        if (!iAccountPlan.isTemplatePlan()) {
            return SSAccountingContext.getAccountPlan(iAccountPlan).orElse(iAccountPlan);
        }

        try {
            return SSAccountPlanImporter.loadPlan(iAccountPlan);
        } catch (IOException ex) {
            SSErrorDialog.showDialog(getMainFrame(), "", ex.getLocalizedMessage());
            return null;
        }
    }

    public void updateFrame() {
        iModel.setObjects(SSAccountingContext.getAccountPlans());
    }

    public static void fireTableDataChanged() {
        if (cInstance != null && !cInstance.isClosed()) {
            cInstance.updateFrame();
        }
    }

    public void actionPerformed(ActionEvent e) {
        iTable = null;
        iModel = null;
        cInstance = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("se.swedsoft.bookkeeping.gui.accountplans.SSAccountPlanFrame");
        sb.append("{iModel=").append(iModel);
        sb.append(", iTable=").append(iTable);
        sb.append('}');
        return sb.toString();
    }
}
