package se.swedsoft.bookkeeping.data.system;


import se.swedsoft.bookkeeping.SSTriggerHandler;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.calc.math.*;
import se.swedsoft.bookkeeping.calc.service.SaldoDeltaService;
import se.swedsoft.bookkeeping.calc.util.SSAutoIncrement;
import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.*;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.autodist.SSAutoDistFrame;
import se.swedsoft.bookkeeping.gui.creditinvoice.SSCreditInvoiceFrame;
import se.swedsoft.bookkeeping.gui.customer.SSCustomerFrame;
import se.swedsoft.bookkeeping.gui.indelivery.SSIndeliveryFrame;
import se.swedsoft.bookkeeping.gui.inpayment.SSInpaymentFrame;
import se.swedsoft.bookkeeping.gui.inventory.SSInventoryFrame;
import se.swedsoft.bookkeeping.gui.invoice.SSInvoiceFrame;
import se.swedsoft.bookkeeping.gui.order.SSOrderFrame;
import se.swedsoft.bookkeeping.gui.outdelivery.SSOutdeliveryFrame;
import se.swedsoft.bookkeeping.gui.outpayment.SSOutpaymentFrame;
import se.swedsoft.bookkeeping.gui.ownreport.SSOwnReportFrame;
import se.swedsoft.bookkeeping.gui.ownreport.util.SSOwnReportAccountRow;
import se.swedsoft.bookkeeping.gui.periodicinvoice.SSPeriodicInvoiceFrame;
import se.swedsoft.bookkeeping.gui.product.SSProductFrame;
import se.swedsoft.bookkeeping.gui.project.SSProjectFrame;
import se.swedsoft.bookkeeping.gui.purchaseorder.SSPurchaseOrderFrame;
import se.swedsoft.bookkeeping.gui.resultunit.SSResultUnitFrame;
import se.swedsoft.bookkeeping.gui.supplier.SSSupplierFrame;
import se.swedsoft.bookkeeping.gui.suppliercreditinvoice.SSSupplierCreditInvoiceFrame;
import se.swedsoft.bookkeeping.gui.supplierinvoice.SSSupplierInvoiceFrame;
import se.swedsoft.bookkeeping.gui.tender.SSTenderFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInitDialog;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.voucher.SSVoucherFrame;
import se.swedsoft.bookkeeping.gui.vouchertemplate.SSVoucherTemplateFrame;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaBuilder;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaEnsurer;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaMigrationManager;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSUtil;
import se.swedsoft.bookkeeping.data.system.trigger.SSEventTriggerDispatcher;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSDB {
    private static final Logger LOG = LoggerFactory.getLogger(SSDB.class);

    private static final String SCHEMA_VERSION_PROPERTY = "fribok.schema.version";
    private static final String SCHEMA_V2 = "v2";

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
    private final ThreadLocal<Boolean> iBypassTriggerDispatcher = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final SSEventTriggerDispatcher iEventTriggerDispatcher;

    private Connection iConnection;
    private String iDetectedSchemaVersion = SCHEMA_V2;

    public static SSDB getInstance() {
        if (cInstance == null) {
            cInstance = new SSDB();
        }
        return cInstance;
    }

    private SSDB() {
        iEventTriggerDispatcher = new SSEventTriggerDispatcher(this);
    }

    public void startupLocal(Connection pConnection) throws SQLException {
        prepareStartupConnection(pConnection);
        createNewTables();
        dropTriggers();
        createLocalTriggers();
        Repositories.init(this);
    }

    void prepareStartupConnection(Connection pConnection) throws SQLException {
        iConnection = pConnection;
        iConnection.setAutoCommit(false);

        // SSDB is a singleton across test classes and app lifecycle; clear any
        // company/year state tied to a previous DB connection.
        iCurrentCompany = null;
        iCurrentYear = null;
        iVouchers = null;
        clearCachedLists();

        // V2-only mode: force schema selection to V2 for all runtimes/tests.
        iDetectedSchemaVersion = SCHEMA_V2;
        System.setProperty(SCHEMA_VERSION_PROPERTY, SCHEMA_V2);
    }

     void initializeSchemaRuntime() {
         initializeSchema();
         Repositories.init(this);
     }

     private void initializeSchema() {
         try {
             SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
             builder.createBaseTables();

             SSSchemaEnsurer ensurer = new SSSchemaEnsurer(iConnection);
             ensurer.ensureAllForwardCompatibility();

             SSSchemaMigrationManager migrationManager = new SSSchemaMigrationManager(iConnection);
             migrationManager.ensureQuantityScaleMigration();

             iConnection.commit();

             builder.dropTriggers();
             builder.createLocalTriggers();
         } catch (SQLException e) {
             LOG.error("Unexpected error in initializeSchema", e);
         }
     }

    void seedDemoDataIfNeeded() {
        boolean iShouldSeedDemoData = shouldSeedV2DemoDataV2();
        if (iShouldSeedDemoData) {
            checkImportDefaultAccountPlans();
        }

        runV2DemoSeedScript();
        if (iShouldSeedDemoData) {
            logV2DemoSeedSummary();
        }
    }

    void initializeCurrentCompanyAndYear() {
        List<SSNewCompany> iCompanies = loadCompanies();
        if (!iCompanies.isEmpty()) {
            if (iCurrentCompany == null || Repositories.companies().findById(iCurrentCompany).isEmpty()) {
                setCurrentCompany(Repositories.companies().findByName("DemofÃƒÆ’Ã‚Â¶retaget").orElse(iCompanies.get(0)));
            }
        }

        if (iCurrentYear == null && iCurrentCompany != null) {
            Optional<SSNewAccountingYear> iDemoYear = getAccountingYearByRangeV2(
                    iCurrentCompany,
                    java.time.LocalDate.of(2024, 1, 1),
                    java.time.LocalDate.of(2024, 12, 31));

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
            createNewTables();
            dropTriggers();
            createLocalTriggers();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private boolean shouldSeedV2DemoDataV2() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return false;
            }

            try (PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT 1 FROM tbl_company WHERE name=?")) {
                iStatement.setObject(1, "DemofÃƒÆ’Ã‚Â¶retaget");
                try (ResultSet iResultSet = iStatement.executeQuery()) {
                    return !iResultSet.next();
                }
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            return false;
        }
    }

    private void runV2DemoSeedScript() {
        executeSqlScriptResource("sql/seed_v2_demo.sql");
    }

    private Optional<SSNewAccountingYear> getAccountingYearByRangeV2(
            SSNewCompany iCompany,
            java.time.LocalDate iFrom,
            java.time.LocalDate iTo) {
        if (iCompany == null || iFrom == null || iTo == null || iConnection == null) {
            return Optional.empty();
        }

        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_accountingyear WHERE companyid=? AND from_date=? AND to_date=?")) {
            iStatement.setObject(1, iCompany.getId());
            iStatement.setObject(2, java.sql.Date.valueOf(iFrom));
            iStatement.setObject(3, java.sql.Date.valueOf(iTo));

            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return Optional.of(Repositories.accountingYears().mapAccountingYear(iResultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            return Optional.empty();
        }
    }

    private void executeSqlScriptResource(String iResourcePath) {
        String iCurrentStatement = null;
        try {
            String q = SSUtil.readResourceToString(iResourcePath);
            StringBuilder scriptBuilder = new StringBuilder();

            for (String line : q.split("\\r?\\n")) {
                if (!line.trim().startsWith("--")) {
                    scriptBuilder.append(line).append('\n');
                }
            }

            for (String statement : scriptBuilder.toString().split(";")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                iCurrentStatement = trimmed;
                try (PreparedStatement iStatement = iConnection.prepareStatement(trimmed)) {
                    iStatement.executeUpdate();
                }
            }

            iConnection.commit();
            LOG.info("Executed SQL seed script: {}", iResourcePath);
        } catch (Exception e) {
            LOG.error("Failed SQL seed script '{}' near statement: {}", iResourcePath, iCurrentStatement, e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            throw new IllegalStateException(
                    "Failed SQL seed script '" + iResourcePath + "' near statement: " + iCurrentStatement,
                    e);
        }
    }

    private void logV2DemoSeedSummary() {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT c.name AS cname, " +
                        "(SELECT COUNT(*) FROM tbl_customer cu WHERE cu.companyid=c.id) AS customer_count, " +
                        "(SELECT COUNT(*) FROM tbl_product p WHERE p.companyid=c.id) AS product_count, " +
                        "(SELECT COUNT(*) FROM tbl_supplier s WHERE s.companyid=c.id) AS supplier_count, " +
                        "(SELECT COUNT(*) FROM tbl_accountingyear y WHERE y.companyid=c.id AND y.from_date=? AND y.to_date=?) AS year_count, " +
                        "(SELECT COUNT(*) FROM tbl_voucher v JOIN tbl_accountingyear y2 ON y2.id=v.yearid WHERE y2.companyid=c.id AND y2.from_date=? AND y2.to_date=?) AS voucher_count " +
                        "FROM tbl_company c WHERE c.name=?")) {
            java.sql.Date iFrom = java.sql.Date.valueOf(java.time.LocalDate.of(2024, 1, 1));
            java.sql.Date iTo = java.sql.Date.valueOf(java.time.LocalDate.of(2024, 12, 31));
            iStatement.setObject(1, iFrom);
            iStatement.setObject(2, iTo);
            iStatement.setObject(3, iFrom);
            iStatement.setObject(4, iTo);
            iStatement.setObject(5, "DemofÃƒÆ’Ã‚Â¶retaget");

            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    LOG.info(
                            "V2 demo seed complete: company='{}', year=2024, years={}, customers={}, products={}, suppliers={}, vouchers={}",
                            iResultSet.getString("cname"),
                            iResultSet.getInt("year_count"),
                            iResultSet.getInt("customer_count"),
                            iResultSet.getInt("product_count"),
                            iResultSet.getInt("supplier_count"),
                            iResultSet.getInt("voucher_count"));
                }
            }
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

            try (Statement iStatement = iConnection.createStatement();
                 ResultSet iResultSet = iStatement.executeQuery("SELECT 0 FROM tbl_accountplan")) {
                if (iResultSet.next()) {
                    // Have at least one account plan in DB. Dont import defaults
                    return;
                }
            }

            LOG.info("Creating default account plans.");

            String[] defaults = new String[]{
                "BAS96(07)-AB & EF.xlsx",
                "BAS96(07)-Enskild-naringsidkare.xlsx",
                "BAS96(07)-HB & KB.xlsx",
                "Bas2006(07)-AB & EF.xlsx",
                "Bas2006(07)-Enskild-naringsidkare.xlsx",
                "Bas2006(07)-HB & KB.xlsx",
                "Bas2007(K1)-Enskild-naringsidkare.xlsx",};

            for (String s : defaults) {
                LOG.info(s);
                String path = "account/default/" + s;
                InputStream is = SSDB.class.getClassLoader().getResourceAsStream(path);
                // Keep loading old packaged .xls files until all bundled defaults are renamed.
                if (is == null && s.endsWith(".xlsx")) {
                    String legacyPath = "account/default/" + s.substring(0, s.length() - 1);
                    is = SSDB.class.getClassLoader().getResourceAsStream(legacyPath);
                    if (is != null) {
                        path = legacyPath;
                    }
                }
                if (is == null) {
                    throw new RuntimeException("Resource not found: " + path);
                }
                try {
                    SSAccountPlanImporter.doImport(is);
                } catch (IOException ex) {
                    LOG.error("Unexpected error", ex);
                } catch (SSImportException ex) {
                    LOG.error("Unexpected error", ex);
                }
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
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

    public void setCurrentCompany(SSNewCompany iCompany) {
        setCurrentCompanyInternal(iCompany);
    }

    void setCurrentCompanyInternal(SSNewCompany iCompany) {
        iCurrentCompany = Repositories.companies().findById(iCompany).orElse(iCompany);
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
        notifyListeners("COMPANY", iCurrentCompany, null);
    }

    public SSNewCompany getCurrentCompany() {
        return getCurrentCompanyInternal();
    }

    SSNewCompany getCurrentCompanyInternal() {
        return iCurrentCompany;
    }


    public void setCurrentYear(SSNewAccountingYear iYear) {
        if (iYear == null) {
            iCurrentYear = null;
            iVouchers = null;
            notifyListeners("YEAR", iCurrentYear, null);
            return;
        }
        Repositories.accountingYears().open(iYear);
    }

    public void applyOpenedYearFromRepository(SSNewAccountingYear iYear) {
        iCurrentYear = iYear;
        iVouchers = null;
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

    public Optional<SSNewAccountingYear> getPreviousYear() {
        return Repositories.accountingYears().findPrevious();
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

    public Optional<SSAutoIncrement> getAutoIncrement() {
        return Optional.empty();
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

    /**
     * Retuns the account plan for the current year
     *
     * @return the acoount plan for the current year
     */
    public SSAccountPlan findCurrentAccountPlan() {

        if (iCurrentYear != null) {
            return iCurrentYear.getAccountPlan();
        }
        return new SSAccountPlan("Default");
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

    public synchronized void triggerAction(String iTriggerName, String iTableName, String iNumber) {

        /** KÃƒÆ’Ã‚Â¶rs dÃƒÆ’Ã‚Â¥ en trigger triggas i databasen. De flesta triggers uppdaterar listan som
         *  som motsvarar objekten triggen kÃƒÆ’Ã‚Â¶rts pÃƒÆ’Ã‚Â¥. Projekt, Resultatenhet och konteringsmallar fÃƒÆ’Ã‚Â¥r
         *  behandlas nÃƒÆ’Ã‚Â¥got annorlunda dÃƒÆ’Ã‚Â¥ dessa inte lÃƒÆ’Ã‚Â¤sts in i minnet vid uppstart.
         */

        try {

            if (!iBypassTriggerDispatcher.get()) {
                if (iEventTriggerDispatcher.dispatch(iTriggerName, iTableName, iNumber)) {
                    return;
                }
                LOG.warn("Trigger {} not dispatched, using legacy fallback path", iTriggerName);
            }

            if (iTriggerName.equals("NEWPURCHASEORDER")
                    && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSPurchaseOrder> optPurchaseOrder = Repositories.purchaseOrders()
                        .findByPurchaseOrder(iPurchaseOrder);
                if (optPurchaseOrder.isEmpty()) {
                    LOG.warn("NEWPURCHASEORDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iPurchaseOrder = optPurchaseOrder.get();
                if (!iPurchaseOrders.contains(iPurchaseOrder)) {
                    iPurchaseOrders.add(iPurchaseOrder);
                }
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITPURCHASEORDER")
                    && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSPurchaseOrder> optPurchaseOrder = Repositories.purchaseOrders()
                        .findByPurchaseOrder(iPurchaseOrder);
                if (optPurchaseOrder.isEmpty()) {
                    LOG.warn("EDITPURCHASEORDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iPurchaseOrder = optPurchaseOrder.get();
                int iIndex = iPurchaseOrders.lastIndexOf(iPurchaseOrder);
                if (iIndex == -1) {
                    return;
                }
                iPurchaseOrders.remove(iIndex);
                iPurchaseOrders.add(iIndex, iPurchaseOrder);
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEPURCHASEORDER")
                    && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                iPurchaseOrders.remove(iPurchaseOrder);
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            }
        } catch (NumberFormatException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public boolean handleMasterdataTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (iTriggerName.contains("PROJECT")) {
            if (SSProjectFrame.getInstance() != null) {
                SSProjectFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.contains("RESULTUNIT")) {
            if (SSResultUnitFrame.getInstance() != null) {
                SSResultUnitFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.contains("VOUCHERTEMPLATE")) {
            if (SSVoucherTemplateFrame.getInstance() != null) {
                SSVoucherTemplateFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWPRODUCT", "EDITPRODUCT", "DELETEPRODUCT")) {
            if (iProducts == null) {
                return true;
            }
            SSProduct iProduct = new SSProduct();
            iProduct.setNumber(iNumber);

            if (iTriggerName.equals("NEWPRODUCT")) {
                Optional<SSProduct> optProduct = Repositories.products().findByProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("NEWPRODUCT trigger: product not found for number {}", iNumber);
                    return true;
                }
                iProduct = optProduct.get();
                if (!iProducts.contains(iProduct)) {
                    iProducts.add(iProduct);
                }
            } else if (iTriggerName.equals("EDITPRODUCT")) {
                Optional<SSProduct> optProduct = Repositories.products().findByProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("EDITPRODUCT trigger: product not found for number {}", iNumber);
                    return true;
                }
                iProduct = optProduct.get();
                int iIndex = iProducts.lastIndexOf(iProduct);
                if (iIndex == -1) {
                    return true;
                }
                iProducts.remove(iIndex);
                iProducts.add(iIndex, iProduct);
            } else {
                iProducts.remove(iProduct);
            }

            if (SSProductFrame.getInstance() != null) {
                SSProductFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWCUSTOMER", "EDITCUSTOMER", "DELETECUSTOMER")) {
            if (iCustomers == null) {
                return true;
            }
            SSCustomer iCustomer = new SSCustomer();
            iCustomer.setNumber(iNumber);

            if (iTriggerName.equals("NEWCUSTOMER")) {
                Optional<SSCustomer> optCustomer = Repositories.customers().findByCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("NEWCUSTOMER trigger: customer not found for number {}", iNumber);
                    return true;
                }
                iCustomer = optCustomer.get();
                if (!iCustomers.contains(iCustomer)) {
                    iCustomers.add(iCustomer);
                }
                if (SSCustomerMath.iInvoicesForCustomers == null) {
                    SSCustomerMath.iInvoicesForCustomers = new HashMap<>();
                }
                SSCustomerMath.iInvoicesForCustomers.put(iCustomer.getNumber(), new LinkedList<>());
            } else if (iTriggerName.equals("EDITCUSTOMER")) {
                Optional<SSCustomer> optCustomer = Repositories.customers().findByCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("EDITCUSTOMER trigger: customer not found for number {}", iNumber);
                    return true;
                }
                iCustomer = optCustomer.get();
                int iIndex = iCustomers.lastIndexOf(iCustomer);
                if (iIndex == -1) {
                    return true;
                }
                iCustomers.remove(iIndex);
                iCustomers.add(iIndex, iCustomer);
            } else {
                iCustomers.remove(iCustomer);
            }

            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWSUPPLIER", "EDITSUPPLIER", "DELETESUPPLIER")) {
            if (iSuppliers == null) {
                return true;
            }
            SSSupplier iSupplier = new SSSupplier();
            iSupplier.setNumber(iNumber);

            if (iTriggerName.equals("NEWSUPPLIER")) {
                Optional<SSSupplier> optSupplier = Repositories.suppliers().findBySupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("NEWSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return true;
                }
                iSupplier = optSupplier.get();
                if (!iSuppliers.contains(iSupplier)) {
                    iSuppliers.add(iSupplier);
                }
                if (SSSupplierMath.iInvoicesForSuppliers == null) {
                    SSSupplierMath.iInvoicesForSuppliers = new HashMap<>();
                }
                SSSupplierMath.iInvoicesForSuppliers.put(iSupplier.getNumber(), new LinkedList<>());
            } else if (iTriggerName.equals("EDITSUPPLIER")) {
                Optional<SSSupplier> optSupplier = Repositories.suppliers().findBySupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("EDITSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return true;
                }
                iSupplier = optSupplier.get();
                int iIndex = iSuppliers.lastIndexOf(iSupplier);
                if (iIndex == -1) {
                    return true;
                }
                iSuppliers.remove(iIndex);
                iSuppliers.add(iIndex, iSupplier);
            } else {
                iSuppliers.remove(iSupplier);
            }

            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWAUTODIST", "EDITAUTODIST", "DELETEAUTODIST")) {
            if (iAutoDists == null) {
                return true;
            }
            Integer iAccount = Integer.parseInt(iNumber);
            SSAutoDist iAutoDist = new SSAutoDist();
            iAutoDist.setAccountNumber(iAccount);

            if (iTriggerName.equals("NEWAUTODIST")) {
                Optional<SSAutoDist> optAutoDist = Repositories.autoDists().findByAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("NEWAUTODIST trigger: autodist not found for number {}", iNumber);
                    return true;
                }
                iAutoDist = optAutoDist.get();
                if (!iAutoDists.contains(iAutoDist)) {
                    iAutoDists.add(iAutoDist);
                }
            } else if (iTriggerName.equals("EDITAUTODIST")) {
                Optional<SSAutoDist> optAutoDist = Repositories.autoDists().findByAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("EDITAUTODIST trigger: autodist not found for number {}", iNumber);
                    return true;
                }
                iAutoDist = optAutoDist.get();
                int iIndex = iAutoDists.lastIndexOf(iAutoDist);
                if (iIndex == -1) {
                    return true;
                }
                iAutoDists.remove(iIndex);
                iAutoDists.add(iIndex, iAutoDist);
            } else {
                iAutoDists.remove(iAutoDist);
            }

            if (SSAutoDistFrame.getInstance() != null) {
                SSAutoDistFrame.getInstance().updateFrame();
            }
            return true;
        }

        return false;
    }

    public boolean handleSalesTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName,
                "NEWTENDER", "EDITTENDER", "DELETETENDER",
                "NEWORDER", "EDITORDER", "DELETEORDER",
                "NEWINVOICE", "EDITINVOICE", "DELETEINVOICE",
                "NEWCREDITINVOICE", "EDITCREDITINVOICE", "DELETECREDITINVOICE",
                "NEWPERIODICINVOICE", "EDITPERIODICINVOICE", "DELETEPERIODICINVOICE")) {
            return false;
        }

        if (isAnyTrigger(iTriggerName, "NEWTENDER", "EDITTENDER", "DELETETENDER")) {
            if (iTenders == null) {
                return true;
            }
            SSTender iTender = new SSTender();
            iTender.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWTENDER")) {
                Optional<SSTender> optTender = Repositories.tenders().findByTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("NEWTENDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iTender = optTender.get();
                if (!iTenders.contains(iTender)) {
                    iTenders.add(iTender);
                }
            } else if (iTriggerName.equals("EDITTENDER")) {
                Optional<SSTender> optTender = Repositories.tenders().findByTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("EDITTENDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iTender = optTender.get();
                int iIndex = iTenders.lastIndexOf(iTender);
                if (iIndex == -1) {
                    return true;
                }
                iTenders.remove(iIndex);
                iTenders.add(iIndex, iTender);
            } else {
                iTenders.remove(iTender);
            }

            if (SSTenderFrame.getInstance() != null) {
                SSTenderFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWORDER", "EDITORDER", "DELETEORDER")) {
            if (iOrders == null) {
                return true;
            }
            SSOrder iOrder = new SSOrder();
            iOrder.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWORDER")) {
                Optional<SSOrder> optOrder = Repositories.orders().findByOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("NEWORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOrder = optOrder.get();
                if (!iOrders.contains(iOrder)) {
                    iOrders.add(iOrder);
                }
            } else if (iTriggerName.equals("EDITORDER")) {
                Optional<SSOrder> optOrder = Repositories.orders().findByOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("EDITORDER trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iOrder = optOrder.get();
                int iIndex = iOrders.lastIndexOf(iOrder);
                if (iIndex == -1) {
                    return true;
                }
                iOrders.remove(iIndex);
                iOrders.add(iIndex, iOrder);
            } else {
                iOrders.remove(iOrder);
            }

            if (SSOrderFrame.getInstance() != null) {
                SSOrderFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWINVOICE", "EDITINVOICE", "DELETEINVOICE")) {
            if (iInvoices == null) {
                return true;
            }
            SSInvoice iInvoice = new SSInvoice();
            iInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWINVOICE")) {
                Optional<SSInvoice> optInvoice = Repositories.invoices().findByInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("NEWINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iInvoice = optInvoice.get();
                if (!iInvoices.contains(iInvoice)) {
                    iInvoices.add(iInvoice);
                }
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(), SSInvoiceMath.getSaldo(iInvoice));
                if (SSCustomerMath.iInvoicesForCustomers.containsKey(iInvoice.getCustomerNr())) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(iInvoice);
                } else {
                    List<SSInvoice> iNumbers = new LinkedList<>();
                    iNumbers.add(iInvoice);
                    SSCustomerMath.iInvoicesForCustomers.put(iInvoice.getCustomerNr(), iNumbers);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            if (iTriggerName.equals("EDITINVOICE")) {
                Optional<SSInvoice> optInvoice = Repositories.invoices().findByInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("EDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iInvoice = optInvoice.get();
                int iIndex = iInvoices.lastIndexOf(iInvoice);
                if (iIndex == -1) {
                    return true;
                }
                iInvoices.remove(iIndex);
                iInvoices.add(iIndex, iInvoice);
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(), SSInvoiceMath.getSaldo(iInvoice));
                iIndex = SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).indexOf(iInvoice);
                if (iIndex != -1) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).remove(iIndex);
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(iIndex, iInvoice);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            iInvoices.remove(iInvoice);
            SSInvoiceMath.iSaldoMap.remove(iInvoice.getNumber());
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWCREDITINVOICE", "EDITCREDITINVOICE", "DELETECREDITINVOICE")) {
            if (iCreditInvoices == null) {
                return true;
            }
            SSCreditInvoice iCreditInvoice = new SSCreditInvoice();
            iCreditInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWCREDITINVOICE")) {
                Optional<SSCreditInvoice> optCreditInvoice = Repositories.creditInvoices().findByCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("NEWCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iCreditInvoice = optCreditInvoice.get();
                if (!iCreditInvoices.contains(iCreditInvoice)) {
                    iCreditInvoices.add(iCreditInvoice);
                }
                SaldoDeltaService.applyCustomerCreditInvoiceNew(iCreditInvoice);
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            if (iTriggerName.equals("EDITCREDITINVOICE")) {
                Optional<SSCreditInvoice> optCreditInvoice = Repositories.creditInvoices().findByCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("EDITCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iCreditInvoice = optCreditInvoice.get();
                int iIndex = iCreditInvoices.lastIndexOf(iCreditInvoice);
                if (iIndex == -1) {
                    return true;
                }
                SSCreditInvoice iOldCreditInvoice = iCreditInvoices.get(iIndex);
                SaldoDeltaService.applyCustomerCreditInvoiceEditRevert(iOldCreditInvoice);
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                iCreditInvoices.remove(iIndex);
                iCreditInvoices.add(iIndex, iCreditInvoice);
                SaldoDeltaService.applyCustomerCreditInvoiceEditApply(iCreditInvoice);
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
                return true;
            }

            iCreditInvoices.remove(iCreditInvoice);
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSCreditInvoiceFrame.getInstance() != null) {
                SSCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName, "NEWPERIODICINVOICE", "EDITPERIODICINVOICE", "DELETEPERIODICINVOICE")) {
            if (iPeriodicInvoices == null) {
                return true;
            }
            SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();
            iPeriodicInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWPERIODICINVOICE")) {
                Optional<SSPeriodicInvoice> optPeriodicInvoice = Repositories.periodicInvoices().findByPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("NEWPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                if (!iPeriodicInvoices.contains(iPeriodicInvoice)) {
                    iPeriodicInvoices.add(iPeriodicInvoice);
                }
            } else if (iTriggerName.equals("EDITPERIODICINVOICE")) {
                Optional<SSPeriodicInvoice> optPeriodicInvoice = Repositories.periodicInvoices().findByPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("EDITPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                int iIndex = iPeriodicInvoices.lastIndexOf(iPeriodicInvoice);
                if (iIndex == -1) {
                    return true;
                }
                iPeriodicInvoices.remove(iIndex);
                iPeriodicInvoices.add(iIndex, iPeriodicInvoice);
            } else {
                iPeriodicInvoices.remove(iPeriodicInvoice);
            }

            if (SSPeriodicInvoiceFrame.getInstance() != null) {
                SSPeriodicInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        return true;
    }

    public boolean handleCustomerPaymentTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName, "NEWINPAYMENT", "EDITINPAYMENT", "DELETEINPAYMENT")) {
            return false;
        }

        if (iInpayments == null) {
            return true;
        }

        if (iTriggerName.equals("NEWINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            Optional<SSInpayment> optInpayment = Repositories.inpayments().findByInpayment(iInpayment);
            if (optInpayment.isEmpty()) {
                LOG.warn("NEWINPAYMENT trigger: inpayment not found for number {}", iNumber);
                return true;
            }
            iInpayment = optInpayment.get();
            if (!iInpayments.contains(iInpayment)) {
                iInpayments.add(iInpayment);
                applyInpaymentSaldoDelta(iInpayment, false);
            }
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            Optional<SSInpayment> optInpayment = Repositories.inpayments().findByInpayment(iInpayment);
            if (optInpayment.isEmpty()) {
                LOG.warn("EDITINPAYMENT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInpayment = optInpayment.get();
            int iIndex = iInpayments.lastIndexOf(iInpayment);
            if (iIndex == -1) {
                return true;
            }
            SSInpayment iOldInpayment = iInpayments.get(iIndex);
            applyInpaymentSaldoDelta(iOldInpayment, true);
            iInpayments.remove(iIndex);
            iInpayments.add(iIndex, iInpayment);
            applyInpaymentSaldoDelta(iInpayment, false);
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINPAYMENT")) {
            SSInpayment iInpayment = new SSInpayment();
            iInpayment.setNumber(Integer.parseInt(iNumber));

            int iIndex = iInpayments.lastIndexOf(iInpayment);
            if (iIndex != -1) {
                SSInpayment iOldInpayment = iInpayments.get(iIndex);
                applyInpaymentSaldoDelta(iOldInpayment, true);
                iInpayments.remove(iIndex);
            }
            if (SSCustomerFrame.getInstance() != null) {
                SSCustomerFrame.getInstance().updateFrame();
            }
            if (SSInvoiceFrame.getInstance() != null) {
                SSInvoiceFrame.getInstance().updateFrame();
            }
            if (SSInpaymentFrame.getInstance() != null) {
                SSInpaymentFrame.getInstance().updateFrame();
            }
            return true;
        }
        return false;
    }

    public boolean handlePurchaseSupplierTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName,
                "NEWPURCHASEORDER", "EDITPURCHASEORDER", "DELETEPURCHASEORDER",
                "NEWOUTPAYMENT",
                "NEWSUPPLIERCREDITINVOICE", "EDITSUPPLIERCREDITINVOICE", "DELETESUPPLIERCREDITINVOICE")) {
            return false;
        }

        if (isAnyTrigger(iTriggerName, "NEWOUTPAYMENT")) {
            if (iOutpayments == null) {
                return true;
            }
            SSOutpayment iOutpayment = new SSOutpayment();
            iOutpayment.setNumber(Integer.parseInt(iNumber));

            Optional<SSOutpayment> optOutpayment = Repositories.outpayments().findByOutpayment(iOutpayment);
            if (optOutpayment.isEmpty()) {
                LOG.warn("NEWOUTPAYMENT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOutpayment = optOutpayment.get();
            if (!iOutpayments.contains(iOutpayment)) {
                iOutpayments.add(iOutpayment);
                applyOutpaymentSaldoDelta(iOutpayment, false);
            }
            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            if (SSSupplierInvoiceFrame.getInstance() != null) {
                SSSupplierInvoiceFrame.getInstance().updateFrame();
            }
            if (SSOutpaymentFrame.getInstance() != null) {
                SSOutpaymentFrame.getInstance().updateFrame();
            }
            if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (isAnyTrigger(iTriggerName,
                "NEWSUPPLIERCREDITINVOICE", "EDITSUPPLIERCREDITINVOICE", "DELETESUPPLIERCREDITINVOICE")) {
            if (iSupplierCreditInvoices == null) {
                return true;
            }
            SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice();
            iSupplierCreditInvoice.setNumber(Integer.parseInt(iNumber));

            if (iTriggerName.equals("NEWSUPPLIERCREDITINVOICE")) {
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = Repositories.supplierCreditInvoices()
                        .findBySupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("NEWSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                if (!iSupplierCreditInvoices.contains(iSupplierCreditInvoice)) {
                    iSupplierCreditInvoices.add(iSupplierCreditInvoice);
                }
                SaldoDeltaService.applySupplierCreditInvoiceNew(iSupplierCreditInvoice);
            } else if (iTriggerName.equals("EDITSUPPLIERCREDITINVOICE")) {
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = Repositories.supplierCreditInvoices()
                        .findBySupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("EDITSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return true;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                int iIndex = iSupplierCreditInvoices.lastIndexOf(iSupplierCreditInvoice);
                if (iIndex == -1) {
                    return true;
                }
                SSSupplierCreditInvoice iOldSupplierCreditInvoice = iSupplierCreditInvoices.get(iIndex);
                SaldoDeltaService.applySupplierCreditInvoiceEditRevert(iOldSupplierCreditInvoice);
                iSupplierCreditInvoices.remove(iIndex);
                iSupplierCreditInvoices.add(iIndex, iSupplierCreditInvoice);
                SaldoDeltaService.applySupplierCreditInvoiceEditApply(iSupplierCreditInvoice);
            } else {
                int iIndex = iSupplierCreditInvoices.lastIndexOf(iSupplierCreditInvoice);
                SSSupplierCreditInvoice iRemovedSupplierCreditInvoice =
                        iIndex >= 0 ? iSupplierCreditInvoices.get(iIndex) : iSupplierCreditInvoice;
                iSupplierCreditInvoices.remove(iSupplierCreditInvoice);
                SaldoDeltaService.applySupplierCreditInvoiceDeleteRevert(iRemovedSupplierCreditInvoice);
            }

            if (SSSupplierFrame.getInstance() != null) {
                SSSupplierFrame.getInstance().updateFrame();
            }
            if (SSSupplierInvoiceFrame.getInstance() != null) {
                SSSupplierInvoiceFrame.getInstance().updateFrame();
            }
            if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
            }
            return true;
        }

        return false;
    }

    public boolean handleInventoryTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName,
                "NEWINVENTORY", "EDITINVENTORY", "DELETEINVENTORY",
                "NEWINDELIVERY", "EDITINDELIVERY", "DELETEINDELIVERY",
                "NEWOUTDELIVERY", "EDITOUTDELIVERY", "DELETEOUTDELIVERY")) {
            return false;
        }

        if (iTriggerName.equals("NEWINVENTORY")) {
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();

            iInventory.setNumber(Integer.parseInt(iNumber));
            Optional<SSInventory> optInventory = Repositories.inventories().findByInventory(iInventory);
            if (optInventory.isEmpty()) {
                LOG.warn("NEWINVENTORY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInventory = optInventory.get();
            if (!iInventories.contains(iInventory)) {
                iInventories.add(iInventory);
            }
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINVENTORY")) {
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();

            iInventory.setNumber(Integer.parseInt(iNumber));
            Optional<SSInventory> optInventory = Repositories.inventories().findByInventory(iInventory);
            if (optInventory.isEmpty()) {
                LOG.warn("EDITINVENTORY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iInventory = optInventory.get();
            int iIndex = iInventories.lastIndexOf(iInventory);

            if (iIndex == -1) {
                return true;
            }
            iInventories.remove(iIndex);
            iInventories.add(iIndex, iInventory);
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINVENTORY")) {
            if (iInventories == null) {
                return true;
            }
            SSInventory iInventory = new SSInventory();

            iInventory.setNumber(Integer.parseInt(iNumber));
            iInventories.remove(iInventory);
            if (SSInventoryFrame.getInstance() != null) {
                SSInventoryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("NEWINDELIVERY")) {
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();

            iIndelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSIndelivery> optIndelivery = Repositories.indeliveries().findByIndelivery(iIndelivery);
            if (optIndelivery.isEmpty()) {
                LOG.warn("NEWINDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iIndelivery = optIndelivery.get();
            if (!iIndeliveries.contains(iIndelivery)) {
                iIndeliveries.add(iIndelivery);
            }
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITINDELIVERY")) {
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();

            iIndelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSIndelivery> optIndelivery = Repositories.indeliveries().findByIndelivery(iIndelivery);
            if (optIndelivery.isEmpty()) {
                LOG.warn("EDITINDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iIndelivery = optIndelivery.get();
            int iIndex = iIndeliveries.lastIndexOf(iIndelivery);

            if (iIndex == -1) {
                return true;
            }
            iIndeliveries.remove(iIndex);
            iIndeliveries.add(iIndex, iIndelivery);
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEINDELIVERY")) {
            if (iIndeliveries == null) {
                return true;
            }
            SSIndelivery iIndelivery = new SSIndelivery();

            iIndelivery.setNumber(Integer.parseInt(iNumber));
            iIndeliveries.remove(iIndelivery);
            if (SSIndeliveryFrame.getInstance() != null) {
                SSIndeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("NEWOUTDELIVERY")) {
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();

            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSOutdelivery> optOutdelivery = Repositories.outdeliveries().findByOutdelivery(iOutdelivery);
            if (optOutdelivery.isEmpty()) {
                LOG.warn("NEWOUTDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOutdelivery = optOutdelivery.get();
            if (!iOutdeliveries.contains(iOutdelivery)) {
                iOutdeliveries.add(iOutdelivery);
            }
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITOUTDELIVERY")) {
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();

            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            Optional<SSOutdelivery> optOutdelivery = Repositories.outdeliveries().findByOutdelivery(iOutdelivery);
            if (optOutdelivery.isEmpty()) {
                LOG.warn("EDITOUTDELIVERY trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOutdelivery = optOutdelivery.get();
            int iIndex = iOutdeliveries.lastIndexOf(iOutdelivery);

            if (iIndex == -1) {
                return true;
            }
            iOutdeliveries.remove(iIndex);
            iOutdeliveries.add(iIndex, iOutdelivery);
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEOUTDELIVERY")) {
            if (iOutdeliveries == null) {
                return true;
            }
            SSOutdelivery iOutdelivery = new SSOutdelivery();

            iOutdelivery.setNumber(Integer.parseInt(iNumber));
            iOutdeliveries.remove(iOutdelivery);
            if (SSOutdeliveryFrame.getInstance() != null) {
                SSOutdeliveryFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }

    public boolean handleAccountingTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName, "NEWVOUCHER", "EDITVOUCHER", "DELETEVOUCHER")) {
            return false;
        }

        if (iVouchers == null) {
            return true;
        }

        if (iTriggerName.equals("NEWVOUCHER")) {
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

            Optional<SSVoucher> optVoucher = Repositories.vouchers().findVoucher(iVoucher);
            if (optVoucher.isEmpty()) {
                LOG.warn("NEWVOUCHER trigger: entity not found for number {}", iNumber);
                return true;
            }
            iVoucher = optVoucher.get();
            if (!iVouchers.contains(iVoucher)) {
                iVouchers.add(iVoucher);
            }
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("EDITVOUCHER")) {
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

            Optional<SSVoucher> optVoucher = Repositories.vouchers().findVoucher(iVoucher);
            if (optVoucher.isEmpty()) {
                LOG.warn("EDITVOUCHER trigger: entity not found for number {}", iNumber);
                return true;
            }
            iVoucher = optVoucher.get();
            int iIndex = iVouchers.lastIndexOf(iVoucher);

            if (iIndex == -1) {
                return true;
            }
            iVouchers.remove(iIndex);
            iVouchers.add(iIndex, iVoucher);
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }

        if (iTriggerName.equals("DELETEVOUCHER")) {
            SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

            iVouchers.remove(iVoucher);
            if (SSVoucherFrame.getInstance() != null) {
                SSVoucherFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }

    public boolean handleReportTriggers(String iTriggerName, String iTableName, String iNumber) {
        if (iTriggerName == null) {
            return false;
        }
        if (!isAnyTrigger(iTriggerName, "NEWOWNREPORT", "EDITOWNREPORT", "DELETEOWNREPORT")) {
            return false;
        }
        if (iOwnReports == null) {
            // Preserve previous behavior: report triggers were ignored until the cache list was initialized.
            return true;
        }
        if (iTriggerName.equals("NEWOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();

            iOwnReport.setId(Integer.parseInt(iNumber));
            Optional<SSOwnReport> optOwnReport = Repositories.ownReports().findByOwnReport(iOwnReport);
            if (optOwnReport.isEmpty()) {
                LOG.warn("NEWOWNREPORT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOwnReport = optOwnReport.get();
            if (!iOwnReports.contains(iOwnReport) && iOwnReport.getId() != -1) {
                iOwnReports.add(iOwnReport);
            }
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.equals("EDITOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();

            iOwnReport.setId(Integer.parseInt(iNumber));
            Optional<SSOwnReport> optOwnReport = Repositories.ownReports().findByOwnReport(iOwnReport);
            if (optOwnReport.isEmpty()) {
                LOG.warn("EDITOWNREPORT trigger: entity not found for number {}", iNumber);
                return true;
            }
            iOwnReport = optOwnReport.get();
            int iIndex = iOwnReports.lastIndexOf(iOwnReport);

            if (iIndex != -1) {
                iOwnReports.remove(iIndex);
                iOwnReports.add(iIndex, iOwnReport);
            } else {
                iOwnReports.add(iOwnReport);
            }
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        if (iTriggerName.equals("DELETEOWNREPORT")) {
            SSOwnReport iOwnReport = new SSOwnReport();

            iOwnReport.setId(Integer.parseInt(iNumber));
            iOwnReports.remove(iOwnReport);
            if (SSOwnReportFrame.getInstance() != null) {
                SSOwnReportFrame.getInstance().updateFrame();
            }
            return true;
        }
        return true;
    }

    private boolean isAnyTrigger(String iTriggerName, String... iTriggers) {
        if (iTriggerName == null) {
            return false;
        }
        for (String iTrigger : iTriggers) {
            if (iTriggerName.equals(iTrigger)) {
                return true;
            }
        }
        return false;
    }

    private void executeLegacyTriggerAction(String iTriggerName, String iTableName, String iNumber) {
        boolean iPrevious = iBypassTriggerDispatcher.get();
        iBypassTriggerDispatcher.set(true);
        try {
            triggerAction(iTriggerName, iTableName, iNumber);
        } finally {
            iBypassTriggerDispatcher.set(iPrevious);
        }
    }

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

    private void applyInpaymentSaldoDelta(SSInpayment iInpayment, boolean iAddToSaldo) {
        // Transitional adapter while trigger orchestration still lives in SSDB.
        SaldoDeltaService.applyInpaymentDelta(iInpayment, iAddToSaldo);
    }


    private void applyOutpaymentSaldoDelta(SSOutpayment iOutpayment, boolean iAddToSaldo) {
        // Transitional adapter while trigger orchestration still lives in SSDB.
        SaldoDeltaService.applyOutpaymentDelta(iOutpayment, iAddToSaldo);
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

    public void createLocalTriggers() {
        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
            builder.createLocalTriggers();
        } catch (SQLException e) {
            LOG.debug("createLocalTriggers encountered: {}", e.getMessage());
        }
    }

    public void createTriggers() {
        createLocalTriggers();
    }

    public void dropTriggers() {
        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
            builder.dropTriggers();
        } catch (SQLException e) {
            LOG.debug("dropTriggers encountered: {}", e.getMessage());
        }
    }

    private String getSchemaResource() {
        return "sql/create_tables_v2.sql";
    }

    public void createNewTables() {
        if (iConnection == null) {
            return;
        }
        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
            builder.createBaseTables();
            iConnection.commit();
        } catch (SQLException e) {
            LOG.error("Unexpected error in createNewTables", e);
        }
    }

    private void ensureAccountingYearSnapshotColumnsV2() throws SQLException {
        ensureColumnExistsV2("tbl_accountingyear", "accountplan", "CLOB");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_schema_version", "INTEGER DEFAULT 1");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_compression_flag", "VARCHAR(10) DEFAULT 'gzip'");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_checksum", "VARCHAR(64)");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_snapshot_version", "INTEGER DEFAULT 0");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_updated_at", "TIMESTAMP");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_updated_by", "VARCHAR(100)");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_name", "VARCHAR(256)");
        ensureColumnExistsV2("tbl_accountingyear", "accountplan_dirty_flag", "BOOLEAN DEFAULT FALSE");
    }

    private void ensureCompanyMailServerColumnsV2() throws SQLException {
        ensureColumnExistsV2("tbl_company", "smtp_name", "VARCHAR(255)");
        ensureColumnExistsV2("tbl_company", "smtp_port", "INTEGER");
        ensureColumnExistsV2("tbl_company", "smtp_bcc_addresses", "VARCHAR(1000)");
        ensureColumnExistsV2("tbl_company", "smtp_auth", "BOOLEAN DEFAULT FALSE");
        ensureColumnExistsV2("tbl_company", "smtp_connection_security", "VARCHAR(20)");
        ensureColumnExistsV2("tbl_company", "smtp_username", "VARCHAR(255)");
        ensureColumnExistsV2("tbl_company", "smtp_password", "VARCHAR(1024)");
    }

    private void ensureProductQuantityColumnsV2() throws SQLException {
        ensureColumnExistsV2("tbl_product", "only_whole_quantity", "BOOLEAN DEFAULT FALSE");
    }

    private void ensureProductParcelRowsTableV2() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS tbl_product_row ("
                + "product_id INTEGER NOT NULL,"
                + "row_index INTEGER NOT NULL,"
                + "product_nr VARCHAR(50),"
                + "description VARCHAR(500),"
                + "quantity INTEGER,"
                + "CONSTRAINT pk_product_row PRIMARY KEY (product_id, row_index),"
                + "CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES tbl_product(id) ON DELETE CASCADE"
                + ")";
        try (PreparedStatement iStatement = iConnection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }
    }

    public void ensureQuantityScaleMigrationV2() throws SQLException {
        final String migrationKey = "quantity_scale_x10_v2";

        ensureSchemaMigrationTableV2();

        if (isSchemaMigrationAppliedV2(migrationKey)) {
            return;
        }

        scaleIntegerColumnByTenV2("tbl_invoice_row", "count");
        scaleIntegerColumnByTenV2("tbl_creditinvoice_row", "count");
        scaleIntegerColumnByTenV2("tbl_periodicinvoice_row", "count");
        scaleIntegerColumnByTenV2("tbl_order_row", "count");
        scaleIntegerColumnByTenV2("tbl_tender_row", "count");
        scaleIntegerColumnByTenV2("tbl_purchaseorder_row", "quantity");
        scaleIntegerColumnByTenV2("tbl_supplierinvoice_row", "quantity");
        scaleIntegerColumnByTenV2("tbl_suppliercreditinvoice_row", "quantity");
        scaleIntegerColumnByTenV2("tbl_inventory_row", "quantity");
        scaleIntegerColumnByTenV2("tbl_inventory_row", "change_qty");
        scaleIntegerColumnByTenV2("tbl_indelivery_row", "change_qty");
        scaleIntegerColumnByTenV2("tbl_outdelivery_row", "change_qty");

        markSchemaMigrationAppliedV2(migrationKey);
    }

    private void ensureSchemaMigrationTableV2() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS tbl_schema_migration ("
                + "migration_key VARCHAR(128) PRIMARY KEY,"
                + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        try (PreparedStatement iStatement = iConnection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }
    }

    private boolean isSchemaMigrationAppliedV2(String migrationKey) throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT 1 FROM tbl_schema_migration WHERE migration_key=?")) {
            iStatement.setString(1, migrationKey);
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private void markSchemaMigrationAppliedV2(String migrationKey) throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "INSERT INTO tbl_schema_migration(migration_key) VALUES (?)")) {
            iStatement.setString(1, migrationKey);
            iStatement.executeUpdate();
        }
    }

    private void scaleIntegerColumnByTenV2(String tableName, String columnName) throws SQLException {
        if (!columnExistsV2(tableName, columnName)) {
            return;
        }

        String sql = "UPDATE " + tableName + " SET " + columnName + "=" + columnName + "*10 WHERE "
                + columnName + " IS NOT NULL";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.executeUpdate();
        }
    }

    private void ensureTemplateAccountTableV2() throws SQLException {
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
        try (PreparedStatement iStatement = iConnection.prepareStatement(ddl)) {
            iStatement.executeUpdate();
        }

        if (columnExistsV2("tbl_account", "accountplan_id")) {
            try (PreparedStatement iCopy = iConnection.prepareStatement(
                    "INSERT INTO tbl_accountplan_account(accountplan_id,number,description,sru_code,vat_code,report_code,active,project_required,result_unit_required) "
                            + "SELECT accountplan_id,number,description,sru_code,vat_code,report_code,active,project_required,result_unit_required "
                            + "FROM tbl_account WHERE accountplan_id IS NOT NULL")) {
                iCopy.executeUpdate();
            } catch (SQLException e) {
                LOG.warn("Template account migration skipped: {}", e.getMessage());
            }
        }
    }

    private void ensureYearOwnedAccountTableV2() throws SQLException {
        ensureColumnExistsV2("tbl_account", "accountingyear_id", "INTEGER");

        dropConstraintIfExistsV2("tbl_account", "fk_account_plan");
        dropConstraintIfExistsV2("tbl_account", "fk_account_year");

        if (columnExistsV2("tbl_account", "accountplan_id")) {
            try (PreparedStatement iDropColumn = iConnection.prepareStatement(
                    "ALTER TABLE tbl_account DROP COLUMN accountplan_id")) {
                iDropColumn.executeUpdate();
            } catch (SQLException e) {
                LOG.warn("Could not drop tbl_account.accountplan_id: {}", e.getMessage());
            }
        }

        try (PreparedStatement iAddFk = iConnection.prepareStatement(
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

        try (PreparedStatement iUnique = iConnection.prepareStatement(
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

    private void ensureSingleActiveAccountYearV2() throws SQLException {
        List<Integer> iYearIds = new ArrayList<>();
        try (PreparedStatement iStatement = iConnection.prepareStatement(
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

        try (PreparedStatement iDelete = iConnection.prepareStatement(
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
     * Ensures {@code fk_year_plan} on {@code tbl_accountingyear} is removed.
     * <p>
     * In the snapshot model the year owns a self-contained CLOB copy of its account plan.
     * The FK to {@code tbl_accountplan} must therefore not exist; years must be readable
     * even after a template plan has been deleted.  This method is idempotent.
     * </p>
     *
     * @throws SQLException if the DDL operation fails with an unexpected error
     */
    private void ensureDropYearPlanFkV2() throws SQLException {
        try (PreparedStatement iDrop = iConnection.prepareStatement(
                "ALTER TABLE tbl_accountingyear DROP CONSTRAINT fk_year_plan")) {
            iDrop.executeUpdate();
            iConnection.commit();
            LOG.info("Dropped fk_year_plan from tbl_accountingyear (snapshot model migration)");
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (msg.contains("not found") || msg.contains("does not exist")
                    || msg.contains("no constraint") || msg.contains("cannot find")) {
                // Already absent ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â nothing to do.
                return;
            }
            LOG.warn("Could not drop fk_year_plan (unexpected): {}", e.getMessage());
        }
    }

    private void ensureColumnExistsV2(String tableName, String columnName, String columnDefinition) throws SQLException {
        String ddl = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
        try (PreparedStatement iStatement = iConnection.prepareStatement(ddl)) {
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

    private boolean columnExistsV2(String tableName, String columnName) {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.setString(1, tableName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, columnName.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        } catch (SQLException e) {
            LOG.warn("Could not inspect column {}.{}: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    private void dropConstraintIfExistsV2(String tableName, String constraintName) throws SQLException {
        try (PreparedStatement iDrop = iConnection.prepareStatement(
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






