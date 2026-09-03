package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

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

    private static final DataFormatter STRING_FORMATTER = new DataFormatter();

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
        if (iCell == null) {
            return "";
        }
        return formatCellValue(iCell);
    }

    /**
     *
     * @return
     */
    public Integer getInteger() {
        if (iCell == null) {
            return 0;
        }
        if (iCell.getCellType() == CellType.NUMERIC) {
            return (int) iCell.getNumericCellValue();
        }
        try {
            return Integer.parseInt(formatCellValue(iCell));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     *
     * @return
     */
    public Double getDouble() {
        if (iCell == null) {
            return 0.0;
        }
        if (iCell.getCellType() == CellType.NUMERIC) {
            return iCell.getNumericCellValue();
        }
        try {
            return Double.parseDouble(formatCellValue(iCell));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     *
     * @return
     */
    public Date getDate() {
        if (iCell == null) {
            return SSDateUtil.toDate(SSDateUtil.today());
        }
        if (iCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(iCell)) {
            return iCell.getDateCellValue();
        }
        DateTimeFormatter iFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            return SSDateUtil.toDate(LocalDate.parse(formatCellValue(iCell), iFormat));
        } catch (DateTimeParseException e) {
            return SSDateUtil.toDate(SSDateUtil.today());
        }
    }

    /**
     *
     * @return
     */
    public Optional<BigDecimal> getBigDecimal() {
        if (iCell == null) {
            return Optional.empty();
        }
        if (iCell.getCellType() == CellType.NUMERIC) {
            return Optional.of(new BigDecimal(iCell.getNumericCellValue()));
        }
        try {
            return Optional.of(new BigDecimal(formatCellValue(iCell)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static String formatCellValue(Cell pCell) {
        if (pCell == null) {
            return "";
        }
        return STRING_FORMATTER.formatCellValue(pCell);
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
