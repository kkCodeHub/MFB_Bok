package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.system.SSDBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles forward-compatibility ensure operations and schema helper methods.
 * <p>
 * Responsibility: Column/constraint existence checks, idempotent schema adjustments
 * to support new columns and tables added over time without breaking existing databases.
 * </p>
 */
public class SSSchemaEnsurer {

    private static final Logger LOG = LoggerFactory.getLogger(SSSchemaEnsurer.class);

    private final Connection connection;
    private final String targetSchema;

    /**
     * Create a new schema ensurer for the given connection.
     *
     * @param connection the database connection
     */
    public SSSchemaEnsurer(Connection connection) {
        this(connection, null);
    }

    /**
     * Create a new schema ensurer for the given connection and target schema.
     *
     * @param connection the database connection
     * @param targetSchema schema to run ensure operations against, or null to use current schema
     */
    public SSSchemaEnsurer(Connection connection, String targetSchema) {
        this.connection = connection;
        this.targetSchema = targetSchema;
    }

    /**
     * Ensure all forward-compatibility columns and tables exist.
     * <p>
     * This method runs a sequence of ensure operations to add columns and tables
     * that have been introduced in newer schema versions.
     * </p>
     *
     * @throws SQLException if any ensure operation fails unexpectedly
     */
    public void ensureAllForwardCompatibility() throws SQLException {
        applyTargetSchema();
        ensureAccountingYearSnapshotColumns();
        ensureCompanyMailServerColumns();
        ensureProductQuantityColumns();
        ensureProductParcelRowsTable();
        ensureTemplateAccountTable();
        ensureAccountPlanColumns();
        ensureYearOwnedAccountTable();
        ensureSingleActiveAccountYear();
        ensureDropYearPlanFk();
    }

    /**
     * Ensure accounting year snapshot-related columns exist.
     *
     * @throws SQLException if column addition fails
     */
    private void ensureAccountingYearSnapshotColumns() throws SQLException {
        ensureColumnExists("tbl_accountingyear", "accountplan", "CLOB");
        ensureColumnExists("tbl_accountingyear", "accountplan_schema_version", "INTEGER DEFAULT 1");
        ensureColumnExists("tbl_accountingyear", "accountplan_compression_flag", "VARCHAR(10) DEFAULT 'gzip'");
        ensureColumnExists("tbl_accountingyear", "accountplan_checksum", "VARCHAR(64)");
        ensureColumnExists("tbl_accountingyear", "accountplan_snapshot_version", "INTEGER DEFAULT 0");
        ensureColumnExists("tbl_accountingyear", "accountplan_updated_at", "TIMESTAMP");
        ensureColumnExists("tbl_accountingyear", "accountplan_updated_by", "VARCHAR(100)");
        ensureColumnExists("tbl_accountingyear", "accountplan_name", "VARCHAR(256)");
        ensureColumnExists("tbl_accountingyear", "accountplan_dirty_flag", "BOOLEAN DEFAULT FALSE");
    }

    /**
     * Ensure account plan template metadata exists and clear legacy template rows.
     *
     * @throws SQLException if column addition fails
     */
    private void ensureAccountPlanColumns() throws SQLException {
        ensureColumnExists("tbl_accountplan", "excel_path", "VARCHAR(512)");
        ensureColumnExists("tbl_accountplan", "is_default", "BOOLEAN DEFAULT FALSE");

        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE tbl_accountplan SET excel_path=COALESCE(excel_path, name || '.xlsx') WHERE excel_path IS NULL OR excel_path=''")) {
            update.executeUpdate();
        }

        String[] defaults = new String[]{
                "Bas2026 K2-AB&EF Full.xlsx",
                "Bas2006(07)-AB & EF.xlsx",
                "Bas2006(07)-Enskild-naringsidkare.xlsx",
                "Bas2006(07)-HB & KB.xlsx",
                "Bas2007(K1)-Enskild-naringsidkare.xlsx"};

