package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import se.swedsoft.bookkeeping.calc.math.SSSupplierMath;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInitDialog;
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


/**
 * Date: 2006-feb-13
 * Time: 16:43:09
 */
public class SSSupplierImporter {

    private final File iFile;

    private final Map<String, Integer> iColumns;

    /**
     * Creates a supplier importer.
     *
     * @param iFile source Excel file
     */
    public SSSupplierImporter(File iFile) {
        this.iFile = iFile;
        iColumns = new HashMap<>();
    }

    /**
     * Imports suppliers from the configured file.
     *
     * @throws SSImportException if import content is invalid
     * @throws IOException if reading the file fails
     */
    public void Import()  throws IOException, SSImportException {
        List<SSSupplier> iSuppliers;

        try {
            Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(iFile);

            // Empty workbook, ie nothing to import
            if (iWorkbook.getNumberOfSheets() == 0) {
                throw new SSImportException(SSBundle.getBundle(),
                        "supplierframe.import.nosheets");
            }

            Sheet iSheet = iWorkbook.getSheetAt(0);

            iSuppliers = importSuppliers(new SSExcelSheet(iSheet));

            iWorkbook.close();

        } catch (IOException e) {
            throw new SSImportException(e.getLocalizedMessage());
        }
        final List<SSSupplier> iNewSuppliers = new LinkedList<>(iSuppliers);
        final boolean iResult = showImportReport(iSuppliers);

        SSInitDialog.runProgress(SSMainFrame.getInstance(), "Importerar leverant�rer",
                () -> {

                        if (iResult) {
                            Integer iOutPaymentNumber = SSSupplierMath.getOutpaymentNumber();

                            for (SSSupplier iSupplier : iNewSuppliers) {
                                if (!se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers().contains(iSupplier)) {
                                    iSupplier.setOutpaymentNumber(iOutPaymentNumber);
                                    se.swedsoft.bookkeeping.data.system.SSPurchaseContext.addSupplier(iSupplier);
                                    iOutPaymentNumber++;
                                }
                            }
                        }

                    });

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

                if (iName.equalsIgnoreCase(SSSupplierExporter.LEVERANTORSNUMMER)) {
                    this.iColumns.put(SSSupplierExporter.LEVERANTORSNUMMER, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.NAMN)) {
                    this.iColumns.put(SSSupplierExporter.NAMN, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.TELEFON1)) {
                    this.iColumns.put(SSSupplierExporter.TELEFON1, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.TELEFON2)) {
                    this.iColumns.put(SSSupplierExporter.TELEFON2, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.FAX)) {
                    this.iColumns.put(SSSupplierExporter.FAX, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.EPOST)) {
                    this.iColumns.put(SSSupplierExporter.EPOST, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.HEMSIDA)) {
                    this.iColumns.put(SSSupplierExporter.HEMSIDA, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.KONTAKTPERSON)) {
                    this.iColumns.put(SSSupplierExporter.KONTAKTPERSON, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ORGANISATIONSNUMMER)) {
                    this.iColumns.put(SSSupplierExporter.ORGANISATIONSNUMMER, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.VART_KUNDNUMMER)) {
                    this.iColumns.put(SSSupplierExporter.VART_KUNDNUMMER, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.BANKGIRO)) {
                    this.iColumns.put(SSSupplierExporter.BANKGIRO, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.PLUSGIRO)) {
                    this.iColumns.put(SSSupplierExporter.PLUSGIRO, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_NAMN)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_NAMN, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_ADRESS1)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_ADRESS1, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_ADRESS2)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_ADRESS2, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_POSTNUMMER)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_POSTNUMMER, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_POSTORT)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_POSTORT, iIndex);
                } else if (iName.equalsIgnoreCase(SSSupplierExporter.ADRESS_LAND)) {
                    this.iColumns.put(SSSupplierExporter.ADRESS_LAND, iIndex);
                } else {
                    throw new SSImportException("Ogiltigt kolumnnamn i importfilen: %s",
                            iName);
                }

            }
            iIndex++;
        }
    }

    /**
     * Imports supplier rows from the provided worksheet.
     *
     * @param pSheet source sheet
     * @return imported suppliers
     */
    private List<SSSupplier> importSuppliers(SSExcelSheet pSheet) {
        List<SSExcelRow> iRows = pSheet.getRows();

        if (iRows.size() < 2) {
            throw new SSImportException(SSBundle.getBundle(),
                    "supplierframe.import.norows");
        }

        getColumnIndexes(iRows.getFirst());

        List<SSSupplier> iSuppliers = new LinkedList<>();

        for (int row = 1; row < iRows.size(); row++) {
            SSExcelRow iRow = iRows.get(row);

            // Skip empty rows
            if (iRow.empty()) {
                continue;
            }

            List<SSExcelCell> iCells = iRow.getCells();

            SSSupplier iSupplier = new SSSupplier();

            // Get the cell
            for (int col = 0; col < iCells.size(); col++) {
                SSExcelCell iCell = iCells.get(col);

                String iValue = iCell.getString();

                if (iColumns.containsKey(SSSupplierExporter.LEVERANTORSNUMMER)
                        && iColumns.get(SSSupplierExporter.LEVERANTORSNUMMER) == col) {
                    iSupplier.setNumber(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.NAMN)
                        && iColumns.get(SSSupplierExporter.NAMN) == col) {
                    iSupplier.setName(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.TELEFON1)
                        && iColumns.get(SSSupplierExporter.TELEFON1) == col) {
                    iSupplier.setPhone1(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.TELEFON2)
                        && iColumns.get(SSSupplierExporter.TELEFON2) == col) {
                    iSupplier.setPhone2(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.FAX)
                        && iColumns.get(SSSupplierExporter.FAX) == col) {
                    iSupplier.setTelefax(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.EPOST)
                        && iColumns.get(SSSupplierExporter.EPOST) == col) {
                    iSupplier.setEMail(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.HEMSIDA)
                        && iColumns.get(SSSupplierExporter.HEMSIDA) == col) {
                    iSupplier.setHomepage(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.KONTAKTPERSON)
                        && iColumns.get(SSSupplierExporter.KONTAKTPERSON) == col) {
                    iSupplier.setYourContact(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ORGANISATIONSNUMMER)
                        && iColumns.get(SSSupplierExporter.ORGANISATIONSNUMMER) == col) {
                    iSupplier.setRegistrationNumber(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.VART_KUNDNUMMER)
                        && iColumns.get(SSSupplierExporter.VART_KUNDNUMMER) == col) {
                    iSupplier.setOurCustomerNr(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.BANKGIRO)
                        && iColumns.get(SSSupplierExporter.BANKGIRO) == col) {
                    iSupplier.setBankGiro(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.PLUSGIRO)
                        && iColumns.get(SSSupplierExporter.PLUSGIRO) == col) {
                    iSupplier.setPlusGiro(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_NAMN)
                        && iColumns.get(SSSupplierExporter.ADRESS_NAMN) == col) {
                    iSupplier.getAddress().setName(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_ADRESS1)
                        && iColumns.get(SSSupplierExporter.ADRESS_ADRESS1) == col) {
                    iSupplier.getAddress().setAddress1(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_ADRESS2)
                        && iColumns.get(SSSupplierExporter.ADRESS_ADRESS2) == col) {
                    iSupplier.getAddress().setAddress2(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_POSTNUMMER)
                        && iColumns.get(SSSupplierExporter.ADRESS_POSTNUMMER) == col) {
                    iSupplier.getAddress().setZipCode(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_POSTORT)
                        && iColumns.get(SSSupplierExporter.ADRESS_POSTORT) == col) {
                    iSupplier.getAddress().setCity(iValue);
                }
                if (iColumns.containsKey(SSSupplierExporter.ADRESS_LAND)
                        && iColumns.get(SSSupplierExporter.ADRESS_LAND) == col) {
                    iSupplier.getAddress().setCountry(iValue);
                }
            }
            if (iSupplier.getNumber() != null && !iSupplier.getNumber().isEmpty()) {
                iSuppliers.add(iSupplier);
            }
        }
        return iSuppliers;
    }

    /**
     * Shows a summary dialog before import is applied.
     *
     * @param iSuppliers suppliers queued for import
     * @return {@code true} when user confirms import
     */
    private boolean showImportReport(List<SSSupplier> iSuppliers) {
        SSImportReportDialog iDialog = new SSImportReportDialog(SSMainFrame.getInstance(),
                SSBundle.getBundle().getString("supplierframe.import.report"));
        // Generate the import dialog
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        sb.append("F�ljande kolumner har importerats fr�n leverant�rsfilen:<br>");
        sb.append("<ul>");
        if (iColumns.containsKey(SSSupplierExporter.LEVERANTORSNUMMER)) {
            sb.append("<li>").append(SSSupplierExporter.LEVERANTORSNUMMER).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.NAMN)) {
            sb.append("<li>").append(SSSupplierExporter.NAMN).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.TELEFON1)) {
            sb.append("<li>").append(SSSupplierExporter.TELEFON1).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.TELEFON2)) {
            sb.append("<li>").append(SSSupplierExporter.TELEFON2).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.FAX)) {
            sb.append("<li>").append(SSSupplierExporter.FAX).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.EPOST)) {
            sb.append("<li>").append(SSSupplierExporter.EPOST).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.HEMSIDA)) {
            sb.append("<li>").append(SSSupplierExporter.HEMSIDA).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.KONTAKTPERSON)) {
            sb.append("<li>").append(SSSupplierExporter.KONTAKTPERSON).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ORGANISATIONSNUMMER)) {
            sb.append("<li>").append(SSSupplierExporter.ORGANISATIONSNUMMER).append(
                    "</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.VART_KUNDNUMMER)) {
            sb.append("<li>").append(SSSupplierExporter.VART_KUNDNUMMER).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.BANKGIRO)) {
            sb.append("<li>").append(SSSupplierExporter.BANKGIRO).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.PLUSGIRO)) {
            sb.append("<li>").append(SSSupplierExporter.PLUSGIRO).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_NAMN)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_NAMN).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_ADRESS1)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_ADRESS1).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_ADRESS2)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_ADRESS2).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_POSTNUMMER)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_POSTNUMMER).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_POSTORT)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_POSTORT).append("</li>");
        }
        if (iColumns.containsKey(SSSupplierExporter.ADRESS_LAND)) {
            sb.append("<li>").append(SSSupplierExporter.ADRESS_LAND).append("</li>");
        }

        sb.append("</ul>");
        sb.append("F�ljande leverant�rer kommer att importeras:<br>");

        sb.append("<ul>");
        for (SSSupplier iSupplier : iSuppliers) {
            sb.append("<li>");
            sb.append(iSupplier);
            sb.append("</li>");
        }
        sb.append("</ul>");

        sb.append("Forts�tt med importeringen ?");
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
        return "se.swedsoft.bookkeeping.importexport.excel.SSSupplierImporter"
                + "{iColumns=" + iColumns
                + ", iFile=" + iFile
                + '}';
    }
}
