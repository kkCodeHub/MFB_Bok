package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.gui.company.panel.SSStandardTextPanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;

import javax.swing.*;
import java.util.ResourceBundle;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:14:40
 */
public class SSCompanyPageStandardText extends SSCompanyPage {

    private static final ResourceBundle BUNDLE = SSBundle.getBundle();

    private SSNewCompany iCompany;

    private JPanel iPanel;

    private SSStandardTextPanel iStandardTextPanel;
    private JLabel iCustomerInvoiceTextboxLabel;
    private JComboBox<CustomerInvoiceTextboxOption> iCustomerInvoiceTextboxComboBox;

    /**
     * @param iDialog
     */
    public SSCompanyPageStandardText(JDialog iDialog) {
        super(iDialog);

        iCustomerInvoiceTextboxLabel.setText(getBundleText(
                "companypanel.standardtext.customerinvoice.textbox",
                "Kundfaktura textbox:"));
        iCustomerInvoiceTextboxComboBox.setModel(createCustomerInvoiceTextboxModel());
    }

    /**
     *
     * @return the name and title
     */
    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.standardtexts");
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

        iStandardTextPanel.setData(iCompany.getStandardTexts());
        setCustomerInvoiceTextboxValue(iCompany.getCustomerInvoiceTextbox());
    }

    /**
     * Get the edited company
     *
     * @return the company
     */
    @Override
    public SSNewCompany getCompany() {
        iStandardTextPanel.getData(iCompany.getStandardTexts());
        iCompany.setCustomerInvoiceTextbox(getCustomerInvoiceTextboxValue());

        return iCompany;
    }

    private DefaultComboBoxModel<CustomerInvoiceTextboxOption> createCustomerInvoiceTextboxModel() {
        DefaultComboBoxModel<CustomerInvoiceTextboxOption> iModel = new DefaultComboBoxModel<>();
        iModel.addElement(new CustomerInvoiceTextboxOption(
                getBundleText("companypanel.standardtext.customerinvoice.textbox.small", "Liten"), 0));
        iModel.addElement(new CustomerInvoiceTextboxOption(
                getBundleText("companypanel.standardtext.customerinvoice.textbox.medium", "Mellan"), 1));
        iModel.addElement(new CustomerInvoiceTextboxOption(
                getBundleText("companypanel.standardtext.customerinvoice.textbox.large", "Stor"), 2));
        return iModel;
    }

    private String getBundleText(String pKey, String pFallback) {
        return BUNDLE.containsKey(pKey) ? BUNDLE.getString(pKey) : pFallback;
    }

    private int getCustomerInvoiceTextboxValue() {
        CustomerInvoiceTextboxOption iOption =
                (CustomerInvoiceTextboxOption) iCustomerInvoiceTextboxComboBox.getSelectedItem();
        return iOption == null ? 0 : iOption.getValue();
    }

    private void setCustomerInvoiceTextboxValue(int pValue) {
        ComboBoxModel<CustomerInvoiceTextboxOption> iModel = iCustomerInvoiceTextboxComboBox.getModel();
        for (int iIndex = 0; iIndex < iModel.getSize(); iIndex++) {
            CustomerInvoiceTextboxOption iOption = iModel.getElementAt(iIndex);
            if (iOption.getValue() == pValue) {
                iCustomerInvoiceTextboxComboBox.setSelectedItem(iOption);
                return;
            }
        }
        iCustomerInvoiceTextboxComboBox.setSelectedIndex(0);
    }

    private static class CustomerInvoiceTextboxOption {

        private final String iDescription;
        private final int iValue;

        CustomerInvoiceTextboxOption(String pDescription, int pValue) {
            iDescription = pDescription;
            iValue = pValue;
        }

        int getValue() {
            return iValue;
        }

        @Override
        public String toString() {
            return iDescription;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPageStandardText");
        sb.append("{iCompany=").append(iCompany);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iStandardTextPanel=").append(iStandardTextPanel);
        sb.append('}');
        return sb.toString();
    }

}
