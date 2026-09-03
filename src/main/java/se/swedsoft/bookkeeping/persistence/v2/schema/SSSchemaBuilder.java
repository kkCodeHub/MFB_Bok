package se.swedsoft.bookkeeping.persistence.v2.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.util.SSUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Handles core schema DDL operations: table creation and trigger lifecycle.
 * <p>
 * Responsibility: Create base tables from SQL resource, manage trigger creation/drop.
 * Does not handle forward-compatibility ensures or data migrations.
 * </p>
 */
public class SSSchemaBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SSSchemaBuilder.class);

    private final Connection connection;

    /**
     * Create a new schema builder for the given connection.
     *
     * @param connection the database connection
     */
    public SSSchemaBuilder(Connection connection) {
        this.connection = connection;
    }

    /**
     * Create base tables from the schema resource (sql/create_tables_v2.sql).
     * <p>
     * This method:
     * 1. Loads the SQL resource
     * 2. Strips line comments
     * 3. Splits and executes each statement separately
     * 4. Falls back for in-memory test databases if CREATE CACHED TABLE is not supported
     * </p>
     *
     * @throws SQLException if a DDL operation fails unexpectedly
     */
    public void createBaseTables() throws SQLException {
        if (connection == null || connection.isClosed()) {
            return;
        }
        createPublicTables();
        createCompanyTables();
    }

    /**
     * Create PUBLIC/shared tables from sql/create_tables_v2_Public.sql.
     *
     * @throws SQLException if a DDL operation fails unexpectedly
     */
    public void createPublicTables() throws SQLException {
        if (connection == null || connection.isClosed()) {
            return;
        }
        setSchema("PUBLIC");
        executeSchemaResource("sql/create_tables_v2_Public.sql");
    }

    /**
     * Create company-scoped tables from sql/create_tables_v2_Company.sql in current schema.
     *
     * @throws SQLException if a DDL operation fails unexpectedly
     */
    public void createCompanyTables() throws SQLException {
        if (connection == null || connection.isClosed()) {
            return;
        }
        executeSchemaResource("sql/create_tables_v2_Company.sql");
    }

    /**
     * Create all database triggers.
     *
     * @throws SQLException if trigger creation fails
     */
    public void createLocalTriggers() throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "CREATE TRIGGER NEWPROJECT  AFTER INSERT ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITPROJECT  AFTER UPDATE ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEPROJECT  AFTER DELETE ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWRESULTUNIT  AFTER INSERT ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITRESULTUNIT  AFTER UPDATE ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETERESULTUNIT  AFTER DELETE ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWPRODUCT  AFTER INSERT ON tbl_product FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITPRODUCT  AFTER UPDATE ON tbl_product FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEPRODUCT  AFTER DELETE ON tbl_product FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWCUSTOMER  AFTER INSERT ON tbl_customer FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITCUSTOMER  AFTER UPDATE ON tbl_customer FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETECUSTOMER  AFTER DELETE ON tbl_customer FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWSUPPLIER  AFTER INSERT ON tbl_supplier FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITSUPPLIER  AFTER UPDATE ON tbl_supplier FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETESUPPLIER  AFTER DELETE ON tbl_supplier FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWVOUCHERTEMPLATE  AFTER INSERT ON tbl_vouchertemplate FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEVOUCHERTEMPLATE  AFTER DELETE ON tbl_vouchertemplate FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWAUTODIST  AFTER INSERT ON tbl_autodist FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITAUTODIST  AFTER UPDATE ON tbl_autodist FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEAUTODIST  AFTER DELETE ON tbl_autodist FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWINPAYMENT  AFTER INSERT ON tbl_inpayment FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITINPAYMENT  AFTER UPDATE ON tbl_inpayment FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEINPAYMENT  AFTER DELETE ON tbl_inpayment FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWTENDER  AFTER INSERT ON tbl_tender FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITTENDER  AFTER UPDATE ON tbl_tender FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETETENDER  AFTER DELETE ON tbl_tender FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWORDER  AFTER INSERT ON tbl_order FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITORDER  AFTER UPDATE ON tbl_order FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEORDER  AFTER DELETE ON tbl_order FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWINVOICE  AFTER INSERT ON tbl_invoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITINVOICE  AFTER UPDATE ON tbl_invoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEINVOICE  AFTER DELETE ON tbl_invoice FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWCREDITINVOICE  AFTER INSERT ON tbl_creditinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITCREDITINVOICE  AFTER UPDATE ON tbl_creditinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETECREDITINVOICE  AFTER DELETE ON tbl_creditinvoice FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWPERIODICINVOICE  AFTER INSERT ON tbl_periodicinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITPERIODICINVOICE  AFTER UPDATE ON tbl_periodicinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEPERIODICINVOICE  AFTER DELETE ON tbl_periodicinvoice FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWOUTPAYMENT  AFTER INSERT ON tbl_outpayment FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITOUTPAYMENT  AFTER UPDATE ON tbl_outpayment FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEOUTPAYMENT  AFTER DELETE ON tbl_outpayment FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWPURCHASEORDER  AFTER INSERT ON tbl_purchaseorder FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITPURCHASEORDER  AFTER UPDATE ON tbl_purchaseorder FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEPURCHASEORDER  AFTER DELETE ON tbl_purchaseorder FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWSUPPLIERINVOICE  AFTER INSERT ON tbl_supplierinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITSUPPLIERINVOICE  AFTER UPDATE ON tbl_supplierinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETESUPPLIERINVOICE  AFTER DELETE ON tbl_supplierinvoice FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWSUPPLIERCREDITINVOICE  AFTER INSERT ON tbl_suppliercreditinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITSUPPLIERCREDITINVOICE  AFTER UPDATE ON tbl_suppliercreditinvoice FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETESUPPLIERCREDITINVOICE  AFTER DELETE ON tbl_suppliercreditinvoice FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWINVENTORY  AFTER INSERT ON tbl_inventory FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITINVENTORY  AFTER UPDATE ON tbl_inventory FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEINVENTORY  AFTER DELETE ON tbl_inventory FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWINDELIVERY  AFTER INSERT ON tbl_indelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITINDELIVERY  AFTER UPDATE ON tbl_indelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEINDELIVERY  AFTER DELETE ON tbl_indelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWOUTDELIVERY  AFTER INSERT ON tbl_outdelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITOUTDELIVERY  AFTER UPDATE ON tbl_outdelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEOUTDELIVERY  AFTER DELETE ON tbl_outdelivery FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWVOUCHER  AFTER INSERT ON tbl_voucher FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITVOUCHER  AFTER UPDATE ON tbl_voucher FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEVOUCHER  AFTER DELETE ON tbl_voucher FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER NEWOWNREPORT  AFTER INSERT ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER EDITOWNREPORT  AFTER UPDATE ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                        + "CREATE TRIGGER DELETEOWNREPORT  AFTER DELETE ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";")) {

            iStatement.executeUpdate();
            connection.commit();

        } catch (SQLException e) {
            // Triggers may already exist; this is not a fatal error
            LOG.debug("createLocalTriggers encountered: {}", e.getMessage());
        }
    }

    /**
     * Drop all database triggers.
     *
     * @throws SQLException if trigger drop fails
     */
    public void dropTriggers() throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "DROP TRIGGER NEWPROJECT;" + "DROP TRIGGER EDITPROJECT;"
                + "DROP TRIGGER DELETEPROJECT;" + "DROP TRIGGER NEWRESULTUNIT;"
                + "DROP TRIGGER EDITRESULTUNIT;" + "DROP TRIGGER DELETERESULTUNIT;"
                + "DROP TRIGGER NEWPRODUCT;" + "DROP TRIGGER EDITPRODUCT;"
                + "DROP TRIGGER DELETEPRODUCT;" + "DROP TRIGGER NEWCUSTOMER;"
                + "DROP TRIGGER EDITCUSTOMER;" + "DROP TRIGGER DELETECUSTOMER;"
                + "DROP TRIGGER NEWSUPPLIER;" + "DROP TRIGGER EDITSUPPLIER;"
                + "DROP TRIGGER DELETESUPPLIER;" + "DROP TRIGGER NEWVOUCHERTEMPLATE;"
                + "DROP TRIGGER DELETEVOUCHERTEMPLATE;" + "DROP TRIGGER NEWAUTODIST;"
                + "DROP TRIGGER EDITAUTODIST;" + "DROP TRIGGER DELETEAUTODIST;"
                + "DROP TRIGGER NEWINPAYMENT;" + "DROP TRIGGER EDITINPAYMENT;"
                + "DROP TRIGGER DELETEINPAYMENT;" + "DROP TRIGGER NEWTENDER;"
                + "DROP TRIGGER EDITTENDER;" + "DROP TRIGGER DELETETENDER;"
                + "DROP TRIGGER NEWORDER;" + "DROP TRIGGER EDITORDER;"
                + "DROP TRIGGER DELETEORDER;" + "DROP TRIGGER NEWINVOICE;"
                + "DROP TRIGGER EDITINVOICE;" + "DROP TRIGGER DELETEINVOICE;"
                + "DROP TRIGGER NEWCREDITINVOICE;" + "DROP TRIGGER EDITCREDITINVOICE;"
                + "DROP TRIGGER DELETECREDITINVOICE;"
                + "DROP TRIGGER NEWPERIODICINVOICE;"
                + "DROP TRIGGER EDITPERIODICINVOICE;"
                + "DROP TRIGGER DELETEPERIODICINVOICE;"
                + "DROP TRIGGER NEWOUTPAYMENT;" + "DROP TRIGGER EDITOUTPAYMENT;"
                + "DROP TRIGGER DELETEOUTPAYMENT;" + "DROP TRIGGER NEWPURCHASEORDER;"
                + "DROP TRIGGER EDITPURCHASEORDER;"
                + "DROP TRIGGER DELETEPURCHASEORDER;"
                + "DROP TRIGGER NEWSUPPLIERINVOICE;"
                + "DROP TRIGGER EDITSUPPLIERINVOICE;"
                + "DROP TRIGGER DELETESUPPLIERINVOICE;"
                + "DROP TRIGGER NEWSUPPLIERCREDITINVOICE;"
                + "DROP TRIGGER EDITSUPPLIERCREDITINVOICE;"
                + "DROP TRIGGER DELETESUPPLIERCREDITINVOICE;"
                + "DROP TRIGGER NEWINVENTORY;" + "DROP TRIGGER EDITINVENTORY;"
                + "DROP TRIGGER DELETEINVENTORY;" + "DROP TRIGGER NEWINDELIVERY;"
                + "DROP TRIGGER EDITINDELIVERY;" + "DROP TRIGGER DELETEINDELIVERY;"
                + "DROP TRIGGER NEWOUTDELIVERY;" + "DROP TRIGGER EDITOUTDELIVERY;"
                + "DROP TRIGGER DELETEOUTDELIVERY;" + "DROP TRIGGER NEWVOUCHER;"
                + "DROP TRIGGER EDITVOUCHER;" + "DROP TRIGGER DELETEVOUCHER;"
                + "DROP TRIGGER NEWOWNREPORT;" + "DROP TRIGGER EDITOWNREPORT;"
                + "DROP TRIGGER DELETEOWNREPORT;")) {

            iStatement.executeUpdate();
            connection.commit();

        } catch (SQLException e) {
            // Triggers may not exist; this is not a fatal error
            LOG.debug("dropTriggers encountered: {}", e.getMessage());
        }
    }

    private void executeSchemaResource(String schemaResource) throws SQLException {
        String q = SSUtil.readResourceToString(schemaResource);
        LOG.info("createBaseTables using schema resource: {}", schemaResource);

        StringBuilder scriptBuilder = new StringBuilder();
        for (String line : q.split("\\r?\\n")) {
            if (!line.trim().startsWith("--")) {
                scriptBuilder.append(line).append('\n');
            }
        }
        String script = scriptBuilder.toString();
        String[] statements = script.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try (PreparedStatement ps = connection.prepareStatement(trimmed)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (trimmed.toUpperCase().contains("CREATE CACHED TABLE")) {
                    String fallback = trimmed.replaceFirst("(?i)CREATE CACHED TABLE",
                            "CREATE TABLE IF NOT EXISTS");
                    try (PreparedStatement ps2 = connection.prepareStatement(fallback)) {
                        ps2.executeUpdate();
                    } catch (SQLException ignored) {
                        LOG.warn("createBaseTables fallback failed: {}", ignored.getMessage());
                    }
                } else {
                    LOG.warn("createBaseTables skipping statement from {}: {}", schemaResource,
                            e.getMessage());
                }
            }
        }
    }

    private void setSchema(String schemaName) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement("SET SCHEMA " + schemaName)) {
            iStatement.executeUpdate();
        }
    }
}
