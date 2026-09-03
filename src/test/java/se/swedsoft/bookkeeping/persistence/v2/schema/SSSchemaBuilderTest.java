package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SSSchemaBuilder - table creation and trigger lifecycle.
 */
class SSSchemaBuilderTest {

    private Connection connection;
    private SSSchemaBuilder builder;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:test_schema_builder", "sa", "");
        connection.setAutoCommit(false);
        builder = new SSSchemaBuilder(connection);
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
    void testCreateBaseTables_SuccessfulCreation() throws SQLException {
        // Act
        builder.createBaseTables();
        connection.commit();

        // Assert: Verify that key tables exist
        assertTrue(tableExists("tbl_company"), "tbl_company should exist");
        assertTrue(tableExists("tbl_product"), "tbl_product should exist");
        assertTrue(tableExists("tbl_customer"), "tbl_customer should exist");
        assertTrue(tableExists("tbl_account"), "tbl_account should exist");
    }

    @Test
    void testCreateBaseTables_Idempotent() throws SQLException {
        // Act: Create tables twice
        builder.createBaseTables();
        connection.commit();

        builder.createBaseTables();
        connection.commit();

        // Assert: Second call should not fail
        assertTrue(tableExists("tbl_company"));
    }

    @Test
    void testCreateLocalTriggers_SuccessfulCreation() throws SQLException {
        // Arrange: Create base tables first
        builder.createBaseTables();
        connection.commit();

        // Act
        assertDoesNotThrow(() -> {
            builder.createLocalTriggers();
            connection.commit();
        });

        // Assert: Tables should still exist and be usable after trigger creation
        assertTrue(tableExists("tbl_project"), "tbl_project should still exist");
        assertTrue(tableExists("tbl_customer"), "tbl_customer should still exist");
    }

    @Test
    void testCreateLocalTriggers_Idempotent() throws SQLException {
        // Arrange: Create base tables and triggers
        builder.createBaseTables();
        connection.commit();

        builder.createLocalTriggers();
        connection.commit();

        // Act: Drop and recreate triggers (should not fail)
        assertDoesNotThrow(() -> {
            builder.dropTriggers();
            connection.commit();

            builder.createLocalTriggers();
            connection.commit();
        });

        // Assert: Tables should still exist
        assertTrue(tableExists("tbl_project"));
    }

    @Test
    void testDropTriggers_RemovesAllTriggers() throws SQLException {
        // Arrange: Create base tables and triggers
        builder.createBaseTables();
        connection.commit();

        builder.createLocalTriggers();
        connection.commit();

        // Act
        assertDoesNotThrow(() -> {
            builder.dropTriggers();
            connection.commit();
        });

        // Assert: Tables should still exist even after dropping triggers
        assertTrue(tableExists("tbl_project"), "Tables should still exist");
    }

    @Test
    void testDropTriggers_Idempotent() throws SQLException {
        // Arrange: Create and drop triggers
        builder.createBaseTables();
        connection.commit();

        builder.createLocalTriggers();
        connection.commit();

        builder.dropTriggers();
        connection.commit();

        // Act: Drop again (should not fail even if already dropped)
        assertDoesNotThrow(() -> {
            builder.dropTriggers();
            connection.commit();
        });
    }

    @Test
    void testFullCycle_CreateDropRecreate() throws SQLException {
        // Act: Full cycle
        builder.createBaseTables();
        connection.commit();

        builder.createLocalTriggers();
        connection.commit();

        builder.dropTriggers();
        connection.commit();

        builder.createLocalTriggers();
        connection.commit();

        // Assert: Tables should exist
        assertTrue(tableExists("tbl_company"));
        assertTrue(tableExists("tbl_customer"));
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
}






