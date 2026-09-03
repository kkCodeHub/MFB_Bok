package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.importexport.dialog.SSImportReportDialog;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelCell;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelSheet;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelWorkbookReader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.awt.GraphicsEnvironment;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Date: 2006-feb-13
 * Time: 16:43:09
 */
public class SSVoucherImporter {

    private final File iFile;

    private final Map<String, Integer> iColumns;

    /**
     * Creates a voucher importer.
     *
     * @param iFile source Excel file
     */
    public SSVoucherImporter(File iFile) {
        this.iFile = iFile;
        iColumns = new HashMap<>();
    }

    /**
     * Imports vouchers from the configured file.
     *
     * @throws SSImportException if import content is invalid
     * @throws IOException if reading the file fails
     */
    public void Import()  throws IOException, SSImportException {
        ImportSummary iSummary;

        try (Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(iFile)) {
            // Empty workbook, ie nothing to import
            if (iWorkbook.getNumberOfSheets() == 0) {
                throw new SSImportException(SSBundle.getBundle(),
                        "voucherframe.import.nosheets");
            }

            Sheet iSheet = iWorkbook.getSheetAt(0);

            iSummary = importVouchers(new SSExcelSheet(iSheet));
        } catch (IOException e) {
            throw new SSImportException(e.getLocalizedMessage());
        }
        if (showImportReport(iSummary)) {
            for (SSVoucher iVoucher : iSummary.getImportedVouchers()) {
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucher(iVoucher, true);
            }
        }

    }

    /**
     * Reads and validates column names from the header row.
     *
     * @param iColumns header row
     */
    private void getColumnIndexes(SSExcelRow iColumns) {

        this.iColumns.clear();
        int iIndex = 0;

        for (SSExcelCell iColumn : iColumns.getCells()) {
            String iName = iColumn.getString();

            if (iName != null && !iName.isEmpty()) {
                if (iName.equalsIgnoreCase(SSVoucherExporter.NUMMER)) {
                    this.iColumns.put(SSVoucherExporter.NUMMER, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.BESKRIVNING)) {
                    this.iColumns.put(SSVoucherExporter.BESKRIVNING, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.DATUM)) {
                    this.iColumns.put(SSVoucherExporter.DATUM, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.KONTO)) {
                    this.iColumns.put(SSVoucherExporter.KONTO, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.DEBET)) {
                    this.iColumns.put(SSVoucherExporter.DEBET, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.KREDIT)) {
                    this.iColumns.put(SSVoucherExporter.KREDIT, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.PROJEKT)) {
                    this.iColumns.put(SSVoucherExporter.PROJEKT, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherExporter.RESULTATENHET)) {
                    this.iColumns.put(SSVoucherExporter.RESULTATENHET, iIndex);
                } else {
                    throw new SSImportException("Ogiltigt kolumnnamn i importfilen: %s",
                            iName);
                }

            }
            iIndex++;
        }
    }

    /**
     * Imports vouchers and rows from the given worksheet.
     *
     * @param pSheet source sheet
     * @return imported vouchers
     */
    private ImportSummary importVouchers(SSExcelSheet pSheet) {

        List<SSExcelRow> iRows = pSheet.getRows();

        if (iRows.size() < 2) {
            throw new SSImportException(SSBundle.getBundle(), "voucherframe.import.norows");
        }

        getColumnIndexes(iRows.getFirst());

        ImportSummary iSummary = new ImportSummary();
        Set<Integer> iSeenNumbers = new HashSet<>();

        SSVoucher    iVoucher = null;
        boolean      iDuplicateVoucher = false;

        for (int row = 1; row < iRows.size(); row++) {
            SSExcelRow iRow = iRows.get(row);

            // Skip empty rows
            if (iRow.empty()) {
                continue;
            }

            Integer iNumber = getInteger(iRow, SSVoucherExporter.NUMMER);
            if (iNumber != null) {
                if (iVoucher != null && !iDuplicateVoucher) {
                    iSummary.addImportedVoucher(iVoucher);
                }
                iVoucher = createVoucher(iNumber, iSeenNumbers, iSummary);
                iDuplicateVoucher = iVoucher == null;
            }

            if (iVoucher == null) {
                continue;
            }

            if (iDuplicateVoucher) {
                continue;
            }

            applyVoucherHeader(iRow, iVoucher);
            applyVoucherRow(iRow, iVoucher);
        }

        if (iVoucher != null && !iDuplicateVoucher) {
            iSummary.addImportedVoucher(iVoucher);
        }
        return iSummary;
    }

    private SSVoucher createVoucher(Integer iNumber, Set<Integer> iSeenNumbers, ImportSummary iSummary) {
        if (iNumber == null) {
            return null;
        }

        if (iSeenNumbers.contains(iNumber) || se.swedsoft.bookkeeping.data.system.SSAccountingContext.hasVoucher(iNumber)) {
            iSummary.addDuplicateNumber(iNumber);
            return null;
        }

        iSeenNumbers.add(iNumber);
        return new SSVoucher(iNumber);
    }

