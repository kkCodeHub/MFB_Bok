package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that company auto-increment counters are persisted and loaded in schema V2.
 */
@Tag("integration")
class SSCompanyAutoIncrementV2IntegrationTest {

    @BeforeEach
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-c-o-m-p-a-n-y-a-u-t-o-i-n-c-r-e-m-e-n-t-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);
    }

    @AfterAll
    static void teardown() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @Test
    void addAndReloadCompanyPersistsAutoIncrementCounters() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Company Counter Test AB");
        company.getAutoIncrement().setNumber("invoice", 501);
        company.getAutoIncrement().setNumber("order", 701);
        company.getAutoIncrement().setNumber("tender", 321);
        company.getAutoIncrement().setNumber("supplierinvoicejournal", 42);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);

        Optional<SSNewCompany> reloadedOpt = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompany(company);
        assertThat(reloadedOpt).isPresent();

        SSNewCompany reloaded = reloadedOpt.get();
        assertThat(reloaded.getAutoIncrement().getNumber("invoice")).isEqualTo(501);
        assertThat(reloaded.getAutoIncrement().getNumber("order")).isEqualTo(701);
        assertThat(reloaded.getAutoIncrement().getNumber("tender")).isEqualTo(321);
        assertThat(reloaded.getAutoIncrement().getNumber("supplierinvoicejournal")).isEqualTo(42);
    }

    @Test
    void updateCompanyPersistsChangedAutoIncrementCounters() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Company Counter Update Test AB");
        company.getAutoIncrement().setNumber("invoice", 10);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);

        company.getAutoIncrement().setNumber("invoice", 999);
        company.getAutoIncrement().setNumber("outpayment", 123);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(company);

        Optional<SSNewCompany> reloadedOpt = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompany(company);
        assertThat(reloadedOpt).isPresent();

        SSNewCompany reloaded = reloadedOpt.get();
        assertThat(reloaded.getAutoIncrement().getNumber("invoice")).isEqualTo(999);
        assertThat(reloaded.getAutoIncrement().getNumber("outpayment")).isEqualTo(123);
    }
}
