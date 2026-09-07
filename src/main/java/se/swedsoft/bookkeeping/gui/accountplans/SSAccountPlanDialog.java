package se.swedsoft.bookkeeping.gui.accountplans;

import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.accountplans.panel.SSAccountPlanPanel;
import se.swedsoft.bookkeeping.gui.accountplans.util.SSAccountPlanTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSExcelFileChooser;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class SSAccountPlanDialog {

    private static final ResourceBundle BUNDLE = SSBundle.getBundle();

    private SSAccountPlanDialog() {
    }

    public static void newDialog(final SSMainFrame iMainFrame) {
        File dataDir = Path.get(Path.USER_DATA);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        SSExcelFileChooser chooser = SSExcelFileChooser.getInstance();
        chooser.setCurrentDirectory(dataDir);
        if (chooser.showOpenDialog(iMainFrame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            SSAccountPlanImporter.doImport(chooser.getSelectedFile());
            SSAccountPlanFrame.fireTableDataChanged();
        } catch (IOException ex) {
            SSErrorDialog.showDialog(iMainFrame, "", ex.getLocalizedMessage());
        } catch (SSImportException ex) {
            SSErrorDialog.showDialog(iMainFrame, "", ex.getLocalizedMessage());
        }
    }

    public static void editDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan) {
        if (iAccountPlan != null && iAccountPlan.isTemplatePlan()) {
            SSErrorDialog.showDialog(iMainFrame, "", "Template account plans cannot be changed.");
            return;
        }

        final SSDialog dialog = new SSDialog(iMainFrame, BUNDLE.getString("accountplanframe.edit.title"));
        final SSAccountPlanPanel panel = new SSAccountPlanPanel(iMainFrame);
        final SSAccountPlan original = iAccountPlan;

        panel.setAccountPlan(new SSAccountPlan(iAccountPlan));
        panel.setShowBase(false);
        final SSAccountPlan initialState = createAccountPlanSnapshot(panel.getAccountPlan());

        ActionListener saveAction = e -> {
            SSAccountPlan workingCopy = panel.getAccountPlan();
            List<SSAccountPlan> plans = SSAccountingContext.getAccountPlans();
            for (SSAccountPlan plan : plans) {
                if (workingCopy.getName().equals(plan.getName()) && !workingCopy.getName().equals(iAccountPlan.getName())) {
                    new SSErrorDialog(iMainFrame, "accountplanframe.duplicate", workingCopy.getName());
                    return;
                }
            }

            original.copyFrom(workingCopy);
            SSAccountingContext.updateAccountPlan(original);
            SSAccountPlanFrame.fireTableDataChanged();
            dialog.closeDialog();
        };

        panel.addOkAction(saveAction);
        panel.addCancelAction(e -> dialog.closeDialog());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!hasUnsavedChanges(panel.getAccountPlan(), initialState)) {
                    dialog.closeDialog();
                    return;
                }
                if (!panel.isValid()) {
                    return;
                }
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(), "accountplanframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                saveAction.actionPerformed(null);
            }
        });

        dialog.add(panel.getPanel(), BorderLayout.CENTER);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(iMainFrame);
        dialog.setVisible();
    }

    public static void editDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan,
                                  final SSAccountPlanTableModel pModel) {
        editDialog(iMainFrame, iAccountPlan);
    }

    public static void editCurrentDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan,
                                         boolean pSuggestedName) {
        final SSDialog dialog = new SSDialog(iMainFrame, BUNDLE.getString("accountplanframe.editcurrent.title"));
        final SSAccountPlanPanel panel = new SSAccountPlanPanel(iMainFrame);

        panel.setSuggestedName(pSuggestedName);
        SSAccountPlan workingCopy = new SSAccountPlan(iAccountPlan);
        workingCopy.setId(null);
        workingCopy.setExcelPath(null);
        workingCopy.setDefaultPlan(false);
        panel.setAccountPlan(workingCopy);
        panel.setShowBase(true);
        final SSAccountPlan initialState = createAccountPlanSnapshot(panel.getAccountPlan());

        ActionListener saveAction = e -> {
            SSAccountPlan accountPlan = panel.getAccountPlan();
            SSNewAccountingYear currentYear = SSAccountingContext.getCurrentYear();
            currentYear.setAccountPlan(accountPlan);
            SSAccountingContext.updateAccountingYear(currentYear);
            SSAccountPlanFrame.fireTableDataChanged();
            dialog.closeDialog();
        };

        panel.addOkAction(saveAction);
        panel.addCancelAction(e -> dialog.closeDialog());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!hasUnsavedChanges(panel.getAccountPlan(), initialState)) {
                    dialog.closeDialog();
                    return;
                }
                if (!panel.isValid()) {
                    return;
                }
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(), "accountplanframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                saveAction.actionPerformed(null);
            }
        });

        dialog.add(panel.getPanel(), BorderLayout.CENTER);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(iMainFrame);
        dialog.setVisible();
    }

    public static void editCurrentDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan,
                                         SSAccountPlanPanel pPanel) {
        editCurrentDialog(iMainFrame, iAccountPlan, false);
    }

    public static void editCurrentDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan,
                                         SSAccountPlanPanel pPanel, boolean pSuggestedName) {
        editCurrentDialog(iMainFrame, iAccountPlan, pSuggestedName);
    }

    public static void copyDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan) {
        if (iAccountPlan != null && iAccountPlan.isTemplatePlan()) {
            SSErrorDialog.showDialog(iMainFrame, "", "Template account plans cannot be copied.");
            return;
        }

        final SSDialog dialog = new SSDialog(iMainFrame, BUNDLE.getString("accountplanframe.copy.title"));
        final SSAccountPlanPanel panel = new SSAccountPlanPanel(iMainFrame);

        panel.setAccountPlan(new SSAccountPlan(iAccountPlan));
        panel.setShowBase(false);
        final SSAccountPlan initialState = createAccountPlanSnapshot(panel.getAccountPlan());

        ActionListener saveAction = e -> {
            SSAccountPlan copy = panel.getAccountPlan();
            List<SSAccountPlan> plans = SSAccountingContext.getAccountPlans();
            for (SSAccountPlan plan : plans) {
                if (copy.getName().equals(plan.getName())) {
                    new SSErrorDialog(iMainFrame, "accountplanframe.duplicate", copy.getName());
                    return;
                }
            }

            SSAccountingContext.addAccountPlan(copy);
            SSAccountPlanFrame.fireTableDataChanged();
            dialog.closeDialog();
        };

        panel.addOkAction(saveAction);
        panel.addCancelAction(e -> dialog.closeDialog());
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!hasUnsavedChanges(panel.getAccountPlan(), initialState)) {
                    dialog.closeDialog();
                    return;
                }
                if (!panel.isValid()) {
                    return;
                }
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(), "accountplanframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                saveAction.actionPerformed(null);
            }
        });

        dialog.add(panel.getPanel(), BorderLayout.CENTER);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(iMainFrame);
        dialog.setVisible();
    }

    public static void editCurrentDialog(final SSMainFrame iMainFrame, SSAccountPlan iAccountPlan) {
        editCurrentDialog(iMainFrame, iAccountPlan, false);
    }

    private static boolean hasUnsavedChanges(SSAccountPlan accountPlan, SSAccountPlan initialState) {
        return !isSameAccountPlan(initialState, accountPlan);
    }

    private static SSAccountPlan createAccountPlanSnapshot(SSAccountPlan accountPlan) {
        return accountPlan == null ? null : new SSAccountPlan(accountPlan);
    }

    private static boolean isSameAccountPlan(SSAccountPlan left, SSAccountPlan right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }

        if (!Objects.equals(left.getName(), right.getName())) {
            return false;
        }
        if (!Objects.equals(left.getBaseName(), right.getBaseName())) {
            return false;
        }
        if (!Objects.equals(left.getAssessementYear(), right.getAssessementYear())) {
            return false;
        }
        if (!Objects.equals(left.getExcelPath(), right.getExcelPath())) {
            return false;
        }
        if (left.isDefaultPlan() != right.isDefaultPlan()) {
            return false;
        }

        String leftType = left.getType() == null ? null : left.getType().getName();
        String rightType = right.getType() == null ? null : right.getType().getName();
        if (!Objects.equals(leftType, rightType)) {
            return false;
        }

        List<SSAccount> leftAccounts = left.getAccounts();
        List<SSAccount> rightAccounts = right.getAccounts();
        if (leftAccounts.size() != rightAccounts.size()) {
            return false;
        }

        for (int i = 0; i < leftAccounts.size(); i++) {
            SSAccount leftAccount = leftAccounts.get(i);
            SSAccount rightAccount = rightAccounts.get(i);

            if (leftAccount == rightAccount) {
                continue;
            }
            if (leftAccount == null || rightAccount == null) {
                return false;
            }
            if (!Objects.equals(leftAccount.getNumber(), rightAccount.getNumber())) {
                return false;
            }
            if (!Objects.equals(leftAccount.getDescription(), rightAccount.getDescription())) {
                return false;
            }
            if (!Objects.equals(leftAccount.getVATCode(), rightAccount.getVATCode())) {
                return false;
            }
            if (!Objects.equals(leftAccount.getSRUCode(), rightAccount.getSRUCode())) {
                return false;
            }
            if (!Objects.equals(leftAccount.getReportCode(), rightAccount.getReportCode())) {
                return false;
            }
            if (leftAccount.isActive() != rightAccount.isActive()) {
                return false;
            }
            if (leftAccount.isProjectRequired() != rightAccount.isProjectRequired()) {
                return false;
            }
            if (leftAccount.isResultUnitRequired() != rightAccount.isResultUnitRequired()) {
                return false;
            }
        }
        return true;
    }
}