    private void applyVoucherHeader(SSExcelRow iRow, SSVoucher iVoucher) {
        Integer iDescriptionColumn = iColumns.get(SSVoucherExporter.BESKRIVNING);
        if (iDescriptionColumn != null) {
            String iDescription = iRow.getString(iDescriptionColumn);
            if (iDescription != null && !iDescription.trim().isEmpty()) {
                iVoucher.setDescription(iDescription);
            }
        }

        Integer iDateColumn = iColumns.get(SSVoucherExporter.DATUM);
        if (iDateColumn != null) {
            java.util.Date iDate = iRow.getDate(iDateColumn);
            if (iDate != null) {
                iVoucher.setLocalDate(SSDateUtil.toLocalDate(iDate));
            }
        }
    }

    private void applyVoucherRow(SSExcelRow iRow, SSVoucher iVoucher) {
        Integer iAccountColumn = iColumns.get(SSVoucherExporter.KONTO);
        if (iAccountColumn == null) {
            return;
        }

        String iAccountValue = iRow.getString(iAccountColumn);
        if (iAccountValue == null || iAccountValue.trim().isEmpty()) {
            return;
        }

        SSVoucherRow iVoucherRow = new SSVoucherRow();
        iVoucherRow.setAccountNr(iRow.getInteger(iAccountColumn));
        iVoucher.getRows().add(iVoucherRow);

        Integer iDebetColumn = iColumns.get(SSVoucherExporter.DEBET);
        if (iDebetColumn != null) {
            iVoucherRow.setDebet(iRow.getBigDecimal(iDebetColumn).orElse(null));
        }

        Integer iCreditColumn = iColumns.get(SSVoucherExporter.KREDIT);
        if (iCreditColumn != null) {
            iVoucherRow.setCredit(iRow.getBigDecimal(iCreditColumn).orElse(null));
        }

        Integer iProjectColumn = iColumns.get(SSVoucherExporter.PROJEKT);
        if (iProjectColumn != null) {
            String iProjectValue = iRow.getString(iProjectColumn);
            if (iProjectValue != null && !iProjectValue.trim().isEmpty()) {
                iVoucherRow.setProjectNr(iProjectValue);
            }
        }

        Integer iResultUnitColumn = iColumns.get(SSVoucherExporter.RESULTATENHET);
        if (iResultUnitColumn != null) {
            String iResultUnitValue = iRow.getString(iResultUnitColumn);
            if (iResultUnitValue != null && !iResultUnitValue.trim().isEmpty()) {
                iVoucherRow.setResultUnitNr(iResultUnitValue);
            }
        }
    }

    /**
     * Shows a summary dialog before import is applied.
     *
     * @param iVouchers vouchers queued for import
     * @return {@code true} when user confirms import
     */
    boolean showImportReport(ImportSummary iSummary) {
        String iReport = buildImportReportText(iSummary);

        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }

        SSImportReportDialog iDialog = new SSImportReportDialog(SSMainFrame.getInstance(),
                SSBundle.getBundle().getString("voucherframe.import.report"));
        iDialog.setText(iReport);
        iDialog.setSize(640, 480);
        SSMainFrame iMainFrame = SSMainFrame.getInstance();
        if (iMainFrame != null) {
            iDialog.setLocationRelativeTo(iMainFrame);
        }

        return iDialog.showDialog() == JOptionPane.OK_OPTION;
    }

    String buildImportReportText(ImportSummary iSummary) {
        return buildImportReportText(iSummary.getImportedVouchers(), iSummary.getDuplicateNumbers());
    }

    String buildImportReportText(List<SSVoucher> iImportedVouchers, List<Integer> iDuplicateNumbers) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        sb.append("Följande verifikationer kommer att importeras:<br>");
        sb.append("<ul>");
        for (SSVoucher iVoucher : iImportedVouchers) {
            sb.append("<li>");
            sb.append(iVoucher);
            sb.append("</li>");
        }
        if (iImportedVouchers.isEmpty()) {
            sb.append("<li>Inga</li>");
        }
        sb.append("</ul>");

        sb.append("Hoppade dubbletter:<br>");
        if (iDuplicateNumbers.isEmpty()) {
            sb.append("Inga");
        } else {
            sb.append("<ul>");
            for (Integer iDuplicateNumber : iDuplicateNumbers) {
                sb.append("<li>");
                sb.append(iDuplicateNumber);
                sb.append("</li>");
            }
            sb.append("</ul>");
        }
        sb.append("<br>Fortsätt med importeringen ?");
        sb.append("</html>");
        return sb.toString();
    }

    private Integer getInteger(SSExcelRow iRow, String iColumn) {
        Integer iIndex = iColumns.get(iColumn);
        if (iIndex == null) {
            return null;
        }
        String iValue = iRow.getString(iIndex);
        if (iValue == null || iValue.trim().isEmpty()) {
            return null;
        }
        return iRow.getInteger(iIndex);
    }

    private static final class ImportSummary {
        private final List<SSVoucher> iImportedVouchers = new LinkedList<>();
        private final List<Integer> iDuplicateNumbers = new LinkedList<>();

        void addImportedVoucher(SSVoucher iVoucher) {
            iImportedVouchers.add(iVoucher);
        }

        void addDuplicateNumber(Integer iNumber) {
            iDuplicateNumbers.add(iNumber);
        }

        List<SSVoucher> getImportedVouchers() {
            return iImportedVouchers;
        }

        List<Integer> getDuplicateNumbers() {
            return iDuplicateNumbers;
        }
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSVoucherImporter"
                + "{iColumns=" + iColumns
                + ", iFile=" + iFile
                + '}';
    }
}
