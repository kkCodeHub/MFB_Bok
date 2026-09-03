package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import se.swedsoft.bookkeeping.util.SSDateUtil;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 2006-feb-14
 * Time: 11:36:34
 */
public class SSWritableExcelRow {
    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelRow.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private int iRow;

    private Row iRow_POI;

    /**
     *
     * @param pRow
     * @param pRowNum
     */
    public SSWritableExcelRow(Row pRow, int pRowNum) {
        iRow_POI = pRow;
        iRow = pRowNum;
    }

    /**
     *
     * @param pCount
     * @return
     */
    public List<SSWritableExcelCell> getCells(int pCount) {
        List<SSWritableExcelCell> iList = new LinkedList<>();

        for (int iColumn = 0; iColumn < pCount; iColumn++) {
            iList.add(new SSWritableExcelCell(iRow_POI, iRow, iColumn));
        }
        return iList;
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
     * @param iColumn
     * @param pValue
     */
    public void setString(int iColumn, String pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            iCell.setCellValue(pValue == null ? "" : pValue);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellStyle
     */
    public void setString(int iColumn, String pValue, CellStyle iCellStyle) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            iCell.setCellValue(pValue == null ? "" : pValue);
            if (iCellStyle != null) {
                iCell.setCellStyle(iCellStyle);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     */
    public void setNumber(int iColumn, java.lang.Number pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue == null) {
                iCell.setCellValue("");
            } else {
                iCell.setCellValue(pValue.doubleValue());
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellStyle
     */
    public void setNumber(int iColumn, java.lang.Number pValue, CellStyle iCellStyle) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue == null) {
                iCell.setCellValue("");
            } else {
                iCell.setCellValue(pValue.doubleValue());
            }
            if (iCellStyle != null) {
                iCell.setCellStyle(iCellStyle);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     */
    public void setDate(int iColumn, Date pValue) {
        setDate(iColumn, SSDateUtil.toLocalDate(pValue));
    }

    public void setDate(int iColumn, LocalDate pValue) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue == null) {
                iCell.setCellValue("");
            } else {
                iCell.setCellValue(pValue.format(DATE_FORMAT));
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellStyle
     */
    public void setDate(int iColumn, Date pValue, CellStyle iCellStyle) {
        setDate(iColumn, SSDateUtil.toLocalDate(pValue), iCellStyle);
    }

    public void setDate(int iColumn, LocalDate pValue, CellStyle iCellStyle) {
        try {
            Cell iCell = iRow_POI.createCell(iColumn);
            if (pValue == null) {
                iCell.setCellValue("");
            } else {
                iCell.setCellValue(pValue.format(DATE_FORMAT));
            }
            if (iCellStyle != null) {
                iCell.setCellStyle(iCellStyle);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelRow");
        sb.append("{iRow=").append(iRow);
        sb.append(", iRow_POI=").append(iRow_POI);
        sb.append('}');
        return sb.toString();
    }
}
