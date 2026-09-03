package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 2006-feb-14
 * Time: 11:57:23
 */
public class SSWritableExcelCell {
    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelCell.class);

    private int iRow;

    private int iColumn;

    private Row iRow_POI;

    /**
     *
     * @param pRow
     * @param pRow2
     * @param pColumn
     */
    public SSWritableExcelCell(Row pRow, int pRow2, int pColumn) {
        iRow_POI = pRow;
        iRow = pRow2;
        iColumn = pColumn;
    }

    /**
     *
     * @param pValue
     */
    public void setString(String pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            iCell.setCellValue(pValue == null ? "" : pValue);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param pValue
     */
    public void setInteger(Integer pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue != null) {
                iCell.setCellValue(pValue.doubleValue());
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param pValue
     */
    public void setDouble(Double pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue != null) {
                iCell.setCellValue(pValue);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @return The column
     */
    public int getColumn() {
        return iColumn;
    }

    /**
     *
     * @return The row
     */
    public int getRow() {
        return iRow;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelCell");
        sb.append("{iColumn=").append(iColumn);
        sb.append(", iRow=").append(iRow);
        sb.append(", iRow_POI=").append(iRow_POI);
        sb.append('}');
        return sb.toString();
    }
}
