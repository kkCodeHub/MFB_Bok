package se.swedsoft.bookkeeping.importexport.excel.util;


import jxl.Cell;
import jxl.DateCell;
import jxl.NumberCell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

import se.swedsoft.bookkeeping.util.SSDateUtil;


/**
 * Date: 2006-feb-14
 * Time: 11:57:23
 */
public class SSExcelCell {

    private int iRow;

    private int iColumn;

    private Cell iCell;

    public SSExcelCell(Cell pCell, int pRow, int pColumn) {
        iCell = pCell;
        iRow = pRow;
        iColumn = pColumn;

    }

    /**
     *
     * @return
     */
    public String getString() {
        return iCell.getContents();
    }

    /**
     *
     * @return
     */
    public Integer getInteger() {
        if (iCell instanceof NumberCell) {
            NumberCell iNumber = (NumberCell) iCell;

            return Math.round((float) iNumber.getValue());
        }
        try {
            return Integer.parseInt(iCell.getContents());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     *
     * @return
     */
    public Double getDouble() {
        if (iCell instanceof NumberCell) {
            NumberCell iNumber = (NumberCell) iCell;

            return iNumber.getValue();
        }

        try {
            return Double.parseDouble(iCell.getContents());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     *
     * @return
     */
    public Date getDate() {
        if (iCell instanceof DateCell) {
            DateCell   iDateCell = (DateCell) iCell;

            return iDateCell.getDate();
        }
        DateTimeFormatter iFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            return SSDateUtil.toDate(LocalDate.parse(iCell.getContents(), iFormat));
        } catch (DateTimeParseException e) {
            return SSDateUtil.toDate(SSDateUtil.today());
        }
    }

    /**
     *
     * @return
     */
    public Optional<BigDecimal> getBigDecimal() {
        if (iCell instanceof NumberCell) {
            NumberCell iNumber = (NumberCell) iCell;

            return Optional.of(new BigDecimal(iNumber.getValue()));
        }
        try {
            return Optional.of(new BigDecimal(iCell.getContents()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     *
     * @return
     */
    public int getColumn() {
        return iColumn;
    }

    /**
     *
     * @return
     */
    public int getRow() {
        return iRow;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSExcelCell");
        sb.append("{iCell=").append(iCell);
        sb.append(", iColumn=").append(iColumn);
        sb.append(", iRow=").append(iRow);
        sb.append('}');
        return sb.toString();
    }
}
