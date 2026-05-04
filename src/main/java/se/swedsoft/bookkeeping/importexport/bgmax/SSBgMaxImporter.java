package se.swedsoft.bookkeeping.importexport.bgmax;


import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.importexport.bgmax.data.BgMaxAvsnitt;
import se.swedsoft.bookkeeping.importexport.bgmax.data.BgMaxBetalning;
import se.swedsoft.bookkeeping.importexport.bgmax.data.BgMaxFile;
import se.swedsoft.bookkeeping.importexport.bgmax.data.BgMaxReferens;
import se.swedsoft.bookkeeping.importexport.bgmax.dialog.BgMaxSelectInvoiceDialog;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;

import se.swedsoft.bookkeeping.util.SSDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: Andreas Lago
 * Date: 2006-aug-22
 * Time: 15:00:47
 */
public class SSBgMaxImporter {    private static final Logger LOG = LoggerFactory.getLogger(SSBgMaxImporter.class);

    private SSBgMaxImporter() {}

    /**
     *
     * @param iFile
     * @throws SSImportException
     * @return
     */
    public static BgMaxFile Import(File iFile) throws SSImportException {

        List<String> iLines = new LinkedList<>();

        try {
            BufferedReader iReader = new BufferedReader(new FileReader(iFile));

            String iLine;

            while ((iLine = iReader.readLine()) != null) {
                iLines.add(iLine);
            }
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }

        BgMaxFile iBgMaxFile = new BgMaxFile();

        iBgMaxFile.parse(iLines);

        LOG.info("{}", iBgMaxFile);

        return iBgMaxFile;
    }

    /**
     *
     * @param iMainFrame
     * @param iBgMaxFile
     * @return
     */
    public static List<SSInpayment> getInpayments(SSMainFrame iMainFrame, BgMaxFile iBgMaxFile) {
        BgMaxSelectInvoiceDialog iDialog = new BgMaxSelectInvoiceDialog(iMainFrame);

        iDialog.setLocationRelativeTo(iMainFrame);

        DateTimeFormatter iDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<SSInpayment> iInpayments = new LinkedList<>();

        for (BgMaxAvsnitt iAvsnitt : iBgMaxFile.getAvsnitts()) {
            SSInpayment iInpayment = new SSInpayment();

            iInpayment.setText("Bankgiro inbetalning " + iAvsnitt.getLopnummer());
            try {
                LocalDate parsed = LocalDate.parse(iAvsnitt.getBetalningsdag(), iDateFormat);
                iInpayment.setLocalDate(parsed);
            } catch (DateTimeParseException e) {
                LOG.error("Unexpected error", e);
            }

            for (BgMaxBetalning iBetalning : iAvsnitt.getBetalningar()) {

                SSInvoice iInvoice = SSInvoiceMath.getInvoiceByReference(
                        iBetalning.getReferens()).orElse(null);

                if (iInvoice == null && !iBetalning.hasNoReferenser()) {
                    for (BgMaxReferens iReferens : iBetalning.getReferenser()) {
                        iInvoice = SSInvoiceMath.getInvoiceByReference(iReferens.getReferens()).orElse(null);
                    }
                }
                if (iInvoice == null) {
                    int iResponce = iDialog.showDialog(iBetalning);

                    if (iResponce != JOptionPane.OK_OPTION) {
                        return null;
                    }

                    iInvoice = iDialog.getInvoice();
                }
                SSInpaymentRow iInpaymentRow = new SSInpaymentRow();

                iInpaymentRow.setCurrencyRate(new BigDecimal(1));
                iInpaymentRow.setInvoice(iInvoice);
                iInpaymentRow.setValue(iBetalning.getBelopp());

                iInpayment.getRows().add(iInpaymentRow);

            }

            iInpayments.add(iInpayment);
        }
        return iInpayments;

    }

}
