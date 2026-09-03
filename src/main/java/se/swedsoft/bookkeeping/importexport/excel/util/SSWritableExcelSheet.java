package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 2006-feb-14
 * Time: 11:30:33
 */
public class SSWritableExcelSheet {
    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelSheet.class);

    private Sheet iSheet;

    /**
     *
     * @param pSheet
     */
    public SSWritableExcelSheet(Sheet pSheet) {
        iSheet = pSheet;
    }

    /**
     *
     * @param pCount
     * @return
     */
    public List<SSWritableExcelRow> getRows(int pCount) {
        List<SSWritableExcelRow> iList = new LinkedList<>();

        for (int iRow = 0; iRow < pCount; iRow++) {
            Row iRow_POI = iSheet.createRow(iRow);
            iList.add(new SSWritableExcelRow(iRow_POI, iRow));
        }
        return iList;
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     */
    public void setString(int iRow, int iColumn, String pValue) {
        try {
            Row iRow_POI = iSheet.getRow(iRow);
            if (iRow_POI == null) {
                iRow_POI = iSheet.createRow(iRow);
            }
            iRow_POI.createCell(iColumn).setCellValue(pValue);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     */
    public void setInteger(int iRow, int iColumn, Integer pValue) {
        try {
            Row iRow_POI = iSheet.getRow(iRow);
            if (iRow_POI == null) {
                iRow_POI = iSheet.createRow(iRow);
            }
            if (pValue != null) {
                iRow_POI.createCell(iColumn).setCellValue(pValue.doubleValue());
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     */
    public void setDouble(int iRow, int iColumn, Double pValue) {
        try {
            Row iRow_POI = iSheet.getRow(iRow);
            if (iRow_POI == null) {
                iRow_POI = iSheet.createRow(iRow);
            }
            if (pValue != null) {
                iRow_POI.createCell(iColumn).setCellValue(pValue);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @return
     */
    public Sheet getSheet() {
        return iSheet;
    }

    /**
     *
     * @param iSheet
     */
    public void setSheet(Sheet iSheet) {
        this.iSheet = iSheet;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelSheet");
        sb.append("{iSheet=").append(iSheet);
        sb.append('}');
        return sb.toString();
    }
}
