package se.swedsoft.bookkeeping.importexport.excel;

import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

/**
 * Imports account plans from Excel.
 */
public class SSAccountPlanImporter {

    private static final ResourceBundle BUNDLE = SSBundle.getBundle();

    private final File iFile;

    /**
     * Creates an account plan importer.
     *
     * @param iFile source Excel file
     */
    public SSAccountPlanImporter(File iFile) {
        this.iFile = iFile;
    }

    /**
     * Imports an account plan from the configured file.
     *
     * @throws IOException if reading the file fails
     * @throws SSImportException if import content is invalid
     */
    public void doImport() throws IOException, SSImportException {
        doImport(iFile);
    }

    /**
     * Imports an account plan from a file.
     *
     * @param file source Excel file
     * @throws IOException if reading the file fails
     * @throws SSImportException if import content is invalid
     */
    public static void doImport(File file) throws IOException, SSImportException {
         if (file == null) {
             throw new NullPointerException("file");
         }
         validateImportLocation(file);
         SSAccountPlan accountPlan = SSAccountPlanLoader.readPlan(file);
         validateFileNameAgainstPlanName(file.getName(), accountPlan);
         storeImportedPlan(accountPlan, file.getName());
     }

    /**
     * Imports an account plan from a stream.
     *
     * @param inputStream source Excel stream
     * @throws IOException if reading the stream fails
     * @throws SSImportException if import content is invalid
     */
    public static void doImport(InputStream inputStream) throws IOException, SSImportException {
        SSAccountPlan accountPlan = SSAccountPlanLoader.readPlan(inputStream);
        storeImportedPlan(accountPlan, null);
    }

    /**
     * Loads an account plan from its Excel template without storing it.
     *
     * @param templatePlan template account plan
     * @return the loaded plan or null
     * @throws IOException if reading the file fails
     */
    public static SSAccountPlan loadPlan(SSAccountPlan templatePlan) throws IOException {
        return SSAccountPlanLoader.loadPlan(templatePlan);
    }

    private static void storeImportedPlan(SSAccountPlan accountPlan, String excelPath) {
        if (accountPlan == null) {
            return;
        }

        for (SSAccountPlan existing : SSAccountingContext.getAccountPlans()) {
            if (accountPlan.getName() != null && accountPlan.getName().equals(existing.getName())) {
                new SSErrorDialog(SSMainFrame.getInstance(), "accountplanframe.duplicate",
                        accountPlan.getName());
                return;
            }
        }

        accountPlan.setExcelPath(excelPath);
        accountPlan.setDefaultPlan(false);
        SSAccountingContext.addAccountPlan(accountPlan);
    }

    public static void validateFileNameAgainstPlanName(String fileName, SSAccountPlan accountPlan) {
        if (fileName == null || accountPlan == null || accountPlan.getName() == null) {
            return;
        }

        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

        int startIndex = 0;
        while (startIndex < baseName.length() && Character.isDigit(baseName.charAt(startIndex))) {
            startIndex++;
        }
        baseName = baseName.substring(startIndex);

        if (!baseName.equals(accountPlan.getName())) {
            throw new SSImportException(BUNDLE, "importaccountplan.fileempty");
        }
    }

    private static void validateImportLocation(File file) throws IOException {
        if (file == null) {
            return;
        }

        File expectedDir = Path.get(Path.USER_DATA).getCanonicalFile();
        File selectedFile = file.getCanonicalFile();
        File parent = selectedFile.getParentFile();
        if (parent == null || !parent.equals(expectedDir)) {
            throw new SSImportException(BUNDLE, "importaccountplan.wrongplace", selectedFile.getPath());
        }
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter"
                + "{iFile=" + iFile
                + '}';
    }
}
