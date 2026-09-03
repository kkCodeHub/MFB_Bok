package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyValidationRules;
import se.swedsoft.bookkeeping.data.util.SSMailServer;
import se.swedsoft.bookkeeping.gui.company.panel.SSMailServerDialog;
import se.swedsoft.bookkeeping.gui.company.util.SSCompanyValidationUtils;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSIntegerTextField;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSImageFileChooser;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:14:40
 *
 * $Id$
 *
 */
public class SSCompanyPageAdditional extends SSCompanyPage {

    private SSNewCompany iCompany;

    private JPanel iPanel;
    private JTextField iContactPerson;
    private JTextField iPhone;
    private JTextField iPhone2;
    private JTextField iTelefax;
    private JTextField iEMail;
    private JTextField iWebAddress;
    private JTextField iBank;
    private JTextField iBankGiroNumber;
    private JTextField iPlusGiroNumber;
    private JTextField iSwiftCode;
    private JTextField iIBAN;
    private JTextField iSMTPAddress;
    private JCheckBox iRoundingOff;
    private SSIntegerTextField iVatPeriod;
    private JButton iEditMailServerButton;
    private JTextField severField;
    private JTextField iSwishImage;
    private JButton iBrowseForSwishButton;
    private JButton iClearSwishButton;
    private JTextField iTextSwish;

    private SSMailServer iMailServer;

    private SSMailServerDialog iMailDialog;
    private JComponent iFirstInvalidComponent;

    /**
     * @param iDialog
     */
    public SSCompanyPageAdditional(JDialog iDialog) {
        super(iDialog);

        iMailDialog = new SSMailServerDialog(iDialog);
        iBrowseForSwishButton.addActionListener(
            e -> {

                SSImageFileChooser iFileChooser = SSImageFileChooser.getInstance();

                if (iFileChooser.showDialog(iBrowseForSwishButton)
                    != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                iSwishImage.setText(iFileChooser.getSelectedFile().getAbsolutePath());

            });
        iClearSwishButton.addActionListener(e -> iSwishImage.setText(""));

        setupDirectValidation();

        addKeyListeners();
    }

    private void setupDirectValidation() {
        SSCompanyValidationUtils.installMaxLengthFilter(iContactPerson, SSCompanyValidationRules.CONTACT_PERSON_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iPhone, SSCompanyValidationRules.PHONE_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iPhone2, SSCompanyValidationRules.PHONE2_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iTelefax, SSCompanyValidationRules.TELEFAX_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iEMail, SSCompanyValidationRules.EMAIL_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iWebAddress, SSCompanyValidationRules.WEB_ADDRESS_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iBank, SSCompanyValidationRules.BANK_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iBankGiroNumber, SSCompanyValidationRules.BANK_ACCOUNT_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iPlusGiroNumber, SSCompanyValidationRules.PLUSGIRO_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iIBAN, SSCompanyValidationRules.IBAN_MAX_LENGTH);
        SSCompanyValidationUtils.installSwiftFilter(iSwiftCode, SSCompanyValidationRules.SWIFT_MAX_LENGTH);
        SSCompanyValidationUtils.installMaxLengthFilter(iSwishImage, SSCompanyValidationRules.SWISH_IMAGE_MAX_LENGTH);
    }

    /**
     * @return the name and title
     */
    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.additional");
    }

