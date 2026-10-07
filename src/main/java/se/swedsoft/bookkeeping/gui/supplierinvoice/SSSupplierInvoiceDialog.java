package se.swedsoft.bookkeeping.gui.supplierinvoice;


import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInformationDialog;

import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.supplierinvoice.panel.SSSupplierInvoicePanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableModel;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:57:02
 */
public class SSSupplierInvoiceDialog {

    private static ResourceBundle bundle = SSBundle.getBundle();

    private SSSupplierInvoiceDialog() {}

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSTableModel<SSSupplierInvoice> pModel) {
        final SSDialog               iDialog = new SSDialog(iMainFrame,
                SSSupplierInvoiceDialog.bundle.getString("supplierinvoiceframe.new.title"));
        final SSSupplierInvoicePanel iPanel = new SSSupplierInvoicePanel(iDialog);

        SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();

        iSupplierInvoice.setNumber(null);
        iPanel.setSupplierInvoice(iSupplierInvoice, true);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iSupplierInvoice);
                SSSupplierInvoice iSupplierInvoice1 = iPanel.getSupplierInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "supplierinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierInvoice1.getVoucher(), false);
                    iSupplierInvoice1.setEntered();
                } else if (!iSupplierInvoice1.isEntered()) {
                    iSupplierInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierInvoices().add(iSupplierInvoice1);

                SSSupplierInvoiceFrame.fireTableDataChanged();

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!iPanel.isValid()) {
                    return;
                }

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "supplierinvoiceframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     *
     * @param iMainFrame
     * @param iSupplierInvoice
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSSupplierInvoice iSupplierInvoice, final SSTableModel<SSSupplierInvoice> pModel) {
        final SSDialog               iDialog = new SSDialog(iMainFrame,
                SSSupplierInvoiceDialog.bundle.getString("supplierinvoiceframe.edit.title"));
        final SSSupplierInvoicePanel iPanel = new SSSupplierInvoicePanel(iDialog);
        final boolean iReadOnly = shouldOpenReadOnly(iSupplierInvoice);

        iPanel.setSupplierInvoice(new SSSupplierInvoice(iSupplierInvoice), false);
        if (shouldShowEditLockedInfo(iSupplierInvoice)) {
            new SSInformationDialog(iMainFrame, "supplierinvoiceframe.editlocked");
            iPanel.setReadOnlyMode(true);
        }
        iPanel.setReadOnlyMode(iReadOnly);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSSupplierInvoice iSupplierInvoice1 = iPanel.getSupplierInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "supplierinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierInvoice1.getVoucher(), false);
                    iSupplierInvoice1.setEntered();
                } else if (!iSupplierInvoice1.isEntered()) {
                    iSupplierInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierInvoices().update(iSupplierInvoice1);

                if (pModel != null) {
                    pModel.fireTableDataChanged();
                }

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (iPanel.isReadOnlyMode()) {
                    return;
                }
                if (!iPanel.isValid()) {
                    return;
                }

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "supplierinvoiceframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     * Returns true when the supplier invoice should open in read-only mode.
     *
     * @param iSupplierInvoice the invoice to evaluate
     * @return true when editing is blocked
     */
    public static boolean shouldOpenReadOnly(SSSupplierInvoice iSupplierInvoice) {
        return iSupplierInvoice.isEntered() || iSupplierInvoice.isCancelled();
    }

    /**
     * Returns true when the locked-info dialog should be shown.
     *
     * @param iSupplierInvoice the invoice to evaluate
     * @return true when the info dialog should be shown
     */
    public static boolean shouldShowEditLockedInfo(SSSupplierInvoice iSupplierInvoice) {
        return shouldOpenReadOnly(iSupplierInvoice);
    }

    /**
     *
     * @param iMainFrame
     * @param iCopyFrom
     * @param pModel
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSSupplierInvoice iCopyFrom, final AbstractTableModel pModel) {
        final SSDialog               iDialog = new SSDialog(iMainFrame,
                SSSupplierInvoiceDialog.bundle.getString("supplierinvoiceframe.copy.title"));
        final SSSupplierInvoicePanel iPanel = new SSSupplierInvoicePanel(iDialog);

        SSSupplierInvoice iNew = new SSSupplierInvoice(iCopyFrom);

        iNew.setNumber(null);
        iNew.setLocalDate(iNew.getLastLocalDate());
        iNew.setEntered(false);
        iNew.setBGCEntered(false);

        for (SSSupplier iSupplier : SSPurchaseContext.getSuppliers()) {
            if (iCopyFrom.getSupplierNr().equals(iSupplier.getNumber())) {
                iNew.setPaymentTerm(iSupplier.getPaymentTerm());
            }
        }
        iNew.setDueDate();

        iPanel.setSupplierInvoice(iNew, false);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iNew);
                SSSupplierInvoice iSupplierInvoice = iPanel.getSupplierInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierInvoice.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierInvoice.isEntered()) {
                    new SSInformationDialog(iMainFrame, "supplierinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierInvoice.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierInvoice.getVoucher()));
                        return;
                    }
                    iSupplierInvoice.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierInvoice.getVoucher(), false);
                    iSupplierInvoice.setEntered();
                } else if (!iSupplierInvoice.isEntered()) {
                    iSupplierInvoice.setVoucher(new SSVoucher());
                }

                Repositories.supplierInvoices().add(iSupplierInvoice);

                SSSupplierInvoiceFrame.fireTableDataChanged();

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!iPanel.isValid()) {
                    return;
                }

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "supplierinvoiceframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    /**
     *
     * @param iMainFrame
     * @param iInvoice
     * @param iOrders
     * @param iModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, SSSupplierInvoice iInvoice, final List<SSPurchaseOrder> iOrders, final SSTableModel iModel) {
        final SSDialog               iDialog = new SSDialog(iMainFrame,
                SSSupplierInvoiceDialog.bundle.getString("supplierinvoiceframe.new.title"));
        final SSSupplierInvoicePanel iPanel = new SSSupplierInvoicePanel(iDialog);

        SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice(iInvoice);

        for (SSSupplierInvoiceRow iRow : iSupplierInvoice.getRows()) {
            if (iRow.getProductNr() != null) {
                SSProduct iProduct = iRow.getProduct(SSProductContext.getProducts());

                iRow.setUnitFreight(iProduct == null ? null : iProduct.getUnitFreight());
                iRow.setProject(
                        iProduct == null
                                ? null
                                : iProduct.getProject(iProduct.getProjectNr()).orElse(null));
                iRow.setResultUnit(
                        iProduct == null
                                ? null
                                : iProduct.getResultUnit(iProduct.getResultUnitNr()).orElse(null));
            }
        }
        iSupplierInvoice.setNumber(null);

        iPanel.setSupplierInvoice(iSupplierInvoice, true);

        iPanel.setOrderNumbers(iOrders);
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iSupplierInvoice);
                SSSupplierInvoice iSupplierInvoice1 = iPanel.getSupplierInvoice();

                Repositories.supplierInvoices().add(iSupplierInvoice1);

                for (SSPurchaseOrder iPurchaseOrder : iOrders) {
                    // Set the sales for the selected order to the new one
                    iPurchaseOrder.setInvoice(iSupplierInvoice1);
                    Repositories.purchaseOrders().update(iPurchaseOrder);
                }

                SSSupplierInvoiceFrame.fireTableDataChanged();

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!iPanel.isValid()) {
                    return;
                }

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "supplierinvoiceframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    private static boolean hasOpenAccountingYearForVoucherDate(SSVoucher pVoucher) {
        SSNewAccountingYear iCurrentYear = SSCompanyYearContext.getCurrentYear();
        if (iCurrentYear == null || pVoucher == null) {
            return false;
        }
        LocalDate iVoucherDate = pVoucher.getLocalDate();
        if (iVoucherDate == null) {
            return false;
        }
        return !iVoucherDate.isBefore(iCurrentYear.getLocalFrom())
                && !iVoucherDate.isAfter(iCurrentYear.getLocalTo());
    }

    private static String getVoucherAccountingYearLabel(SSVoucher pVoucher) {
        if (pVoucher == null || pVoucher.getLocalDate() == null) {
            return "????";
        }
        return Integer.toString(pVoucher.getLocalDate().getYear());
    }

    private static void assignNextNumberIfMissing(SSSupplierInvoice pSupplierInvoice) {
        if (pSupplierInvoice == null || pSupplierInvoice.getNumber() != null) {
            return;
        }
        pSupplierInvoice.setNumber(SSPurchaseContext.getMaxSupplierInvoiceId() + 1);
    }
}
