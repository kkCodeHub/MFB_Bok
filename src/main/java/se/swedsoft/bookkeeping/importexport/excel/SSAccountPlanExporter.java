package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelCell;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelSheet;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 * Date: 2006-feb-13
 * Time: 16:43:01
 */
public class SSAccountPlanExporter {

    private static final ResourceBundle cBundle = SSBundle.getBundle();

    private static final String C_NAME = cBundle.getString("importaccountplan.field_name");
    private static final String C_TYPE = cBundle.getString("importaccountplan.field_type");
    private static final String C_YEAR = cBundle.getString("importaccountplan.field_year");
    private static final String C_START = cBundle.getString("importaccountplan.field_start");

    private final File iFile;

    public SSAccountPlanExporter(File iFile) {
        this.iFile = iFile;
    }

    /**
     * Exports an account plan to Excel.
     *
     * @param pAccountPlan account plan to export
     * @throws IOException if writing to disk fails
     */
    public void doExport(SSAccountPlan pAccountPlan) throws IOException {
        try (Workbook iWorkbook = new XSSFWorkbook();
             FileOutputStream iOutput = new FileOutputStream(iFile)) {
            Sheet iSheet = iWorkbook.createSheet(pAccountPlan.getName());

            writeAccountPlan(new SSWritableExcelSheet(iSheet), pAccountPlan);

            iWorkbook.write(iOutput);
        } catch (IOException e) {
            throw new SSExportException(e.getLocalizedMessage());
        }
    }

    /**
     * Writes account plan metadata and rows to the worksheet.
     *
     * @param pSheet writable destination sheet
     * @param pAccountPlan source account plan
     */
    private void writeAccountPlan(SSWritableExcelSheet pSheet, SSAccountPlan pAccountPlan) {
        int iRowStart = 6;

        Iterator<SSAccount> iAccounts = pAccountPlan.getAccounts().iterator();

        for (SSWritableExcelRow iRow : pSheet.getRows(
                pAccountPlan.getAccounts().size() + 6)) {

            // Name
            if (iRow.getRow() == 0) {
                iRow.setString(0, C_NAME);
                iRow.setString(1, pAccountPlan.getName());
                continue;
            }
            // Type
            if (iRow.getRow() == 1) {
                iRow.setString(0, C_TYPE);
                iRow.setString(1, pAccountPlan.getType().toString());
                continue;
            }
            // Assessment year
            if (iRow.getRow() == 2) {
                iRow.setString(0, C_YEAR);
                iRow.setString(1, pAccountPlan.getAssessementYear());
                continue;
            }
            // Account offset
            if (iRow.getRow() == 3) {
                iRow.setString(0, C_START);
                iRow.setNumber(1, iRowStart);
                continue;
            }

            if (iRow.getRow() < iRowStart - 1) {
                continue;
            }

            if (!iAccounts.hasNext()) {
                continue;
            }

            SSAccount iAccount = iAccounts.next();

            for (SSWritableExcelCell iCell : iRow.getCells(5)) {

                switch (iCell.getColumn()) {
                case 0:
                    iCell.setInteger(iAccount.getNumber());
                    break;

                case 1:
                    iCell.setString(iAccount.getDescription());
                    break;

                case 2:
                    iCell.setString(iAccount.getVATCode());
                    break;

                case 3:
                    iCell.setString(iAccount.getSRUCode());
                    break;

                case 4:
                    iCell.setString(iAccount.getReportCode());
                    break;

                default:
                    break;
                }

            }
        }

    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanExporter"
                + "{iFile=" + iFile
                + '}';
    }
}
