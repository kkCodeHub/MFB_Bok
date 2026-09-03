package se.swedsoft.bookkeeping.print;


import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.calc.SSOCRNumber;
import se.swedsoft.bookkeeping.calc.math.*;
import se.swedsoft.bookkeeping.calc.util.SSAutoIncrement;
import se.swedsoft.bookkeeping.calc.util.SSVATUtil;
import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.system.SSInvoiceActionPolicy;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSMail;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.*;
import se.swedsoft.bookkeeping.gui.voucher.SSVoucherFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.print.dialog.*;
import se.swedsoft.bookkeeping.print.report.*;
import se.swedsoft.bookkeeping.print.report.journals.*;
import se.swedsoft.bookkeeping.print.report.sales.*;

import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.util.SSDateUtil;


/**
 * $Id$
 *
 */
public class SSReportFactory {    private static final Logger LOG = LoggerFactory.getLogger(SSReportFactory.class);

    private static final File PDF_FILE_DIR = new File(Path.get(Path.APP_DATA), "pdftoemail");
    private SSReportFactory() {}

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param pYearData
     */
    public static void buildVoucherReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear pYearData) {
        final SSVoucherListDialog iDialog = new SSVoucherListDialog(iMainFrame);

        iDialog.setLocalDateFrom(pYearData.getLocalFrom());
        iDialog.setLocalDateTo(pYearData.getLocalTo());
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        List<SSVoucher> iVouchers = iDialog.getElementsToPrint();

                        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

                        SSVoucherListPrinter iPrinter = new SSVoucherListPrinter(iVouchers);

                        if (iDialog.isDateSelected()) {
                            LocalDate iDateFrom = iDialog.getLocalDateFrom();
                            LocalDate iDateTo = iDialog.getLocalDateTo();

                            iPrinter.addParameter("periodTitle",
                                    String.format(
                                    SSBundle.getBundle().getString("voucherlistreport.period.date"),
                                    iFormat.format(SSDateUtil.toDate(iDateFrom)),
                                    iFormat.format(SSDateUtil.toDate(iDateTo))));
                            iPrinter.addParameter("periodText", " ");
                        }

                        if (iDialog.isNumberSelected()) {
                            Integer iNumberFrom = iDialog.getNumberFrom();
                            Integer iNumberTo = iDialog.getNumberTo();

                            iPrinter.addParameter("periodTitle",
                                    String.format(
                                    SSBundle.getBundle().getString(
                                            "voucherlistreport.period.number"),
                                            iNumberFrom,
                                            iNumberTo));
                            iPrinter.addParameter("periodText", " ");
                        }

                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     * @param pYearData
     */
    public static void MainbookReport(final SSMainFrame iMainFrame, final SSNewAccountingYear pYearData) {
        final SSMainBookDialog iDialog = new SSMainBookDialog(iMainFrame);

        iDialog.setLocalDateFrom(pYearData.getLocalFrom());
        iDialog.setLocalDateTo(pYearData.getLocalTo());

        iDialog.addOkActionListener(e -> {

                iDialog.closeDialog();

                final LocalDate  lDateFrom = iDialog.getLocalDateFrom();
                final LocalDate  lDateTo = iDialog.getLocalDateTo();
                final SSAccount  lAccountFrom = iDialog.getAccountFrom();
                final SSAccount  lAccountTo = iDialog.getAccountTo();

                final SSNewProject    iProject = iDialog.getProject();
                final SSNewResultUnit iResultUnit = iDialog.getResultUnit();
                final boolean      isProjectSelected = iDialog.isProjectSelected();
                final boolean      isResultUnitSelected = iDialog.isResultUnitSelected();

                SSProgressDialog.runProgress(iMainFrame,
                        () -> {

                                SSMainBookPrinter iPrinter = new SSMainBookPrinter(pYearData,
                                        lAccountFrom, lAccountTo, lDateFrom, lDateTo, iProject,
                                        iResultUnit);

                                if (isProjectSelected) {}
                                if (isResultUnitSelected) {}

                                iPrinter.preview(iMainFrame);

                            });


            });
        iDialog.addCancelActionListener(e -> iDialog.closeDialog());
        iDialog.pack();
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();

    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param yearData
     * @param lastYearData
     */
    public static void buildResultReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear yearData, final SSNewAccountingYear lastYearData) {
        LocalDate from = yearData.getLocalFrom();
        LocalDate to = yearData.getLocalTo();

        final SSDialog iDialog = new SSDialog(iMainFrame,
                bundle.getString("resultreport.perioddialog.title"));
        final SSResultPrinterSetupPanel iPanel = new SSResultPrinterSetupPanel();

        iPanel.setLocalFrom(from);
        iPanel.setLocalTo(to);
        iPanel.setPrintBudget(false);
        iPanel.setPrintLastyear(false);
        iPanel.setPrintLastyearEnabled(lastYearData != null);

        iDialog.add(iPanel.getPanel(), BorderLayout.CENTER);

        iPanel.addOkActionListener(e -> {

                final LocalDate lFrom = iPanel.getLocalFrom();
                final LocalDate lTo = iPanel.getLocalTo();
                final boolean lPrintBudget = iPanel.getPrintBudget();
                final boolean lPrintLastyear = iPanel.getPrintLastyear();

                iDialog.closeDialog();

                SSProgressDialog.runProgress(iMainFrame,
                        () -> {

                                SSResultPrinter iPrinter = new SSResultPrinter(lFrom, lTo,
                                        lPrintBudget, lPrintLastyear);

                                iPrinter.preview(iMainFrame);

                            });


            });
        iPanel.addCancelActionListener(e -> iDialog.closeDialog());

	iDialog.getRootPane().setDefaultButton(iPanel.getButtonPanel().getOkButton());

        iDialog.pack();
        iDialog.setLocationRelativeTo(iMainFrame);
        iDialog.setVisible();
    }

    public static void buildOwnReport(final SSMainFrame iMainFrame, final SSOwnReport iOwnReport) {
        SSPeriodSelectionDialog iDateDialog = new SSPeriodSelectionDialog(iMainFrame,
                "Välj period");
        SSNewAccountingYear iYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();

        iDateDialog.setLocalFrom(iYear.getLocalFrom());
        iDateDialog.setLocalTo(iYear.getLocalTo());
        iDateDialog.setLocationRelativeTo(iMainFrame);

        if (iDateDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDateDialog.getLocalFrom();
        final LocalDate iTo = iDateDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSOwnReportPrinter iPrinter = new SSOwnReportPrinter(iFrom, iTo,
                                iOwnReport);

                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void ProjectResultReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, SSNewAccountingYear iAccountingYear) {
        final SSProjectResultSetupDialog iDialog = new SSProjectResultSetupDialog(
                iMainFrame, bundle.getString("resultreport.perioddialog.title"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();
        final SSNewProject iProject = iDialog.getProject();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSProjectResultPrinter iPrinter = new SSProjectResultPrinter(iFrom, iTo,
                                iProject);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void ResultUnitResultReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, SSNewAccountingYear iAccountingYear) {

        final SSResultUnitResultSetupDialog iDialog = new SSResultUnitResultSetupDialog(
                iMainFrame, bundle.getString("resultreport.perioddialog.title"));

        iDialog.setLocalFrom(iAccountingYear.getLocalFrom());
        iDialog.setLocalTo(iAccountingYear.getLocalTo());
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();
        final SSNewResultUnit iResultUnit = iDialog.getSelectedResultUnit();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSResultUnitResultPrinter iPrinter = new SSResultUnitResultPrinter(iFrom,
                                iTo, iResultUnit);

                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void buildBalanceReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear iAccountingYear) {
        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                bundle.getString("balancereport.perioddialog.title"));

        iDialog.setLocalFrom(iAccountingYear.getLocalFrom());
        iDialog.setLocalTo(iAccountingYear.getLocalTo());
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSBalancePrinter iPrinter = new SSBalancePrinter(iFrom, iTo);

                iPrinter.preview(iMainFrame);

            });

    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void buildBudgetReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear iAccountingYear) {
        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                bundle.getString("budgetreport.perioddialog.title"));

        iDialog.setLocalFrom(iAccountingYear.getLocalFrom());
        iDialog.setLocalTo(iAccountingYear.getLocalTo());
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSBudgetPrinter iPrinter = new SSBudgetPrinter(iFrom, iTo);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void VATReport2015(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear iAccountingYear) {
        final String lockString = "voucher"
                + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getId()
                + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getId();
        SSVATReportDialog iDialog = new SSVATReportDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);
        int iResponce = iDialog.showDialog();

        if (iResponce != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

	final int iStartVoucher = iDialog.getStartVoucher();

        // Get the R1, R2 and A accounts
        final SSAccount iAccountR1 = iDialog.getAccountR1();
        final SSAccount iAccountR2 = iDialog.getAccountR2();
        final SSAccount iAccountA = iDialog.getAccountA();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        SSVATReport2015Printer   iPrinter1 = new SSVATReport2015Printer(
                                iAccountingYear, iFrom, iTo,
                                iStartVoucher);
                        SSVATControl2015Printer  iPrinter2 = new SSVATControl2015Printer(
                                iAccountingYear, iFrom, iTo,
                                iStartVoucher);

                        final SSVoucher iVoucher = iPrinter2.getVoucher(iAccountR1, iAccountR2,
                                iAccountA);

                        SSVoucherPrinter  iPrinter3 = new SSVoucherPrinter(iVoucher,
                                bundle.getString("vatbasisreport.title"), iAccountR1, iAccountR2,
                                iAccountA);

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);
                        iPrinter.addReport(iPrinter3);

                        iPrinter.preview(iMainFrame,
                                e -> {

                                        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);
                                        SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame,
                                                SSBundle.getBundle(), "vatcontrol2015.voucherdialog",
                                                iFormat.format(SSDateUtil.toDate(iFrom)),
                                                iFormat.format(SSDateUtil.toDate(iTo)),
                                                iVoucher.getNumber());

                                        int iResponce1 = iDialog1.getResponce();

                                        if (iResponce1 != JOptionPane.OK_OPTION) {
                                            return;
                                        }
                                        se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher, false);

                                        if (SSVoucherFrame.getInstance() != null) {
                                            SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                                        }

                                    });

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void SimplestatementReport(final SSMainFrame iMainFrame) {
        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("simplestatement.dialog.title"));

        iDialog.setLocalFrom(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getLocalFrom());
        iDialog.setLocalTo(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getLocalTo());
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.YES_NO_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSSimpleStatementPrinter iPrinter = new SSSimpleStatementPrinter(iFrom,
                                iTo);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param pVoucher
     * @param pAccountingYear
     * @param pFrom
     * @param pTo
     */
    public static void dialogVATVoucher(final SSMainFrame iMainFrame, final SSVoucher pVoucher,
            final SSNewAccountingYear pAccountingYear, final LocalDate pFrom, final LocalDate pTo) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);

        // Manually construct an input popup
        SSQueryDialog iDialog = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                "vatbasis.dialog", format.format(SSDateUtil.toDate(pFrom)),
                format.format(SSDateUtil.toDate(pTo)),
                pVoucher.getNumber());
        int iResponce = iDialog.getResponce();

        if (iResponce != JOptionPane.YES_NO_OPTION) {
            return;
        }
        se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(pVoucher, false);

        if (SSVoucherFrame.getInstance() != null) {
            SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
        }
    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param yearData
     */
    public static void buildAccountDiagramReport(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear yearData) {
        List<SSAccount> iAccounts = se.swedsoft.bookkeeping.data.system.SSAccountingContext.getAccounts();

        List<SSAccount> iAccountsWithoutSRUCode = SSAccountMath.getAccountsWithoutSRUCode(
                iAccounts);

        if (!iAccountsWithoutSRUCode.isEmpty()) {
            new SSWarningDialog(iMainFrame, "accountdiagramreport.dialogmissingSRUCode");
        }

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSAccountdiagramPrinter iPrinter = new SSAccountdiagramPrinter();

                iPrinter.preview(iMainFrame);

            });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void OrderListReport(final SSMainFrame iMainFrame) {
        SSOrderListDialog iDialog = new SSOrderListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSOrder> iOrders = iDialog.getOrdersToPrint();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSOrderListPrinter iPrinter = new SSOrderListPrinter(iOrders);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void InvoiceListReport(final SSMainFrame iMainFrame) {
        SSInvoiceListDialog iDialog = new SSInvoiceListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSInvoice> iInvoices = iDialog.getInvoicesToPrint();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSInvoiceListPrinter iPrinter = new SSInvoiceListPrinter(iInvoices);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void CreditInvoiceListReport(final SSMainFrame iMainFrame) {
        SSCreditInvoiceListDialog iDialog = new SSCreditInvoiceListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSCreditInvoice> iInvoices = iDialog.getInvoicesToPrint();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSCreditInvoiceListPrinter iPrinter = new SSCreditInvoiceListPrinter(
                                iInvoices);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void TenderListReport(final SSMainFrame iMainFrame) {
        SSTenderListDialog iDialog = new SSTenderListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSTender> iTenders = iDialog.getTendersToPrint();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSTenderListPrinter iPrinter = new SSTenderListPrinter(iTenders);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void PurchaseOrderListReport(final SSMainFrame iMainFrame) {
        SSPurchaseOrderListDialog iDialog = new SSPurchaseOrderListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSPurchaseOrder> iOrders = iDialog.getElementsToPrint();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSPurchaseOrderListPrinter iPrinter = new SSPurchaseOrderListPrinter(
                                iOrders);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void SupplierInvoiceListReport(final SSMainFrame iMainFrame) {
        SSSupplierInvoiceListDialog iDialog = new SSSupplierInvoiceListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSSupplierInvoice> iInvoices = iDialog.getElementsToPrint();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSSupplierInvoiceListPrinter iPrinter = new SSSupplierInvoiceListPrinter(
                                iInvoices);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void SupplierCreditInvoiceListReport(final SSMainFrame iMainFrame) {
        SSSupplierCreditInvoiceListDialog iDialog = new SSSupplierCreditInvoiceListDialog(
                iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSSupplierCreditInvoice> iInvoices = iDialog.getElementsToPrint();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSSupplierCreditInvoiceListPrinter iPrinter = new SSSupplierCreditInvoiceListPrinter(
                                iInvoices);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void InventoryList(final SSMainFrame iMainFrame) {
        final SSInventoryListDialog iDialog = new SSInventoryListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final List<SSInventory> iInventories = iDialog.getElementsToPrint();
        final boolean           isDateSelected = iDialog.isDateSelected();
        final boolean           isProductSelected = iDialog.isProductSelected();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSInventoryListPrinter iPrinter = new SSInventoryListPrinter(iInventories);

                        if (isDateSelected) {
                            iPrinter.addParameter("dateFrom", SSDateUtil.toDate(iDialog.getLocalDateFrom()));
                            iPrinter.addParameter("dateTo", SSDateUtil.toDate(iDialog.getLocalDateTo()));
                        }
                        if (isProductSelected) {
                            SSProduct iProduct = iDialog.getProduct();

                            iPrinter.addParameter("periodTitle",
                                    SSBundle.getBundle().getString(
                                    "inventorylistreport.producttitle"));
                            iPrinter.addParameter("periodText",
                                    iProduct == null ? null : iProduct.getNumber());
                        }

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void IndeliveryList(final SSMainFrame iMainFrame) {
        final SSIndeliveryListDialog iDialog = new SSIndeliveryListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final List<SSIndelivery> iIndeliveries = iDialog.getElementsToPrint();
        final boolean            isDateSelected = iDialog.isDateSelected();
        final boolean            isProductSelected = iDialog.isProductSelected();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSIndeliveryListPrinter iPrinter = new SSIndeliveryListPrinter(
                                iIndeliveries);

                        if (isDateSelected) {
                            iPrinter.addParameter("dateFrom", SSDateUtil.toDate(iDialog.getLocalDateFrom()));
                            iPrinter.addParameter("dateTo", SSDateUtil.toDate(iDialog.getLocalDateTo()));
                        }
                        if (isProductSelected) {
                            SSProduct iProduct = iDialog.getProduct();

                            iPrinter.addParameter("periodTitle",
                                    SSBundle.getBundle().getString(
                                    "indeliverylistreport.producttitle"));
                            iPrinter.addParameter("periodText",
                                    iProduct == null ? null : iProduct.getNumber());
                        }
                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void OutdeliveryList(final SSMainFrame iMainFrame) {
        final SSOutdeliveryListDialog iDialog = new SSOutdeliveryListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final List<SSOutdelivery> iOutdeliveries = iDialog.getElementsToPrint();
        final boolean             isDateSelected = iDialog.isDateSelected();
        final boolean             isProductSelected = iDialog.isProductSelected();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSOutdeliveryListPrinter iPrinter = new SSOutdeliveryListPrinter(
                                iOutdeliveries);

                        if (isDateSelected) {
                            LocalDate iDateFrom = iDialog.getLocalDateFrom();
                            LocalDate iDateTo = iDialog.getLocalDateTo();

                            iPrinter.addParameter("dateFrom", SSDateUtil.toDate(iDateFrom));
                            iPrinter.addParameter("dateTo", SSDateUtil.toDate(iDateTo));
                        }
                        if (isProductSelected) {
                            SSProduct iProduct = iDialog.getProduct();

                            iPrinter.addParameter("periodTitle",
                                    SSBundle.getBundle().getString(
                                    "indeliverylistreport.producttitle"));
                            iPrinter.addParameter("periodText",
                                    iProduct == null ? null : iProduct.getNumber());
                        }
                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void InpaymentList(final SSMainFrame iMainFrame) {
        final SSInpaymentListDialog iDialog = new SSInpaymentListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSInpayment> iInpayments = iDialog.getElementsToPrint();
        final boolean           isDateSelected = iDialog.isDateSelected();
        final boolean           isInvoiceSelected = iDialog.isInvoiceSelected();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSInpaymentListPrinter iPrinter = new SSInpaymentListPrinter(iInpayments);

                        if (isDateSelected) {
                            LocalDate iDateFrom = iDialog.getLocalDateFrom();
                            LocalDate iDateTo = iDialog.getLocalDateTo();

                            iPrinter.addParameter("dateFrom", SSDateUtil.toDate(iDateFrom));
                            iPrinter.addParameter("dateTo", SSDateUtil.toDate(iDateTo));
                        }
                        if (isInvoiceSelected) {
                            SSInvoice iInvoice = iDialog.getInvoice();

                            iPrinter.addParameter("periodTitle",
                                    SSBundle.getBundle().getString(
                                    "inpaymentlistreport.invoicetitle"));
                            iPrinter.addParameter("periodText",
                                    iInvoice == null ? null : iInvoice.getNumber().toString());
                        }

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void OutpaymentList(final SSMainFrame iMainFrame) {
        final SSOutpaymentListDialog iDialog = new SSOutpaymentListDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final List<SSOutpayment> iInpayments = iDialog.getElementsToPrint();
        final boolean           isDateSelected = iDialog.isDateSelected();
        final boolean           isInvoiceSelected = iDialog.isInvoiceSelected();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSOutpaymentListPrinter iPrinter = new SSOutpaymentListPrinter(iInpayments);

                        if (isDateSelected) {
                            iPrinter.addParameter("dateFrom", SSDateUtil.toDate(iDialog.getLocalDateFrom()));
                            iPrinter.addParameter("dateTo", SSDateUtil.toDate(iDialog.getLocalDateTo()));
                        }
                        if (isInvoiceSelected) {
                            SSSupplierInvoice iInvoice = iDialog.getInvoice();

                            iPrinter.addParameter("periodTitle",
                                    SSBundle.getBundle().getString(
                                    "outpaymentlistreport.invoicetitle"));
                            iPrinter.addParameter("periodText",
                                    iInvoice == null ? null : iInvoice.getNumber().toString());
                        }

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void StockValue(final SSMainFrame iMainFrame) {
        SSStockValueDialog iDialog = new SSStockValueDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iDate = iDialog.getLocalDate();
        final boolean isDateSelected = iDialog.isDateSelected();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSStockValuePrinter iPrinter;

                if (isDateSelected) {
                    iPrinter = new SSStockValuePrinter(iDate);
                } else {
                    iPrinter = new SSStockValuePrinter();
                }

                if (isDateSelected) {}

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void StockAccount(final SSMainFrame iMainFrame) {

        SSStockAccountDialog iDialog = new SSStockAccountDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final LocalDate iDate = iDialog.getLocalDate();
        final boolean iDateSelected = iDialog.isDateSelected();

        SSProgressDialog.runProgress(iMainFrame, () -> {


                SSStockAccountPrinter iPrinter;

                if (iDateSelected) {
                    iPrinter = new SSStockAccountPrinter(iDate);
                } else {
                    iPrinter = new SSStockAccountPrinter();
                }
                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void AccountsRecievableReport(final SSMainFrame iMainFrame) {
        SSAccountsrecievableDialog iDialog = new SSAccountsrecievableDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final LocalDate iDate = iDialog.getLocalDate();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSAccountsRecievablePrinter iPrinter = new SSAccountsRecievablePrinter(
                                iDate);

                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void CustomerClaimReport(final SSMainFrame iMainFrame) {
        SSCustomerclaimDialog iDialog = new SSCustomerclaimDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iDate = iDialog.getLocalDate();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSCustomerclaimPrinter iPrinter = new SSCustomerclaimPrinter(iDate);

                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void AccountsPayableReport(final SSMainFrame iMainFrame) {
        SSAccountsPayableDialog iDialog = new SSAccountsPayableDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iDate = iDialog.getLocalDate();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSAccountsPayablePrinter iPrinter = new SSAccountsPayablePrinter(iDate);

                iPrinter.preview(iMainFrame);

            });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void SupplierDebtReport(final SSMainFrame iMainFrame) {
        SSSupplierdebtDialog iDialog = new SSSupplierdebtDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final LocalDate iDate = iDialog.getLocalDate();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSSupplierdebtPrinter iPrinter = new SSSupplierdebtPrinter(iDate);

                iPrinter.preview(iMainFrame);

            });

    }

    /**
     * Försäljningsrapport
     *
     * @param iMainFrame
     */
    public static void SaleReport(final SSMainFrame iMainFrame) {
        SSSaleReportDialog iDialog = new SSSaleReportDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        final LocalDate                       iFrom = iDialog.getLocalFrom();
        final LocalDate                       iTo = iDialog.getLocalTo();
        final SSSaleReportPrinter.SortingMode iSortingMode = iDialog.getSortingMode();
        final boolean                         iAscending = iDialog.getAscending();

        SSProgressDialog.runProgress(iMainFrame,
                SSBundle.getBundle().getString("salereport.title"),
                () -> {

                        SSSaleReportPrinter iPrinter = new SSSaleReportPrinter(
                                iFrom, iTo, iSortingMode,
                                iAscending);

                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void Salevalues(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear iAccountingYear) {
        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                bundle.getString("salevalues.perioddialog.title"));

        if (iAccountingYear != null) {
            iDialog.setLocalFrom(iAccountingYear.getLocalFrom());
            iDialog.setLocalTo(iAccountingYear.getLocalTo());
        } else {
            LocalDate now = LocalDate.now();
            iDialog.setLocalFrom(now);
            iDialog.setLocalTo(now.plusMonths(1));
        }
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSSalevaluesPrinter iPrinter = new SSSalevaluesPrinter(iFrom, iTo);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     * @param bundle
     * @param iAccountingYear
     */
    public static void Purchasevalues(final SSMainFrame iMainFrame, final ResourceBundle bundle, final SSNewAccountingYear iAccountingYear) {
        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                bundle.getString("purchasevalues.perioddialog.title"));

        if (iAccountingYear != null) {
            iDialog.setLocalFrom(iAccountingYear.getLocalFrom());
            iDialog.setLocalTo(iAccountingYear.getLocalTo());
        } else {
            LocalDate now = LocalDate.now();
            iDialog.setLocalFrom(now);
            iDialog.setLocalTo(now.plusMonths(1));
        }
        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSPurchasevaluePrinter iPrinter = new SSPurchasevaluePrinter(iFrom, iTo);

                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     * @param iInvoices
     */
    public static void InvoiceReport(final SSMainFrame iMainFrame, final List<SSInvoice> iInvoices) {
        final List<SSInvoice> iAllowedInvoices = iInvoices.stream()
                .filter(SSInvoiceActionPolicy::canPrint)
                .collect(Collectors.toList());
        if (iAllowedInvoices.isEmpty()) {
            return;
        }

        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.invoice"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSInvoice iInvoice : iAllowedInvoices) {
                            SSInvoicePrinter iInvoicePrinter = new SSInvoicePrinter(iInvoice,
                                    iLanguage);

                            iPrinter.addReport(iInvoicePrinter);
                        }
                        Runnable iEmailAction = null;
                        if (iAllowedInvoices.size() == 1) {
                            SSInvoice iSingleInvoice = iAllowedInvoices.get(0);
                            iEmailAction = () -> {
                                if (!SSMail.isOk(iSingleInvoice.getCustomer())) {
                                    return;
                                }
                                if (sendInvoiceByEmail(iSingleInvoice, iLanguage)) {
                                    markInvoicesAsPrinted(Collections.singletonList(iSingleInvoice));
                                }
                            };
                        }

                        iPrinter.preview(iMainFrame, () -> markInvoicesAsPrinted(iAllowedInvoices), iEmailAction,
                                true);


                    });
    }

    public static void EmailInvoiceReport(final SSMainFrame iMainFrame, final SSInvoice iInvoice) {
        if (!SSInvoiceActionPolicy.canSendByEmail(iInvoice)) {
            return;
        }

        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.invoice"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {
                        if (sendInvoiceByEmail(iInvoice, iLanguage)) {
                            markInvoicesAsPrinted(Collections.singletonList(iInvoice));
                        }
                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iInvoices
     */
    public static void OCRInvoiceReport(final SSMainFrame iMainFrame, final List<SSInvoice> iInvoices) {
        final List<SSInvoice> iAllowedInvoices = iInvoices.stream()
                .filter(SSInvoiceActionPolicy::canPrint)
                .collect(Collectors.toList());
        if (iAllowedInvoices.isEmpty()) {
            return;
        }

        SSOCRInvoiceDialog iDialog = new SSOCRInvoiceDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.ocrinvoice"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale  iLanguage = iDialog.getLanguage();
        final boolean iShowBackground = iDialog.doShowBackground();

        for (SSInvoice iInvoice : iAllowedInvoices) {
            String iOCRNumber = SSOCRNumber.getOCRNumber(iInvoice);

            iInvoice.setOCRNumber(iOCRNumber);
            se.swedsoft.bookkeeping.data.system.SSSalesContext.updateInvoice(iInvoice);
        }

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSInvoice iInvoice : iAllowedInvoices) {
                            SSOCRInvoicePrinter iInvoicePrinter = new SSOCRInvoicePrinter(iInvoice,
                                    iLanguage, iShowBackground);

                            iPrinter.addReport(iInvoicePrinter);
                        }
                        iPrinter.preview(iMainFrame, () -> markInvoicesAsPrinted(iAllowedInvoices));

                    });
    }

    private static void markInvoicesAsPrinted(List<SSInvoice> iInvoices) {
        for (SSInvoice iInvoice : iInvoices) {
            if (!iInvoice.isPrinted()) {
                iInvoice.setPrinted();
                se.swedsoft.bookkeeping.data.system.SSSalesContext.updateInvoice(iInvoice);
            }
        }
    }

    private static boolean sendInvoiceByEmail(SSInvoice iInvoice, Locale iLanguage) {
        SSMultiPrinter iPrinter = new SSMultiPrinter();
        SSInvoicePrinter iInvoicePrinter = new SSInvoicePrinter(iInvoice, iLanguage);

        iPrinter.addReport(iInvoicePrinter);
        iPrinter.generateReport();
        iPrinter.getPrinter();

        String iFileName = "faktura.pdf";
        if (!PDF_FILE_DIR.exists()) {
            PDF_FILE_DIR.mkdirs();
        }

        try {
            JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                    new File(PDF_FILE_DIR, iFileName).getPath());
        } catch (JRException e) {
            LOG.error("Unexpected error", e);
        }

        String iSubject = "Faktura " + iInvoice.getNumber() + " från "
                + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

        try {
            if (!SSMail.sendMail(iInvoice.getCustomer().getEMail(), iSubject, iFileName)) {
                return false;
            }
        } catch (MessagingException e) {
            LOG.error("Unexpected error", e);
            new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
            return false;
        }
        SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");
        return true;
    }

    /**
     *
     * @param iMainFrame
     * @param iCreditInvoices
     */
    public static void CreditInvoiceReport(final SSMainFrame iMainFrame, final List<SSCreditInvoice> iCreditInvoices) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.creditinvoice"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            iCreditInvoice.setPrinted();
            se.swedsoft.bookkeeping.data.system.SSSalesContext.updateCreditInvoice(iCreditInvoice);
        }

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
                            SSCreditinvoicePrinter iCreditinvoicePrinter = new SSCreditinvoicePrinter(
                                    iCreditInvoice, iLanguage);

                            iPrinter.addReport(iCreditinvoicePrinter);
                        }
                        iPrinter.preview(iMainFrame);


                    });
    }

    public static void EmailCreditInvoiceReport(final SSMainFrame iMainFrame, final SSCreditInvoice iCreditInvoice) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.creditinvoice"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        if (sendCreditInvoiceByEmail(iCreditInvoice, iLanguage)) {
                            iCreditInvoice.setPrinted();
                            se.swedsoft.bookkeeping.data.system.SSSalesContext.updateCreditInvoice(iCreditInvoice);
                        }

                    });
    }

    private static boolean sendCreditInvoiceByEmail(SSCreditInvoice iCreditInvoice, Locale iLanguage) {
        SSMultiPrinter iPrinter = new SSMultiPrinter();
        SSCreditinvoicePrinter iCreditInvoicePrinter = new SSCreditinvoicePrinter(iCreditInvoice, iLanguage);

        iPrinter.addReport(iCreditInvoicePrinter);

        iPrinter.generateReport();
        iPrinter.getPrinter();
        String iFileName = "kreditfaktura.pdf";
        if (!PDF_FILE_DIR.exists()) {
            PDF_FILE_DIR.mkdirs();
        }
        try {
            JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                    new File(PDF_FILE_DIR, iFileName).getPath());
        } catch (JRException e) {
            LOG.error("Unexpected error", e);
        }
        String iSubject = "Kreditfaktura " + iCreditInvoice.getNumber()
                + " från " + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

        try {
            if (!SSMail.sendMail(iCreditInvoice.getCustomer().getEMail(), iSubject, iFileName)) {
                return false;
            }
        } catch (MessagingException e) {
            LOG.error("Unexpected error", e);
            new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
            return false;
        }
        SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");
        return true;
    }

    /**
     *
     * @param iMainFrame
     * @param iOrders
     */
    public static void OrderReport(final SSMainFrame iMainFrame, final List<SSOrder> iOrders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.order"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        for (SSOrder iOrder : iOrders) {
            iOrder.setPrinted();
            se.swedsoft.bookkeeping.data.system.SSSalesContext.updateOrder(iOrder);
        }

        SSProgressDialog.runProgress(iMainFrame, () -> {

                SSMultiPrinter iPrinter = new SSMultiPrinter();

                for (SSOrder iOrder : iOrders) {
                    SSOrderPrinter iOrderPrinter = new SSOrderPrinter(iOrder, iLanguage);

                    iPrinter.addReport(iOrderPrinter);
                }
                iPrinter.preview(iMainFrame);

            });
    }

    /**
     *
     * @param iMainFrame
     * @param iOrder
     */
    public static void EmailOrderReport(final SSMainFrame iMainFrame, final SSOrder iOrder) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.order"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        iOrder.setPrinted();
        se.swedsoft.bookkeeping.data.system.SSSalesContext.updateOrder(iOrder);
        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                                      SSMultiPrinter iPrinter = new SSMultiPrinter();
                                      SSOrderPrinter iOrderPrinter = new SSOrderPrinter(iOrder, iLanguage);

                                      iPrinter.addReport(iOrderPrinter);

                                      iPrinter.generateReport();
                                      iPrinter.getPrinter();
                                      String iFileName = "order.pdf";
                        if (!PDF_FILE_DIR.exists()) {
                            PDF_FILE_DIR.mkdirs();
                        }

                                      try {
                                          JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                                                  new File(PDF_FILE_DIR, iFileName).getPath());
                                      } catch (JRException e) {
                                          LOG.error("Unexpected error", e);
                                      }
                                      String iSubject = "Order " + iOrder.getNumber() + " från "
                                              + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

                                      try {
                                          if (iOrder.getCustomer() != null) {
                                              if (!SSMail.sendMail(iOrder.getCustomer().getEMail(), iSubject,
                                                      iFileName)) {
                                                  return;
                                              }
                                          }
                                      } catch (MessagingException e) {
                                          LOG.error("Unexpected error", e);
                                          new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
                                          return;
                                      }
                                      SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iTenders
     */
    public static void TenderReport(final SSMainFrame iMainFrame, final List<SSTender> iTenders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.tender"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        for (SSTender iTender : iTenders) {
            iTender.setPrinted();
            Repositories.tenders().update(iTender);
        }
        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSTender iTender : iTenders) {
                            SSTenderPrinter iTenderPrinter = new SSTenderPrinter(iTender,
                                    iLanguage);

                            iPrinter.addReport(iTenderPrinter);
                        }
                        iPrinter.preview(iMainFrame);

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iTender
     */
    public static void EmailTenderReport(final SSMainFrame iMainFrame, final SSTender iTender) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.tender"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        iTender.setPrinted();
        Repositories.tenders().update(iTender);

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                                      SSMultiPrinter iPrinter = new SSMultiPrinter();
                                      SSTenderPrinter iTenderPrinter = new SSTenderPrinter(iTender, iLanguage);

                                      iPrinter.addReport(iTenderPrinter);

                                      iPrinter.generateReport();
                                      iPrinter.getPrinter();
                                      String iFileName = "offert.pdf";
                        if (!PDF_FILE_DIR.exists()) {
                            PDF_FILE_DIR.mkdirs();
                        }

                                      try {
                                          JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                                                  new File(PDF_FILE_DIR, iFileName).getPath());
                                      } catch (JRException e) {
                                          LOG.error("Unexpected error", e);
                                      }
                                      String iSubject = "Offert " + iTender.getNumber() + " från "
                                              + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

                                      try {
                                          if (iTender.getCustomer() != null) {
                                              if (!SSMail.sendMail(iTender.getCustomer().getEMail(), iSubject,
                                                      iFileName)) {
                                                  return;
                                              }
                                          }
                                      } catch (MessagingException e) {
                                          LOG.error("Unexpected error", e);
                                          new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
                                          return;
                                      }
                                      SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iOrders
     */
    public static void PickingslipReport(final SSMainFrame iMainFrame, final List<SSOrder> iOrders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.pickingslip"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSOrder iOrder : iOrders) {
                            SSPickingslipPrinter iPickingslipPrinter = new SSPickingslipPrinter(
                                    iOrder, iLanguage);

                            iPrinter.addReport(iPickingslipPrinter);
                        }
                        iPrinter.preview(iMainFrame);

                    });

    }

    /**
     *
     * @param iMainFrame
     * @param iOrders
     */
    public static void DeliverynoteReport(final SSMainFrame iMainFrame, final List<SSOrder> iOrders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.deliverynote"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSOrder iOrder : iOrders) {
                            SSDeliverynotePrinter iDeliverynotePrinter = new SSDeliverynotePrinter(
                                    iOrder, iLanguage);

                            iPrinter.addReport(iDeliverynotePrinter);
                        }
                        iPrinter.preview(iMainFrame);


                    });

    }

    /**
     *
     * @param iMainFrame
     * @param iPurchaseOrders
     */
    public static void PurchaseOrderReport(final SSMainFrame iMainFrame, final  List<SSPurchaseOrder> iPurchaseOrders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.purchaseorder"));

        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        for (SSPurchaseOrder iPurchaseOrder : iPurchaseOrders) {
            iPurchaseOrder.setPrinted();
            Repositories.purchaseOrders().update(iPurchaseOrder);
        }
        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSPurchaseOrder iPurchaseOrder : iPurchaseOrders) {
                            SSPurchaseOrderPrinter iPurchaseOrderPrinter = new SSPurchaseOrderPrinter(
                                    iPurchaseOrder, iLanguage);

                            iPrinter.addReport(iPurchaseOrderPrinter);
                        }
                        iPrinter.preview(iMainFrame);


                    });
    }

    public static void EmailPurchaseOrderReport(final SSMainFrame iMainFrame, final SSPurchaseOrder iPurchaseOrder) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.purchaseorder"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        iPurchaseOrder.setPrinted();
        Repositories.purchaseOrders().update(iPurchaseOrder);

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                                      SSMultiPrinter iPrinter = new SSMultiPrinter();
                                      SSPurchaseOrderPrinter iPurchaseOrderPrinter = new SSPurchaseOrderPrinter(
                                              iPurchaseOrder, iLanguage);

                                      iPrinter.addReport(iPurchaseOrderPrinter);

                                      iPrinter.generateReport();
                                      iPrinter.getPrinter();
                                      String iFileName = "inkopsorder.pdf";
                        if (!PDF_FILE_DIR.exists()) {
                            PDF_FILE_DIR.mkdirs();
                        }

                                      try {
                                          JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                                                  new File(PDF_FILE_DIR, iFileName).getPath());
                                      } catch (JRException e) {
                                          LOG.error("Unexpected error", e);
                                      }
                                      String iSubject = "Inköpsorder " + iPurchaseOrder.getNumber() + " från "
                                              + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

                                      try {
                                          if (!SSMail.sendMail(iPurchaseOrder.getSupplier().getEMail(), iSubject,
                                                  iFileName)) {
                                              return;
                                          }
                                      } catch (MessagingException e) {
                                          LOG.error("Unexpected error", e);
                                          new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
                                          return;
                                      }
                                      SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");

                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iPurchaseOrders
     */
    public static void InquiryReport(final SSMainFrame iMainFrame, final  List<SSPurchaseOrder> iPurchaseOrders) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.inquiry"));

        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        for (SSPurchaseOrder iPurchaseOrder : iPurchaseOrders) {
                            SSInquiryPrinter iInquiryPrinter = new SSInquiryPrinter(iPurchaseOrder,
                                    iLanguage);

                            iPrinter.addReport(iInquiryPrinter);
                        }
                        iPrinter.preview(iMainFrame);


                    });
    }

    public static void EmailInquiryReport(final SSMainFrame iMainFrame, final  SSPurchaseOrder iPurchaseOrder) {
        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.inquiry"));

        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                                      SSMultiPrinter iPrinter = new SSMultiPrinter();
                                      SSInquiryPrinter iInquiryPrinter = new SSInquiryPrinter(iPurchaseOrder,
                                              iLanguage);

                                      iPrinter.addReport(iInquiryPrinter);
                                      iPrinter.generateReport();
                                      iPrinter.getPrinter();
                                      String iFileName = "forfragan.pdf";
                        if (!PDF_FILE_DIR.exists()) {
                            PDF_FILE_DIR.mkdirs();
                        }

                                      try {
                                          JasperExportManager.exportReportToPdfFile(iPrinter.getPrinter(),
                                                  new File(PDF_FILE_DIR, iFileName).getPath());
                                      } catch (JRException e) {
                                          LOG.error("Unexpected error", e);
                                      }
                                      String iSubject = "Förfrågan från "
                                              + se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getName();

                                      try {
                                          if (!SSMail.sendMail(iPurchaseOrder.getSupplier().getEMail(), iSubject,
                                                  iFileName)) {
                                              return;
                                          }
                                      } catch (MessagingException e) {
                                          LOG.error("Unexpected error", e);
                                          new SSErrorDialog(SSMainFrame.getInstance(), "mail.somethingwrong");
                                          return;
                                      }
                                      SSInformationDialog.showDialog(SSMainFrame.getInstance(), "mail.success");


                    });
    }

    /**
     *
     * @param iMainFrame
     * @param iInvoices
     */
    public static void ReminderReport(final SSMainFrame iMainFrame, final List<SSInvoice> iInvoices) {
        final List<SSInvoice> iAllowedInvoices = iInvoices.stream()
                .filter(SSInvoiceActionPolicy::canSelectForReminder)
                .collect(Collectors.toList());
        if (iAllowedInvoices.isEmpty()) {
            return;
        }

        SSLanguageDialog iDialog = new SSLanguageDialog(iMainFrame,
                SSBundle.getBundle().getString("report.title.reminder"));

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final Locale iLanguage = iDialog.getLanguage();

        final InternalFrameAdapter iRegisterAdapter = new InternalFrameAdapter() {

            /**
             * Invoked when an internal frame has been closed.
             */
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                if (SSQueryDialog.showDialog(iMainFrame, SSBundle.getBundle(),
                        "invoiceframe.registerremainder")
                        != JOptionPane.OK_OPTION) {
                    return;
                }
                for (SSInvoice iInvoice : iAllowedInvoices) {
                    iInvoice.setNumRemainders(iInvoice.getNumReminders() + 1);
                    se.swedsoft.bookkeeping.data.system.SSSalesContext.updateInvoice(iInvoice);
                }
            }
        };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        Map<SSCustomer, List<SSInvoice>> iInvoicesPerCustomer = new HashMap<>();

                        for (SSInvoice iInvoice : iAllowedInvoices) {
                            SSCustomer iCustomer = iInvoice.getCustomer();

                            List<SSInvoice> iInvoicesForCustomer = iInvoicesPerCustomer.get(
                                    iCustomer);

                            if (iInvoicesForCustomer == null) {
                                iInvoicesForCustomer = new LinkedList<>();

                                iInvoicesPerCustomer.put(iCustomer, iInvoicesForCustomer);
                            }
                            iInvoicesForCustomer.add(iInvoice);
                        }
                        // Create the multi report
                        SSMultiPrinter iMultiPrinter = new SSMultiPrinter();

                        // Get the invoices for the customer
                        for (Map.Entry<SSCustomer, List<SSInvoice>> ssCustomerListEntry : iInvoicesPerCustomer.entrySet()) {
                            List<SSInvoice> iInvoicesForCustomer = ssCustomerListEntry.getValue();

                            SSReminderPrinter iPrinter = new SSReminderPrinter(
                                    iInvoicesForCustomer, ssCustomerListEntry.getKey(), iLanguage);

                            iMultiPrinter.addReport(iPrinter);
                        }
                        iMultiPrinter.preview(iMainFrame, iRegisterAdapter);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void QuarterReport(final SSMainFrame iMainFrame) {
        SSQuarterReportDialog iDialog = new SSQuarterReportDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }

        final LocalDate iDate = iDialog.getLocalDate();
        final LocalDate iEndDate = iDialog.getLocalEndDate();

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSQuarterReportPrinter iPrinter = new SSQuarterReportPrinter(
                                Locale.getDefault(), iDate, iEndDate);

                        iPrinter.preview(iMainFrame);

                    });
    }

    // Journals

    /**
     *
     * @param iMainFrame
     */
    public static void InvoiceJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("invoicejournal.dialog.title"));
        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        iDialog.setLocationRelativeTo(iMainFrame);
        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSInvoice> iInvoices = se.swedsoft.bookkeeping.data.system.SSSalesContext.getInvoices();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSInvoice> iFiltered = iInvoices.stream()
                .filter(iInvoice -> SSInvoiceActionPolicy.canPostToJournal(iInvoice)
                        && SSInvoiceMath.inPeriod(iInvoice, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame, "invoicejournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("invoicejournal") + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "invoicejournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSInvoice iInvoice : iFiltered) {
            SSVoucher iCurrent = iInvoice.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "invoicejournal.dialog.register", iNumber, iVoucher1.getNumber());

                if (iDialog1.getResponce() != JOptionPane.YES_NO_OPTION) {
                    return;
                }
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                String iJournalNumbers = "FA" + iNumber;
                // Mark all invoices as entered
                for (SSInvoice iInvoice : iFiltered) {
                    if (!SSInvoiceActionPolicy.canPostToJournal(iInvoice)) {
                        continue;
                    }
                    iInvoice.setJournalNumbers(iJournalNumbers);
                    iInvoice.setVoucher(iVoucher1);
                    iInvoice.setEntered();
                    se.swedsoft.bookkeeping.data.system.SSSalesContext.updateInvoice(iInvoice);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement("invoicejournal");

                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSInvoicejournalPrinter iPrinter1 = new SSInvoicejournalPrinter(iFiltered,
                                iNumber, iTo);

                        SSVoucherPrinter iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void CreditInvoiceJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("creditinvoicejournal.dialog.title"));

        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSCreditInvoice> iCreditInvoices = se.swedsoft.bookkeeping.data.system.SSSalesContext.getCreditInvoices();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSCreditInvoice> iFiltered = iCreditInvoices.stream()
                .filter(iCreditInvoice -> !iCreditInvoice.isEntered()
                        && SSInvoiceMath.inPeriod(iCreditInvoice, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame, "creditinvoicejournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("creditinvoicejournal") + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "creditinvoicejournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSCreditInvoice iCreditInvoice : iFiltered) {
            SSVoucher iCurrent = iCreditInvoice.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "creditinvoicejournal.dialog.register", iNumber,
                        iVoucher1.getNumber());

                if (iDialog1.getResponce() != JOptionPane.YES_NO_OPTION) {
                    return;
                }
                // Mark all invoices as entered
                for (SSCreditInvoice iCreditInvoice : iFiltered) {
                    iCreditInvoice.setEntered();
                    se.swedsoft.bookkeeping.data.system.SSSalesContext.updateCreditInvoice(iCreditInvoice);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement("creditinvoicejournal");
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);

                // Add the voucher to the database.
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSCreditinvoicejournalPrinter iPrinter1 = new SSCreditinvoicejournalPrinter(
                                iFiltered, iNumber, iTo);

                        SSVoucherPrinter            iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void InpaymentJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("inpaymentjournal.dialog.title"));

        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        /* iDialog.setFrom( se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getFrom() );
         iDialog.setTo  ( se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getTo()   );*/

        iDialog.setLocationRelativeTo(iMainFrame);

        if (iDialog.showDialog() != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSInpayment> iInpayments = Repositories.inpayments().findAll();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSInpayment> iFiltered = iInpayments.stream()
                .filter(iInpayment -> !iInpayment.isEntered()
                        && SSInpaymentMath.inPeriod(iInpayment, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame, "inpaymentjournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("inpaymentjournal") + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "inpaymentjournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSInpayment iInpayment : iFiltered) {
            SSVoucher iCurrent = iInpayment.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "inpaymentjournal.dialog.register", iNumber, iVoucher1.getNumber());

                if (iDialog1.getResponce() != JOptionPane.YES_NO_OPTION) {
                    return;
                }
                // Mark all invoices as entered
                for (SSInpayment iInpayment : iFiltered) {
                    iInpayment.setEntered();
                    Repositories.inpayments().update(iInpayment);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement("inpaymentjournal");
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);

                // Add the voucher to the database.
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSInpaymentjournalPrinter iPrinter1 = new SSInpaymentjournalPrinter(
                                iFiltered, iNumber, iTo);

                        SSVoucherPrinter            iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });

    }

    /**
     *
     * @param iMainFrame
     */
    public static void SupplierInvoiceJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("supplierinvoicejournal.dialog.title"));

        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        iDialog.setLocationRelativeTo(iMainFrame);

        int iResponce = iDialog.showDialog();

        if (iResponce != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSSupplierInvoice> iInvoices = Repositories.supplierInvoices().findAll();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSSupplierInvoice> iFiltered = iInvoices.stream()
                .filter(iInvoice -> !iInvoice.isEntered()
                        && SSSupplierInvoiceMath.inPeriod(iInvoice, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame, "supplierinvoicejournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("supplierinvoicejournal") + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "supplierinvoicejournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSSupplierInvoice iInvoice : iFiltered) {
            SSVoucher iCurrent = iInvoice.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "supplierinvoicejournal.dialog.register", iNumber,
                        iVoucher1.getNumber());
                int iResponce1 = iDialog1.getResponce();

                if (iResponce1 != JOptionPane.YES_NO_OPTION) {
                    return;
                }
                // Mark all invoices as entered
                for (SSSupplierInvoice iInvoice : iFiltered) {
                    iInvoice.setEntered();
                    Repositories.supplierInvoices().update(iInvoice);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement(
                        "supplierinvoicejournal");
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);
                // Add the voucher to the database.
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSSupplierInvoicejournalPrinter iPrinter1 = new SSSupplierInvoicejournalPrinter(
                                iFiltered, iNumber, iTo);

                        SSVoucherPrinter     iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void SupplierCreditInvoiceJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("suppliercreditinvoicejournal.dialog.title"));

        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        iDialog.setLocationRelativeTo(iMainFrame);

        int iResponce = iDialog.showDialog();

        if (iResponce != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSSupplierCreditInvoice> iInvoices = Repositories.supplierCreditInvoices().findAll();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSSupplierCreditInvoice> iFiltered = iInvoices.stream()
                .filter(iInvoice -> !iInvoice.isEntered()
                        && SSSupplierCreditInvoiceMath.inPeriod(iInvoice, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame,
                    "suppliercreditinvoicejournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("suppliercreditinvoicejournal")
                + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "suppliercreditinvoicejournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSSupplierCreditInvoice iInvoice : iFiltered) {
            SSVoucher iCurrent = iInvoice.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "suppliercreditinvoicejournal.dialog.register", iNumber,
                        iVoucher1.getNumber());
                int iResponce1 = iDialog1.getResponce();

                if (iResponce1 != JOptionPane.YES_NO_OPTION) {
                    return;
                }
                // Mark all invoices as entered
                for (SSSupplierCreditInvoice iInvoice : iFiltered) {
                    iInvoice.setEntered();
                    Repositories.supplierCreditInvoices().update(iInvoice);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement(
                        "suppliercreditinvoicejournal");
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);
                // Add the voucher to the database.
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSSuppliercreditinvoicejournalPrinter iPrinter1 = new SSSuppliercreditinvoicejournalPrinter(
                                iFiltered, iNumber, iTo);

                        SSVoucherPrinter     iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });
    }

    /**
     *
     * @param iMainFrame
     */
    public static void OutpaymentJournal(final SSMainFrame iMainFrame) {
        SSAutoIncrement iAutoIncrement = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getAutoIncrement();

        SSPeriodSelectionDialog iDialog = new SSPeriodSelectionDialog(iMainFrame,
                SSBundle.getBundle().getString("outpaymentjournal.dialog.title"));

        java.time.LocalDate prevMonth = java.time.LocalDate.now().minusMonths(1);
        LocalDate iFirstDayOfMonth = prevMonth.withDayOfMonth(1);
        LocalDate iLastDayOfMonth = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());

        iDialog.setLocalFrom(iFirstDayOfMonth);
        iDialog.setLocalTo(iLastDayOfMonth);

        iDialog.setLocationRelativeTo(iMainFrame);
        int iResponce = iDialog.showDialog();

        if (iResponce != JOptionPane.OK_OPTION) {
            return;
        }
        List<SSOutpayment> iOutpayments = Repositories.outpayments().findAll();

        final LocalDate iFrom = iDialog.getLocalFrom();
        final LocalDate iTo = iDialog.getLocalTo();

        final List<SSOutpayment> iFiltered = iOutpayments.stream()
                .filter(iOutpayment -> !iOutpayment.isEntered()
                        && SSOutpaymentMath.inPeriod(iOutpayment, iFrom, iTo))
                .collect(Collectors.toList());
        if (iFiltered.isEmpty()) {
            new SSInformationDialog(iMainFrame, "outpaymentjournal.dialog.norows");
            return;
        }

        final Integer iNumber = iAutoIncrement.getNumber("outpaymentjournal") + 1;

        SSVoucher iVoucher = new SSVoucher();

        iVoucher.setDescription(
                String.format(
                        SSBundle.getBundle().getString(
                                "outpaymentjournal.voucher.description"),
                                iNumber));
        iVoucher.setLocalDate(iTo);

        for (SSOutpayment iOutpayment : iFiltered) {
            SSVoucher iCurrent = iOutpayment.generateVoucher();

            for (SSVoucherRow iRow : iCurrent.getRows()) {
                iVoucher.addVoucherRow(new SSVoucherRow(iRow));
            }
        }
        final SSVoucher iVoucher1 = SSVoucherMath.compress(iVoucher);

        final ActionListener iCloseListener = e -> {

                SSQueryDialog iDialog1 = new SSQueryDialog(iMainFrame, SSBundle.getBundle(),
                        "outpaymentjournal.dialog.register", iNumber,
                        iVoucher1.getNumber());
                int iResponce1 = iDialog1.getResponce();

                if (iResponce1 != JOptionPane.YES_NO_OPTION) {
                    return;
                }

                // Mark all outpayments as entered
                for (SSOutpayment iOutpayment : iFiltered) {
                    iOutpayment.setEntered();
                    Repositories.outpayments().update(iOutpayment);
                }
                // Auto increment the invoice journal counter.
                SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

                iCurrentCompany.getAutoIncrement().doAutoIncrement("outpaymentjournal");
                se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(iCurrentCompany);
                // Add the voucher to the database.
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher1, false);

                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().getModel().fireTableDataChanged();
                }

            };

        SSProgressDialog.runProgress(iMainFrame,
                () -> {

                        SSOutpaymentjournalPrinter iPrinter1 = new SSOutpaymentjournalPrinter(
                                iFiltered, iNumber, iTo);

                        SSVoucherPrinter            iPrinter2 = new SSVoucherPrinter(iVoucher1,
                                iPrinter1.getTitle());

                        SSMultiPrinter iPrinter = new SSMultiPrinter();

                        iPrinter.addReport(iPrinter1);
                        iPrinter.addReport(iPrinter2);

                        iPrinter.preview(iMainFrame, iCloseListener);

                    });

    }

}
