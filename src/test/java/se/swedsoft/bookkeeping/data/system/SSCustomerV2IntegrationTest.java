package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Integration slice for customer CRUD against schema V2.
 */
@Tag("integration")
class SSCustomerV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_customer";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

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
    void clearCaches() {
        SSDB.getInstance().clearLists();
    }

    @Test
    void addAndFetchCustomerInSchemaV2() {
        SSCustomer c = new SSCustomer();
        c.setNumber("C-V2-001");
        c.setName("V2 Integration AB");
        c.setEMail("v2@integration.se");
        c.setPhone1("08-100200");

        SSDB.getInstance().addCustomer(c);

        Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("V2 Integration AB");
        assertThat(fetched.get().getEMail()).isEqualTo("v2@integration.se");
        assertThat(fetched.get().getPhone1()).isEqualTo("08-100200");

        SSDB.getInstance().deleteCustomer(c);
    }

    @Test
    void updateAndDeleteCustomerInSchemaV2() {
        SSCustomer c = new SSCustomer();
        c.setNumber("C-V2-002");
        c.setName("Before V2 Update");

        SSDB.getInstance().addCustomer(c);

        Optional<SSCustomer> fetched = SSDB.getInstance().getCustomer("C-V2-002");
        assertThat(fetched).isPresent();

        SSCustomer updatedCustomer = fetched.get();
        updatedCustomer.setName("After V2 Update");
        SSDB.getInstance().updateCustomer(updatedCustomer);

        SSDB.getInstance().clearLists();
        Optional<SSCustomer> updated = SSDB.getInstance().getCustomer("C-V2-002");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("After V2 Update");

        SSDB.getInstance().deleteCustomer(updated.get());
        List<SSCustomer> all = SSDB.getInstance().getCustomers();
        assertThat(all).extracting(SSCustomer::getNumber).doesNotContain("C-V2-002");
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