        for (String defaultName : defaults) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE tbl_accountplan SET excel_path=?, is_default=TRUE WHERE name=?")) {
                update.setObject(1, "account/default/" + defaultName);
                update.setObject(2, defaultName.substring(0, defaultName.lastIndexOf('.')));
                update.executeUpdate();
            }
        }

        try (PreparedStatement clear = connection.prepareStatement("DELETE FROM tbl_accountplan_account")) {
            clear.executeUpdate();
        }

        connection.commit();
    }
    /**
     * Ensure company mail server configuration columns exist.
     *
     * @throws SQLException if column addition fails
     */
    private void ensureCompanyMailServerColumns() throws SQLException {
        ensureColumnExists("tbl_company", "smtp_name", "VARCHAR(255)");
        ensureColumnExists("tbl_company", "smtp_port", "INTEGER");
        ensureColumnExists("tbl_company", "smtp_bcc_addresses", "VARCHAR(1000)");
        ensureColumnExists("tbl_company", "smtp_auth", "BOOLEAN DEFAULT FALSE");
        ensureColumnExists("tbl_company", "smtp_connection_security", "VARCHAR(20)");
        ensureColumnExists("tbl_company", "smtp_username", "VARCHAR(255)");
        ensureColumnExists("tbl_company", "smtp_password", "VARCHAR(1024)");
        ensureColumnExists("tbl_company", "swish_image", "VARCHAR(500)");
        ensureColumnExists("tbl_company", "swish_text", "VARCHAR(500)");
    }

    /**
     * Ensure product quantity constraint columns exist.
     *
     * @throws SQLException if column addition fails
     */
    private void ensureProductQuantityColumns() throws SQLException {
        ensureColumnExists("tbl_product", "only_whole_quantity", "BOOLEAN DEFAULT FALSE");
    }

    /**
     * Ensure product parcel rows table exists.
     *
     * @throws SQLException if table creation fails
     */
    private void ensureProductParcelRowsTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS tbl_product_row ("
                + "product_id INTEGER NOT NULL,"
                + "row_index INTEGER NOT NULL,"
                + "product_nr VARCHAR(50),"
                + "description VARCHAR(500),"
                + "quantity INTEGER,"
                + "CONSTRAINT pk_product_row PRIMARY KEY (product_id, row_index),"
                + "CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES tbl_product(id) ON DELETE CASCADE"
                + ")";
        try (PreparedStatement iStatement = connection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }
    }

    /**
     * Ensure template account table exists and migrate data from legacy account format if needed.
     *
     * @throws SQLException if table creation or data migration fails
     */
    private void ensureTemplateAccountTable() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS tbl_accountplan_account ("
                + "id INTEGER IDENTITY,"
                + "accountplan_id INTEGER NOT NULL,"
                + "number INTEGER NOT NULL,"
                + "description VARCHAR(255),"
                + "sru_code VARCHAR(20),"
                + "vat_code VARCHAR(20),"
                + "report_code VARCHAR(20),"
                + "active BOOLEAN DEFAULT TRUE,"
                + "project_required BOOLEAN DEFAULT FALSE,"
                + "result_unit_required BOOLEAN DEFAULT FALSE,"
                + "CONSTRAINT pk_accountplan_account PRIMARY KEY (id),"
                + "CONSTRAINT fk_accountplan_account_plan FOREIGN KEY (accountplan_id) REFERENCES tbl_accountplan(id) ON DELETE CASCADE"
                + ")";
        try (PreparedStatement iStatement = connection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }
    }

    /**
     * Ensure year-owned account table structure and constraints are present.
     * <p>
     * This transitions from a template plan model to a year-owned snapshot model.
     * </p>
     *
     * @throws SQLException if DDL operations fail
     */
    private void ensureYearOwnedAccountTable() throws SQLException {
        ensureColumnExists("tbl_account", "accountingyear_id", "INTEGER");

        dropConstraintIfExists("tbl_account", "fk_account_plan");
        dropConstraintIfExists("tbl_account", "fk_account_year");

        if (columnExists("tbl_account", "accountplan_id")) {
            try (PreparedStatement iDropColumn = connection.prepareStatement(
                    "ALTER TABLE tbl_account DROP COLUMN accountplan_id")) {
                iDropColumn.executeUpdate();
            } catch (SQLException e) {
                LOG.warn("Could not drop tbl_account.accountplan_id: {}", e.getMessage());
            }
        }

        try (PreparedStatement iAddFk = connection.prepareStatement(
                "ALTER TABLE tbl_account ADD CONSTRAINT fk_account_year "
                        + "FOREIGN KEY (accountingyear_id) REFERENCES tbl_accountingyear(id) ON DELETE CASCADE")) {
            iAddFk.executeUpdate();
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.contains("already exists") && !msg.contains("duplicate")
                    && !msg.contains("integrity constraint")) {
                throw e;
            }
        }

        try (PreparedStatement iUnique = connection.prepareStatement(
                "ALTER TABLE tbl_account ADD CONSTRAINT uq_account_year_number "
                        + "UNIQUE (accountingyear_id, number)")) {
            iUnique.executeUpdate();
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.contains("already exists") && !msg.contains("duplicate")
                    && !msg.contains("integrity constraint")) {
                throw e;
            }
        }
    }

    /**
     * Ensure only one accounting year is active (used during schema migration).
     * <p>
     * If multiple years are detected in tbl_account, keeps the configured year
     * (or the first one) and removes accounts from other years.
     * </p>
     *
     * @throws SQLException if the operation fails
     */
    private void ensureSingleActiveAccountYear() throws SQLException {
        List<Integer> iYearIds = new ArrayList<>();
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT DISTINCT accountingyear_id FROM tbl_account WHERE accountingyear_id IS NOT NULL ORDER BY accountingyear_id");
             ResultSet iResultSet = iStatement.executeQuery()) {
            while (iResultSet.next()) {
                iYearIds.add((Integer) iResultSet.getObject(1));
            }
        }

        if (iYearIds.size() <= 1) {
            return;
        }

        Integer iKeepYearId = SSDBConfig.getYearId();
        if (iKeepYearId == null || !iYearIds.contains(iKeepYearId)) {
            iKeepYearId = iYearIds.get(0);
        }

        try (PreparedStatement iDelete = connection.prepareStatement(
                "DELETE FROM tbl_account WHERE accountingyear_id<>?")) {
            iDelete.setObject(1, iKeepYearId);
            int iDeletedRows = iDelete.executeUpdate();
            if (iDeletedRows > 0) {
                LOG.warn("tbl_account contained multiple accountingyear_id values {}; kept {}, removed {} rows",
                        iYearIds, iKeepYearId, iDeletedRows);
            }
        }
    }

    /**
     * Ensure the foreign key from tbl_accountingyear to tbl_accountplan is dropped.
     * <p>
     * In the snapshot model, the year owns a self-contained CLOB copy of its account plan.
     * The FK to tbl_accountplan must therefore not exist; years must be readable
     * even after a template plan has been deleted. This method is idempotent.
     * </p>
     *
     * @throws SQLException if the DDL operation fails with an unexpected error
     */
    private void ensureDropYearPlanFk() throws SQLException {
        try (PreparedStatement iDrop = connection.prepareStatement(
                "ALTER TABLE tbl_accountingyear DROP CONSTRAINT fk_year_plan")) {
            iDrop.executeUpdate();
            connection.commit();
            LOG.info("Dropped fk_year_plan from tbl_accountingyear (snapshot model migration)");
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (msg.contains("not found") || msg.contains("does not exist")
                    || msg.contains("no constraint") || msg.contains("cannot find")) {
                // Already absent – nothing to do.
                return;
            }
            LOG.warn("Could not drop fk_year_plan (unexpected): {}", e.getMessage());
        }
    }

    /**
     * Ensure a column exists in a table, adding it if necessary.
     * <p>
     * This is an idempotent operation: if the column already exists, nothing happens.
     * </p>
     *
     * @param tableName the table name
     * @param columnName the column name
     * @param columnDefinition the column definition (e.g., "VARCHAR(255)", "INTEGER DEFAULT 0")
     * @throws SQLException if column addition fails with an unexpected error
     */
    public void ensureColumnExists(String tableName, String columnName, String columnDefinition) throws SQLException {
        String ddl = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
        try (PreparedStatement iStatement = connection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
            LOG.info("Added missing column {}.{}", tableName, columnName);
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("duplicate") || message.contains("already exists")) {
                return;
            }
            throw e;
        }
    }

    /**
     * Check if a column exists in a table.
     *
     * @param tableName the table name
     * @param columnName the column name
     * @return true if the column exists, false otherwise
     */
    public boolean columnExists(String tableName, String columnName) {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?"
                + (targetSchema != null ? " AND UPPER(TABLE_SCHEMA)=?" : "");
        try (PreparedStatement iStatement = connection.prepareStatement(sql)) {
            iStatement.setString(1, tableName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, columnName.toUpperCase(Locale.ROOT));
            if (targetSchema != null) {
                iStatement.setString(3, targetSchema.toUpperCase(Locale.ROOT));
            }
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        } catch (SQLException e) {
            LOG.warn("Could not inspect column {}.{}: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    /**
     * Drop a constraint if it exists.
     * <p>
     * This is an idempotent operation: if the constraint does not exist,
     * it is silently ignored.
     * </p>
     *
     * @param tableName the table name
     * @param constraintName the constraint name
     * @throws SQLException if the drop fails with an unexpected error
     */
    public void dropConstraintIfExists(String tableName, String constraintName) throws SQLException {
        try (PreparedStatement iDrop = connection.prepareStatement(
                "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName)) {
            iDrop.executeUpdate();

        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.contains("not found") && !msg.contains("does not exist")
                    && !msg.contains("no constraint") && !msg.contains("cannot find")) {
                throw e;
            }
        }
    }

    private void applyTargetSchema() throws SQLException {
        if (targetSchema == null || targetSchema.trim().isEmpty()) {
            return;
        }
        try (PreparedStatement iStatement = connection.prepareStatement("SET SCHEMA " + targetSchema)) {
            iStatement.executeUpdate();
        }
    }
}



