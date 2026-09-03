package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

/**
 * Date: 2006-feb-14
 * Time: 11:36:34
 */
public class SSExcelRow {

    private int iRow;

    private Row iRow_POI;

    public SSExcelRow(Row pRow, int pRowNum) {
        iRow_POI = pRow;
        iRow = pRowNum;
    }

    public List<SSExcelCell> getCells() {
        List<SSExcelCell> iList = new LinkedList<>();

        if (iRow_POI == null) {
            return iList;
        }

        int iColumn = 0;
        for (Cell iCell : iRow_POI) {
            iList.add(new SSExcelCell(iCell, iRow, iColumn));
            iColumn++;
        }
        return iList;
    }

    public boolean empty() {
        if (iRow_POI == null) {
            return true;
        }

        for (Cell iCell : iRow_POI) {
            String content = SSExcelCell.formatCellValue(iCell);
            if (content != null && !content.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return
     */
    public int getRow() {
        return iRow;
    }

    /**
     *
     * @param pColumn
     * @return
     */
    public String getString(int pColumn) {
        if (iRow_POI == null) {
            return "";
        }
        Cell iCell = iRow_POI.getCell(pColumn);
        if (iCell == null) {
            return "";
        }
        return SSExcelCell.formatCellValue(iCell);
    }

    /**
     *
     * @param pColumn
     * @return
     */
    public Integer getInteger(int pColumn) {
        try {
            return Integer.parseInt(getString(pColumn));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     *
     * @param pColumn
     * @return
     */
    public Double getDouble(int pColumn) {
        try {
            return Double.parseDouble(getString(pColumn));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     *
     * @param pColumn
     * @return
     */
    public Date getDate(int pColumn) {
        if (iRow_POI == null) {
            return null;
        }
        Cell iCell = iRow_POI.getCell(pColumn);
        if (iCell == null) {
            return null;
        }
        return new SSExcelCell(iCell, iRow, pColumn).getDate();
    }

    /**
     *
     * @param pColumn
     * @return
     */
    public java.util.Optional<BigDecimal> getBigDecimal(int pColumn) {
        if (iRow_POI == null) {
            return java.util.Optional.empty();
        }
        Cell iCell = iRow_POI.getCell(pColumn);
        if (iCell == null) {
            return java.util.Optional.empty();
        }
        return new SSExcelCell(iCell, iRow, pColumn).getBigDecimal();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSExcelRow");
        sb.append("{iRow=").append(iRow);
        sb.append(", iRow_POI=").append(iRow_POI);
        sb.append('}');
        return sb.toString();
    }
}
