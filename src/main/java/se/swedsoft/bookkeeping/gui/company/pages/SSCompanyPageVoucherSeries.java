package se.swedsoft.bookkeeping.gui.company.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSDBConfig;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.gui.util.table.SSTableSorter;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherSeriesService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Company settings page for Val 8: Voucher Series Configuration.
 * Displays system and custom event-to-series mappings with CRUD operations.
 */
public class SSCompanyPageVoucherSeries extends SSCompanyPage {
    private static final Logger LOG = LoggerFactory.getLogger(SSCompanyPageVoucherSeries.class);
    private static final int COLUMN_PADDING = 10;

    private JPanel iPanel;
    private SSTable iTable;
    private JButton iAddButton;
    private JButton iEditButton;
    private JButton iDeleteButton;
    private JLabel iTitleLabel;

    private SSNewCompany iCompany;
    private V2VoucherSeriesService iService;

    public SSCompanyPageVoucherSeries(JDialog iDialog) {
        super(iDialog);
        initializeUI();
    }

    private void initializeUI() {
        iPanel = new JPanel(new BorderLayout());

        iTitleLabel = new JLabel("");
        iTitleLabel.setFont(iTitleLabel.getFont().deriveFont(Font.BOLD));

        iTable = new SSTable();
        iTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setupTableColumns();

        JScrollPane tableScroll = new JScrollPane(iTable);
        tableScroll.setPreferredSize(new Dimension(600, 300));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iAddButton = new JButton("Lägg till");
        iEditButton = new JButton("Ändra");
        iDeleteButton = new JButton("Ta bort");

        iAddButton.addActionListener(e -> addCustomMapping());
        iEditButton.addActionListener(e -> editCustomMapping());
        iDeleteButton.addActionListener(e -> deleteCustomMapping());

        buttonPanel.add(iAddButton);
        buttonPanel.add(iEditButton);
        buttonPanel.add(iDeleteButton);

        iPanel.add(iTitleLabel, BorderLayout.NORTH);
        iPanel.add(tableScroll, BorderLayout.CENTER);
        iPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupTableColumns() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addColumn("Verifikatkod");
        model.addColumn("Händelsekod");
        model.addColumn("Händelsenamn");
        model.addColumn("Typ");
        model.addColumn("Id");
        iTable.setModel(model);
        iTable.setRowSelectionAllowed(true);
        iTable.setColumnSelectionAllowed(false);
        iTable.getColumnModel().getColumn(0).setPreferredWidth(56);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(56);
        iTable.getColumnModel().getColumn(2).setPreferredWidth(272);
        iTable.getColumnModel().getColumn(3).setPreferredWidth(56);
        iTable.getColumnModel().getColumn(4).setMinWidth(0);
        iTable.getColumnModel().getColumn(4).setMaxWidth(0);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(0);
        iTable.getColumnModel().getColumn(0).setCellRenderer(createIndentedRenderer());
        iTable.getColumnModel().getColumn(1).setCellRenderer(createIndentedRenderer());
    }

