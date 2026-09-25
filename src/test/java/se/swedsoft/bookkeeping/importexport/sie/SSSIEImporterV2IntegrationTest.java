package se.swedsoft.bookkeeping.importexport.sie;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;
import se.swedsoft.bookkeeping.testsupport.data.SSTestDataFactory;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Integration test for SIE import behavior in schema V2.
 */
    @Tag("integration")
class SSSIEImporterV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_sie_importer";
    private static final Charset SIE_CHARSET = Charset.forName("IBM437");

    private static Connection connection;

    @BeforeAll
    static void setupV2Database() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(JDBC_URL);

        Integer iCompanyId = SSV2DatabaseFixture.createCompany(connection, "V2 SIE Importer Test Company AB");
        SSV2DatabaseFixture.setCurrentCompany(iCompanyId, "V2 SIE Importer Test Company AB");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterAll
    static void teardownV2Database() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @BeforeEach
    void resetState() {
        SSV2DatabaseFixture.clearState();
        SSCompanyYearContext.getCurrentYear();
    }

    @AfterEach
    void cleanupState() {
        SSV2DatabaseFixture.clearState();
    }

    @Test
    void importHandlesVoucherDeletionWithoutConcurrentModification() throws Exception {
        SSVoucher voucher = SSTestDataFactory.balancedVoucher(
                97_001, LocalDate.of(2025, 6, 10), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));

        SSAccountingContext.addVoucher(voucher, true);
        assertThat(SSAccountingContext.getVouchers()).isNotEmpty();

        SSTestFileHelper.withTempFile("fribok-sie-import-", ".se", sieFile -> {
            Files.write(sieFile, readResourceLines("sie-import-flagga0.se"), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImport);
            assertThat(hasCause(thrown, ConcurrentModificationException.class)).isFalse();

            SSV2DatabaseFixture.clearState();
            assertThat(SSAccountingContext.getVouchers()).isEmpty();
        });
    }

    @Test
    void importPersistsAccountPlanAndOpeningBalanceAndDoesNotModifySourceFile() throws Exception {
        SSTestFileHelper.withTempFile("fribok-sie-import-full-", ".se", sieFile -> {
            Files.write(sieFile, readResourceLines("sie-import-full.se"), SIE_CHARSET);
            int accountPlansBeforeImport = countAccountPlans();

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImport);
            assertThat(hasCause(thrown, ConcurrentModificationException.class)).isFalse();

            SSV2DatabaseFixture.clearState();
            SSNewAccountingYear year = SSCompanyYearContext.getCurrentYear();
            int accountPlansAfterImport = countAccountPlans();

            assertThat(year.getAccountPlan().getName())
                    .isEqualTo("SIEimp V2 SIE Importer Test Company AB 2025");
            assertThat(year.getAccountPlan().getBaseName())
                    .isEqualTo("SIEimp V2 SIE Importer Test Company AB 2025");
            assertThat(year.getAccountPlan().getAccount(9998)).isNotNull();
            assertThat(accountPlansAfterImport).isEqualTo(accountPlansBeforeImport);

            Map<SSAccount, BigDecimal> inBalance = year.getInBalance();
            assertThat(inBalance.entrySet())
                    .anyMatch(entry -> entry.getKey() != null
                            && Integer.valueOf(9998).equals(entry.getKey().getNumber())
                            && new BigDecimal("1234.00").compareTo(entry.getValue()) == 0);

            List<String> persistedLines = Files.readAllLines(sieFile, SIE_CHARSET);
            assertThat(persistedLines).contains("#FLAGGA 0");
            assertThat(persistedLines).doesNotContain("#FLAGGA 1");
        });
    }

    private static int countAccountPlans() throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM PUBLIC.tbl_accountplan");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    @Test
    void importAbortsWhenRar0DiffersFromOpenYear() throws Exception {
        SSTestFileHelper.withTempFile("fribok-sie-import-rar0-", ".se", sieFile -> {
            Files.write(sieFile, readResourceLines("sie-import-rar0-mismatch.se"), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImport);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("RAR 0");
        });
    }

    @Test
    void voucherImportAbortsWhenNumberAlreadyExistsInOpenYear() throws Exception {
        final int iVoucherNumber = 980001;
        SSVoucher existingVoucher = SSTestDataFactory.balancedVoucher(
                iVoucherNumber, LocalDate.of(2025, 1, 10), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));
        existingVoucher.setSeries("A");
        SSAccountingContext.addVoucher(existingVoucher, true);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();

        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-duplicate-existing-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A " + iVoucherNumber + " 20250110 \"Dup\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Importen avbryts");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
    }

    @Test
    void voucherImportAllowsSameNumberWhenSeriesDiffersFromExistingVoucher() throws Exception {
        final int iVoucherNumber = 980002;
        SSVoucher existingVoucher = SSTestDataFactory.balancedVoucher(
                iVoucherNumber, LocalDate.of(2025, 1, 10), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));
        existingVoucher.setSeries("A");
        SSAccountingContext.addVoucher(existingVoucher, true);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();

        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-existing-other-series-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER B " + iVoucherNumber + " 20250110 \"Serie B\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isNull();
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport + 1);
            assertThat(hasVoucher("A", iVoucherNumber)).isTrue();
            assertThat(hasVoucher("B", iVoucherNumber)).isTrue();
        });
    }

    @Test
    void voucherImportAbortsWhenFileContainsDuplicateVoucherNumber() throws Exception {
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();
        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-duplicate-file-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 991234 20250110 \"Dup 1\"",
                    "#VER A 991234 20250111 \"Dup 2\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Dubblett");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
    }

    @Test
    void voucherImportAllowsSameNumberWhenSeriesDiffersWithinFile() throws Exception {
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();
        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-same-number-other-series-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 991235 20250110 \"Serie A\"",
                    "#VER B 991235 20250111 \"Serie B\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isNull();
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport + 2);
            assertThat(hasVoucher("A", 991235)).isTrue();
            assertThat(hasVoucher("B", 991235)).isTrue();
        });
    }

    @Test
    void voucherImportCanRenumberIntoSeriesForEventCodeIn() throws Exception {
        ensureAccountsExist(1910, 3010);
        SSVoucher existingVoucher = SSTestDataFactory.balancedVoucher(
                8, LocalDate.of(2025, 1, 9), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));
        existingVoucher.setSeries("I");
        SSAccountingContext.addVoucher(existingVoucher, true);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();

        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-in-series-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 1 20250110 \"Serie A\"",
                    "{",
                    "#TRANS 1910 {} -100.00 20250110 \"\" 0",
                    "#TRANS 3010 {} 100.00 20250110 \"\" 0",
                    "}",
                    "#VER B 1 20250111 \"Serie B\"",
                    "{",
                    "#TRANS 1910 {} -200.00 20250111 \"\" 0",
                    "#TRANS 3010 {} 200.00 20250111 \"\" 0",
                    "}"
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(() ->
                    importer.doImportVouchers(SSSIEImporter.VoucherImportMode.USE_EVENT_CODE_IN));

            assertThat(thrown).isNull();
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport + 2);
            assertThat(hasVoucher("I", 9)).isTrue();
            assertThat(hasVoucher("I", 10)).isTrue();
            assertThat(hasVoucher("A", 1)).isFalse();
            assertThat(hasVoucher("B", 1)).isFalse();
        });
    }

    @Test
    void voucherImportWithEventCodeInAllowsDuplicateSourceVoucherNumbers() throws Exception {
        ensureAccountsExist(1910, 3010);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();
        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-duplicate-source-number-in-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 77 20250110 \"Serie A\"",
                    "{",
                    "#TRANS 1910 {} -100.00 20250110 \"\" 0",
                    "#TRANS 3010 {} 100.00 20250110 \"\" 0",
                    "}",
                    "#VER B 77 20250111 \"Serie B\"",
                    "{",
                    "#TRANS 1910 {} -150.00 20250111 \"\" 0",
                    "#TRANS 3010 {} 150.00 20250111 \"\" 0",
                    "}"
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(() ->
                    importer.doImportVouchers(SSSIEImporter.VoucherImportMode.USE_EVENT_CODE_IN));

            assertThat(thrown).isNull();
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport + 2);
        });
    }

    @Test
    void voucherImportAbortsWithReadableMessageWhenVoucherDateMatchesNoAccountingYear() throws Exception {
        ensureAccountsExist(1910, 3010);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();
        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-date-outside-year-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 1 20280417 \"Fel år\"",
                    "{",
                    "#TRANS 1910 {} -100.00 20280417 \"\" 0",
                    "#TRANS 3010 {} 100.00 20280417 \"\" 0",
                    "}"
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Import av SIE-verifikat avbryts.")
                    .hasMessageContaining("Verifikationsdatum 2028-04-17 ligger utanfor oppet bokforingsar");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
    }

    @Test
    void voucherImportChecksAccountingYearBeforeDuplicateCheck() throws Exception {
        ensureAccountsExist(1910, 3010);
        SSVoucher existingVoucher = SSTestDataFactory.balancedVoucher(
                1, LocalDate.of(2025, 1, 10), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));
        existingVoucher.setSeries("A");
        SSAccountingContext.addVoucher(existingVoucher, true);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();

        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-year-before-duplicate-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 1 20280417 \"Fel år\"",
                    "{",
                    "#TRANS 1910 {} -100.00 20280417 \"\" 0",
                    "#TRANS 3010 {} 100.00 20280417 \"\" 0",
                    "}"
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Verifikationsdatum 2028-04-17 ligger utanfor oppet bokforingsar")
                    .hasMessageNotContaining("Verifikationsnummer finns redan");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
    }

    private static void ensureAccountsExist(int... pAccountNumbers) {
        SSNewAccountingYear iYear = SSCompanyYearContext.getCurrentYear();
        boolean iUpdated = false;
        for (int iAccountNumber : pAccountNumbers) {
            if (iYear.getAccountPlan().getAccount(iAccountNumber) != null) {
                continue;
            }
            iYear.getAccountPlan().addAccount(new SSAccount(iAccountNumber));
            iUpdated = true;
        }
        if (iUpdated) {
            SSAccountingContext.updateAccountingYear(iYear);
        }
    }

    private static List<String> readResourceLines(String pName) throws IOException {
        String iPath = "/se/swedsoft/bookkeeping/importexport/sie/" + pName;
        InputStream iStream = SSSIEImporterV2IntegrationTest.class.getResourceAsStream(iPath);
        if (iStream == null) {
            throw new IOException("Resource not found: " + iPath);
        }

        try (BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8))) {
            return iReader.lines().toList();
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasVoucher(String series, int number) {
        SSVoucher voucher = new SSVoucher(number);
        voucher.setSeries(series);
        return SSAccountingContext.getVoucher(voucher).isPresent();
    }

}
