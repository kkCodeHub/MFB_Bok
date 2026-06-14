package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link SSCustomerExporter}.
 */
class SSCustomerExporterTest {

    @Test
    void exportWritesCustomerNameAndInvoiceAddressNameToSeparateColumns() throws Exception {
        SSCustomer iCustomer = new SSCustomer();
        iCustomer.setNumber("C001");
        iCustomer.setName("Kundnamn AB");
        iCustomer.getInvoiceAddress().setName("Faktura Namn AB");
        iCustomer.getInvoiceAddress().setAddress1("Fakturaadress 1");

        SSTestFileHelper.withTempFile("fribok-customer-export", ".xlsx", iFile -> {
            SSCustomerExporter iExporter = new SSCustomerExporter(iFile.toFile(), List.of(iCustomer));
            iExporter.export();

            try (InputStream iInputStream = Files.newInputStream(iFile); Workbook iWorkbook = new XSSFWorkbook(iInputStream)) {
                Sheet iSheet = iWorkbook.getSheetAt(0);
                Row iHeader = iSheet.getRow(0);
                Row iRow = iSheet.getRow(1);

                assertThat(iHeader.getCell(1).getStringCellValue()).isEqualTo(SSCustomerExporter.NAMN);
                assertThat(iHeader.getCell(10).getStringCellValue())
                        .isEqualTo(SSCustomerExporter.FAKTURAADRESS_NAMN);
                assertThat(iRow.getCell(1).getStringCellValue()).isEqualTo("Kundnamn AB");
                assertThat(iRow.getCell(10).getStringCellValue()).isEqualTo("Faktura Namn AB");
                assertThat(iRow.getCell(11).getStringCellValue()).isEqualTo("Fakturaadress 1");
            }
        });
    }
}
