package se.swedsoft.bookkeeping.gui.invoice;


import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.system.SSInvoiceActionPolicy;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.invoice.dialog.SSInvoiceTypeDialog;
import se.swedsoft.bookkeeping.gui.invoice.panel.SSInvoicePanel;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInformationDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.util.SSDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-jul-31
 * Time: 15:01:54
 */
public class SSInvoiceDialog {
    private static final Logger LOG = LoggerFactory.getLogger(SSInvoiceDialog.class);

    private SSInvoiceDialog() {}

    /**
     * Opens a dialog to create a new invoice.
     *
     * @param iMainFrame the owning frame
     */
    public static void newDialog(final SSMainFrame iMainFrame) {
        final SSDialog       iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("invoiceframe.new.title"));
        SSInvoiceType iInvoiceType = SSInvoiceTypeDialog.showDialog(iMainFrame);

        if (iInvoiceType == null) {
            return;
        }

        final SSInvoicePanel iPanel = new SSInvoicePanel(iDialog);

        iPanel.setInvoice(new SSInvoice(iInvoiceType));

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInvoice iInvoice = iPanel.getInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.doSaveCustomerAndProducts()) {
                    SSInvoiceMath.addCustomerAndProducts(iInvoice);
                }

                if (iPanel.isVoucherGenerated() && !iInvoice.isEntered() && !iInvoice.isCancelled()) {
                    iInvoice.setNumber(SSSalesContext.getMaxInvoiceId() + 1);
                    iInvoice.generateVoucher();
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInvoice.isEntered()) {
                    new SSInformationDialog(iMainFrame, "invoiceframe.entered");
                }

                if (iSaveVoucher) {
                    SSAccountingContext.addVoucher(iInvoice.getVoucher(), false);
                    iInvoice.setEntered();
                } else if (!iInvoice.isEntered()) {
                    iInvoice.setVoucher(new SSVoucher());
                }

                SSSalesContext.addInvoice(iInvoice);

                SSInvoiceFrame.fireTableDataChanged();

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
                        "invoiceframe.saveonclose")
                        != JOptionPane.OK_OPTION) {
                    return;
                }

                iSaveAction.actionPerformed(null);
            }
        });
        iDialog.setSize(800, 600);
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.showDialog();
    }

    /**
     * Opens a dialog to edit an existing invoice.
     *
     * @param iMainFrame the owning frame
     * @param iInvoice   the invoice to edit
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSInvoice iInvoice) {
        final SSDialog       iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("invoiceframe.edit.title"));
        final SSInvoicePanel iPanel = new SSInvoicePanel(iDialog);
        final boolean iReadOnly = shouldOpenReadOnly(iInvoice);
        final boolean iKonteringPossibleMode = shouldOpenKonteringPossibleMode(iInvoice);

        if (shouldShowEditLockedInfo(iInvoice)) {
            SSInformationDialog.showDialog(iMainFrame, "invoiceframe.editlocked");
        }

        iPanel.setInvoice(new SSInvoice(iInvoice));
        iPanel.setSavecustomerandproductsSelected(false);
        if (iKonteringPossibleMode) {
            iPanel.setKonteringPossibleMode();
        } else {
            iPanel.setReadOnlyMode(iReadOnly);
        }

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInvoice iInvoice1 = iPanel.getInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.doSaveCustomerAndProducts()) {
                    SSInvoiceMath.addCustomerAndProducts(iInvoice1);
                }

                if (iPanel.isVoucherGenerated() && !iInvoice1.isEntered() && !iInvoice1.isCancelled()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "invoiceframe.entered");
                }

                if (iSaveVoucher) {
                    SSAccountingContext.addVoucher(iInvoice1.getVoucher(), false);
                    iInvoice1.setEntered();
                } else if (!iInvoice1.isEntered()) {
                    iInvoice1.setVoucher(new SSVoucher());
                }

                SSSalesContext.updateInvoice(iInvoice1);

                SSInvoiceFrame.fireTableDataChanged();

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
                        "invoiceframe.saveonclose")
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

    static boolean shouldShowEditLockedInfo(SSInvoice pInvoice) {
        return !SSInvoiceActionPolicy.canEdit(pInvoice);
    }

    static boolean shouldOpenReadOnly(SSInvoice pInvoice) {
        return !SSInvoiceActionPolicy.canEdit(pInvoice);
    }

    static boolean shouldOpenKonteringPossibleMode(SSInvoice pInvoice) {
        return pInvoice != null
                && !pInvoice.isEntered()
                && !pInvoice.isCancelled()
                && !SSInvoiceActionPolicy.canEdit(pInvoice);
    }

    /**
     * Opens a dialog to copy an existing invoice.
     *
     * @param iMainFrame the owning frame
     * @param iInvoice   the invoice to copy
     */
    public static void copyDialog(final SSMainFrame iMainFrame, SSInvoice iInvoice) {
        final SSDialog       iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("invoiceframe.copy.title"));
        final SSInvoicePanel iPanel = new SSInvoicePanel(iDialog);
        SSInvoice iNew = new SSInvoice(iInvoice);

        iNew.setNumber(null);
        iNew.setLocalDate(SSDateUtil.today());
        iNew.setDueDate();
        iNew.setEntered(false);
        iNew.setPrinted(false);
        iNew.setInterestInvoiced(false);
        iNew.setOCRNumber(null);
        iNew.setNumRemainders(0);

        iPanel.setInvoice(iNew);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInvoice iInvoice1 = iPanel.getInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.doSaveCustomerAndProducts()) {
                    SSInvoiceMath.addCustomerAndProducts(iInvoice1);
                }

                if (iPanel.isVoucherGenerated() && !iInvoice1.isEntered() && !iInvoice1.isCancelled()) {
                    iInvoice1.setNumber(SSSalesContext.getMaxInvoiceId() + 1);
                    iInvoice1.generateVoucher();
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "invoiceframe.entered");
                }

                if (iSaveVoucher) {
                    SSAccountingContext.addVoucher(iInvoice1.getVoucher(), false);
                    iInvoice1.setEntered();
                } else if (!iInvoice1.isEntered()) {
                    iInvoice1.setVoucher(new SSVoucher());
                }

                SSSalesContext.addInvoice(iInvoice1);
                SSInvoiceFrame.fireTableDataChanged();

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                // SSInvoice iInvoice = iPanel.getInvoice();
                // remove all references for the sales
                // SSOrderMath.removeReference(iInvoice);

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
                        "invoiceframe.saveonclose")
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
     * Opens a dialog to create a new invoice from a list of orders.
     *
     * @param iMainFrame the owning frame
     * @param iInvoice   the pre-built invoice template
     * @param iOrders    the source orders
     */
    public static void newDialog(final SSMainFrame iMainFrame, SSInvoice iInvoice, final List<SSOrder> iOrders) {
        final SSDialog       iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("invoiceframe.new.title"));
        final SSInvoicePanel iPanel = new SSInvoicePanel(iDialog);

        // iInvoice.setPaymentTerm(iInvoice.getCustomer() == null ? null : iInvoice.getCustomer().getPaymentTerm());

        iInvoice.setDueDate();
        iInvoice.setEntered(false);
        iInvoice.setPrinted(false);

        iPanel.setInvoice(new SSInvoice(iInvoice));
        // iPanel.getInvoice().doAutoIncrecement();
        // iPanel.getInvoice().setEntered(false);
        // iPanel.setOrderNumbers(iOrders);
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInvoice iInvoice1 = iPanel.getInvoice();
                boolean iSaveVoucher = false;

                if (iPanel.doSaveCustomerAndProducts()) {
                    SSInvoiceMath.addCustomerAndProducts(iInvoice1);
                }

                if (iPanel.isVoucherGenerated() && !iInvoice1.isEntered() && !iInvoice1.isCancelled()) {
                    iInvoice1.setNumber(SSSalesContext.getMaxInvoiceId() + 1);
                    iInvoice1.generateVoucher();
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "invoiceframe.voucher.saveonclose");

                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInvoice1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "invoiceframe.entered");
                }

                if (iSaveVoucher) {
                    SSAccountingContext.addVoucher(iInvoice1.getVoucher(), false);
                    iInvoice1.setEntered();
                } else if (!iInvoice1.isEntered()) {
                    iInvoice1.setVoucher(new SSVoucher());
                }

                SSSalesContext.addInvoice(iInvoice1);

                for (SSOrder iOrder : iOrders) {
                    // Set the invoice for the order
                    if (SSSalesContext.getOrders().contains(iOrder)) {
                        iOrder.setInvoice(iInvoice1);
                        SSSalesContext.updateOrder(iOrder);
                    }
                }

                SSInvoiceFrame.fireTableDataChanged();

                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iDialog.closeDialog();
                // SSInvoice iInvoice = iPanel.getInvoice();
                // remove all references for the sales
                // SSOrderMath.removeReference(iInvoice);

                iPanel.dispose();

            });
        iDialog.addWindowListener(
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!iPanel.isValid()) {
                    return;
                }

                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "invoiceframe.saveonclose")
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
}
