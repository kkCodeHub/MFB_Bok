package se.swedsoft.bookkeeping.gui.invoice;


import se.swedsoft.bookkeeping.calc.math.SSCustomerMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSPeriodicInvoiceMath;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSInvoiceActionPolicy;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.product.SSProductFrame;
import se.swedsoft.bookkeeping.gui.creditinvoice.SSCreditInvoiceDialog;
import se.swedsoft.bookkeeping.gui.creditinvoice.SSCreditInvoiceFrame;
import se.swedsoft.bookkeeping.gui.inpayment.SSInpaymentDialog;
import se.swedsoft.bookkeeping.gui.inpayment.SSInpaymentFrame;
import se.swedsoft.bookkeeping.gui.invoice.dialog.SSInterestInvoiceDialog;
import se.swedsoft.bookkeeping.gui.invoice.panel.SSInvoiceSearchPanel;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInvoiceTableModel;
import se.swedsoft.bookkeeping.gui.periodicinvoice.SSPeriodicInvoiceDialog;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSButton;
import se.swedsoft.bookkeeping.gui.util.components.SSMenuButton;
import se.swedsoft.bookkeeping.gui.util.components.SSTabbedPanePanel;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSConfirmDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSDefaultTableFrame;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.gui.util.table.SSTableSorter;
import se.swedsoft.bookkeeping.print.SSReportFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:47:21
 */
public class SSInvoiceFrame extends SSDefaultTableFrame {

    private static SSInvoiceFrame cInstance;

    /**
     *
     * @param pMainFrame
     * @param pWidth
     * @param pHeight
     */
    public static void showFrame(SSMainFrame pMainFrame, int pWidth, int pHeight) {
        if (cInstance == null || SSInvoiceFrame.cInstance.isClosed()) {
            cInstance = new SSInvoiceFrame(pMainFrame, pWidth, pHeight);
        }
        SSInvoiceFrame.cInstance.setVisible(true);
        SSInvoiceFrame.cInstance.deIconize();
        SSInvoiceFrame.cInstance.updateFrame();

        if (SSPeriodicInvoiceMath.hasPendingPeriodicInvoices()
                && new SSConfirmDialog("periodicinvoiceframe.pendingperiodicinvoices").openDialog(
                        pMainFrame)
                                == JOptionPane.OK_OPTION) {
            SSPeriodicInvoiceDialog.pendingPeriodicInvoicesDialog(pMainFrame);
        }
    }

    /**
     *
     * @return The SSNewCompanyFrame
     */
    public static SSInvoiceFrame getInstance() {
        return cInstance;
    }

    private JTabbedPane iTabbedPane;

    private SSTable iTable;

    private JScrollPane iTableScrollPane;

    private SSInvoiceTableModel iModel;

    private SSInvoiceSearchPanel iSearchPanel;

    private SSButton iReminderButton;

    private SSButton iInpaymentButton;

    private SSButton iCreditInvoiceButton;

    private JMenuItem iReminderMenuItem;

    /**
     * Constructor.
     *
     * @param pMainFrame The main frame.
     * @param width     The width of the frame.
     * @param height    The height of the frame.
     */
    private SSInvoiceFrame(SSMainFrame pMainFrame, int width, int height) {
        super(pMainFrame, SSBundle.getBundle().getString("invoiceframe.title"), width,
                height);
    }

