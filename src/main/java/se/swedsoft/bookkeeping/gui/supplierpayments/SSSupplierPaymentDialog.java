package se.swedsoft.bookkeeping.gui.supplierpayments;


import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;

import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.supplierpayments.util.SSSupplierPaymentTableModel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.SSButtonPanel;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSFileChooser;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterTXT;
import se.swedsoft.bookkeeping.gui.util.table.SSTable;
import se.swedsoft.bookkeeping.importexport.supplierpayments.SSSupplierPaymentExporter;
import se.swedsoft.bookkeeping.importexport.supplierpayments.data.SupplierPayment;
import se.swedsoft.bookkeeping.importexport.supplierpayments.data.SupplierPaymentConfig;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * $Id$
 *
 */
public class SSSupplierPaymentDialog extends SSDialog {    private static final Logger LOG = LoggerFactory.getLogger(SSSupplierPaymentDialog.class);


    private JPanel iPanel;

    private SSTable iTable;

    private SSSupplierPaymentTableModel iModel;

    private SSButtonPanel iButtonPanel;

    private JTextField iOurBankGiroNumber;

    private JTextField iMessage;

    /**
     *
     * @param iMainFrame
     * @param iSupplierInvoices
     */
    public SSSupplierPaymentDialog(final SSMainFrame iMainFrame, List<SSSupplierInvoice> iSupplierInvoices) {
        super(iMainFrame, SSBundle.getBundle().getString("supplierpaymentframe.title"));
        setPanel(iPanel);
        iTable.setColumnSortingEnabled(false);
        iTable.setColorReadOnly(true);

        iModel = new SSSupplierPaymentTableModel(iSupplierInvoices);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_NUMBER);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_SUPPLIER_NUMBER);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_SUPPLIER_NAME);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_DATE, true);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_VALUE, true);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_CURRENCY);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_PAYMENT_METHOD, true);
        iModel.addColumn(SSSupplierPaymentTableModel.COLUMN_ACCOUNT, true);

        iModel.setupTable(iTable);

        iOurBankGiroNumber.setText(
                SSDB.getInstance().getCurrentCompany().getBankGiroNumber());

        iButtonPanel.addOkActionListener(
                e -> {

                        List<SupplierPayment> iSupplierPayments = iModel.getObjects();

                        for (SupplierPayment iPayment : iSupplierPayments) {
                            SSSupplierInvoice pSupplierInvoice = iPayment.getSupplierInvoice();

                            if (SSDB.getInstance().getSupplierInvoice(pSupplierInvoice) == null) {
                                iSupplierPayments.remove(iPayment);
                                new SSErrorDialog(iMainFrame,
                                        "supplierinvoiceframe.supplierinvoicegone",
                                        pSupplierInvoice.getNumber());
                            }

                        }
                        iModel.setObjects(iSupplierPayments);
                        if (iSupplierPayments.isEmpty()) {
                            new SSErrorDialog(iMainFrame,
                                    "supplierinvoiceframe.nosupplierpayments");
                            return;
                        }

                        SSFileChooser iFileChooser = new SSFileChooser(new SSFilterTXT());

                        iFileChooser.setSelectedFile(new File("Leverantörsbetalning.txt"));
                        int iResponce = iFileChooser.showSaveDialog(iMainFrame);

                        if (iResponce != SSFileChooser.APPROVE_OPTION) {
                            return;
                        }

                        for (SupplierPayment iPayment : iSupplierPayments) {
                            SSSupplierInvoice pSupplierInvoice = iPayment.getSupplierInvoice();

                            if (SSDB.getInstance().getSupplierInvoice(pSupplierInvoice) == null) {
                                iSupplierPayments.remove(iPayment);
                                new SSErrorDialog(iMainFrame,
                                        "supplierinvoiceframe.supplierinvoicegone",
                                        pSupplierInvoice.getNumber());
                            }

                        }
                        iModel.setObjects(iSupplierPayments);
                        if (iSupplierPayments.isEmpty()) {
                            new SSErrorDialog(iMainFrame,
                                    "supplierinvoiceframe.nosupplierpayments");
                            return;
                        }
                        LocalDate iDate = null;

                        for (SupplierPayment iSupplierPayment : iSupplierPayments) {
                            LocalDate iPaymentDate = iSupplierPayment.getLocalDate();
                            if (iDate == null || (iPaymentDate != null && iPaymentDate.isAfter(iDate))) {
                                iDate = iPaymentDate;
                            }
                            iSupplierPayment.getSupplierInvoice().setBGCEntered();
                            SSDB.getInstance().updateSupplierInvoice(
                                    iSupplierPayment.getSupplierInvoice());
                        }
                        SupplierPaymentConfig.setOurBankGiroAccount(iOurBankGiroNumber.getText());
                        SupplierPaymentConfig.setMessage(iMessage.getText());
                        SupplierPaymentConfig.setMessageDate(SSDateUtil.toDate(iDate));

                        try {
                            SSSupplierPaymentExporter.Export(iFileChooser.getSelectedFile(),
                                    iSupplierPayments);
                        } catch (SSExportException e1) {
                            SSErrorDialog.showDialog(iMainFrame,
                                    SSBundle.getBundle().getString("supplierpaymentframe.error"),
                                    e1.getMessage());

                            LOG.error("Unexpected error", e1);

                            return;
                        }

                        closeDialog(JOptionPane.OK_OPTION);

                    });

        iButtonPanel.addCancelActionListener(
                e -> {

                        closeDialog();

                    });

        getRootPane().setDefaultButton(iButtonPanel.getOkButton());

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.supplierpayments.SSSupplierPaymentDialog");
        sb.append("{iButtonPanel=").append(iButtonPanel);
        sb.append(", iMessage=").append(iMessage);
        sb.append(", iModel=").append(iModel);
        sb.append(", iOurBankGiroNumber=").append(iOurBankGiroNumber);
        sb.append(", iPanel=").append(iPanel);
        sb.append(", iTable=").append(iTable);
        sb.append('}');
        return sb.toString();
    }
}
