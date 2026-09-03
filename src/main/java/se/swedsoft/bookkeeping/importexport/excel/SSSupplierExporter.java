package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import se.swedsoft.bookkeeping.data.SSSupplier;
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
 * $Id$
 */
public class SSSupplierExporter {
    // Column names
    public static final String LEVERANTORSNUMMER = "Leverantörs-id";
    public static final String NAMN = "Namn";
    public static final String TELEFON1 = "Telefon1";
    public static final String TELEFON2 = "Telefon2";
    public static final String FAX = "Fax";
    public static final String EPOST = "Epost";
    public static final String HEMSIDA = "Hemsida";
    public static final String KONTAKTPERSON = "Kontaktperson";
    public static final String ORGANISATIONSNUMMER = "Organisationsnummer";
    public static final String VART_KUNDNUMMER = "Vårt kundnummer";
    public static final String BANKGIRO = "Bankgiro";
    public static final String PLUSGIRO = "Plusgiro";
    public static final String ADRESS_NAMN = "Adress.Namn";
    public static final String ADRESS_ADRESS1 = "Adress.Adress1";
    public static final String ADRESS_ADRESS2 = "Adress.Adress2";
    public static final String ADRESS_POSTNUMMER = "Adress.Postnummer";
    public static final String ADRESS_POSTORT = "Adress.Postort";
    public static final String ADRESS_LAND = "Adress.Land";

    private final File iFile;
    private final List<SSSupplier> iSuppliers;

    /**
     * Creates an exporter that uses all suppliers from the database.
     *
     * @param iFile destination Excel file
     */
    public SSSupplierExporter(File iFile) {
        this.iFile = iFile;
        iSuppliers = se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers();
    }

    /**
     * Creates an exporter with an explicit supplier list.
     *
     * @param iFile destination Excel file
     * @param iSuppliers suppliers to export
     */
    public SSSupplierExporter(File iFile, List<SSSupplier> iSuppliers) {
        this.iFile = iFile;
        this.iSuppliers = iSuppliers;
    }

    /**
     * Exports suppliers to Excel format.
     *
     * @throws IOException if writing to disk fails
     * @throws SSExportException if workbook export fails
     */
    public void export()  throws IOException, SSExportException {
        try {
            Workbook iWorkbook = new XSSFWorkbook();

            Sheet iSheet = iWorkbook.createSheet("Leverantörer");

            writeSuppliers(new SSWritableExcelSheet(iSheet));

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(iFile)) {
                iWorkbook.write(fos);
            }
            iWorkbook.close();

        } catch (Exception e) {
            throw new SSExportException(e.getLocalizedMessage());
        }

    }

    /**
     * Writes all suppliers to the worksheet.
     *
     * @param pSheet writable destination sheet
     * @throws Exception if writing to workbook fails
     */
    private void writeSuppliers(SSWritableExcelSheet pSheet) throws Exception {

        List<SSWritableExcelRow> iRows = pSheet.getRows(iSuppliers.size() + 1);

        // Write the column names
        SSWritableExcelRow iColumns = iRows.getFirst();

        Workbook iWorkbook = pSheet.getSheet().getWorkbook();
        CellStyle iCellFormat = iWorkbook.createCellStyle();
        iCellFormat.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        iCellFormat.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        iColumns.setString(0, LEVERANTORSNUMMER, iCellFormat);
        iColumns.setString(1, NAMN, iCellFormat);
        iColumns.setString(2, TELEFON1, iCellFormat);
        iColumns.setString(3, TELEFON2, iCellFormat);
        iColumns.setString(4, FAX, iCellFormat);
        iColumns.setString(5, EPOST, iCellFormat);
        iColumns.setString(6, HEMSIDA, iCellFormat);
        iColumns.setString(7, KONTAKTPERSON, iCellFormat);
        iColumns.setString(8, ORGANISATIONSNUMMER, iCellFormat);
        iColumns.setString(9, VART_KUNDNUMMER, iCellFormat);
        iColumns.setString(10, BANKGIRO, iCellFormat);
        iColumns.setString(11, PLUSGIRO, iCellFormat);
        iColumns.setString(12, ADRESS_NAMN, iCellFormat);
        iColumns.setString(13, ADRESS_ADRESS1, iCellFormat);
        iColumns.setString(14, ADRESS_ADRESS2, iCellFormat);
        iColumns.setString(15, ADRESS_POSTNUMMER, iCellFormat);
        iColumns.setString(16, ADRESS_POSTORT, iCellFormat);
        iColumns.setString(17, ADRESS_LAND, iCellFormat);

        int iRowIndex = 1;

        for (SSSupplier iSupplier : iSuppliers) {
            SSWritableExcelRow iRow = iRows.get(iRowIndex);

            iRow.setString(0, iSupplier.getNumber());
            iRow.setString(1, iSupplier.getName());
            iRow.setString(2, iSupplier.getPhone1());
            iRow.setString(3, iSupplier.getPhone2());
            iRow.setString(4, iSupplier.getTelefax());
            iRow.setString(5, iSupplier.getEMail());
            iRow.setString(6, iSupplier.getHomepage());
            iRow.setString(7, iSupplier.getYourContact());
            iRow.setString(8, iSupplier.getRegistrationNumber());
            iRow.setString(9, iSupplier.getOurCustomerNr());
            iRow.setString(10, iSupplier.getBankgiro());
            iRow.setString(11, iSupplier.getPlusgiro());

            iRow.setString(12, iSupplier.getAddress().getName());
            iRow.setString(13, iSupplier.getAddress().getAddress1());
            iRow.setString(14, iSupplier.getAddress().getAddress2());
            iRow.setString(15, iSupplier.getAddress().getZipCode());
            iRow.setString(16, iSupplier.getAddress().getCity());
            iRow.setString(17, iSupplier.getAddress().getCountry());

            iRowIndex++;
        }

    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSSupplierExporter"
                + "{iFile=" + iFile
                + ", iSuppliers=" + iSuppliers
                + '}';
    }
}
