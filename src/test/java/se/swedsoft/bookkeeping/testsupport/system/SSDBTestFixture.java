package se.swedsoft.bookkeeping.testsupport.system;

import org.junit.jupiter.api.Tag;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Shared in-memory HSQLDB fixture for integration tests.
 *
 * <p>Call {@link #setupOnce()} from a {@code @BeforeAll} method in each test
 * class to ensure the database is open.  Call {@link #resetCaches()} from a
 * {@code @BeforeEach} method to clear SSDB's in-memory list caches so that
 * every test reads fresh data from the DB.</p>
 *
 * <p>The fixture is intentionally kept as a plain utility class (not a JUnit
 * extension) so that test classes have full control over their lifecycle.</p>
 *
 * <p>All integration tests should carry {@code @Tag("integration")} so they
 * can be run independently from the fast unit-test suite.</p>
 */
@Tag("integration")
public final class SSDBTestFixture {

    /** JDBC URL for the shared in-memory HSQLDB instance. */
    static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test";
    private static final String CREATE_TABLES_V2_PUBLIC_SQL = "/sql/create_tables_v2_Public.sql";
    private static final String CREATE_TABLES_V2_COMPANY_SQL = "/sql/create_tables_v2_Company.sql";

    /**
     * Collects uncaught exceptions from background threads (e.g. HSQLDB
     * trigger threads).  Tests should call {@link #drainUncaughtExceptions()}
     * after exercising code that fires database triggers to ensure no
     * exceptions were silently swallowed on a background thread.
     */
    private static final CopyOnWriteArrayList<Throwable> uncaughtExceptions =
            new CopyOnWriteArrayList<>();

    private static boolean started = false;

    private SSDBTestFixture() {}

    /**
     * Opens the in-memory database (once per JVM), creates a test company and
     * accounting year, and sets them as the current company/year in SSDB.
     *
     * <p>Safe to call repeatedly — subsequent calls are no-ops once the DB is
     * already open.</p>
     *
     * @throws Exception if the database cannot be opened or populated
     */
    public static synchronized void setupOnce() throws Exception {
        if (started) {
            return;
        }

        // Install a default handler that captures uncaught exceptions from
        // background threads (such as HSQLDB trigger threads) so that tests
        // can assert that no errors occurred asynchronously.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                uncaughtExceptions.add(throwable));

        // SSDBConfig.load() runs in a static initializer when SSDBConfig is
        // first touched (which happens inside startupLocal).  It reads/writes
        // database.config in Path.APP_BASE (= current working directory).
        // That is the Maven project root during tests, which is writable.
        // No special override is needed.

        Class.forName("org.hsqldb.jdbcDriver");
        Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");

        prepareSchemaForStartup(conn);

        // startupLocal creates tables, seeds example company, imports account
        // plans (slow, one-time), and reads last-used company/year from config.
        SSSystemConfigContext.startupLocal(conn);

        // Ensure we have at least one company.  startupLocal already seeds an
        // example company via sql/example.sql, so getCompanies() is non-empty
        // on a fresh DB.  On subsequent JVM runs the example company is already
        // present so the seed is skipped.
        List<SSNewCompany> companies = SSCompanyYearContext.getCompanies();
        SSNewCompany company;
        if (companies == null || companies.isEmpty()) {
            company = buildTestCompany();
            SSCompanyYearContext.addCompany(company);
            // addCompany sets the id on the object — re-read the id from DB.
            company = SSCompanyYearContext.getCompanies().get(0);
        } else {
            company = companies.get(0);
        }
        SSCompanyYearContext.setCurrentCompany(company);

        // Ensure we have at least one accounting year for the company.
        List<SSNewAccountingYear> years = SSCompanyYearContext.getYears();
        SSNewAccountingYear year;
        if (years == null || years.isEmpty()) {
            year = buildTestYear();
            SSAccountingContext.addAccountingYear(year);
            year = SSCompanyYearContext.getYears().get(0);
        } else {
            year = years.get(0);
        }
        SSCompanyYearContext.setCurrentYear(year);

        // Warm up in-memory caches (no Swing dialog).
        SSAccountingContext.getVouchers();

        started = true;
    }

    /**
     * Clears all SSDB in-memory list caches by re-setting the current company
     * and accounting year, then eagerly reloads the voucher list.
     *
     * <p>A brief sleep before clearing is necessary because HSQLDB fires
     * {@code AFTER} triggers on a background thread.  The trigger handler calls
     * {@code getVoucher()} which can return {@code null} if the row has not yet
     * been committed, and that {@code null} gets appended to {@code iVouchers}.
     * Waiting for the background thread to finish before nulling {@code iVouchers}
     * avoids this race condition.</p>
     *
     * <p>Eagerly calling {@link SSDB#getVouchers()} after the reset ensures that
     * {@code iVouchers} is populated from the DB before the test body runs, so
     * any subsequent trigger callbacks find a non-null list and operate on real
     * voucher objects.</p>
     */
    public static void resetCaches() {
        try {
            // Let any in-flight HSQLDB background trigger threads complete.
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        SSNewCompany current = SSCompanyYearContext.getCurrentCompany();
        if (current != null) {
            SSCompanyYearContext.setCurrentCompany(current);
        }
        SSNewAccountingYear currentYear = SSCompanyYearContext.getCurrentYear();
        if (currentYear != null) {
            SSCompanyYearContext.setCurrentYear(currentYear);
        }
        // Eagerly populate iVouchers from the DB so background triggers that
        // fire afterwards operate on a non-null list and cannot add nulls.
        SSAccountingContext.getVouchers();
    }

    /**
     * Drains all uncaught exceptions captured since the last call and throws
     * an {@link AssertionError} if any were recorded.
     *
     * <p>Call this at the end of each test (e.g. in an {@code @AfterEach}
     * method) to ensure that background-thread errors cause the test to
     * fail.</p>
     */
    public static void drainUncaughtExceptions() {
        // Give background trigger threads a moment to finish.
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<Throwable> captured = List.copyOf(uncaughtExceptions);
        uncaughtExceptions.clear();

        if (!captured.isEmpty()) {
            AssertionError error = new AssertionError(
                    "Background thread(s) threw " + captured.size()
                    + " uncaught exception(s); first: " + captured.get(0));
            for (Throwable t : captured) {
                error.addSuppressed(t);
            }
            throw error;
        }
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private static SSNewCompany buildTestCompany() {
        SSNewCompany c = new SSNewCompany();
        c.setName("Test Company AB");
        c.setCorporateID("556000-0001");
        return c;
    }

    private static SSNewAccountingYear buildTestYear() {
        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2024, 1, 1));
        year.setLocalTo(LocalDate.of(2024, 12, 31));
        return year;
    }

    private static void prepareSchemaForStartup(Connection pConnection) throws SQLException, IOException {
        executeSqlScript(pConnection, CREATE_TABLES_V2_PUBLIC_SQL);
        executeSqlScript(pConnection, CREATE_TABLES_V2_COMPANY_SQL);

        try (Statement iStatement = pConnection.createStatement();
             java.sql.ResultSet iResultSet = iStatement.executeQuery("SELECT 1 FROM tbl_accountplan")) {
            if (iResultSet.next()) {
                return;
            }
        }

        try (PreparedStatement iStatement = pConnection.prepareStatement(
                "INSERT INTO tbl_accountplan(name, base_name, assessment_year, plan_type) VALUES (?, ?, ?, ?)")) {
            iStatement.setString(1, "Testkontoplan");
            iStatement.setString(2, "Testkontoplan");
            iStatement.setString(3, "2024");
            iStatement.setString(4, "TEST");
            iStatement.executeUpdate();
        }
        pConnection.commit();
    }

    private static void executeSqlScript(Connection pConnection, String pResourcePath)
            throws SQLException, IOException {
        try (InputStream iInputStream = SSDBTestFixture.class.getResourceAsStream(pResourcePath)) {
            if (iInputStream == null) {
                throw new IOException("Resource not found: " + pResourcePath);
            }

            StringBuilder iSql = new StringBuilder();
            try (BufferedReader iReader = new BufferedReader(
                    new InputStreamReader(iInputStream, StandardCharsets.UTF_8))) {
                String iLine;
                while ((iLine = iReader.readLine()) != null) {
                    String iTrimmed = iLine.trim();
                    if (!iTrimmed.startsWith("--")) {
                        iSql.append(iLine).append('\n');
                    }
                }
            }

            for (String iStatementText : iSql.toString().split(";")) {
                String iTrimmed = iStatementText.trim();
                if (iTrimmed.isEmpty()) {
                    continue;
                }

                try (Statement iStatement = pConnection.createStatement()) {
                    iStatement.executeUpdate(iTrimmed);
                }
            }
            pConnection.commit();
        }
    }
}
