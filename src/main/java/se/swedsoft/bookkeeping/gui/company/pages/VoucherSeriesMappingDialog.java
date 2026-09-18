package se.swedsoft.bookkeeping.gui.company.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherSeriesService;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dialog for adding or editing custom voucher series mappings.
 */
public class VoucherSeriesMappingDialog extends SSDialog {
    private static final Logger LOG = LoggerFactory.getLogger(VoucherSeriesMappingDialog.class);

    public enum Mode {
        ADD, EDIT
    }

    public static class MappingData {
        public int mappingId;
        public String customEventName;
        public String seriesCode;

        public MappingData(int mappingId, String customEventName, String seriesCode) {
            this.mappingId = mappingId;
            this.customEventName = customEventName;
            this.seriesCode = seriesCode;
        }
    }

    private V2VoucherSeriesService iService;
    private int iYearId;
    private Mode iMode;
    private MappingData iExistingData;
    private boolean iRestoringSelection;
    private SeriesCodeOption iLastEnabledSelection;

    private JTextField iCustomEventNameField;
    private JComboBox<SeriesCodeOption> iSeriesCodeCombo;
    private JButton iOkButton;
    private JButton iCancelButton;
    private int iResultCode = JOptionPane.CANCEL_OPTION;

    public VoucherSeriesMappingDialog(JDialog parent, V2VoucherSeriesService service, 
            int yearId, Mode mode, MappingData existingData) {
        super(parent, mode == Mode.ADD ? "Lägg till egen verifikatserie" : "Ändra verifikatserie");
        
        this.iService = service;
        this.iYearId = yearId;
        this.iMode = mode;
        this.iExistingData = existingData;

        initializeUI();
        setupData();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("Händelsenamn:");
        iCustomEventNameField = new JTextField(20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        contentPanel.add(iCustomEventNameField, gbc);

        JLabel seriesLabel = new JLabel("Verifikatkod:");
        iSeriesCodeCombo = new JComboBox<>();
        iSeriesCodeCombo.setRenderer(new SeriesCodeRenderer());
        iSeriesCodeCombo.addActionListener(e -> handleSeriesSelection());
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(seriesLabel, gbc);
        gbc.gridx = 1;
        contentPanel.add(iSeriesCodeCombo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        iOkButton = new JButton("OK");
        iCancelButton = new JButton("Avbryt");

        iOkButton.addActionListener(e -> saveMapping());
        iCancelButton.addActionListener(e -> {
            iResultCode = JOptionPane.CANCEL_OPTION;
            dispose();
        });

        buttonPanel.add(iOkButton);
        buttonPanel.add(iCancelButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setPanel(mainPanel);
    }

    private void setupData() {
        Set<String> usedCodes = loadUsedSeriesCodes();
        List<SeriesCodeOption> options = new java.util.ArrayList<>();
        String currentCode = iExistingData != null ? iExistingData.seriesCode : null;

        for (char code = 'A'; code <= 'Z'; code++) {
            String seriesCode = String.valueOf(code);
            boolean enabled = !usedCodes.contains(seriesCode) || seriesCode.equals(currentCode);
            options.add(new SeriesCodeOption(seriesCode, enabled));
        }

        iSeriesCodeCombo.setModel(new DefaultComboBoxModel<>(options.toArray(new SeriesCodeOption[0])));

        if (iMode == Mode.EDIT && iExistingData != null) {
            iCustomEventNameField.setText(iExistingData.customEventName);
            selectSeriesCode(iExistingData.seriesCode);
        } else {
            iCustomEventNameField.setText("");
            selectFirstEnabledSeriesCode();
        }
        iCustomEventNameField.selectAll();
        iCustomEventNameField.requestFocus();
        updateOkButtonState();
    }

    private void saveMapping() {
        String customEventName = iCustomEventNameField.getText().trim();
        SeriesCodeOption selected = (SeriesCodeOption) iSeriesCodeCombo.getSelectedItem();
        String seriesCode = selected != null ? selected.code : null;

        if (customEventName.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "Händelsenamn får inte vara tomt",
                    "Validering", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selected == null || !selected.enabled) {
            return;
        }

        try {
            if (iMode == Mode.ADD) {
                iService.addCustomMapping(iYearId, customEventName, seriesCode);
                LOG.info("Added custom mapping: {}", customEventName);
            } else if (iMode == Mode.EDIT && iExistingData != null) {
                iService.updateCustomMapping(iExistingData.mappingId, customEventName, seriesCode);
                LOG.info("Updated custom mapping: {}", customEventName);
            }
            iResultCode = JOptionPane.OK_OPTION;
            dispose();
        } catch (SQLException ex) {
            LOG.error("Failed to save mapping", ex);
            JOptionPane.showMessageDialog(this, 
                    "Fel vid sparning: " + ex.getMessage(),
                    "Fel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<String> loadUsedSeriesCodes() {
        Set<String> usedCodes = new HashSet<>();
        try {
            for (Map<String, Object> mapping : iService.getMappingsForYear(iYearId)) {
                if (iMode == Mode.EDIT && iExistingData != null) {
                    int mappingId = ((Number) mapping.get("id")).intValue();
                    if (mappingId == iExistingData.mappingId) {
                        continue;
                    }
                }
                String seriesCode = (String) mapping.get("series_code");
                if (seriesCode != null && !seriesCode.isEmpty()) {
                    usedCodes.add(seriesCode);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Failed to load used series codes", ex);
        }
        return usedCodes;
    }

    private void selectSeriesCode(String seriesCode) {
        if (seriesCode == null) {
            return;
        }
        for (int i = 0; i < iSeriesCodeCombo.getItemCount(); i++) {
            SeriesCodeOption option = iSeriesCodeCombo.getItemAt(i);
            if (option != null && seriesCode.equals(option.code)) {
                iSeriesCodeCombo.setSelectedIndex(i);
                if (option.enabled) {
                    iLastEnabledSelection = option;
                }
                return;
            }
        }
    }

    private void selectFirstEnabledSeriesCode() {
        for (int i = 0; i < iSeriesCodeCombo.getItemCount(); i++) {
            SeriesCodeOption option = iSeriesCodeCombo.getItemAt(i);
            if (option != null && option.enabled) {
                iSeriesCodeCombo.setSelectedIndex(i);
                iLastEnabledSelection = option;
                return;
            }
        }
        if (iSeriesCodeCombo.getItemCount() > 0) {
            iSeriesCodeCombo.setSelectedIndex(0);
        }
    }

    private void handleSeriesSelection() {
        if (iRestoringSelection) {
            return;
        }
        SeriesCodeOption selected = (SeriesCodeOption) iSeriesCodeCombo.getSelectedItem();
        if (selected == null) {
            updateOkButtonState();
            return;
        }
        if (!selected.enabled) {
            iRestoringSelection = true;
            try {
                if (iLastEnabledSelection != null) {
                    iSeriesCodeCombo.setSelectedItem(iLastEnabledSelection);
                }
            } finally {
                iRestoringSelection = false;
            }
            updateOkButtonState();
            return;
        }
        iLastEnabledSelection = selected;
        updateOkButtonState();
    }

    private void updateOkButtonState() {
        SeriesCodeOption selected = (SeriesCodeOption) iSeriesCodeCombo.getSelectedItem();
        iOkButton.setEnabled(selected != null && selected.enabled);
    }

    private static final class SeriesCodeOption {
        private final String code;
        private final boolean enabled;

        private SeriesCodeOption(String code, boolean enabled) {
            this.code = code;
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    private static final class SeriesCodeRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SeriesCodeOption) {
                SeriesCodeOption option = (SeriesCodeOption) value;
                label.setText(option.code);
                if (!option.enabled) {
                    label.setForeground(Color.GRAY);
                }
            }
            return label;
        }
    }

    public int showDialog() {
        setVisible(true);
        return iResultCode;
    }
}
