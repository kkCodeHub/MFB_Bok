package se.swedsoft.bookkeeping.importexport.excel.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests for numeric-safe string access in Excel wrappers.
 */
class SSExcelCellTest {

    @Test
    void getStringReturnsFormattedNumericValue() throws IOException {
        try (XSSFWorkbook iWorkbook = new XSSFWorkbook()) {
            Row iRow = iWorkbook.createSheet("Sheet1").createRow(0);
            iRow.createCell(0).setCellValue(2024);

            SSExcelCell iCell = new SSExcelCell(iRow.getCell(0), 0, 0);

            assertThat(iCell.getString()).isEqualTo("2024");
        }
    }

    @Test
    void rowGetStringReturnsFormattedNumericValue() throws IOException {
        try (XSSFWorkbook iWorkbook = new XSSFWorkbook()) {
            Row iRowPoi = iWorkbook.createSheet("Sheet1").createRow(0);
            iRowPoi.createCell(1).setCellValue(2024);

            SSExcelRow iRow = new SSExcelRow(iRowPoi, 0);

            assertThat(iRow.getString(1)).isEqualTo("2024");
        }
    }

    @Test
    void rowWithNumericCellIsNotEmpty() throws IOException {
        try (XSSFWorkbook iWorkbook = new XSSFWorkbook()) {
            Row iRowPoi = iWorkbook.createSheet("Sheet1").createRow(0);
            iRowPoi.createCell(0).setCellValue(1);

            SSExcelRow iRow = new SSExcelRow(iRowPoi, 0);

            assertThat(iRow.empty()).isFalse();
        }
    }
}


