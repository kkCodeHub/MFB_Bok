package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.common.*;
import se.swedsoft.bookkeeping.data.system.SSCompanyValidationRules;
import se.swedsoft.bookkeeping.gui.company.util.SSCompanyValidationUtils;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSCurrencyTextField;
import se.swedsoft.bookkeeping.gui.util.components.SSEditableTableComboBox;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSImageFileChooser;
import se.swedsoft.bookkeeping.gui.util.model.*;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:14:40
 */
public class SSCompanyPageGeneral extends SSCompanyPage {

    private SSNewCompany iCompany;

    private JPanel iPanel;

    private JButton iBrowseForLogoButton;

    private JTextField iLogotype;

    private SSEditableTableComboBox<SSCurrency> iCurrency;
    private JCheckBox iTaxRegistered;
    private JTextField iWeightUnit;
    private JTextField iVolumeUnit;
    private JTextField iEstimatedDelivery;
    private SSCurrencyTextField iReminderfee;
    private SSCurrencyTextField iDelayintrest;
    private JTextField iVATNumber;
    private JTextField iResidence;
    private JTextField iCorporateID;
    private JTextField iName;
    private SSEditableTableComboBox<SSPaymentTerm> iPaymentTerm;
    private SSEditableTableComboBox<SSDeliveryTerm> iDeliveryTerm;
    private SSEditableTableComboBox<SSDeliveryWay> iDeliveryWay;
    private SSEditableTableComboBox<SSUnit> iStandardUnit;
    private JButton iClearLogoButton;
    private JComponent iFirstInvalidComponent;

    /**
     *
     * @param iDialog
     */
    public SSCompanyPageGeneral(JDialog iDialog) {
        super(iDialog);

        iCurrency.getComboBox().setModel(SSCurrencyTableModel.getDropDownModel());
        iCurrency.getComboBox().setSearchColumns(0);
        iCurrency.setEditingFactory(SSCurrencyTableModel.getEditingFactory(iDialog));

        iPaymentTerm.getComboBox().setModel(SSPaymentTermTableModel.getDropDownModel());
        iPaymentTerm.getComboBox().setSearchColumns(0);
        iPaymentTerm.setEditingFactory(SSPaymentTermTableModel.getEditingFactory(iDialog));

        iDeliveryTerm.getComboBox().setModel(SSDeliveryTermTableModel.getDropDownModel());
        iDeliveryTerm.getComboBox().setSearchColumns(0);
        iDeliveryTerm.setEditingFactory(
                SSDeliveryTermTableModel.getEditingFactory(iDialog));

        iDeliveryWay.getComboBox().setModel(SSDeliveryWayTableModel.getDropDownModel());
        iDeliveryWay.getComboBox().setSearchColumns(0);
        iDeliveryWay.setEditingFactory(SSDeliveryWayTableModel.getEditingFactory(iDialog));

        iStandardUnit.getComboBox().setModel(SSUnitTableModel.getDropDownModel());
        iStandardUnit.getComboBox().setSearchColumns(0);
        iStandardUnit.setEditingFactory(SSUnitTableModel.getEditingFactory(iDialog));

        iBrowseForLogoButton.addActionListener(
                e -> {

                        SSImageFileChooser iFileChooser = SSImageFileChooser.getInstance();

                        if (iFileChooser.showDialog(iBrowseForLogoButton)
                                != JFileChooser.APPROVE_OPTION) {
                            return;
                        }

                        iLogotype.setText(iFileChooser.getSelectedFile().getAbsolutePath());

                    });
        iClearLogoButton.addActionListener(e -> iLogotype.setText(""));

        setupDirectValidation();

        addKeyListeners();
    }

