package se.swedsoft.bookkeeping.gui.util.dialogs;


import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSIntegerTextField;

import javax.swing.*;


/**
 * Dialog for editing payment terms.
 */
public class SSPaymentTermDialog extends SSDialog {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private JTextField iDescription;

    private JTextField iName;

    private SSIntegerTextField iDays;

    /**
     * @param iFrame owner frame
     */
    public SSPaymentTermDialog(JFrame iFrame) {
        super(iFrame, SSBundle.getBundle().getString("paymenttermdialog.title"));

        setPanel(iPanel);

        iButtonPanel.addOkActionListener(e -> closeDialog(JOptionPane.OK_OPTION));

        iButtonPanel.addCancelActionListener(e -> closeDialog(JOptionPane.CANCEL_OPTION));

        getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        setLocationRelativeTo(iFrame);
    }

    /**
     * @param iDialog owner dialog
     */
    public SSPaymentTermDialog(JDialog iDialog) {
        super(iDialog, SSBundle.getBundle().getString("paymenttermdialog.title"));

        setPanel(iPanel);

        iButtonPanel.addOkActionListener(e -> closeDialog(JOptionPane.OK_OPTION));

        iButtonPanel.addCancelActionListener(e -> closeDialog(JOptionPane.CANCEL_OPTION));
        setLocationRelativeTo(iDialog);
    }

    @Override
    public String getName() {
        return iName.getText();
    }

    @Override
    public void setName(String iName) {
        this.iName.setText(iName);
    }

    public String getDescription() {
        return iDescription.getText();
    }

    public void setDescription(String iDescription) {
        this.iDescription.setText(iDescription);
    }

    public Integer getDays() {
        Integer value = iDays.getValue();
        return value != null ? value : 0;
    }

    public void setDays(Integer iValue) {
        iDays.setValue(iValue != null ? iValue : 0);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.dialogs.SSPaymentTermDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iDays=").append(iDays);
        sb.append(", iDescription=").append(iDescription);
        sb.append(", iName=").append(iName);
        sb.append(", iPanel=").append(iPanel);
        sb.append('}');
        return sb.toString();
    }
}
