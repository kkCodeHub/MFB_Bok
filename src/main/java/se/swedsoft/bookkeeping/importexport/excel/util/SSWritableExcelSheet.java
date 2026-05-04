package se.swedsoft.bookkeeping.importexport.excel.util;


import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-14
 * Time: 11:30:33
 */
public class SSWritableExcelSheet {    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelSheet.class);


    private WritableSheet iSheet;

    /**
     *
     * @param pSheet
     */
    public SSWritableExcelSheet(WritableSheet pSheet) {
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
            iList.add(new SSWritableExcelRow(iSheet, iRow));
        }
        return iList;
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     * @throws WriteException
     */
    public void setString(int iRow, int iColumn, String pValue) throws WriteException {

        try {
            iSheet.addCell(new Label(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     * @throws WriteException
     */
    public void setInteger(int iRow, int iColumn, Integer pValue) throws WriteException {
        try {
            iSheet.addCell(new jxl.write.Number(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param iRow
     * @param iColumn
     * @param pValue
     * @throws WriteException
     */
    public void setDouble(int iRow, int iColumn, Double pValue) throws WriteException {
        try {
            iSheet.addCell(new Number(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @return
     */
    public WritableSheet getSheet() {
        return iSheet;
    }

    /**
     *
     * @param iSheet
     */
    public void setSheet(WritableSheet iSheet) {
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
