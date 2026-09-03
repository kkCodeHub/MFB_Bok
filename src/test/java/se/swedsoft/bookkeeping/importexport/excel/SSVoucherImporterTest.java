package se.swedsoft.bookkeeping.importexport.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.testsupport.system.SSDBTestFixture;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSVoucherImporterTest {

    private static final int EXISTING_NUMBER = 91_001;
    private static final int UNIQUE_NUMBER = 91_002;
    private static final int FILE_DUPLICATE_NUMBER = 91_003;

    private static String originalHeadlessProperty;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setupDatabase() throws Exception {
        originalHeadlessProperty = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
        SSDBTestFixture.setupOnce();
    }

    @AfterAll
    static void restoreHeadlessProperty() {
        if (originalHeadlessProperty == null) {
            System.clearProperty("java.awt.headless");
        } else {
            System.setProperty("java.awt.headless", originalHeadlessProperty);
        }
    }

    @BeforeEach
    void resetState() {
        SSDBTestFixture.resetCaches();
    }

    @AfterEach
    void clearState() {
        deleteVoucher(EXISTING_NUMBER);
        deleteVoucher(UNIQUE_NUMBER);
        deleteVoucher(FILE_DUPLICATE_NUMBER);
        SSDBTestFixture.resetCaches();
    }

    @Test
    void importsVoucherBlocksAndSkipsDuplicateNumbers() throws Exception {
        addExistingVoucher(EXISTING_NUMBER);

        Path file = tempDir.resolve("verifikationer.xlsx");
        writeWorkbook(file);

        new SSVoucherImporter(file.toFile()).Import();

        SSDBTestFixture.resetCaches();
        List<SSVoucher> vouchers = SSAccountingContext.getVouchers();

        assertThat(vouchers).extracting(SSVoucher::getNumber)
                .contains(UNIQUE_NUMBER, FILE_DUPLICATE_NUMBER, EXISTING_NUMBER);
        assertThat(vouchers).filteredOn(voucher -> voucher.getNumber() == EXISTING_NUMBER)
                .hasSize(1);
        assertThat(vouchers).filteredOn(voucher -> voucher.getNumber() == FILE_DUPLICATE_NUMBER)
                .hasSize(1);
        SSVoucher importedVoucher = vouchers.stream()
                .filter(voucher -> voucher.getNumber() == UNIQUE_NUMBER)
                .findFirst()
                .orElseThrow();
        assertThat(importedVoucher.getRows()).hasSize(2);
    }

    @Test
    void importReportIncludesImportedAndSkippedDuplicates() {
        SSVoucher voucher = new SSVoucher(UNIQUE_NUMBER);
        voucher.setDescription("Test voucher");

        SSVoucherImporter importer = new SSVoucherImporter(Path.of("unused.xlsx").toFile());
        String report = importer.buildImportReportText(List.of(voucher), List.of(EXISTING_NUMBER, FILE_DUPLICATE_NUMBER));

        assertThat(report).contains("Följande verifikationer kommer att importeras");
        assertThat(report).contains("Hoppade dubbletter");
        assertThat(report).contains(Integer.toString(UNIQUE_NUMBER));
        assertThat(report).contains(Integer.toString(EXISTING_NUMBER));
        assertThat(report).contains(Integer.toString(FILE_DUPLICATE_NUMBER));
    }

    private static void writeWorkbook(Path file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Verifikationer");
            writeHeader(sheet.createRow(0));
            writeVoucherRow(sheet.createRow(1), UNIQUE_NUMBER, "Unique voucher", "2026-01-10");
            writeTransactionRow(sheet.createRow(2), 1910, new BigDecimal("100.00"), null, null, null);
            writeTransactionRow(sheet.createRow(3), 3010, null, new BigDecimal("100.00"), null, null);
            writeVoucherRow(sheet.createRow(4), EXISTING_NUMBER, "Duplicate existing", "2026-01-11");
            writeTransactionRow(sheet.createRow(5), 1910, new BigDecimal("200.00"), null, null, null);
            writeTransactionRow(sheet.createRow(6), 3010, null, new BigDecimal("200.00"), null, null);
            writeVoucherRow(sheet.createRow(7), FILE_DUPLICATE_NUMBER, "First file occurrence", "2026-01-12");
            writeTransactionRow(sheet.createRow(8), 1910, new BigDecimal("300.00"), null, null, null);
            writeTransactionRow(sheet.createRow(9), 3010, null, new BigDecimal("300.00"), null, null);
            writeVoucherRow(sheet.createRow(10), FILE_DUPLICATE_NUMBER, "Second file occurrence", "2026-01-13");
            writeTransactionRow(sheet.createRow(11), 1910, new BigDecimal("400.00"), null, null, null);
            writeTransactionRow(sheet.createRow(12), 3010, null, new BigDecimal("400.00"), null, null);

            try (OutputStream outputStream = Files.newOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    private static void writeHeader(Row row) {
        row.createCell(0).setCellValue(SSVoucherExporter.NUMMER);
        row.createCell(1).setCellValue(SSVoucherExporter.BESKRIVNING);
        row.createCell(2).setCellValue(SSVoucherExporter.DATUM);
        row.createCell(3).setCellValue(SSVoucherExporter.KONTO);
        row.createCell(4).setCellValue(SSVoucherExporter.DEBET);
        row.createCell(5).setCellValue(SSVoucherExporter.KREDIT);
        row.createCell(6).setCellValue(SSVoucherExporter.PROJEKT);
        row.createCell(7).setCellValue(SSVoucherExporter.RESULTATENHET);
    }

    private static void writeVoucherRow(Row row, int number, String description, String date) {
        row.createCell(0).setCellValue(number);
        row.createCell(1).setCellValue(description);
        row.createCell(2).setCellValue(date);
    }

    private static void writeTransactionRow(Row row, int account, BigDecimal debet, BigDecimal credit,
            String project, String resultUnit) {
        row.createCell(3).setCellValue(account);
        if (debet != null) {
            row.createCell(4).setCellValue(debet.doubleValue());
        }
        if (credit != null) {
            row.createCell(5).setCellValue(credit.doubleValue());
        }
        if (project != null) {
            row.createCell(6).setCellValue(project);
        }
        if (resultUnit != null) {
            row.createCell(7).setCellValue(resultUnit);
        }
    }

    private static void addExistingVoucher(int number) {
        SSVoucher voucher = new SSVoucher(number);
        voucher.setDescription("Existing voucher");
        voucher.setLocalDate(LocalDate.of(2026, 1, 1));
        voucher.getRows().add(voucherRow(1910, new BigDecimal("50.00"), null));
        voucher.getRows().add(voucherRow(3010, null, new BigDecimal("50.00")));
        SSAccountingContext.addVoucher(voucher, true);
    }

    private static void deleteVoucher(int number) {
        SSAccountingContext.getVoucher(new SSVoucher(number))
                .ifPresent(SSAccountingContext::deleteVoucher);
    }

    private static SSVoucherRow voucherRow(int account, BigDecimal debet, BigDecimal credit) {
        SSVoucherRow row = new SSVoucherRow();
        row.setAccountNr(account);
        row.setDebet(debet);
        row.setCredit(credit);
        return row;
    }
}
