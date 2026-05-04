package se.swedsoft.bookkeeping.importexport.excel.util;


import jxl.format.CellFormat;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

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
public class SSWritableExcelRow {    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelRow.class);


    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private int iRow;

    private WritableSheet iSheet;

    /**
     *
     * @param pSheet
     * @param pRow
     */
    public SSWritableExcelRow(WritableSheet pSheet, int pRow) {
        iSheet = pSheet;
        iRow = pRow;
    }

    /**
     *
     * @param pCount
     * @return
     */
    public List<SSWritableExcelCell> getCells(int pCount) {
        List<SSWritableExcelCell> iList = new LinkedList<>();

        for (int iColumn = 0; iColumn < pCount; iColumn++) {
            iList.add(new SSWritableExcelCell(iSheet, iRow, iColumn));
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
     * @throws WriteException
     */
    public void setString(int iColumn, String pValue) throws WriteException {
        try {
            iSheet.addCell(new Label(iColumn, iRow, pValue == null ? "" : pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellFormat
     * @throws WriteException
     */
    public void setString(int iColumn, String pValue, CellFormat iCellFormat) throws WriteException {
        try {
            iSheet.addCell(
                    new Label(iColumn, iRow, pValue == null ? "" : pValue, iCellFormat));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @throws WriteException
     */
    public void setNumber(int iColumn, java.lang.Number pValue) throws WriteException {
        try {
            if (pValue == null) {
                iSheet.addCell(new Label(iColumn, iRow, ""));
            } else {
                iSheet.addCell(new Number(iColumn, iRow, pValue.doubleValue()));
            }
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }

    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellFormat
     * @throws WriteException
     */
    public void setNumber(int iColumn, java.lang.Number pValue, CellFormat iCellFormat) throws WriteException {
        try {
            if (pValue == null) {
                iSheet.addCell(new Label(iColumn, iRow, "", iCellFormat));
            } else {
                iSheet.addCell(
                        new Number(iColumn, iRow, pValue.doubleValue(), iCellFormat));
            }
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @throws WriteException
     */
    public void setDate(int iColumn, Date pValue) throws WriteException {
        setDate(iColumn, SSDateUtil.toLocalDate(pValue));
    }

    public void setDate(int iColumn, LocalDate pValue) throws WriteException {
        try {
            if (pValue == null) {
                iSheet.addCell(new Label(iColumn, iRow, ""));
            } else {
                iSheet.addCell(new Label(iColumn, iRow, pValue.format(DATE_FORMAT)));
            }

        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iColumn
     * @param pValue
     * @param iCellFormat
     * @throws WriteException
     */
    public void setDate(int iColumn, Date pValue, CellFormat iCellFormat) throws WriteException {
        setDate(iColumn, SSDateUtil.toLocalDate(pValue), iCellFormat);
    }

    public void setDate(int iColumn, LocalDate pValue, CellFormat iCellFormat) throws WriteException {
        try {
            if (pValue == null) {
                iSheet.addCell(new Label(iColumn, iRow, "", iCellFormat));
            } else {
                iSheet.addCell(new Label(iColumn, iRow, pValue.format(DATE_FORMAT), iCellFormat));
            }
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSWritableExcelRow");
        sb.append("{iRow=").append(iRow);
        sb.append(", iSheet=").append(iSheet);
        sb.append('}');
        return sb.toString();
    }
}
