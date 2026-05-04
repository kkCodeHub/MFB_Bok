package se.swedsoft.bookkeeping.importexport.excel.util;


import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-14
 * Time: 11:57:23
 */
public class SSWritableExcelCell {    private static final Logger LOG = LoggerFactory.getLogger(SSWritableExcelCell.class);


    private int iRow;

    private int iColumn;

    private WritableSheet iSheet;

    /**
     *
     * @param pSheet
     * @param pRow
     * @param pColumn
     */
    public SSWritableExcelCell(WritableSheet pSheet, int pRow, int pColumn) {
        iSheet = pSheet;
        iRow = pRow;
        iColumn = pColumn;

    }

    /**
     *
     * @param pValue
     * @throws WriteException
     */
    public void setString(String pValue) throws WriteException {
        try {
            iSheet.addCell(new Label(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param pValue
     * @throws WriteException
     */
    public void setInteger(Integer pValue) throws WriteException {
        try {
            iSheet.addCell(new Number(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     *
     * @param pValue
     * @throws WriteException
     */
    public void setDouble(Double pValue) throws WriteException {
        try {
            iSheet.addCell(new Number(iColumn, iRow, pValue));
        } catch (RowsExceededException e) {
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
        sb.append(", iSheet=").append(iSheet);
        sb.append('}');
        return sb.toString();
    }
}
