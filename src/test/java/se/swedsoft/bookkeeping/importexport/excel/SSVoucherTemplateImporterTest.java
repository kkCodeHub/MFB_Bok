package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.importexport.excel.util.SSExcelSheet;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherTemplateImporterTest {

    @Test
    void importVouchersGroupsRowsUnderSameHeaderUntilNextHeader() throws Exception {
        SSVoucherTemplateImporter importer = new SSVoucherTemplateImporter(new File("unused.xlsx"));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Konteringmallar");
            sheet.createRow(0).createCell(0).setCellValue("Beskrivning");
            sheet.getRow(0).createCell(1).setCellValue("Konto");
            sheet.getRow(0).createCell(2).setCellValue("Debet");
            sheet.getRow(0).createCell(3).setCellValue("Kredit");

            sheet.createRow(1).createCell(0).setCellValue("Mall A");
            sheet.createRow(2).createCell(1).setCellValue(1910);
            sheet.getRow(2).createCell(2).setCellValue(100);
            sheet.createRow(3).createCell(1).setCellValue(3010);
            sheet.getRow(3).createCell(3).setCellValue(100);
            sheet.createRow(4).createCell(0).setCellValue("Mall B");
            sheet.createRow(5).createCell(1).setCellValue(1510);
            sheet.getRow(5).createCell(2).setCellValue(55);

            List<SSVoucherTemplate> templates = importer.importVouchers(new SSExcelSheet(sheet));

            assertThat(templates).hasSize(2);
            assertThat(templates.get(0).getDescription()).isEqualTo("Mall A");
            assertThat(templates.get(0).getRows()).hasSize(2);
            assertThat(templates.get(0).getRows().get(0).getAccountNr()).isEqualTo(1910);
            assertThat(templates.get(0).getRows().get(1).getAccountNr()).isEqualTo(3010);
            assertThat(templates.get(1).getDescription()).isEqualTo("Mall B");
            assertThat(templates.get(1).getRows()).hasSize(1);
            assertThat(templates.get(1).getRows().get(0).getAccountNr()).isEqualTo(1510);
        }
    }

    @Test
    void buildImportReportTextIncludesDuplicateHeaderWarning() {
        SSVoucherTemplateImporter importer = new SSVoucherTemplateImporter(new File("unused.xlsx"));
        SSVoucherTemplate imported = new SSVoucherTemplate();
        imported.setDescription("Ny mall");
        SSVoucherTemplate duplicate = new SSVoucherTemplate();
        duplicate.setDescription("Befintlig mall");

        String report = importer.buildImportReportText(List.of(imported), List.of(duplicate));

        assertThat(report).contains("Hoppade dubbletter (rubriken finns redan):");
        assertThat(report).contains("Befintlig mall");
    }

    @Test
    void getExistingDuplicatesMatchesExactHeaderText() {
        SSVoucherTemplateImporter importer = new SSVoucherTemplateImporter(new File("unused.xlsx"));
        SSVoucherTemplate importedA = new SSVoucherTemplate();
        importedA.setDescription("Likadan");
        SSVoucherTemplate importedB = new SSVoucherTemplate();
        importedB.setDescription("Annan");
        SSVoucherTemplate existing = new SSVoucherTemplate();
        existing.setDescription("Likadan");

        List<SSVoucherTemplate> duplicates = importer.getExistingDuplicates(
                List.of(importedA, importedB),
                List.of(existing)
        );

        assertThat(duplicates).containsExactly(importedA);
    }
}