    /**
     * This method should return a toolbar if the sub-class wants one.
     * Otherwise, it may return null.
     *
     * @return A JToolBar or null.
     */
    @Override
    public JToolBar getToolBar() {
        JToolBar toolBar = new JToolBar();

        // New
        // ***************************
        SSButton iButton = new SSButton("ICON_NEWITEM", "invoiceframe.newbutton",
                e -> SSInvoiceDialog.newDialog(getMainFrame()));

        toolBar.add(iButton);

        // Edit
        // ***************************
        iButton = new SSButton("ICON_EDITITEM", "invoiceframe.editbutton",
                e -> {

                        SSInvoice iSelected = iModel.getSelectedRow(iTable);
                        Integer iNumber = null;

                        if (iSelected != null) {
                            iNumber = iSelected.getNumber();
                            iSelected = getInvoice(iSelected);
                        }
                        if (iSelected != null) {
                            SSInvoiceDialog.editDialog(getMainFrame(), iSelected);
                        } else {
                            new SSErrorDialog(getMainFrame(), "invoiceframe.invoicegone", iNumber);
                        }

                    });
        iTable.addSelectionDependentComponent(iButton);
        toolBar.add(iButton);
        toolBar.addSeparator();

        // Copy
        // ***************************
        iButton = new SSButton("ICON_COPYITEM", "invoiceframe.copybutton",
                e -> {

                        SSInvoice iSelected = iModel.getSelectedRow(iTable);
                        Integer iNumber = null;

                        if (iSelected != null) {
                            iNumber = iSelected.getNumber();
                            iSelected = getInvoice(iSelected);
                        }
                        if (iSelected != null) {
                            SSInvoiceDialog.copyDialog(getMainFrame(), iSelected);
                        } else {
                            new SSErrorDialog(getMainFrame(), "invoiceframe.invoicegone", iNumber);
                        }

                    });
        iTable.addSelectionDependentComponent(iButton);
        toolBar.add(iButton);
        toolBar.addSeparator();

        // Delete
        // ***************************
        iButton = new SSButton("ICON_DELETEITEM", "invoiceframe.deletebutton",
                e -> {

                        int[] selected = iTable.getSelectedRows();
                        List<SSInvoice> toDelete = iModel.getObjects(selected);

                        deleteSelectedInvoice(toDelete);

                    });
        iTable.addSelectionDependentComponent(iButton);
        toolBar.add(iButton);
        toolBar.addSeparator();

        // Create inpayment for sales
        // ***************************
        iButton = new SSButton("ICON_COINS24", "invoiceframe.inpaymentbutton",
                e -> {

                        if (iTable.getSelectedRowCount() > 0) {
                            List<SSInvoice> iSelected = iModel.getObjects(iTable.getSelectedRows());

                            iSelected = getInvoices(iSelected);
                            iSelected.removeIf(iInvoice -> !SSInvoiceActionPolicy.canRegisterInpayment(iInvoice));
                            SSInpayment iInpayment = new SSInpayment();

                            if (!iSelected.isEmpty()) {
                                iInpayment.addInvoices(iSelected);

                                if (SSInpaymentFrame.getInstance() != null) {
                                    SSInpaymentDialog.newDialog(getMainFrame(), iInpayment,
                                            SSInpaymentFrame.getInstance().getModel());
                                } else {
                                    SSInpaymentDialog.newDialog(getMainFrame(), iInpayment, null);
                                }
                            }
                        }

                    });
        iInpaymentButton = iButton;
        iTable.addSelectionDependentComponent(iButton);
        toolBar.add(iButton);

        // Create creditinvoice for sales
        // ***************************
        iButton = new SSButton("ICON_CREATECHANGE", "invoiceframe.creditinvoicebutton",
                e -> {

                        SSInvoice iSelected = iModel.getSelectedRow(iTable);
                        Integer iNumber = null;

                        if (iSelected != null) {
                            iNumber = iSelected.getNumber();
                            iSelected = getInvoice(iSelected);
                        }
                        if (iSelected != null && SSInvoiceActionPolicy.canCreateCreditInvoice(iSelected)) {

                            if (SSCreditInvoiceFrame.getInstance() != null) {
                                SSCreditInvoiceDialog.newDialog(getMainFrame(), iSelected,
                                        SSCreditInvoiceFrame.getInstance().getModel());
                            } else {
                                SSCreditInvoiceDialog.newDialog(getMainFrame(), iSelected, null);
                            }
                        } else if (iSelected == null) {
                            new SSErrorDialog(getMainFrame(), "invoiceframe.invoicegone", iNumber);
                        }

                    });
        iCreditInvoiceButton = iButton;
        iTable.addSelectionDependentComponent(iButton);
        toolBar.add(iButton);

        // Skriv ut påminellse för valda fakturor
        // ***************************
        iButton = new SSButton("ICON_EXCLAMATION24", "invoiceframe.reminderbutton",
                e -> {

                        List<SSInvoice> iSelected = iModel.getObjects(iTable.getSelectedRows());

                        iSelected = getInvoices(iSelected);
                        iSelected.removeIf(iInvoice -> !SSInvoiceActionPolicy.canSelectForReminder(iInvoice));
                        if (!iSelected.isEmpty()) {
                            SSReportFactory.ReminderReport(getMainFrame(), iSelected);
                            // updateFrame();
                        }
                        // iModel.fireTableDataChanged();

                    });
        iReminderButton = iButton;
        toolBar.add(iButton);

        // Skapa räntefakturor
        // ***************************
        iButton = new SSButton("ICON_INVOICE24", "invoiceframe.interestinvoicebutton",
                e -> SSInterestInvoiceDialog.showDialog(getMainFrame(), iModel));
        toolBar.add(iButton);

        toolBar.addSeparator();

        // Print
        // ***************************
        SSMenuButton<SSButton> iMenuButton = new SSMenuButton<>("ICON_PRINT",
                "invoiceframe.printbutton");
        JMenuItem iMenuItem = iMenuButton.add("invoiceframe.print.invoicereport",
                e -> {

                        List<SSInvoice> iSelected = iModel.getSelectedRows(iTable);

                        iSelected = getInvoices(iSelected);
                        iSelected.removeIf(iInvoice -> !SSInvoiceActionPolicy.canPrint(iInvoice));
                        if (!iSelected.isEmpty()) {
                            SSReportFactory.InvoiceReport(getMainFrame(), iSelected);
                        }

                    });

        iTable.addSelectionDependentComponent(iMenuItem);
        iMenuItem = iMenuButton.add("invoiceframe.print.reminder", e -> {

                List<SSInvoice> iSelected = iModel.getObjects(iTable.getSelectedRows());

                iSelected = getInvoices(iSelected);
                iSelected.removeIf(iInvoice -> !SSInvoiceActionPolicy.canSelectForReminder(iInvoice));
                if (!iSelected.isEmpty()) {
                    SSReportFactory.ReminderReport(getMainFrame(), iSelected);
                }

            });
        iReminderMenuItem = iMenuItem;

        iMenuButton.addSeparator();
        iMenuItem = iMenuButton.add("invoiceframe.print.ocrinvoicereport",
                e -> {

                        List<SSInvoice> iSelected = iModel.getSelectedRows(iTable);

                        iSelected = getInvoices(iSelected);
                        iSelected.removeIf(iInvoice -> !SSInvoiceActionPolicy.canPrint(iInvoice));
                        if (!iSelected.isEmpty()) {
                            SSReportFactory.OCRInvoiceReport(getMainFrame(), iSelected);
                        }

                    });
        iTable.addSelectionDependentComponent(iMenuItem);

        iMenuButton.addSeparator();
        iMenuButton.add("invoiceframe.print.invoicelistreport", e -> SSReportFactory.InvoiceListReport(getMainFrame()));
        toolBar.add(iMenuButton);

        updateReminderActionsState();

        return toolBar;
    }

