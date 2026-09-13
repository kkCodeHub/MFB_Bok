package se.swedsoft.bookkeeping.data.system;


import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.calc.math.*;
import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.common.*;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaBuilder;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanDefaultResourceDiscovery;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanLoader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.data.system.trigger.SSTriggerSchemaService;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSDB {
    private static final Logger LOG = LoggerFactory.getLogger(SSDB.class);

    private static final String SCHEMA_VERSION_PROPERTY = "fribok.schema.version";
    private static final String SCHEMA_V2 = "v2";
    private static final String DEFAULT_SCHEMA_NAME = "PUBLIC";
    private static final String DEMO_SCHEMA_NAME = "co_0";
    private static final String DEMO_COMPANY_NAME = "Demoföretaget";
    private static final String SEED_STATE_TABLE = "PUBLIC.tbl_seed_state";
    private static SSDB cInstance;

    public static final Object iSyncObject = new Object();

    private SSNewCompany iCurrentCompany;
    private SSNewAccountingYear iCurrentYear;

    List<SSProduct> iProducts;
    List<SSCustomer> iCustomers;
    List<SSSupplier> iSuppliers;
    List<SSAutoDist> iAutoDists;

    List<SSInpayment> iInpayments;
    List<SSTender> iTenders;
    List<SSOrder> iOrders;
    List<SSInvoice> iInvoices;
    List<SSCreditInvoice> iCreditInvoices;
    List<SSPeriodicInvoice> iPeriodicInvoices;

    List<SSOutpayment> iOutpayments;
    List<SSPurchaseOrder> iPurchaseOrders;
    List<SSSupplierInvoice> iSupplierInvoices;
    List<SSSupplierCreditInvoice> iSupplierCreditInvoices;

    List<SSInventory> iInventories;
    List<SSIndelivery> iIndeliveries;
    List<SSOutdelivery> iOutdeliveries;

    List<SSVoucher> iVouchers;
    List<SSOwnReport> iOwnReports;

    private final SSDBEventBus iEventBus = new SSDBEventBus();
    private final SSTriggerSchemaService iTriggerSchemaService;
    private final SSSeedService iSeedService;

    private Connection iConnection;

    public static SSDB getInstance() {
        if (cInstance == null) {
            cInstance = new SSDB();
        }
        return cInstance;
    }

    private SSDB() {
        iTriggerSchemaService = new SSTriggerSchemaService(() -> iConnection);
        iSeedService = new SSSeedService();
    }

    public void startupLocal(Connection pConnection) throws SQLException {
 //       LOG.info("KK startupLocal, SSDB.java");
        prepareStartupConnection(pConnection);
        failIfLegacySingleSchemaDatabase();
        createNewTables();
        Repositories.init(this);
        checkImportDefaultAccountPlans();
        ensureCatalogBootstrapForCurrentSchema();
        iSeedService.runDemoSeedIfNeeded(iConnection, schemaName -> {
            try {
                setSchema(schemaName);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to set schema " + schemaName, e);
            }
        }, this::setCurrentCompany);
        validateSchemaContract();
        iTriggerSchemaService.rebuildLocalTriggers();
        initializeCurrentCompanyAndYear();
        logStartupV2Mode();
    }

    void prepareStartupConnection(Connection pConnection) throws SQLException {
        iConnection = pConnection;
        iConnection.setAutoCommit(false);

        // SSDB is a singleton across test classes and app lifecycle; clear any
        // company/year state tied to a previous DB connection.
        iCurrentCompany = null;
        iCurrentYear = null;
        clearCachedLists();

        System.setProperty(SCHEMA_VERSION_PROPERTY, SCHEMA_V2);
    }

    public void initializeCurrentCompanyAndYear() {
        Repositories.companies().applyActiveCompanySchema();
        Optional<String> iActiveCatalogCompanyName = Repositories.companies().findActiveCompanyName();

        List<SSNewCompany> iCompanies = loadCompanies();
        if (iCompanies.isEmpty()) {
            setCurrentCompany(null);
            setCurrentYear(null);
            return;
        }

        if (!iActiveCatalogCompanyName.isPresent()) {
            setCurrentCompany(null);
            setCurrentYear(null);
            return;
        }

        if (iCurrentCompany == null || Repositories.companies().findById(iCurrentCompany).isEmpty()) {
            SSNewCompany iSelectedCompany;
            String iCatalogCompanyName = iActiveCatalogCompanyName.get();
            iSelectedCompany = Repositories.companies().findByName(iCatalogCompanyName).orElse(iCompanies.get(0));
            if (!Objects.equals(iCatalogCompanyName, iSelectedCompany.getName())) {
                showUiWarning("Företagsnamn avviker mellan katalog och företagsdata. Kontrollera företagsuppgifter.");
            }
            setCurrentCompany(iSelectedCompany);
        }

        if (iCurrentYear == null && iCurrentCompany != null) {
            Optional<SSNewAccountingYear> iDemoYear = iSeedService.getAccountingYearByRangeV2(
                    iConnection,
                    iCurrentCompany,
                    java.time.LocalDate.of(2025, 1, 1),
                    java.time.LocalDate.of(2025, 12, 31));

            if (iDemoYear.isPresent()) {
                openYear(iDemoYear.get());
            } else {
                loadYearsForCompany(iCurrentCompany).stream()
                        .max(Comparator.comparing(SSNewAccountingYear::getLocalTo))
                        .ifPresent(this::openYear);
            }
        }
    }

    void logStartupV2Mode() {
        LOG.info("startupLocal running in schema V2 mode; skipping legacy last-company restore");
    }

    public void init(boolean iShowDialog) {
        if (iCurrentCompany == null) {
            return;
        }

        loadProducts();
        loadCustomers();
        loadSuppliers();
        loadAutoDists();

        loadInpayments();
        loadTenders();
        loadOrders();
        loadInvoices();
        loadCreditInvoices();
        loadPeriodicInvoices();

        loadOutpayments();
        loadPurchaseOrders();
        loadSupplierInvoices();
        loadSupplierCreditInvoices();

        loadInventories();
        loadIndeliveries();
        loadOutdeliveries();

        loadOwnReports();

        SSInvoiceMath.iSaldoMap = null;
        SSInvoiceMath.calculateSaldos();
        SSCustomerMath.iInvoicesForCustomers = null;
        SSCustomerMath.getInvoicesForCustomers();
        SSSupplierInvoiceMath.iSaldoMap = null;
        SSSupplierInvoiceMath.calculateSaldos();
        SSSupplierMath.iInvoicesForSuppliers = null;
        SSSupplierMath.getInvoicesForSuppliers();
        // SSOrderMath.setInvoiceForOrders();
        initYear(false);

    }

    public void initYear(boolean iShowLoadingDialog) {
        if (iCurrentYear == null) {
            return;
        }

        iVouchers = null;
        getCurrentYear();
        loadVouchers();

    }

    public void shutdown() {
        try {
            if (!iConnection.isClosed()) {
                try (Statement iStatement = iConnection.createStatement()) {
                    iStatement.executeQuery("SHUTDOWN");
                }
                iConnection.close();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public void shutdownCompact() {
        try {
            try (Statement iStatement = iConnection.createStatement()) {
                iStatement.executeQuery("SHUTDOWN COMPACT");
            }
            iConnection.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public void loadLocalDatabase() {
        // loadLocalDatabase enbart använd vid inläsning av säkerhetsbackup.
//        LOG.info("KK loadLocalDatabase, SSDB.java");
        try {
            if (iConnection != null) {
                iConnection.close();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            LOG.info("ERROR: failed to load HSQLDB JDBC driver.");
            LOG.error("Unexpected error", e);
            return;
        }

        try {
            File dbDir = new File(Path.get(Path.USER_DATA), "db");
            iConnection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + dbDir.getAbsolutePath() + File.separator + "JFSDB", "sa", "");
            iConnection.setAutoCommit(false);
            failIfLegacySingleSchemaDatabase();
            createNewTables();
            //  Inlagda metoder för att testa inläsning av säkerhetsbackup ab DB.
            Repositories.init(this);
            checkImportDefaultAccountPlans();
            ensureCatalogBootstrapForCurrentSchema();
            validateSchemaContract();
            iTriggerSchemaService.rebuildLocalTriggers();
            initializeCurrentCompanyAndYear();
            logStartupV2Mode();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /* Create default account plans if no account plan exists in DB */
    private void checkImportDefaultAccountPlans() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return;
            }

            List<String> discoveredDefaultPaths = SSAccountPlanDefaultResourceDiscovery.discoverDefaultExcelResources(
                    SSDB.class.getClassLoader());
            Set<String> validDefaultPaths = new HashSet<>();

            for (String path : discoveredDefaultPaths) {
                String fileName = getFileName(path);
                try (InputStream is = SSDB.class.getClassLoader().getResourceAsStream(path)) {
                    if (is == null) {
                        LOG.warn("Default account-plan resource not found: {}", path);
                        continue;
                    }

                    SSAccountPlan plan = SSAccountPlanLoader.readPlan(is);
                    SSAccountPlanImporter.validateFileNameAgainstPlanName(fileName, plan);
                    plan.setExcelPath(path);
                    plan.setDefaultPlan(true);

                    upsertDefaultAccountPlan(plan);
                    validDefaultPaths.add(path);
                } catch (SSImportException | IOException | SQLException e) {
                    LOG.warn("Skipping invalid default account-plan '{}': {}", path, e.getMessage());
                }
            }

            cleanupRemovedDefaultAccountPlans(validDefaultPaths);
        } catch (SQLException | IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private void upsertDefaultAccountPlan(SSAccountPlan plan) throws SQLException {
        Integer existingId = null;
        try (PreparedStatement exists = iConnection.prepareStatement("SELECT id FROM tbl_accountplan WHERE name=?")) {
            exists.setObject(1, plan.getName());
            try (ResultSet resultSet = exists.executeQuery()) {
                if (resultSet.next()) {
                    existingId = resultSet.getInt(1);
                }
            }
        }

        if (existingId != null) {
            plan.setId(existingId);
            Repositories.accountPlans().update(plan);
            return;
        }
        Repositories.accountPlans().add(plan);
    }

    private void cleanupRemovedDefaultAccountPlans(Set<String> validDefaultPaths) {
        for (SSAccountPlan existing : Repositories.accountPlans().findAll()) {
            if (!existing.isDefaultPlan()) {
                continue;
            }
            String excelPath = existing.getExcelPath();
            if (excelPath == null || !validDefaultPaths.contains(excelPath)) {
                Repositories.accountPlans().delete(existing);
            }
        }
    }

    private String getFileName(String path) {
        if (path == null) {
            return null;
        }
        int separatorIndex = path.lastIndexOf('/');
        return separatorIndex < 0 ? path : path.substring(separatorIndex + 1);
    }

    public void deleteDatabaseFiles() {
        try {
            try (PreparedStatement iStatement = iConnection.prepareStatement("SHUTDOWN")) {
                iStatement.executeUpdate();
            }
            iConnection.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        File iDbDir = new File(Path.get(Path.USER_DATA), "db");
        File iPropFile = new File(iDbDir, "JFSDB.properties");
        File iScriptFile = new File(iDbDir, "JFSDB.script");
        File iDataFile = new File(iDbDir, "JFSDB.data");
        File iBackupFile = new File(iDbDir, "JFSDB.backup");
        File iLogFile = new File(iDbDir, "JFSDB.log");

        if (iPropFile.exists()) {
            iPropFile.delete();
        }
        if (iScriptFile.exists()) {
            iScriptFile.delete();
        }
        if (iDataFile.exists()) {
            iDataFile.delete();
        }
        if (iBackupFile.exists()) {
            iBackupFile.delete();
        }
        if (iLogFile.exists()) {
            iLogFile.delete();
        }
    }

    public void clearCachedLists() {
        clearCachedListsInternal();
    }

    void clearCachedListsInternal() {
        iProducts = null;
        iCustomers = null;
        iSuppliers = null;
        iAutoDists = null;
        iInpayments = null;
        iTenders = null;
        iOrders = null;
        iInvoices = null;
        iCreditInvoices = null;
        iPeriodicInvoices = null;
        iOutpayments = null;
        iPurchaseOrders = null;
        iSupplierInvoices = null;
        iSupplierCreditInvoices = null;
        iInventories = null;
        iIndeliveries = null;
        iOutdeliveries = null;
        iOwnReports = null;
    }

    void clearYearCachesInternal() {
        iVouchers = null;
    }

    public void setCurrentCompany(SSNewCompany iCompany) {
        if (iCompany == null) {
            iCurrentCompany = null;
            clearCachedListsInternal();
            notifyListeners("COMPANY", iCurrentCompany, null);
            return;
        }
        iCurrentCompany = Repositories.companies().findById(iCompany).orElse(iCompany);
        if (iCurrentCompany.getId() != null) {
            Repositories.companies().activateAndApplySchema(iCurrentCompany.getId());
        } else if (iCurrentCompany.getSchemaName() != null) {
            Repositories.companies().registerOrActivateCompanySchema(iCurrentCompany.getSchemaName(),
                    iCurrentCompany.getName());
            iCurrentCompany = Repositories.companies().findBySchemaName(iCurrentCompany.getSchemaName())
                    .orElse(iCurrentCompany);
        }
        clearCachedListsInternal();
        notifyListeners("COMPANY", iCurrentCompany, null);
    }

    public SSNewCompany getCurrentCompany() {
        return iCurrentCompany;
    }


    public void setCurrentYear(SSNewAccountingYear iYear) {
        if (iYear == null) {
            iCurrentYear = null;
            clearYearCachesInternal();
            notifyListeners("YEAR", iCurrentYear, null);
            return;
        }
        Repositories.accountingYears().open(iYear);
    }

    public void applyOpenedYearFromRepository(SSNewAccountingYear iYear) {
        iCurrentYear = iYear;
        clearYearCachesInternal();
        notifyListeners("YEAR", iCurrentYear, null);
    }

    /**
     * Opens an accounting year as the active year using the open/close lifecycle.
     * <p>
     * The account plan snapshot stored in {@code TBL_accountingyear} is read and
     * copied into {@code TBL_account} rows (keyed by {@code accountingyear_id}).
     * </p>
     *
     * @param iYear the year to open; must not be {@code null}
     */
    public void openYear(SSNewAccountingYear iYear) {
        Repositories.accountingYears().open(iYear);
    }


    public SSNewAccountingYear getCurrentYear() {
        return Repositories.accountingYears().findById(iCurrentYear).orElse(null);
    }

    public List<SSNewCompany> loadCompanies() {
        try {
            return new LinkedList<>(Repositories.companies().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
            return new LinkedList<>();
        }
    }

    public List<SSNewAccountingYear> loadYearsForCompany(SSNewCompany iCompany) {
        return Repositories.accountingYears().findForCompany(iCompany);
    }

    /**
     *
     * Adds a property listerner to the database, the avaiable properties is:
     *   IO      : I/O event
     *   COMPANY : Changed active company
     *   YEAR    : Changed active year
     *
     * @param pProperty
     * @param pPropertyChangeListener
     */
    public void addPropertyChangeListener(String pProperty, PropertyChangeListener pPropertyChangeListener) {
        iEventBus.addPropertyChangeListener(pProperty, pPropertyChangeListener);
    }

    /**
     *
     * @param pProperty
     * @param pNewValue
     * @param pOldValue
     */
    public void notifyListeners(String pProperty, Object pNewValue, Object pOldValue) {
        iEventBus.notifyListeners(this, pProperty, pNewValue, pOldValue);
    }

    public List<SSVoucher> loadVouchers() {
        List<SSVoucher> cached = iVouchers;
        if (cached != null) {
            return cached;
        }
        List<SSVoucher> freshList = iCurrentYear == null
                ? new LinkedList<>()
                : new LinkedList<>(Repositories.vouchers().findByYear(iCurrentYear));
        iVouchers = freshList;
        return iVouchers;
    }

    private void failIfLegacySingleSchemaDatabase() throws SQLException {
        if (iConnection == null || iConnection.isClosed()) {
            return;
        }
        boolean iHasCompanyTable = tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY");
        boolean iHasCatalogTable = tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG");
        if (iHasCompanyTable && !iHasCatalogTable) {
            throw new SQLException("Legacy single-schema database detected. Migration is not supported.");
        }
    }

    private boolean tableExistsInSchema(String schemaName, String tableNameUpper) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=?";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, tableNameUpper.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private boolean columnExistsInSchema(String schemaName, String tableNameUpper, String columnNameUpper) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=? "
                + "AND UPPER(COLUMN_NAME)=?";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, tableNameUpper.toUpperCase(Locale.ROOT));
            iStatement.setString(3, columnNameUpper.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private Optional<String> loadActiveCatalogSchemaName() throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE "
                        + "ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY");
             ResultSet iResultSet = iStatement.executeQuery()) {
            if (iResultSet.next()) {
                return Optional.ofNullable(iResultSet.getString("schema_name"));
            }
            return Optional.empty();
        }
    }

    private void validateSchemaContract() throws SQLException {
        if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG")) {
            throw new SQLException("Database schema mismatch: missing PUBLIC.TBL_COMPANY_CATALOG");
        }
        if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_ACCOUNTPLAN")) {
            throw new SQLException("Database schema mismatch: missing PUBLIC.TBL_ACCOUNTPLAN");
        }

        Optional<String> iActiveSchemaOptional = loadActiveCatalogSchemaName();
        if (iActiveSchemaOptional.isEmpty()) {
            throw new SQLException("Database schema mismatch: no active company schema in PUBLIC.TBL_COMPANY_CATALOG");
        }

        String iActiveSchema = iActiveSchemaOptional.get();
        if (!schemaExists(iActiveSchema)) {
            throw new SQLException("Database schema mismatch: active schema does not exist: " + iActiveSchema);
        }

        requireTable(iActiveSchema, "TBL_COMPANY");
        requireTable(iActiveSchema, "TBL_ACCOUNTINGYEAR");
        requireTable(iActiveSchema, "TBL_ACCOUNT");
        requireTable(iActiveSchema, "TBL_PRODUCT");
        requireTable(iActiveSchema, "TBL_CUSTOMER");
        requireTable(iActiveSchema, "TBL_SUPPLIER");
        requireTable(iActiveSchema, "TBL_INVOICE");

        requireColumn(iActiveSchema, "TBL_COMPANY", "ID");
        requireColumn(iActiveSchema, "TBL_COMPANY", "NAME");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "ID");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "COMPANYID");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "FROM_DATE");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "TO_DATE");
        requireColumn(iActiveSchema, "TBL_ACCOUNT", "ACCOUNTINGYEAR_ID");
        requireColumn(iActiveSchema, "TBL_ACCOUNT", "NUMBER");
        requireColumn(iActiveSchema, "TBL_PRODUCT", "NUMBER");
        requireColumn(iActiveSchema, "TBL_CUSTOMER", "NUMBER");
        requireColumn(iActiveSchema, "TBL_SUPPLIER", "NUMBER");
        requireColumn(iActiveSchema, "TBL_INVOICE", "NUMBER");
    }

    private void requireTable(String schemaName, String tableNameUpper) throws SQLException {
        if (!tableExistsInSchema(schemaName, tableNameUpper)) {
            throw new SQLException("Database schema mismatch: missing " + schemaName + "." + tableNameUpper);
        }
    }

    private void requireColumn(String schemaName, String tableNameUpper, String columnNameUpper) throws SQLException {
        if (!columnExistsInSchema(schemaName, tableNameUpper, columnNameUpper)) {
            throw new SQLException("Database schema mismatch: missing column "
                    + schemaName + "." + tableNameUpper + "." + columnNameUpper);
        }
    }

    private void ensureCatalogBootstrapForCurrentSchema() {
        try {
            if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG")) {
                return;
            }
            ensureSeedStateTable();

            int iCatalogRows = 0;
            try (PreparedStatement iCountStatement = iConnection.prepareStatement(
                    "SELECT COUNT(*) FROM PUBLIC.tbl_company_catalog");
                 ResultSet iCountResult = iCountStatement.executeQuery()) {
                if (iCountResult.next()) {
                    iCatalogRows = iCountResult.getInt(1);
                }
            }

            if (iCatalogRows == 0) {
                setSchema(DEFAULT_SCHEMA_NAME);
                if (schemaExists(DEMO_SCHEMA_NAME)) {
                    dropSchema(DEMO_SCHEMA_NAME);
                }
                createSchemaIfMissing(DEMO_SCHEMA_NAME);
                setSchema(DEMO_SCHEMA_NAME);

                SSSchemaBuilder iBuilder = new SSSchemaBuilder(iConnection);
                iBuilder.createCompanyTables();

                String iCompanyName = resolvePreferredCompanyNameInCurrentSchema();
                if (iCompanyName == null) {
                    iCompanyName = DEMO_COMPANY_NAME;
                }

                try (PreparedStatement iInsertStatement = iConnection.prepareStatement(
                        "INSERT INTO PUBLIC.tbl_company_catalog(schema_name, company_name, is_active) VALUES (?, ?, TRUE)")) {
                    iInsertStatement.setString(1, DEMO_SCHEMA_NAME);
                    iInsertStatement.setString(2, iCompanyName);
                    iInsertStatement.executeUpdate();
                }
                iConnection.commit();
                return;
            }

            Optional<String> iActiveSchemaName = loadActiveCatalogSchemaName();
            if (iActiveSchemaName.isPresent()) {
                setSchema(iActiveSchemaName.get());
                new SSSchemaBuilder(iConnection).createCompanyTables();
                iConnection.commit();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
        }
    }

    private void ensureSeedStateTable() throws SQLException {
        try (PreparedStatement iCreateTable = iConnection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + SEED_STATE_TABLE + " (seed_done BOOLEAN DEFAULT FALSE NOT NULL)")) {
            iCreateTable.executeUpdate();
        }
        try (PreparedStatement iCountStatement = iConnection.prepareStatement(
                "SELECT COUNT(*) FROM " + SEED_STATE_TABLE);
             ResultSet iCountResult = iCountStatement.executeQuery()) {
            if (iCountResult.next() && iCountResult.getInt(1) == 0) {
                try (PreparedStatement iInsert = iConnection.prepareStatement(
                        "INSERT INTO " + SEED_STATE_TABLE + "(seed_done) VALUES(FALSE)")) {
                    iInsert.executeUpdate();
                }
            }
        }
    }

    private String resolvePreferredCompanyNameInCurrentSchema() throws SQLException {
        try (PreparedStatement iCompanyStatement = iConnection.prepareStatement(
                "SELECT name FROM tbl_company ORDER BY CASE WHEN name=? THEN 0 ELSE 1 END, id FETCH FIRST 1 ROWS ONLY")) {
            iCompanyStatement.setString(1, DEMO_COMPANY_NAME);
            try (ResultSet iResultSet = iCompanyStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return iResultSet.getString(1);
                }
                return null;
            }
        }
    }

    private void createSchemaIfMissing(String schemaName) throws SQLException {
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("CREATE SCHEMA " + quoteIdentifier(schemaName));
        } catch (SQLException e) {
            String iMessage = e.getMessage();
            if (iMessage == null || !iMessage.toLowerCase(Locale.ROOT).contains("already exists")) {
                throw e;
            }
        }
    }

    private boolean schemaExists(String schemaName) throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE UPPER(SCHEMA_NAME)=?")) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private void dropSchema(String schemaName) throws SQLException {
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("DROP SCHEMA " + quoteIdentifier(schemaName) + " CASCADE");
        }
    }

    private void setSchema(String schemaName) throws SQLException {
        String iSafeSchema = schemaName == null || schemaName.trim().isEmpty()
                ? DEFAULT_SCHEMA_NAME
                : schemaName;
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("SET SCHEMA " + quoteIdentifier(iSafeSchema));
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void showUiWarning(String message) {
        LOG.warn(message);
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        javax.swing.JOptionPane.showMessageDialog(
                SSMainFrame.getInstance(),
                message,
                "Varning",
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Rolls back the current database transaction.
     *
     * @throws SQLException if the rollback fails
     */
    public void rollbackCurrentTransaction() throws SQLException {
        iConnection.rollback();
    }

    /**
     * Returns the current database connection.
     *
     * @return the active connection
     */
    public Connection getConnection() {
        return iConnection;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    // ---- V2 database API for SSCurrency repository ----

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSProduct> loadProducts() {
        if (iProducts != null) {
            return iProducts;
        }
        iProducts = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iProducts;
        }
        try {
            iProducts.addAll(Repositories.products().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iProducts;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSCustomer> loadCustomers() {
        if (iCustomers != null) {
            return iCustomers;
        }
        iCustomers = new LinkedList<>();
        try {
            iCustomers.addAll(Repositories.customers().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iCustomers;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the suppliers for the current company.
     *
     * @return  A List of suppliers or an empty list.
     */
    public List<SSSupplier> loadSuppliers() {
        if (iSuppliers != null) {
            return iSuppliers;
        }
        iSuppliers = new LinkedList<>();
        try {
            iSuppliers.addAll(Repositories.suppliers().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iSuppliers;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the autodistributions for the current company.
     *
     * @return  A List of autodists or an empty list.
     */
    public List<SSAutoDist> loadAutoDists() {
        if (iAutoDists != null) {
            return iAutoDists;
        }
        try {
            iAutoDists = new LinkedList<>(Repositories.autoDists().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
        if (iAutoDists == null) {
            iAutoDists = new LinkedList<>();
        }
        return iAutoDists;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the tenders in the current company.
     *
     * @return  A List of tenders or an empty list.
     */
    public List<SSTender> loadTenders() {
        if (iTenders != null) {
            return iTenders;
        }
        try {
            iTenders = new LinkedList<>(Repositories.tenders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iTenders == null) {
            iTenders = new LinkedList<>();
        }
        return iTenders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSOrder> loadOrders() {
        if (iOrders != null) {
            return iOrders;
        }
        try {
            iOrders = new LinkedList<>(Repositories.orders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOrders == null) {
            iOrders = new LinkedList<>();
        }
        return iOrders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSInvoice> loadInvoices() {
        if (iInvoices != null) {
            return iInvoices;
        }
        try {
            iInvoices = new LinkedList<>(Repositories.invoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInvoices == null) {
            iInvoices = new LinkedList<>();
        }
        return iInvoices;
    }


    /**
     * Returns the inpayments in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSInpayment> loadInpayments() {
        if (iInpayments != null) {
            return iInpayments;
        }
        try {
            iInpayments = new LinkedList<>(Repositories.inpayments().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInpayments == null) {
            iInpayments = new LinkedList<>();
        }
        return iInpayments;
    }


    /**
     * Returns the outpayments in the current company.
     *
     * @return  A List of outpayments or an empty list.
     */
    public List<SSOutpayment> loadOutpayments() {
        if (iOutpayments != null) {
            return iOutpayments;
        }
        try {
            iOutpayments = new LinkedList<>(Repositories.outpayments().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOutpayments == null) {
            iOutpayments = new LinkedList<>();
        }
        return iOutpayments;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSCreditInvoice> loadCreditInvoices() {
        if (iCreditInvoices != null) {
            return iCreditInvoices;
        }
        try {
            iCreditInvoices = new LinkedList<>(Repositories.creditInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iCreditInvoices == null) {
            iCreditInvoices = new LinkedList<>();
        }
        return iCreditInvoices;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the periodic invoices in the current company.
     *
     * @return  A List of periodic invoices or an empty list.
     */
    public List<SSPeriodicInvoice> loadPeriodicInvoices() {
        if (iPeriodicInvoices != null) {
            return iPeriodicInvoices;
        }
        try {
            iPeriodicInvoices = new LinkedList<>(Repositories.periodicInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iPeriodicInvoices == null) {
            iPeriodicInvoices = new LinkedList<>();
        }
        return iPeriodicInvoices;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the purchase orders in the current company.
     *
     * @return  A List of orders or an empty list.
     */
    public List<SSPurchaseOrder> loadPurchaseOrders() {
        if (iPurchaseOrders != null) {
            return iPurchaseOrders;
        }
        try {
            iPurchaseOrders = new LinkedList<>(Repositories.purchaseOrders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iPurchaseOrders == null) {
            iPurchaseOrders = new LinkedList<>();
        }
        return iPurchaseOrders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the supplier invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierInvoice> loadSupplierInvoices() {
        if (iSupplierInvoices != null) {
            return iSupplierInvoices;
        }
        try {
            iSupplierInvoices = new LinkedList<>(Repositories.supplierInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iSupplierInvoices == null) {
            iSupplierInvoices = new LinkedList<>();
        }
        return iSupplierInvoices;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierCreditInvoice> loadSupplierCreditInvoices() {
        if (iSupplierCreditInvoices != null) {
            return iSupplierCreditInvoices;
        }
        try {
            iSupplierCreditInvoices = new LinkedList<>(Repositories.supplierCreditInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iSupplierCreditInvoices == null) {
            iSupplierCreditInvoices = new LinkedList<>();
        }
        return iSupplierCreditInvoices;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSInventory> loadInventories() {
        if (iInventories != null) {
            return iInventories;
        }

        try {
            iInventories = new LinkedList<>(Repositories.inventories().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInventories == null) {
            iInventories = new LinkedList<>();
        }
        return iInventories;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSIndelivery> loadIndeliveries() {
        if (iIndeliveries != null) {
            return iIndeliveries;
        }
        try {
            iIndeliveries = new LinkedList<>(Repositories.indeliveries().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iIndeliveries == null) {
            iIndeliveries = new LinkedList<>();
        }
        return iIndeliveries;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSOutdelivery> loadOutdeliveries() {
        if (iOutdeliveries != null) {
            return iOutdeliveries;
        }
        try {
            iOutdeliveries = new LinkedList<>(Repositories.outdeliveries().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOutdeliveries == null) {
            iOutdeliveries = new LinkedList<>();
        }
        return iOutdeliveries;
    }

    // /////////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////////

    public List<SSOwnReport> loadOwnReports() {
        if (iOwnReports != null) {
            return iOwnReports;
        }
        try {
            iOwnReports = new LinkedList<>(Repositories.ownReports().findAll());
        } catch (IllegalStateException e) {
            LOG.error("Unexpected error loading own reports", e);
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error", e.getMessage());
            iOwnReports = new LinkedList<>();
        }
        return iOwnReports;
    }

    // /////////////////////////////////////////////////////////////////////////////

    public void createNewTables() {
        if (iConnection == null) {
            return;
        }
        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
            builder.createPublicTables();
            iConnection.commit();
        } catch (SQLException e) {
            LOG.error("Unexpected error in createNewTables", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.system.SSDB");
        sb.append("{iAutoDists=").append(iAutoDists);
        sb.append(", iConnection=").append(iConnection);
        sb.append(", iCreditInvoices=").append(iCreditInvoices);
        sb.append(", iCurrentCompany=").append(iCurrentCompany);
        sb.append(", iCurrentYear=").append(iCurrentYear);
        sb.append(", iCustomers=").append(iCustomers);
        sb.append(", iIndeliveries=").append(iIndeliveries);
        sb.append(", iInpayments=").append(iInpayments);
        sb.append(", iInventories=").append(iInventories);
        sb.append(", iInvoices=").append(iInvoices);
        sb.append(", iEventBus=").append(iEventBus);
        sb.append(", iOrders=").append(iOrders);
        sb.append(", iOutdeliveries=").append(iOutdeliveries);
        sb.append(", iOutpayments=").append(iOutpayments);
        sb.append(", iOwnReports=").append(iOwnReports);
        sb.append(", iPeriodicInvoices=").append(iPeriodicInvoices);
        sb.append(", iProducts=").append(iProducts);
        sb.append(", iPurchaseOrders=").append(iPurchaseOrders);
        sb.append(", iSupplierCreditInvoices=").append(iSupplierCreditInvoices);
        sb.append(", iSupplierInvoices=").append(iSupplierInvoices);
        sb.append(", iSuppliers=").append(iSuppliers);
        sb.append(", iTenders=").append(iTenders);
        sb.append(", iVouchers=").append(iVouchers);
        sb.append('}');
        return sb.toString();
    }
}
