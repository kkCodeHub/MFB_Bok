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
        SSVoucher existingVoucher = SSTestDataFactory.balancedVoucher(
                1, LocalDate.of(2025, 1, 10), null,
                1910, new BigDecimal("100.00"),
                3010, new BigDecimal("100.00"));
        SSAccountingContext.addVoucher(existingVoucher, true);
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();

        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-duplicate-existing-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 1 20250110 \"Dup\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Importen avbryts");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
    }

    @Test
    void voucherImportAbortsWhenFileContainsDuplicateVoucherNumber() throws Exception {
        int voucherCountBeforeImport = SSAccountingContext.getVouchers().size();
        SSTestFileHelper.withTempFile("fribok-sie-voucher-import-duplicate-file-", ".se", sieFile -> {
            Files.write(sieFile, List.of(
                    "#FLAGGA 0",
                    "#VER A 777777 20250110 \"Dup 1\"",
                    "#VER A 777777 20250111 \"Dup 2\""
            ), SIE_CHARSET);

            SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
            Throwable thrown = catchThrowable(importer::doImportVouchers);

            assertThat(thrown).isInstanceOf(SSImportException.class)
                    .hasMessageContaining("Dubblett");
            assertThat(SSAccountingContext.getVouchers()).hasSize(voucherCountBeforeImport);
        });
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

}
