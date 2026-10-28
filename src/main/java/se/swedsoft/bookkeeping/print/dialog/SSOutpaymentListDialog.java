package se.swedsoft.bookkeeping.print.dialog;


import se.swedsoft.bookkeeping.calc.math.SSOutpaymentMath;
import se.swedsoft.bookkeeping.calc.util.SSFilter;
import se.swedsoft.bookkeeping.calc.util.SSFilterFactory;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.supplier.util.SSSupplierTableModel;
import se.swedsoft.bookkeeping.gui.supplierinvoice.util.SSSupplierInvoiceTableModel;
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
import java.util.LinkedHashSet;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * $Id$
 *
 */
public class SSOutpaymentListDialog extends SSDialog {

    private JPanel iPanel;

    private SSButtonPanel iButtonPanel;

    private JCheckBox iCheckDate;
    private JCheckBox iCheckSupplier;
    private JCheckBox iCheckInvoice;

    private SSTableComboBox<SSSupplier> iSupplier;
    private SSTableComboBox<SSSupplierInvoice> iInvoice;
    private SSDateChooser iToDate;
    private SSDateChooser iFromDate;

    /**
     *
     * @param iMainFrame
     */
    public SSOutpaymentListDialog(SSMainFrame iMainFrame) {
        super(iMainFrame,
                SSBundle.getBundle().getString("outpaymentlistreport.dialog.title"));

        setPanel(iPanel);

        iButtonPanel.addCancelActionListener(e -> setModalResult(JOptionPane.CANCEL_OPTION, true));
        iButtonPanel.addOkActionListener(e -> setModalResult(JOptionPane.OK_OPTION, true));

	getRootPane().setDefaultButton(iButtonPanel.getOkButton());

        iSupplier.setModel(createSupplierDropDownModel());
        iSupplier.setSearchColumns(0);
        iInvoice.setModel(SSSupplierInvoiceTableModel.getDropDownModel());
        iInvoice.setSearchColumns(0);

        ChangeListener iChangeListener = e -> {

                iSupplier.setEnabled(iCheckSupplier.isSelected());
                iInvoice.setEnabled(iCheckInvoice.isSelected());

                iFromDate.setEnabled(iCheckDate.isSelected());
                iToDate.setEnabled(iCheckDate.isSelected());

            };

        iCheckDate.addChangeListener(iChangeListener);
        iCheckSupplier.addChangeListener(iChangeListener);
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
    public List<SSOutpayment> getElementsToPrint() {
        List<SSOutpayment> iOutpayments = Repositories.outpayments().findAll();

        SSFilterFactory<SSOutpayment> iFactory = new SSFilterFactory<>(
                iOutpayments);

        // Filter by supplier
        if (iCheckSupplier.isSelected() && iSupplier.getSelected() != null) {
            final SSSupplier iSupplier = this.iSupplier.getSelected();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSOutpayment iOutpayment) {
                    for (SSSupplierInvoice iInvoice : iOutpayment.getSupplierInvoices()) {
                        if (iInvoice != null && iInvoice.hasSupplier(iSupplier)) {
                            return true;
                        }
                    }
                    return false;
                }
            });

        }
        // Filter by supplier invoice
        if (iCheckInvoice.isSelected() && iInvoice.getSelected() != null) {
            final SSSupplierInvoice iInvoice = this.iInvoice.getSelected();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSOutpayment iInpayment) {
                    return SSOutpaymentMath.hasInvoice(iInpayment, iInvoice);
                }
            });

        }
        // Filter by date
        if (iCheckDate.isSelected()) {
            final LocalDate iDateFrom = iFromDate.getLocalDate();
            final LocalDate iDateTo = iToDate.getLocalDate();

            iFactory.applyFilter(new SSFilter<>() {
                public boolean applyFilter(SSOutpayment iOutpayment) {
                    return SSOutpaymentMath.inPeriod(iOutpayment, iDateFrom, iDateTo);
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

    /**
     *
     * @return
     */
    public boolean isInvoiceSelected() {
        return iCheckInvoice.isSelected();
    }

    public boolean isSupplierSelected() {
        return iCheckSupplier.isSelected();
    }

    /**
     *
     * @return
     */
    public LocalDate getLocalDateFrom() {
        return iFromDate.getLocalDate();
    }

    /**
     *
     * @return
     */
    public LocalDate getLocalDateTo() {
        return iToDate.getLocalDate();
    }

    /**
     *
     * @return
     */
    public SSSupplierInvoice getInvoice() {
        return iInvoice.getSelected();
    }

    public SSSupplier getSupplier() {
        return iSupplier.getSelected();
    }

    private SSSupplierTableModel createSupplierDropDownModel() {
        SSSupplierTableModel iModel = new SSSupplierTableModel(getSuppliersWithOutpayments());
        iModel.addColumn(SSSupplierTableModel.COLUMN_NUMBER);
        iModel.addColumn(SSSupplierTableModel.COLUMN_NAME);
        return iModel;
    }

    private List<SSSupplier> getSuppliersWithOutpayments() {
        List<SSOutpayment> iOutpayments = Repositories.outpayments().findAll();
        List<SSSupplier> iSuppliers = Repositories.suppliers().findAll();
        Set<SSSupplier> iSuppliersWithOutpayments = new LinkedHashSet<>();

        for (SSOutpayment iOutpayment : iOutpayments) {
            for (SSSupplierInvoice iInvoice : iOutpayment.getSupplierInvoices()) {
                SSSupplier iSupplier = iInvoice == null ? null : iInvoice.getSupplier(iSuppliers);
                if (iSupplier != null) {
                    iSuppliersWithOutpayments.add(iSupplier);
                }
            }
        }

        return iSuppliersWithOutpayments.stream()
                .filter(iSupplier -> iSupplier != null && iSupplier.getNumber() != null)
                .sorted((pLeft, pRight) -> pLeft.getNumber().compareTo(pRight.getNumber()))
                .toList();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.dialog.SSOutpaymentListDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iCheckDate=").append(iCheckDate);
        sb.append(", iCheckInvoice=").append(iCheckInvoice);
        sb.append(", iCheckSupplier=").append(iCheckSupplier);
        sb.append(", iFromDate=").append(iFromDate);
        sb.append(", iInvoice=").append(iInvoice);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iSupplier=").append(iSupplier);
        sb.append(", iToDate=").append(iToDate);
        sb.append('}');
        return sb.toString();
    }
}
