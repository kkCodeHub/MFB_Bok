package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
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
        List<SSVoucherTemplate> iExistingTemplates = SSAccountingContext.getVoucherTemplates();
        List<SSVoucherTemplate> iDuplicateTemplates = getExistingDuplicates(iVoucherTemplates, iExistingTemplates);
        List<SSVoucherTemplate> iTemplatesToImport = filterOutDuplicates(iVoucherTemplates, iDuplicateTemplates);

        boolean iResult = showImportReport(iTemplatesToImport, iDuplicateTemplates);

        if (iResult) {
            for (SSVoucherTemplate iVoucherTemplate : iTemplatesToImport) {
                SSAccountingContext.addVoucherTemplate(iVoucherTemplate);
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

        for (SSExcelCell iColumn : iColumns.getCells()) {
            String iName = iColumn.getString();
            int iColumnIndex = iColumn.getColumn();

            if (iName != null && !iName.isEmpty()) {
                if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.BESKRIVNING)) {
                    this.iColumns.put(SSVoucherTemplateExporter.BESKRIVNING, iColumnIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.KONTO)) {
                    this.iColumns.put(SSVoucherTemplateExporter.KONTO, iColumnIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.DEBET)) {
                    this.iColumns.put(SSVoucherTemplateExporter.DEBET, iColumnIndex);
                } else if (iName.equalsIgnoreCase(SSVoucherTemplateExporter.KREDIT)) {
                    this.iColumns.put(SSVoucherTemplateExporter.KREDIT, iColumnIndex);
                } else {
                    throw new SSImportException("Ogiltigt kolumnnamn i importfilen: %s",
                            iName);
                }

            }
        }
    }

    /**
     * Imports voucher templates and rows from the given worksheet.
     *
     * @param pSheet source sheet
     * @return imported voucher templates
     */
    List<SSVoucherTemplate> importVouchers(SSExcelSheet pSheet) {

        List<SSExcelRow> iRows = pSheet.getRows();

        if (iRows.size() < 2) {
            throw new SSImportException(SSBundle.getBundle(),
                    "vouchertemplateframe.import.norows");
        }

        getColumnIndexes(iRows.getFirst());

        List<SSVoucherTemplate> iVoucherTemplates = new LinkedList<>();

        SSVoucherTemplate iVoucherTemplate = null;

        for (int row = 1; row < iRows.size(); row++) {
            SSExcelRow iRow = iRows.get(row);

            // Skip empty rows
            if (iRow.empty()) {
                continue;
            }

            String iDescription = getTrimmedValue(iRow, SSVoucherTemplateExporter.BESKRIVNING);
            if (!iDescription.isEmpty()) {
                iVoucherTemplate = new SSVoucherTemplate();
                iVoucherTemplate.setDescription(iDescription);
                iVoucherTemplates.add(iVoucherTemplate);
            }

            if (iVoucherTemplate == null) {
                continue;
            }

            Integer iAccountNumber = getOptionalInteger(iRow, SSVoucherTemplateExporter.KONTO);
            java.util.Optional<java.math.BigDecimal> iDebet = getOptionalBigDecimal(iRow, SSVoucherTemplateExporter.DEBET);
            java.util.Optional<java.math.BigDecimal> iCredit = getOptionalBigDecimal(iRow, SSVoucherTemplateExporter.KREDIT);

            if (iAccountNumber == null && iDebet.isEmpty() && iCredit.isEmpty()) {
                continue;
            }

            if (iAccountNumber == null) {
                throw new SSImportException("Ogiltig konteringsrad utan kontonummer pa rad %s", row + 1);
            }

            SSVoucherTemplateRow iVoucherTemplateRow = new SSVoucherTemplateRow();
            iVoucherTemplateRow.setAccountNr(iAccountNumber);
            iVoucherTemplateRow.setDebet(iDebet.orElse(null));
            iVoucherTemplateRow.setCredit(iCredit.orElse(null));
            iVoucherTemplate.getRows().add(iVoucherTemplateRow);
        }
        return iVoucherTemplates;
    }

    private String getTrimmedValue(SSExcelRow iRow, String pColumnName) {
        Integer iColumn = iColumns.get(pColumnName);
        if (iColumn == null) {
            return "";
        }
        String iValue = iRow.getString(iColumn);
        return iValue == null ? "" : iValue.trim();
    }

    private Integer getOptionalInteger(SSExcelRow iRow, String pColumnName) {
        Integer iColumn = iColumns.get(pColumnName);
        if (iColumn == null) {
            return null;
        }
        String iValue = iRow.getString(iColumn);
        if (iValue == null || iValue.trim().isEmpty()) {
            return null;
        }
        return iRow.getInteger(iColumn);
    }

    private java.util.Optional<java.math.BigDecimal> getOptionalBigDecimal(SSExcelRow iRow, String pColumnName) {
        Integer iColumn = iColumns.get(pColumnName);
        if (iColumn == null) {
            return java.util.Optional.empty();
        }
        String iValue = iRow.getString(iColumn);
        if (iValue == null || iValue.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        return iRow.getBigDecimal(iColumn);
    }

    List<SSVoucherTemplate> getExistingDuplicates(List<SSVoucherTemplate> pImported,
                                                  List<SSVoucherTemplate> pExisting) {
        List<SSVoucherTemplate> iDuplicates = new LinkedList<>();
        for (SSVoucherTemplate iTemplate : pImported) {
            if (pExisting.contains(iTemplate)) {
                iDuplicates.add(iTemplate);
            }
        }
        return iDuplicates;
    }

    private List<SSVoucherTemplate> filterOutDuplicates(List<SSVoucherTemplate> pImported,
                                                        List<SSVoucherTemplate> pDuplicates) {
        List<SSVoucherTemplate> iResult = new LinkedList<>();
        for (SSVoucherTemplate iTemplate : pImported) {
            if (!pDuplicates.contains(iTemplate)) {
                iResult.add(iTemplate);
            }
        }
        return iResult;
    }

    /**
     * Shows a summary dialog before import is applied.
     *
     * @param iVoucherTemplates templates queued for import
     * @return {@code true} when user confirms import
     */
    private boolean showImportReport(List<SSVoucherTemplate> iVoucherTemplates,
                                     List<SSVoucherTemplate> iDuplicateTemplates) {
        SSImportReportDialog iDialog = new SSImportReportDialog(SSMainFrame.getInstance(),
                SSBundle.getBundle().getString("vouchertemplateframe.import.report"));
        iDialog.setText(buildImportReportText(iVoucherTemplates, iDuplicateTemplates));
        iDialog.setSize(640, 480);
        SSMainFrame iMainFrame = SSMainFrame.getInstance();
        if (iMainFrame != null) {
            iDialog.setLocationRelativeTo(iMainFrame);
        }

        return iDialog.showDialog() == JOptionPane.OK_OPTION;
    }

    String buildImportReportText(List<SSVoucherTemplate> iVoucherTemplates,
                                 List<SSVoucherTemplate> iDuplicateTemplates) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        sb.append("Följande konteringsmallar kommer att importeras:<br>");
        sb.append("<ul>");
        for (SSVoucherTemplate iVoucherTemplate : iVoucherTemplates) {
            sb.append("<li>");
            sb.append(iVoucherTemplate);
            sb.append("</li>");
        }
        if (iVoucherTemplates.isEmpty()) {
            sb.append("<li>Inga</li>");
        }
        sb.append("</ul>");

        sb.append("Hoppade dubbletter (rubriken finns redan):<br>");
        if (iDuplicateTemplates.isEmpty()) {
            sb.append("Inga");
        } else {
            sb.append("<ul>");
            for (SSVoucherTemplate iDuplicateTemplate : iDuplicateTemplates) {
                sb.append("<li>");
                sb.append(iDuplicateTemplate);
                sb.append("</li>");
            }
            sb.append("</ul>");
        }

        sb.append("<br>Fortsätt med importeringen ?");
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSVoucherTemplateImporter"
                + "{iColumns=" + iColumns
                + ", iFile=" + iFile
                + '}';
    }
}
