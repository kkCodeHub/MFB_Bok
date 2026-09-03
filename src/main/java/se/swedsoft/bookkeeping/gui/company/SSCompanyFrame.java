package se.swedsoft.bookkeeping.gui.company;


import org.fribok.bookkeeping.app.SSDBUiInitializer;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.*;
import se.swedsoft.bookkeeping.data.util.SSConfig;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.accountingyear.SSAccountingYearFrame;
import se.swedsoft.bookkeeping.gui.company.util.SSCompanyTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSButton;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSDefaultTableFrame;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.util.frame.SSInternalFrame;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Optional;


/**
 * Date: 2006-feb-02
 * Time: 10:40:47
 */
public class SSCompanyFrame extends SSDefaultTableFrame {

    private static SSCompanyFrame cInstance;

    private SSTable iTable;

    private JCheckBox iShowAtStartup;

    private SSDefaultTableModel<SSNewCompany> iModel;

    /**
     *
     * @param pMainFrame
     * @param pWidth
     * @param pHeight
     */
    public static void showFrame(SSMainFrame pMainFrame, int pWidth, int pHeight) {
        if (cInstance == null || cInstance.isClosed()) {
            cInstance = new SSCompanyFrame(pMainFrame, pWidth, pHeight);
        }
        cInstance.setVisible(true);
        cInstance.deIconize();
    }

    /**
     *
     * @return The SSNewCompanyFrame
     */
    public static SSCompanyFrame getInstance() {
        return cInstance;
    }

