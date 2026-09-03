package se.swedsoft.bookkeeping.gui.company.pages;


import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.gui.util.SSBundle;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-aug-25
 * Time: 10:14:40
 */
public class SSCompanyPageAutoIncrement extends SSCompanyPage implements ChangeListener {

    private SSNewCompany iCompany;

    private JPanel iPanel;

    private JSpinner iInvoice;
    private JSpinner iOrder;
    private JSpinner iTender;
    private JSpinner iOutpayment;
    private JSpinner iSupplierCreditInvoice;
    private JSpinner iSupplierInvoice;
    private JSpinner iInpayment;
    private JSpinner iCreditInvoice;
    private JSpinner iPurchaseOrder;
    private JComponent iFirstInvalidComponent;

    /**
     *
     * @param iDialog
     */
    public SSCompanyPageAutoIncrement(JDialog iDialog) {
        super(iDialog);

        iInvoice.setEditor(new JSpinner.NumberEditor(iInvoice, "0"));
        iOrder.setEditor(new JSpinner.NumberEditor(iOrder, "0"));
        iTender.setEditor(new JSpinner.NumberEditor(iTender, "0"));
        iOutpayment.setEditor(new JSpinner.NumberEditor(iOutpayment, "0"));
        iSupplierCreditInvoice.setEditor(
                new JSpinner.NumberEditor(iSupplierCreditInvoice, "0"));
        iSupplierInvoice.setEditor(new JSpinner.NumberEditor(iSupplierInvoice, "0"));
        iInpayment.setEditor(new JSpinner.NumberEditor(iInpayment, "0"));
        iCreditInvoice.setEditor(new JSpinner.NumberEditor(iCreditInvoice, "0"));
        iPurchaseOrder.setEditor(new JSpinner.NumberEditor(iPurchaseOrder, "0"));

        iInvoice.addChangeListener(this);
        iOrder.addChangeListener(this);
        iTender.addChangeListener(this);
        iOutpayment.addChangeListener(this);
        iSupplierCreditInvoice.addChangeListener(this);
        iSupplierInvoice.addChangeListener(this);
        iInpayment.addChangeListener(this);
        iCreditInvoice.addChangeListener(this);
        iPurchaseOrder.addChangeListener(this);

        addKeyListeners();
    }

    /**
     *
     * @return the name and title
     */
    @Override
    public String getName() {
        return SSBundle.getBundle().getString("companyframe.pages.autoincrement");
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

        iInvoice.setValue(iCompany.getAutoIncrement().getNumber("invoice"));
        iOrder.setValue(iCompany.getAutoIncrement().getNumber("order"));
        iTender.setValue(iCompany.getAutoIncrement().getNumber("tender"));

        iOutpayment.setValue(iCompany.getAutoIncrement().getNumber("outpayment"));
        iSupplierCreditInvoice.setValue(
                iCompany.getAutoIncrement().getNumber("suppliercreditinvoice"));
        iSupplierInvoice.setValue(iCompany.getAutoIncrement().getNumber("supplierinvoice"));
        iInpayment.setValue(iCompany.getAutoIncrement().getNumber("inpayment"));
        iCreditInvoice.setValue(iCompany.getAutoIncrement().getNumber("creditinvoice"));
        iPurchaseOrder.setValue(iCompany.getAutoIncrement().getNumber("purchaseorder"));

    }

    /**
     * Get the edited company
     *
     * @return the company
     */
    @Override
    public SSNewCompany getCompany() {

        iCompany.getAutoIncrement().setNumber("invoice",
                ((Number) iInvoice.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("order",
                ((Number) iOrder.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("tender",
                ((Number) iTender.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("outpayment",
                ((Number) iOutpayment.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("suppliercreditinvoice",
                ((Number) iSupplierCreditInvoice.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("supplierinvoice",
                ((Number) iSupplierInvoice.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("inpayment",
                ((Number) iInpayment.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("creditinvoice",
                ((Number) iCreditInvoice.getValue()).intValue());
        iCompany.getAutoIncrement().setNumber("purchaseorder",
                ((Number) iPurchaseOrder.getValue()).intValue());

        return iCompany;
    }

    /**
     * Invoked when the target of the listener has changed its state.
     *
     * @param e a ChangeEvent object
     */
    public void stateChanged(ChangeEvent e) {
        JSpinner iSpinner = (JSpinner) e.getSource();

        Number iNumber = (Number) iSpinner.getValue();

        if (iNumber.intValue() < 0) {
            iSpinner.setValue(0);
        }
    }

    public void addKeyListeners() {
        SwingUtilities.invokeLater(() -> iTender.requestFocusInWindow());

        iTender.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iOrder.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iOrder.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iInvoice.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iInvoice.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iCreditInvoice.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iCreditInvoice.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> iInpayment.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iInpayment.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iPurchaseOrder.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iPurchaseOrder.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iSupplierInvoice.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iSupplierInvoice.getEditor().getComponent(0).addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iSupplierCreditInvoice.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });

        iSupplierCreditInvoice.getEditor().getComponent(0).addKeyListener(
                new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(
                            () -> iOutpayment.getEditor().getComponent(0).requestFocusInWindow());
                }
            }
        });
    }

    @Override
    public List<String> validatePage() {
        List<String> iErrors = new ArrayList<>();
        iFirstInvalidComponent = null;

        validateSpinner(iErrors, iTender, "Auto increment: Tender");
        validateSpinner(iErrors, iOrder, "Auto increment: Order");
        validateSpinner(iErrors, iInvoice, "Auto increment: Invoice");
        validateSpinner(iErrors, iCreditInvoice, "Auto increment: Credit invoice");
        validateSpinner(iErrors, iInpayment, "Auto increment: Inpayment");
        validateSpinner(iErrors, iPurchaseOrder, "Auto increment: Purchase order");
        validateSpinner(iErrors, iSupplierInvoice, "Auto increment: Supplier invoice");
        validateSpinner(iErrors, iSupplierCreditInvoice, "Auto increment: Supplier credit invoice");
        validateSpinner(iErrors, iOutpayment, "Auto increment: Outpayment");
        return iErrors;
    }

    @Override
    public JComponent getFirstInvalidComponent() {
        return iFirstInvalidComponent;
    }

    private void validateSpinner(List<String> iErrors, JSpinner iSpinner, String iLabel) {
        int iValue = ((Number) iSpinner.getValue()).intValue();
        if (iValue < 0) {
            iErrors.add(iLabel + " must be 0 or greater.");
            if (iFirstInvalidComponent == null) {
                iFirstInvalidComponent = iSpinner;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.company.pages.SSCompanyPageAutoIncrement");
        sb.append("{iCompany=").append(iCompany);
        sb.append(", iCreditInvoice=").append(iCreditInvoice);
        sb.append(", iInpayment=").append(iInpayment);
        sb.append(", iInvoice=").append(iInvoice);
        sb.append(", iOrder=").append(iOrder);
        sb.append(", iOutpayment=").append(iOutpayment);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iPurchaseOrder=").append(iPurchaseOrder);
        sb.append(", iSupplierCreditInvoice=").append(iSupplierCreditInvoice);
        sb.append(", iSupplierInvoice=").append(iSupplierInvoice);
        sb.append(", iTender=").append(iTender);
        sb.append('}');
        return sb.toString();
    }
}
