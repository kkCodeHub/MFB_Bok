package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSDB;

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
 * Integration tests for the {@link Repositories} factory against schema V2.
 *
 * <p>Verifies that {@link Repositories#init(SSDB)} wires in the V2
 * repository implementations when {@code fribok.schema.version=v2} and that
 * full CRUD round-trips work through the repository API for all three
 * masterdata types (customer, product, supplier).</p>
 *
 * <p>Each test runs against an isolated in-memory HSQLDB instance so there
 * is no cross-test contamination with other integration slices.</p>
 */
@Tag("integration")
class SSMasterdataV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_masterdata_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Masterdata Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Masterdata Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        Repositories.init(SSDB.getInstance());
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

    // -------------------------------------------------------------------------
    // Customer via Repositories.customers()
    // -------------------------------------------------------------------------

    @Test
    void repositoriesInitUsesV2Implementations() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.customers()).isNotNull();
        assertThat(Repositories.products()).isNotNull();
        assertThat(Repositories.suppliers()).isNotNull();
    }

    @Test
    void addAndFetchCustomerViaRepository() {
        SSCustomer c = new SSCustomer();
        c.setNumber("CR-V2-001");
        c.setName("Repo Customer AB");
        c.setEMail("repo@customer.se");

        Repositories.customers().add(c);

        Optional<SSCustomer> fetched = Repositories.customers().findByNumber("CR-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("Repo Customer AB");
        assertThat(fetched.get().getEMail()).isEqualTo("repo@customer.se");

        Repositories.customers().delete(c);
    }

    @Test
    void updateAndDeleteCustomerViaRepository() {
        SSCustomer c = new SSCustomer();
        c.setNumber("CR-V2-002");
        c.setName("Before Repo Update");

        Repositories.customers().add(c);

        Optional<SSCustomer> fetched = Repositories.customers().findByNumber("CR-V2-002");
        assertThat(fetched).isPresent();

        SSCustomer updated = fetched.get();
        updated.setName("After Repo Update");
        Repositories.customers().update(updated);

        SSDB.getInstance().clearLists();
        Optional<SSCustomer> reloaded = Repositories.customers().findByNumber("CR-V2-002");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("After Repo Update");

        Repositories.customers().delete(reloaded.get());
        List<SSCustomer> all = Repositories.customers().findAll();
        assertThat(all).extracting(SSCustomer::getNumber).doesNotContain("CR-V2-002");
    }

    @Test
    void findAllSubsetCustomersViaRepository() {
        SSCustomer c1 = new SSCustomer();
        c1.setNumber("CR-V2-003");
        c1.setName("Subset Customer One");
        SSCustomer c2 = new SSCustomer();
        c2.setNumber("CR-V2-004");
        c2.setName("Subset Customer Two");

        Repositories.customers().add(c1);
        Repositories.customers().add(c2);

        SSDB.getInstance().clearLists();
        List<SSCustomer> subset = Repositories.customers().findAll(List.of(c1, c2));
        assertThat(subset).hasSize(2);
        assertThat(subset).extracting(SSCustomer::getNumber)
                .containsExactlyInAnyOrder("CR-V2-003", "CR-V2-004");

        Repositories.customers().delete(c1);
        Repositories.customers().delete(c2);
    }

    // -------------------------------------------------------------------------
    // Product via Repositories.products()
    // -------------------------------------------------------------------------

    @Test
    void addAndFetchProductViaRepository() {
        SSProduct p = new SSProduct();
        p.setNumber("PR-V2-001");
        p.setDescription("Repo Product");
        p.setSellingPrice(new BigDecimal("149.00"));

        Repositories.products().add(p);

        Optional<SSProduct> fetched = Repositories.products().findByNumber("PR-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Repo Product");
        assertThat(fetched.get().getSellingPrice()).isEqualByComparingTo("149.00");

        Repositories.products().delete(p);
    }

    @Test
    void updateAndDeleteProductViaRepository() {
        SSProduct p = new SSProduct();
        p.setNumber("PR-V2-002");
        p.setDescription("Before Product Repo Update");
        p.setSellingPrice(BigDecimal.TEN);

        Repositories.products().add(p);

        Optional<SSProduct> fetched = Repositories.products().findByNumber("PR-V2-002");
        assertThat(fetched).isPresent();

        SSProduct updated = fetched.get();
        updated.setDescription("After Product Repo Update");
        updated.setSellingPrice(new BigDecimal("20.00"));
        Repositories.products().update(updated);

        SSDB.getInstance().clearLists();
        Optional<SSProduct> reloaded = Repositories.products().findByNumber("PR-V2-002");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("After Product Repo Update");
        assertThat(reloaded.get().getSellingPrice()).isEqualByComparingTo("20.00");

        Repositories.products().delete(reloaded.get());
        List<SSProduct> all = Repositories.products().findAll();
        assertThat(all).extracting(SSProduct::getNumber).doesNotContain("PR-V2-002");
    }

    // -------------------------------------------------------------------------
    // Supplier via Repositories.suppliers()
    // -------------------------------------------------------------------------

    @Test
    void addAndFetchSupplierViaRepository() {
        SSSupplier s = new SSSupplier();
        s.setNumber("SR-V2-001");
        s.setName("Repo Supplier AB");
        s.setEMail("repo@supplier.se");

        Repositories.suppliers().add(s);

        Optional<SSSupplier> fetched = Repositories.suppliers().findBySupplier(s);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("Repo Supplier AB");
        assertThat(fetched.get().getEMail()).isEqualTo("repo@supplier.se");

        Repositories.suppliers().delete(s);
    }

    @Test
    void updateAndDeleteSupplierViaRepository() {
        SSSupplier s = new SSSupplier();
        s.setNumber("SR-V2-002");
        s.setName("Before Supplier Repo Update");

        Repositories.suppliers().add(s);

        Optional<SSSupplier> fetched = Repositories.suppliers().findBySupplier(s);
        assertThat(fetched).isPresent();

        SSSupplier updated = fetched.get();
        updated.setName("After Supplier Repo Update");
        Repositories.suppliers().update(updated);

        SSDB.getInstance().clearLists();
        Optional<SSSupplier> reloaded = Repositories.suppliers().findBySupplier(s);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("After Supplier Repo Update");

        Repositories.suppliers().delete(reloaded.get());
        List<SSSupplier> all = Repositories.suppliers().findAll();
        assertThat(all).extracting(SSSupplier::getNumber).doesNotContain("SR-V2-002");
    }

    @Test
    void findAllSubsetSuppliersViaRepository() {
        SSSupplier s1 = new SSSupplier();
        s1.setNumber("SR-V2-003");
        s1.setName("Subset Supplier One");
        SSSupplier s2 = new SSSupplier();
        s2.setNumber("SR-V2-004");
        s2.setName("Subset Supplier Two");

        Repositories.suppliers().add(s1);
        Repositories.suppliers().add(s2);

        SSDB.getInstance().clearLists();
        List<SSSupplier> subset = Repositories.suppliers().findAll(List.of(s1, s2));
        assertThat(subset).hasSize(2);
        assertThat(subset).extracting(SSSupplier::getNumber)
                .containsExactlyInAnyOrder("SR-V2-003", "SR-V2-004");

        Repositories.suppliers().delete(s1);
        Repositories.suppliers().delete(s2);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

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
        throw new IllegalStateException(
                "Could not create test company for masterdata repository V2 integration test");
    }
}

