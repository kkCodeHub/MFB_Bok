package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelSheet;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * User: Andreas Lago
 * Date: 2006-aug-01
 * Time: 11:32:25
 */
public class SSVoucherExporter {
    // Column names
    public static final String NUMMER = "Nummer";
    public static final String BESKRIVNING = "Beskrivning";

    public static final String DATUM = "Datum";

    public static final String KONTO = "Konto";
    public static final String DEBET = "Debet";
    public static final String KREDIT = "Kredit";
    public static final String PROJEKT = "Projekt";
    public static final String RESULTATENHET = "Resultatenhet";

    private final File iFile;
    private final List<SSVoucher> iVouchers;

    /**
     * Creates an exporter that uses all vouchers from the database.
     *
     * @param iFile destination Excel file
     */
    public SSVoucherExporter(File iFile) {
        this.iFile = iFile;
        iVouchers = se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers();
    }

    /**
     * Creates an exporter with an explicit voucher list.
     *
     * @param iFile destination Excel file
     * @param iVouchers vouchers to export
     */
    public SSVoucherExporter(File iFile, List<SSVoucher> iVouchers) {
        this.iFile = iFile;
        this.iVouchers = iVouchers;
    }

    /**
     * Exports vouchers to Excel format.
     *
     * @throws IOException if writing to disk fails
     * @throws SSExportException if workbook export fails
     */
    public void export()  throws IOException, SSExportException {
        try {
            Workbook iWorkbook = new XSSFWorkbook();

            Sheet iSheet = iWorkbook.createSheet("Verifikationer");

            writeVouchers(new SSWritableExcelSheet(iSheet));

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(iFile)) {
                iWorkbook.write(fos);
            }
            iWorkbook.close();

        } catch (Exception e) {
            throw new SSExportException(e.getLocalizedMessage());
        }

    }

    /**
     * Calculates required row count for voucher export.
     *
     * @param iVouchers vouchers to count rows for
     * @return number of output rows excluding header padding
     */
    private int getNumRows(List<SSVoucher> iVouchers) {
        int count = 0;

        for (SSVoucher iVoucher : iVouchers) {
            count = count + iVoucher.getRows().size() + 1;
        }
        return count;
    }

    /**
     * Writes vouchers and voucher rows to the worksheet.
     *
     * @param pSheet writable destination sheet
     * @throws Exception if writing to workbook fails
     */
    private void writeVouchers(SSWritableExcelSheet pSheet) throws Exception {
        List<SSWritableExcelRow> iRows = pSheet.getRows(getNumRows(iVouchers) + 4);

        Workbook iWorkbook = pSheet.getSheet().getWorkbook();
        CellStyle iCellFormat = iWorkbook.createCellStyle();
        iCellFormat.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        iCellFormat.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        SSWritableExcelRow iHeaderRow = iRows.getFirst();
        iHeaderRow.setString(0, NUMMER, iCellFormat);
        iHeaderRow.setString(1, BESKRIVNING, iCellFormat);
        iHeaderRow.setString(2, DATUM, iCellFormat);
        iHeaderRow.setString(3, KONTO, iCellFormat);
        iHeaderRow.setString(4, DEBET, iCellFormat);
        iHeaderRow.setString(5, KREDIT, iCellFormat);
        iHeaderRow.setString(6, PROJEKT, iCellFormat);
        iHeaderRow.setString(7, RESULTATENHET, iCellFormat);

        iCellFormat = iWorkbook.createCellStyle();
        Font iFont = iWorkbook.createFont();
        iFont.setFontName("Arial");
        iFont.setBold(true);
        iCellFormat.setFont(iFont);

        int iRowIndex = 1;

        for (SSVoucher iVoucher : iVouchers) {
            iRowIndex++;
            SSWritableExcelRow iRow = iRows.get(iRowIndex);

            iRow.setNumber(0, iVoucher.getNumber(), iCellFormat);
            iRow.setString(1, iVoucher.getDescription(), iCellFormat);
            iRow.setDate(2, iVoucher.getLocalDate(), iCellFormat);

            for (SSVoucherRow iVoucherRow : iVoucher.getRows()) {

                if (iVoucherRow.isCrossed()) {
                    continue;
                }

                iRowIndex++;
                iRow = iRows.get(iRowIndex);
                iRow.setNumber(3, iVoucherRow.getAccountNr());
                iRow.setNumber(4, iVoucherRow.getDebet());
                iRow.setNumber(5, iVoucherRow.getCredit());
                iRow.setString(6, iVoucherRow.getProjectNr());
                iRow.setString(7, iVoucherRow.getResultUnitNr());
            }
        }

    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSVoucherExporter"
                + "{iFile=" + iFile
                + ", iVouchers=" + iVouchers
                + '}';
    }
}