    private void loadData() {
        if (iService == null || iCompany == null) {
            return;
        }

        try {
            Integer yearId = resolveYearId();
            if (yearId == null) {
                return;
            }

            String dateRange = getAccountingYearDateRange(yearId);
            iTitleLabel.setText("Bokföringsår: " + dateRange);

            DefaultTableModel model = (DefaultTableModel) ((SSTableSorter) iTable.getModel()).getTableModel();
            model.setRowCount(0);

            List<Map<String, Object>> mappings = iService.getMappingsForYear(yearId);

            for (Map<String, Object> mapping : mappings) {
                int mappingId = ((Number) mapping.get("id")).intValue();
                String seriesCode = (String) mapping.getOrDefault("series_code", "");
                String eventCode = (String) mapping.getOrDefault("event_code", "");
                String displayName = (String) mapping.getOrDefault("display_event_name", "");
                Boolean isCustom = (Boolean) mapping.getOrDefault("is_custom", false);
                String type = isCustom ? "Egen" : "System";

                model.addRow(new Object[]{seriesCode, eventCode, displayName, type, mappingId});
            }
        } catch (SQLException ex) {
            LOG.error("Failed to load voucher series mappings", ex);
            JOptionPane.showMessageDialog(iPanel,
                    "Fel vid inläsning av verifikatserier: " + ex.getMessage(),
                    "Fel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getAccountingYearDateRange(int yearId) {
        try {
            SSNewAccountingYear probe = new SSNewAccountingYear();
            probe.setId(yearId);
            SSNewAccountingYear year = Repositories.accountingYears().findById(probe).orElse(null);
            if (year == null) {
                year = Repositories.accountingYears().findCurrent().orElse(null);
            }
            if (year != null && year.getLocalFrom() != null && year.getLocalTo() != null) {
                return year.getLocalFrom().toString() + " - " + year.getLocalTo().toString();
            }
        } catch (Exception ex) {
            LOG.error("Failed to get accounting year date range", ex);
        }
        return "Okänt bokföringsår";
    }

    private Integer resolveYearId() {
        Integer yearId = SSDBConfig.getYearId();
        if (yearId != null) {
            return yearId;
        }
        SSNewAccountingYear currentYear = Repositories.accountingYears().findCurrent().orElse(null);
        return currentYear != null ? currentYear.getId() : null;
    }

    private void addCustomMapping() {
        Integer yearId = resolveYearId();
        if (yearId == null) {
            JOptionPane.showMessageDialog(iPanel,
                    "Inget räkenskapsår valt",
                    "Fel", JOptionPane.ERROR_MESSAGE);
            return;
        }

        VoucherSeriesMappingDialog dialog = new VoucherSeriesMappingDialog(
                (JDialog) iDialog,
                iService,
                yearId,
                VoucherSeriesMappingDialog.Mode.ADD,
                null);

        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            loadData();
        }
    }

    private void editCustomMapping() {
        int selectedRow = iTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(iPanel,
                    "Välj en rad att ändra",
                    "Val krävs", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) ((SSTableSorter) iTable.getModel()).getTableModel();
        int modelRow = iTable.convertRowIndexToModel(selectedRow);
        Object typeObj = model.getValueAt(modelRow, 3);
        String type = typeObj != null ? typeObj.toString() : "";

        if ("System".equals(type)) {
            JOptionPane.showMessageDialog(iPanel,
                    "Systemrader kan inte ändras",
                    "Begränsning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String seriesCode = (String) model.getValueAt(modelRow, 0);
        String customName = (String) model.getValueAt(modelRow, 2);
        int mappingId = ((Number) model.getValueAt(modelRow, 4)).intValue();

        Integer yearId = resolveYearId();
        if (yearId == null) {
            JOptionPane.showMessageDialog(iPanel,
                    "Inget räkenskapsår valt",
                    "Fel", JOptionPane.ERROR_MESSAGE);
            return;
        }

        VoucherSeriesMappingDialog dialog = new VoucherSeriesMappingDialog(
                (JDialog) iDialog,
                iService,
                yearId,
                VoucherSeriesMappingDialog.Mode.EDIT,
                new VoucherSeriesMappingDialog.MappingData(mappingId, customName, seriesCode));

        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            loadData();
        }
    }

    private void deleteCustomMapping() {
        int selectedRow = iTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(iPanel,
                    "Välj en rad att ta bort",
                    "Val krävs", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) ((SSTableSorter) iTable.getModel()).getTableModel();
        int modelRow = iTable.convertRowIndexToModel(selectedRow);
        Object typeObj = model.getValueAt(modelRow, 3);
        String type = typeObj != null ? typeObj.toString() : "";

        if ("System".equals(type)) {
            JOptionPane.showMessageDialog(iPanel,
                    "Systemrader kan inte tas bort",
                    "Begränsning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(iPanel,
                "Är du säker på att du vill ta bort denna rad?",
                "Bekräfta borttagning", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Integer yearId = resolveYearId();
                if (yearId == null) {
                    return;
                }

                String customName = (String) model.getValueAt(modelRow, 2);
                List<Map<String, Object>> mappings = iService.getMappingsForYear(yearId);

                for (Map<String, Object> mapping : mappings) {
                    if (customName.equals(mapping.get("display_event_name")) &&
                        (Boolean) mapping.getOrDefault("is_custom", false)) {
                        int mappingId = ((Number) mapping.get("id")).intValue();
                        iService.deleteCustomMapping(mappingId);
                        break;
                    }
                }
                loadData();
            } catch (SQLException ex) {
                LOG.error("Failed to delete custom mapping", ex);
                JOptionPane.showMessageDialog(iPanel,
                        "Fel vid borttagning: " + ex.getMessage(),
                        "Fel", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.voucherseries");
    }

    @Override
    public JPanel getPanel() {
        return iPanel;
    }

    @Override
    public void setCompany(SSNewCompany iCompany) {
        this.iCompany = iCompany;
        LOG.info("SSCompanyPageVoucherSeries.setCompany() called with company: {}", iCompany != null ? iCompany.getName() : "null");

        if (iService == null && iCompany != null) {
            try {
                Connection conn = SSSystemConfigContext.getDatabase().getConnection();
                LOG.info("Connection obtained: {}", conn != null ? "not null" : "null");
                if (conn != null) {
                    iService = new V2VoucherSeriesService(conn);
                    LOG.info("Service created successfully");
                }
            } catch (Exception ex) {
                LOG.error("Failed to create service", ex);
            }
        }

        LOG.info("Calling loadData()...");
        loadData();
    }

    @Override
    public SSNewCompany getCompany() {
        return iCompany;
    }

    private DefaultTableCellRenderer createIndentedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value);
                setBorder(new EmptyBorder(0, COLUMN_PADDING, 0, 0));
            }
        };
    }
}
