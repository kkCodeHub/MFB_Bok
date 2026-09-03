package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelSheet;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static se.swedsoft.bookkeeping.data.SSVoucherTemplate.SSVoucherTemplateRow;

/**
 * User: Andreas Lago
 * Date: 2006-aug-01
 * Time: 11:32:25
 */
public class SSVoucherTemplateExporter {
    // Column names
    public static final String BESKRIVNING = "Beskrivning";

    public static final String KONTO = "Konto";
    public static final String DEBET = "Debet";
    public static final String KREDIT = "Kredit";

    private final File iFile;
    private final List<SSVoucherTemplate> iVouchers;

    /**
     * Creates an exporter that uses all voucher templates from the database.
     *
     * @param iFile destination Excel file
     */
    public SSVoucherTemplateExporter(File iFile) {
        this.iFile = iFile;
        iVouchers = se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVoucherTemplates();
    }

    /**
     * Creates an exporter with an explicit voucher template list.
     *
     * @param iFile destination Excel file
     * @param iVouchers voucher templates to export
     */
    public SSVoucherTemplateExporter(File iFile, List<SSVoucherTemplate> iVouchers) {
        this.iFile = iFile;
        this.iVouchers = iVouchers;
    }

    /**
     * Exports voucher templates to Excel format.
     *
     * @throws IOException if writing to disk fails
     * @throws SSExportException if workbook export fails
     */
    public void export() throws IOException, SSExportException {
        try {
            Workbook iWorkbook = new XSSFWorkbook();

            Sheet iSheet = iWorkbook.createSheet("Konteringmallar");

            writeVoucherTemplates(new SSWritableExcelSheet(iSheet), iWorkbook);

            try (FileOutputStream iOutput = new FileOutputStream(iFile)) {
                iWorkbook.write(iOutput);
            }
            iWorkbook.close();

        } catch (IOException e) {
            throw new SSExportException(e.getLocalizedMessage());
        }
    }

    /**
     * Calculates required row count for template export.
     *
     * @param iVouchers voucher templates to count rows for
     * @return number of output rows excluding header padding
     */
    private int getNumRows(List<SSVoucherTemplate> iVouchers) {
        int count = 0;

        for (SSVoucherTemplate iVoucher : iVouchers) {
            count = count + iVoucher.getRows().size() + 1;
        }
        return count;
    }

    /**
     * Writes voucher templates and rows to the worksheet.
     *
     * @param pSheet writable destination sheet
     * @throws SSExportException if writing to workbook fails
     */
    private void writeVoucherTemplates(SSWritableExcelSheet pSheet, Workbook iWorkbook)
            throws SSExportException {

        List<SSWritableExcelRow> iRows = pSheet.getRows(getNumRows(iVouchers) + 4);

        CellStyle iCellFormat = iWorkbook.createCellStyle();
        iCellFormat.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        iCellFormat.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        SSWritableExcelRow iHeaderRow = iRows.getFirst();
        iHeaderRow.setString(0, BESKRIVNING, iCellFormat);
        iHeaderRow.setString(1, KONTO, iCellFormat);
        iHeaderRow.setString(2, DEBET, iCellFormat);
        iHeaderRow.setString(3, KREDIT, iCellFormat);

        CellStyle iCellFont = iWorkbook.createCellStyle();
        Font iFont = iWorkbook.createFont();
        iFont.setFontName("Arial");
        iFont.setBold(true);

        iCellFont.setFont(iFont);

        int iRowIndex = 1;

        for (SSVoucherTemplate iVoucher : iVouchers) {
            iRowIndex++;
            SSWritableExcelRow iRow = iRows.get(iRowIndex);

            iRow.setString(0, iVoucher.getDescription(), iCellFont);

            for (SSVoucherTemplateRow iVoucherRow : iVoucher.getRows()) {
                iRowIndex++;
                iRow = iRows.get(iRowIndex);
                iRow.setNumber(1, iVoucherRow.getAccountNr());
                iRow.setNumber(2, iVoucherRow.getDebet());
                iRow.setNumber(3, iVoucherRow.getCredit());
            }
        }

    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSVoucherTemplateExporter"
                + "{iFile=" + iFile
                + ", iVouchers=" + iVouchers
                + '}';
    }
}
