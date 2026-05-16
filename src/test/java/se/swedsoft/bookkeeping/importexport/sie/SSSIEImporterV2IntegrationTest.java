package se.swedsoft.bookkeeping.importexport.sie;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for SIE import behavior in schema V2.
 */
@Tag("integration")
class SSSIEImporterV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_sie_importer";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 SIE Importer Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 SIE Importer Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void clearCaches() {
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void importHandlesVoucherDeletionWithoutConcurrentModification() throws Exception {
        SSVoucher voucher = new SSVoucher(97_001);
        voucher.setLocalDate(LocalDate.of(2025, 6, 10));

        SSVoucherRow row1 = new SSVoucherRow();
        row1.setAccountNr(1910);
        row1.setDebet(new BigDecimal("100.00"));
        voucher.getRows().add(row1);

        SSVoucherRow row2 = new SSVoucherRow();
        row2.setAccountNr(3010);
        row2.setCredit(new BigDecimal("100.00"));
        voucher.getRows().add(row2);

        SSDB.getInstance().addVoucher(voucher, true);
        assertThat(SSDB.getInstance().getVouchers()).isNotEmpty();

        Path sieFile = Files.createTempFile("fribok-sie-import-", ".se");
        Files.write(sieFile, List.of("#FLAGGA 0"), Charset.forName("IBM437"));

        SSSIEImporter importer = new SSSIEImporter(sieFile.toFile());
        assertThatCode(importer::doImport).doesNotThrowAnyException();

        SSDB.getInstance().clearLists();
        assertThat(SSDB.getInstance().getVouchers()).isEmpty();
    }

    private static Integer createCompany(String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create test company for schema V2 SIE importer test");
    }
}

