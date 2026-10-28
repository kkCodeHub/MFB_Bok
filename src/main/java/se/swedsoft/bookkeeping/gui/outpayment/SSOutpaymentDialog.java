package se.swedsoft.bookkeeping.gui.outpayment;


import se.swedsoft.bookkeeping.calc.math.SSOutpaymentMath;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.outpayment.panel.SSOutpaymentPanel;
import se.swedsoft.bookkeeping.gui.supplierinvoice.SSSupplierInvoiceFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInformationDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * User: Andreas Lago
 * Date: 2006-mar-21
 * Time: 10:57:02
 */
public class SSOutpaymentDialog {
    private SSOutpaymentDialog() {}

    private static void applyDefaultDescription(SSOutpayment pOutpayment) {
        if (pOutpayment == null) {
            return;
        }
        if (pOutpayment.getText() != null && !pOutpayment.getText().trim().isEmpty()) {
            return;
        }
        pOutpayment.getDefaultDescription().ifPresent(pOutpayment::setText);
    }

    private static void assignNextNumberIfMissing(SSOutpayment pOutpayment) {
        if (pOutpayment == null || pOutpayment.getNumber() != null) {
            return;
        }
        pOutpayment.doAutoIncrecement();
    }

    /**
     *
     * @param iMainFrame
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final AbstractTableModel pModel) {
        final SSDialog iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outpaymentframe.new.title"));
        final SSOutpaymentPanel iPanel = new SSOutpaymentPanel(iDialog);
        iPanel.setAutoUpdateDescriptionFromInvoice(true);

        SSOutpayment iOutpayment = new SSOutpayment();
        assignNextNumberIfMissing(iOutpayment);
        applyDefaultDescription(iOutpayment);
        iPanel.setOutpayment(iOutpayment);
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSOutpayment iOutpayment1 = iPanel.getOutpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iOutpayment1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "outpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iOutpayment1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "outpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableOutpaymentAmounts(iOutpayment1);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iOutpayment1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iOutpayment1.getVoucher()));
                        return;
                    }
                    iOutpayment1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_OUTPAYMENT));
                    SSAccountingContext.addVoucher(iOutpayment1.getVoucher(), false);
                    iOutpayment1.setEntered();
                } else if (!iOutpayment1.isEntered()) {
                    iOutpayment1.setVoucher(new SSVoucher());
                }

                Repositories.outpayments().add(iOutpayment1);
                SSSupplierInvoiceFrame.fireTableDataChanged();
                SSOutpaymentFrame.fireTableDataChanged();

                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });

        iDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outpaymentframe.saveonclose")
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
     * @param iOutpayment
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, SSOutpayment iOutpayment, final AbstractTableModel pModel) {
        final SSDialog iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outpaymentframe.new.title"));
        final SSOutpaymentPanel iPanel = new SSOutpaymentPanel(iDialog);
        iPanel.setAutoUpdateDescriptionFromInvoice(true);

        assignNextNumberIfMissing(iOutpayment);
        applyDefaultDescription(iOutpayment);
        iPanel.setOutpayment(iOutpayment);
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSOutpayment iOutpayment1 = iPanel.getOutpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iOutpayment1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "outpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iOutpayment1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "outpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableOutpaymentAmounts(iOutpayment1);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iOutpayment1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iOutpayment1.getVoucher()));
                        return;
                    }
                    iOutpayment1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_OUTPAYMENT));
                    SSAccountingContext.addVoucher(iOutpayment1.getVoucher(), false);
                    iOutpayment1.setEntered();
                } else if (!iOutpayment1.isEntered()) {
                    iOutpayment1.setVoucher(new SSVoucher());
                }

                Repositories.outpayments().add(iOutpayment1);
                SSSupplierInvoiceFrame.fireTableDataChanged();
                SSOutpaymentFrame.fireTableDataChanged();

                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outpaymentframe.saveonclose")
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
     * @param iSelected
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, SSOutpayment iSelected, final AbstractTableModel pModel) {
        final SSDialog iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("outpaymentframe.edit.title"));
        final SSOutpaymentPanel iPanel = new SSOutpaymentPanel(iDialog);
        final boolean iReadOnly = iSelected != null && iSelected.isEntered();

        if (iReadOnly) {
            SSInformationDialog.showDialog(iMainFrame, "outpaymentframe.editlocked");
        }

        iPanel.setOutpayment(new SSOutpayment(iSelected));
        iPanel.setReadOnlyMode(iReadOnly);
        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSOutpayment iOutpayment = iPanel.getOutpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iOutpayment.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "outpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iOutpayment.isEntered()) {
                    new SSInformationDialog(iMainFrame, "outpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableOutpaymentAmounts(iOutpayment);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iOutpayment.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iOutpayment.getVoucher()));
                        return;
                    }
                    iOutpayment.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_OUTPAYMENT));
                    SSAccountingContext.addVoucher(iOutpayment.getVoucher(), false);
                    iOutpayment.setEntered();
                } else if (!iOutpayment.isEntered()) {
                    iOutpayment.setVoucher(new SSVoucher());
                }

                Repositories.outpayments().update(iOutpayment);
                SSOutpaymentFrame.fireTableDataChanged();

                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                iPanel.dispose();
                iDialog.closeDialog();

            };

        iPanel.addOkAction(iSaveAction);

        iPanel.addCancelAction(e -> {

                iPanel.dispose();
                iDialog.closeDialog();

            });
        iDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (iPanel.isReadOnlyMode()) {
                    return;
                }
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "outpaymentframe.saveonclose")
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

    /**
     * Outpayment postability is based on payment amount and invoice amount only.
     * No currency conversion or rate-based checks are applied.
     */
    private static boolean hasPostableOutpaymentAmounts(SSOutpayment pOutpayment) {
        if (pOutpayment == null) {
            return false;
        }
        for (SSOutpaymentRow iRow : pOutpayment.getRows()) {
            BigDecimal iPaymentAmount = iRow.getValue();
            if (iPaymentAmount != null && iPaymentAmount.signum() != 0) {
                return true;
            }
            SSSupplierInvoice iInvoice = iRow.getSupplierInvoice(Repositories.supplierInvoices().findAll());
            if (iInvoice != null
                    && SSOutpaymentMath.getOutpaymentAmountForInvoice(iInvoice).signum() != 0) {
                return true;
            }
        }
        return false;
    }
}
