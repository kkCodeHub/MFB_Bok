package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.importexport.dialog.SSImportReportDialog;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelCell;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelRow;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelSheet;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelWorkbookReader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static se.swedsoft.bookkeeping.data.SSVoucherTemplate.SSVoucherTemplateRow;


/**
 * Date: 2006-feb-13
 * Time: 16:43:09
 */
public class SSVoucherTemplateImporter {

    private final File iFile;

    private final Map<String, Integer> iColumns;

    /**
     * Creates a voucher template importer.
     *
     * @param iFile source Excel file
     */
    public SSVoucherTemplateImporter(File iFile) {
        this.iFile = iFile;
        iColumns = new HashMap<>();
    }

    /**
     * Imports voucher templates from the configured file.
     *
     * @throws SSImportException if import content is invalid
     * @throws IOException if reading the file fails
     */
    public void Import()  throws IOException, SSImportException {
        List<SSVoucherTemplate> iVoucherTemplates;

        try {
            Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(iFile);

            // Empty workbook, ie nothing to import
            if (iWorkbook.getNumberOfSheets() == 0) {
                throw new SSImportException(SSBundle.getBundle(),
                        "vouchertemplateframe.import.nosheets");
            }

            Sheet iSheet = iWorkbook.getSheetAt(0);

            iVoucherTemplates = importVouchers(new SSExcelSheet(iSheet));

            iWorkbook.close();

        } catch (IOException e) {
            throw new SSImportException(e.getLocalizedMessage());
        }
        boolean iResult = showImportReport(iVoucherTemplates);

        if (iResult) {
            for (SSVoucherTemplate iVoucherTemplate : iVoucherTemplates) {
                if (!se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVoucherTemplates().contains(iVoucherTemplate)) {
                    se.swedsoft.bookkeeping.data.system.SSAccountingContext.addVoucherTemplate(iVoucherTemplate);
                }

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
                if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.BESKRIVNING)) {
                    this.iColumns.put(SSVoucherTemplateExporter.BESKRIVNING, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.KONTO)) {
                    this.iColumns.put(SSVoucherTemplateExporter.KONTO, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.DEBET)) {
                    this.iColumns.put(SSVoucherTemplateExporter.DEBET, iIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.KREDIT)) {
                    this.iColumns.put(SSVoucherTemplateExporter.KREDIT, iIndex);
                } else {
                    throw new SSImportException("Ogiltigt kolumnnamn i importfilen: %s",
                            iName);
                }

            }
            iIndex++;
        }
    }

    /**
     * Imports voucher templates and rows from the given worksheet.
     *
     * @param pSheet source sheet
     * @return imported voucher templates
     */
    private List<SSVoucherTemplate> importVouchers(SSExcelSheet pSheet) {

        List<SSExcelRow> iRows = pSheet.getRows();

        if (iRows.size() < 2) {
            throw new SSImportException(SSBundle.getBundle(),
                    "vouchertemplateframe.import.norows");
        }

        getColumnIndexes(iRows.getFirst());

        List<SSVoucherTemplate> iVoucherTemplates = new LinkedList<>();

        SSVoucherTemplate    iVoucherTemplate = null;
        SSVoucherTemplateRow iVoucherTemplateRow = null;

        for (int row = 1; row < iRows.size(); row++) {
            SSExcelRow iRow = iRows.get(row);

            // Skip empty rows
            if (iRow.empty()) {
                continue;
            }

            List<SSExcelCell> iCells = iRow.getCells();

            // Get the cell
            for (int col = 0; col < iCells.size(); col++) {
                SSExcelCell iCell = iCells.get(col);

                String iValue = iCell.getString();

                if (iValue == null || iValue.trim().isEmpty()) {
                    continue;
                }

                if (iColumns.containsKey(SSVoucherTemplateExporter.BESKRIVNING)
                        && iColumns.get(SSVoucherTemplateExporter.BESKRIVNING) == col) {
                    iVoucherTemplate = new SSVoucherTemplate();
                    iVoucherTemplate.setDescription(iCell.getString());

                    iVoucherTemplates.add(iVoucherTemplate);
                }

                if (iVoucherTemplate == null) {
                    continue;
                }

                if (iColumns.containsKey(SSVoucherTemplateExporter.KONTO)
                        && iColumns.get(SSVoucherTemplateExporter.KONTO) == col) {
                    iVoucherTemplateRow = new SSVoucherTemplateRow();
                    iVoucherTemplateRow.setAccountNr(iCell.getInteger());

                    iVoucherTemplate.getRows().add(iVoucherTemplateRow);
                }

                if (iVoucherTemplateRow == null) {
                    continue;
                }

                if (iColumns.containsKey(SSVoucherTemplateExporter.DEBET)
                        && iColumns.get(SSVoucherTemplateExporter.DEBET) == col) {
                    iVoucherTemplateRow.setDebet(iCell.getBigDecimal().orElse(null));
                }
                if (iColumns.containsKey(SSVoucherTemplateExporter.KREDIT)
                        && iColumns.get(SSVoucherTemplateExporter.KREDIT) == col) {
                    iVoucherTemplateRow.setCredit(iCell.getBigDecimal().orElse(null));
                }
            }

        }
        return iVoucherTemplates;
    }

    /**
     * Shows a summary dialog before import is applied.
     *
     * @param iVoucherTemplates templates queued for import
     * @return {@code true} when user confirms import
     */
    private boolean showImportReport(List<SSVoucherTemplate> iVoucherTemplates) {
        SSImportReportDialog iDialog = new SSImportReportDialog(SSMainFrame.getInstance(),
                SSBundle.getBundle().getString("vouchertemplateframe.import.report"));
        // Generate the import dialog
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");

        sb.append("Följande konteringsmallar kommer att importeras:<br>");

        sb.append("<ul>");
        for (SSVoucherTemplate iVoucherTemplate : iVoucherTemplates) {
            sb.append("<li>");
            sb.append(iVoucherTemplate);
            sb.append("</li>");
        }
        sb.append("</ul>");

        sb.append("Fortsätt med importeringen ?");
        sb.append("</html>");

        iDialog.setText(sb.toString());
        iDialog.setSize(640, 480);
        SSMainFrame iMainFrame = SSMainFrame.getInstance();
        if (iMainFrame != null) {
            iDialog.setLocationRelativeTo(iMainFrame);
        }

        return iDialog.showDialog() == JOptionPane.OK_OPTION;
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSVoucherTemplateImporter"
                + "{iColumns=" + iColumns
                + ", iFile=" + iFile
                + '}';
    }
}
