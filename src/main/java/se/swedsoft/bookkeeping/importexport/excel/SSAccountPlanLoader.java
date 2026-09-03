package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelCell;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelSheet;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelWorkbookReader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

/**
 * Loads account plans from Excel templates.
 */
public final class SSAccountPlanLoader {

    private static final ResourceBundle BUNDLE = SSBundle.getBundle();

    private static final String C_NAME = BUNDLE.getString("importaccountplan.field_name");
    private static final String C_TYPE = BUNDLE.getString("importaccountplan.field_type");
    private static final String C_YEAR = BUNDLE.getString("importaccountplan.field_year");
    private static final String C_START = BUNDLE.getString("importaccountplan.field_start");

    private SSAccountPlanLoader() {
    }

    public static SSAccountPlan loadPlan(SSAccountPlan templatePlan) throws IOException {
        if (templatePlan == null) {
            return null;
        }

        String excelPath = templatePlan.getExcelPath();
        if (excelPath == null || excelPath.trim().isEmpty()) {
            return new SSAccountPlan(templatePlan);
        }

        try (Workbook workbook = openWorkbook(templatePlan)) {
            SSAccountPlan loadedPlan = readPlan(workbook);
            loadedPlan.setId(templatePlan.getId());
            loadedPlan.setExcelPath(templatePlan.getExcelPath());
            loadedPlan.setDefaultPlan(templatePlan.isDefaultPlan());
            return loadedPlan;
        }
    }

    public static SSAccountPlan readPlan(File file) throws IOException {
        try (Workbook workbook = SSExcelWorkbookReader.openWorkbook(file)) {
            return readPlan(workbook);
        }
    }

    public static SSAccountPlan readPlan(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return readPlan(workbook);
        }
    }

    private static Workbook openWorkbook(SSAccountPlan plan) throws IOException {
        if (plan.isDefaultPlan()) {
            InputStream inputStream = SSDB.class.getClassLoader().getResourceAsStream(plan.getExcelPath());
            if (inputStream == null) {
                throw new IOException("Resource not found: " + plan.getExcelPath());
            }
            return WorkbookFactory.create(inputStream);
        }

        File file = new File(Path.get(Path.USER_DATA), plan.getExcelPath());
        return SSExcelWorkbookReader.openWorkbook(file);
    }

    private static SSAccountPlan readPlan(Workbook workbook) {
        SSAccountPlan accountPlan = new SSAccountPlan();

        if (workbook.getNumberOfSheets() == 0) {
            throw new SSImportException(BUNDLE, "importaccountplan.nosheets");
        }

        readAccountPlan(new SSExcelSheet(workbook.getSheetAt(0)), accountPlan);
        return accountPlan;
    }

    private static void readAccountPlan(SSExcelSheet sheet, SSAccountPlan accountPlan) {
        int rowStart = Integer.MAX_VALUE;

        for (SSExcelRow row : sheet.getRows()) {
            if (row.empty()) {
                continue;
            }

            String label = row.getString(0);

            if (label.startsWith(C_NAME)) {
                accountPlan.setName(row.getString(1));
                continue;
            }
            if (label.startsWith(C_TYPE)) {
                accountPlan.setType(row.getString(1));
                continue;
            }
            if (label.startsWith(C_YEAR)) {
                accountPlan.setAssessementYear(row.getString(1));
                continue;
            }
            if (label.startsWith(C_START)) {
                rowStart = row.getInteger(1);
                continue;
            }

            if (row.getRow() < rowStart - 1) {
                continue;
            }

            SSAccount account = new SSAccount();
            for (SSExcelCell cell : row.getCells()) {
                switch (cell.getColumn()) {
                    case 0:
                        account.setNumber(cell.getInteger());
                        break;
                    case 1:
                        account.setDescription(cell.getString());
                        break;
                    case 2:
                        account.setVATCode(cell.getString());
                        break;
                    case 3:
                        account.setSRUCode(cell.getString());
                        break;
                    case 4:
                        account.setReportCode(cell.getString());
                        break;
                    default:
                        break;
                }
            }
            accountPlan.addAccount(account);
        }

        if (accountPlan.getAccounts().isEmpty()) {
            throw new SSImportException(BUNDLE, "importaccountplan.fileempty");
        }
    }
}
