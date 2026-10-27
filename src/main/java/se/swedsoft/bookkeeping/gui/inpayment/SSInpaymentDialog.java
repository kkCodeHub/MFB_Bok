package se.swedsoft.bookkeeping.gui.inpayment;


import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.inpayment.panel.SSInpaymentPanel;
import se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInformationDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSQueryDialog;
import se.swedsoft.bookkeeping.gui.util.model.SSDefaultTableModel;
import se.swedsoft.bookkeeping.persistence.Repositories;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * User: Andreas Lago
 * Date: 2006-aug-01
 * Time: 09:23:40
 */
public class SSInpaymentDialog {
    private SSInpaymentDialog() {}

    private static void applyDefaultDescription(SSInpayment iInpayment) {
        if (iInpayment == null) {
            return;
        }
        if (iInpayment.getText() != null && !iInpayment.getText().trim().isEmpty()) {
            return;
        }
        iInpayment.getDefaultDescription().ifPresent(iInpayment::setText);
    }

    private static void assignNextNumberIfMissing(SSInpayment iInpayment) {
        if (iInpayment == null || iInpayment.getNumber() != null) {
            return;
        }
        iInpayment.doAutoIncrecement();
    }

    /**
     *
     * @param iMainFrame
     * @param pInpayment
     * @param pModel
     */
    public static void editDialog(final SSMainFrame iMainFrame, final SSInpayment pInpayment, final SSDefaultTableModel<SSInpayment> pModel) {
        final SSDialog       iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inpaymentframe.edit.title"));
        final SSInpaymentPanel iPanel = new SSInpaymentPanel(iDialog);
        final boolean iReadOnly = pInpayment != null && pInpayment.isEntered();

        if (iReadOnly) {
            SSInformationDialog.showDialog(iMainFrame, "inpaymentframe.editlocked");
        }

        iPanel.setInpayment(new SSInpayment(pInpayment));
        iPanel.setReadOnlyMode(iReadOnly);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInpayment iInpayment = iPanel.getInpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iInpayment.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "inpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInpayment.isEntered()) {
                    new SSInformationDialog(iMainFrame, "inpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableInpaymentAmounts(iInpayment);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iInpayment.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iInpayment.getVoucher()));
                        return;
                    }
                    iInpayment.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_INPAYMENT));
                    SSAccountingContext.addVoucher(iInpayment.getVoucher(), false);
                    iInpayment.setEntered();
                } else if (!iInpayment.isEntered()) {
                    iInpayment.setVoucher(new SSVoucher());
                }

                Repositories.inpayments().update(iInpayment);
                SSInpaymentFrame.fireTableDataChanged();

                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
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
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "inpaymentframe.saveonclose")
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
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, final SSDefaultTableModel<SSInpayment> pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inpaymentframe.new.title"));
        final SSInpaymentPanel iPanel = new SSInpaymentPanel(iDialog);
        iPanel.setAutoUpdateDescriptionFromInvoice(true);

        SSInpayment iInpayment = new SSInpayment();
        assignNextNumberIfMissing(iInpayment);
        applyDefaultDescription(iInpayment);

        iPanel.setInpayment(iInpayment);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInpayment iInpayment1 = iPanel.getInpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iInpayment1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "inpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInpayment1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "inpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableInpaymentAmounts(iInpayment1);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iInpayment1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iInpayment1.getVoucher()));
                        return;
                    }
                    iInpayment1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_INPAYMENT));
                    SSAccountingContext.addVoucher(iInpayment1.getVoucher(), false);
                    iInpayment1.setEntered();
                } else if (!iInpayment1.isEntered()) {
                    iInpayment1.setVoucher(new SSVoucher());
                }

                Repositories.inpayments().add(iInpayment1);
                SSInvoiceFrame.fireTableDataChanged();
                SSInpaymentFrame.fireTableDataChanged();

                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
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
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "inpaymentframe.saveonclose")
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
     * @param iInpayment
     * @param pModel
     */
    public static void newDialog(final SSMainFrame iMainFrame, SSInpayment iInpayment, final SSDefaultTableModel<SSInpayment> pModel) {
        final SSDialog         iDialog = new SSDialog(iMainFrame,
                SSBundle.getBundle().getString("inpaymentframe.new.title"));
        final SSInpaymentPanel iPanel = new SSInpaymentPanel(iDialog);
        iPanel.setAutoUpdateDescriptionFromInvoice(true);

        assignNextNumberIfMissing(iInpayment);
        applyDefaultDescription(iInpayment);
        iPanel.setInpayment(iInpayment);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        final ActionListener iSaveAction = e -> {

                SSInpayment iInpayment1 = iPanel.getInpayment();
                boolean iSaveVoucher = false;

                if (iPanel.isVoucherGenerated() && !iInpayment1.isEntered()) {
                    int iResponse = SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                            "inpaymentframe.voucher.saveonclose");
                    iSaveVoucher = iResponse == JOptionPane.OK_OPTION;
                } else if (iInpayment1.isEntered()) {
                    new SSInformationDialog(iMainFrame, "inpaymentframe.entered");
                }
                iSaveVoucher = iSaveVoucher && hasPostableInpaymentAmounts(iInpayment1);

                if (iSaveVoucher) {
                    if (!hasOpenAccountingYearForVoucherDate(iInpayment1.getVoucher())) {
                        SSInformationDialog.showDialog(iMainFrame, "invoiceframe.voucher.badyear",
                                getVoucherAccountingYearLabel(iInpayment1.getVoucher()));
                        return;
                    }
                    iInpayment1.getVoucher().setSeries(
                            SSAccountingContext.resolveVoucherSeriesForEventCode(
                                    SSAccountingContext.VOUCHER_EVENT_CODE_INPAYMENT));
                    SSAccountingContext.addVoucher(iInpayment1.getVoucher(), false);
                    iInpayment1.setEntered();
                } else if (!iInpayment1.isEntered()) {
                    iInpayment1.setVoucher(new SSVoucher());
                }

                Repositories.inpayments().add(iInpayment1);

                SSInvoiceFrame.fireTableDataChanged();
                SSInpaymentFrame.fireTableDataChanged();

                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
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
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "inpaymentframe.saveonclose")
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

    private static boolean hasPostableInpaymentAmounts(SSInpayment pInpayment) {
        if (pInpayment == null) {
            return false;
        }
        for (SSInpaymentRow iRow : pInpayment.getRows()) {
            BigDecimal iPaymentAmount = iRow.getValue();
            if (iPaymentAmount != null && iPaymentAmount.signum() != 0) {
                return true;
            }
            SSInvoice iInvoice = iRow.getInvoice(se.swedsoft.bookkeeping.data.system.SSSalesContext.getInvoices());
            if (iInvoice != null
                    && SSInpaymentMath.getInpaymentAmountForInvoice(iInvoice).signum() != 0) {
                return true;
            }
        }
        return false;
    }
}