    /**
     * This method should return the main content for the frame.
     * Such as an object table.
     *
     * @return The main content for this frame.
     */
    @Override
    public JComponent getMainContent() {

        iTable = new SSTable();

        iModel = new SSInvoiceTableModel();
        iModel.addColumn(SSInvoiceTableModel.COLUMN_PRINTED);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_NUMBER);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_TYPE);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_CUSTOMER_NR);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_CUSTOMER_NAME);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_DATE);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_DUEDATE);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_NET_SUM);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_CURRENCY);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_CURRENCY_RATE);
        iModel.addColumn(SSInvoiceTableModel.COLUMN_TOTAL_SUM);
        // iModel.addColumn(SSInvoiceTableModel.COLUMN_SALDO);
        iModel.addColumn(SSInvoiceTableModel.getSaldoColumn());
        iModel.addColumn(SSInvoiceTableModel.COLUMN_REMINDERS);

        iModel.setupTable(iTable);
        ((SSTableSorter) iTable.getModel()).setSortingStatus(1, SSTableSorter.ASCENDING);
        iTable.addSelectionListener(e -> updateReminderActionsState());

        iTableScrollPane = new JScrollPane(iTable);

        iTable.addDblClickListener(
                e -> {

                        SSInvoice iSelected = iModel.getSelectedRow(iTable);
                        Integer iNumber;

                        if (iSelected != null) {
                            iNumber = iSelected.getNumber();
                            iSelected = getInvoice(iSelected);
                        } else {
                            return;
                        }
                        if (iSelected != null) {
                            SSInvoiceDialog.editDialog(getMainFrame(), iSelected);
                        } else {
                            new SSErrorDialog(getMainFrame(), "invoiceframe.invoicegone", iNumber);
                        }

                    });

        iTabbedPane = new JTabbedPane();

        iTabbedPane.add(SSBundle.getBundle().getString("invoiceframe.filter.1"),
                new SSTabbedPanePanel());
        iTabbedPane.add(SSBundle.getBundle().getString("invoiceframe.filter.2"),
                new SSTabbedPanePanel());
        iTabbedPane.add(SSBundle.getBundle().getString("invoiceframe.filter.3"),
                new SSTabbedPanePanel());

        iTabbedPane.addChangeListener(e -> iSearchPanel.ApplyFilter(SSSalesContext.getInvoices()));
        // setFilterIndex(0);

        JPanel iPanel = new JPanel();

        iSearchPanel = new SSInvoiceSearchPanel(iModel);
        iPanel.setLayout(new BorderLayout());
        iPanel.add(iSearchPanel, BorderLayout.NORTH);
        iPanel.add(iTabbedPane, BorderLayout.CENTER);
        iPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));

        updateReminderActionsState();

        return iPanel;
    }

    /**
     *
     * @param index
     * @param iInvoices
     */
    public void setFilterIndex(int index, List<SSInvoice> iInvoices) {
        JPanel iPanel = (JPanel) iTabbedPane.getComponentAt(index);

        // Move the shared scroll pane to the selected tab panel, if needed.
        if (iTableScrollPane.getParent() != iPanel) {
            Container oldParent = iTableScrollPane.getParent();
            if (oldParent != null) {
                oldParent.remove(iTableScrollPane);
                if (oldParent instanceof JComponent) {
                    ((JComponent) oldParent).revalidate();
                }
            }
            iPanel.removeAll();
            iPanel.add(iTableScrollPane, BorderLayout.CENTER);
            iPanel.revalidate();
            iPanel.repaint();
        }

        List<SSInvoice> iFiltered = Collections.emptyList();

        //

        switch (index) {
        // Alla
        case 0:
            iFiltered = iInvoices;
            break;

        // Obetalda
        case 1:
            iFiltered = new LinkedList<>();
            for (SSInvoice iInvoice : iInvoices) {

                /* if( SSInvoiceMath.getSaldo(iInvoice).signum() != 0){
                 iFiltered.add(iInvoice);
                 }*/
                if (SSInvoiceMath.iSaldoMap != null
                        && SSInvoiceMath.iSaldoMap.containsKey(iInvoice.getNumber())) {
                    if (SSInvoiceMath.iSaldoMap.get(iInvoice.getNumber()).signum() != 0) {
                        iFiltered.add(iInvoice);
                    }
                }
            }
            break;

        // Förfallna
        case 2:
            iFiltered = new LinkedList<>();
            for (SSInvoice iInvoice : iInvoices) {
                if (SSInvoiceMath.iSaldoMap != null
                        && SSInvoiceMath.iSaldoMap.containsKey(iInvoice.getNumber())) {
                    if (SSInvoiceMath.iSaldoMap.get(iInvoice.getNumber()).signum() != 0
                            && SSInvoiceMath.expired(iInvoice)
                            && SSInvoiceActionPolicy.canSelectForReminder(iInvoice)) {
                        iFiltered.add(iInvoice);
                    }
                }
            }
            break;
        }
        iModel.setObjects(iFiltered);
        iTabbedPane.revalidate();
        iTabbedPane.repaint();
        updateReminderActionsState();
    }

    /**
     *
     * @return
     */
    public SSInvoiceTableModel getModel() {
        return iModel;
    }

    public JTabbedPane getTabbedPane() {
        return iTabbedPane;
    }

    /**
     * This method should return the status bar content, if any.
     *
     * @return The content for the status bar or null if none is wanted.
     */
    @Override
    public JComponent getStatusBar() {
        return null;
    }

    /**
     * Indicates whether this frame is a company data related frame.
     *
     * @return A boolean value.
     */
    @Override
    public boolean isCompanyFrame() {
        return true;
    }

    /**
     * Indicates whether this frame is a year data related frame.
     *
     * @return A boolean value.
     */
    @Override
    public boolean isYearDataFrame() {
        return false;
    }

    /**
     *
     * @param delete
     */
    private void deleteSelectedInvoice(List<SSInvoice> delete) {
        if (delete.isEmpty()) {
            return;
        }

        SSQueryDialog iDialog = new SSQueryDialog(getMainFrame(), "invoiceframe.delete");
        int iResponce = iDialog.getResponce();

        if (iResponce == JOptionPane.YES_OPTION) {
            List<SSInvoice> iCurrentInvoices = getInvoices(delete);
            iCurrentInvoices.sort(Comparator.comparing(SSInvoice::getNumber, Comparator.nullsLast(Integer::compareTo))
                    .reversed());

            for (SSInvoice iInvoice : iCurrentInvoices) {
                if (SSInvoiceActionPolicy.canDeletePhysically(iInvoice, SSSalesContext.getInvoices())) {
                    deleteInvoicePhysically(iInvoice);
                } else if (SSInvoiceActionPolicy.canUncancel(iInvoice, SSSalesContext.getInvoices())) {
                    iInvoice.clearCancelled();
                    SSSalesContext.updateInvoice(iInvoice);
                } else if (SSInvoiceActionPolicy.canCancel(iInvoice)) {
                    iInvoice.setCancelled();
                    SSSalesContext.updateInvoice(iInvoice);
                }
            }
            updateFrame();
        }
    }

    private void deleteInvoicePhysically(SSInvoice iInvoice) {
        decrementInvoiceCounter();
        removeInvoiceFromCustomerCache(iInvoice);
        SSSalesContext.deleteInvoice(iInvoice);
    }

    /**
     * Applies the invoice-number counter step used when the highest invoice is physically deleted.
     * The counter is reduced by exactly one and persisted on the current company.
     */
    private void decrementInvoiceCounter() {
        SSNewCompany iCurrentCompany = SSCompanyYearContext.getCurrentCompany();
        if (iCurrentCompany == null) {
            return;
        }
        int iCurrentCounter = iCurrentCompany.getAutoIncrement().getNumber("invoice");
        if (iCurrentCounter <= 0) {
            return;
        }
        iCurrentCompany.getAutoIncrement().setNumber("invoice", iCurrentCounter - 1);
        SSCompanyYearContext.updateCompany(iCurrentCompany);
    }

    private void removeInvoiceFromCustomerCache(SSInvoice iInvoice) {
        List<SSInvoice> iInvoicesForCustomer = SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr());
        if (iInvoicesForCustomer == null) {
            return;
        }
        int iIndex = iInvoicesForCustomer.indexOf(iInvoice);
        if (iIndex != -1) {
            iInvoicesForCustomer.remove(iIndex);
        }
    }

    private SSInvoice getInvoice(SSInvoice iInvoice) {
        return SSSalesContext.getInvoice(iInvoice).orElse(null);
    }

    private List<SSInvoice> getInvoices(List<SSInvoice> iInvoices) {
        return SSSalesContext.getInvoices(iInvoices);
    }

    /**
     *
     */
    public static void fireTableDataChanged() {
        if (cInstance != null) {
            cInstance.updateFrame();
        }
    }

    public void updateFrame() {
        iSearchPanel.ApplyFilter(SSSalesContext.getInvoices());
        updateReminderActionsState();
        SSProductFrame.fireTableDataChanged();
    }

    private void updateReminderActionsState() {
        if (iModel == null || iTable == null) {
            return;
        }

        List<SSInvoice> iSelected = iModel.getObjects(iTable.getSelectedRows());
        boolean hasSelection = !iSelected.isEmpty();
        boolean onlyExpired = hasSelection && hasOnlyExpiredInvoices(iSelected);
        boolean inpaymentAllowed = hasSelection && hasOnlyInvoicesAllowedForInpayment(iSelected);
        boolean creditAllowed = hasSingleInvoiceAllowedForCredit(iSelected);

        String defaultTooltip = SSBundle.getBundle().getString("invoiceframe.reminderbutton.tooltip");
        String disabledTooltip = SSBundle.getBundle().getString("invoiceframe.reminderbutton.disabled.tooltip");
        String menuDefaultTooltip = SSBundle.getBundle().getString("invoiceframe.print.reminder.tooltip");

        if (iReminderButton != null) {
            iReminderButton.setEnabled(onlyExpired);
            iReminderButton.setToolTipText(!hasSelection || onlyExpired ? defaultTooltip : disabledTooltip);
        }
        if (iReminderMenuItem != null) {
            iReminderMenuItem.setEnabled(onlyExpired);
            iReminderMenuItem.setToolTipText(!hasSelection || onlyExpired
                    ? menuDefaultTooltip
                    : disabledTooltip);
        }
        if (iInpaymentButton != null) {
            iInpaymentButton.setEnabled(inpaymentAllowed);
        }
        if (iCreditInvoiceButton != null) {
            iCreditInvoiceButton.setEnabled(creditAllowed);
        }
    }

    private boolean hasOnlyExpiredInvoices(List<SSInvoice> iInvoices) {
        for (SSInvoice iInvoice : iInvoices) {
            SSInvoice iCurrent = getInvoice(iInvoice);
            if (iCurrent == null
                    || !SSInvoiceMath.expired(iCurrent)
                    || !SSInvoiceActionPolicy.canSelectForReminder(iCurrent)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasOnlyInvoicesAllowedForInpayment(List<SSInvoice> iInvoices) {
        for (SSInvoice iInvoice : iInvoices) {
            SSInvoice iCurrent = getInvoice(iInvoice);
            if (iCurrent == null || !SSInvoiceActionPolicy.canRegisterInpayment(iCurrent)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSingleInvoiceAllowedForCredit(List<SSInvoice> iInvoices) {
        if (iInvoices.size() != 1) {
            return false;
        }
        SSInvoice iCurrent = getInvoice(iInvoices.get(0));
        return iCurrent != null && SSInvoiceActionPolicy.canCreateCreditInvoice(iCurrent);
    }

    public void actionPerformed(ActionEvent e) {
        iTable = null;
        iModel = null;
        cInstance = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame");
        sb.append("{iModel=").append(iModel);
        sb.append(", iSearchPanel=").append(iSearchPanel);
        sb.append(", iTabbedPane=").append(iTabbedPane);
        sb.append(", iTable=").append(iTable);
        sb.append('}');
        return sb.toString();
    }
}
