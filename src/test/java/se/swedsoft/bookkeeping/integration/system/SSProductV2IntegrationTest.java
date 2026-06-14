package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSProductRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration slice for product CRUD against schema V2.
 */
@Tag("integration")
class SSProductV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-p-r-o-d-u-c-t-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Product Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Product Test Company AB");
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
    void addAndFetchProductInSchemaV2() {
        SSProduct p = new SSProduct();
        p.setNumber("P-V2-001");
        p.setDescription("V2 Product");
        p.setSellingPrice(new BigDecimal("99.50"));

        SSProductContext.addProduct(p);

        Optional<SSProduct> fetched = SSProductContext.getProduct(p);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("V2 Product");
        assertThat(fetched.get().getSellingPrice()).isEqualByComparingTo("99.50");

        SSProductContext.deleteProduct(p);
    }

    @Test
    void updateAndDeleteProductInSchemaV2() {
        SSProduct p = new SSProduct();
        p.setNumber("P-V2-002");
        p.setDescription("Before Product Update");
        SSProductContext.addProduct(p);

        Optional<SSProduct> fetched = SSProductContext.getProduct(p);
        assertThat(fetched).isPresent();

        SSProduct updatedProduct = fetched.get();
        updatedProduct.setDescription("After Product Update");
        SSProductContext.updateProduct(updatedProduct);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSProduct> updated = Optional.ofNullable(SSProductContext.getProduct("P-V2-002"));
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After Product Update");

        SSProductContext.deleteProduct(updated.get());
        List<SSProduct> all = SSProductContext.getProducts();
        assertThat(all).extracting(SSProduct::getNumber).doesNotContain("P-V2-002");
    }

    @Test
    void shouldPersistAndLoadParcelRowsWithTenthsQuantityInSchemaV2() {
        SSProduct component = new SSProduct();
        component.setNumber("P-V2-COMP");
        component.setDescription("Component Product");
        component.setSellingPrice(new BigDecimal("10.00"));
        SSProductContext.addProduct(component);

        SSProduct parcel = new SSProduct();
        parcel.setNumber("P-V2-PARCEL");
        parcel.setDescription("Parcel Product");

        SSProductRow row = new SSProductRow();
        row.setProduct(component);
        row.setQuantity(25); // 2.5 in tenths-based storage
        parcel.getParcelRows().add(row);

        SSProductContext.addProduct(parcel);
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        Optional<SSProduct> fetched = Optional.ofNullable(SSProductContext.getProduct("P-V2-PARCEL"));
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getParcelRows()).hasSize(1);
        assertThat(fetched.get().getParcelRows().get(0).getProductNr()).isEqualTo("P-V2-COMP");
        assertThat(fetched.get().getParcelRows().get(0).getQuantity()).isEqualTo(25);

        SSProductContext.deleteProduct(fetched.get());
        SSProductContext.deleteProduct(component);
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
        throw new IllegalStateException("Could not create test company for schema V2 product test");
    }
}


