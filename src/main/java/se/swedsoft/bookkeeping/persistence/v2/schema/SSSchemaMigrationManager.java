package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manages schema migration tracking and idempotent data migrations.
 * <p>
 * Responsibility: Track applied migrations, execute incremental schema changes,
 * ensure migrations can be run multiple times safely.
 * </p>
 */
public class SSSchemaMigrationManager {

    private static final Logger LOG = LoggerFactory.getLogger(SSSchemaMigrationManager.class);

    private final Connection connection;

    /**
     * Create a new migration manager for the given connection.
     *
     * @param connection the database connection
     */
    public SSSchemaMigrationManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Ensure the migration tracking table exists.
     * <p>
     * This table records which migrations have been applied, using a migration key
     * as the primary key and an applied timestamp.
     * </p>
     *
     * @throws SQLException if table creation fails
     */
    public void ensureSchemaMigrationTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS tbl_schema_migration ("
                + "migration_key VARCHAR(128) PRIMARY KEY,"
                + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        try (PreparedStatement iStatement = connection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }
    }

    /**
     * Check if a specific migration has already been applied.
     *
     * @param migrationKey the unique migration identifier
     * @return true if the migration has been applied, false otherwise
     * @throws SQLException if the query fails
     */
    public boolean isMigrationApplied(String migrationKey) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT 1 FROM tbl_schema_migration WHERE migration_key=?")) {
            iStatement.setString(1, migrationKey);
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    /**
     * Mark a migration as applied.
     * <p>
     * This idempotently records that a migration has been executed.
     * </p>
     *
     * @param migrationKey the unique migration identifier
     * @throws SQLException if the insert fails
     */
    public void markMigrationApplied(String migrationKey) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "INSERT INTO tbl_schema_migration(migration_key) VALUES (?)")) {
            iStatement.setString(1, migrationKey);
            iStatement.executeUpdate();
        }
    }

    /**
     * Execute the quantity scale migration (x10) if not already applied.
     * <p>
     * Scales all quantity-related columns by a factor of 10 across multiple tables.
     * Uses migration tracking to ensure idempotency.
     * </p>
     *
     * @throws SQLException if a migration step fails
     */
    public void ensureQuantityScaleMigration() throws SQLException {
        final String migrationKey = "quantity_scale_x10_v2";

        ensureSchemaMigrationTable();

        if (isMigrationApplied(migrationKey)) {
            return;
        }

        scaleIntegerColumnByTen("tbl_invoice_row", "count");
        scaleIntegerColumnByTen("tbl_creditinvoice_row", "count");
        scaleIntegerColumnByTen("tbl_periodicinvoice_row", "count");
        scaleIntegerColumnByTen("tbl_order_row", "count");
        scaleIntegerColumnByTen("tbl_tender_row", "count");
        scaleIntegerColumnByTen("tbl_purchaseorder_row", "quantity");
        scaleIntegerColumnByTen("tbl_supplierinvoice_row", "quantity");
        scaleIntegerColumnByTen("tbl_suppliercreditinvoice_row", "quantity");
        scaleIntegerColumnByTen("tbl_inventory_row", "quantity");
        scaleIntegerColumnByTen("tbl_inventory_row", "change_qty");
        scaleIntegerColumnByTen("tbl_indelivery_row", "change_qty");
        scaleIntegerColumnByTen("tbl_outdelivery_row", "change_qty");

        markMigrationApplied(migrationKey);
    }

    /**
     * Scale an integer column by a factor of 10 (multiply each value by 10).
     * <p>
     * This is an idempotent operation: if the column does not exist, it is skipped.
     * </p>
     *
     * @param tableName the table name
     * @param columnName the column name
     * @throws SQLException if the update fails
     */
    private void scaleIntegerColumnByTen(String tableName, String columnName) throws SQLException {
        SSSchemaEnsurer ensurer = new SSSchemaEnsurer(connection);
        if (!ensurer.columnExists(tableName, columnName)) {
            return;
        }

        String sql = "UPDATE " + tableName + " SET " + columnName + "=" + columnName + "*10 WHERE "
                + columnName + " IS NOT NULL";
        try (PreparedStatement iStatement = connection.prepareStatement(sql)) {
            iStatement.executeUpdate();
        }
    }
}

