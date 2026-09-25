package se.swedsoft.bookkeeping.importexport.sie.fields;


import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.gui.util.SSBundleString;
import se.swedsoft.bookkeeping.importexport.sie.SSSIEExporter;
import se.swedsoft.bookkeeping.importexport.sie.SSSIEImporter;
import se.swedsoft.bookkeeping.importexport.sie.util.SIELabel;
import se.swedsoft.bookkeeping.importexport.sie.util.SIEReader;
import se.swedsoft.bookkeeping.importexport.sie.util.SIEWriter;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static se.swedsoft.bookkeeping.importexport.sie.util.SIEReader.SIEDataType.STRING;


/**
 * Date: 2006-feb-24
 * Time: 09:04:07
 */
public class SIEEntryVerifikation implements SIEEntry {

    private static final String DEFAULT_VOUCHER_SERIES = "A";

    public static String normalizeVoucherSeries(String pSerie) {
        if (pSerie == null || pSerie.trim().isEmpty()) {
            return DEFAULT_VOUCHER_SERIES;
        }
        String iSeries = pSerie.trim().toUpperCase(Locale.ROOT);
        if (iSeries.length() != 1 || iSeries.charAt(0) < 'A' || iSeries.charAt(0) > 'Z') {
            throw new SSImportException("Ogiltig verifikationsserie i SIE-filen: %s", pSerie);
        }
        return iSeries;
    }

    /**
     * Imports the entry
     *
     * @param iImporter
     * @param iReader
     * @return If anything was imported
     * @throws SSImportException
     *
     */
    @Override
    public boolean importEntry(SSSIEImporter iImporter, SIEReader iReader, SSNewAccountingYear iYearData) throws SSImportException {
        // #VER serie vernr [verdatum] [vertext] [regdatum]
        if (!iReader.hasFields(STRING, STRING, STRING)) {
            throw new SSImportException(
                    SSBundleString.getString("sieimport.fielderror", iReader.peekLine()));
        }

        SSVoucher iVoucher = new SSVoucher();

        String     iSerie = iReader.nextString();
        Integer    iNumber = iReader.nextInteger().orElse(null);
        java.util.Date iDate = iReader.hasNextDate() ? iReader.nextDate() : SSDateUtil.toDate(SSDateUtil.today());
        String     iDescription = iReader.hasNextString() ? iReader.nextString() : null;

        if (iNumber == null) {
            throw new SSImportException(
                    SSBundleString.getString("sieimport.fielderror", iReader.peekLine()));
        }

        iVoucher.setSeries(iImporter.resolveVoucherSeriesForVoucherImport(iSerie));
        if (iImporter.shouldKeepImportedVoucherNumber()) {
            iVoucher.setNumber(iNumber);
        }
        iVoucher.setLocalDate(SSDateUtil.toLocalDate(iDate));
        iVoucher.setDescription(iDescription);

        while (iReader.hasNextLine()) {
            iReader.nextLine();

            String iLabel = iReader.next();

            SIEEntry iEntry = iImporter.getFactory().get(iLabel);

            if (iEntry == null || !(iEntry instanceof SIEEntryTransaktion)) {
                throw new SSImportException("Row of voucher is not #TRANS");
            }

            ((SIEEntryTransaktion) iEntry).importEntry(iVoucher, iImporter, iReader,
                    iYearData);
        }
        se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(
                iVoucher,
                iImporter.shouldKeepImportedVoucherNumber());
        return true;
    }

    /**
     * Exports the entry
     *
     * @param iExporter
     * @param iWriter
     * @return If anything was exported
     * @throws SSExportException
     *
     */
    @Override
    public boolean exportEntry(SSSIEExporter iExporter, SIEWriter iWriter, SSNewAccountingYear iCurrentYearData) throws SSExportException {
        List<SSVoucher> iVouchers = se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers();

        SIEEntryTransaktion iEntry = new SIEEntryTransaktion();

        // #VER serie vernr [verdatum] [vertext] [regdatum]
        for (SSVoucher iVoucher: iVouchers) {
            iWriter.append(SIELabel.SIE_VER);
            iWriter.append(getExportVoucherSeries(iVoucher));
            iWriter.append(iVoucher.getNumber());
            LocalDate iVoucherDate = iVoucher.getLocalDate();
            iWriter.append(iVoucherDate);
            iWriter.append(iVoucher.getDescription());
            iWriter.newLine();

            iWriter.newLine("{");
            iEntry.exportEntry(iVoucher, iExporter, iWriter);
            iWriter.newLine("}");
        }

        return !iVouchers.isEmpty();
    }

    static String getExportVoucherSeries(SSVoucher pVoucher) {
        if (pVoucher == null) {
            return DEFAULT_VOUCHER_SERIES;
        }
        String iSeries = pVoucher.getSeries();
        if (iSeries == null || iSeries.trim().isEmpty()) {
            return DEFAULT_VOUCHER_SERIES;
        }
        return iSeries.trim().toUpperCase(Locale.ROOT);
    }
}
