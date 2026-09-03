package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for schema initialization and migration scenarios.
 */
@Tag("integration")
class SSSchemaInitializationIntegrationTest {

    @Test
    void testStartupOnEmptyDatabase() throws Exception {
        String dbUrl = "jdbc:hsqldb:mem:schema_init_empty_" + System.nanoTime();
        Connection connection = SSV2DatabaseFixture.openDatabase(dbUrl);

        try {
            assertTrue(tableExists(connection, "PUBLIC", "tbl_company_catalog"));
            assertTrue(tableExists(connection, "PUBLIC", "tbl_accountplan"));

            String activeSchema = activeSchema(connection);
            assertTrue(tableExists(connection, activeSchema, "tbl_company"));
            assertTrue(tableExists(connection, activeSchema, "tbl_product"));
            assertTrue(tableExists(connection, activeSchema, "tbl_customer"));
            assertTrue(tableExists(connection, activeSchema, "tbl_account"));
            assertTrue(tableExists(connection, activeSchema, "tbl_invoice"));
        } finally {
            SSV2DatabaseFixture.closeDatabase(connection);
        }
    }

    @Test
    void testSchemaInitializationIsIdempotent() throws Exception {
        String dbUrl = "jdbc:hsqldb:mem:schema_init_idempotent_" + System.nanoTime();
        Connection connection = SSV2DatabaseFixture.openDatabase(dbUrl);

        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(connection);
            String activeSchema = activeSchema(connection);

            int companyColumnsBefore = countColumns(connection, activeSchema, "tbl_company");

            builder.createPublicTables();
            setSchema(connection, activeSchema);
            builder.createCompanyTables();
            connection.commit();

            builder.createPublicTables();
            setSchema(connection, activeSchema);
            builder.createCompanyTables();
            connection.commit();

            assertEquals(companyColumnsBefore, countColumns(connection, activeSchema, "tbl_company"));
        } finally {
            SSV2DatabaseFixture.closeDatabase(connection);
        }
    }

    @Test
    void testStartupFailsOnLegacySingleSchemaDatabase() throws Exception {
        String dbUrl = "jdbc:hsqldb:mem:schema_init_legacy_" + System.nanoTime();
        Class.forName("org.hsqldb.jdbcDriver");
        Connection connection = DriverManager.getConnection(dbUrl, "sa", "");
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS PUBLIC.tbl_company(id INTEGER IDENTITY, name VARCHAR(255))")) {
                statement.executeUpdate();
            }
            connection.commit();

            SQLException exception = assertThrows(SQLException.class,
                    () -> SSSystemConfigContext.startupLocal(connection));
            assertTrue(exception.getMessage().contains("Legacy single-schema database detected"));
        } finally {
            connection.close();
            System.clearProperty("fribok.schema.version");
        }
    }

    @Test
    void testStartupFailsWhenRequiredColumnIsMissing() throws Exception {
        String dbUrl = "jdbc:hsqldb:mem:schema_init_missing_column_" + System.nanoTime();
        Connection connection = SSV2DatabaseFixture.openDatabase(dbUrl);

        try {
            String activeSchema = activeSchema(connection);

            try (PreparedStatement statement = connection.prepareStatement(
                    "SET SCHEMA " + quoteIdentifier(activeSchema))) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "ALTER TABLE tbl_company DROP COLUMN name")) {
                statement.executeUpdate();
            }
            connection.commit();

            SQLException exception = assertThrows(SQLException.class,
                    () -> SSSystemConfigContext.startupLocal(connection));
            assertTrue(exception.getMessage().contains("Database schema mismatch"));
        } finally {
            SSV2DatabaseFixture.closeDatabase(connection);
        }
    }

    private String activeSchema(Connection connection) throws Exception {
        String query = "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE "
                + "ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        throw new SQLException("No active schema found");
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws Exception {
        String query = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            statement.setString(2, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private int countColumns(Connection connection, String schemaName, String tableName) throws Exception {
        String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            statement.setString(2, tableName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private void setSchema(Connection connection, String schemaName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SET SCHEMA " + quoteIdentifier(schemaName))) {
            statement.executeUpdate();
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
