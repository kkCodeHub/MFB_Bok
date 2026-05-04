package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSProduct;

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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_product";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

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
    void clearCaches() {
        SSDB.getInstance().clearLists();
    }

    @Test
    void addAndFetchProductInSchemaV2() {
        SSProduct p = new SSProduct();
        p.setNumber("P-V2-001");
        p.setDescription("V2 Product");
        p.setSellingPrice(new BigDecimal("99.50"));

        SSDB.getInstance().addProduct(p);

        Optional<SSProduct> fetched = SSDB.getInstance().getProduct("P-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("V2 Product");
        assertThat(fetched.get().getSellingPrice()).isEqualByComparingTo("99.50");

        SSDB.getInstance().deleteProduct(p);
    }

    @Test
    void updateAndDeleteProductInSchemaV2() {
        SSProduct p = new SSProduct();
        p.setNumber("P-V2-002");
        p.setDescription("Before Product Update");
        SSDB.getInstance().addProduct(p);

        Optional<SSProduct> fetched = SSDB.getInstance().getProduct("P-V2-002");
        assertThat(fetched).isPresent();

        SSProduct updatedProduct = fetched.get();
        updatedProduct.setDescription("After Product Update");
        SSDB.getInstance().updateProduct(updatedProduct);

        SSDB.getInstance().clearLists();
        Optional<SSProduct> updated = SSDB.getInstance().getProduct("P-V2-002");
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After Product Update");

        SSDB.getInstance().deleteProduct(updated.get());
        List<SSProduct> all = SSDB.getInstance().getProducts();
        assertThat(all).extracting(SSProduct::getNumber).doesNotContain("P-V2-002");
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