    /**
     * Constructor.
     *
     * @param pMainFrame The main frame.
     * @param width     The width of the frame.
     * @param height    The height of the frame.
     */
    private SSCompanyFrame(SSMainFrame pMainFrame, int width, int height) {
        super(pMainFrame, SSBundle.getBundle().getString("companyframe.title"), width,
                height);
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                updateFrame();
            }
        });

    }

    /**
     * This method should return a toolbar if the sub-class wants one.
     * Otherwise, it may return null.
     *
     * @return A JToolBar or null.
     */
    @Override
    public JToolBar getToolBar() {
        JToolBar iToolBar = new JToolBar();

        // Open
        // ***************************
        SSButton iButton = new SSButton("ICON_OPENITEM", "companyframe.openbutton",
                e -> openSelectedCompany());

        iToolBar.add(iButton);
        iToolBar.addSeparator();
        iTable.addSelectionDependentComponent(iButton);

        // New
        // ***************************
        iButton = new SSButton("ICON_NEWITEM", "companyframe.newbutton",
                e -> {

                        updateFrame();
                        SSCompanyDialog.newDialog(getMainFrame(), iModel);

                    });
        iButton.setEnabled(true);
        iToolBar.add(iButton);

        // Edit
        // ***************************
        iButton = new SSButton("ICON_EDITITEM", "companyframe.editbutton",
                e -> editSelectedCompany());
        iToolBar.add(iButton);
        iTable.addSelectionDependentComponent(iButton);

        // Delete
        // ***************************
        iButton = new SSButton("ICON_DELETEITEM", "companyframe.deletebutton",
                e -> deleteSelectedCompany());
        iToolBar.add(iButton);
        iTable.addSelectionDependentComponent(iButton);

        return iToolBar;
    }

    /**
     * This method should return the main content for the frame.
     * Such as an object table.
     *
     * @return The main content for this frame.
     */
    @Override
    public JComponent getMainContent() {
        iModel = new SSCompanyTableModel();
        updateFrame();

        iShowAtStartup = new JCheckBox(
                SSBundle.getBundle().getString("companyframe.showatstart"));
        iShowAtStartup.setSelected(
                (Boolean) SSConfig.getInstance().get("companyframe.showatstart", true));

        iShowAtStartup.addActionListener(
                e -> {

                        SSConfig.getInstance().set("companyframe.showatstart",
                                iShowAtStartup.isSelected());

                    });

        iTable = new SSTable() {
            @Override
            public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                if (row >= 0) {
                    SSNewCompany company = iModel.getObject(convertRowIndexToModel(row));
                    if (company != null && company.isNeedsAttention()) {
                        return "Kontrollera företaget";
                    }
                }
                return null;
            }
        };

        iTable.setSingleSelect();
        iTable.setColumnSortingEnabled(false);
        iTable.setModel(iModel);
        iTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                SSNewCompany company = iModel.getObject(table.convertRowIndexToModel(row));
                if (company != null && company.isNeedsAttention()) {
                    component.setForeground(Color.RED);
                } else {
                    component.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                }
                return component;
            }
        });

        iTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        iTable.getColumnModel().getColumn(0).setMaxWidth(70);

        iTable.addDblClickListener(e -> openSelectedCompany());
        JPanel iPanel = new JPanel();

        iPanel.setLayout(new BorderLayout());
        iPanel.add(new JScrollPane(iTable), BorderLayout.CENTER);
        iPanel.add(iShowAtStartup, BorderLayout.SOUTH);
        iPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        return iPanel;
    }

    /**
     *
     * @return The selected company, if any
     */
    private SSNewCompany getSelected() {
        int selected = iTable.getSelectedRow();

        if (selected >= 0) {
            return iModel.getObject(selected);
        }
        return null;
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
        return false;
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
     * Öppnar det valda företaget
     */
    private void openSelectedCompany() {
        // Hämta markerat företag
        SSNewCompany iNewCompany = getSelected();

        // Kontrollera att ett företag blev valt
        if (iNewCompany == null) {
            // Inget företag markerat. Visa felmeddelande.
            new SSErrorDialog(getMainFrame(), "companyframe.selectonecompany");
            return;
        }
        // Stäng fönstret om företaget redan är öppet.
        if (iNewCompany.equals(SSCompanyYearContext.getCurrentCompany())) {
            cInstance.dispose();
            return;
        }
        iNewCompany = resolveSelectedCompany(iNewCompany);
        if (iNewCompany == null) {
            new SSErrorDialog(getMainFrame(), "companyframe.companygone");
            return;
        }

        // Fråga om företaget ska öppnas.
        SSQueryDialog iDialog = new SSQueryDialog(getMainFrame(), SSBundle.getBundle(),
                "companyframe.replacecompany", iNewCompany.getName());

        if (iDialog.getResponce() != JOptionPane.YES_OPTION) {
            // Svarade inte ja. Avbryt funktionen
            return;
        }
        SSNewCompany companyToOpen = iNewCompany;
        try {
            if (companyToOpen.getId() == null && companyToOpen.getSchemaName() != null) {
                Repositories.companies().registerOrActivateCompanySchema(companyToOpen.getSchemaName(), companyToOpen.getName());
                companyToOpen = Repositories.companies().findBySchemaName(companyToOpen.getSchemaName()).orElse(companyToOpen);
            }
            SSCompanyYearContext.setCurrentCompany(companyToOpen);
            if (companyToOpen.getId() != null) {
                SSDBConfig.setCompanyId(companyToOpen.getId());
            }
        } catch (RuntimeException e) {
            SSErrorDialog.showDialog(getMainFrame(), "Företaget är korrupt",
                    "Företaget är korrupt och kan inte öppnas. Radera företaget.");
            updateFrame();
            return;
        }

        SSCompanyYearContext.setCurrentYear(null);

        // Stäng alla fönster
        SSFrameManager.getInstance().close();

        // Läs in det förra öppna året för företaget
        Optional<SSNewAccountingYear> iYear = SSDBConfig.loadCompanySetting(companyToOpen.getId());

        if (iYear.isEmpty()) {
            // Inget år för företaget sparat. Öppna årsfönstret
            if (companyToOpen.getId() != null) {
                SSDBConfig.setYearId(companyToOpen.getId(), null);
            }
            SSAccountingYearFrame.showFrame(getMainFrame(), 500, 300, false);
        } else {
            // Hittade ett sparat år. Sätt det som nuvarande
            if (SSCompanyYearContext.canOpenAccountingYear(iYear.get())) {
                SSCompanyYearContext.openYear(iYear.get());
                if (companyToOpen.getId() != null) {
                    SSDBConfig.setYearId(companyToOpen.getId(), iYear.get().getId());
                }
            } else {
                if (companyToOpen.getId() != null) {
                    SSDBConfig.setYearId(companyToOpen.getId(), null);
                }
                SSAccountingYearFrame.showFrame(getMainFrame(), 500, 300, false);
            }
        }
        SSDBUiInitializer.init(true);
    }

    private void editSelectedCompany() {
        SSNewCompany pCompany = getSelected();

        updateFrame();
        // If nothing selected, return
        if (pCompany == null) {
            new SSErrorDialog(getMainFrame(), "companyframe.selectonecompany");
            return;
        }
        if (pCompany.isNeedsAttention()) {
            SSErrorDialog.showDialog(getMainFrame(), "Kontrollera företaget",
                    "Företaget måste kontrolleras innan redigering.");
            return;
        }

        // Kontrollera att företaget fortfarande finns i databasen
        pCompany = resolveSelectedCompany(pCompany);
        if (pCompany == null) {
            // Företaget fanns inte kvar i databasen. Visa felmeddelande.
            new SSErrorDialog(getMainFrame(), "companyframe.companygone");
            return;
        }
        // Ask the user if he want's to open the selected company to be able to edit it
        if (!pCompany.equals(SSCompanyYearContext.getCurrentCompany())) {
            // Ask to open the company
            String iCurrent = SSCompanyYearContext.getCurrentCompany() == null
                    ? ""
                    : SSCompanyYearContext.getCurrentCompany().getName();
            String iNew = pCompany.getName();

            SSQueryDialog iDialog = new SSQueryDialog(getMainFrame(), SSBundle.getBundle(),
                    "companyframe.editcompany.openquery", iNew, iCurrent);

            if (iDialog.getResponce() != JOptionPane.YES_OPTION) {
                updateFrame();
                return;
            }

            // Close all company related frames
            SSInternalFrame.closeAllFrames();
            // Select the company

            // Sätt det valda företaget som nuvarande företag
            SSCompanyYearContext.setCurrentCompany(pCompany);
            if (pCompany.getId() != null) {
                SSDBConfig.setCompanyId(pCompany.getId());
            }

            SSCompanyYearContext.setCurrentYear(null);

            // Stäng alla fönster
            SSFrameManager.getInstance().close();

            // Läs in det förra öppna året för företaget
            Optional<SSNewAccountingYear> iYear = SSDBConfig.loadCompanySetting(pCompany.getId());

            if (iYear.isEmpty()) {
                // Inget år för företaget sparat. Öppna årsfönstret
                if (pCompany.getId() != null) {
                    SSDBConfig.setYearId(pCompany.getId(), null);
                }
                SSAccountingYearFrame.showFrame(getMainFrame(), 500, 300, false);
            } else {
                // Hittade ett sparat år. Sätt det som nuvarande
                if (SSCompanyYearContext.canOpenAccountingYear(iYear.get())) {
                    SSCompanyYearContext.openYear(iYear.get());
                    if (pCompany.getId() != null) {
                        SSDBConfig.setYearId(pCompany.getId(), iYear.get().getId());
                    }
                } else {
                    if (pCompany.getId() != null) {
                        SSDBConfig.setYearId(pCompany.getId(), null);
                    }
                    SSAccountingYearFrame.showFrame(getMainFrame(), 500, 300, false);
                }
            }
            SSDBUiInitializer.init(true);
        }
        if (cInstance != null) {
            updateFrame();
        }

        SSCompanyDialog.editDialog(getMainFrame(), SSCompanyYearContext.getCurrentCompany(),
                iModel);
    }

    private void deleteSelectedCompany() {
        SSNewCompany pCompany = getSelected();

        if (pCompany == null) {
            new SSErrorDialog(getMainFrame(), "companyframe.selectonecompany");
            return;
        }
        updateFrame();

        SSQueryDialog iDialog = new SSQueryDialog(getMainFrame(), SSBundle.getBundle(),
                "companyframe.deletecompany", pCompany.getName());

        if (iDialog.getResponce() != JOptionPane.YES_OPTION) {
            return;
        }
        updateFrame();

        boolean iCurrentRemoved = false;

        if (pCompany.equals(SSCompanyYearContext.getCurrentCompany())) {
            SSFrameManager.getInstance().close();
            iCurrentRemoved = true;
        }

        try {
            SSCompanyYearContext.deleteCompany(pCompany);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("co_0")) {
                SSErrorDialog.showDialog(getMainFrame(), "Kan inte radera företag",
                        "Demoföretaget (co_0) kan inte raderas.");
            } else {
                SSErrorDialog.showDialog(getMainFrame(), "Kan inte radera företag",
                        "Företaget kunde inte raderas.");
            }
            return;
        }

        if (iCurrentRemoved) {
            SSCompanyYearContext.setCurrentCompany(null);
            SSCompanyYearContext.setCurrentYear(null);
        }
        updateFrame();
    }

    public void updateFrame() {
        iModel.setObjects(SSCompanyYearContext.getCompanies());
        SSCompanyYearContext.notifyListeners("COMPANY",
                SSCompanyYearContext.getCurrentCompany(), null);
    }

    private SSNewCompany resolveSelectedCompany(SSNewCompany selected) {
        if (selected == null) {
            return null;
        }
        if (selected.getId() != null) {
            return SSCompanyYearContext.getCompany(selected).orElse(null);
        }
        if (selected.getSchemaName() != null) {
            return Repositories.companies().findBySchemaName(selected.getSchemaName()).orElse(selected);
        }
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        iTable = null;
        iModel = null;
        iShowAtStartup = null;
        cInstance = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.SSCompanyFrame");
        sb.append("{iModel=").append(iModel);
        sb.append(", iShowAtStartup=").append(iShowAtStartup);
        sb.append(", iTable=").append(iTable);
        sb.append('}');
        return sb.toString();
    }
}
