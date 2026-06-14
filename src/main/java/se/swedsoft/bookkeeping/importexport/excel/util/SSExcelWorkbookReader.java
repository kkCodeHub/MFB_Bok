package se.swedsoft.bookkeeping.importexport.excel.util;


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


/**
 * Utility for opening Excel files based on extension.
 */
public final class SSExcelWorkbookReader {

    private SSExcelWorkbookReader() {
    }

    /**
     * Opens an Excel workbook and chooses parser by extension.
     *
     * @param pFile Excel file to open
     * @return loaded workbook
     * @throws IOException if the file cannot be read or extension is unsupported
     */
    public static Workbook openWorkbook(File pFile) throws IOException {
        String iFileName = pFile.getName();
        String iExtension = getExtension(iFileName);

        try (InputStream iInputStream = new FileInputStream(pFile)) {
            if ("xlsx".equals(iExtension)) {
                return new XSSFWorkbook(iInputStream);
            }
            if ("xls".equals(iExtension)) {
                return new HSSFWorkbook(iInputStream);
            }
        }

        throw new IOException("Unsupported Excel extension for file: " + iFileName
                + ". Supported extensions are .xlsx and .xls");
    }

    private static String getExtension(String pFileName) {
        int iDotIndex = pFileName.lastIndexOf('.');

        if (iDotIndex < 0 || iDotIndex >= pFileName.length() - 1) {
            return "";
        }

        return pFileName.substring(iDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
