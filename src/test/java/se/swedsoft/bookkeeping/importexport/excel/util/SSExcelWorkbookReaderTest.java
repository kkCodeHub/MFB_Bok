package se.swedsoft.bookkeeping.importexport.excel.util;


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Tests for {@link SSExcelWorkbookReader}.
 */
class SSExcelWorkbookReaderTest {

    @Test
    void openWorkbookReadsXlsxWithXssfWorkbook() throws Exception {
        SSTestFileHelper.withTempFile("fribok-excel-import", ".xlsx", iFile -> {
            writeWorkbookFile(iFile, new XSSFWorkbook());

            try (Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(iFile.toFile())) {
                assertThat(iWorkbook).isInstanceOf(XSSFWorkbook.class);
                assertThat(iWorkbook.getNumberOfSheets()).isEqualTo(1);
            }
        });
    }

    @Test
    void openWorkbookReadsXlsWithHssfWorkbook() throws Exception {
        SSTestFileHelper.withTempFile("fribok-excel-import", ".xls", iFile -> {
            writeWorkbookFile(iFile, new HSSFWorkbook());

            try (Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(iFile.toFile())) {
                assertThat(iWorkbook).isInstanceOf(HSSFWorkbook.class);
                assertThat(iWorkbook.getNumberOfSheets()).isEqualTo(1);
            }
        });
    }

    @Test
    void openWorkbookThrowsForUnsupportedExtension() throws Exception {
        SSTestFileHelper.withTempFile("fribok-excel-import", ".txt", iFile -> {
            assertThatThrownBy(() -> {
                        try (Workbook iWorkbook = SSExcelWorkbookReader.openWorkbook(
                                iFile.toFile())) {
                            assertThat(iWorkbook).isNotNull();
                        }
                    })
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining(".xlsx")
                    .hasMessageContaining(".xls");
        });
    }

    private static void writeWorkbookFile(Path pFile, Workbook pWorkbook)
            throws IOException {
        try (Workbook iWorkbook = pWorkbook; OutputStream iOutputStream = Files.newOutputStream(pFile)) {
            iWorkbook.createSheet("Sheet1");
            iWorkbook.write(iOutputStream);
        }
    }
}

