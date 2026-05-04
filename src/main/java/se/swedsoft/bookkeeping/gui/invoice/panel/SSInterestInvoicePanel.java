package se.swedsoft.bookkeeping.gui.invoice.panel;


import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInterestInvoiceTableModel;
import se.swedsoft.bookkeeping.gui.util.SSSelectionListener;
import se.swedsoft.bookkeeping.gui.util.components.SSButton;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.model.SSAccountTableModel;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-apr-12
 * Time: 10:30:06
 */
public class SSInterestInvoicePanel {
    private JPanel iPanel;
    private SSButton iCancelButton;
    private SSButton iOkButton;
    private SSTable iTable;

    private JTextField iDescription;

    private SSTableComboBox<SSAccount> iAccount;

    private SSInterestInvoiceTableModel iModel;
    private JTextField iAccountText;

    public SSInterestInvoicePanel(List<SSInvoice> iRows) {
        iModel = new SSInterestInvoiceTableModel(iRows);

        iTable.setModel(iModel);

        SSAccount iSelected = SSDB.getInstance().getCurrentCompany().getDefaultAccount(
                SSDB.getInstance().getCurrentAccountPlan(),
                SSDefaultAccount.InterestProfit).orElse(null);

        iAccount.setModel(SSAccountTableModel.getDropDownModel());
        iAccount.setSearchColumns(0);
        iAccount.setAllowCustomValues(true);
        iAccount.addSelectionListener(new SSSelectionListener<>() {
            public void selected(SSAccount selected) {
                if (selected != null) {
                    iAccountText.setText(selected.getDescription());
                } else {
                    iAccount.setText("");
                }
            }
        });
        iAccount.setSelected(iSelected, true);

        SSInterestInvoiceTableModel.setupTable(iTable);
    }

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     *
     * @param pActionListener
     */

    public void addOkAction(ActionListener pActionListener) {
        iOkButton.addActionListener(pActionListener);
    }

    /**
     *
     * @param pActionListener
     */
    public void addCancelAction(ActionListener pActionListener) {
        iCancelButton.addActionListener(pActionListener);
    }

    /**
     *
     * @return
     */
    public List<SSInvoice> getInterestInvoices() {
        String    iDescription = this.iDescription.getText();
        SSAccount iAccount = this.iAccount.getSelected();

        return iModel.getInterestInvoices(iDescription, iAccount);
    }

    /**
     *
     * @return
     */
    public static List<SSInvoice> getRows() {
        // Get all invoices from the DB
        List<SSInvoice> iInvoices = SSDB.getInstance().getInvoices();

        // Get all the rows
        List<SSInvoice> iRows = new LinkedList<>();

        for (SSInvoice iInvoice : iInvoices) {
            // Skip invoices that is flagged as interest invoiced or is a cash sales
            if (iInvoice.isInterestInvoiced() || iInvoice.getType() == SSInvoiceType.CASH) {
                continue;
            }

            BigDecimal iSaldo = SSInvoiceMath.getSaldo(iInvoice.getNumber());

            Date iLastInpayment = SSInpaymentMath.getLastInpaymentForInvoice(iInvoice);

            if (iLastInpayment == null) {
                iLastInpayment = SSDateUtil.toDate(SSDateUtil.today());
            }

            LocalDate lastInpaymentDate = SSDateUtil.toLocalDate(iLastInpayment);
            LocalDate dueDate = iInvoice.getLocalDueDate();

            if (iSaldo.signum() == 0 && dueDate != null && lastInpaymentDate != null
                    && lastInpaymentDate.isAfter(dueDate)) {
                iRows.add(iInvoice);
            }
        }

        return iRows;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.invoice.panel.SSInterestInvoicePanel");
        sb.append("{iAccount=").append(iAccount);
        sb.append(", iAccountText=").append(iAccountText);
        sb.append(", iCancelButton=").append(iCancelButton);
        sb.append(", iDescription=").append(iDescription);
        sb.append(", iModel=").append(iModel);
        sb.append(", iOkButton=").append(iOkButton);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTable=").append(iTable);
        sb.append('}');
        return sb.toString();
    }
}