    /**
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

        iPhone.setText(iCompany.getPhone());
        iPhone2.setText(iCompany.getPhone2());
        iTelefax.setText(iCompany.getTelefax());
        iEMail.setText(iCompany.getEMail());
        iWebAddress.setText(iCompany.getHomepage());
        iContactPerson.setText(iCompany.getContactPerson());
        iBank.setText(iCompany.getBank());
        iBankGiroNumber.setText(iCompany.getBankGiroNumber());
        iPlusGiroNumber.setText(iCompany.getPlusGiroNumber());
        iSwiftCode.setText(iCompany.getBIC());
        iIBAN.setText(iCompany.getIBAN());
        // iSMTPAddress.setText(iCompany.getSMTP());
        iRoundingOff.setSelected(iCompany.isRoundingOff());
        iVatPeriod.setValue(iCompany.getVatPeriod());
        iSwishImage.setText(iCompany.getSwishImagePath());
        iTextSwish.setText(iCompany.getSwishText() != null ? iCompany.getSwishText() : "");
        setMailServer(iCompany.getMailServer());
    }

    private void setMailServer(SSMailServer server) {
        iMailServer = server;
        if (iMailServer != null) {
            severField.setText(iMailServer.getURI().getHost());
        } else {
            severField.setText("");
        }
    }

    /**
     * Get the edited company
     *
     * @return the company
     */
    @Override
    public SSNewCompany getCompany() {
        iCompany.setPhone(iPhone.getText());
        iCompany.setPhone2(iPhone2.getText());
        iCompany.setTelefax(iTelefax.getText());
        iCompany.setHomepage(iWebAddress.getText());
        iCompany.setEMail(iEMail.getText());
        iCompany.setContactPerson(iContactPerson.getText());
        iCompany.setBank(iBank.getText());
        iCompany.setBankGiroNumber(iBankGiroNumber.getText());
        iCompany.setPlusGiroNumber(iPlusGiroNumber.getText());
        iCompany.setBIC(iSwiftCode.getText());
        iCompany.setIBAN(iIBAN.getText());
        // iCompany.setSMTP(iSMTPAddress.getText());
        iCompany.setRoundingOff(iRoundingOff.isSelected());
        iCompany.setVatPeriod(iVatPeriod.getValue());
        iCompany.setSwishImagePath(iSwishImage.getText());
        iCompany.setSwishText(iTextSwish.getText());

        iCompany.setMailServer(iMailServer);

        return iCompany;
    }

