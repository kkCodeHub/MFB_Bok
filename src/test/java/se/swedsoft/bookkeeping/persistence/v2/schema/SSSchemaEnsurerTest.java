package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SSSchemaEnsurer - forward-compatibility column and constraint operations.
 */
class SSSchemaEnsurerTest {

    private Connection connection;
    private SSSchemaEnsurer ensurer;
    private SSSchemaBuilder builder;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:test_schema_ensurer", "sa", "");
        connection.setAutoCommit(false);

        builder = new SSSchemaBuilder(connection);
        builder.createBaseTables();
        connection.commit();

        ensurer = new SSSchemaEnsurer(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.rollback();
            connection.close();
        }
        System.clearProperty("fribok.schema.version");
    }

    @Test
    void testColumnExists_ReturnsTrueForExistingColumn() throws SQLException {
        // Act & Assert
        assertTrue(ensurer.columnExists("tbl_company", "id"));
        assertTrue(ensurer.columnExists("tbl_company", "name"));
    }

    @Test
    void testColumnExists_ReturnsFalseForNonExistentColumn() throws SQLException {
        // Act & Assert
        assertFalse(ensurer.columnExists("tbl_company", "nonexistent_column"));
    }

    @Test
    void testEnsureColumnExists_AddsNewColumn() throws SQLException {
        // Act
        ensurer.ensureColumnExists("tbl_company", "test_column", "VARCHAR(100)");
        connection.commit();

        // Assert
        assertTrue(ensurer.columnExists("tbl_company", "test_column"));
    }

    @Test
    void testEnsureColumnExists_Idempotent() throws SQLException {
        // Act: Add column twice
        ensurer.ensureColumnExists("tbl_company", "test_column_2", "VARCHAR(100)");
        connection.commit();

        ensurer.ensureColumnExists("tbl_company", "test_column_2", "VARCHAR(100)");
        connection.commit();

        // Assert: Column should exist, no error thrown
        assertTrue(ensurer.columnExists("tbl_company", "test_column_2"));
    }

    @Test
    void testEnsureAllForwardCompatibility_SuccessfulExecution() throws SQLException {
        // Act
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: Verify snapshot columns exist
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_schema_version"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_checksum"));

        // Verify mail server columns exist
        assertTrue(ensurer.columnExists("tbl_company", "smtp_name"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_port"));

        // Verify product quantity column exists
        assertTrue(ensurer.columnExists("tbl_product", "only_whole_quantity"));
    }

    @Test
    void testEnsureAllForwardCompatibility_Idempotent() throws SQLException {
        // Act: Run twice
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: All columns should still exist, no duplicates or errors
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_name"));
        assertTrue(ensurer.columnExists("tbl_product", "only_whole_quantity"));
    }

    @Test
    void testEnsureAccountingYearSnapshotColumns() throws SQLException {
        // Act
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: All snapshot-related columns should exist
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_schema_version"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_compression_flag"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_checksum"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_snapshot_version"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_updated_at"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_updated_by"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_name"));
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan_dirty_flag"));
    }

    @Test
    void testEnsureCompanyMailServerColumns() throws SQLException {
        // Act
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: All mail server columns should exist
        assertTrue(ensurer.columnExists("tbl_company", "smtp_name"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_port"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_bcc_addresses"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_auth"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_connection_security"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_username"));
        assertTrue(ensurer.columnExists("tbl_company", "smtp_password"));
    }

    @Test
    void testEnsureProductParcelRowsTable() throws SQLException {
        // Act
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: Product rows table should exist
        assertTrue(tableExists("tbl_product_row"));
    }

    @Test
    void testEnsureTemplateAccountTable() throws SQLException {
        // Act
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: Template account table should exist
        assertTrue(tableExists("tbl_accountplan_account"));
    }

    @Test
    void testDropConstraintIfExists_RemovesConstraint() throws SQLException {
        // Arrange: Create a test constraint
        try (PreparedStatement ps = connection.prepareStatement(
                "ALTER TABLE tbl_account ADD CONSTRAINT test_constraint PRIMARY KEY (id)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // Constraint might already exist, that's okay
        }
        connection.commit();

        // Act
        ensurer.dropConstraintIfExists("tbl_account", "test_constraint");
        connection.commit();

        // Assert: Constraint should be gone
        assertFalse(constraintExists("tbl_account", "test_constraint"));
    }

    @Test
    void testDropConstraintIfExists_Idempotent() throws SQLException {
        // Act: Drop non-existent constraint twice (should not fail)
        assertDoesNotThrow(() -> {
            ensurer.dropConstraintIfExists("tbl_account", "nonexistent_constraint");
            connection.commit();
        });

        assertDoesNotThrow(() -> {
            ensurer.dropConstraintIfExists("tbl_account", "nonexistent_constraint");
            connection.commit();
        });
    }

    @Test
    void testFullForwardCompatibilityMigration() throws SQLException {
        // Act: Simulate a legacy database and apply full forward compatibility
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Assert: Verify the database is now compatible
        // Snapshot columns
        assertTrue(ensurer.columnExists("tbl_accountingyear", "accountplan"));
        // Mail server columns
        assertTrue(ensurer.columnExists("tbl_company", "smtp_name"));
        // Product quantity column
        assertTrue(ensurer.columnExists("tbl_product", "only_whole_quantity"));
        // Product rows table
        assertTrue(tableExists("tbl_product_row"));
        // Account plan table
        assertTrue(tableExists("tbl_accountplan_account"));
    }

    @Test
    void testBackwardCompatibility_ExistingDatabaseUnmodified() throws SQLException {
        // Arrange
        ensurer.ensureAllForwardCompatibility();
        connection.commit();

        // Act: Run again to ensure idempotency
        int columnCountBefore = countColumns("tbl_company");
        ensurer.ensureAllForwardCompatibility();
        connection.commit();
        int columnCountAfter = countColumns("tbl_company");

        // Assert: Column count should not increase on second run
        assertEquals(columnCountBefore, columnCountAfter);
    }

    // Helper methods

    private boolean tableExists(String tableName) throws SQLException {
        String query = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean constraintExists(String tableName, String constraintName) throws SQLException {
        String query = "SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE UPPER(TABLE_NAME) = ? AND UPPER(CONSTRAINT_NAME) = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, tableName.toUpperCase());
            ps.setString(2, constraintName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int countColumns(String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, tableName.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
}

