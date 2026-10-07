package se.swedsoft.bookkeeping.gui.suppliercreditinvoice;


import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.suppliercreditinvoice.dialog.SSSelectSupplierInvoiceDialog;
import se.swedsoft.bookkeeping.gui.suppliercreditinvoice.panel.SSSupplierCreditInvoicePanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInformationDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.table.model.SSTableModel;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ResourceBundle;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:57:02
 */
public class SSSupplierCreditInvoiceDialog {

    private static ResourceBundle bundle = SSBundle.getBundle();

    private SSSupplierCreditInvoiceDialog() {}

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSTableModel<SSSupplierCreditInvoice> pModel) {
        final SSDialog                    iDialog = new SSDialog(iMainFrame,
                SSSupplierCreditInvoiceDialog.bundle.getString(
                "suppliercreditinvoiceframe.new.title"));

        SSSupplierInvoice iSupplierInvoice = SSSelectSupplierInvoiceDialog.showDialog(
                iMainFrame);
        final SSSupplierCreditInvoicePanel iPanel = new SSSupplierCreditInvoicePanel(
                iDialog);

        if (iSupplierInvoice == null) {
            new SSErrorDialog(iMainFrame,
                    "suppliercreditinvoiceframe.suppliercreditinvoicenoinvoice");
            return;
        } else if (iSupplierInvoice.getNumber() == -1) {
            return;
        }

        SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice(
                iSupplierInvoice);

        iSupplierCreditInvoice.setEntered(false);
        iSupplierCreditInvoice.setNumber(null);
        iPanel.setCreditSupplierInvoice(iSupplierCreditInvoice);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iSupplierCreditInvoice);
                SSSupplierCreditInvoice iSupplierCreditInvoice1 = iPanel.getSupplierCreditInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierCreditInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierCreditInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "suppliercreditinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierCreditInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierCreditInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierCreditInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierCreditInvoice1.getVoucher(), false);
                    iSupplierCreditInvoice1.setEntered();
                } else if (!iSupplierCreditInvoice1.isEntered()) {
                    iSupplierCreditInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierCreditInvoices().add(iSupplierCreditInvoice1);
                SSSupplierCreditInvoiceFrame.fireTableDataChanged();

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
                        "suppliercreditinvoiceframe.saveonclose")
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
    public static void newDialog(final SSMainFrame iMainFrame, SSSupplierInvoice iSupplierInvoice, final SSTableModel<SSSupplierCreditInvoice> pModel) {
        final SSDialog iDialog = new SSDialog(iMainFrame,
                SSSupplierCreditInvoiceDialog.bundle.getString(
                "suppliercreditinvoiceframe.new.title"));
        final SSSupplierCreditInvoicePanel iPanel = new SSSupplierCreditInvoicePanel(
                iDialog);

        SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice(
                iSupplierInvoice);

        iSupplierCreditInvoice.setEntered(false);
        iSupplierCreditInvoice.setNumber(null);
        for (SSSupplierInvoiceRow iRow : iSupplierInvoice.getRows()) {
            if (iRow.getProductNr() != null) {
                SSProduct iProduct = iRow.getProduct(SSProductContext.getProducts());

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
        iPanel.setCreditSupplierInvoice(iSupplierCreditInvoice);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iSupplierCreditInvoice);
                SSSupplierCreditInvoice iSupplierCreditInvoice1 = iPanel.getSupplierCreditInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierCreditInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierCreditInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "suppliercreditinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierCreditInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierCreditInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierCreditInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierCreditInvoice1.getVoucher(), false);
                    iSupplierCreditInvoice1.setEntered();
                } else if (!iSupplierCreditInvoice1.isEntered()) {
                    iSupplierCreditInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierCreditInvoices().add(iSupplierCreditInvoice1);
                SSSupplierCreditInvoiceFrame.fireTableDataChanged();

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
                        "suppliercreditinvoiceframe.saveonclose")
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
     * @param iSupplierCreditInvoice
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSSupplierCreditInvoice iSupplierCreditInvoice, final SSTableModel<SSSupplierCreditInvoice> pModel) {
        final SSDialog                     iDialog = new SSDialog(iMainFrame,
                SSSupplierCreditInvoiceDialog.bundle.getString(
                "suppliercreditinvoiceframe.edit.title"));
        final SSSupplierCreditInvoicePanel iPanel = new SSSupplierCreditInvoicePanel(
                iDialog);
        final boolean iReadOnly = shouldOpenReadOnly(iSupplierCreditInvoice);

        iPanel.setCreditSupplierInvoice(iSupplierCreditInvoice);
        if (shouldShowEditLockedInfo(iSupplierCreditInvoice)) {
            new SSInformationDialog(iMainFrame, "suppliercreditinvoiceframe.editlocked");
        }
        if (iReadOnly) {
            iPanel.setReadOnlyMode(true);
        }

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSSupplierCreditInvoice iSupplierCreditInvoice1 = iPanel.getSupplierCreditInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierCreditInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierCreditInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "suppliercreditinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierCreditInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierCreditInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierCreditInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierCreditInvoice1.getVoucher(), false);
                    iSupplierCreditInvoice1.setEntered();
                } else if (!iSupplierCreditInvoice1.isEntered()) {
                    iSupplierCreditInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierCreditInvoices().update(iSupplierCreditInvoice1);
                SSSupplierCreditInvoiceFrame.fireTableDataChanged();

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
                        "suppliercreditinvoiceframe.saveonclose")
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
     * Returns true when the supplier credit invoice should open in read-only mode.
     *
     * @param iSupplierCreditInvoice the invoice to evaluate
     * @return true when editing is blocked
     */
    public static boolean shouldOpenReadOnly(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        return iSupplierCreditInvoice.isEntered() || iSupplierCreditInvoice.isCancelled();
    }

    /**
     * Returns true when the locked-info dialog should be shown.
     *
     * @param iSupplierCreditInvoice the invoice to evaluate
     * @return true when the info dialog should be shown
     */
    public static boolean shouldShowEditLockedInfo(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        return shouldOpenReadOnly(iSupplierCreditInvoice);
    }

    /**
     *
     * @param iMainFrame
     * @param iSupplierCreditInvoice
     * @param pModel
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSSupplierCreditInvoice iSupplierCreditInvoice, final SSTableModel<SSSupplierCreditInvoice> pModel) {
        final SSDialog                    iDialog = new SSDialog(iMainFrame,
                SSSupplierCreditInvoiceDialog.bundle.getString(
                "suppliercreditinvoiceframe.copy.title"));
        final SSSupplierCreditInvoicePanel iPanel = new SSSupplierCreditInvoicePanel(
                iDialog);

        SSSupplierCreditInvoice iNew = new SSSupplierCreditInvoice(iSupplierCreditInvoice);

        iNew.setNumber(null);
        iNew.setEntered(false);
        iNew.setLocalDate(SSDateUtil.today());
        if (iSupplierCreditInvoice.getCrediting() == null) {
            new SSErrorDialog(iMainFrame,
                    "suppliercreditinvoiceframe.suppliercreditinvoicenoinvoice");
            return;
        }
        iPanel.setCreditSupplierInvoice(iNew);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                assignNextNumberIfMissing(iNew);
                SSSupplierCreditInvoice iSupplierCreditInvoice1 = iPanel.getSupplierCreditInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iSupplierCreditInvoice1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iSupplierCreditInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "suppliercreditinvoiceframe.entered");
                }

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iSupplierCreditInvoice1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iSupplierCreditInvoice1.getVoucher()));
                        return;
                    }
                    iSupplierCreditInvoice1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_SUPPLIER_INVOICE));
                    SSAccountingContext.addVoucher(iSupplierCreditInvoice1.getVoucher(), false);
                    iSupplierCreditInvoice1.setEntered();
                } else if (!iSupplierCreditInvoice1.isEntered()) {
                    iSupplierCreditInvoice1.setVoucher(new SSVoucher());
                }

                Repositories.supplierCreditInvoices().add(iSupplierCreditInvoice1);
                SSSupplierCreditInvoiceFrame.fireTableDataChanged();

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
                        "suppliercreditinvoiceframe.saveonclose")
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

    private static void assignNextNumberIfMissing(SSSupplierCreditInvoice pSupplierCreditInvoice) {
        if (pSupplierCreditInvoice == null || pSupplierCreditInvoice.getNumber() != null) {
            return;
        }
        pSupplierCreditInvoice.setNumber(SSPurchaseContext.getMaxSupplierCreditInvoiceId() + 1);
    }

}