    /**
     * Adds listeners for going to next field when enter key is pressed.
     */
    public void addKeyListeners() {

        SwingUtilities.invokeLater(() -> iContactPerson.requestFocusInWindow());

        iEditMailServerButton.addActionListener(e -> setMailServer(iMailDialog.showServerQuery(iMailServer)));

        iContactPerson.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iPhone.requestFocusInWindow());
                }
            }
        });

        iPhone.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iPhone2.requestFocusInWindow());
                }
            }
        });

        iPhone2.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iTelefax.requestFocusInWindow());
                }
            }
        });

        iTelefax.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iEMail.requestFocusInWindow());
                }
            }
        });

        iEMail.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iWebAddress.requestFocusInWindow());
                }
            }
        });

        iWebAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iEditMailServerButton.requestFocusInWindow());
                }
            }
        });

        // iSMTPAddress removed and replaced by the editMailServer button.
        // iWebAddress.addKeyListener(new KeyAdapter() {
        // public void keyPressed(KeyEvent e) {
        // if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        // SwingUtilities.invokeLater(new Runnable() {
        // public void run() {
        // iSMTPAddress.requestFocusInWindow();
        // }
        // });
        // }
        // }
        // });

        // iSMTPAddress removed and replaced by the editMailServer button.
        // iSMTPAddress.addKeyListener(new KeyAdapter() {
        // public void keyPressed(KeyEvent e) {
        // if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        // SwingUtilities.invokeLater(new Runnable() {
        // public void run() {
        // iBank.requestFocusInWindow();
        // }
        // });
        // }
        // }
        // });

        iBank.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iBankGiroNumber.requestFocusInWindow());
                }
            }
        });

        iBankGiroNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iPlusGiroNumber.requestFocusInWindow());
                }
            }
        });

        iPlusGiroNumber.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iSwiftCode.requestFocusInWindow());
                }
            }
        });

        iSwiftCode.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iIBAN.requestFocusInWindow());
                }
            }
        });

    }

    @Override
    public List<String> validatePage() {
        List<String> iErrors = new ArrayList<>();
        iFirstInvalidComponent = null;

        validateLength(iErrors, iContactPerson, "Additional: Contact person", SSCompanyValidationRules.CONTACT_PERSON_MAX_LENGTH);
        validateLength(iErrors, iPhone, "Additional: Phone", SSCompanyValidationRules.PHONE_MAX_LENGTH);
        validateLength(iErrors, iPhone2, "Additional: Phone 2", SSCompanyValidationRules.PHONE2_MAX_LENGTH);
        validateLength(iErrors, iTelefax, "Additional: Telefax", SSCompanyValidationRules.TELEFAX_MAX_LENGTH);
        validateLength(iErrors, iEMail, "Additional: Email", SSCompanyValidationRules.EMAIL_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidEmail(iEMail.getText())) {
            iErrors.add("Additional: E-postadressen har ogiltigt format. Förväntat format: namn@domän.tld");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iEMail;
            }
        }

        validateLength(iErrors, iWebAddress, "Additional: Web address", SSCompanyValidationRules.WEB_ADDRESS_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidWebAddress(iWebAddress.getText())) {
            iErrors.add("Additional: Webbadress ska börja med http://, https:// eller www., t.ex. https://www.foretaget.se");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iWebAddress;
            }
        }

        validateLength(iErrors, iBank, "Additional: Bank", SSCompanyValidationRules.BANK_MAX_LENGTH);
        validateLength(iErrors, iBankGiroNumber, "Additional: Bank account", SSCompanyValidationRules.BANK_ACCOUNT_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidBankgiro(iBankGiroNumber.getText())) {
            iErrors.add("Additional: Bankgiro ska ha formatet NNN-NNNN eller NNNN-NNNN (7-8 siffror), med eller utan bindestreck.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iBankGiroNumber;
            }
        }

        validateLength(iErrors, iPlusGiroNumber, "Additional: Plusgiro", SSCompanyValidationRules.PLUSGIRO_MAX_LENGTH);
        if (!SSCompanyValidationRules.isValidPlusgiro(iPlusGiroNumber.getText())) {
            iErrors.add("Additional: Postgiro/Plusgiro ska vara 2-8 siffror, med eller utan bindestreck.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iPlusGiroNumber;
            }
        }

        validateLength(iErrors, iIBAN, "Additional: IBAN", SSCompanyValidationRules.IBAN_MAX_LENGTH);
        validateLength(iErrors, iSwiftCode, "Additional: SWIFT", SSCompanyValidationRules.SWIFT_MAX_LENGTH);
        validateLength(iErrors, iSwishImage, "Additional: Swish image", SSCompanyValidationRules.SWISH_IMAGE_MAX_LENGTH);
        SSCompanyValidationRules.validateSwishImageFileName(iCompany.getName(), iCompany.getCorporateID(), iSwishImage.getText())
                .ifPresent(iMessage -> {
                    iErrors.add(iMessage);
                    if (iFirstInvalidComponent == null) {
                        iFirstInvalidComponent = iSwishImage;
                    }
                });

        if (!SSCompanyValidationRules.isSwiftCharactersValid(iSwiftCode.getText())) {
            iErrors.add("Additional: SWIFT may only contain letters A-Z and digits 0-9.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iSwiftCode;
            }
        }

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

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPageAdditional");
        sb.append("{iBank=").append(iBank);
        sb.append(", iBankGiroNumber=").append(iBankGiroNumber);
        sb.append(", iCompany=").append(iCompany);
        sb.append(", iContactPerson=").append(iContactPerson);
        sb.append(", iEditMailServerButton=").append(iEditMailServerButton);
        sb.append(", iEMail=").append(iEMail);
        sb.append(", iIBAN=").append(iIBAN);
        sb.append(", iMailDialog=").append(iMailDialog);
        sb.append(", iMailServer=").append(iMailServer);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iPhone=").append(iPhone);
        sb.append(", iPhone2=").append(iPhone2);
        sb.append(", iPlusGiroNumber=").append(iPlusGiroNumber);
        sb.append(", iRoundingOff=").append(iRoundingOff);
        sb.append(", iSMTPAddress=").append(iSMTPAddress);
        sb.append(", iSwiftCode=").append(iSwiftCode);
        sb.append(", iTelefax=").append(iTelefax);
        sb.append(", iVatPeriod=").append(iVatPeriod);
        sb.append(", iWebAddress=").append(iWebAddress);
        sb.append(", severField=").append(severField);
        sb.append(", iSwishImage=").append(iSwishImage);
        sb.append('}');
        return sb.toString();
    }
}
