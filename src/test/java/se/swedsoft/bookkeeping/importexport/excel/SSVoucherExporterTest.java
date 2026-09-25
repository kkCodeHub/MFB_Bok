package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherExporterTest {

    @Test
    void exportWritesVoucherSeriesAsFirstColumn() throws Exception {
        SSVoucher iVoucher = new SSVoucher(1234);
        iVoucher.setSeries("C");
        iVoucher.setDescription("Testverifikat");
        SSVoucherRow iVoucherRow = new SSVoucherRow();
        iVoucherRow.setAccountNr(1910);
        iVoucherRow.setDebet(new BigDecimal("100.00"));
        iVoucher.getRows().add(iVoucherRow);

        SSTestFileHelper.withTempFile("fribok-voucher-export", ".xlsx", iFile -> {
            new SSVoucherExporter(iFile.toFile(), List.of(iVoucher)).export();

            try (InputStream iInputStream = Files.newInputStream(iFile); Workbook iWorkbook = new XSSFWorkbook(iInputStream)) {
                Sheet iSheet = iWorkbook.getSheetAt(0);
                Row iHeader = iSheet.getRow(0);
                Row iVoucherHeaderRow = findVoucherHeaderRow(iSheet, 1234);

                assertThat(iHeader.getCell(0).getStringCellValue()).isEqualTo(SSVoucherExporter.VERIFIKATSERIE);
                assertThat(iHeader.getCell(1).getStringCellValue()).isEqualTo(SSVoucherExporter.NUMMER);
                assertThat(iVoucherHeaderRow.getCell(0).getStringCellValue()).isEqualTo("C");
            }
        });
    }

    private static Row findVoucherHeaderRow(Sheet iSheet, int iVoucherNumber) {
        for (int iRowIndex = 1; iRowIndex <= iSheet.getLastRowNum(); iRowIndex++) {
            Row iRow = iSheet.getRow(iRowIndex);
            if (iRow == null) {
                continue;
            }
            Cell iNumberCell = iRow.getCell(1);
            if (iNumberCell == null || iNumberCell.getCellType() != CellType.NUMERIC) {
                continue;
            }
            if ((int) iNumberCell.getNumericCellValue() == iVoucherNumber) {
                return iRow;
            }
        }
        throw new AssertionError("Could not find voucher header row for voucher " + iVoucherNumber);
    }
}
