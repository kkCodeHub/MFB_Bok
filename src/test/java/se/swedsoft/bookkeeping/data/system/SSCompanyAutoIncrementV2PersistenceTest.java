package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that company auto-increment counters are persisted and loaded in schema V2.
 */
@Tag("integration")
class SSCompanyAutoIncrementV2PersistenceTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_company_autoincrement";

    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSDB.getInstance().startupLocal(connection);
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

        SSDB.getInstance().addCompany(company);

        Optional<SSNewCompany> reloadedOpt = SSDB.getInstance().getCompany(company);
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
        SSDB.getInstance().addCompany(company);

        company.getAutoIncrement().setNumber("invoice", 999);
        company.getAutoIncrement().setNumber("outpayment", 123);
        SSDB.getInstance().updateCompany(company);

        Optional<SSNewCompany> reloadedOpt = SSDB.getInstance().getCompany(company);
        assertThat(reloadedOpt).isPresent();

        SSNewCompany reloaded = reloadedOpt.get();
        assertThat(reloaded.getAutoIncrement().getNumber("invoice")).isEqualTo(999);
        assertThat(reloaded.getAutoIncrement().getNumber("outpayment")).isEqualTo(123);
    }
}

