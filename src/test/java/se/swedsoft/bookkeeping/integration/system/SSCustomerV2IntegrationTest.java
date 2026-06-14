package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration slice for customer CRUD against schema V2.
 */
@Tag("integration")
class SSCustomerV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-c-u-s-t-o-m-e-r-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchCustomerInSchemaV2() {
        SSCustomer c = new SSCustomer();
        c.setNumber("C-V2-001");
        c.setName("V2 Integration AB");
        c.setEMail("v2@integration.se");
        c.setPhone1("08-100200");

        SSMasterdataContext.addCustomer(c);

        Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("V2 Integration AB");
        assertThat(fetched.get().getEMail()).isEqualTo("v2@integration.se");
        assertThat(fetched.get().getPhone1()).isEqualTo("08-100200");

        SSMasterdataContext.deleteCustomer(c);
    }

    @Test
    void updateAndDeleteCustomerInSchemaV2() {
        SSCustomer c = new SSCustomer();
        c.setNumber("C-V2-002");
        c.setName("Before V2 Update");

        SSMasterdataContext.addCustomer(c);

        Optional<SSCustomer> fetched = SSMasterdataContext.getCustomer("C-V2-002");
        assertThat(fetched).isPresent();

        SSCustomer updatedCustomer = fetched.get();
        updatedCustomer.setName("After V2 Update");
        SSMasterdataContext.updateCustomer(updatedCustomer);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSCustomer> updated = SSMasterdataContext.getCustomer("C-V2-002");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("After V2 Update");

        SSMasterdataContext.deleteCustomer(updated.get());
        List<SSCustomer> all = SSMasterdataContext.getCustomers();
        assertThat(all).extracting(SSCustomer::getNumber).doesNotContain("C-V2-002");
    }

    @Test
    void getInvoiceCurrencyHandlesMissingCurrencyInSchemaV2() {
        SSCustomer customer = new SSCustomer();

        assertThatCode(customer::getInvoiceCurrency).doesNotThrowAnyException();
        assertThat(customer.getInvoiceCurrency()).isNull();
    }

    private static Integer createCompany(String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create test company for schema V2 integration test");
    }
}


