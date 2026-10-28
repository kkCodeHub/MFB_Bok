package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.calc.util.SSFilter;
import se.swedsoft.bookkeeping.calc.util.SSFilterFactory;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.customer.util.SSCustomerTableModel;
import se.swedsoft.bookkeeping.gui.invoice.util.SSInvoiceTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * $Id$
 *
 */
public class SSInpaymentListDialog extends SSDialog {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private JCheckBox iCheckDate;
    private JCheckBox iCheckCustomer;
    private JCheckBox iCheckInvoice;

    private SSTableComboBox<SSCustomer> iCustomer;
    private SSTableComboBox<SSInvoice> iInvoice;
    private SSDateChooser iToDate;
    private SSDateChooser iFromDate;

    /**
     *
     * @param iMainFrame
     */
    public SSInpaymentListDialog(SSMainFrame iMainFrame) {
        super(iMainFrame,
                SSBundle.getBundle().getString("inpaymentlistreport.dialog.title"));

        setPanel(iPanel);

        iButtonPanel.addCancelActionListener(e -> setModalResult(JOptionPane.CANCEL_OPTION, true));
        iButtonPanel.addOkActionListener(e -> setModalResult(JOptionPane.OK_OPTION, true));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        iCustomer.setModel(createCustomerDropDownModel());
        iCustomer.setSearchColumns(0);

        iInvoice.setModel(SSInvoiceTableModel.getDropDownModel());
        iInvoice.setSearchColumns(0);

        ChangeListener iChangeListener = e -> {

                iCustomer.setEnabled(iCheckCustomer.isSelected());
                iInvoice.setEnabled(iCheckInvoice.isSelected());

                iFromDate.setEnabled(iCheckDate.isSelected());
                iToDate.setEnabled(iCheckDate.isSelected());

            };

        iCheckDate.addChangeListener(iChangeListener);
        iCheckCustomer.addChangeListener(iChangeListener);
        iCheckInvoice.addChangeListener(iChangeListener);

        iChangeListener.stateChanged(null);
    }

    /**
     *
     * @return
     */
    public JPanel getPanel() {
        return iPanel;
    }

    /**
     * Returns the invoices to print depending on the user selections
     *
     * @return
     */
    public List<SSInpayment> getElementsToPrint() {
        List<SSInpayment> iInpayments = Repositories.inpayments().findAll();

        SSFilterFactory<SSInpayment> iFactory = new SSFilterFactory<>(
                iInpayments);

        // Filter by customer
        if (iCheckCustomer.isSelected() && iCustomer.getSelected() != null) {
            final SSCustomer iCustomer = this.iCustomer.getSelected();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSInpayment iInpayment) {
                    return SSInpaymentMath.hasCustomer(iInpayment, iCustomer);
                }
            });
        }

        // Filter by invoice
        if (iCheckInvoice.isSelected() && iInvoice.getSelected() != null) {
            final SSInvoice iInvoice = this.iInvoice.getSelected();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSInpayment iInpayment) {
                    return SSInpaymentMath.hasInvoice(iInpayment, iInvoice);
                }
            });

        }
        // Filter by date
        if (iCheckDate.isSelected()) {
            final LocalDate iDateFrom = iFromDate.getLocalDate();
            final LocalDate iDateTo = iToDate.getLocalDate();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSInpayment iInpayment) {
                    return SSInpaymentMath.inPeriod(iInpayment, iDateFrom, iDateTo);
                }
            });
        }

        return iFactory.getObjects();
    }

    /**
     *
     * @return
     */
    public boolean isDateSelected() {
        return iCheckDate.isSelected();
    }

    public boolean isCustomerSelected() {
        return iCheckCustomer.isSelected();
    }

    /**
     *
     * @return
     */
    public boolean isInvoiceSelected() {
        return iCheckInvoice.isSelected();
    }

    /**
     *
     * @return
     */
    public Date getDateFrom() {
        return SSDateUtil.toDate(getLocalDateFrom());
    }

    /**
     *
     * @return
     */
    public Date getDateTo() {
        return SSDateUtil.toDate(getLocalDateTo());
    }

    public LocalDate getLocalDateFrom() {
        return iFromDate.getLocalDate();
    }

    public LocalDate getLocalDateTo() {
        return iToDate.getLocalDate();
    }

    /**
     *
     * @return
     */
    public SSInvoice getInvoice() {
        return iInvoice.getSelected();
    }

    public SSCustomer getCustomer() {
        return iCustomer.getSelected();
    }

    private SSCustomerTableModel createCustomerDropDownModel() {
        SSCustomerTableModel iModel = new SSCustomerTableModel(getCustomersWithInpayments());

        iModel.addColumn(SSCustomerTableModel.COLUMN_NUMBER);
        iModel.addColumn(SSCustomerTableModel.COLUMN_NAME);

        return iModel;
    }

    private List<SSCustomer> getCustomersWithInpayments() {
        List<SSInpayment> iInpayments = Repositories.inpayments().findAll();
        List<SSCustomer> iCustomers = se.swedsoft.bookkeeping.data.system.SSSalesContext.getCustomers();
        Set<SSCustomer> iCustomersWithInpayments = new LinkedHashSet<>();

        for (SSInpayment iInpayment : iInpayments) {
            for (SSInvoice iInvoice : iInpayment.getInvoices()) {
                SSCustomer iCustomer = iInvoice.getCustomer(iCustomers);
                if (iCustomer != null) {
                    iCustomersWithInpayments.add(iCustomer);
                }
            }
        }

        return iCustomersWithInpayments.stream()
                .filter(iCustomer -> iCustomer != null && iCustomer.getNumber() != null)
                .sorted((pLeft, pRight) -> pLeft.getNumber().compareTo(pRight.getNumber()))
                .toList();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSInpaymentListDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iCheckCustomer=").append(iCheckCustomer);
        sb.append(", iCheckDate=").append(iCheckDate);
        sb.append(", iCheckInvoice=").append(iCheckInvoice);
        sb.append(", iCustomer=").append(iCustomer);
        sb.append(", iFromDate=").append(iFromDate);
        sb.append(", iInvoice=").append(iInvoice);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iToDate=").append(iToDate);
        sb.append('}');
        return sb.toString();
    }
}
