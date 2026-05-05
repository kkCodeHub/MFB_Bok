package se.swedsoft.bookkeeping.data.system;


import se.swedsoft.bookkeeping.SSTriggerHandler;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.calc.math.*;
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.rmi.server.UID;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Optional;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * $Id$
 */
public class SSDB {    private static final Logger LOG = LoggerFactory.getLogger(SSDB.class);

    private static final String SCHEMA_VERSION_PROPERTY = "fribok.schema.version";
    private static final String SCHEMA_V2 = "v2";


    // The instance of the database
    private static SSDB cInstance;

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

    /**
     * Returns the instance of the database
     *
     * @return the database
     */
    public static SSDB getInstance() {
        if (cInstance == null) {
            cInstance = new SSDB();
        }
        return cInstance;
    }

    public static final Object iSyncObject = new Object();

    Connection iConnection;

    // Listeners
    private Map<String, List<PropertyChangeListener>> iListenerMap;

    private SSDB() {
        iListenerMap = new HashMap<>();
    }

    /**
     *
     * @param pConnection
     *
     * @throws SQLException
     */
    public void startupLocal(Connection pConnection) throws SQLException {
        iConnection = pConnection;
        iConnection.setAutoCommit(false);

        createNewTables();
        // dropTriggers();
        createLocalTriggers();

        if (useSchemaV2()) {
            LOG.info("startupLocal running in schema V2 mode; skipping legacy seed/import and last-company restore");
            return;
        }

        checkCreateExampleCompany();
        checkImportDefaultAccountPlans();

        // Läs in företaget och året som senast var öppet.
        Integer iLastCompany = SSDBConfig.getCompanyId();
        Integer iLastYear = SSDBConfig.getYearId();

        ResultSet iResultSet;
        PreparedStatement iStatement;

        if (iLastCompany != null) {
            iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_company WHERE id=?");
            iStatement.setObject(1, iLastCompany);
            iResultSet = iStatement.executeQuery();
            if (iResultSet.next()) {
                try {
                    SSNewCompany iCompany = (SSNewCompany) iResultSet.getObject("company");
                    setCurrentCompany(iCompany);
                } catch (RuntimeException e) {
                    LOG.warn("Could not restore last company (possible HSQLDB format mismatch): {}",
                            e.getMessage());
                }
            }
            iResultSet.close();
            iStatement.close();
        }

        if (iLastYear != null && iCurrentCompany != null) {
            iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_accountingyear WHERE id=?");
            iStatement.setObject(1, iLastYear);
            iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                try {
                    SSNewAccountingYear iYear = (SSNewAccountingYear) iResultSet.getObject(
                            "accountingyear");
                    setCurrentYear(iYear);
                } catch (RuntimeException e) {
                    LOG.warn("Could not restore last year (possible HSQLDB format mismatch): {}",
                            e.getMessage());
                }
            }
            iResultSet.close();
            iStatement.close();
        }
    }

    public void init(boolean iShowDialog) {
        if (iCurrentCompany == null) {
            return;
        }

        if (iShowDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> {

                            getProducts();
                            getCustomers();
                            getSuppliers();
                            getAutoDists();

                            getInpayments();
                            getTenders();
                            getOrders();
                            getInvoices();
                            getCreditInvoices();
                            getPeriodicInvoices();

                            getOutpayments();
                            getPurchaseOrders();
                            getSupplierInvoices();
                            getSupplierCreditInvoices();

                            getInventories();
                            getIndeliveries();
                            getOutdeliveries();

                            getOwnReports();

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

                        });
        } else {
            getProducts();
            getCustomers();
            getSuppliers();
            getAutoDists();

            getInpayments();
            getTenders();
            getOrders();
            getInvoices();
            getCreditInvoices();
            getPeriodicInvoices();

            getOutpayments();
            getPurchaseOrders();
            getSupplierInvoices();
            getSupplierCreditInvoices();

            getInventories();
            getIndeliveries();
            getOutdeliveries();

            getOwnReports();

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

    }

    public void initYear(boolean iShowLoadingDialog) {
        if (iCurrentYear == null) {
            return;
        }

        iVouchers = null;
        getCurrentYear();

        if (iShowLoadingDialog) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> getVouchers());
        } else {
            getVouchers();
        }

    }

    public void shutdown() {
        try {
            if (!iConnection.isClosed()) {
                Statement iStatement = iConnection.createStatement();

                iStatement.executeQuery("SHUTDOWN");
                iStatement.close();
                iConnection.close();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public void shutdownCompact() {
        try {
            Statement iStatement = iConnection.createStatement();

            iStatement.executeQuery("SHUTDOWN COMPACT");
            iStatement.close();
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

    /* skapa exempelföretaget i databasen */
    private void checkCreateExampleCompany() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return;
            }

            Statement iStatement = iConnection.createStatement();
            ResultSet iResultSet = iStatement.executeQuery("SELECT 0 FROM tbl_company");
            if (iResultSet.next()) {
                // Have at least one company in DB
                iStatement.close();
                return;
            }

            LOG.info("Creating example company.");

            String q = SSUtil.readResourceToString("sql/example.sql");

            iStatement.executeUpdate(q);
            iConnection.commit();
            iStatement.close();
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

            Statement iStatement = iConnection.createStatement();
            ResultSet iResultSet = iStatement.executeQuery("SELECT 0 FROM tbl_accountplan");
            if (iResultSet.next()) {
                // Have at least one account plan in DB. Dont import defaults
                iStatement.close();
                return;
            }
            iStatement.close();

            LOG.info("Creating default account plans.");

            String[] defaults = new String[]{
                "BAS96(07)-AB & EF.xls",
                "BAS96(07)-Enskild-naringsidkare.xls",
                "BAS96(07)-HB & KB.xls",
                "Bas2006(07)-AB & EF.xls",
                "Bas2006(07)-Enskild-naringsidkare.xls",
                "Bas2006(07)-HB & KB.xls",
                "Bas2007(K1)-Enskild-naringsidkare.xls",};

            for (String s : defaults) {
                LOG.info(s);
                String path = "account/default/" + s;
                InputStream is = SSDB.class.getClassLoader().getResourceAsStream(path);
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

    public void restart() {}

    public void delete() {
        try {
            PreparedStatement iStatement = iConnection.prepareStatement("SHUTDOWN");

            iStatement.executeUpdate();
            iStatement.close();
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

    public void clear() {}

    public void clearLists() {
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
        if (useSchemaV2()) {
            iCurrentCompany = iCompany;
        } else {
            iCurrentCompany = getCompany(iCompany).orElse(null);
        }
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
        if (!useSchemaV2()) {
            iCurrentCompany = getCompany(iCurrentCompany).orElse(null);
        }
        return iCurrentCompany;
    }

    public void setCurrentYear(SSNewAccountingYear iYear) {
        iCurrentYear = iYear;
        iVouchers = null;
        notifyListeners("YEAR", iCurrentYear, null);
    }

    public SSNewAccountingYear getCurrentYear() {
        return getAccountingYear(iCurrentYear).orElse(null);
    }

    public List<SSNewCompany> getCompanies() {
        List<SSNewCompany> iCompanies = null;

        try {
            iCompanies = new LinkedList<>();

            if (iConnection == null || iConnection.isClosed()) {
                return iCompanies;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_company");
            ResultSet iResultSet = iStatement.executeQuery();

            if (useSchemaV2()) {
                while (iResultSet.next()) {
                    SSNewCompany iCompany = new SSNewCompany();
                    iCompany.setId(iResultSet.getInt("id"));
                    iCompany.setName(iResultSet.getString("name"));
                    iCompanies.add(iCompany);
                }
                iResultSet.close();
                iStatement.close();
                return iCompanies;
            }

            while (iResultSet.next()) {
                try {
                    iCompanies.add((SSNewCompany) iResultSet.getObject("company"));
                } catch (RuntimeException e) {
                    LOG.warn("Skipping company row with unreadable data (HSQLDB format mismatch): {}",
                            e.getMessage());
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iCompanies;
    }

    public Optional<SSNewCompany> getCompany(SSNewCompany pCompany) {
        try {
            if (pCompany == null || iConnection.isClosed()) {
                return Optional.empty();
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_company WHERE id=?");

            iStatement.setObject(1, pCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                if (useSchemaV2()) {
                    SSNewCompany iCompany = new SSNewCompany();
                    iCompany.setId(iResultSet.getInt("id"));
                    iCompany.setName(iResultSet.getString("name"));
                    iResultSet.close();
                    iStatement.close();
                    return Optional.of(iCompany);
                }

                SSNewCompany iCompany = (SSNewCompany) iResultSet.getObject("company");

                iResultSet.close();
                iStatement.close();
                return Optional.of(iCompany);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addCompany(SSNewCompany iCompany) {
        if (iCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_company VALUES(NULL,?)");

            iStatement.setObject(1, iCompany);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement("SELECT * FROM tbl_company");
            ResultSet iResultSet = iStatement.executeQuery();
            Integer iId = -1;

            while (iResultSet.next()) {
                if (iResultSet.isLast()) {
                    iId = iResultSet.getInt("id");
                }
            }
            iResultSet.close();
            iStatement.close();
            iCompany.setId(iId);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.error("Unexpected error", e);
            }
            iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_company SET company=? WHERE id=?");
            iStatement.setObject(1, iCompany);
            iStatement.setObject(2, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateCompany(SSNewCompany iCompany) {
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_company SET company=? WHERE id=?");

            iStatement.setObject(1, iCompany);
            iStatement.setObject(2, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            notifyListeners("COMPANY", iCompany, null);

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteCompany(SSNewCompany iCompany) {
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_project WHERE companyid=?");

            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_resultunit WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_product WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_customer WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_supplier WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_vouchertemplate WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_autodist WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_inpayment WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_tender WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_order WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_invoice WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_creditinvoice WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_periodicinvoice WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_outpayment WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_purchaseorder WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_supplierinvoice WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_suppliercreditinvoice WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_inventory WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_indelivery WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_outdelivery WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_ownreport WHERE companyid=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            for (SSNewAccountingYear iYear : getYearsForCompany(iCompany)) {
                deleteAccountingYear(iYear);
            }

            iStatement = iConnection.prepareStatement("DELETE FROM tbl_company WHERE id=?");
            iStatement.setObject(1, iCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public List<SSNewAccountingYear> getYears() {
        List<SSNewAccountingYear> iYears = new LinkedList<>();

        if (iCurrentCompany != null) {
            try {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_accountingyear WHERE companyid=?");

                iStatement.setObject(1, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                while (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iYears.add(mapAccountingYearV2(iResultSet));
                    } else {
                        iYears.add(
                                (SSNewAccountingYear) iResultSet.getObject("accountingyear"));
                    }
                }
                iResultSet.close();
                iStatement.close();
            } catch (SQLException e) {
                LOG.error("Unexpected error", e);
                try {
                    iConnection.rollback();
                } catch (SQLException ignored) {}
                SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                        e.getMessage());
            }
        }
        return iYears;
    }

    public List<SSNewAccountingYear> getYearsForCompany(SSNewCompany iCompany) {
        List<SSNewAccountingYear> iYears = new LinkedList<>();

        if (iCompany != null) {
            try {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_accountingyear WHERE companyid=?");

                iStatement.setObject(1, iCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                while (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iYears.add(mapAccountingYearV2(iResultSet));
                    } else {
                        iYears.add(
                                (SSNewAccountingYear) iResultSet.getObject("accountingyear"));
                    }
                }
                iResultSet.close();
                iStatement.close();
            } catch (SQLException e) {
                LOG.error("Unexpected error", e);
                try {
                    iConnection.rollback();
                } catch (SQLException ignored) {}
                SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                        e.getMessage());
            }
        }
        return iYears;
    }

    public Optional<SSNewAccountingYear> getAccountingYear(SSNewAccountingYear pAccountingYear) {
        try {
            if (pAccountingYear == null) {
                return Optional.empty();
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_accountingyear WHERE id=?");

            iStatement.setObject(1, pAccountingYear.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSNewAccountingYear iAccountingYear;

                if (useSchemaV2()) {
                    iAccountingYear = mapAccountingYearV2(iResultSet);
                } else {
                    iAccountingYear = (SSNewAccountingYear) iResultSet.getObject(
                            "accountingyear");
                }

                iResultSet.close();
                iStatement.close();
                return Optional.of(iAccountingYear);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addAccountingYear(SSNewAccountingYear iAccountingYear) {
        if (iAccountingYear == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_accountingyear(companyid,from_date,to_date,accountplan_id) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);

                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, java.sql.Date.valueOf(iAccountingYear.getLocalFrom()));
                iStatement.setObject(3, java.sql.Date.valueOf(iAccountingYear.getLocalTo()));
                iStatement.setObject(4, iAccountingYear.getAccountPlan() == null
                        ? null : iAccountingYear.getAccountPlan().getId());
                iStatement.executeUpdate();

                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iAccountingYear.setId(iKeys.getInt(1));
                    }
                }

                iConnection.commit();
                iStatement.close();
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_accountingyear VALUES(NULL,?,?)");

            iStatement.setObject(1, iAccountingYear);
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement("SELECT * FROM tbl_accountingyear");
            ResultSet iResultSet = iStatement.executeQuery();
            Integer iId = -1;

            while (iResultSet.next()) {
                if (iResultSet.isLast()) {
                    iId = iResultSet.getInt("id");
                }
            }
            iAccountingYear.setId(iId);
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_accountingyear SET accountingyear=? WHERE id=?");
            iStatement.setObject(1, iAccountingYear);
            iStatement.setObject(2, iAccountingYear.getId());
            iStatement.executeUpdate();
            iConnection.commit();

            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateAccountingYear(SSNewAccountingYear iAccountingYear) {
        if (iAccountingYear == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_accountingyear SET from_date=?,to_date=?,accountplan_id=? WHERE id=?");

                iStatement.setObject(1, java.sql.Date.valueOf(iAccountingYear.getLocalFrom()));
                iStatement.setObject(2, java.sql.Date.valueOf(iAccountingYear.getLocalTo()));
                iStatement.setObject(3, iAccountingYear.getAccountPlan() == null
                        ? null : iAccountingYear.getAccountPlan().getId());
                iStatement.setObject(4, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();

                if (iAccountingYear.equals(iCurrentYear)) {
                    iCurrentYear = iAccountingYear;
                    notifyListeners("YEAR", iAccountingYear, null);
                }
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_accountingyear SET accountingyear=? WHERE id=?");

            iStatement.setObject(1, iAccountingYear);
            iStatement.setObject(2, iAccountingYear.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            if (iAccountingYear.equals(iCurrentYear)) {
                iCurrentYear = iAccountingYear;
                notifyListeners("YEAR", iAccountingYear, null);
            }

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteAccountingYear(SSNewAccountingYear iAccountingYear) {
        if (iAccountingYear == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_voucher_row WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();

                iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_voucher WHERE yearid=?");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();

                iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_year_balance WHERE year_id=?");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();

                iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_budget_row WHERE year_id=?");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();

                iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_accountingyear WHERE id=?");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_voucher WHERE yearid=?");

            iStatement.setObject(1, iAccountingYear.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_accountingyear WHERE id=?");
            iStatement.setObject(1, iAccountingYear.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    private SSNewAccountingYear mapAccountingYearV2(ResultSet iResultSet) throws SQLException {
        SSNewAccountingYear iAccountingYear = new SSNewAccountingYear();
        iAccountingYear.setId(iResultSet.getInt("id"));

        java.sql.Date iFromDate = iResultSet.getDate("from_date");
        if (iFromDate != null) {
            iAccountingYear.setLocalFrom(iFromDate.toLocalDate());
        }

        java.sql.Date iToDate = iResultSet.getDate("to_date");
        if (iToDate != null) {
            iAccountingYear.setLocalTo(iToDate.toLocalDate());
        }

        Integer iAccountPlanId = (Integer) iResultSet.getObject("accountplan_id");
        if (iAccountPlanId != null) {
            SSAccountPlan iPlanProbe = new SSAccountPlan();
            iPlanProbe.setId(iAccountPlanId);
            getAccountPlan(iPlanProbe).ifPresent(iAccountingYear::setAccountPlan);
        }

        return iAccountingYear;
    }

    public Optional<SSNewAccountingYear> getPreviousYear() {
        iCurrentYear = getCurrentYear();
        if (iCurrentYear == null) {
            return Optional.empty();
        }
        List<SSNewAccountingYear> iYears = getYears();

        java.time.LocalDate iFirstDayOfCurrent = iCurrentYear.getLocalFrom();

        // Get the last day of the previous year (day before the current year starts)
        java.time.LocalDate dayBeforeCurrent = iFirstDayOfCurrent.minusDays(1);

        for (SSNewAccountingYear iAccountingYear : iYears) {
            java.time.LocalDate lastDayOfYear = iAccountingYear.getLocalTo();
            if (dayBeforeCurrent.equals(lastDayOfYear)) {
                return Optional.of(iAccountingYear);
            }
        }
        return Optional.empty();
    }

    public Optional<SSNewAccountingYear> getLastYear() {
        List<SSNewAccountingYear> iYears = new LinkedList<>();

        if (iCurrentCompany != null) {
            try {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_accountingyear WHERE companyid=?");

                iStatement.setObject(1, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                while (iResultSet.next()) {
                    iYears.add(
                            (SSNewAccountingYear) iResultSet.getObject("accountingyear"));
                }
                iResultSet.close();
                iStatement.close();

                java.time.LocalDate iLastDate = null;
                SSNewAccountingYear iAccountingYear = null;

                for (SSNewAccountingYear iYear : iYears) {
                    if (iLastDate == null) {
                        iLastDate = iYear.getLocalTo();
                        iAccountingYear = iYear;
                    }
                    java.time.LocalDate iTo = iYear.getLocalTo();

                    if (iTo.isAfter(iLastDate)) {
                        iLastDate = iTo;
                        iAccountingYear = iYear;
                    }
                }
                return Optional.ofNullable(iAccountingYear);
            } catch (SQLException e) {
                LOG.error("Unexpected error", e);
                try {
                    iConnection.rollback();
                } catch (SQLException ignored) {}
                SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                        e.getMessage());
            }
        }
        return Optional.empty();
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
        List<PropertyChangeListener> iPropertyChangeListeners = iListenerMap.get(pProperty);

        if (iPropertyChangeListeners == null) {
            iPropertyChangeListeners = new LinkedList<>();

            iListenerMap.put(pProperty, iPropertyChangeListeners);
        }

        iPropertyChangeListeners.add(pPropertyChangeListener);
    }

    /**
     *
     * @param pProperty
     * @param pNewValue
     * @param pOldValue
     */
    public void notifyListeners(String pProperty, Object pNewValue, Object pOldValue) {

        List<PropertyChangeListener> iPropertyChangeListeners = iListenerMap.get(pProperty);

        if (iPropertyChangeListeners == null) {
            return;
        }

        PropertyChangeEvent iPropertyChangeEvent = new PropertyChangeEvent(this, pProperty,
                pOldValue, pNewValue);

        for (PropertyChangeListener iPropertyChangeListener : iPropertyChangeListeners) {
            iPropertyChangeListener.propertyChange(iPropertyChangeEvent);
        }
    }

    public Optional<SSAutoIncrement> getAutoIncrement() {
        return Optional.empty();
    }

    public List<SSVoucher> getVouchers() {
        List<SSVoucher> cached = iVouchers;
        if (cached != null) {
            return cached;
        }
        // Build the list into a local variable so that a concurrent call to
        // initYear() (which nullifies iVouchers) cannot cause a NullPointerException
        // while we are still iterating the ResultSet.
        List<SSVoucher> freshList = new LinkedList<>();
        if (iCurrentYear == null) {
            iVouchers = freshList;
            return freshList;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_voucher WHERE yearid=? AND id>?");
                iStatement.setObject(1, iCurrentYear.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        freshList.add(mapVoucherV2(iResultSet));
                    } else {
                        freshList.add((SSVoucher) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        iVouchers = freshList;
        return iVouchers;
    }

    public List<SSVoucher> getVouchers(SSNewAccountingYear iAccountingYear) {
        List<SSVoucher> iVoucherList = new LinkedList<>();

        if (iAccountingYear == null) {
            return iVoucherList;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_voucher WHERE yearid=? AND id>?");
                iStatement.setObject(1, iAccountingYear.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iVoucherList.add(mapVoucherV2(iResultSet));
                    } else {
                        iVoucherList.add((SSVoucher) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iVoucherList;
    }

    public Optional<SSVoucher> getVoucher(SSVoucher pVoucher) {
        if (pVoucher == null || iCurrentYear == null) {
            return Optional.empty();
        }

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_voucher WHERE number=? AND yearid=?");

            iStatement.setObject(1, pVoucher.getNumber());
            iStatement.setObject(2, iCurrentYear.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSVoucher iVoucher = useSchemaV2()
                        ? mapVoucherV2(iResultSet)
                        : (SSVoucher) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iVoucher);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSVoucher> getVoucher(SSNewAccountingYear iAccountingYear, int iNumber) {
        if (iAccountingYear == null) {
            return Optional.empty();
        }

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_voucher WHERE number=? AND yearid=?");

            iStatement.setObject(1, iNumber);
            iStatement.setObject(2, iAccountingYear.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSVoucher iVoucher = useSchemaV2()
                        ? mapVoucherV2(iResultSet)
                        : (SSVoucher) iResultSet.getObject(3);
                iResultSet.close();
                iStatement.close();
                return Optional.of(iVoucher);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSVoucher> getVouchers(List<SSVoucher> pVouchers) {
        if (pVouchers == null || iCurrentYear == null) {
            return Collections.emptyList();
        }
        List<SSVoucher> iVouchers = new LinkedList<>();

        try {
            for (SSVoucher iVoucher : pVouchers) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_voucher WHERE number=? AND yearid=?");

                iStatement.setObject(1, iVoucher.getNumber());
                iStatement.setObject(2, iCurrentYear.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iVouchers.add(mapVoucherV2(iResultSet));
                    } else {
                        iVouchers.add((SSVoucher) iResultSet.getObject("voucher"));
                    }
                }
                iStatement.close();
            }

            return iVouchers;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addVoucher(SSVoucher iVoucher, boolean iHasNumber) {
        if (iVoucher == null || iCurrentYear == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (!iHasNumber) {
                iStatement = iConnection.prepareStatement(
                        "SELECT MAX(number) AS maxnum FROM tbl_voucher WHERE yearid=?");
                iStatement.setObject(1, iCurrentYear.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    Integer iNumber = iResultSet.getInt("maxnum");

                    iVoucher.setNumber(iNumber + 1);
                } else {
                    iVoucher.setNumber(1);
                }
                iResultSet.close();
                iStatement.close();
            }

            Integer iVoucherId = null;
            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_voucher(number,yearid,vdate,description,corrects_id,corrected_by_id) VALUES(?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                iStatement.setObject(1, iVoucher.getNumber());
                iStatement.setObject(2, iCurrentYear.getId());
                iStatement.setObject(3, java.sql.Date.valueOf(iVoucher.getLocalDate()));
                iStatement.setObject(4, iVoucher.getDescription());
                iStatement.setObject(5, getVoucherIdByNumberV2(iVoucher.getCorrects()));
                iStatement.setObject(6, getVoucherIdByNumberV2(iVoucher.getCorrectedBy()));
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_voucher VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iVoucher.getNumber());
                iStatement.setObject(2, iVoucher);
                iStatement.setObject(3, iCurrentYear.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iVoucherId = iKeys.getInt(1);
                    }
                }
                if (iVoucherId == null) {
                    iVoucherId = getVoucherIdV2(iVoucher.getNumber(), iCurrentYear.getId());
                }
                if (iVoucherId != null) {
                    replaceVoucherRowsV2(iVoucherId, iVoucher);
                }
            }

            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public Integer getLastVoucherNumber() {
        if (iCurrentYear == null) {
            return 0;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_voucher WHERE yearid=?");

            iStatement.setObject(1, iCurrentYear.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iNumber = 0;

            if (iResultSet.next()) {
                iNumber = iResultSet.getInt("maxnum");
            }
            iResultSet.close();
            iStatement.close();

            return iNumber;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return 0;
    }

    public void updateVoucher(SSVoucher iVoucher) {
        if (iVoucher == null || iCurrentYear == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iVoucherId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_voucher SET vdate=?,description=?,corrects_id=?,corrected_by_id=? WHERE number=? AND yearid=?");
                iStatement.setObject(1, java.sql.Date.valueOf(iVoucher.getLocalDate()));
                iStatement.setObject(2, iVoucher.getDescription());
                iStatement.setObject(3, getVoucherIdByNumberV2(iVoucher.getCorrects()));
                iStatement.setObject(4, getVoucherIdByNumberV2(iVoucher.getCorrectedBy()));
                iStatement.setObject(5, iVoucher.getNumber());
                iStatement.setObject(6, iCurrentYear.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_voucher SET voucher=? WHERE number=? AND yearid=?");

                iStatement.setObject(1, iVoucher);
                iStatement.setObject(2, iVoucher.getNumber());
                iStatement.setObject(3, iCurrentYear.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iVoucherId = getVoucherIdV2(iVoucher.getNumber(), iCurrentYear.getId());
                if (iVoucherId != null) {
                    replaceVoucherRowsV2(iVoucherId, iVoucher);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteVoucher(SSVoucher iVoucher) {
        if (iVoucher == null || iCurrentYear == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iVoucherId = getVoucherIdV2(iVoucher.getNumber(), iCurrentYear.getId());
                if (iVoucherId != null) {
                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_voucher_row WHERE voucher_id=?");
                    iDeleteRows.setObject(1, iVoucherId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_voucher WHERE number=? AND yearid=?");

            iStatement.setObject(1, iVoucher.getNumber());
            iStatement.setObject(2, iCurrentYear.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    private Integer getVoucherIdByNumberV2(SSVoucher iVoucher) throws SQLException {
        if (iVoucher == null || iCurrentYear == null) {
            return null;
        }
        return getVoucherIdV2(iVoucher.getNumber(), iCurrentYear.getId());
    }

    private Integer getVoucherIdV2(Integer iVoucherNumber, Integer iYearId) throws SQLException {
        if (iVoucherNumber == null || iYearId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_voucher WHERE number=? AND yearid=?");
        iStatement.setObject(1, iVoucherNumber);
        iStatement.setObject(2, iYearId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private Integer getVoucherNumberForIdV2(Integer iVoucherId) throws SQLException {
        if (iVoucherId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT number FROM tbl_voucher WHERE id=?");
        iStatement.setObject(1, iVoucherId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private List<SSVoucherRow> getVoucherRowsV2(Integer iVoucherId) throws SQLException {
        List<SSVoucherRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_voucher_row WHERE voucher_id=? ORDER BY id");
        iStatement.setObject(1, iVoucherId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSVoucherRow iRow = new SSVoucherRow();
            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRow.setDebet(iResultSet.getBigDecimal("debet"));
            iRow.setCredit(iResultSet.getBigDecimal("credit"));
            iRow.setEditedDate(iResultSet.getDate("edited_date"));
            iRow.setEditedSignature(iResultSet.getString("edited_signature"));
            iRow.setCrossed(iResultSet.getBoolean("crossed"));
            iRow.setAdded(iResultSet.getBoolean("added"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replaceVoucherRowsV2(Integer iVoucherId, SSVoucher iVoucher) throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_voucher_row WHERE voucher_id=?");
        iDelete.setObject(1, iVoucherId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSVoucherRow iRow : iVoucher.getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_voucher_row(voucher_id,account_nr,project_number,result_unit_number,debet,credit,edited_date,edited_signature,crossed,added) VALUES(?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iVoucherId);
            iInsert.setObject(2, iRow.getAccountNr());
            iInsert.setObject(3, iRow.getProjectNr());
            iInsert.setObject(4, iRow.getResultUnitNr());
            iInsert.setObject(5, iRow.getDebet());
            iInsert.setObject(6, iRow.getCredit());
            if (iRow.getEditedDate() == null) {
                iInsert.setNull(7, Types.DATE);
            } else {
                iInsert.setObject(7, new java.sql.Date(iRow.getEditedDate().getTime()));
            }
            iInsert.setObject(8, iRow.getEditedSignature());
            iInsert.setObject(9, iRow.isCrossed());
            iInsert.setObject(10, iRow.isAdded());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private SSVoucher mapVoucherV2(ResultSet iResultSet) throws SQLException {
        SSVoucher iVoucher = new SSVoucher(iResultSet.getInt("number"), true);
        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iVoucher.setLocalDate(iDate.toLocalDate());
        }
        iVoucher.setDescription(iResultSet.getString("description"));

        Integer iCorrectsId = (Integer) iResultSet.getObject("corrects_id");
        Integer iCorrectedById = (Integer) iResultSet.getObject("corrected_by_id");
        Integer iCorrectsNumber = getVoucherNumberForIdV2(iCorrectsId);
        Integer iCorrectedByNumber = getVoucherNumberForIdV2(iCorrectedById);
        if (iCorrectsNumber != null) {
            iVoucher.setCorrects(new SSVoucher(iCorrectsNumber, true));
        }
        if (iCorrectedByNumber != null) {
            iVoucher.setCorrectedBy(new SSVoucher(iCorrectedByNumber, true));
        }

        List<SSVoucherRow> iRows = getVoucherRowsV2(iResultSet.getInt("id"));
        iVoucher.getRows().clear();
        iVoucher.getRows().addAll(iRows);
        return iVoucher;
    }

    public List<SSVoucherTemplate> getVoucherTemplates() {
        List<SSVoucherTemplate> iVoucherTemplates = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iVoucherTemplates;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_vouchertemplate WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();
            int i = 0;

            while (iResultSet.next()) {
                iVoucherTemplates.add((SSVoucherTemplate) iResultSet.getObject(2));
                i++;
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iVoucherTemplates;
    }

    public List<SSVoucherTemplate> getVoucherTemplates(List<SSVoucherTemplate> pVoucherTemplates) {
        if (pVoucherTemplates == null) {
            return Collections.emptyList();
        }
        List<SSVoucherTemplate> iVoucherTemplates = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iVoucherTemplates;
        }
        try {
            for (SSVoucherTemplate iVoucherTemplate : pVoucherTemplates) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_vouchertemplate WHERE name=? AND companyid=?");

                iStatement.setObject(1, iVoucherTemplate.getDescription());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iVoucherTemplates.add((SSVoucherTemplate) iResultSet.getObject(2));
                }
                iStatement.close();
            }

            return iVoucherTemplates;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addVoucherTemplate(SSVoucherTemplate iVoucherTemplate) {
        if (iVoucherTemplate == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_vouchertemplate VALUES(?,?,?)");

            iStatement.setObject(1, iVoucherTemplate.getDescription());
            iStatement.setObject(2, iVoucherTemplate);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteVoucherTemplate(SSVoucherTemplate iVoucherTemplate) {
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_vouchertemplate WHERE name=? AND companyid=?");

            iStatement.setObject(1, iVoucherTemplate.getDescription());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public List<SSAccount> getAccounts() {
        return iCurrentYear == null
                ? new LinkedList<>()
                : iCurrentYear.getAccounts();
    }

    /**
     * Retuns the account plan for the current year
     *
     * @return the acoount plan for the current year
     */
    public SSAccountPlan getCurrentAccountPlan() {

        if (iCurrentYear != null) {
            return iCurrentYear.getAccountPlan();
        }
        return new SSAccountPlan("Default");
    }

    public List<SSAccountPlan> getAccountPlans() {
        List<SSAccountPlan> iAccountPlans = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_accountplan");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                if (useSchemaV2()) {
                    iAccountPlans.add(mapAccountPlanV2(iResultSet));
                } else {
                    iAccountPlans.add((SSAccountPlan) iResultSet.getObject("accountplan"));
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iAccountPlans;
    }

    public Optional<SSAccountPlan> getAccountPlan(SSAccountPlan pAccountPlan) {
        if (pAccountPlan == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_accountplan WHERE id=?");

            iStatement.setObject(1, pAccountPlan.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSAccountPlan iAccountPlan;
                if (useSchemaV2()) {
                    iAccountPlan = mapAccountPlanV2(iResultSet);
                } else {
                    iAccountPlan = (SSAccountPlan) iResultSet.getObject("accountplan");
                }

                iStatement.close();
                return Optional.of(iAccountPlan);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addAccountPlan(SSAccountPlan iAccountPlan) {
        if (iAccountPlan == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_accountplan(name,base_name,assessment_year,plan_type) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);

                iStatement.setObject(1, iAccountPlan.getName());
                iStatement.setObject(2, iAccountPlan.getBaseName());
                iStatement.setObject(3, iAccountPlan.getAssessementYear());
                iStatement.setObject(4, iAccountPlan.getType() == null ? null : iAccountPlan.getType().getName());
                iStatement.executeUpdate();

                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iAccountPlan.setId(iKeys.getInt(1));
                    }
                }

                if (iAccountPlan.getId() != null) {
                    replaceAccountRowsV2(iAccountPlan.getId(), iAccountPlan);
                }

                iConnection.commit();
                iStatement.close();
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_accountplan VALUES(NULL,?)");

            iStatement.setObject(1, iAccountPlan);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement("SELECT * FROM tbl_accountplan");
            ResultSet iResultSet = iStatement.executeQuery();
            Integer iId = -1;

            while (iResultSet.next()) {
                if (iResultSet.isLast()) {
                    iId = iResultSet.getInt("id");
                }
            }
            iAccountPlan.setId(iId);
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_accountplan SET accountplan=? WHERE id=?");
            iStatement.setObject(1, iAccountPlan);
            iStatement.setObject(2, iAccountPlan.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateAccountPlan(SSAccountPlan iAccountPlan) {
        if (iAccountPlan == null) {
            return;
        }

        try {
            if (useSchemaV2()) {
                Integer iPlanId = iAccountPlan.getId();
                if (iPlanId == null) {
                    iPlanId = resolveAccountPlanIdByNameV2(iAccountPlan.getName());
                    iAccountPlan.setId(iPlanId);
                }

                PreparedStatement iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_accountplan SET name=?,base_name=?,assessment_year=?,plan_type=? WHERE id=?");

                iStatement.setObject(1, iAccountPlan.getName());
                iStatement.setObject(2, iAccountPlan.getBaseName());
                iStatement.setObject(3, iAccountPlan.getAssessementYear());
                iStatement.setObject(4, iAccountPlan.getType() == null ? null : iAccountPlan.getType().getName());
                iStatement.setObject(5, iPlanId);
                iStatement.executeUpdate();

                if (iPlanId != null) {
                    replaceAccountRowsV2(iPlanId, iAccountPlan);
                }

                iConnection.commit();
                iStatement.close();
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_accountplan SET accountplan=? WHERE id=?");

            iStatement.setObject(1, iAccountPlan);
            iStatement.setObject(2, iAccountPlan.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteAccountPlan(SSAccountPlan iAccountPlan) {
        if (iAccountPlan == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iPlanId = iAccountPlan.getId();
                if (iPlanId == null) {
                    iPlanId = resolveAccountPlanIdByNameV2(iAccountPlan.getName());
                }
                if (iPlanId == null) {
                    return;
                }

                PreparedStatement iDeleteAccounts = iConnection.prepareStatement(
                        "DELETE FROM tbl_account WHERE accountplan_id=?");
                iDeleteAccounts.setObject(1, iPlanId);
                iDeleteAccounts.executeUpdate();
                iDeleteAccounts.close();

                PreparedStatement iStatement = iConnection.prepareStatement(
                        "DELETE FROM tbl_accountplan WHERE id=?");
                iStatement.setObject(1, iPlanId);
                iStatement.executeUpdate();
                iConnection.commit();
                iStatement.close();
                return;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_accountplan WHERE id=?");

            iStatement.setObject(1, iAccountPlan.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    private SSAccountPlan mapAccountPlanV2(ResultSet iResultSet) throws SQLException {
        SSAccountPlan iAccountPlan = new SSAccountPlan();
        iAccountPlan.setId(iResultSet.getInt("id"));
        iAccountPlan.setName(iResultSet.getString("name"));
        iAccountPlan.setBaseName(iResultSet.getString("base_name"));
        iAccountPlan.setAssessementYear(iResultSet.getString("assessment_year"));

        String iPlanType = iResultSet.getString("plan_type");
        if (iPlanType != null) {
            iAccountPlan.setType(iPlanType);
        }

        iAccountPlan.setAccounts(loadAccountsForPlanV2(iAccountPlan.getId()));
        return iAccountPlan;
    }

    private List<SSAccount> loadAccountsForPlanV2(Integer iPlanId) throws SQLException {
        List<SSAccount> iAccounts = new LinkedList<>();
        if (iPlanId == null) {
            return iAccounts;
        }

        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_account WHERE accountplan_id=? ORDER BY number");
        iStatement.setObject(1, iPlanId);
        ResultSet iResultSet = iStatement.executeQuery();

        try {
            while (iResultSet.next()) {
                SSAccount iAccount = new SSAccount();
                iAccount.setNumber(iResultSet.getInt("number"));
                iAccount.setDescription(iResultSet.getString("description"));
                iAccount.setSRUCode(iResultSet.getString("sru_code"));
                iAccount.setVATCode(iResultSet.getString("vat_code"));
                iAccount.setReportCode(iResultSet.getString("report_code"));
                iAccount.setActive(iResultSet.getBoolean("active"));
                iAccount.setProjectRequired(iResultSet.getBoolean("project_required"));
                iAccount.setResultUnitRequired(iResultSet.getBoolean("result_unit_required"));
                iAccounts.add(iAccount);
            }
            return iAccounts;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private void replaceAccountRowsV2(Integer iPlanId, SSAccountPlan iAccountPlan) throws SQLException {
        if (iPlanId == null || iAccountPlan == null) {
            return;
        }

        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_account WHERE accountplan_id=?");
        iDelete.setObject(1, iPlanId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSAccount iAccount : iAccountPlan.getAccounts()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_account(accountplan_id,number,description,sru_code,vat_code,report_code,active,project_required,result_unit_required) VALUES(?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iPlanId);
            iInsert.setObject(2, iAccount.getNumber());
            iInsert.setObject(3, iAccount.getDescription());
            iInsert.setObject(4, iAccount.getSRUCode());
            iInsert.setObject(5, iAccount.getVATCode());
            iInsert.setObject(6, iAccount.getReportCode());
            iInsert.setBoolean(7, iAccount.isActive());
            iInsert.setBoolean(8, iAccount.isProjectRequired());
            iInsert.setBoolean(9, iAccount.isResultUnitRequired());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer resolveAccountPlanIdByNameV2(String iPlanName) throws SQLException {
        if (iPlanName == null) {
            return null;
        }

        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_accountplan WHERE name=?");
        iStatement.setObject(1, iPlanName);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    public List<SSUnit> getUnits() {
        List<SSUnit> iUnits = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_unit");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iUnits.add((SSUnit) iResultSet.getObject("unit"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iUnits;
    }

    public void addUnit(SSUnit iUnit) {
        if (iUnit == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_unit VALUES(?,?)");

            iStatement.setObject(1, iUnit.getName());
            iStatement.setObject(2, iUnit);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateUnit(SSUnit iUnit) {
        if (iUnit == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_unit SET unit=? WHERE name=?");

            iStatement.setObject(1, iUnit);
            iStatement.setObject(2, iUnit.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteUnit(SSUnit iUnit) {
        if (iUnit == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_unit WHERE name=?");

            iStatement.setObject(1, iUnit.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    private List<SSSaleRow> getPeriodicInvoiceRowsV2(Integer iPeriodicInvoiceId) throws SQLException {
        List<SSSaleRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=? ORDER BY id");
        iStatement.setObject(1, iPeriodicInvoiceId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSSaleRow iRow = new SSSaleRow();
            iRow.setProductNr(iResultSet.getString("product_nr"));
            iRow.setDescription(iResultSet.getString("description"));
            iRow.setUnitprice(iResultSet.getBigDecimal("unitprice"));
            iRow.setQuantity((Integer) iResultSet.getObject("count"));

            String iUnit = iResultSet.getString("unit");
            if (iUnit != null) {
                iRow.setUnit(new SSUnit(iUnit, iUnit));
            }

            iRow.setDiscount(iResultSet.getBigDecimal("discount"));

            String iTaxCode = iResultSet.getString("tax_code");
            if (iTaxCode != null) {
                try {
                    iRow.setTaxCode(SSTaxCode.valueOf(iTaxCode));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from partial migrations.
                }
            }

            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replacePeriodicInvoiceRowsV2(Integer iPeriodicInvoiceId, SSPeriodicInvoice iPeriodicInvoice)
            throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=?");
        iDelete.setObject(1, iPeriodicInvoiceId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSSaleRow iRow : iPeriodicInvoice.getTemplate().getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_periodicinvoice_row(periodicinvoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iPeriodicInvoiceId);
            iInsert.setObject(2, iRow.getProductNr());
            iInsert.setObject(3, iRow.getDescription());
            iInsert.setObject(4, iRow.getUnitprice());
            iInsert.setObject(5, iRow.getQuantity());
            iInsert.setObject(6, iRow.getUnit() == null ? null : iRow.getUnit().getName());
            iInsert.setObject(7, iRow.getDiscount());
            iInsert.setObject(8, iRow.getTaxCode() == null ? null : iRow.getTaxCode().name());
            iInsert.setObject(9, iRow.getAccountNr());
            iInsert.setObject(10, iRow.getProjectNr());
            iInsert.setObject(11, iRow.getResultUnitNr());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer getPeriodicInvoiceIdV2(Integer iPeriodicInvoiceNumber, Integer iCompanyId)
            throws SQLException {
        if (iPeriodicInvoiceNumber == null || iCompanyId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_periodicinvoice WHERE number=? AND companyid=?");
        iStatement.setObject(1, iPeriodicInvoiceNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private int bindPeriodicInvoiceColumnsV2(PreparedStatement iStatement, int iIndex,
                                              SSPeriodicInvoice iPeriodicInvoice) throws SQLException {
        SSInvoice iTemplate = iPeriodicInvoice.getTemplate();

        bindLocalDateV2(iStatement, iIndex++, iPeriodicInvoice.getLocalDate());
        iStatement.setObject(iIndex++, iPeriodicInvoice.getCount());
        iStatement.setObject(iIndex++, iPeriodicInvoice.getPeriod());
        iStatement.setObject(iIndex++, iPeriodicInvoice.getDescription());
        bindLocalDateV2(iStatement, iIndex++, iPeriodicInvoice.getLocalPeriodStart());
        bindLocalDateV2(iStatement, iIndex++, iPeriodicInvoice.getLocalPeriodEnd());
        iStatement.setObject(iIndex++, iPeriodicInvoice.getAppendPeriod());
        iStatement.setObject(iIndex++, iPeriodicInvoice.isAppendInformation());
        iStatement.setObject(iIndex++, iPeriodicInvoice.getInformation());

        iStatement.setObject(iIndex++, iTemplate.getCustomerNr());
        iStatement.setObject(iIndex++, iTemplate.getCustomerName());
        iStatement.setObject(iIndex++, iTemplate.getOurContactPerson());
        iStatement.setObject(iIndex++, iTemplate.getYourContactPerson());
        iStatement.setObject(iIndex++, iTemplate.getDelayInterest());
        iStatement.setObject(iIndex++, getCurrencyCodeV2(iTemplate.getCurrency()));
        iStatement.setObject(iIndex++, iTemplate.getPaymentTerm() == null ? null : iTemplate.getPaymentTerm().getName());
        iStatement.setObject(iIndex++, iTemplate.getDeliveryTerm() == null ? null : iTemplate.getDeliveryTerm().getName());
        iStatement.setObject(iIndex++, iTemplate.getDeliveryWay() == null ? null : iTemplate.getDeliveryWay().getName());
        iStatement.setObject(iIndex++, iTemplate.getTaxFree());
        iStatement.setObject(iIndex++, iTemplate.getText());
        iStatement.setObject(iIndex++, iTemplate.isPrinted());
        iStatement.setObject(iIndex++, iTemplate.getCurrencyRate());
        bindLocalDateV2(iStatement, iIndex++, iTemplate.getLocalDueDate());
        iStatement.setObject(iIndex++, iTemplate.getYourOrderNumber());
        iStatement.setObject(iIndex++, iTemplate.isStockInfluencing());
        iIndex = bindAddressV2(iStatement, iIndex, iTemplate.getInvoiceAddress());
        return bindAddressV2(iStatement, iIndex, iTemplate.getDeliveryAddress());
    }

    private SSPeriodicInvoice mapPeriodicInvoiceV2(ResultSet iResultSet) throws SQLException {
        SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();
        iPeriodicInvoice.setNumber((Integer) iResultSet.getObject("number"));

        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iPeriodicInvoice.setLocalDate(iDate.toLocalDate());
        }

        iPeriodicInvoice.setCount((Integer) iResultSet.getObject("count"));
        iPeriodicInvoice.setPeriod((Integer) iResultSet.getObject("period"));
        iPeriodicInvoice.setDescription(iResultSet.getString("description"));

        java.sql.Date iPeriodStart = iResultSet.getDate("period_start");
        if (iPeriodStart != null) {
            iPeriodicInvoice.setLocalPeriodStart(iPeriodStart.toLocalDate());
        }

        java.sql.Date iPeriodEnd = iResultSet.getDate("period_end");
        if (iPeriodEnd != null) {
            iPeriodicInvoice.setLocalPeriodEnd(iPeriodEnd.toLocalDate());
        }

        iPeriodicInvoice.setAppendPeriod(iResultSet.getBoolean("append_period"));
        iPeriodicInvoice.setAppendInformation(iResultSet.getBoolean("append_information"));
        iPeriodicInvoice.setInformation(iResultSet.getString("information"));

        SSInvoice iTemplate = iPeriodicInvoice.getTemplate();
        iTemplate.setCustomerNr(iResultSet.getString("customer_nr"));
        iTemplate.setCustomerName(iResultSet.getString("customer_name"));
        iTemplate.setOurContactPerson(iResultSet.getString("our_contact"));
        iTemplate.setYourContactPerson(iResultSet.getString("your_contact"));
        iTemplate.setDelayInterest(iResultSet.getBigDecimal("delay_interest"));
        iTemplate.setTaxFree(iResultSet.getBoolean("tax_free"));
        iTemplate.setText(iResultSet.getString("sale_text"));
        iTemplate.setPrinted(iResultSet.getBoolean("printed"));
        iTemplate.setCurrencyRate(iResultSet.getBigDecimal("currency_rate"));
        iTemplate.setYourOrderNumber(iResultSet.getString("your_order_number"));
        iTemplate.setStockInfluencing(iResultSet.getBoolean("stock_influencing"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iTemplate.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        } else {
            iTemplate.setCurrency(null);
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iTemplate.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        } else {
            iTemplate.setPaymentTerm(null);
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iTemplate.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        } else {
            iTemplate.setDeliveryTerm(null);
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iTemplate.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        } else {
            iTemplate.setDeliveryWay(null);
        }

        java.sql.Date iPaymentDay = iResultSet.getDate("payment_day");
        if (iPaymentDay != null) {
            iTemplate.setLocalDueDate(iPaymentDay.toLocalDate());
        }

        iTemplate.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iTemplate.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        iTemplate.getRows().clear();
        iTemplate.getRows().addAll(getPeriodicInvoiceRowsV2(iResultSet.getInt("id")));

        return iPeriodicInvoice;
    }

    /**
     * Returns a List of the current curriencies
     *
     * @return A List of curriencies.
     */
    public List<SSCurrency> getCurrencies() {
        List<SSCurrency> iCurrencies = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_currency");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iCurrencies.add((SSCurrency) iResultSet.getObject("currency"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iCurrencies;
    }

    public Optional<SSCurrency> getCurrency(SSCurrency iCurrency) {
        SSCurrency iUpdatedCurrency = new SSCurrency();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_currency WHERE code=?");

            iStatement.setObject(1, iCurrency.getName());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                iUpdatedCurrency = (SSCurrency) iResultSet.getObject("currency");
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.of(iUpdatedCurrency);
    }

    public void addCurrency(SSCurrency iCurrency) {
        if (iCurrency == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_currency VALUES(?,?)");

            iStatement.setObject(1, iCurrency.getName());
            iStatement.setObject(2, iCurrency);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateCurrency(SSCurrency iCurrency) {
        if (iCurrency == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_currency SET currency=? WHERE code=?");

            iStatement.setObject(1, iCurrency);
            iStatement.setObject(2, iCurrency.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteCurrency(SSCurrency iCurrency) {
        if (iCurrency == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_currency WHERE code=?");

            iStatement.setObject(1, iCurrency.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the delivery ways
     *
     * @return a list of deliveryways
     */
    public List<SSDeliveryWay> getDeliveryWays() {
        List<SSDeliveryWay> iDeliveryWays = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_deliveryway");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iDeliveryWays.add((SSDeliveryWay) iResultSet.getObject("deliveryway"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iDeliveryWays;
    }

    public void addDeliveryWay(SSDeliveryWay iDeliveryWay) {
        if (iDeliveryWay == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_deliveryway VALUES(?,?)");

            iStatement.setObject(1, iDeliveryWay.getName());
            iStatement.setObject(2, iDeliveryWay);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateDeliveryWay(SSDeliveryWay iDeliveryWay) {
        if (iDeliveryWay == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_deliveryway SET deliveryway=? WHERE name=?");

            iStatement.setObject(1, iDeliveryWay);
            iStatement.setObject(2, iDeliveryWay.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteDeliveryWay(SSDeliveryWay iDeliveryWay) {
        if (iDeliveryWay == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_deliveryway WHERE name=?");

            iStatement.setObject(1, iDeliveryWay.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retuns a list of delivery terms.
     *
     * @return a list of delivery terms
     */
    public List<SSDeliveryTerm> getDeliveryTerms() {
        List<SSDeliveryTerm> iDeliveryTerms = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_deliveryterm");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iDeliveryTerms.add((SSDeliveryTerm) iResultSet.getObject("deliveryterm"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iDeliveryTerms;
    }

    public void addDeliveryTerm(SSDeliveryTerm iDeliveryTerm) {
        if (iDeliveryTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_deliveryterm VALUES(?,?)");

            iStatement.setObject(1, iDeliveryTerm.getName());
            iStatement.setObject(2, iDeliveryTerm);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateDeliveryTerm(SSDeliveryTerm iDeliveryTerm) {
        if (iDeliveryTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_deliveryterm SET deliveryterm=? WHERE name=?");

            iStatement.setObject(1, iDeliveryTerm);
            iStatement.setObject(2, iDeliveryTerm.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteDeliveryTerm(SSDeliveryTerm iDeliveryTerm) {
        if (iDeliveryTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_deliveryterm WHERE name=?");

            iStatement.setObject(1, iDeliveryTerm.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Returns the payment terms
     *
     * @return a list of payment terms
     */
    public List<SSPaymentTerm> getPaymentTerms() {
        List<SSPaymentTerm> iPaymentTerms = new LinkedList<>();

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_paymentterm");
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iPaymentTerms.add((SSPaymentTerm) iResultSet.getObject("paymentterm"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iPaymentTerms;
    }

    public void addPaymentTerm(SSPaymentTerm iPaymentTerm) {
        if (iPaymentTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_paymentterm VALUES(?,?)");

            iStatement.setObject(1, iPaymentTerm.getName());
            iStatement.setObject(2, iPaymentTerm);
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updatePaymentTerm(SSPaymentTerm iPaymentTerm) {
        if (iPaymentTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_paymentterm SET paymentterm=? WHERE name=?");

            iStatement.setObject(1, iPaymentTerm);
            iStatement.setObject(2, iPaymentTerm.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deletePaymentTerm(SSPaymentTerm iPaymentTerm) {
        if (iPaymentTerm == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_paymentterm WHERE name=?");

            iStatement.setObject(1, iPaymentTerm.getName());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSNewResultUnit> getResultUnits() {
        List<SSNewResultUnit> iResultUnits = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iResultUnits;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_resultunit WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iResultUnits.add((SSNewResultUnit) iResultSet.getObject("resultunit"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iResultUnits;
    }

    public Optional<SSNewResultUnit> getResultUnit(SSNewResultUnit pResultUnit) {
        if (pResultUnit == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_resultunit WHERE number=? AND companyid=?");

            iStatement.setObject(1, pResultUnit.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSNewResultUnit iResultUnit = (SSNewResultUnit) iResultSet.getObject(
                        "resultunit");

                iStatement.close();
                return Optional.of(iResultUnit);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSNewResultUnit> getResultUnit(String pResultUnitNumber) {
        if (pResultUnitNumber == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_resultunit WHERE number=? AND companyid=?");

            iStatement.setObject(1, pResultUnitNumber);
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSNewResultUnit iResultUnit = (SSNewResultUnit) iResultSet.getObject(
                        "resultunit");

                iStatement.close();
                return Optional.of(iResultUnit);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSNewResultUnit> getResultUnits(List<SSNewResultUnit> pResultUnits) {
        if (pResultUnits == null) {
            return Collections.emptyList();
        }
        List<SSNewResultUnit> iResultUnits = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iResultUnits;
        }
        try {
            for (SSNewResultUnit iResultUnit : pResultUnits) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_resultunit WHERE number=? AND companyid=?");

                iStatement.setObject(1, iResultUnit.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iResultUnits.add((SSNewResultUnit) iResultSet.getObject("resultunit"));
                }
                iStatement.close();
            }

            return iResultUnits;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addResultUnit(SSNewResultUnit iResultUnit) {
        if (iResultUnit == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_resultunit VALUES(?,?,?)");

            iStatement.setObject(1, iResultUnit.getNumber());
            iStatement.setObject(2, iResultUnit);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateResultUnit(SSNewResultUnit iResultUnit) {
        if (iResultUnit == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_resultunit SET resultunit=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iResultUnit);
            iStatement.setObject(2, iResultUnit.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteResultUnit(SSNewResultUnit iResultUnit) {
        if (iResultUnit == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_resultunit WHERE number=? AND companyid=?");

            iStatement.setObject(1, iResultUnit.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSNewProject> getProjects() {
        List<SSNewProject> iProjects = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iProjects;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_project WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            while (iResultSet.next()) {
                iProjects.add((SSNewProject) iResultSet.getObject("project"));
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iProjects;
    }

    public Optional<SSNewProject> getProject(SSNewProject pProject) {
        if (pProject == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_project WHERE number=? AND companyid=?");

            iStatement.setObject(1, pProject.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSNewProject iProject = (SSNewProject) iResultSet.getObject("project");

                iStatement.close();
                return Optional.of(iProject);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSNewProject> getProject(String pProjectNumber) {
        if (pProjectNumber == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_project WHERE number=? AND companyid=?");

            iStatement.setObject(1, pProjectNumber);
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSNewProject iProject = (SSNewProject) iResultSet.getObject("project");

                iStatement.close();
                return Optional.of(iProject);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSNewProject> getProjects(List<SSNewProject> pProjects) {
        if (pProjects == null) {
            return Collections.emptyList();
        }
        List<SSNewProject> iProjects = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iProjects;
        }
        try {
            for (SSNewProject iProject : pProjects) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_project WHERE number=? AND companyid=?");

                iStatement.setObject(1, iProject.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iProjects.add((SSNewProject) iResultSet.getObject("project"));
                }
                iStatement.close();
            }

            return iProjects;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addProject(SSNewProject iProject) {
        if (iProject == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_project VALUES(?,?,?)");

            iStatement.setObject(1, iProject.getNumber());
            iStatement.setObject(2, iProject);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateProject(SSNewProject iProject) {
        if (iProject == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_project SET project=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iProject);
            iStatement.setObject(2, iProject.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteProject(SSNewProject iProject) {
        if (iProject == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_project WHERE number=? AND companyid=?");

            iStatement.setObject(1, iProject.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public synchronized void triggerAction(String iTriggerName, String iTableName, String iNumber) {

        /** Körs då en trigger triggas i databasen. De flesta triggers uppdaterar listan som
         *  som motsvarar objekten triggen körts på. Projekt, Resultatenhet och konteringsmallar får
         *  behandlas något annorlunda då dessa inte lästs in i minnet vid uppstart.
         */

        try {

            /**
             *  REGISTER
             */
            if (iTriggerName.contains("PROJECT")) {
                if (SSProjectFrame.getInstance() != null) {
                    SSProjectFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.contains("RESULTUNIT")) {
                if (SSResultUnitFrame.getInstance() != null) {
                    SSResultUnitFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWPRODUCT") && iProducts != null) {
                SSProduct iProduct = new SSProduct();

                iProduct.setNumber(iNumber);
                Optional<SSProduct> optProduct = getProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("NEWPRODUCT trigger: product not found for number {}", iNumber);
                    return;
                }
                iProduct = optProduct.get();

                iProducts.add(iProduct);
                iProduct = null;
                if (SSProductFrame.getInstance() != null) {
                    SSProductFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITPRODUCT") && iProducts != null) {
                SSProduct iProduct = new SSProduct();

                iProduct.setNumber(iNumber);
                Optional<SSProduct> optProduct = getProduct(iProduct);
                if (optProduct.isEmpty()) {
                    LOG.warn("EDITPRODUCT trigger: product not found for number {}", iNumber);
                    return;
                }
                iProduct = optProduct.get();
                int iIndex = iProducts.lastIndexOf(iProduct);

                if (iIndex == -1) {
                    return;
                }
                iProducts.remove(iIndex);
                iProducts.add(iIndex, iProduct);
                iProduct = null;
                if (SSProductFrame.getInstance() != null) {
                    SSProductFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEPRODUCT") && iProducts != null) {
                SSProduct iProduct = new SSProduct();

                iProduct.setNumber(iNumber);
                iProducts.remove(iProduct);
                iProduct = null;
                if (SSProductFrame.getInstance() != null) {
                    SSProductFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWCUSTOMER") && iCustomers != null) {
                SSCustomer iCustomer = new SSCustomer();

                iCustomer.setNumber(iNumber);
                Optional<SSCustomer> optCustomer = getCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("NEWCUSTOMER trigger: customer not found for number {}", iNumber);
                    return;
                }
                iCustomer = optCustomer.get();
                iCustomers.add(iCustomer);
                SSCustomerMath.iInvoicesForCustomers.put(iCustomer.getNumber(),
                        new LinkedList<>());
                iCustomer = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITCUSTOMER") && iCustomers != null) {
                SSCustomer iCustomer = new SSCustomer();

                iCustomer.setNumber(iNumber);
                Optional<SSCustomer> optCustomer = getCustomer(iCustomer);
                if (optCustomer.isEmpty()) {
                    LOG.warn("EDITCUSTOMER trigger: customer not found for number {}", iNumber);
                    return;
                }
                iCustomer = optCustomer.get();
                int iIndex = iCustomers.lastIndexOf(iCustomer);

                if (iIndex == -1) {
                    return;
                }
                iCustomers.remove(iIndex);
                iCustomers.add(iIndex, iCustomer);
                iCustomer = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETECUSTOMER") && iCustomers != null) {
                SSCustomer iCustomer = new SSCustomer();

                iCustomer.setNumber(iNumber);
                iCustomers.remove(iCustomer);
                iCustomer = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWSUPPLIER") && iSuppliers != null) {
                SSSupplier iSupplier = new SSSupplier();

                iSupplier.setNumber(iNumber);
                Optional<SSSupplier> optSupplier = getSupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("NEWSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return;
                }
                iSupplier = optSupplier.get();
                iSuppliers.add(iSupplier);
                SSSupplierMath.iInvoicesForSuppliers.put(iSupplier.getNumber(),
                        new LinkedList<>());
                iSupplier = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITSUPPLIER") && iSuppliers != null) {
                SSSupplier iSupplier = new SSSupplier();

                iSupplier.setNumber(iNumber);
                Optional<SSSupplier> optSupplier = getSupplier(iSupplier);
                if (optSupplier.isEmpty()) {
                    LOG.warn("EDITSUPPLIER trigger: supplier not found for number {}", iNumber);
                    return;
                }
                iSupplier = optSupplier.get();
                int iIndex = iSuppliers.lastIndexOf(iSupplier);

                if (iIndex == -1) {
                    return;
                }
                iSuppliers.remove(iIndex);
                iSuppliers.add(iIndex, iSupplier);
                iSupplier = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETESUPPLIER") && iSuppliers != null) {
                SSSupplier iSupplier = new SSSupplier();

                iSupplier.setNumber(iNumber);
                iSuppliers.remove(iSupplier);
                iSupplier = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.contains("VOUCHERTEMPLATE")) {
                if (SSVoucherTemplateFrame.getInstance() != null) {
                    SSVoucherTemplateFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWAUTODIST") && iAutoDists != null) {
                Integer iAccount = Integer.parseInt(iNumber);
                SSAutoDist iAutoDist = new SSAutoDist();

                iAutoDist.setAccountNumber(iAccount);
                Optional<SSAutoDist> optAutoDist = getAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("NEWAUTODIST trigger: autodist not found for number {}", iNumber);
                    return;
                }
                iAutoDist = optAutoDist.get();
                iAutoDists.add(iAutoDist);
                iAutoDist = null;
                if (SSAutoDistFrame.getInstance() != null) {
                    SSAutoDistFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("EDITAUTODIST") && iAutoDists != null) {
                Integer iAccount = Integer.parseInt(iNumber);
                SSAutoDist iAutoDist = new SSAutoDist();

                iAutoDist.setAccountNumber(iAccount);
                Optional<SSAutoDist> optAutoDist = getAutoDist(iAutoDist);
                if (optAutoDist.isEmpty()) {
                    LOG.warn("EDITAUTODIST trigger: autodist not found for number {}", iNumber);
                    return;
                }
                iAutoDist = optAutoDist.get();
                int iIndex = iAutoDists.lastIndexOf(iAutoDist);

                if (iIndex == -1) {
                    return;
                }
                iAutoDists.remove(iIndex);
                iAutoDists.add(iIndex, iAutoDist);
                iAutoDist = null;
                if (SSAutoDistFrame.getInstance() != null) {
                    SSAutoDistFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEAUTODIST") && iAutoDists != null) {
                Integer iAccount = Integer.parseInt(iNumber);
                SSAutoDist iAutoDist = new SSAutoDist();

                iAutoDist.setAccountNumber(iAccount);
                iAutoDists.remove(iAutoDist);
                iAutoDist = null;
                if (SSAutoDistFrame.getInstance() != null) {
                    SSAutoDistFrame.getInstance().updateFrame();
                }
            } /**
             * FÖRSÄLJNING
             */ else if (iTriggerName.equals("NEWINPAYMENT") && iInpayments != null) {
                SSInpayment iInpayment = new SSInpayment();

                iInpayment.setNumber(Integer.parseInt(iNumber));
                Optional<SSInpayment> optInpayment = getInpayment(iInpayment);
                if (optInpayment.isEmpty()) {
                    LOG.warn("NEWINPAYMENT trigger: inpayment not found for number {}", iNumber);
                    return;
                }
                iInpayment = optInpayment.get();
                if (!iInpayments.contains(iInpayment)) {
                    iInpayments.add(iInpayment);
                }
                for (SSInpaymentRow iRow : iInpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSInvoiceMath.iSaldoMap.containsKey(iRow.getInvoiceNr())) {
                            SSInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).subtract(
                                    iRow.getValue()));
                        }
                    }
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
                iInpayment = null;
            } else if (iTriggerName.equals("EDITINPAYMENT") && iInpayments != null) {
                SSInpayment iInpayment = new SSInpayment();

                iInpayment.setNumber(Integer.parseInt(iNumber));
                Optional<SSInpayment> optInpayment = getInpayment(iInpayment);
                if (optInpayment.isEmpty()) {
                    LOG.warn("EDITINPAYMENT trigger: entity not found for number {}", iNumber);
                    return;
                }
                iInpayment = optInpayment.get();
                int iIndex = iInpayments.lastIndexOf(iInpayment);

                if (iIndex == -1) {
                    return;
                }
                SSInpayment iOldInpayment = iInpayments.get(iIndex);

                for (SSInpaymentRow iRow : iOldInpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSInvoiceMath.iSaldoMap.containsKey(iRow.getInvoiceNr())) {
                            SSInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).add(
                                    iRow.getValue()));
                        }
                    }
                }
                iInpayments.remove(iIndex);

                iInpayments.add(iIndex, iInpayment);
                for (SSInpaymentRow iRow : iInpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSInvoiceMath.iSaldoMap.containsKey(iRow.getInvoiceNr())) {
                            SSInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).subtract(
                                    iRow.getValue()));
                        }
                    }
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                iInpayment = null;
                if (SSInpaymentFrame.getInstance() != null) {
                    SSInpaymentFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEINPAYMENT") && iInpayments != null) {
                SSInpayment iInpayment = new SSInpayment();

                iInpayment.setNumber(Integer.parseInt(iNumber));
                iInpayments.remove(iInpayment);

                iInpayment = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSInpaymentFrame.getInstance() != null) {
                    SSInpaymentFrame.getInstance().updateFrame();
                }

            } else if (iTriggerName.equals("NEWTENDER") && iTenders != null) {
                SSTender iTender = new SSTender();

                iTender.setNumber(Integer.parseInt(iNumber));
                Optional<SSTender> optTender = getTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("NEWTENDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iTender = optTender.get();
                if (!iTenders.contains(iTender)) {
                    iTenders.add(iTender);
                }
                if (SSTenderFrame.getInstance() != null) {
                    SSTenderFrame.getInstance().updateFrame();
                }
                iTender = null;
            } else if (iTriggerName.equals("EDITTENDER") && iTenders != null) {
                SSTender iTender = new SSTender();

                iTender.setNumber(Integer.parseInt(iNumber));
                Optional<SSTender> optTender = getTender(iTender);
                if (optTender.isEmpty()) {
                    LOG.warn("EDITTENDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iTender = optTender.get();
                int iIndex = iTenders.lastIndexOf(iTender);

                if (iIndex == -1) {
                    return;
                }
                iTenders.remove(iIndex);
                iTenders.add(iIndex, iTender);
                iTender = null;
                if (SSTenderFrame.getInstance() != null) {
                    SSTenderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETETENDER") && iTenders != null) {
                SSTender iTender = new SSTender();

                iTender.setNumber(Integer.parseInt(iNumber));
                iTenders.remove(iTender);
                iTender = null;
                if (SSTenderFrame.getInstance() != null) {
                    SSTenderFrame.getInstance().updateFrame();
                }

            } else if (iTriggerName.equals("NEWORDER") && iOrders != null) {
                SSOrder iOrder = new SSOrder();

                iOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSOrder> optOrder = getOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("NEWORDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOrder = optOrder.get();
                if (!iOrders.contains(iOrder)) {
                    iOrders.add(iOrder);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                iOrder = null;
            } else if (iTriggerName.equals("EDITORDER") && iOrders != null) {
                SSOrder iOrder = new SSOrder();

                iOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSOrder> optOrder = getOrder(iOrder);
                if (optOrder.isEmpty()) {
                    LOG.warn("EDITORDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOrder = optOrder.get();
                int iIndex = iOrders.lastIndexOf(iOrder);

                if (iIndex == -1) {
                    return;
                }
                iOrders.remove(iIndex);
                iOrders.add(iIndex, iOrder);
                iOrder = null;
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEORDER") && iOrders != null) {
                SSOrder iOrder = new SSOrder();

                iOrder.setNumber(Integer.parseInt(iNumber));
                iOrders.remove(iOrder);
                iOrder = null;
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWINVOICE") && iInvoices != null) {
                SSInvoice iInvoice = new SSInvoice();

                iInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSInvoice> optInvoice = getInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("NEWINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iInvoice = optInvoice.get();
                if (!iInvoices.contains(iInvoice)) {
                    iInvoices.add(iInvoice);
                }
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(),
                        SSInvoiceMath.getSaldo(iInvoice));
                if (SSCustomerMath.iInvoicesForCustomers.containsKey(
                        iInvoice.getCustomerNr())) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(
                            iInvoice);
                } else {
                    List<SSInvoice> iNumbers = new LinkedList<>();

                    iNumbers.add(iInvoice);
                    SSCustomerMath.iInvoicesForCustomers.put(iInvoice.getCustomerNr(),
                            iNumbers);
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
                iInvoice = null;
            } else if (iTriggerName.equals("EDITINVOICE") && iInvoices != null) {
                SSInvoice iInvoice = new SSInvoice();

                iInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSInvoice> optInvoice = getInvoice(iInvoice);
                if (optInvoice.isEmpty()) {
                    LOG.warn("EDITINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iInvoice = optInvoice.get();
                int iIndex = iInvoices.lastIndexOf(iInvoice);

                if (iIndex == -1) {
                    return;
                }
                iInvoices.remove(iIndex);
                iInvoices.add(iIndex, iInvoice);
                SSInvoiceMath.iSaldoMap.put(iInvoice.getNumber(),
                        SSInvoiceMath.getSaldo(iInvoice));
                iIndex = SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).indexOf(
                        iInvoice);
                if (iIndex != -1) {
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).remove(
                            iIndex);
                    SSCustomerMath.iInvoicesForCustomers.get(iInvoice.getCustomerNr()).add(
                            iIndex, iInvoice);
                }
                iInvoice = null;
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEINVOICE") && iInvoices != null) {
                SSInvoice iInvoice = new SSInvoice();

                iInvoice.setNumber(Integer.parseInt(iNumber));
                iInvoices.remove(iInvoice);
                SSInvoiceMath.iSaldoMap.remove(iInvoice.getNumber());
                iInvoice = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWCREDITINVOICE") && iCreditInvoices != null) {
                SSCreditInvoice iCreditInvoice = new SSCreditInvoice();

                iCreditInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSCreditInvoice> optCreditInvoice = getCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("NEWCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iCreditInvoice = optCreditInvoice.get();
                if (!iCreditInvoices.contains(iCreditInvoice)) {
                    iCreditInvoices.add(iCreditInvoice);
                }

                if (SSInvoiceMath.iSaldoMap.containsKey(iCreditInvoice.getCreditingNr())) {
                    SSInvoiceMath.iSaldoMap.put(iCreditInvoice.getCreditingNr(),
                            SSInvoiceMath.iSaldoMap.get(iCreditInvoice.getCreditingNr()).subtract(
                            SSCreditInvoiceMath.getTotalSum(iCreditInvoice)));
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
                iCreditInvoice = null;
            } else if (iTriggerName.equals("EDITCREDITINVOICE") && iCreditInvoices != null) {
                SSCreditInvoice iCreditInvoice = new SSCreditInvoice();

                iCreditInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSCreditInvoice> optCreditInvoice = getCreditInvoice(iCreditInvoice);
                if (optCreditInvoice.isEmpty()) {
                    LOG.warn("EDITCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iCreditInvoice = optCreditInvoice.get();
                int iIndex = iCreditInvoices.lastIndexOf(iCreditInvoice);

                if (iIndex == -1) {
                    return;
                }
                SSCreditInvoice iOldCreditInvoice = iCreditInvoices.get(iIndex);

                if (SSInvoiceMath.iSaldoMap.containsKey(iOldCreditInvoice.getCreditingNr())) {
                    SSInvoiceMath.iSaldoMap.put(iOldCreditInvoice.getCreditingNr(),
                            SSInvoiceMath.iSaldoMap.get(iOldCreditInvoice.getCreditingNr()).add(
                            SSCreditInvoiceMath.getTotalSum(iOldCreditInvoice)));
                }
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                iCreditInvoices.remove(iIndex);
                iCreditInvoices.add(iIndex, iCreditInvoice);
                if (SSInvoiceMath.iSaldoMap.containsKey(iCreditInvoice.getCreditingNr())) {
                    SSInvoiceMath.iSaldoMap.put(iCreditInvoice.getCreditingNr(),
                            SSInvoiceMath.iSaldoMap.get(iCreditInvoice.getCreditingNr()).subtract(
                            SSCreditInvoiceMath.getTotalSum(iCreditInvoice)));
                }
                if (SSInvoiceFrame.getInstance() != null) {
                    SSInvoiceFrame.getInstance().updateFrame();
                }
                iCreditInvoice = null;
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETECREDITINVOICE")
                    && iCreditInvoices != null) {
                SSCreditInvoice iCreditInvoice = new SSCreditInvoice();

                iCreditInvoice.setNumber(Integer.parseInt(iNumber));
                iCreditInvoices.remove(iCreditInvoice);
                iCreditInvoice = null;
                if (SSCustomerFrame.getInstance() != null) {
                    SSCustomerFrame.getInstance().updateFrame();
                }
                if (SSCreditInvoiceFrame.getInstance() != null) {
                    SSCreditInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWPERIODICINVOICE")
                    && iPeriodicInvoices != null) {
                SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();

                iPeriodicInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSPeriodicInvoice> optPeriodicInvoice = getPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("NEWPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                if (!iPeriodicInvoices.contains(iPeriodicInvoice)) {
                    iPeriodicInvoices.add(iPeriodicInvoice);
                }
                if (SSPeriodicInvoiceFrame.getInstance() != null) {
                    SSPeriodicInvoiceFrame.getInstance().updateFrame();
                }
                iPeriodicInvoice = null;
            } else if (iTriggerName.equals("EDITPERIODICINVOICE")
                    && iPeriodicInvoices != null) {
                SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();

                iPeriodicInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSPeriodicInvoice> optPeriodicInvoice = getPeriodicInvoice(iPeriodicInvoice);
                if (optPeriodicInvoice.isEmpty()) {
                    LOG.warn("EDITPERIODICINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iPeriodicInvoice = optPeriodicInvoice.get();
                int iIndex = iPeriodicInvoices.lastIndexOf(iPeriodicInvoice);

                if (iIndex == -1) {
                    return;
                }
                iPeriodicInvoices.remove(iIndex);
                iPeriodicInvoices.add(iIndex, iPeriodicInvoice);
                iPeriodicInvoice = null;
                if (SSPeriodicInvoiceFrame.getInstance() != null) {
                    SSPeriodicInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEPERIODICINVOICE")
                    && iPeriodicInvoices != null) {
                SSPeriodicInvoice iPeriodicInvoice = new SSPeriodicInvoice();

                iPeriodicInvoice.setNumber(Integer.parseInt(iNumber));
                iPeriodicInvoices.remove(iPeriodicInvoice);
                iPeriodicInvoice = null;
                if (SSPeriodicInvoiceFrame.getInstance() != null) {
                    SSPeriodicInvoiceFrame.getInstance().updateFrame();
                }
            } /**
             * INKÖP
             */ else if (iTriggerName.equals("NEWOUTPAYMENT") && iOutpayments != null) {
                SSOutpayment iOutpayment = new SSOutpayment();

                iOutpayment.setNumber(Integer.parseInt(iNumber));
                Optional<SSOutpayment> optOutpayment = getOutpayment(iOutpayment);
                if (optOutpayment.isEmpty()) {
                    LOG.warn("NEWOUTPAYMENT trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOutpayment = optOutpayment.get();
                if (!iOutpayments.contains(iOutpayment)) {
                    iOutpayments.add(iOutpayment);
                }
                for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                                iRow.getInvoiceNr())) {
                            SSSupplierInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSSupplierInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).subtract(
                                    iRow.getValue()));
                        }
                    }
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
                iOutpayment = null;
            } else if (iTriggerName.equals("EDITOUTPAYMENT") && iOutpayments != null) {
                SSOutpayment iOutpayment = new SSOutpayment();

                iOutpayment.setNumber(Integer.parseInt(iNumber));
                Optional<SSOutpayment> optOutpayment = getOutpayment(iOutpayment);
                if (optOutpayment.isEmpty()) {
                    LOG.warn("EDITOUTPAYMENT trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOutpayment = optOutpayment.get();
                int iIndex = iOutpayments.lastIndexOf(iOutpayment);

                if (iIndex == -1) {
                    return;
                }
                SSOutpayment iOldOutpayment = iOutpayments.get(iIndex);

                for (SSOutpaymentRow iRow : iOldOutpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                                iRow.getInvoiceNr())) {
                            SSSupplierInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSSupplierInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).add(
                                    iRow.getValue()));
                        }
                    }
                }
                iOutpayments.remove(iIndex);
                iOutpayments.add(iIndex, iOutpayment);
                for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
                    if (iRow.getValue() != null && iRow.getInvoiceNr() != null) {
                        if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                                iRow.getInvoiceNr())) {
                            SSSupplierInvoiceMath.iSaldoMap.put(iRow.getInvoiceNr(),
                                    SSSupplierInvoiceMath.iSaldoMap.get(iRow.getInvoiceNr()).subtract(
                                    iRow.getValue()));
                        }
                    }
                }
                iOutpayment = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                if (SSOutpaymentFrame.getInstance() != null) {
                    SSOutpaymentFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEOUTPAYMENT") && iOutpayments != null) {
                SSOutpayment iOutpayment = new SSOutpayment();

                iOutpayment.setNumber(Integer.parseInt(iNumber));
                iOutpayments.remove(iOutpayment);
                iOutpayment = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                if (SSOutpaymentFrame.getInstance() != null) {
                    SSOutpaymentFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWPURCHASEORDER") && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSPurchaseOrder> optPurchaseOrder = getPurchaseOrder(iPurchaseOrder);
                if (optPurchaseOrder.isEmpty()) {
                    LOG.warn("NEWPURCHASEORDER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iPurchaseOrder = optPurchaseOrder.get();
                if (!iPurchaseOrders.contains(iPurchaseOrder)) {
                    iPurchaseOrders.add(iPurchaseOrder);
                }
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
                iPurchaseOrder = null;
            } else if (iTriggerName.equals("EDITPURCHASEORDER") && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                Optional<SSPurchaseOrder> optPurchaseOrder = getPurchaseOrder(iPurchaseOrder);
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
                iPurchaseOrder = null;
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEPURCHASEORDER")
                    && iPurchaseOrders != null) {
                SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();

                iPurchaseOrder.setNumber(Integer.parseInt(iNumber));
                iPurchaseOrders.remove(iPurchaseOrder);
                iPurchaseOrder = null;
                if (SSOrderFrame.getInstance() != null) {
                    SSOrderFrame.getInstance().updateFrame();
                }
                if (SSPurchaseOrderFrame.getInstance() != null) {
                    SSPurchaseOrderFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWSUPPLIERINVOICE")
                    && iSupplierInvoices != null) {
                SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();

                iSupplierInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSSupplierInvoice> optSupplierInvoice = getSupplierInvoice(iSupplierInvoice);
                if (optSupplierInvoice.isEmpty()) {
                    LOG.warn("NEWSUPPLIERINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iSupplierInvoice = optSupplierInvoice.get();
                if (!iSupplierInvoices.contains(iSupplierInvoice)) {
                    iSupplierInvoices.add(iSupplierInvoice);
                }
                SSSupplierInvoiceMath.iSaldoMap.put(iSupplierInvoice.getNumber(),
                        SSSupplierInvoiceMath.getSaldo(iSupplierInvoice));
                if (SSSupplierMath.iInvoicesForSuppliers.containsKey(
                        iSupplierInvoice.getSupplierNr())) {
                    SSSupplierMath.iInvoicesForSuppliers.get(iSupplierInvoice.getSupplierNr()).add(
                            iSupplierInvoice);
                } else {
                    List<SSSupplierInvoice> iNumbers = new LinkedList<>();

                    iNumbers.add(iSupplierInvoice);
                    SSSupplierMath.iInvoicesForSuppliers.put(
                            iSupplierInvoice.getSupplierNr(), iNumbers);
                }
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                iSupplierInvoice = null;
            } else if (iTriggerName.equals("EDITSUPPLIERINVOICE")
                    && iSupplierInvoices != null) {
                SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();

                iSupplierInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSSupplierInvoice> optSupplierInvoice = getSupplierInvoice(iSupplierInvoice);
                if (optSupplierInvoice.isEmpty()) {
                    LOG.warn("EDITSUPPLIERINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iSupplierInvoice = optSupplierInvoice.get();
                int iIndex = iSupplierInvoices.lastIndexOf(iSupplierInvoice);

                if (iIndex == -1) {
                    return;
                }
                iSupplierInvoices.remove(iIndex);
                iSupplierInvoices.add(iIndex, iSupplierInvoice);
                SSSupplierInvoiceMath.iSaldoMap.put(iSupplierInvoice.getNumber(),
                        SSSupplierInvoiceMath.getSaldo(iSupplierInvoice));
                iIndex = SSSupplierMath.iInvoicesForSuppliers.get(iSupplierInvoice.getSupplierNr()).indexOf(
                        iSupplierInvoice);
                if (iIndex != -1) {
                    SSSupplierMath.iInvoicesForSuppliers.get(iSupplierInvoice.getSupplierNr()).remove(
                            iIndex);
                    SSSupplierMath.iInvoicesForSuppliers.get(iSupplierInvoice.getSupplierNr()).add(
                            iIndex, iSupplierInvoice);
                }
                iSupplierInvoice = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETESUPPLIERINVOICE")
                    && iSupplierInvoices != null) {
                SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();

                iSupplierInvoice.setNumber(Integer.parseInt(iNumber));
                iSupplierInvoices.remove(iSupplierInvoice);
                SSSupplierInvoiceMath.iSaldoMap.remove(iSupplierInvoice.getNumber());
                iSupplierInvoice = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWSUPPLIERCREDITINVOICE")
                    && iSupplierCreditInvoices != null) {
                SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice();

                iSupplierCreditInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = getSupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("NEWSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                if (!iSupplierCreditInvoices.contains(iSupplierCreditInvoice)) {
                    iSupplierCreditInvoices.add(iSupplierCreditInvoice);
                }
                if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                        iSupplierCreditInvoice.getCreditingNr())) {
                    SSSupplierInvoiceMath.iSaldoMap.put(
                            iSupplierCreditInvoice.getCreditingNr(),
                            SSSupplierInvoiceMath.iSaldoMap.get(iSupplierCreditInvoice.getCreditingNr()).subtract(
                                    SSSupplierInvoiceMath.getTotalSum(
                                            iSupplierCreditInvoice)));
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
                iSupplierCreditInvoice = null;
            } else if (iTriggerName.equals("EDITSUPPLIERCREDITINVOICE")
                    && iSupplierCreditInvoices != null) {
                SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice();

                iSupplierCreditInvoice.setNumber(Integer.parseInt(iNumber));
                Optional<SSSupplierCreditInvoice> optSupplierCreditInvoice = getSupplierCreditInvoice(iSupplierCreditInvoice);
                if (optSupplierCreditInvoice.isEmpty()) {
                    LOG.warn("EDITSUPPLIERCREDITINVOICE trigger: entity not found for number {}", iNumber);
                    return;
                }
                iSupplierCreditInvoice = optSupplierCreditInvoice.get();
                int iIndex = iSupplierCreditInvoices.lastIndexOf(iSupplierCreditInvoice);

                if (iIndex == -1) {
                    return;
                }
                SSSupplierCreditInvoice iOldSupplierCreditInvoice = iSupplierCreditInvoices.get(
                        iIndex);

                if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                        iOldSupplierCreditInvoice.getCreditingNr())) {
                    SSSupplierInvoiceMath.iSaldoMap.put(
                            iOldSupplierCreditInvoice.getCreditingNr(),
                            SSSupplierInvoiceMath.iSaldoMap.get(iOldSupplierCreditInvoice.getCreditingNr()).add(
                                    SSSupplierInvoiceMath.getTotalSum(
                                            iOldSupplierCreditInvoice)));
                }
                iSupplierCreditInvoices.remove(iIndex);
                iSupplierCreditInvoices.add(iIndex, iSupplierCreditInvoice);
                if (SSSupplierInvoiceMath.iSaldoMap.containsKey(
                        iSupplierCreditInvoice.getCreditingNr())) {
                    SSSupplierInvoiceMath.iSaldoMap.put(
                            iSupplierCreditInvoice.getCreditingNr(),
                            SSSupplierInvoiceMath.iSaldoMap.get(iSupplierCreditInvoice.getCreditingNr()).subtract(
                                    SSSupplierInvoiceMath.getTotalSum(
                                            iSupplierCreditInvoice)));
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                iSupplierCreditInvoice = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                    SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETESUPPLIERCREDITINVOICE")
                    && iSupplierCreditInvoices != null) {
                SSSupplierCreditInvoice iSupplierCreditInvoice = new SSSupplierCreditInvoice();

                iSupplierCreditInvoice.setNumber(Integer.parseInt(iNumber));
                iSupplierCreditInvoices.remove(iSupplierCreditInvoice);
                iSupplierCreditInvoice = null;
                if (SSSupplierFrame.getInstance() != null) {
                    SSSupplierFrame.getInstance().updateFrame();
                }
                if (SSSupplierInvoiceFrame.getInstance() != null) {
                    SSSupplierInvoiceFrame.getInstance().updateFrame();
                }
                if (SSSupplierCreditInvoiceFrame.getInstance() != null) {
                    SSSupplierCreditInvoiceFrame.getInstance().updateFrame();
                }
            } /**
             * LAGER
             */ else if (iTriggerName.equals("NEWINVENTORY") && iInventories != null) {
                SSInventory iInventory = new SSInventory();

                iInventory.setNumber(Integer.parseInt(iNumber));
                Optional<SSInventory> optInventory = getInventory(iInventory);
                if (optInventory.isEmpty()) {
                    LOG.warn("NEWINVENTORY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iInventory = optInventory.get();
                if (!iInventories.contains(iInventory)) {
                    iInventories.add(iInventory);
                }
                if (SSInventoryFrame.getInstance() != null) {
                    SSInventoryFrame.getInstance().updateFrame();
                }
                iInventory = null;
            } else if (iTriggerName.equals("EDITINVENTORY") && iInventories != null) {
                SSInventory iInventory = new SSInventory();

                iInventory.setNumber(Integer.parseInt(iNumber));
                Optional<SSInventory> optInventory = getInventory(iInventory);
                if (optInventory.isEmpty()) {
                    LOG.warn("EDITINVENTORY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iInventory = optInventory.get();
                int iIndex = iInventories.lastIndexOf(iInventory);

                if (iIndex == -1) {
                    return;
                }
                iInventories.remove(iIndex);
                iInventories.add(iIndex, iInventory);
                iInventory = null;
                if (SSInventoryFrame.getInstance() != null) {
                    SSInventoryFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEINVENTORY") && iInventories != null) {
                SSInventory iInventory = new SSInventory();

                iInventory.setNumber(Integer.parseInt(iNumber));
                iInventories.remove(iInventory);
                iInventory = null;
                if (SSInventoryFrame.getInstance() != null) {
                    SSInventoryFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWINDELIVERY") && iIndeliveries != null) {
                SSIndelivery iIndelivery = new SSIndelivery();

                iIndelivery.setNumber(Integer.parseInt(iNumber));
                Optional<SSIndelivery> optIndelivery = getIndelivery(iIndelivery);
                if (optIndelivery.isEmpty()) {
                    LOG.warn("NEWINDELIVERY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iIndelivery = optIndelivery.get();
                if (!iIndeliveries.contains(iIndelivery)) {
                    iIndeliveries.add(iIndelivery);
                }
                if (SSIndeliveryFrame.getInstance() != null) {
                    SSIndeliveryFrame.getInstance().updateFrame();
                }
                iIndelivery = null;
            } else if (iTriggerName.equals("EDITINDELIVERY") && iIndeliveries != null) {
                SSIndelivery iIndelivery = new SSIndelivery();

                iIndelivery.setNumber(Integer.parseInt(iNumber));
                Optional<SSIndelivery> optIndelivery = getIndelivery(iIndelivery);
                if (optIndelivery.isEmpty()) {
                    LOG.warn("EDITINDELIVERY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iIndelivery = optIndelivery.get();
                int iIndex = iIndeliveries.lastIndexOf(iIndelivery);

                if (iIndex == -1) {
                    return;
                }
                iIndeliveries.remove(iIndex);
                iIndeliveries.add(iIndex, iIndelivery);
                iIndelivery = null;
                if (SSIndeliveryFrame.getInstance() != null) {
                    SSIndeliveryFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEINDELIVERY") && iIndeliveries != null) {
                SSIndelivery iIndelivery = new SSIndelivery();

                iIndelivery.setNumber(Integer.parseInt(iNumber));
                iIndeliveries.remove(iIndelivery);
                iIndelivery = null;
                if (SSIndeliveryFrame.getInstance() != null) {
                    SSIndeliveryFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWOUTDELIVERY") && iOutdeliveries != null) {
                SSOutdelivery iOutdelivery = new SSOutdelivery();

                iOutdelivery.setNumber(Integer.parseInt(iNumber));
                Optional<SSOutdelivery> optOutdelivery = getOutdelivery(iOutdelivery);
                if (optOutdelivery.isEmpty()) {
                    LOG.warn("NEWOUTDELIVERY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOutdelivery = optOutdelivery.get();
                if (!iOutdeliveries.contains(iOutdelivery)) {
                    iOutdeliveries.add(iOutdelivery);
                }
                if (SSOutdeliveryFrame.getInstance() != null) {
                    SSOutdeliveryFrame.getInstance().updateFrame();
                }
                iOutdelivery = null;
            } else if (iTriggerName.equals("EDITOUTDELIVERY") && iOutdeliveries != null) {
                SSOutdelivery iOutdelivery = new SSOutdelivery();

                iOutdelivery.setNumber(Integer.parseInt(iNumber));
                Optional<SSOutdelivery> optOutdelivery = getOutdelivery(iOutdelivery);
                if (optOutdelivery.isEmpty()) {
                    LOG.warn("EDITOUTDELIVERY trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOutdelivery = optOutdelivery.get();
                int iIndex = iOutdeliveries.lastIndexOf(iOutdelivery);

                if (iIndex == -1) {
                    return;
                }
                iOutdeliveries.remove(iIndex);
                iOutdeliveries.add(iIndex, iOutdelivery);
                iOutdelivery = null;
                if (SSOutdeliveryFrame.getInstance() != null) {
                    SSOutdeliveryFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEOUTDELIVERY") && iOutdeliveries != null) {
                SSOutdelivery iOutdelivery = new SSOutdelivery();

                iOutdelivery.setNumber(Integer.parseInt(iNumber));
                iOutdeliveries.remove(iOutdelivery);
                iOutdelivery = null;
                if (SSOutdeliveryFrame.getInstance() != null) {
                    SSOutdeliveryFrame.getInstance().updateFrame();
                }
            } /**
             * BOKFÖRING
             */ else if (iTriggerName.equals("NEWVOUCHER") && iVouchers != null) {
                SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

                Optional<SSVoucher> optVoucher = getVoucher(iVoucher);
                if (optVoucher.isEmpty()) {
                    LOG.warn("NEWVOUCHER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iVoucher = optVoucher.get();
                if (!iVouchers.contains(iVoucher)) {
                    iVouchers.add(iVoucher);
                }
                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().updateFrame();
                }
                iVoucher = null;
            } else if (iTriggerName.equals("EDITVOUCHER") && iVouchers != null) {
                SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

                Optional<SSVoucher> optVoucher = getVoucher(iVoucher);
                if (optVoucher.isEmpty()) {
                    LOG.warn("EDITVOUCHER trigger: entity not found for number {}", iNumber);
                    return;
                }
                iVoucher = optVoucher.get();
                int iIndex = iVouchers.lastIndexOf(iVoucher);

                if (iIndex == -1) {
                    return;
                }
                iVouchers.remove(iIndex);
                iVouchers.add(iIndex, iVoucher);
                iVoucher = null;
                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEVOUCHER") && iVouchers != null) {
                SSVoucher iVoucher = new SSVoucher(Integer.parseInt(iNumber));

                iVouchers.remove(iVoucher);
                iVoucher = null;
                if (SSVoucherFrame.getInstance() != null) {
                    SSVoucherFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("NEWOWNREPORT") && iOwnReports != null) {
                SSOwnReport iOwnReport = new SSOwnReport();

                iOwnReport.setId(Integer.parseInt(iNumber));
                Optional<SSOwnReport> optOwnReport = getOwnReport(iOwnReport);
                if (optOwnReport.isEmpty()) {
                    LOG.warn("NEWOWNREPORT trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOwnReport = optOwnReport.get();
                if (!iOwnReports.contains(iOwnReport) && iOwnReport.getId() != -1) {
                    iOwnReports.add(iOwnReport);
                }
                if (SSOwnReportFrame.getInstance() != null) {
                    SSOwnReportFrame.getInstance().updateFrame();
                }
                iOwnReport = null;
            } else if (iTriggerName.equals("EDITOWNREPORT") && iOwnReports != null) {
                SSOwnReport iOwnReport = new SSOwnReport();

                iOwnReport.setId(Integer.parseInt(iNumber));
                Optional<SSOwnReport> optOwnReport = getOwnReport(iOwnReport);
                if (optOwnReport.isEmpty()) {
                    LOG.warn("EDITOWNREPORT trigger: entity not found for number {}", iNumber);
                    return;
                }
                iOwnReport = optOwnReport.get();
                int iIndex = iOwnReports.lastIndexOf(iOwnReport);

                if (iIndex != -1) {
                    iOwnReports.remove(iIndex);
                    iOwnReports.add(iIndex, iOwnReport);
                } else {
                    iOwnReports.add(iOwnReport);
                }
                iOwnReport = null;
                if (SSOwnReportFrame.getInstance() != null) {
                    SSOwnReportFrame.getInstance().updateFrame();
                }
            } else if (iTriggerName.equals("DELETEOWNREPORT") && iOwnReports != null) {
                SSOwnReport iOwnReport = new SSOwnReport();

                iOwnReport.setId(Integer.parseInt(iNumber));
                iOwnReports.remove(iOwnReport);
                iOwnReport = null;
                if (SSOwnReportFrame.getInstance() != null) {
                    SSOwnReportFrame.getInstance().updateFrame();
                }
            }
        } catch (NumberFormatException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public List<SSProduct> getProducts() {
        if (iProducts != null) {
            return iProducts;
        }
        iProducts = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iProducts;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_product WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iProducts.add(mapProductV2(iResultSet));
                    } else {
                        iProducts.add((SSProduct) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iProducts;
    }

    public Optional<SSProduct> getProduct(SSProduct pProduct) {
        if (pProduct == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_product WHERE number=? AND companyid=?");

            iStatement.setObject(1, pProduct.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSProduct iProduct = useSchemaV2()
                        ? mapProductV2(iResultSet)
                        : (SSProduct) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iProduct);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSProduct> getProduct(String iProductNumber) {
        if (iProductNumber == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_product WHERE LOWER(number)=LOWER(?) AND companyid=?");

            iStatement.setObject(1, iProductNumber);
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSProduct iProduct = useSchemaV2()
                        ? mapProductV2(iResultSet)
                        : (SSProduct) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iProduct);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSProduct> getProducts(List<SSProduct> pProducts) {
        if (pProducts == null) {
            return Collections.emptyList();
        }
        List<SSProduct> iProducts = new LinkedList<>();

        if (this.iProducts != null) {
            for (SSProduct iProduct : pProducts) {
                if (this.iProducts.contains(iProduct)) {
                    iProducts.add(iProduct);
                }
            }
            return iProducts;
        }
        if (iCurrentCompany == null) {
            return iProducts;
        }
        try {
            for (SSProduct iProduct : pProducts) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_product WHERE number=? AND companyid=?");

                iStatement.setObject(1, iProduct.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iProducts.add(mapProductV2(iResultSet));
                    } else {
                        iProducts.add((SSProduct) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iProducts;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addProduct(SSProduct iProduct) {
        if (iProduct == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iProductId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_product(" +
                                "number,companyid,description,unitprice,tax_code,warehouse_location," +
                                "orderpoint,ordercount,purchase_price,stock_price,freight,supplier_nr," +
                                "supplier_product_nr,expired,stock_goods,unit,weight,volume,project_number" +
                                ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);

                bindProductV2(iStatement, iProduct, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_product VALUES(NULL,?,?,?)");

                iStatement.setObject(1, iProduct.getNumber());
                iStatement.setObject(2, iProduct);
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet keys = iStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        iProductId = keys.getInt(1);
                    }
                }
                if (iProductId == null) {
                    iProductId = getProductIdV2(iProduct.getNumber(), iCurrentCompany.getId());
                }
                if (iProductId != null) {
                    replaceProductAccountsV2(iProductId, iProduct);
                }
            }

            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateProduct(SSProduct iProduct) {
        if (iProduct == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_product SET " +
                                "description=?,unitprice=?,tax_code=?,warehouse_location=?,orderpoint=?," +
                                "ordercount=?,purchase_price=?,stock_price=?,freight=?,supplier_nr=?," +
                                "supplier_product_nr=?,expired=?,stock_goods=?,unit=?,weight=?,volume=?," +
                                "project_number=? WHERE number=? AND companyid=?");

                int i = 1;
                iStatement.setObject(i++, iProduct.getDescription());
                iStatement.setObject(i++, iProduct.getSellingPrice());
                iStatement.setObject(i++, iProduct.getTaxCode() == null ? null : iProduct.getTaxCode().name());
                iStatement.setObject(i++, iProduct.getWarehouseLocation());
                iStatement.setObject(i++, iProduct.getOrderpoint());
                iStatement.setObject(i++, iProduct.getOrdercount());
                iStatement.setObject(i++, iProduct.getPurchasePrice());
                iStatement.setObject(i++, iProduct.getStockPrice());
                iStatement.setObject(i++, iProduct.getUnitFreight());
                iStatement.setObject(i++, iProduct.getSupplierNr());
                iStatement.setObject(i++, iProduct.getSupplierProductNr());
                iStatement.setObject(i++, iProduct.isExpired());
                iStatement.setObject(i++, iProduct.isStockProduct());
                iStatement.setObject(i++, iProduct.getUnit() == null ? null : iProduct.getUnit().getName());
                iStatement.setObject(i++, iProduct.getWeight());
                iStatement.setObject(i++, iProduct.getVolume());
                iStatement.setObject(i++, iProduct.getProjectNr());
                iStatement.setObject(i++, iProduct.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_product SET product=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iProduct);
                iStatement.setObject(2, iProduct.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();

            if (useSchemaV2()) {
                Integer iProductId = getProductIdV2(iProduct.getNumber(), iCurrentCompany.getId());
                if (iProductId != null) {
                    replaceProductAccountsV2(iProductId, iProduct);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteProduct(SSProduct iProduct) {
        if (iProduct == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iProductId = getProductIdV2(iProduct.getNumber(), iCurrentCompany.getId());
                if (iProductId != null) {
                    deleteProductAccountsV2(iProductId);
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_product WHERE number=? AND companyid=?");

            iStatement.setObject(1, iProduct.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    private void bindProductV2(PreparedStatement iStatement, SSProduct iProduct, Integer iCompanyId)
            throws SQLException {
        int i = 1;
        iStatement.setObject(i++, iProduct.getNumber());
        iStatement.setObject(i++, iCompanyId);
        iStatement.setObject(i++, iProduct.getDescription());
        iStatement.setObject(i++, iProduct.getSellingPrice());
        iStatement.setObject(i++, iProduct.getTaxCode() == null ? null : iProduct.getTaxCode().name());
        iStatement.setObject(i++, iProduct.getWarehouseLocation());
        iStatement.setObject(i++, iProduct.getOrderpoint());
        iStatement.setObject(i++, iProduct.getOrdercount());
        iStatement.setObject(i++, iProduct.getPurchasePrice());
        iStatement.setObject(i++, iProduct.getStockPrice());
        iStatement.setObject(i++, iProduct.getUnitFreight());
        iStatement.setObject(i++, iProduct.getSupplierNr());
        iStatement.setObject(i++, iProduct.getSupplierProductNr());
        iStatement.setObject(i++, iProduct.isExpired());
        iStatement.setObject(i++, iProduct.isStockProduct());
        iStatement.setObject(i++, iProduct.getUnit() == null ? null : iProduct.getUnit().getName());
        iStatement.setObject(i++, iProduct.getWeight());
        iStatement.setObject(i++, iProduct.getVolume());
        iStatement.setObject(i, iProduct.getProjectNr());
    }

    private Integer getProductIdV2(String iProductNumber, Integer iCompanyId) throws SQLException {
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_product WHERE number=? AND companyid=?");
        iStatement.setObject(1, iProductNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private void deleteProductAccountsV2(Integer iProductId) throws SQLException {
        PreparedStatement iStatement = iConnection.prepareStatement(
                "DELETE FROM tbl_product_account WHERE product_id=?");
        iStatement.setObject(1, iProductId);
        iStatement.executeUpdate();
        iStatement.close();
    }

    private void replaceProductAccountsV2(Integer iProductId, SSProduct iProduct) throws SQLException {
        deleteProductAccountsV2(iProductId);

        for (SSDefaultAccount iDefaultAccount : SSDefaultAccount.values()) {
            Integer iAccount = iProduct.getDefaultAccount(iDefaultAccount, null);
            if (iAccount == null) {
                continue;
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_product_account(product_id,account_type,account_nr) VALUES(?,?,?)");
            iStatement.setObject(1, iProductId);
            iStatement.setObject(2, iDefaultAccount.name());
            iStatement.setObject(3, iAccount);
            iStatement.executeUpdate();
            iStatement.close();
        }
    }

    private SSProduct mapProductV2(ResultSet iResultSet) throws SQLException {
        SSProduct iProduct = new SSProduct();

        iProduct.setNumber(iResultSet.getString("number"));
        iProduct.setDescription(iResultSet.getString("description"));
        iProduct.setSellingPrice(iResultSet.getBigDecimal("unitprice"));

        String iTaxCode = iResultSet.getString("tax_code");
        if (iTaxCode != null) {
            try {
                iProduct.setTaxCode(SSTaxCode.valueOf(iTaxCode));
            } catch (IllegalArgumentException ignored) {
                // Keep null tax code if database contains unknown enum value.
            }
        }

        iProduct.setWarehouseLocation(iResultSet.getString("warehouse_location"));
        iProduct.setOrderpoint((Integer) iResultSet.getObject("orderpoint"));
        iProduct.setOrdercount((Integer) iResultSet.getObject("ordercount"));
        iProduct.setPurchasePrice(iResultSet.getBigDecimal("purchase_price"));
        iProduct.setStockPrice(iResultSet.getBigDecimal("stock_price"));
        iProduct.setUnitFreight(iResultSet.getBigDecimal("freight"));
        iProduct.setSupplierNr(iResultSet.getString("supplier_nr"));
        iProduct.setSupplierProductNr(iResultSet.getString("supplier_product_nr"));
        iProduct.setExpired(iResultSet.getBoolean("expired"));
        iProduct.setStockProduct(iResultSet.getBoolean("stock_goods"));

        String iUnit = iResultSet.getString("unit");
        if (iUnit != null) {
            iProduct.setUnit(new SSUnit(iUnit, iUnit));
        }

        iProduct.setWeight(iResultSet.getBigDecimal("weight"));
        iProduct.setVolume(iResultSet.getBigDecimal("volume"));
        iProduct.setProjectNr(iResultSet.getString("project_number"));

        Integer iProductId = iResultSet.getInt("id");
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT account_type,account_nr FROM tbl_product_account WHERE product_id=?");
        iStatement.setObject(1, iProductId);
        ResultSet iAccounts = iStatement.executeQuery();
        while (iAccounts.next()) {
            try {
                SSDefaultAccount iDefaultAccount = SSDefaultAccount.valueOf(iAccounts.getString("account_type"));
                iProduct.setDefaultAccount(iDefaultAccount, iAccounts.getInt("account_nr"));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown account type values.
            }
        }
        iAccounts.close();
        iStatement.close();

        return iProduct;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the customers for the current company.
     *
     * @return  A List of customers or an empty list.
     */
    private SSAddress mapAddressV2(ResultSet iResultSet, String iPrefix) throws SQLException {
        SSAddress iAddress = new SSAddress();
        iAddress.setName(iResultSet.getString(iPrefix + "_name"));
        iAddress.setAddress1(iResultSet.getString(iPrefix + "_address"));
        iAddress.setAddress2(iResultSet.getString(iPrefix + "_street"));
        iAddress.setZipCode(iResultSet.getString(iPrefix + "_zipcode"));
        iAddress.setCity(iResultSet.getString(iPrefix + "_city"));
        iAddress.setCountry(iResultSet.getString(iPrefix + "_country"));
        return iAddress;
    }

    private SSCustomer mapCustomerV2(ResultSet iResultSet) throws SQLException {
        SSCustomer iCustomer = new SSCustomer();

        iCustomer.setNumber(iResultSet.getString("number"));
        iCustomer.setName(iResultSet.getString("name"));
        iCustomer.setEMail(iResultSet.getString("email"));
        iCustomer.setPhone1(iResultSet.getString("phone"));
        iCustomer.setPhone2(iResultSet.getString("phone2"));
        iCustomer.setTelefax(iResultSet.getString("telefax"));
        iCustomer.setRegistrationNumber(iResultSet.getString("registration_number"));
        iCustomer.setOurContactPerson(iResultSet.getString("our_contact"));
        iCustomer.setYourContactPerson(iResultSet.getString("your_contact"));
        iCustomer.setVATNumber(iResultSet.getString("vat_number"));
        iCustomer.setBankgiro(iResultSet.getString("bankgiro"));
        iCustomer.setPlusgiro(iResultSet.getString("plusgiro"));
        iCustomer.setAccountNumber(iResultSet.getString("account_number"));
        iCustomer.setClearingNumber(iResultSet.getString("clearing_number"));
        iCustomer.setEuSaleCommodity(iResultSet.getBoolean("eu_sale_commodity"));
        iCustomer.setEuSaleYhirdPartCommodity(iResultSet.getBoolean("eu_sale_third_part"));
        iCustomer.setTaxFree(iResultSet.getBoolean("vat_free_sale"));
        iCustomer.setHideUnitprice(iResultSet.getBoolean("hide_unitprice"));
        iCustomer.setCreditLimit(iResultSet.getBigDecimal("credit_limit"));
        iCustomer.setDiscount(iResultSet.getBigDecimal("discount"));
        iCustomer.setComment(iResultSet.getString("comment"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iCustomer.setInvoiceCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iCustomer.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iCustomer.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iCustomer.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        }

        iCustomer.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iCustomer.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        return iCustomer;
    }

    private int bindAddressV2(PreparedStatement iStatement, int iIndex, SSAddress iAddress)
            throws SQLException {
        SSAddress iSafeAddress = iAddress == null ? new SSAddress() : iAddress;
        iStatement.setObject(iIndex++, iSafeAddress.getName());
        iStatement.setObject(iIndex++, iSafeAddress.getAddress1());
        iStatement.setObject(iIndex++, iSafeAddress.getAddress2());
        iStatement.setObject(iIndex++, iSafeAddress.getZipCode());
        iStatement.setObject(iIndex++, iSafeAddress.getCity());
        iStatement.setObject(iIndex++, iSafeAddress.getCountry());
        return iIndex;
    }

    private String getCustomerCurrencyCodeV2(SSCustomer iCustomer) {
        try {
            SSCurrency iCurrency = iCustomer.getInvoiceCurrency();
            return iCurrency == null ? null : iCurrency.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public List<SSCustomer> getCustomers() {
        if (iCustomers != null) {
            return iCustomers;
        }
        iCustomers = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iCustomers;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_customer WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iCustomers.add(mapCustomerV2(iResultSet));
                    } else {
                        iCustomers.add((SSCustomer) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iCustomers;
    }

    public Optional<SSCustomer> getCustomer(SSCustomer pCustomer) {
        if (pCustomer == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_customer WHERE number=? AND companyid=?");

            iStatement.setObject(1, pCustomer.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSCustomer iCustomer = useSchemaV2()
                        ? mapCustomerV2(iResultSet)
                        : (SSCustomer) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iCustomer);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSCustomer> getCustomer(String iCustomerNumber) {
        if (iCustomerNumber == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_customer WHERE LOWER(number)=LOWER(?) AND companyid=?");

            iStatement.setObject(1, iCustomerNumber);
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSCustomer iCustomer = useSchemaV2()
                        ? mapCustomerV2(iResultSet)
                        : (SSCustomer) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iCustomer);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSCustomer> getCustomers(List<SSCustomer> pCustomers) {
        if (pCustomers == null) {
            return Collections.emptyList();
        }
        List<SSCustomer> iCustomers = new LinkedList<>();

        if (this.iCustomers != null) {
            for (SSCustomer iCustomer : pCustomers) {
                if (this.iCustomers.contains(iCustomer)) {
                    iCustomers.add(iCustomer);
                }
            }
            return iCustomers;
        }
        if (iCurrentCompany == null) {
            return iCustomers;
        }
        try {
            for (SSCustomer iCustomer : pCustomers) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_customer WHERE number=? AND companyid=?");

                iStatement.setObject(1, iCustomer.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iCustomers.add(mapCustomerV2(iResultSet));
                    } else {
                        iCustomers.add((SSCustomer) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iCustomers;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addCustomer(SSCustomer iCustomer) {
        if (iCustomer == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_customer(" +
                                "number,companyid,name,email,phone,phone2,telefax,registration_number," +
                                "our_contact,your_contact,vat_number,bankgiro,plusgiro,account_number," +
                                "clearing_number,eu_sale_commodity,eu_sale_third_part,vat_free_sale," +
                                "hide_unitprice,credit_limit,discount,comment,currency_code,payment_term," +
                                "delivery_term,delivery_way,inv_addr_name,inv_addr_address,inv_addr_street," +
                                "inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name,del_addr_address," +
                                "del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                int i = 1;
                iStatement.setObject(i++, iCustomer.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                iStatement.setObject(i++, iCustomer.getName());
                iStatement.setObject(i++, iCustomer.getEMail());
                iStatement.setObject(i++, iCustomer.getPhone1());
                iStatement.setObject(i++, iCustomer.getPhone2());
                iStatement.setObject(i++, iCustomer.getTelefax());
                iStatement.setObject(i++, iCustomer.getRegistrationNumber());
                iStatement.setObject(i++, iCustomer.getOurContactPerson());
                iStatement.setObject(i++, iCustomer.getYourContactPerson());
                iStatement.setObject(i++, iCustomer.getVATNumber());
                iStatement.setObject(i++, iCustomer.getBankgiro());
                iStatement.setObject(i++, iCustomer.getPlusgiro());
                iStatement.setObject(i++, iCustomer.getAccountNumber());
                iStatement.setObject(i++, iCustomer.getClearingNumber());
                iStatement.setObject(i++, iCustomer.getEuSaleCommodity());
                iStatement.setObject(i++, iCustomer.getEuSaleYhirdPartCommodity());
                iStatement.setObject(i++, iCustomer.getTaxFree());
                iStatement.setObject(i++, iCustomer.getHideUnitprice());
                iStatement.setObject(i++, iCustomer.getCreditLimit());
                iStatement.setObject(i++, iCustomer.getDiscount());
                iStatement.setObject(i++, iCustomer.getComment());
                iStatement.setObject(i++, getCustomerCurrencyCodeV2(iCustomer));
                iStatement.setObject(i++, iCustomer.getPaymentTerm() == null
                        ? null : iCustomer.getPaymentTerm().getName());
                iStatement.setObject(i++, iCustomer.getDeliveryTerm() == null
                        ? null : iCustomer.getDeliveryTerm().getName());
                iStatement.setObject(i++, iCustomer.getDeliveryWay() == null
                        ? null : iCustomer.getDeliveryWay().getName());
                i = bindAddressV2(iStatement, i, iCustomer.getInvoiceAddress());
                bindAddressV2(iStatement, i, iCustomer.getDeliveryAddress());
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_customer VALUES(NULL,?,?,?)");

                iStatement.setObject(1, iCustomer.getNumber());
                iStatement.setObject(2, iCustomer);
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateCustomer(SSCustomer iCustomer) {
        if (iCustomer == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_customer SET " +
                                "name=?,email=?,phone=?,phone2=?,telefax=?,registration_number=?," +
                                "our_contact=?,your_contact=?,vat_number=?,bankgiro=?,plusgiro=?," +
                                "account_number=?,clearing_number=?,eu_sale_commodity=?,eu_sale_third_part=?," +
                                "vat_free_sale=?,hide_unitprice=?,credit_limit=?,discount=?,comment=?," +
                                "currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                                "inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                                "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?," +
                                "del_addr_street=?,del_addr_zipcode=?,del_addr_city=?,del_addr_country=? " +
                                "WHERE number=? AND companyid=?");

                int i = 1;
                iStatement.setObject(i++, iCustomer.getName());
                iStatement.setObject(i++, iCustomer.getEMail());
                iStatement.setObject(i++, iCustomer.getPhone1());
                iStatement.setObject(i++, iCustomer.getPhone2());
                iStatement.setObject(i++, iCustomer.getTelefax());
                iStatement.setObject(i++, iCustomer.getRegistrationNumber());
                iStatement.setObject(i++, iCustomer.getOurContactPerson());
                iStatement.setObject(i++, iCustomer.getYourContactPerson());
                iStatement.setObject(i++, iCustomer.getVATNumber());
                iStatement.setObject(i++, iCustomer.getBankgiro());
                iStatement.setObject(i++, iCustomer.getPlusgiro());
                iStatement.setObject(i++, iCustomer.getAccountNumber());
                iStatement.setObject(i++, iCustomer.getClearingNumber());
                iStatement.setObject(i++, iCustomer.getEuSaleCommodity());
                iStatement.setObject(i++, iCustomer.getEuSaleYhirdPartCommodity());
                iStatement.setObject(i++, iCustomer.getTaxFree());
                iStatement.setObject(i++, iCustomer.getHideUnitprice());
                iStatement.setObject(i++, iCustomer.getCreditLimit());
                iStatement.setObject(i++, iCustomer.getDiscount());
                iStatement.setObject(i++, iCustomer.getComment());
                iStatement.setObject(i++, getCustomerCurrencyCodeV2(iCustomer));
                iStatement.setObject(i++, iCustomer.getPaymentTerm() == null
                        ? null : iCustomer.getPaymentTerm().getName());
                iStatement.setObject(i++, iCustomer.getDeliveryTerm() == null
                        ? null : iCustomer.getDeliveryTerm().getName());
                iStatement.setObject(i++, iCustomer.getDeliveryWay() == null
                        ? null : iCustomer.getDeliveryWay().getName());
                i = bindAddressV2(iStatement, i, iCustomer.getInvoiceAddress());
                i = bindAddressV2(iStatement, i, iCustomer.getDeliveryAddress());
                iStatement.setObject(i++, iCustomer.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_customer SET customer=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iCustomer);
                iStatement.setObject(2, iCustomer.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteCustomer(SSCustomer iCustomer) {
        if (iCustomer == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_customer WHERE number=? AND companyid=?");

            iStatement.setObject(1, iCustomer.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the suppliers for the current company.
     *
     * @return  A List of suppliers or an empty list.
     */
    public List<SSSupplier> getSuppliers() {
        if (iSuppliers != null) {
            return iSuppliers;
        }
        iSuppliers = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iSuppliers;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_supplier WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iSuppliers.add(mapSupplierV2(iResultSet));
                    } else {
                        iSuppliers.add((SSSupplier) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iSuppliers;
    }

    public Optional<SSSupplier> getSupplier(SSSupplier pSupplier) {
        if (pSupplier == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_supplier WHERE number=? AND companyid=?");

            iStatement.setObject(1, pSupplier.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSSupplier iSupplier = useSchemaV2()
                        ? mapSupplierV2(iResultSet)
                        : (SSSupplier) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iSupplier);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSSupplier> getSuppliers(List<SSSupplier> pSuppliers) {
        if (pSuppliers == null) {
            return Collections.emptyList();
        }
        List<SSSupplier> iSuppliers = new LinkedList<>();

        if (this.iSuppliers != null) {
            for (SSSupplier iSupplier : pSuppliers) {
                if (this.iSuppliers.contains(iSupplier)) {
                    iSuppliers.add(iSupplier);
                }
            }
            return iSuppliers;
        }
        if (iCurrentCompany == null) {
            return iSuppliers;
        }
        try {
            for (SSSupplier iSupplier : pSuppliers) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_supplier WHERE number=? AND companyid=?");

                iStatement.setObject(1, iSupplier.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iSuppliers.add(mapSupplierV2(iResultSet));
                    } else {
                        iSuppliers.add((SSSupplier) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iSuppliers;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addSupplier(SSSupplier iSupplier) {
        if (iSupplier == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_supplier(" +
                                "number,companyid,name,phone,phone2,telefax,email,homepage," +
                                "registration_number,your_contact,our_contact,our_customer_nr," +
                                "bankgiro,plusgiro,outpayment_number,comment,currency_code,payment_term," +
                                "delivery_term,delivery_way,addr_name,addr_address,addr_street,addr_zipcode," +
                                "addr_city,addr_country) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                int i = 1;
                iStatement.setObject(i++, iSupplier.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                iStatement.setObject(i++, iSupplier.getName());
                iStatement.setObject(i++, iSupplier.getPhone1());
                iStatement.setObject(i++, iSupplier.getPhone2());
                iStatement.setObject(i++, iSupplier.getTelefax());
                iStatement.setObject(i++, iSupplier.getEMail());
                iStatement.setObject(i++, iSupplier.getHomepage());
                iStatement.setObject(i++, iSupplier.getRegistrationNumber());
                iStatement.setObject(i++, iSupplier.getYourContact());
                iStatement.setObject(i++, iSupplier.getOurContact());
                iStatement.setObject(i++, iSupplier.getOurCustomerNr());
                iStatement.setObject(i++, iSupplier.getBankgiro());
                iStatement.setObject(i++, iSupplier.getPlusgiro());
                iStatement.setObject(i++, iSupplier.getOutpaymentNumber());
                iStatement.setObject(i++, iSupplier.getComment());
                iStatement.setObject(i++, getSupplierCurrencyCodeV2(iSupplier));
                iStatement.setObject(i++, iSupplier.getPaymentTerm() == null
                        ? null : iSupplier.getPaymentTerm().getName());
                iStatement.setObject(i++, iSupplier.getDeliveryTerm() == null
                        ? null : iSupplier.getDeliveryTerm().getName());
                iStatement.setObject(i++, iSupplier.getDeliveryWay() == null
                        ? null : iSupplier.getDeliveryWay().getName());
                bindAddressV2(iStatement, i, iSupplier.getAddress());
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_supplier VALUES(NULL,?,?,?)");

                iStatement.setObject(1, iSupplier.getNumber());
                iStatement.setObject(2, iSupplier);
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateSupplier(SSSupplier iSupplier) {
        if (iSupplier == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_supplier SET " +
                                "name=?,phone=?,phone2=?,telefax=?,email=?,homepage=?,registration_number=?," +
                                "your_contact=?,our_contact=?,our_customer_nr=?,bankgiro=?,plusgiro=?," +
                                "outpayment_number=?,comment=?,currency_code=?,payment_term=?,delivery_term=?," +
                                "delivery_way=?,addr_name=?,addr_address=?,addr_street=?,addr_zipcode=?," +
                                "addr_city=?,addr_country=? WHERE number=? AND companyid=?");

                int i = 1;
                iStatement.setObject(i++, iSupplier.getName());
                iStatement.setObject(i++, iSupplier.getPhone1());
                iStatement.setObject(i++, iSupplier.getPhone2());
                iStatement.setObject(i++, iSupplier.getTelefax());
                iStatement.setObject(i++, iSupplier.getEMail());
                iStatement.setObject(i++, iSupplier.getHomepage());
                iStatement.setObject(i++, iSupplier.getRegistrationNumber());
                iStatement.setObject(i++, iSupplier.getYourContact());
                iStatement.setObject(i++, iSupplier.getOurContact());
                iStatement.setObject(i++, iSupplier.getOurCustomerNr());
                iStatement.setObject(i++, iSupplier.getBankgiro());
                iStatement.setObject(i++, iSupplier.getPlusgiro());
                iStatement.setObject(i++, iSupplier.getOutpaymentNumber());
                iStatement.setObject(i++, iSupplier.getComment());
                iStatement.setObject(i++, getSupplierCurrencyCodeV2(iSupplier));
                iStatement.setObject(i++, iSupplier.getPaymentTerm() == null
                        ? null : iSupplier.getPaymentTerm().getName());
                iStatement.setObject(i++, iSupplier.getDeliveryTerm() == null
                        ? null : iSupplier.getDeliveryTerm().getName());
                iStatement.setObject(i++, iSupplier.getDeliveryWay() == null
                        ? null : iSupplier.getDeliveryWay().getName());
                i = bindAddressV2(iStatement, i, iSupplier.getAddress());
                iStatement.setObject(i++, iSupplier.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_supplier SET supplier=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iSupplier);
                iStatement.setObject(2, iSupplier.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteSupplier(SSSupplier iSupplier) {
        if (iSupplier == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_supplier WHERE number=? AND companyid=?");

            iStatement.setObject(1, iSupplier.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    private String getSupplierCurrencyCodeV2(SSSupplier iSupplier) {
        try {
            SSCurrency iCurrency = iSupplier.getCurrency();
            return iCurrency == null ? null : iCurrency.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private SSSupplier mapSupplierV2(ResultSet iResultSet) throws SQLException {
        SSSupplier iSupplier = new SSSupplier();

        iSupplier.setNumber(iResultSet.getString("number"));
        iSupplier.setName(iResultSet.getString("name"));
        iSupplier.setPhone1(iResultSet.getString("phone"));
        iSupplier.setPhone2(iResultSet.getString("phone2"));
        iSupplier.setTelefax(iResultSet.getString("telefax"));
        iSupplier.setEMail(iResultSet.getString("email"));
        iSupplier.setHomepage(iResultSet.getString("homepage"));
        iSupplier.setRegistrationNumber(iResultSet.getString("registration_number"));
        iSupplier.setYourContact(iResultSet.getString("your_contact"));
        iSupplier.setOurContact(iResultSet.getString("our_contact"));
        iSupplier.setOurCustomerNr(iResultSet.getString("our_customer_nr"));
        iSupplier.setBankGiro(iResultSet.getString("bankgiro"));
        iSupplier.setPlusGiro(iResultSet.getString("plusgiro"));
        iSupplier.setOutpaymentNumber((Integer) iResultSet.getObject("outpayment_number"));
        iSupplier.setComment(iResultSet.getString("comment"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iSupplier.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iSupplier.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iSupplier.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iSupplier.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        }

        iSupplier.setAddress(mapAddressV2(iResultSet, "addr"));
        return iSupplier;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the autodistributions for the current company.
     *
     * @return  A List of autodists or an empty list.
     */
    public List<SSAutoDist> getAutoDists() {
        if (iAutoDists != null) {
            return iAutoDists;
        }
        iAutoDists = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iAutoDists;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_autodist WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iAutoDists.add((SSAutoDist) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iAutoDists;
    }

    public Optional<SSAutoDist> getAutoDist(SSAutoDist pAutoDist) {
        if (pAutoDist == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_autodist WHERE number=? AND companyid=?");

            iStatement.setObject(1, pAutoDist.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSAutoDist iAutoDist = (SSAutoDist) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iAutoDist);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSAutoDist> getAutoDists(List<SSAutoDist> pAutoDists) {
        if (pAutoDists == null) {
            return Collections.emptyList();
        }
        List<SSAutoDist> iAutoDists = new LinkedList<>();

        if (this.iAutoDists != null) {
            for (SSAutoDist iAutoDist : pAutoDists) {
                if (this.iAutoDists.contains(iAutoDist)) {
                    iAutoDists.add(iAutoDist);
                }
            }
            return iAutoDists;
        }
        if (iCurrentCompany == null) {
            return iAutoDists;
        }
        try {
            for (SSAutoDist iAutoDist : pAutoDists) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_autodist WHERE number=? AND companyid=?");

                iStatement.setObject(1, iAutoDist.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iAutoDists.add((SSAutoDist) iResultSet.getObject(3));
                }
                iStatement.close();
            }

            return iAutoDists;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addAutoDist(SSAutoDist iAutoDist) {
        if (iAutoDist == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_autodist VALUES(NULL,?,?,?)");

            iStatement.setObject(1, iAutoDist.getNumber());
            iStatement.setObject(2, iAutoDist);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateAutoDist(SSAutoDist iAutoDist, SSAutoDist iOriginal) {
        if (iAutoDist == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_autodist SET autodist=?, number=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iAutoDist);
            iStatement.setObject(2, iAutoDist.getNumber());
            iStatement.setObject(3, iOriginal.getNumber());
            iStatement.setObject(4, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteAutoDist(SSAutoDist iAutoDist) {
        if (iAutoDist == null || iCurrentCompany == null) {
            return;
        }
        if (iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_autodist WHERE number=? AND companyid=?");

            iStatement.setObject(1, iAutoDist.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    private List<SSSaleRow> getTenderRowsV2(Integer iTenderId) throws SQLException {
        List<SSSaleRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_tender_row WHERE tender_id=? ORDER BY id");
        iStatement.setObject(1, iTenderId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSSaleRow iRow = new SSSaleRow();
            iRow.setProductNr(iResultSet.getString("product_nr"));
            iRow.setDescription(iResultSet.getString("description"));
            iRow.setUnitprice(iResultSet.getBigDecimal("unitprice"));
            iRow.setQuantity((Integer) iResultSet.getObject("count"));

            String iUnit = iResultSet.getString("unit");
            if (iUnit != null) {
                iRow.setUnit(new SSUnit(iUnit, iUnit));
            }

            iRow.setDiscount(iResultSet.getBigDecimal("discount"));

            String iTaxCode = iResultSet.getString("tax_code");
            if (iTaxCode != null) {
                try {
                    iRow.setTaxCode(SSTaxCode.valueOf(iTaxCode));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from partial migrations.
                }
            }

            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replaceTenderRowsV2(Integer iTenderId, SSTender iTender) throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_tender_row WHERE tender_id=?");
        iDelete.setObject(1, iTenderId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSSaleRow iRow : iTender.getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_tender_row(tender_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iTenderId);
            iInsert.setObject(2, iRow.getProductNr());
            iInsert.setObject(3, iRow.getDescription());
            iInsert.setObject(4, iRow.getUnitprice());
            iInsert.setObject(5, iRow.getQuantity());
            iInsert.setObject(6, iRow.getUnit() == null ? null : iRow.getUnit().getName());
            iInsert.setObject(7, iRow.getDiscount());
            iInsert.setObject(8, iRow.getTaxCode() == null ? null : iRow.getTaxCode().name());
            iInsert.setObject(9, iRow.getAccountNr());
            iInsert.setObject(10, iRow.getProjectNr());
            iInsert.setObject(11, iRow.getResultUnitNr());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer getTenderIdV2(Integer iTenderNumber, Integer iCompanyId) throws SQLException {
        if (iTenderNumber == null || iCompanyId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_tender WHERE number=? AND companyid=?");
        iStatement.setObject(1, iTenderNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private int bindTenderColumnsV2(PreparedStatement iStatement, int iIndex, SSTender iTender)
            throws SQLException {
        bindLocalDateV2(iStatement, iIndex++, iTender.getLocalDate());
        iStatement.setObject(iIndex++, iTender.getCustomerNr());
        iStatement.setObject(iIndex++, iTender.getCustomerName());
        iStatement.setObject(iIndex++, iTender.getOurContactPerson());
        iStatement.setObject(iIndex++, iTender.getYourContactPerson());
        iStatement.setObject(iIndex++, iTender.getDelayInterest());
        iStatement.setObject(iIndex++, getCurrencyCodeV2(iTender.getCurrency()));
        iStatement.setObject(iIndex++, iTender.getPaymentTerm() == null ? null : iTender.getPaymentTerm().getName());
        iStatement.setObject(iIndex++, iTender.getDeliveryTerm() == null ? null : iTender.getDeliveryTerm().getName());
        iStatement.setObject(iIndex++, iTender.getDeliveryWay() == null ? null : iTender.getDeliveryWay().getName());
        iStatement.setObject(iIndex++, iTender.getTaxFree());
        iStatement.setObject(iIndex++, iTender.getText());
        iStatement.setObject(iIndex++, iTender.getEuSaleCommodity());
        iStatement.setObject(iIndex++, iTender.getEuSaleThirdPartCommodity());
        iStatement.setObject(iIndex++, iTender.isPrinted());
        bindLocalDateV2(iStatement, iIndex++, iTender.getLocalExpires());
        iStatement.setObject(iIndex++, iTender.getOrderNr());
        iStatement.setObject(iIndex++, iTender.getCurrencyRate());
        iIndex = bindAddressV2(iStatement, iIndex, iTender.getInvoiceAddress());
        return bindAddressV2(iStatement, iIndex, iTender.getDeliveryAddress());
    }

    private SSTender mapTenderV2(ResultSet iResultSet) throws SQLException {
        SSTender iTender = new SSTender();
        iTender.setNumber((Integer) iResultSet.getObject("number"));

        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iTender.setLocalDate(iDate.toLocalDate());
        }

        iTender.setCustomerNr(iResultSet.getString("customer_nr"));
        iTender.setCustomerName(iResultSet.getString("customer_name"));
        iTender.setOurContactPerson(iResultSet.getString("our_contact"));
        iTender.setYourContactPerson(iResultSet.getString("your_contact"));
        iTender.setDelayInterest(iResultSet.getBigDecimal("delay_interest"));
        iTender.setTaxFree(iResultSet.getBoolean("tax_free"));
        iTender.setText(iResultSet.getString("sale_text"));
        iTender.setEuSaleCommodity(iResultSet.getBoolean("eu_sale_commodity"));
        iTender.setEuSaleYhirdPartCommodity(iResultSet.getBoolean("eu_sale_third_part"));
        iTender.setPrinted(iResultSet.getBoolean("printed"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iTender.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        } else {
            iTender.setCurrency(null);
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iTender.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        } else {
            iTender.setPaymentTerm(null);
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iTender.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        } else {
            iTender.setDeliveryTerm(null);
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iTender.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        } else {
            iTender.setDeliveryWay(null);
        }

        java.sql.Date iExpires = iResultSet.getDate("expires");
        if (iExpires != null) {
            iTender.setLocalExpires(iExpires.toLocalDate());
        }

        Integer iOrderNr = (Integer) iResultSet.getObject("order_nr");
        if (iOrderNr != null) {
            SSOrder iOrder = new SSOrder();
            iOrder.setNumber(iOrderNr);
            iTender.setOrder(iOrder);
        }

        iTender.setCurrencyRate(iResultSet.getBigDecimal("currency_rate"));
        iTender.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iTender.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        iTender.getRows().clear();
        iTender.getRows().addAll(getTenderRowsV2(iResultSet.getInt("id")));
        return iTender;
    }

    /**
     * Returns the tenders in the current company.
     *
     * @return  A List of tenders or an empty list.
     */
    public List<SSTender> getTenders() {
        if (iTenders != null) {
            return iTenders;
        }
        iTenders = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iTenders;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_tender WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iTenders.add(mapTenderV2(iResultSet));
                    } else {
                        iTenders.add((SSTender) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iTenders;
    }

    public Optional<SSTender> getTender(SSTender pTender) {
        if (pTender == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_tender WHERE number=? AND companyid=?");

            iStatement.setObject(1, pTender.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSTender iTender = useSchemaV2()
                        ? mapTenderV2(iResultSet)
                        : (SSTender) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iTender);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSTender> getTenders(List<SSTender> pTenders) {
        if (pTenders == null) {
            return Collections.emptyList();
        }
        List<SSTender> iTenders = new LinkedList<>();

        if (this.iTenders != null) {
            for (SSTender iTender : pTenders) {
                if (this.iTenders.contains(iTender)) {
                    iTenders.add(iTender);
                }
            }
            return iTenders;
        }
        if (iCurrentCompany == null) {
            return iTenders;
        }

        try {
            for (SSTender iTender : pTenders) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_tender WHERE number=? AND companyid=?");

                iStatement.setObject(1, iTender.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iTenders.add(mapTenderV2(iResultSet));
                    } else {
                        iTenders.add((SSTender) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iTenders;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addTender(SSTender iTender) {
        if (iTender == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_tender WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "tender");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iTender.setNumber(iNumber + 1);
                } else {
                    iTender.setNumber(iCompanyNumber + 1);
                }
            } else {
                iTender.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            Integer iTenderId = null;
            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_tender(" +
                                "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                                "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                                "sale_text,eu_sale_commodity,eu_sale_third_part,printed,expires,order_nr," +
                                "currency_rate,inv_addr_name,inv_addr_address,inv_addr_street,inv_addr_zipcode," +
                                "inv_addr_city,inv_addr_country,del_addr_name,del_addr_address,del_addr_street," +
                                "del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                iStatement.setObject(i++, iTender.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                bindTenderColumnsV2(iStatement, i, iTender);
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_tender VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iTender.getNumber());
                iStatement.setObject(2, iTender);
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iTenderId = iKeys.getInt(1);
                    }
                }
                if (iTenderId == null) {
                    iTenderId = getTenderIdV2(iTender.getNumber(), iCurrentCompany.getId());
                }
                if (iTenderId != null) {
                    replaceTenderRowsV2(iTenderId, iTender);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateTender(SSTender iTender) {
        if (iTender == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iTenderId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_tender SET " +
                                "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                                "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                                "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                                "expires=?,order_nr=?,currency_rate=?,inv_addr_name=?,inv_addr_address=?," +
                                "inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?,inv_addr_country=?," +
                                "del_addr_name=?,del_addr_address=?,del_addr_street=?,del_addr_zipcode=?," +
                                "del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?");
                int i = bindTenderColumnsV2(iStatement, 1, iTender);
                iStatement.setObject(i++, iTender.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_tender SET tender=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iTender);
                iStatement.setObject(2, iTender.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iTenderId = getTenderIdV2(iTender.getNumber(), iCurrentCompany.getId());
                if (iTenderId != null) {
                    replaceTenderRowsV2(iTenderId, iTender);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteTender(SSTender iTender) {
        if (iTender == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iTenderId = getTenderIdV2(iTender.getNumber(), iCurrentCompany.getId());
                if (iTenderId != null) {
                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_tender_row WHERE tender_id=?");
                    iDeleteRows.setObject(1, iTenderId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_tender WHERE number=? AND companyid=?");

            iStatement.setObject(1, iTender.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    private List<SSSaleRow> getOrderRowsV2(Integer iOrderId) throws SQLException {
        List<SSSaleRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_order_row WHERE order_id=? ORDER BY id");
        iStatement.setObject(1, iOrderId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSSaleRow iRow = new SSSaleRow();
            iRow.setProductNr(iResultSet.getString("product_nr"));
            iRow.setDescription(iResultSet.getString("description"));
            iRow.setUnitprice(iResultSet.getBigDecimal("unitprice"));
            iRow.setQuantity((Integer) iResultSet.getObject("count"));

            String iUnit = iResultSet.getString("unit");
            if (iUnit != null) {
                iRow.setUnit(new SSUnit(iUnit, iUnit));
            }

            iRow.setDiscount(iResultSet.getBigDecimal("discount"));

            String iTaxCode = iResultSet.getString("tax_code");
            if (iTaxCode != null) {
                try {
                    iRow.setTaxCode(SSTaxCode.valueOf(iTaxCode));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from partial migrations.
                }
            }

            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replaceOrderRowsV2(Integer iOrderId, SSOrder iOrder) throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_order_row WHERE order_id=?");
        iDelete.setObject(1, iOrderId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSSaleRow iRow : iOrder.getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_order_row(order_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iOrderId);
            iInsert.setObject(2, iRow.getProductNr());
            iInsert.setObject(3, iRow.getDescription());
            iInsert.setObject(4, iRow.getUnitprice());
            iInsert.setObject(5, iRow.getQuantity());
            iInsert.setObject(6, iRow.getUnit() == null ? null : iRow.getUnit().getName());
            iInsert.setObject(7, iRow.getDiscount());
            iInsert.setObject(8, iRow.getTaxCode() == null ? null : iRow.getTaxCode().name());
            iInsert.setObject(9, iRow.getAccountNr());
            iInsert.setObject(10, iRow.getProjectNr());
            iInsert.setObject(11, iRow.getResultUnitNr());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer getOrderIdV2(Integer iOrderNumber, Integer iCompanyId) throws SQLException {
        if (iOrderNumber == null || iCompanyId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_order WHERE number=? AND companyid=?");
        iStatement.setObject(1, iOrderNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private int bindOrderColumnsV2(PreparedStatement iStatement, int iIndex, SSOrder iOrder)
            throws SQLException {
        bindLocalDateV2(iStatement, iIndex++, iOrder.getLocalDate());
        iStatement.setObject(iIndex++, iOrder.getCustomerNr());
        iStatement.setObject(iIndex++, iOrder.getCustomerName());
        iStatement.setObject(iIndex++, iOrder.getOurContactPerson());
        iStatement.setObject(iIndex++, iOrder.getYourContactPerson());
        iStatement.setObject(iIndex++, iOrder.getDelayInterest());
        iStatement.setObject(iIndex++, getCurrencyCodeV2(iOrder.getCurrency()));
        iStatement.setObject(iIndex++, iOrder.getPaymentTerm() == null ? null : iOrder.getPaymentTerm().getName());
        iStatement.setObject(iIndex++, iOrder.getDeliveryTerm() == null ? null : iOrder.getDeliveryTerm().getName());
        iStatement.setObject(iIndex++, iOrder.getDeliveryWay() == null ? null : iOrder.getDeliveryWay().getName());
        iStatement.setObject(iIndex++, iOrder.getTaxFree());
        iStatement.setObject(iIndex++, iOrder.getText());
        iStatement.setObject(iIndex++, iOrder.getEuSaleCommodity());
        iStatement.setObject(iIndex++, iOrder.getEuSaleThirdPartCommodity());
        iStatement.setObject(iIndex++, iOrder.isPrinted());
        iStatement.setObject(iIndex++, iOrder.getYourOrderNumber());
        iStatement.setObject(iIndex++, iOrder.getEstimatedDelivery());
        iStatement.setObject(iIndex++, iOrder.getInvoiceNr());
        iStatement.setObject(iIndex++, iOrder.getPeriodicInvoiceNr());
        iStatement.setObject(iIndex++, iOrder.getPurchaseOrderNr());
        iStatement.setObject(iIndex++, iOrder.getHideUnitprice());
        iStatement.setObject(iIndex++, iOrder.getCurrencyRate());
        iIndex = bindAddressV2(iStatement, iIndex, iOrder.getInvoiceAddress());
        return bindAddressV2(iStatement, iIndex, iOrder.getDeliveryAddress());
    }

    private SSOrder mapOrderV2(ResultSet iResultSet) throws SQLException {
        SSOrder iOrder = new SSOrder();

        iOrder.setNumber((Integer) iResultSet.getObject("number"));

        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iOrder.setLocalDate(iDate.toLocalDate());
        }

        iOrder.setCustomerNr(iResultSet.getString("customer_nr"));
        iOrder.setCustomerName(iResultSet.getString("customer_name"));
        iOrder.setOurContactPerson(iResultSet.getString("our_contact"));
        iOrder.setYourContactPerson(iResultSet.getString("your_contact"));
        iOrder.setDelayInterest(iResultSet.getBigDecimal("delay_interest"));
        iOrder.setTaxFree(iResultSet.getBoolean("tax_free"));
        iOrder.setText(iResultSet.getString("sale_text"));
        iOrder.setEuSaleCommodity(iResultSet.getBoolean("eu_sale_commodity"));
        iOrder.setEuSaleYhirdPartCommodity(iResultSet.getBoolean("eu_sale_third_part"));
        iOrder.setPrinted(iResultSet.getBoolean("printed"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iOrder.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        } else {
            iOrder.setCurrency(null);
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iOrder.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        } else {
            iOrder.setPaymentTerm(null);
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iOrder.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        } else {
            iOrder.setDeliveryTerm(null);
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iOrder.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        } else {
            iOrder.setDeliveryWay(null);
        }

        iOrder.setYourOrderNumber(iResultSet.getString("your_order_number"));
        iOrder.setEstimatedDelivery(iResultSet.getString("estimated_delivery"));
        iOrder.setInvoiceNr((Integer) iResultSet.getObject("invoice_nr"));
        iOrder.setPeriodicInvoiceNr((Integer) iResultSet.getObject("periodicinvoice_nr"));
        Integer iPurchaseOrderNr = (Integer) iResultSet.getObject("purchaseorder_nr");
        if (iPurchaseOrderNr != null) {
            SSPurchaseOrder iPurchaseOrder = new SSPurchaseOrder();
            iPurchaseOrder.setNumber(iPurchaseOrderNr);
            iOrder.setPurchaseOrder(iPurchaseOrder);
        }
        iOrder.setHideUnitprice(iResultSet.getBoolean("hide_unitprice"));
        iOrder.setCurrencyRate(iResultSet.getBigDecimal("currency_rate"));

        iOrder.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iOrder.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        iOrder.getRows().clear();
        iOrder.getRows().addAll(getOrderRowsV2(iResultSet.getInt("id")));
        return iOrder;
    }

    public List<SSOrder> getOrders() {
        if (iOrders != null) {
            return iOrders;
        }
        iOrders = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iOrders;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_order WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iOrders.add(mapOrderV2(iResultSet));
                    } else {
                        iOrders.add((SSOrder) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iOrders;
    }

    public Optional<SSOrder> getOrder(SSOrder pOrder) {
        if (pOrder == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_order WHERE number=? AND companyid=?");

            iStatement.setObject(1, pOrder.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSOrder iOrder = useSchemaV2()
                        ? mapOrderV2(iResultSet)
                        : (SSOrder) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iOrder);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSOrder> getOrders(List<SSOrder> pOrders) {
        if (pOrders == null) {
            return Collections.emptyList();
        }
        List<SSOrder> iOrders = new LinkedList<>();

        if (this.iOrders != null) {
            for (SSOrder iOrder : pOrders) {
                if (this.iOrders.contains(iOrder)) {
                    iOrders.add(iOrder);
                }
            }
            return iOrders;
        }
        if (iCurrentCompany == null) {
            return iOrders;
        }
        try {
            for (SSOrder iOrder : pOrders) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_order WHERE number=? AND companyid=?");

                iStatement.setObject(1, iOrder.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iOrders.add(mapOrderV2(iResultSet));
                    } else {
                        iOrders.add((SSOrder) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iOrders;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addOrder(SSOrder iOrder) {
        if (iOrder == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_order WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "order");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iOrder.setNumber(iNumber + 1);
                } else {
                    iOrder.setNumber(iCompanyNumber + 1);
                }
            } else {
                iOrder.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            Integer iOrderId = null;
            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_order(" +
                                "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                                "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                                "sale_text,eu_sale_commodity,eu_sale_third_part,printed,your_order_number," +
                                "estimated_delivery,invoice_nr,periodicinvoice_nr,purchaseorder_nr,hide_unitprice," +
                                "currency_rate,inv_addr_name,inv_addr_address,inv_addr_street,inv_addr_zipcode," +
                                "inv_addr_city,inv_addr_country,del_addr_name,del_addr_address,del_addr_street," +
                                "del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                iStatement.setObject(i++, iOrder.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                bindOrderColumnsV2(iStatement, i, iOrder);
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_order VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iOrder.getNumber());
                iStatement.setObject(2, iOrder);
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iOrderId = iKeys.getInt(1);
                    }
                }
                if (iOrderId == null) {
                    iOrderId = getOrderIdV2(iOrder.getNumber(), iCurrentCompany.getId());
                }
                if (iOrderId != null) {
                    replaceOrderRowsV2(iOrderId, iOrder);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateOrder(SSOrder iOrder) {
        if (iOrder == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iOrderId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_order SET " +
                                "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                                "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                                "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                                "your_order_number=?,estimated_delivery=?,invoice_nr=?,periodicinvoice_nr=?," +
                                "purchaseorder_nr=?,hide_unitprice=?,currency_rate=?,inv_addr_name=?," +
                                "inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?," +
                                "inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                                "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?");
                int i = bindOrderColumnsV2(iStatement, 1, iOrder);
                iStatement.setObject(i++, iOrder.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_order SET iorder=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iOrder);
                iStatement.setObject(2, iOrder.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iOrderId = getOrderIdV2(iOrder.getNumber(), iCurrentCompany.getId());
                if (iOrderId != null) {
                    replaceOrderRowsV2(iOrderId, iOrder);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteOrder(SSOrder iOrder) {
        if (iOrder == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iOrderId = getOrderIdV2(iOrder.getNumber(), iCurrentCompany.getId());
                if (iOrderId != null) {
                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_order_row WHERE order_id=?");
                    iDeleteRows.setObject(1, iOrderId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_order WHERE number=? AND companyid=?");

            iStatement.setObject(1, iOrder.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    private void bindLocalDateV2(PreparedStatement iStatement, int iIndex, java.time.LocalDate iDate)
            throws SQLException {
        if (iDate == null) {
            iStatement.setNull(iIndex, Types.DATE);
        } else {
            iStatement.setObject(iIndex, java.sql.Date.valueOf(iDate));
        }
    }

    private String getCurrencyCodeV2(SSCurrency iCurrency) {
        return iCurrency == null ? null : iCurrency.getName();
    }

    private List<SSSaleRow> getInvoiceRowsV2(Integer iInvoiceId) throws SQLException {
        List<SSSaleRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_invoice_row WHERE invoice_id=? ORDER BY id");
        iStatement.setObject(1, iInvoiceId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSSaleRow iRow = new SSSaleRow();
            iRow.setProductNr(iResultSet.getString("product_nr"));
            iRow.setDescription(iResultSet.getString("description"));
            iRow.setUnitprice(iResultSet.getBigDecimal("unitprice"));
            iRow.setQuantity((Integer) iResultSet.getObject("count"));

            String iUnit = iResultSet.getString("unit");
            if (iUnit != null) {
                iRow.setUnit(new SSUnit(iUnit, iUnit));
            }

            iRow.setDiscount(iResultSet.getBigDecimal("discount"));

            String iTaxCode = iResultSet.getString("tax_code");
            if (iTaxCode != null) {
                try {
                    iRow.setTaxCode(SSTaxCode.valueOf(iTaxCode));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from partial migrations.
                }
            }

            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replaceInvoiceRowsV2(Integer iInvoiceId, SSInvoice iInvoice) throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_invoice_row WHERE invoice_id=?");
        iDelete.setObject(1, iInvoiceId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSSaleRow iRow : iInvoice.getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_invoice_row(invoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iInvoiceId);
            iInsert.setObject(2, iRow.getProductNr());
            iInsert.setObject(3, iRow.getDescription());
            iInsert.setObject(4, iRow.getUnitprice());
            iInsert.setObject(5, iRow.getQuantity());
            iInsert.setObject(6, iRow.getUnit() == null ? null : iRow.getUnit().getName());
            iInsert.setObject(7, iRow.getDiscount());
            iInsert.setObject(8, iRow.getTaxCode() == null ? null : iRow.getTaxCode().name());
            iInsert.setObject(9, iRow.getAccountNr());
            iInsert.setObject(10, iRow.getProjectNr());
            iInsert.setObject(11, iRow.getResultUnitNr());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer getInvoiceIdV2(Integer iInvoiceNumber, Integer iCompanyId) throws SQLException {
        if (iInvoiceNumber == null || iCompanyId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_invoice WHERE number=? AND companyid=?");
        iStatement.setObject(1, iInvoiceNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private int bindInvoiceColumnsV2(PreparedStatement iStatement, int iIndex, SSInvoice iInvoice)
            throws SQLException {
        bindLocalDateV2(iStatement, iIndex++, iInvoice.getLocalDate());
        iStatement.setObject(iIndex++, iInvoice.getCustomerNr());
        iStatement.setObject(iIndex++, iInvoice.getCustomerName());
        iStatement.setObject(iIndex++, iInvoice.getOurContactPerson());
        iStatement.setObject(iIndex++, iInvoice.getYourContactPerson());
        iStatement.setObject(iIndex++, iInvoice.getDelayInterest());
        iStatement.setObject(iIndex++, getCurrencyCodeV2(iInvoice.getCurrency()));
        iStatement.setObject(iIndex++, iInvoice.getPaymentTerm() == null ? null : iInvoice.getPaymentTerm().getName());
        iStatement.setObject(iIndex++, iInvoice.getDeliveryTerm() == null ? null : iInvoice.getDeliveryTerm().getName());
        iStatement.setObject(iIndex++, iInvoice.getDeliveryWay() == null ? null : iInvoice.getDeliveryWay().getName());
        iStatement.setObject(iIndex++, iInvoice.getTaxFree());
        iStatement.setObject(iIndex++, iInvoice.getText());
        iStatement.setObject(iIndex++, iInvoice.getEuSaleCommodity());
        iStatement.setObject(iIndex++, iInvoice.getEuSaleThirdPartCommodity());
        iStatement.setObject(iIndex++, iInvoice.isPrinted());
        iStatement.setObject(iIndex++, iInvoice.getType() == null ? null : iInvoice.getType().name());
        iStatement.setObject(iIndex++, iInvoice.getCurrencyRate());
        bindLocalDateV2(iStatement, iIndex++, iInvoice.getLocalDueDate());
        iStatement.setObject(iIndex++, iInvoice.getYourOrderNumber());
        iStatement.setObject(iIndex++, iInvoice.getOCRNumber());
        iStatement.setObject(iIndex++, iInvoice.isEntered());
        iStatement.setObject(iIndex++, iInvoice.getNumReminders());
        iStatement.setObject(iIndex++, iInvoice.isInterestInvoiced());
        iStatement.setObject(iIndex++, iInvoice.isStockInfluencing());
        iStatement.setObject(iIndex++, iInvoice.getOrderNumbers());
        iStatement.setObject(iIndex++, getVoucherIdByNumberV2(iInvoice.getVoucher()));
        iIndex = bindAddressV2(iStatement, iIndex, iInvoice.getInvoiceAddress());
        return bindAddressV2(iStatement, iIndex, iInvoice.getDeliveryAddress());
    }

    private SSInvoice mapInvoiceV2(ResultSet iResultSet) throws SQLException {
        SSInvoice iInvoice = new SSInvoice();

        iInvoice.setNumber((Integer) iResultSet.getObject("number"));

        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iInvoice.setLocalDate(iDate.toLocalDate());
        }

        iInvoice.setCustomerNr(iResultSet.getString("customer_nr"));
        iInvoice.setCustomerName(iResultSet.getString("customer_name"));
        iInvoice.setOurContactPerson(iResultSet.getString("our_contact"));
        iInvoice.setYourContactPerson(iResultSet.getString("your_contact"));
        iInvoice.setDelayInterest(iResultSet.getBigDecimal("delay_interest"));
        iInvoice.setTaxFree(iResultSet.getBoolean("tax_free"));
        iInvoice.setText(iResultSet.getString("sale_text"));
        iInvoice.setEuSaleCommodity(iResultSet.getBoolean("eu_sale_commodity"));
        iInvoice.setEuSaleYhirdPartCommodity(iResultSet.getBoolean("eu_sale_third_part"));
        iInvoice.setPrinted(iResultSet.getBoolean("printed"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iInvoice.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iInvoice.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iInvoice.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iInvoice.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        }

        String iInvoiceType = iResultSet.getString("invoice_type");
        if (iInvoiceType != null) {
            try {
                iInvoice.setType(SSInvoiceType.valueOf(iInvoiceType));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown enum values from partial migrations.
            }
        }

        iInvoice.setCurrencyRate(iResultSet.getBigDecimal("currency_rate"));

        java.sql.Date iPaymentDay = iResultSet.getDate("payment_day");
        if (iPaymentDay != null) {
            iInvoice.setLocalDueDate(iPaymentDay.toLocalDate());
        }

        iInvoice.setYourOrderNumber(iResultSet.getString("your_order_number"));
        iInvoice.setOCRNumber(iResultSet.getString("ocr_number"));
        iInvoice.setEntered(iResultSet.getBoolean("entered"));
        iInvoice.setNumRemainders(iResultSet.getInt("num_reminders"));
        iInvoice.setInterestInvoiced(iResultSet.getBoolean("interest_invoiced"));
        iInvoice.setStockInfluencing(iResultSet.getBoolean("stock_influencing"));
        iInvoice.setOrderNumbers(iResultSet.getString("order_numbers"));

        Integer iVoucherId = (Integer) iResultSet.getObject("voucher_id");
        Integer iVoucherNumber = getVoucherNumberForIdV2(iVoucherId);
        if (iVoucherNumber != null) {
            iInvoice.setVoucher(new SSVoucher(iVoucherNumber));
        }

        iInvoice.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iInvoice.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        iInvoice.getRows().clear();
        iInvoice.getRows().addAll(getInvoiceRowsV2(iResultSet.getInt("id")));
        return iInvoice;
    }

    public List<SSInvoice> getInvoices() {
        if (iInvoices != null) {
            return iInvoices;
        }
        iInvoices = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iInvoices;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_invoice WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();

                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iInvoices.add(mapInvoiceV2(iResultSet));
                    } else {
                        iInvoices.add((SSInvoice) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iInvoices;
    }

    public Optional<SSInvoice> getInvoice(SSInvoice pInvoice) {
        if (pInvoice == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_invoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, pInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSInvoice iInvoice = useSchemaV2()
                        ? mapInvoiceV2(iResultSet)
                        : (SSInvoice) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iInvoice);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSInvoice> getInvoices(List<SSInvoice> pInvoices) {
        if (pInvoices == null) {
            return Collections.emptyList();
        }
        List<SSInvoice> iInvoices = new LinkedList<>();

        if (this.iInvoices != null) {
            for (SSInvoice iInvoice : pInvoices) {
                if (this.iInvoices.contains(iInvoice)) {
                    iInvoices.add(iInvoice);
                }
            }
            return iInvoices;
        }
        if (iCurrentCompany == null) {
            return iInvoices;
        }
        try {
            for (SSInvoice iInvoice : pInvoices) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_invoice WHERE number=? AND companyid=?");

                iStatement.setObject(1, iInvoice.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iInvoices.add(mapInvoiceV2(iResultSet));
                    } else {
                        iInvoices.add((SSInvoice) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iInvoices;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addInvoice(SSInvoice iInvoice) {
        if (iInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_invoice WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "invoice");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iInvoice.setNumber(iNumber + 1);
                } else {
                    iInvoice.setNumber(iCompanyNumber + 1);
                }
            } else {
                iInvoice.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            Integer iInvoiceId = null;
            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_invoice(" +
                                "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                                "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                                "sale_text,eu_sale_commodity,eu_sale_third_part,printed,invoice_type,currency_rate," +
                                "payment_day,your_order_number,ocr_number,entered,num_reminders,interest_invoiced," +
                                "stock_influencing,order_numbers,voucher_id,inv_addr_name,inv_addr_address," +
                                "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                                "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                iStatement.setObject(i++, iInvoice.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                bindInvoiceColumnsV2(iStatement, i, iInvoice);
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_invoice VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iInvoice.getNumber());
                iStatement.setObject(2, iInvoice);
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iInvoiceId = iKeys.getInt(1);
                    }
                }
                if (iInvoiceId == null) {
                    iInvoiceId = getInvoiceIdV2(iInvoice.getNumber(), iCurrentCompany.getId());
                }
                if (iInvoiceId != null) {
                    replaceInvoiceRowsV2(iInvoiceId, iInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateInvoice(SSInvoice iInvoice) {
        if (iInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iInvoiceId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_invoice SET " +
                                "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                                "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                                "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                                "invoice_type=?,currency_rate=?,payment_day=?,your_order_number=?,ocr_number=?," +
                                "entered=?,num_reminders=?,interest_invoiced=?,stock_influencing=?,order_numbers=?," +
                                "voucher_id=?,inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                                "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                                "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?");
                int i = bindInvoiceColumnsV2(iStatement, 1, iInvoice);
                iStatement.setObject(i++, iInvoice.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_invoice SET invoice=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iInvoice);
                iStatement.setObject(2, iInvoice.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iInvoiceId = getInvoiceIdV2(iInvoice.getNumber(), iCurrentCompany.getId());
                if (iInvoiceId != null) {
                    replaceInvoiceRowsV2(iInvoiceId, iInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteInvoice(SSInvoice iInvoice) {
        if (iInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iInvoiceId = getInvoiceIdV2(iInvoice.getNumber(), iCurrentCompany.getId());
                if (iInvoiceId != null) {
                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_invoice_row WHERE invoice_id=?");
                    iDeleteRows.setObject(1, iInvoiceId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_invoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, iInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the inpayments in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSInpayment> getInpayments() {
        if (iInpayments != null) {
            return iInpayments;
        }
        iInpayments = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iInpayments;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_inpayment WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iInpayments.add((SSInpayment) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iInpayments;
    }

    public Optional<SSInpayment> getInpayment(SSInpayment pInpayment) {
        if (pInpayment == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_inpayment WHERE number=? AND companyid=?");

            iStatement.setObject(1, pInpayment.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSInpayment iInpayment = (SSInpayment) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iInpayment);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addInpayment(SSInpayment iInpayment) {
        if (iInpayment == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_inpayment WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "inpayment");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iInpayment.setNumber(iNumber + 1);
                } else {
                    iInpayment.setNumber(iCompanyNumber + 1);
                }
            } else {
                iInpayment.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_inpayment VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iInpayment.getNumber());
            iStatement.setObject(2, iInpayment);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateInpayment(SSInpayment iInpayment) {
        if (iInpayment == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_inpayment SET inpayment=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iInpayment);
            iStatement.setObject(2, iInpayment.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteInpayment(SSInpayment iInpayment) {
        if (iInpayment == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_inpayment WHERE number=? AND companyid=?");

            iStatement.setObject(1, iInpayment.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    /**
     * Returns the outpayments in the current company.
     *
     * @return  A List of outpayments or an empty list.
     */
    public List<SSOutpayment> getOutpayments() {
        if (iOutpayments != null) {
            return iOutpayments;
        }
        iOutpayments = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iOutpayments;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_outpayment WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iOutpayments.add((SSOutpayment) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iOutpayments;
    }

    public Optional<SSOutpayment> getOutpayment(SSOutpayment pOutpayment) {
        if (pOutpayment == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_outpayment WHERE number=? AND companyid=?");

            iStatement.setObject(1, pOutpayment.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSOutpayment iOutpayment = (SSOutpayment) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iOutpayment);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addOutpayment(SSOutpayment iOutpayment) {
        if (iOutpayment == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_outpayment WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "outpayment");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iOutpayment.setNumber(iNumber + 1);
                } else {
                    iOutpayment.setNumber(iCompanyNumber + 1);
                }
            } else {
                iOutpayment.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_outpayment VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iOutpayment.getNumber());
            iStatement.setObject(2, iOutpayment);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateOutpayment(SSOutpayment iOutpayment) {
        if (iOutpayment == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_outpayment SET outpayment=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iOutpayment);
            iStatement.setObject(2, iOutpayment.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteOutpayment(SSOutpayment iOutpayment) {
        if (iOutpayment == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_outpayment WHERE number=? AND companyid=?");

            iStatement.setObject(1, iOutpayment.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    private List<SSSaleRow> getCreditInvoiceRowsV2(Integer iCreditInvoiceId) throws SQLException {
        List<SSSaleRow> iRows = new LinkedList<>();
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_creditinvoice_row WHERE creditinvoice_id=? ORDER BY id");
        iStatement.setObject(1, iCreditInvoiceId);
        ResultSet iResultSet = iStatement.executeQuery();
        while (iResultSet.next()) {
            SSSaleRow iRow = new SSSaleRow();
            iRow.setProductNr(iResultSet.getString("product_nr"));
            iRow.setDescription(iResultSet.getString("description"));
            iRow.setUnitprice(iResultSet.getBigDecimal("unitprice"));
            iRow.setQuantity((Integer) iResultSet.getObject("count"));

            String iUnit = iResultSet.getString("unit");
            if (iUnit != null) {
                iRow.setUnit(new SSUnit(iUnit, iUnit));
            }

            iRow.setDiscount(iResultSet.getBigDecimal("discount"));

            String iTaxCode = iResultSet.getString("tax_code");
            if (iTaxCode != null) {
                try {
                    iRow.setTaxCode(SSTaxCode.valueOf(iTaxCode));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown enum values from partial migrations.
                }
            }

            iRow.setAccountNr((Integer) iResultSet.getObject("account_nr"));
            iRow.setProjectNr(iResultSet.getString("project_number"));
            iRow.setResultUnitNr(iResultSet.getString("result_unit_number"));
            iRows.add(iRow);
        }
        iResultSet.close();
        iStatement.close();
        return iRows;
    }

    private void replaceCreditInvoiceRowsV2(Integer iCreditInvoiceId, SSCreditInvoice iCreditInvoice)
            throws SQLException {
        PreparedStatement iDelete = iConnection.prepareStatement(
                "DELETE FROM tbl_creditinvoice_row WHERE creditinvoice_id=?");
        iDelete.setObject(1, iCreditInvoiceId);
        iDelete.executeUpdate();
        iDelete.close();

        for (SSSaleRow iRow : iCreditInvoice.getRows()) {
            PreparedStatement iInsert = iConnection.prepareStatement(
                    "INSERT INTO tbl_creditinvoice_row(creditinvoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            iInsert.setObject(1, iCreditInvoiceId);
            iInsert.setObject(2, iRow.getProductNr());
            iInsert.setObject(3, iRow.getDescription());
            iInsert.setObject(4, iRow.getUnitprice());
            iInsert.setObject(5, iRow.getQuantity());
            iInsert.setObject(6, iRow.getUnit() == null ? null : iRow.getUnit().getName());
            iInsert.setObject(7, iRow.getDiscount());
            iInsert.setObject(8, iRow.getTaxCode() == null ? null : iRow.getTaxCode().name());
            iInsert.setObject(9, iRow.getAccountNr());
            iInsert.setObject(10, iRow.getProjectNr());
            iInsert.setObject(11, iRow.getResultUnitNr());
            iInsert.executeUpdate();
            iInsert.close();
        }
    }

    private Integer getCreditInvoiceIdV2(Integer iCreditInvoiceNumber, Integer iCompanyId)
            throws SQLException {
        if (iCreditInvoiceNumber == null || iCompanyId == null) {
            return null;
        }
        PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT id FROM tbl_creditinvoice WHERE number=? AND companyid=?");
        iStatement.setObject(1, iCreditInvoiceNumber);
        iStatement.setObject(2, iCompanyId);
        ResultSet iResultSet = iStatement.executeQuery();
        try {
            if (iResultSet.next()) {
                return iResultSet.getInt(1);
            }
            return null;
        } finally {
            iResultSet.close();
            iStatement.close();
        }
    }

    private int bindCreditInvoiceColumnsV2(PreparedStatement iStatement, int iIndex,
                                            SSCreditInvoice iCreditInvoice) throws SQLException {
        iStatement.setObject(iIndex++, iCreditInvoice.getCreditingNr());
        bindLocalDateV2(iStatement, iIndex++, iCreditInvoice.getLocalDate());
        iStatement.setObject(iIndex++, iCreditInvoice.getCustomerNr());
        iStatement.setObject(iIndex++, iCreditInvoice.getCustomerName());
        iStatement.setObject(iIndex++, iCreditInvoice.getOurContactPerson());
        iStatement.setObject(iIndex++, iCreditInvoice.getYourContactPerson());
        iStatement.setObject(iIndex++, iCreditInvoice.getDelayInterest());
        iStatement.setObject(iIndex++, getCurrencyCodeV2(iCreditInvoice.getCurrency()));
        iStatement.setObject(iIndex++, iCreditInvoice.getPaymentTerm() == null ? null : iCreditInvoice.getPaymentTerm().getName());
        iStatement.setObject(iIndex++, iCreditInvoice.getDeliveryTerm() == null ? null : iCreditInvoice.getDeliveryTerm().getName());
        iStatement.setObject(iIndex++, iCreditInvoice.getDeliveryWay() == null ? null : iCreditInvoice.getDeliveryWay().getName());
        iStatement.setObject(iIndex++, iCreditInvoice.getTaxFree());
        iStatement.setObject(iIndex++, iCreditInvoice.getText());
        iStatement.setObject(iIndex++, iCreditInvoice.getEuSaleCommodity());
        iStatement.setObject(iIndex++, iCreditInvoice.getEuSaleThirdPartCommodity());
        iStatement.setObject(iIndex++, iCreditInvoice.isPrinted());
        iStatement.setObject(iIndex++, iCreditInvoice.getType() == null ? null : iCreditInvoice.getType().name());
        iStatement.setObject(iIndex++, iCreditInvoice.getCurrencyRate());
        bindLocalDateV2(iStatement, iIndex++, iCreditInvoice.getLocalDueDate());
        iStatement.setObject(iIndex++, iCreditInvoice.getYourOrderNumber());
        iStatement.setObject(iIndex++, iCreditInvoice.getOCRNumber());
        iStatement.setObject(iIndex++, iCreditInvoice.isEntered());
        iStatement.setObject(iIndex++, iCreditInvoice.getNumReminders());
        iStatement.setObject(iIndex++, iCreditInvoice.isInterestInvoiced());
        iStatement.setObject(iIndex++, iCreditInvoice.isStockInfluencing());
        iStatement.setObject(iIndex++, iCreditInvoice.getOrderNumbers());
        iStatement.setObject(iIndex++, getVoucherIdByNumberV2(iCreditInvoice.getVoucher()));
        iIndex = bindAddressV2(iStatement, iIndex, iCreditInvoice.getInvoiceAddress());
        return bindAddressV2(iStatement, iIndex, iCreditInvoice.getDeliveryAddress());
    }

    private SSCreditInvoice mapCreditInvoiceV2(ResultSet iResultSet) throws SQLException {
        SSCreditInvoice iCreditInvoice = new SSCreditInvoice();

        iCreditInvoice.setNumber((Integer) iResultSet.getObject("number"));
        iCreditInvoice.setCreditingNr((Integer) iResultSet.getObject("crediting_nr"));

        java.sql.Date iDate = iResultSet.getDate("vdate");
        if (iDate != null) {
            iCreditInvoice.setLocalDate(iDate.toLocalDate());
        }

        iCreditInvoice.setCustomerNr(iResultSet.getString("customer_nr"));
        iCreditInvoice.setCustomerName(iResultSet.getString("customer_name"));
        iCreditInvoice.setOurContactPerson(iResultSet.getString("our_contact"));
        iCreditInvoice.setYourContactPerson(iResultSet.getString("your_contact"));
        iCreditInvoice.setDelayInterest(iResultSet.getBigDecimal("delay_interest"));
        iCreditInvoice.setTaxFree(iResultSet.getBoolean("tax_free"));
        iCreditInvoice.setText(iResultSet.getString("sale_text"));
        iCreditInvoice.setEuSaleCommodity(iResultSet.getBoolean("eu_sale_commodity"));
        iCreditInvoice.setEuSaleYhirdPartCommodity(iResultSet.getBoolean("eu_sale_third_part"));
        iCreditInvoice.setPrinted(iResultSet.getBoolean("printed"));

        String iCurrencyCode = iResultSet.getString("currency_code");
        if (iCurrencyCode != null) {
            iCreditInvoice.setCurrency(new SSCurrency(iCurrencyCode, iCurrencyCode));
        }

        String iPaymentTerm = iResultSet.getString("payment_term");
        if (iPaymentTerm != null) {
            iCreditInvoice.setPaymentTerm(new SSPaymentTerm(iPaymentTerm, iPaymentTerm));
        }

        String iDeliveryTerm = iResultSet.getString("delivery_term");
        if (iDeliveryTerm != null) {
            iCreditInvoice.setDeliveryTerm(new SSDeliveryTerm(iDeliveryTerm, iDeliveryTerm));
        }

        String iDeliveryWay = iResultSet.getString("delivery_way");
        if (iDeliveryWay != null) {
            iCreditInvoice.setDeliveryWay(new SSDeliveryWay(iDeliveryWay, iDeliveryWay));
        }

        String iInvoiceType = iResultSet.getString("invoice_type");
        if (iInvoiceType != null) {
            try {
                iCreditInvoice.setType(SSInvoiceType.valueOf(iInvoiceType));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown enum values from partial migrations.
            }
        }

        iCreditInvoice.setCurrencyRate(iResultSet.getBigDecimal("currency_rate"));

        java.sql.Date iPaymentDay = iResultSet.getDate("payment_day");
        if (iPaymentDay != null) {
            iCreditInvoice.setLocalDueDate(iPaymentDay.toLocalDate());
        }

        iCreditInvoice.setYourOrderNumber(iResultSet.getString("your_order_number"));
        iCreditInvoice.setOCRNumber(iResultSet.getString("ocr_number"));
        iCreditInvoice.setEntered(iResultSet.getBoolean("entered"));
        iCreditInvoice.setNumRemainders(iResultSet.getInt("num_reminders"));
        iCreditInvoice.setInterestInvoiced(iResultSet.getBoolean("interest_invoiced"));
        iCreditInvoice.setStockInfluencing(iResultSet.getBoolean("stock_influencing"));
        iCreditInvoice.setOrderNumbers(iResultSet.getString("order_numbers"));

        Integer iVoucherId = (Integer) iResultSet.getObject("voucher_id");
        Integer iVoucherNumber = getVoucherNumberForIdV2(iVoucherId);
        if (iVoucherNumber != null) {
            iCreditInvoice.setVoucher(new SSVoucher(iVoucherNumber));
        }

        iCreditInvoice.setInvoiceAddress(mapAddressV2(iResultSet, "inv_addr"));
        iCreditInvoice.setDeliveryAddress(mapAddressV2(iResultSet, "del_addr"));

        iCreditInvoice.getRows().clear();
        iCreditInvoice.getRows().addAll(getCreditInvoiceRowsV2(iResultSet.getInt("id")));
        return iCreditInvoice;
    }

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSCreditInvoice> getCreditInvoices() {
        if (iCreditInvoices != null) {
            return iCreditInvoices;
        }
        iCreditInvoices = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iCreditInvoices;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_creditinvoice WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iCreditInvoices.add(mapCreditInvoiceV2(iResultSet));
                    } else {
                        iCreditInvoices.add((SSCreditInvoice) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iCreditInvoices;
    }

    public Optional<SSCreditInvoice> getCreditInvoice(SSCreditInvoice pCreditInvoice) {
        if (pCreditInvoice == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_creditinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, pCreditInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSCreditInvoice iCreditInvoice = useSchemaV2()
                        ? mapCreditInvoiceV2(iResultSet)
                        : (SSCreditInvoice) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iCreditInvoice);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSCreditInvoice> getCreditInvoices(List<SSCreditInvoice> pCreditInvoices) {
        if (pCreditInvoices == null) {
            return Collections.emptyList();
        }
        List<SSCreditInvoice> iCreditInvoices = new LinkedList<>();

        if (this.iCreditInvoices != null) {
            for (SSCreditInvoice iCreditInvoice : pCreditInvoices) {
                if (this.iCreditInvoices.contains(iCreditInvoice)) {
                    iCreditInvoices.add(iCreditInvoice);
                }
            }
            return iCreditInvoices;
        }
        if (iCurrentCompany == null) {
            return iCreditInvoices;
        }
        try {
            for (SSCreditInvoice iCreditInvoice : pCreditInvoices) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_creditinvoice WHERE number=? AND companyid=?");

                iStatement.setObject(1, iCreditInvoice.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    if (useSchemaV2()) {
                        iCreditInvoices.add(mapCreditInvoiceV2(iResultSet));
                    } else {
                        iCreditInvoices.add((SSCreditInvoice) iResultSet.getObject(3));
                    }
                }
                iStatement.close();
            }

            return iCreditInvoices;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addCreditInvoice(SSCreditInvoice iCreditInvoice) {
        if (iCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_creditinvoice WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "creditinvoice");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iCreditInvoice.setNumber(iNumber + 1);
                } else {
                    iCreditInvoice.setNumber(iCompanyNumber + 1);
                }
            } else {
                iCreditInvoice.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            Integer iCreditInvoiceId = null;
            if (useSchemaV2()) {
                String iPlaceholders = String.join(",", Collections.nCopies(41, "?"));
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_creditinvoice(" +
                                "number,companyid,crediting_nr,vdate,customer_nr,customer_name,our_contact,your_contact," +
                                "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                                "sale_text,eu_sale_commodity,eu_sale_third_part,printed,invoice_type,currency_rate," +
                                "payment_day,your_order_number,ocr_number,entered,num_reminders,interest_invoiced," +
                                "stock_influencing,order_numbers,voucher_id,inv_addr_name,inv_addr_address," +
                                "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                                "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(" + iPlaceholders + ")",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                iStatement.setObject(i++, iCreditInvoice.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                bindCreditInvoiceColumnsV2(iStatement, i, iCreditInvoice);
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_creditinvoice VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iCreditInvoice.getNumber());
                iStatement.setObject(2, iCreditInvoice);
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iCreditInvoiceId = iKeys.getInt(1);
                    }
                }
                if (iCreditInvoiceId == null) {
                    iCreditInvoiceId = getCreditInvoiceIdV2(iCreditInvoice.getNumber(),
                            iCurrentCompany.getId());
                }
                if (iCreditInvoiceId != null) {
                    replaceCreditInvoiceRowsV2(iCreditInvoiceId, iCreditInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateCreditInvoice(SSCreditInvoice iCreditInvoice) {
        if (iCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iCreditInvoiceId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_creditinvoice SET " +
                                "crediting_nr=?,vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                                "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                                "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                                "invoice_type=?,currency_rate=?,payment_day=?,your_order_number=?,ocr_number=?," +
                                "entered=?,num_reminders=?,interest_invoiced=?,stock_influencing=?,order_numbers=?," +
                                "voucher_id=?,inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                                "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                                "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?");
                int i = bindCreditInvoiceColumnsV2(iStatement, 1, iCreditInvoice);
                iStatement.setObject(i++, iCreditInvoice.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_creditinvoice SET creditinvoice=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iCreditInvoice);
                iStatement.setObject(2, iCreditInvoice.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iCreditInvoiceId = getCreditInvoiceIdV2(iCreditInvoice.getNumber(),
                        iCurrentCompany.getId());
                if (iCreditInvoiceId != null) {
                    replaceCreditInvoiceRowsV2(iCreditInvoiceId, iCreditInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteCreditInvoice(SSCreditInvoice iCreditInvoice) {
        if (iCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iCreditInvoiceId = getCreditInvoiceIdV2(iCreditInvoice.getNumber(),
                        iCurrentCompany.getId());
                if (iCreditInvoiceId != null) {
                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_creditinvoice_row WHERE creditinvoice_id=?");
                    iDeleteRows.setObject(1, iCreditInvoiceId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_creditinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, iCreditInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the periodic invoices in the current company.
     *
     * @return  A List of periodic invoices or an empty list.
     */
    public List<SSPeriodicInvoice> getPeriodicInvoices() {
        if (iPeriodicInvoices != null) {
            return iPeriodicInvoices;
        }
        iPeriodicInvoices = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iPeriodicInvoices;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_periodicinvoice WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    if (useSchemaV2()) {
                        iPeriodicInvoices.add(mapPeriodicInvoiceV2(iResultSet));
                    } else {
                        iPeriodicInvoices.add((SSPeriodicInvoice) iResultSet.getObject(3));
                    }
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iPeriodicInvoices;
    }

    public Optional<SSPeriodicInvoice> getPeriodicInvoice(SSPeriodicInvoice pPeriodicInvoice) {
        if (pPeriodicInvoice == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_periodicinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, pPeriodicInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSPeriodicInvoice iPeriodicInvoice = useSchemaV2()
                        ? mapPeriodicInvoiceV2(iResultSet)
                        : (SSPeriodicInvoice) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iPeriodicInvoice);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addPeriodicInvoice(SSPeriodicInvoice iPeriodicInvoice) {
        if (iPeriodicInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_periodicinvoice WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "periodicinvoice");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iPeriodicInvoice.setNumber(iNumber + 1);
                } else {
                    iPeriodicInvoice.setNumber(iCompanyNumber + 1);
                }
            } else {
                iPeriodicInvoice.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            Integer iPeriodicInvoiceId = null;
            if (useSchemaV2()) {
                String iPlaceholders = String.join(",", Collections.nCopies(39, "?"));
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_periodicinvoice(" +
                                "number,companyid,vdate,count,period,description,period_start,period_end," +
                                "append_period,append_information,information,customer_nr,customer_name," +
                                "our_contact,your_contact,delay_interest,currency_code,payment_term," +
                                "delivery_term,delivery_way,tax_free,sale_text,printed,currency_rate," +
                                "payment_day,your_order_number,stock_influencing,inv_addr_name,inv_addr_address," +
                                "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                                "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                                "VALUES(" + iPlaceholders + ")",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                iStatement.setObject(i++, iPeriodicInvoice.getNumber());
                iStatement.setObject(i++, iCurrentCompany.getId());
                bindPeriodicInvoiceColumnsV2(iStatement, i, iPeriodicInvoice);
            } else {
                iStatement = iConnection.prepareStatement(
                        "INSERT INTO tbl_periodicinvoice VALUES(NULL,?,?,?)");
                iStatement.setObject(1, iPeriodicInvoice.getNumber());
                iStatement.setObject(2, iPeriodicInvoice);
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        iPeriodicInvoiceId = iKeys.getInt(1);
                    }
                }
                if (iPeriodicInvoiceId == null) {
                    iPeriodicInvoiceId = getPeriodicInvoiceIdV2(iPeriodicInvoice.getNumber(),
                            iCurrentCompany.getId());
                }
                if (iPeriodicInvoiceId != null) {
                    replacePeriodicInvoiceRowsV2(iPeriodicInvoiceId, iPeriodicInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updatePeriodicInvoice(SSPeriodicInvoice iPeriodicInvoice) {
        if (iPeriodicInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement;
            Integer iPeriodicInvoiceId = null;

            if (useSchemaV2()) {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_periodicinvoice SET " +
                                "vdate=?,count=?,period=?,description=?,period_start=?,period_end=?," +
                                "append_period=?,append_information=?,information=?,customer_nr=?,customer_name=?," +
                                "our_contact=?,your_contact=?,delay_interest=?,currency_code=?,payment_term=?," +
                                "delivery_term=?,delivery_way=?,tax_free=?,sale_text=?,printed=?,currency_rate=?," +
                                "payment_day=?,your_order_number=?,stock_influencing=?,inv_addr_name=?," +
                                "inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?," +
                                "inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                                "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?");
                int i = bindPeriodicInvoiceColumnsV2(iStatement, 1, iPeriodicInvoice);
                iStatement.setObject(i++, iPeriodicInvoice.getNumber());
                iStatement.setObject(i, iCurrentCompany.getId());
            } else {
                iStatement = iConnection.prepareStatement(
                        "UPDATE tbl_periodicinvoice SET periodicinvoice=? WHERE number=? AND companyid=?");

                iStatement.setObject(1, iPeriodicInvoice);
                iStatement.setObject(2, iPeriodicInvoice.getNumber());
                iStatement.setObject(3, iCurrentCompany.getId());
            }
            iStatement.executeUpdate();

            if (useSchemaV2()) {
                iPeriodicInvoiceId = getPeriodicInvoiceIdV2(iPeriodicInvoice.getNumber(),
                        iCurrentCompany.getId());
                if (iPeriodicInvoiceId != null) {
                    replacePeriodicInvoiceRowsV2(iPeriodicInvoiceId, iPeriodicInvoice);
                }
            }

            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deletePeriodicInvoice(SSPeriodicInvoice iPeriodicInvoice) {
        if (iPeriodicInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            if (useSchemaV2()) {
                Integer iPeriodicInvoiceId = getPeriodicInvoiceIdV2(iPeriodicInvoice.getNumber(),
                        iCurrentCompany.getId());
                if (iPeriodicInvoiceId != null) {
                    PreparedStatement iDeleteAdded = iConnection.prepareStatement(
                            "DELETE FROM tbl_periodicinvoice_added WHERE periodicinvoice_id=?");
                    iDeleteAdded.setObject(1, iPeriodicInvoiceId);
                    iDeleteAdded.executeUpdate();
                    iDeleteAdded.close();

                    PreparedStatement iClearInvoices = iConnection.prepareStatement(
                            "UPDATE tbl_invoice SET periodicinvoice_id=NULL WHERE periodicinvoice_id=?");
                    iClearInvoices.setObject(1, iPeriodicInvoiceId);
                    iClearInvoices.executeUpdate();
                    iClearInvoices.close();

                    PreparedStatement iDeleteRows = iConnection.prepareStatement(
                            "DELETE FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=?");
                    iDeleteRows.setObject(1, iPeriodicInvoiceId);
                    iDeleteRows.executeUpdate();
                    iDeleteRows.close();
                }
            }

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_periodicinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, iPeriodicInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the purchase orders in the current company.
     *
     * @return  A List of orders or an empty list.
     */
    public List<SSPurchaseOrder> getPurchaseOrders() {
        if (iPurchaseOrders != null) {
            return iPurchaseOrders;
        }
        iPurchaseOrders = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iPurchaseOrders;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_purchaseorder WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iPurchaseOrders.add((SSPurchaseOrder) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iPurchaseOrders;
    }

    public Optional<SSPurchaseOrder> getPurchaseOrder(SSPurchaseOrder pPurchaseOrder) {
        if (pPurchaseOrder == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_purchaseorder WHERE number=? AND companyid=?");

            iStatement.setObject(1, pPurchaseOrder.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSPurchaseOrder iPurchaseOrder = (SSPurchaseOrder) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iPurchaseOrder);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSPurchaseOrder> getPurchaseOrders(List<SSPurchaseOrder> pPurchaseOrders) {
        if (pPurchaseOrders == null) {
            return Collections.emptyList();
        }
        List<SSPurchaseOrder> iPurchaseOrders = new LinkedList<>();

        if (this.iPurchaseOrders != null) {
            for (SSPurchaseOrder iPurchaseOrder : pPurchaseOrders) {
                if (this.iPurchaseOrders.contains(iPurchaseOrder)) {
                    iPurchaseOrders.add(iPurchaseOrder);
                }
            }
            return iPurchaseOrders;
        }
        if (iCurrentCompany == null) {
            return iPurchaseOrders;
        }
        try {
            for (SSPurchaseOrder iPurchaseOrder : pPurchaseOrders) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_purchaseorder WHERE number=? AND companyid=?");

                iStatement.setObject(1, iPurchaseOrder.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iPurchaseOrders.add((SSPurchaseOrder) iResultSet.getObject(3));
                }
                iStatement.close();
            }

            return iPurchaseOrders;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addPurchaseOrder(SSPurchaseOrder iPurchaseOrder) {
        if (iPurchaseOrder == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_purchaseorder WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "purchaseorder");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iPurchaseOrder.setNumber(iNumber + 1);
                } else {
                    iPurchaseOrder.setNumber(iCompanyNumber + 1);
                }
            } else {
                iPurchaseOrder.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_purchaseorder VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iPurchaseOrder.getNumber());
            iStatement.setObject(2, iPurchaseOrder);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updatePurchaseOrder(SSPurchaseOrder iPurchaseOrder) {
        if (iPurchaseOrder == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_purchaseorder SET purchaseorder=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iPurchaseOrder);
            iStatement.setObject(2, iPurchaseOrder.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deletePurchaseOrder(SSPurchaseOrder iPurchaseOrder) {
        if (iPurchaseOrder == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_purchaseorder WHERE number=? AND companyid=?");

            iStatement.setObject(1, iPurchaseOrder.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the supplier invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierInvoice> getSupplierInvoices() {
        if (iSupplierInvoices != null) {
            return iSupplierInvoices;
        }
        iSupplierInvoices = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iSupplierInvoices;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_supplierinvoice WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iSupplierInvoices.add((SSSupplierInvoice) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iSupplierInvoices;
    }

    public Optional<SSSupplierInvoice> getSupplierInvoice(SSSupplierInvoice pSupplierInvoice) {
        if (pSupplierInvoice == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_supplierinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, pSupplierInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSSupplierInvoice iSupplierInvoice = (SSSupplierInvoice) iResultSet.getObject(
                        3);

                iStatement.close();
                return Optional.of(iSupplierInvoice);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSSupplierInvoice> getSupplierInvoices(List<SSSupplierInvoice> pSupplierInvoices) {
        if (pSupplierInvoices == null) {
            return Collections.emptyList();
        }
        List<SSSupplierInvoice> iSupplierInvoices = new LinkedList<>();

        if (this.iSupplierInvoices != null) {
            for (SSSupplierInvoice iSupplierInvoice : pSupplierInvoices) {
                if (this.iSupplierInvoices.contains(iSupplierInvoice)) {
                    iSupplierInvoices.add(iSupplierInvoice);
                }
            }
            return iSupplierInvoices;
        }
        if (iCurrentCompany == null) {
            return iSupplierInvoices;
        }
        try {
            for (SSSupplierInvoice iSupplierInvoice : pSupplierInvoices) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_supplierinvoice WHERE number=? AND companyid=?");

                iStatement.setObject(1, iSupplierInvoice.getNumber());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iSupplierInvoices.add((SSSupplierInvoice) iResultSet.getObject(3));
                }
                iStatement.close();
            }

            return iSupplierInvoices;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addSupplierInvoice(SSSupplierInvoice iSupplierInvoice) {
        if (iSupplierInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_supplierinvoice WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "supplierinvoice");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iSupplierInvoice.setNumber(iNumber + 1);
                } else {
                    iSupplierInvoice.setNumber(iCompanyNumber + 1);
                }
            } else {
                iSupplierInvoice.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_supplierinvoice VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iSupplierInvoice.getNumber());
            iStatement.setObject(2, iSupplierInvoice);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateSupplierInvoice(SSSupplierInvoice iSupplierInvoice) {
        if (iSupplierInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_supplierinvoice SET supplierinvoice=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iSupplierInvoice);
            iStatement.setObject(2, iSupplierInvoice.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteSupplierInvoice(SSSupplierInvoice iSupplierInvoice) {
        if (iSupplierInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_supplierinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, iSupplierInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierCreditInvoice> getSupplierCreditInvoices() {
        if (iSupplierCreditInvoices != null) {
            return iSupplierCreditInvoices;
        }
        iSupplierCreditInvoices = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iSupplierCreditInvoices;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_suppliercreditinvoice WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iSupplierCreditInvoices.add(
                            (SSSupplierCreditInvoice) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iSupplierCreditInvoices;
    }

    public Optional<SSSupplierCreditInvoice> getSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice) {
        if (pSupplierCreditInvoice == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_suppliercreditinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, pSupplierCreditInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSSupplierCreditInvoice iSupplierCreditInvoice = (SSSupplierCreditInvoice) iResultSet.getObject(
                        3);

                iStatement.close();
                return Optional.of(iSupplierCreditInvoice);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addSupplierCreditInvoice(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        if (iSupplierCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_suppliercreditinvoice WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "suppliercreditinvoice");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iSupplierCreditInvoice.setNumber(iNumber + 1);
                } else {
                    iSupplierCreditInvoice.setNumber(iCompanyNumber + 1);
                }
            } else {
                iSupplierCreditInvoice.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_suppliercreditinvoice VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iSupplierCreditInvoice.getNumber());
            iStatement.setObject(2, iSupplierCreditInvoice);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateSupplierCreditInvoice(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        if (iSupplierCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_suppliercreditinvoice SET suppliercreditinvoice=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iSupplierCreditInvoice);
            iStatement.setObject(2, iSupplierCreditInvoice.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteSupplierCreditInvoice(SSSupplierCreditInvoice iSupplierCreditInvoice) {
        if (iSupplierCreditInvoice == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_suppliercreditinvoice WHERE number=? AND companyid=?");

            iStatement.setObject(1, iSupplierCreditInvoice.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSInventory> getInventories() {
        if (iInventories != null) {
            return iInventories;
        }
        iInventories = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iInventories;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_inventory WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iInventories.add((SSInventory) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iInventories;
    }

    public Optional<SSInventory> getInventory(SSInventory pInventory) {
        if (pInventory == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_inventory WHERE number=? AND companyid=?");

            iStatement.setObject(1, pInventory.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSInventory iInventory = (SSInventory) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iInventory);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addInventory(SSInventory iInventory) {
        if (iInventory == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_inventory WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "inventory");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iInventory.setNumber(iNumber + 1);
                } else {
                    iInventory.setNumber(iCompanyNumber + 1);
                }
            } else {
                iInventory.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_inventory VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iInventory.getNumber());
            iStatement.setObject(2, iInventory);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateInventory(SSInventory iInventory) {
        if (iInventory == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_inventory SET inventory=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iInventory);
            iStatement.setObject(2, iInventory.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteInventory(SSInventory iInventory) {
        if (iInventory == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_inventory WHERE number=? AND companyid=?");

            iStatement.setObject(1, iInventory.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSIndelivery> getIndeliveries() {
        if (iIndeliveries != null) {
            return iIndeliveries;
        }
        iIndeliveries = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iIndeliveries;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_indelivery WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iIndeliveries.add((SSIndelivery) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iIndeliveries;
    }

    public Optional<SSIndelivery> getIndelivery(SSIndelivery pIndelivery) {
        if (pIndelivery == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_indelivery WHERE number=? AND companyid=?");

            iStatement.setObject(1, pIndelivery.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSIndelivery iIndelivery = (SSIndelivery) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iIndelivery);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addIndelivery(SSIndelivery iIndelivery) {
        if (iIndelivery == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_indelivery WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "indelivery");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iIndelivery.setNumber(iNumber + 1);
                } else {
                    iIndelivery.setNumber(iCompanyNumber + 1);
                }
            } else {
                iIndelivery.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_indelivery VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iIndelivery.getNumber());
            iStatement.setObject(2, iIndelivery);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateIndelivery(SSIndelivery iIndelivery) {
        if (iIndelivery == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_indelivery SET indelivery=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iIndelivery);
            iStatement.setObject(2, iIndelivery.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteIndelivery(SSIndelivery iIndelivery) {
        if (iIndelivery == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_indelivery WHERE number=? AND companyid=?");

            iStatement.setObject(1, iIndelivery.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSOutdelivery> getOutdeliveries() {
        if (iOutdeliveries != null) {
            return iOutdeliveries;
        }
        iOutdeliveries = new LinkedList<>();
        if (iCurrentCompany == null) {
            return iOutdeliveries;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_outdelivery WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);

                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iOutdeliveries.add((SSOutdelivery) iResultSet.getObject(3));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iOutdeliveries;
    }

    public Optional<SSOutdelivery> getOutdelivery(SSOutdelivery pOutdelivery) {
        if (pOutdelivery == null || iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_outdelivery WHERE number=? AND companyid=?");

            iStatement.setObject(1, pOutdelivery.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSOutdelivery iOutdelivery = (SSOutdelivery) iResultSet.getObject(3);

                iStatement.close();
                return Optional.of(iOutdelivery);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public void addOutdelivery(SSOutdelivery iOutdelivery) {
        if (iOutdelivery == null || iCurrentCompany == null) {
            return;
        }
        try {

            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_outdelivery WHERE companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            Integer iCompanyNumber = getCurrentCompany().getAutoIncrement().getNumber(
                    "outdelivery");

            if (iResultSet.next()) {
                Integer iNumber = iResultSet.getInt("maxnum");

                if (iNumber > iCompanyNumber) {
                    iOutdelivery.setNumber(iNumber + 1);
                } else {
                    iOutdelivery.setNumber(iCompanyNumber + 1);
                }
            } else {
                iOutdelivery.setNumber(iCompanyNumber + 1);
            }
            iResultSet.close();
            iStatement.close();

            iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_outdelivery VALUES(NULL,?,?,?)");
            iStatement.setObject(1, iOutdelivery.getNumber());
            iStatement.setObject(2, iOutdelivery);
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);

            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateOutdelivery(SSOutdelivery iOutdelivery) {
        if (iOutdelivery == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_outdelivery SET outdelivery=? WHERE number=? AND companyid=?");

            iStatement.setObject(1, iOutdelivery);
            iStatement.setObject(2, iOutdelivery.getNumber());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteOutdelivery(SSOutdelivery iOutdelivery) {
        if (iOutdelivery == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_outdelivery WHERE number=? AND companyid=?");

            iStatement.setObject(1, iOutdelivery.getNumber());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    public List<SSOwnReport> getOwnReports() {
        if (iOwnReports != null) {
            return iOwnReports;
        }
        iOwnReports = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iOwnReports;
        }
        try {
            Integer iMax = -1;
            ResultSet iResultSet;
            PreparedStatement iStatement;

            while (true) {
                iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_ownreport WHERE companyid=? AND id>?");
                iStatement.setObject(1, iCurrentCompany.getId());
                iStatement.setObject(2, iMax);
                iStatement.setMaxRows(1024);
                iResultSet = iStatement.executeQuery();
                int i = 0;

                while (iResultSet.next()) {
                    iMax = iResultSet.getInt(1);
                    iOwnReports.add((SSOwnReport) iResultSet.getObject(2));
                    i++;
                }
                if (i != 1024) {
                    break;
                }
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return iOwnReports;
    }

    public Optional<SSOwnReport> getOwnReport(SSOwnReport pOwnReport) {
        if (pOwnReport == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_ownreport WHERE id=? AND companyid=?");

            iStatement.setObject(1, pOwnReport.getId());
            iStatement.setObject(2, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSOwnReport iOwnReport = (SSOwnReport) iResultSet.getObject(2);

                iStatement.close();
                return Optional.of(iOwnReport);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<SSOwnReport> getOwnReport(Integer iOwnReportNumber) {
        if (iOwnReportNumber == null) {
            return Optional.empty();
        }
        if (iCurrentCompany == null) {
            return Optional.empty();
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "SELECT * FROM tbl_ownreport WHERE id=" + iOwnReportNumber
                    + " AND companyid=?");

            iStatement.setObject(1, iCurrentCompany.getId());
            ResultSet iResultSet = iStatement.executeQuery();

            if (iResultSet.next()) {
                SSOwnReport iOwnReport = (SSOwnReport) iResultSet.getObject(2);

                iStatement.close();
                return Optional.of(iOwnReport);
            }
            iResultSet.close();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Optional.empty();
    }

    public List<SSOwnReport> getOwnReports(List<SSOwnReport> pOwnReports) {
        if (pOwnReports == null) {
            return Collections.emptyList();
        }
        List<SSOwnReport> iOwnReports = new LinkedList<>();

        if (this.iOwnReports != null) {
            for (SSOwnReport iOwnReport : pOwnReports) {
                if (this.iOwnReports.contains(iOwnReport)) {
                    iOwnReports.add(iOwnReport);
                }
            }
            return iOwnReports;
        }
        if (iCurrentCompany == null) {
            return iOwnReports;
        }
        try {
            for (SSOwnReport iOwnReport : pOwnReports) {
                PreparedStatement iStatement = iConnection.prepareStatement(
                        "SELECT * FROM tbl_ownreport WHERE id=? AND companyid=?");

                iStatement.setObject(1, iOwnReport.getId());
                iStatement.setObject(2, iCurrentCompany.getId());
                ResultSet iResultSet = iStatement.executeQuery();

                if (iResultSet.next()) {
                    iOwnReports.add((SSOwnReport) iResultSet.getObject(2));
                }
                iStatement.close();
            }

            return iOwnReports;
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    public void addOwnReport(SSOwnReport iOwnReport) {
        if (iOwnReport == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "INSERT INTO tbl_ownreport VALUES(NULL,?,?)");

            iStatement.setObject(1, iOwnReport);
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

            iStatement = iConnection.prepareStatement("SELECT * FROM tbl_ownreport");
            ResultSet iResultSet = iStatement.executeQuery();
            Integer iId = -1;

            while (iResultSet.next()) {
                if (iResultSet.isLast()) {
                    iId = iResultSet.getInt("id");
                }
            }
            iResultSet.close();
            iStatement.close();
            iOwnReport.setId(iId);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOG.error("Unexpected error", e);
            }
            iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_ownreport SET ownreport=? WHERE id=?");
            iStatement.setObject(1, iOwnReport);
            iStatement.setObject(2, iOwnReport.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void updateOwnReport(SSOwnReport iOwnReport) {
        if (iOwnReport == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "UPDATE tbl_ownreport SET ownreport=? WHERE id=? AND companyid=?");

            iStatement.setObject(1, iOwnReport);
            iStatement.setObject(2, iOwnReport.getId());
            iStatement.setObject(3, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    public void deleteOwnReport(SSOwnReport iOwnReport) {
        if (iOwnReport == null || iCurrentCompany == null) {
            return;
        }
        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "DELETE FROM tbl_ownreport WHERE id=? AND companyid=?");

            iStatement.setObject(1, iOwnReport.getId());
            iStatement.setObject(2, iCurrentCompany.getId());
            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
    }

    // /////////////////////////////////////////////////////////////////////////////

    public void createLocalTriggers() {

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
                    "CREATE TRIGGER NEWPROJECT  AFTER INSERT ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITPROJECT  AFTER UPDATE ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETEPROJECT  AFTER DELETE ON tbl_project FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER NEWRESULTUNIT  AFTER INSERT ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITRESULTUNIT  AFTER UPDATE ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETERESULTUNIT  AFTER DELETE ON tbl_resultunit FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER NEWPRODUCT  AFTER INSERT ON tbl_product FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITPRODUCT  AFTER UPDATE ON tbl_product FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETEPRODUCT  AFTER DELETE ON tbl_product FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER NEWCUSTOMER  AFTER INSERT ON tbl_customer FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITCUSTOMER  AFTER UPDATE ON tbl_customer FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETECUSTOMER  AFTER DELETE ON tbl_customer FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER NEWSUPPLIER  AFTER INSERT ON tbl_supplier FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITSUPPLIER  AFTER UPDATE ON tbl_supplier FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETESUPPLIER  AFTER DELETE ON tbl_supplier FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
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
                            + "CREATE TRIGGER NEWVOUCHER  AFTER INSERT ON tbl_voucher FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITVOUCHER  AFTER UPDATE ON tbl_voucher FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETEVOUCHER  AFTER DELETE ON tbl_voucher FOR EACH ROW QUEUE 10000 CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER NEWOWNREPORT  AFTER INSERT ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER EDITOWNREPORT  AFTER UPDATE ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";"
                            + "CREATE TRIGGER DELETEOWNREPORT  AFTER DELETE ON tbl_ownreport FOR EACH ROW CALL \"se.swedsoft.bookkeeping.SSTriggerHandler\";");

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {// LOG.info("Triggers fanns redan vi lokal tilläggning");
            // LOG.error("Unexpected error", e);
        }
    }

    public void createTriggers() {
        createLocalTriggers();
    }

    public void dropTriggers() {

        try {
            PreparedStatement iStatement = iConnection.prepareStatement(
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
                    + "DROP TRIGGER DELETEOWNREPORT;");

            iStatement.executeUpdate();
            iConnection.commit();
            iStatement.close();

        } catch (SQLException e) {// LOG.info("Triggers fanns inte vid borttagning");
        }
    }

    public File getFile(UID pIdentifier) {
        String iFileName = pIdentifier.toString();

        iFileName = iFileName.replace(":", ".");
        iFileName = iFileName.replace("-", ".");

        return new File(Path.get(Path.USER_DATA), "db/" + iFileName + ".data");
    }

    private String getSchemaResource() {
        String schemaVersion = System.getProperty(SCHEMA_VERSION_PROPERTY, "v1").trim();
        if (SCHEMA_V2.equalsIgnoreCase(schemaVersion)) {
            return "sql/create_tables_v2.sql";
        }
        return "sql/create_tables.sql";
    }

    private boolean useSchemaV2() {
        return "sql/create_tables_v2.sql".equals(getSchemaResource());
    }

    public void createNewTables() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return;
            }

            String schemaResource = getSchemaResource();
            String q = SSUtil.readResourceToString(schemaResource);
            LOG.info("createNewTables using schema resource: {}", schemaResource);

            // Strip SQL line comments before splitting on ';' so semicolons inside
            // comments do not produce invalid fragments.
            StringBuilder scriptBuilder = new StringBuilder();
            for (String line : q.split("\\r?\\n")) {
                if (!line.trim().startsWith("--")) {
                    scriptBuilder.append(line).append('\n');
                }
            }
            String script = scriptBuilder.toString();

            // Split SQL script into individual statements (separated by ';') and
            // execute each separately, since PreparedStatement.executeUpdate() can
            // only handle one statement at a time.
            String[] statements = script.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    PreparedStatement ps = iConnection.prepareStatement(trimmed);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    // "CREATE CACHED TABLE" is not supported in in-memory (test) databases.
                    // Fall back to "CREATE TABLE IF NOT EXISTS" for compatibility.
                    if (trimmed.toUpperCase().contains("CREATE CACHED TABLE")) {
                        String fallback = trimmed.replaceFirst("(?i)CREATE CACHED TABLE",
                                "CREATE TABLE IF NOT EXISTS");
                        try (PreparedStatement ps2 = iConnection.prepareStatement(fallback)) {
                            ps2.executeUpdate();
                        } catch (SQLException ignored) {
                            LOG.warn("createNewTables fallback failed: {}", ignored.getMessage());
                        }
                    } else {
                        LOG.warn("createNewTables skipping statement from {}: {}", schemaResource,
                                e.getMessage());
                    }
                }
            }
            iConnection.commit();

            dropTriggers();
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
        sb.append(", iListenerMap=").append(iListenerMap);
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
