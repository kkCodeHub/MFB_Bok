package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.calc.math.SSSupplierCreditInvoiceMath;
import se.swedsoft.bookkeeping.calc.util.SSFilter;
import se.swedsoft.bookkeeping.calc.util.SSFilterFactory;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.supplier.util.SSSupplierTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.components.SSTableComboBox;
import se.swedsoft.bookkeeping.gui.util.datechooser.SSDateChooser;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;


/**
 * $Id$
 *
 */
public class SSSupplierCreditInvoiceListDialog extends SSDialog {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private JCheckBox iCheckDate;
    private JCheckBox iCheckSupplier;

    private SSTableComboBox<SSSupplier> iSupplier;
    private SSDateChooser iToDate;
    private SSDateChooser iFromDate;

    /**
     *
     * @param iMainFrame
     */
    public SSSupplierCreditInvoiceListDialog(SSMainFrame iMainFrame) {
        super(iMainFrame,
                SSBundle.getBundle().getString(
                "suppliercreditinvoicelistreport.dialog.title"));

        setPanel(iPanel);

        iButtonPanel.addCancelActionListener(e -> setModalResult(JOptionPane.CANCEL_OPTION, true));
        iButtonPanel.addOkActionListener(e -> setModalResult(JOptionPane.OK_OPTION, true));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        iSupplier.setModel(SSSupplierTableModel.getDropDownModel());
        iSupplier.setSearchColumns(0);

        ChangeListener iChangeListener = e -> {

                iSupplier.setEnabled(iCheckSupplier.isSelected());

                iFromDate.setEnabled(iCheckDate.isSelected());
                iToDate.setEnabled(iCheckDate.isSelected());

            };

        iCheckDate.addChangeListener(iChangeListener);
        iCheckSupplier.addChangeListener(iChangeListener);

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
    public List<SSSupplierCreditInvoice> getElementsToPrint() {
        List<SSSupplierCreditInvoice> iInvoices = SSDB.getInstance().getSupplierCreditInvoices();

        SSFilterFactory<SSSupplierCreditInvoice> iFactory = new SSFilterFactory<>(
                iInvoices);

        // Filter by a customer
        if (iCheckSupplier.isSelected() && iSupplier.getSelected() != null) {
            final SSSupplier iSupplier = this.iSupplier.getSelected();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSSupplierCreditInvoice iInvoice) {
                    return iInvoice.hasSupplier(iSupplier);
                }
            });

        }
        // Filter by date
        if (iCheckDate.isSelected()) {
            final LocalDate iDateFrom = iFromDate.getLocalDate();
            final LocalDate iDateTo = iToDate.getLocalDate();

            iFactory.applyFilter(
                    new SSFilter<>() {
                public boolean applyFilter(SSSupplierCreditInvoice iInvoice) {
                    return SSSupplierCreditInvoiceMath.inPeriod(iInvoice, SSDateUtil.toDate(iDateFrom),
                            SSDateUtil.toDate(iDateTo));
                }
            });
        }

        return iFactory.getObjects();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSSupplierCreditInvoiceListDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iCheckDate=").append(iCheckDate);
        sb.append(", iCheckSupplier=").append(iCheckSupplier);
        sb.append(", iFromDate=").append(iFromDate);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iSupplier=").append(iSupplier);
        sb.append(", iToDate=").append(iToDate);
        sb.append('}');
        return sb.toString();
    }
}