    private void setupDirectValidation() {
        SSCompanyValidationUtils.installMaxLengthFilter(iName, SSCompanyValidationRules.NAME_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iResidence, SSCompanyValidationRules.RESIDENCE_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iCorporateID, SSCompanyValidationRules.CORPORATE_ID_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iVATNumber, SSCompanyValidationRules.VAT_NUMBER_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iLogotype, SSCompanyValidationRules.LOGOTYPE_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iEstimatedDelivery, SSCompanyValidationRules.ESTIMATED_DELIVERY_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iVolumeUnit, SSCompanyValidationRules.VOLUME_UNIT_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iWeightUnit, SSCompanyValidationRules.WEIGHT_UNIT_MAX_LENGTH);
    }

    /**
     *
     * @return the name and title
     */
    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.general");
    }

    /**
     *
     * @return the panel
     */
    @Override
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     * Set the company to edit
     *
     * @param iCompany
     */
    @Override
    public void setCompany(SSNewCompany iCompany) {
        this.iCompany = iCompany;
        iName.setText(iCompany.getName());
        iResidence.setText(iCompany.getResidence());
        iTaxRegistered.setSelected(iCompany.getTaxRegistered());
        iCorporateID.setText(iCompany.getCorporateID());
        iLogotype.setText(iCompany.getLogotype());
        iVATNumber.setText(iCompany.getVATNumber());
        iDelayintrest.setValue(iCompany.getDelayInterest());
        iReminderfee.setValue(iCompany.getReminderfee());
        iCurrency.setSelected(iCompany.getCurrency());
        iEstimatedDelivery.setText(iCompany.getEstimatedDelivery());
        iVolumeUnit.setText(iCompany.getVolumeUnit());
        iWeightUnit.setText(iCompany.getWeightUnit());
        iStandardUnit.setSelected(iCompany.getStandardUnit());
        iPaymentTerm.setSelected(iCompany.getPaymentTerm());
        iDeliveryTerm.setSelected(iCompany.getDeliveryTerm());
        iDeliveryWay.setSelected(iCompany.getDeliveryWay());
    }

    /**
     * Get the edited company
     *
     * @return the company
     */
    @Override
    public SSNewCompany getCompany() {
        iCompany.setName(iName.getText());
        iCompany.setResidence(iResidence.getText());
        iCompany.setTaxRegistered(iTaxRegistered.isSelected());
        iCompany.setCorporateID(iCorporateID.getText());
        iCompany.setLogotype(iLogotype.getText());
        iCompany.setVATNumber(iVATNumber.getText());
        iCompany.setDelayInterest(iDelayintrest.getValue());
        iCompany.setReminderfee(iReminderfee.getValue());
        iCompany.setCurrency(iCurrency.getSelected());
        iCompany.setEstimatedDelivery(iEstimatedDelivery.getText());
        iCompany.setVolumeUnit(iVolumeUnit.getText());
        iCompany.setWeightUnit(iWeightUnit.getText());
        iCompany.setStandardUnit(iStandardUnit.getSelected());
        iCompany.setPaymentTerm(iPaymentTerm.getSelected());
        iCompany.setDeliveryTerm(iDeliveryTerm.getSelected());
        iCompany.setDeliveryWay(iDeliveryWay.getSelected());

        return iCompany;
    }

    public void addKeyListeners() {

        SwingUtilities.invokeLater(() -> iName.requestFocusInWindow());

        iName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iResidence.requestFocusInWindow());
                }
            }
        });

        iResidence.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iCorporateID.requestFocusInWindow());
                }
            }
        });

        iCorporateID.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iVATNumber.requestFocusInWindow());
                }
            }
        });

        iVATNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iDelayintrest.requestFocusInWindow());
                }
            }
        });

        iDelayintrest.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iReminderfee.requestFocusInWindow());
                }
            }
        });

        iReminderfee.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iPaymentTerm.getComboBox().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iPaymentTerm.getComboBox().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iDeliveryTerm.getComboBox().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iDeliveryTerm.getComboBox().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iDeliveryWay.getComboBox().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iDeliveryWay.getComboBox().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iCurrency.getComboBox().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iCurrency.getComboBox().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iEstimatedDelivery.requestFocusInWindow());
                }
            }
        });

        iEstimatedDelivery.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iVolumeUnit.requestFocusInWindow());
                }
            }
        });

        iVolumeUnit.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iWeightUnit.requestFocusInWindow());
                }
            }
        });

        iWeightUnit.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iStandardUnit.getComboBox().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iStandardUnit.getComboBox().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iTaxRegistered.requestFocusInWindow());
                }
            }
        });
    }

    @Override
    public List<String> validatePage() {
        List<String> iErrors = new ArrayList<>();
        iFirstInvalidComponent = null;

        if (iName.getText() == null || iName.getText().trim().isEmpty()) {
            iErrors.add("General: Company name is required.");
            iFirstInvalidComponent = iName;
        }

        validateLength(iErrors, iName, "General: Company name", SSCompanyValidationRules.NAME_MAX_LENGTH);
        validateLength(iErrors, iResidence, "General: Residence", SSCompanyValidationRules.RESIDENCE_MAX_LENGTH);
        validateLength(iErrors, iCorporateID, "General: Corporate ID", SSCompanyValidationRules.CORPORATE_ID_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidSwedishCorporateId(iCorporateID.getText())) {
            iErrors.add("General: Org.nr/personnummer ska vara ett giltigt organisationsnummer eller personnummer.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iCorporateID;
            }
        }

        validateLength(iErrors, iVATNumber, "General: VAT number", SSCompanyValidationRules.VAT_NUMBER_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidSwedishVatNumber(iVATNumber.getText())) {
            iErrors.add("General: Momsnr ska ha formatet SE följt av 12 siffror, t.ex. SE556677889901.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iVATNumber;
            }
        }

        validateLength(iErrors, iLogotype, "General: Logotype", SSCompanyValidationRules.LOGOTYPE_MAX_LENGTH);
        validateLength(iErrors, iEstimatedDelivery, "General: Estimated delivery", SSCompanyValidationRules.ESTIMATED_DELIVERY_MAX_LENGTH);
        validateLength(iErrors, iWeightUnit, "General: Weight unit", SSCompanyValidationRules.WEIGHT_UNIT_MAX_LENGTH);
        validateLength(iErrors, iVolumeUnit, "General: Volume unit", SSCompanyValidationRules.VOLUME_UNIT_MAX_LENGTH);

        return iErrors;
    }

    @Override
    public JComponent getFirstInvalidComponent() {
        return iFirstInvalidComponent;
    }

    private void validateLength(List<String> iErrors, JTextField iField, String iLabel, int iMaxLength) {
        String iValue = iField.getText();
        if (iValue != null && iValue.length() > iMaxLength) {
            iErrors.add(iLabel + " must be at most " + iMaxLength + " characters.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iField;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPageGeneral");
        sb.append("{iBrowseForLogoButton=").append(iBrowseForLogoButton);
        sb.append(", iCompany=").append(iCompany);
        sb.append(", iCorporateID=").append(iCorporateID);
        sb.append(", iCurrency=").append(iCurrency);
        sb.append(", iDelayintrest=").append(iDelayintrest);
        sb.append(", iDeliveryTerm=").append(iDeliveryTerm);
        sb.append(", iDeliveryWay=").append(iDeliveryWay);
        sb.append(", iEstimatedDelivery=").append(iEstimatedDelivery);
        sb.append(", iLogotype=").append(iLogotype);
        sb.append(", iName=").append(iName);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iPaymentTerm=").append(iPaymentTerm);
        sb.append(", iReminderfee=").append(iReminderfee);
        sb.append(", iResidence=").append(iResidence);
        sb.append(", iStandardUnit=").append(iStandardUnit);
        sb.append(", iTaxRegistered=").append(iTaxRegistered);
        sb.append(", iVATNumber=").append(iVATNumber);
        sb.append(", iVolumeUnit=").append(iVolumeUnit);
        sb.append(", iWeightUnit=").append(iWeightUnit);
        sb.append('}');
        return sb.toString();
    }
}
