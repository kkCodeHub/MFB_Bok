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
 * Tests for SSSchemaMigrationManager - migration tracking and idempotent data migrations.
 */
class SSSchemaMigrationManagerTest {

    private Connection connection;
    private SSSchemaMigrationManager migrationManager;
    private SSSchemaBuilder builder;
    private SSSchemaEnsurer ensurer;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:test_migration_manager", "sa", "");
        connection.setAutoCommit(false);

        builder = new SSSchemaBuilder(connection);
        ensurer = new SSSchemaEnsurer(connection);
        migrationManager = new SSSchemaMigrationManager(connection);

        // Create base schema
        builder.createBaseTables();
        connection.commit();
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
    void testEnsureSchemaMigrationTable_CreatesTable() throws SQLException {
        // Act
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Assert
        assertTrue(tableExists("tbl_schema_migration"));
    }

    @Test
    void testEnsureSchemaMigrationTable_Idempotent() throws SQLException {
        // Act: Create migration table twice
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Assert: Should still exist
        assertTrue(tableExists("tbl_schema_migration"));
    }

    @Test
    void testMarkAndCheckMigration_TrackingWorks() throws SQLException {
        // Arrange
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Act
        assertFalse(migrationManager.isMigrationApplied("test_migration_1"));
        migrationManager.markMigrationApplied("test_migration_1");
        connection.commit();

        // Assert
        assertTrue(migrationManager.isMigrationApplied("test_migration_1"));
    }

    @Test
    void testMigrationTracking_MultipleMigrations() throws SQLException {
        // Arrange
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Act
        migrationManager.markMigrationApplied("migration_a");
        migrationManager.markMigrationApplied("migration_b");
        migrationManager.markMigrationApplied("migration_c");
        connection.commit();

        // Assert: All three should be tracked
        assertTrue(migrationManager.isMigrationApplied("migration_a"));
        assertTrue(migrationManager.isMigrationApplied("migration_b"));
        assertTrue(migrationManager.isMigrationApplied("migration_c"));
        assertFalse(migrationManager.isMigrationApplied("migration_d"));
    }

    @Test
    void testQuantityScaleMigration_CharacteristicsWithoutData() throws SQLException {
        // Arrange: Just migration infrastructure
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Act
        migrationManager.ensureQuantityScaleMigration();
        connection.commit();

        // Assert: Migration should be tracked as applied
        assertTrue(migrationManager.isMigrationApplied("quantity_scale_x10_v2"),
                "Migration should be recorded when rows dont exist");
    }

    @Test
    void testQuantityScaleMigration_SkipsNonExistentColumns() throws SQLException {
        // Arrange: Create table without the column we're trying to scale
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE test_table (id INTEGER)")) {
            ps.executeUpdate();
        }
        connection.commit();

        // Act: Should not fail when column doesn't exist
        assertDoesNotThrow(() -> {
            migrationManager.ensureQuantityScaleMigration();
            connection.commit();
        });
    }

    @Test
    void testMigrationStartupSequence_EmptyDatabase() throws SQLException {
        // Arrange: Connection with base schema only
        builder.createBaseTables();
        connection.commit();

        // Act: Run through full migration sequence
        migrationManager.ensureSchemaMigrationTable();
        assertTrue(tableExists("tbl_schema_migration"));

        migrationManager.ensureQuantityScaleMigration();
        connection.commit();

        // Assert
        assertTrue(migrationManager.isMigrationApplied("quantity_scale_x10_v2"));
    }

    @Test
    void testMigrationStartupSequence_ExistingDatabase() throws SQLException {
        // Arrange: Simulate already-migrated database
        migrationManager.ensureSchemaMigrationTable();
        connection.commit();

        // Pre-mark the migration as applied
        if (!migrationManager.isMigrationApplied("quantity_scale_x10_v2")) {
            migrationManager.markMigrationApplied("quantity_scale_x10_v2");
        }
        connection.commit();

        // Act: Re-run migrations (should be skipped)
        int rowsBefore = countMigrationRows();
        migrationManager.ensureQuantityScaleMigration();
        connection.commit();
        int rowsAfter = countMigrationRows();

        // Assert: Should not add duplicate records
        assertEquals(rowsBefore, rowsAfter, "Duplicate migration records should not be created");
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


    private int countMigrationRows() throws SQLException {
        String query = "SELECT COUNT(*) FROM tbl_schema_migration";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}


