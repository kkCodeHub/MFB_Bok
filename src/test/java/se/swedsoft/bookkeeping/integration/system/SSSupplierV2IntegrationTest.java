package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplier;

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
 * Integration slice for supplier CRUD against schema V2.
 */
@Tag("integration")
class SSSupplierV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-s-u-p-p-l-i-e-r-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Supplier Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Supplier Test Company AB");
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
    void addAndFetchSupplierInSchemaV2() {
        SSSupplier s = new SSSupplier();
        s.setNumber("S-V2-001");
        s.setName("V2 Supplier AB");
        s.setEMail("supplier@v2.se");
        s.setPhone1("08-777666");

        SSMasterdataContext.addSupplier(s);

        Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("V2 Supplier AB");
        assertThat(fetched.get().getEMail()).isEqualTo("supplier@v2.se");
        assertThat(fetched.get().getPhone1()).isEqualTo("08-777666");

        SSMasterdataContext.deleteSupplier(s);
    }

    @Test
    void updateAndDeleteSupplierInSchemaV2() {
        SSSupplier s = new SSSupplier();
        s.setNumber("S-V2-002");
        s.setName("Before Supplier Update");
        SSMasterdataContext.addSupplier(s);

        Optional<SSSupplier> fetched = SSMasterdataContext.getSupplier(s);
        assertThat(fetched).isPresent();

        SSSupplier updatedSupplier = fetched.get();
        updatedSupplier.setName("After Supplier Update");
        SSMasterdataContext.updateSupplier(updatedSupplier);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSSupplier> updated = SSMasterdataContext.getSupplier(s);
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("After Supplier Update");

        SSMasterdataContext.deleteSupplier(updated.get());
        List<SSSupplier> all = SSMasterdataContext.getSuppliers();
        assertThat(all).extracting(SSSupplier::getNumber).doesNotContain("S-V2-002");
    }

    @Test
    void getCurrencyHandlesMissingCurrencyInSchemaV2() {
        SSSupplier supplier = new SSSupplier();

        assertThatCode(supplier::getCurrency).doesNotThrowAnyException();
        assertThat(supplier.getCurrency()).isNull();
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
        throw new IllegalStateException("Could not create test company for schema V2 supplier test");
    }
}


