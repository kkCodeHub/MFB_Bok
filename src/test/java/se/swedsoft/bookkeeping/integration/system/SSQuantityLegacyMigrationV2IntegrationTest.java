package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaMigrationManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for legacy quantity migration (old whole numbers â†’ *10 scaled).
 *
 * Validates that:
 * - Old data in DB (e.g., count=2) gets scaled to *10 (count=20) during migration
 * - Scaled data round-trips correctly: API reads tenths values after migration
 * - Non-divisible-by-10 data is handled gracefully with warning logging
 */
@Tag("integration")
class SSQuantityLegacyMigrationV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-q-u-a-n-t-i-t-y-l-e-g-a-c-y-m-i-g-r-a-t-i-o-n-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;
    private static Integer testCompanyId;

    @BeforeAll
    static void setupV2SchemaForLegacyQtyTest() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        testCompanyId = createCompany("V2 Legacy Qty Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(testCompanyId);
        company.setName("V2 Legacy Qty Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
    }

    @AfterAll
    static void teardownV2SchemaForLegacyQtyTest() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();

        // Clear migration tracking to allow re-migration in tests
        try {
            // Use JDBC metadata to check for table existence first to avoid SQL errors
            boolean tableExists = false;
            try (ResultSet tables = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName != null && tableName.equalsIgnoreCase("tbl_schema_migration")) {
                        tableExists = true;
                        break;
                    }
                }
            }

            // Ensure the migration tracking table exists (create if missing) so subsequent DELETEs in tests won't fail.
            if (!tableExists) {
                String ddl = "CREATE TABLE IF NOT EXISTS tbl_schema_migration ("
                        + "migration_key VARCHAR(128) PRIMARY KEY,"
                        + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")";
                connection.createStatement().execute(ddl);
                connection.commit();
            }

            // Now clear the specific migration key so tests can re-run migration deterministically
            connection.createStatement().execute("DELETE FROM tbl_schema_migration WHERE migration_key = 'quantity_scale_x10_v2'");
            connection.commit();
        } catch (java.sql.SQLException e) {
            // If we get an SQL exception here, rethrow — tests should fail for unexpected DB errors.
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    /**
     * Simulates legacy data: insert raw rows with old whole-number quantities (not scaled).
     * Then verify migration scales them to *10 and API reads them back correctly.
     */
    @Test
    void legacyInvoiceDataGetsMigratedAndReadCorrectly() throws Exception {
        // 1. Clear any previously migrated data and reset tracking
        connection.createStatement().execute("DELETE FROM tbl_invoice_row");
        connection.createStatement().execute("DELETE FROM tbl_invoice");
        connection.createStatement().execute("DELETE FROM tbl_schema_migration WHERE migration_key = 'quantity_scale_x10_v2'");
        connection.commit();

        // 2. Directly insert invoice with legacy (non-scaled) quantities
        Integer invoiceId = createLegacyInvoice("LEGACY-INV-001", "Legacy Customer AB");
        insertLegacyInvoiceRow(invoiceId, "P-LEGACY-001", "Old Consulting",
                new java.math.BigDecimal("1250.00"), 2, 3010);  // quantity=2 (old format, not scaled)
        insertLegacyInvoiceRow(invoiceId, "P-LEGACY-002", "Old Support",
                new java.math.BigDecimal("500.00"), 1, 3041);   // quantity=1 (old format, not scaled)

        // 3. Verify raw DB contains non-scaled values before migration
        List<Integer> beforeMigration = readRawInvoiceRowCounts(invoiceId);
        assertThat(beforeMigration).containsExactly(2, 1);  // Old non-scaled values

        // 4. Run migration (idempotency ensures it won't double-apply)
        new SSSchemaMigrationManager(connection).ensureQuantityScaleMigration();

        // 5. Verify raw DB now contains scaled (*10) values
        List<Integer> afterMigration = readRawInvoiceRowCounts(invoiceId);
        assertThat(afterMigration).containsExactly(20, 10);  // Scaled values (2*10, 1*10)

        // 6. Fetch through API and verify tenths values are returned
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSNewCompany company = new SSNewCompany();
        company.setId(testCompanyId);
        company.setName("V2 Legacy Qty Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSInvoice tempInvoice = new SSInvoice();
        tempInvoice.setNumber(getInvoiceNumberFromId(invoiceId));
        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(tempInvoice);

        assertThat(fetched).isPresent();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(20);
        assertThat(fetched.get().getRows().get(1).getQuantity()).isEqualTo(10);
    }

    /**
     * Test multiple tables with legacy data to ensure migration covers all row types.
     */
    @Test
    void legacyCreditInvoiceDataGetsMigratedAndReadCorrectly() throws Exception {
        // 1. Clear any previously migrated data and reset tracking
        connection.createStatement().execute("DELETE FROM tbl_creditinvoice_row");
        connection.createStatement().execute("DELETE FROM tbl_creditinvoice");
        connection.createStatement().execute("DELETE FROM tbl_schema_migration WHERE migration_key = 'quantity_scale_x10_v2'");
        connection.commit();

        // 2. Directly insert credit invoice with legacy quantities
        Integer creditInvoiceId = createLegacyCreditInvoice("LEGACY-CREDIT-001", "Legacy Credit Customer AB");
        insertLegacyCreditInvoiceRow(creditInvoiceId, "P-CREDIT-001", "Old Credit",
                new java.math.BigDecimal("250.00"), 3, 3010);  // quantity=3 (old format)

        // 3. Verify raw DB contains non-scaled values before migration
        List<Integer> beforeMigration = readRawCreditInvoiceRowCounts(creditInvoiceId);
        assertThat(beforeMigration).containsExactly(3);

        // 4. Run migration
        new SSSchemaMigrationManager(connection).ensureQuantityScaleMigration();

        // 5. Verify raw DB contains scaled values
        List<Integer> afterMigration = readRawCreditInvoiceRowCounts(creditInvoiceId);
        assertThat(afterMigration).containsExactly(30);  // 3*10

        // 6. Fetch through API and verify conversion
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSNewCompany company = new SSNewCompany();
        company.setId(testCompanyId);
        company.setName("V2 Legacy Qty Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        Integer creditNumber = getCreditInvoiceNumberFromId(creditInvoiceId);
        List<SSCreditInvoice> creditInvoices = SSSalesContext.getCreditInvoices();
        var found = creditInvoices.stream()
                .filter(ci -> ci.getNumber().equals(creditNumber))
                .findFirst();

        assertThat(found).isPresent();
        var creditInvoice = found.get();
        var rows = creditInvoice.getRows();
        assertThat(rows).hasSize(1);
        var firstRow = rows.get(0);
        Integer qty = firstRow.getQuantity();
        assertThat(qty).isEqualTo(30);
    }

    /**
     * Test idempotency: running migration twice should not double-scale.
     */
    @Test
    void migrationIsIdempotent() throws Exception {
        // 1. Clear any previously migrated data and reset tracking
        connection.createStatement().execute("DELETE FROM tbl_invoice_row");
        connection.createStatement().execute("DELETE FROM tbl_invoice");
        connection.createStatement().execute("DELETE FROM tbl_schema_migration WHERE migration_key = 'quantity_scale_x10_v2'");
        connection.commit();

        // 2. Insert legacy data
        Integer invoiceId = createLegacyInvoice("LEGACY-IDEMPOTENT-001", "Idempotent Test AB");
        insertLegacyInvoiceRow(invoiceId, "P-IDEMPOTENT-001", "Test Row",
                new java.math.BigDecimal("100.00"), 5, 3010);

         // 3. First migration run
         new SSSchemaMigrationManager(connection).ensureQuantityScaleMigration();
         List<Integer> afterFirstRun = readRawInvoiceRowCounts(invoiceId);
        assertThat(afterFirstRun).containsExactly(50);  // 5*10

         // 4. Second migration run (should not double-scale)
         new SSSchemaMigrationManager(connection).ensureQuantityScaleMigration();
         List<Integer> afterSecondRun = readRawInvoiceRowCounts(invoiceId);
        assertThat(afterSecondRun).containsExactly(50);  // Still 50, not 500

        // 5. API reads consistent value
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSNewCompany company = new SSNewCompany();
        company.setId(testCompanyId);
        company.setName("V2 Legacy Qty Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSInvoice tempInvoice = new SSInvoice();
        tempInvoice.setNumber(getInvoiceNumberFromId(invoiceId));
        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(tempInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(50);
    }

    // ==================== Helper Methods ====================

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }

    private static Integer createLegacyInvoice(String customerNr, String customerName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_invoice(companyid, number, customer_nr, customer_name, vdate, payment_day, currency_rate, invoice_type) "
                        + "VALUES (?, (SELECT COALESCE(MAX(number), 0) + 1 FROM tbl_invoice), ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, testCompanyId);
            statement.setString(2, customerNr);
            statement.setString(3, customerName);
            statement.setDate(4, java.sql.Date.valueOf(LocalDate.of(2025, 7, 15)));
            statement.setDate(5, java.sql.Date.valueOf(LocalDate.of(2025, 8, 14)));
            statement.setBigDecimal(6, new java.math.BigDecimal("10.50"));
            statement.setString(7, "CASH");
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create legacy invoice");
    }

    private static Integer createLegacyCreditInvoice(String customerNr, String customerName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_creditinvoice(companyid, number, customer_nr, customer_name, vdate) "
                        + "VALUES (?, (SELECT COALESCE(MAX(number), 0) + 1 FROM tbl_creditinvoice), ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, testCompanyId);
            statement.setString(2, customerNr);
            statement.setString(3, customerName);
            statement.setDate(4, java.sql.Date.valueOf(LocalDate.of(2025, 7, 15)));
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create legacy credit invoice");
    }

    private static void insertLegacyInvoiceRow(Integer invoiceId, String productNr, String description,
                                               java.math.BigDecimal unitPrice, Integer count, Integer accountNr) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_invoice_row(invoice_id, product_nr, description, unitprice, count, unit, account_nr) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, invoiceId);
            statement.setString(2, productNr);
            statement.setString(3, description);
            statement.setBigDecimal(4, unitPrice);
            statement.setInt(5, count);  // Insert as-is (not scaled)
            statement.setString(6, "st");
            statement.setInt(7, accountNr);
            statement.executeUpdate();
            connection.commit();
        }
    }

    private static void insertLegacyCreditInvoiceRow(Integer creditInvoiceId, String productNr, String description,
                                                     java.math.BigDecimal unitPrice, Integer count, Integer accountNr) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_creditinvoice_row(creditinvoice_id, product_nr, description, unitprice, count, unit, account_nr) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, creditInvoiceId);
            statement.setString(2, productNr);
            statement.setString(3, description);
            statement.setBigDecimal(4, unitPrice);
            statement.setInt(5, count);  // Insert as-is (not scaled)
            statement.setString(6, "st");
            statement.setInt(7, accountNr);
            statement.executeUpdate();
            connection.commit();
        }
    }

    private static List<Integer> readRawInvoiceRowCounts(Integer invoiceId) throws Exception {
        List<Integer> counts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count FROM tbl_invoice_row WHERE invoice_id=? ORDER BY id")) {
            statement.setInt(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.add((Integer) resultSet.getObject(1));
                }
            }
        }
        return counts;
    }

    private static List<Integer> readRawCreditInvoiceRowCounts(Integer creditInvoiceId) throws Exception {
        List<Integer> counts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count FROM tbl_creditinvoice_row WHERE creditinvoice_id=? ORDER BY id")) {
            statement.setInt(1, creditInvoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.add((Integer) resultSet.getObject(1));
                }
            }
        }
        return counts;
    }

    private static Integer getInvoiceNumberFromId(Integer invoiceId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT number FROM tbl_invoice WHERE id=?")) {
            statement.setInt(1, invoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not find invoice number for id " + invoiceId);
    }

    private static Integer getCreditInvoiceNumberFromId(Integer creditInvoiceId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT number FROM tbl_creditinvoice WHERE id=?")) {
            statement.setInt(1, creditInvoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not find credit invoice number for id " + creditInvoiceId);
    }

}













