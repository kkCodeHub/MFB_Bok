package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2PaymentTermRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany();
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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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
        assertThat(Repositories.projects()).isNotNull();
        assertThat(Repositories.resultUnits()).isNotNull();
        assertThat(Repositories.units()).isNotNull();
        assertThat(Repositories.deliveryWays()).isNotNull();
        assertThat(Repositories.deliveryTerms()).isNotNull();
    }

    @Test
    void addAndFetchResultUnitViaRepository() {
        SSNewResultUnit resultUnit = new SSNewResultUnit();
        resultUnit.setNumber("RU-V2-001");
        resultUnit.setName("Repo Result Unit One");
        resultUnit.setDescription("Repository result unit create/fetch");

        Repositories.resultUnits().add(resultUnit);

        Optional<SSNewResultUnit> fetched = Repositories.resultUnits().findByNumber("RU-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("Repo Result Unit One");
        assertThat(fetched.get().getDescription()).isEqualTo("Repository result unit create/fetch");

        Repositories.resultUnits().delete(resultUnit);
    }

    @Test
    void updateAndDeleteResultUnitViaRepository() {
        SSNewResultUnit resultUnit = new SSNewResultUnit();
        resultUnit.setNumber("RU-V2-002");
        resultUnit.setName("Before Result Unit Repo Update");
        resultUnit.setDescription("Before update");
        Repositories.resultUnits().add(resultUnit);

        Optional<SSNewResultUnit> fetched = Repositories.resultUnits().findByNumber("RU-V2-002");
        assertThat(fetched).isPresent();

        SSNewResultUnit updated = fetched.get();
        updated.setName("After Result Unit Repo Update");
        updated.setDescription("After update");
        Repositories.resultUnits().update(updated);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSNewResultUnit> reloaded = Repositories.resultUnits().findByNumber("RU-V2-002");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("After Result Unit Repo Update");
        assertThat(reloaded.get().getDescription()).isEqualTo("After update");

        Repositories.resultUnits().delete(reloaded.get());
        List<SSNewResultUnit> all = Repositories.resultUnits().findAll();
        assertThat(all).extracting(SSNewResultUnit::getNumber).doesNotContain("RU-V2-002");
    }

    @Test
    void findAllSubsetResultUnitsViaRepository() {
        SSNewResultUnit r1 = new SSNewResultUnit();
        r1.setNumber("RU-V2-003");
        r1.setName("Subset Result Unit One");

        SSNewResultUnit r2 = new SSNewResultUnit();
        r2.setNumber("RU-V2-004");
        r2.setName("Subset Result Unit Two");

        Repositories.resultUnits().add(r1);
        Repositories.resultUnits().add(r2);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSNewResultUnit> subset = Repositories.resultUnits().findAll(List.of(r1, r2));
        assertThat(subset).hasSize(2);
        assertThat(subset).extracting(SSNewResultUnit::getNumber)
                .containsExactlyInAnyOrder("RU-V2-003", "RU-V2-004");

        Repositories.resultUnits().delete(r1);
        Repositories.resultUnits().delete(r2);
    }

    @Test
    void addAndFetchProjectViaRepository() {
        SSNewProject project = new SSNewProject();
        project.setNumber("PJ-V2-001");
        project.setName("Repo Project One");
        project.setDescription("Repository project create/fetch");

        Repositories.projects().add(project);

        Optional<SSNewProject> fetched = Repositories.projects().findByNumber("PJ-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("Repo Project One");
        assertThat(fetched.get().getDescription()).isEqualTo("Repository project create/fetch");

        Repositories.projects().delete(project);
    }

    @Test
    void updateAndDeleteProjectViaRepository() {
        SSNewProject project = new SSNewProject();
        project.setNumber("PJ-V2-002");
        project.setName("Before Project Repo Update");
        project.setDescription("Before update");
        Repositories.projects().add(project);

        Optional<SSNewProject> fetched = Repositories.projects().findByNumber("PJ-V2-002");
        assertThat(fetched).isPresent();

        SSNewProject updated = fetched.get();
        updated.setName("After Project Repo Update");
        updated.setDescription("After update");
        Repositories.projects().update(updated);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSNewProject> reloaded = Repositories.projects().findByNumber("PJ-V2-002");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("After Project Repo Update");
        assertThat(reloaded.get().getDescription()).isEqualTo("After update");

        Repositories.projects().delete(reloaded.get());
        List<SSNewProject> all = Repositories.projects().findAll();
        assertThat(all).extracting(SSNewProject::getNumber).doesNotContain("PJ-V2-002");
    }

    @Test
    void findAllSubsetProjectsViaRepository() {
        SSNewProject p1 = new SSNewProject();
        p1.setNumber("PJ-V2-003");
        p1.setName("Subset Project One");

        SSNewProject p2 = new SSNewProject();
        p2.setNumber("PJ-V2-004");
        p2.setName("Subset Project Two");

        Repositories.projects().add(p1);
        Repositories.projects().add(p2);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSNewProject> subset = Repositories.projects().findAll(List.of(p1, p2));
        assertThat(subset).hasSize(2);
        assertThat(subset).extracting(SSNewProject::getNumber)
                .containsExactlyInAnyOrder("PJ-V2-003", "PJ-V2-004");

        Repositories.projects().delete(p1);
        Repositories.projects().delete(p2);
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

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
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

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSSupplier> subset = Repositories.suppliers().findAll(List.of(s1, s2));
        assertThat(subset).hasSize(2);
        assertThat(subset).extracting(SSSupplier::getNumber)
                .containsExactlyInAnyOrder("SR-V2-003", "SR-V2-004");

        Repositories.suppliers().delete(s1);
        Repositories.suppliers().delete(s2);
    }

    // -------------------------------------------------------------------------
    // Unit via Repositories.units()
    // -------------------------------------------------------------------------

    @Test
    void addUpdateAndDeleteUnitViaRepository() {
        SSUnit unit = new SSUnit();
        unit.setName("UNIT-V2-001");
        unit.setDescription("Repository Unit");

        Repositories.units().add(unit);

        Optional<SSUnit> fetched = Repositories.units().findByName("UNIT-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Repository Unit");

        SSUnit updated = fetched.get();
        updated.setDescription("Updated Repository Unit");
        Repositories.units().update(updated);

        Optional<SSUnit> reloaded = Repositories.units().findByName("UNIT-V2-001");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Updated Repository Unit");

        Repositories.units().delete(reloaded.get());
        assertThat(Repositories.units().findByName("UNIT-V2-001")).isEmpty();
    }

    @Test
    void findAllUnitsViaRepository() {
        SSUnit unitOne = new SSUnit();
        unitOne.setName("UNIT-V2-002");
        unitOne.setDescription("Repository Unit One");

        SSUnit unitTwo = new SSUnit();
        unitTwo.setName("UNIT-V2-003");
        unitTwo.setDescription("Repository Unit Two");

        Repositories.units().add(unitOne);
        Repositories.units().add(unitTwo);

        List<SSUnit> allUnits = Repositories.units().findAll();
        assertThat(allUnits).extracting(SSUnit::getName)
                .contains("UNIT-V2-002", "UNIT-V2-003");

        Repositories.units().delete(unitOne);
        Repositories.units().delete(unitTwo);
    }

    // -------------------------------------------------------------------------
    // Currency via Repositories.currencies()
    // -------------------------------------------------------------------------

    @Test
    void addUpdateAndDeleteCurrencyViaRepository() {
        SSCurrency currency = new SSCurrency();
        currency.setName("TEST");
        currency.setDescription("Test Currency");
        currency.setExchangeRate(new BigDecimal("1.50"));

        Repositories.currencies().add(currency);

        Optional<SSCurrency> fetched = Repositories.currencies().findByCode("TEST");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Test Currency");
        assertThat(fetched.get().getExchangeRate()).isEqualByComparingTo("1.50");

        SSCurrency updated = fetched.get();
        updated.setDescription("Updated Test Currency");
        updated.setExchangeRate(new BigDecimal("2.00"));
        Repositories.currencies().update(updated);

        Optional<SSCurrency> reloaded = Repositories.currencies().findByCode("TEST");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Updated Test Currency");
        assertThat(reloaded.get().getExchangeRate()).isEqualByComparingTo("2.00");

        Repositories.currencies().delete(reloaded.get());
        assertThat(Repositories.currencies().findByCode("TEST")).isEmpty();
    }

    @Test
    void findAllCurrenciesViaRepository() {
        SSCurrency currencyOne = new SSCurrency();
        currencyOne.setName("CUR1");
        currencyOne.setDescription("Currency One");
        currencyOne.setExchangeRate(new BigDecimal("1.10"));

        SSCurrency currencyTwo = new SSCurrency();
        currencyTwo.setName("CUR2");
        currencyTwo.setDescription("Currency Two");
        currencyTwo.setExchangeRate(new BigDecimal("2.20"));

        Repositories.currencies().add(currencyOne);
        Repositories.currencies().add(currencyTwo);

        List<SSCurrency> allCurrencies = Repositories.currencies().findAll();
        assertThat(allCurrencies).extracting(SSCurrency::getName)
                .contains("CUR1", "CUR2");

        Repositories.currencies().delete(currencyOne);
        Repositories.currencies().delete(currencyTwo);
    }

    // -------------------------------------------------------------------------
    // Payment Term via Repositories.paymentTerms()
    // -------------------------------------------------------------------------

    @Test
    void addUpdateAndDeletePaymentTermViaRepository() {
        SSPaymentTerm term = new SSPaymentTerm();
        term.setName("NET-30");
        term.setDescription("Test Payment Term");
        term.setDays(30);

        Repositories.paymentTerms().add(term);

        Optional<SSPaymentTerm> fetched = Repositories.paymentTerms().findByName("NET-30");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Test Payment Term");
        assertThat(fetched.get().getDays()).isEqualTo(30);
        assertThat(fetched.get().decodeValue()).isEqualTo(30);

        SSPaymentTerm updated = fetched.get();
        updated.setDescription("Updated Payment Term");
        updated.setDays(45);
        Repositories.paymentTerms().update(updated);

        Optional<SSPaymentTerm> reloaded = Repositories.paymentTerms().findByName("NET-30");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Updated Payment Term");
        assertThat(reloaded.get().getDays()).isEqualTo(45);
        assertThat(reloaded.get().decodeValue()).isEqualTo(45);

        Repositories.paymentTerms().delete(reloaded.get());
        assertThat(Repositories.paymentTerms().findByName("30")).isEmpty();
    }

    @Test
    void findAllPaymentTermsViaRepository() {
        SSPaymentTerm termOne = new SSPaymentTerm();
        termOne.setName("PT-ONE");
        termOne.setDescription("Payment Term One");
        termOne.setDays(15);

        SSPaymentTerm termTwo = new SSPaymentTerm();
        termTwo.setName("PT-TWO");
        termTwo.setDescription("Payment Term Two");
        termTwo.setDays(60);

        Repositories.paymentTerms().add(termOne);
        Repositories.paymentTerms().add(termTwo);

        List<SSPaymentTerm> allTerms = Repositories.paymentTerms().findAll();
        assertThat(allTerms).extracting(SSPaymentTerm::getName)
                .contains("PT-ONE", "PT-TWO");
        assertThat(allTerms).extracting(SSPaymentTerm::getDays)
                .contains(15, 60);

        Repositories.paymentTerms().delete(termOne);
        Repositories.paymentTerms().delete(termTwo);
    }

    @Test
    void paymentTermUpdateFailureRollsBackChanges() {
        SSPaymentTerm term = new SSPaymentTerm();
        term.setName("31");
        term.setDescription("Before rollback test");

        Repositories.paymentTerms().add(term);

        try {
            SSPaymentTerm updated = Repositories.paymentTerms().findByName("31").orElseThrow();
            updated.setDescription("After failed update");

            System.setProperty(V2PaymentTermRepository.FAIL_UPDATE_PROPERTY, "true");

            assertThatThrownBy(() -> Repositories.paymentTerms().update(updated))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update payment term '31'");
        } finally {
            System.clearProperty(V2PaymentTermRepository.FAIL_UPDATE_PROPERTY);
        }

        Optional<SSPaymentTerm> rolledBack = Repositories.paymentTerms().findByName("31");
        assertThat(rolledBack).isPresent();
        assertThat(rolledBack.get().getDescription()).isEqualTo("Before rollback test");

        SSPaymentTerm recovered = rolledBack.get();
        recovered.setDescription("After successful retry");
        Repositories.paymentTerms().update(recovered);

        Optional<SSPaymentTerm> reloaded = Repositories.paymentTerms().findByName("31");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("After successful retry");

        Repositories.paymentTerms().delete(reloaded.get());
    }

    // -------------------------------------------------------------------------
    // Delivery Way via Repositories.deliveryWays()
    // -------------------------------------------------------------------------

    @Test
    void addUpdateAndDeleteDeliveryWayViaRepository() {
        SSDeliveryWay way = new SSDeliveryWay();
        way.setName("WAY-V2-001");
        way.setDescription("Test Delivery Way");

        Repositories.deliveryWays().add(way);

        Optional<SSDeliveryWay> fetched = Repositories.deliveryWays().findByName("WAY-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Test Delivery Way");

        SSDeliveryWay updated = fetched.get();
        updated.setDescription("Updated Delivery Way");
        Repositories.deliveryWays().update(updated);

        Optional<SSDeliveryWay> reloaded = Repositories.deliveryWays().findByName("WAY-V2-001");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Updated Delivery Way");

        Repositories.deliveryWays().delete(reloaded.get());
        assertThat(Repositories.deliveryWays().findByName("WAY-V2-001")).isEmpty();
    }

    @Test
    void findAllDeliveryWaysViaRepository() {
        SSDeliveryWay wayOne = new SSDeliveryWay();
        wayOne.setName("WAY-V2-002");
        wayOne.setDescription("Delivery Way One");

        SSDeliveryWay wayTwo = new SSDeliveryWay();
        wayTwo.setName("WAY-V2-003");
        wayTwo.setDescription("Delivery Way Two");

        Repositories.deliveryWays().add(wayOne);
        Repositories.deliveryWays().add(wayTwo);

        List<SSDeliveryWay> allWays = Repositories.deliveryWays().findAll();
        assertThat(allWays).extracting(SSDeliveryWay::getName)
                .contains("WAY-V2-002", "WAY-V2-003");

        Repositories.deliveryWays().delete(wayOne);
        Repositories.deliveryWays().delete(wayTwo);
    }

    // -------------------------------------------------------------------------
    // Delivery Term via Repositories.deliveryTerms()
    // -------------------------------------------------------------------------

    @Test
    void addUpdateAndDeleteDeliveryTermViaRepository() {
        SSDeliveryTerm term = new SSDeliveryTerm();
        term.setName("TERM-V2-001");
        term.setDescription("Test Delivery Term");

        Repositories.deliveryTerms().add(term);

        Optional<SSDeliveryTerm> fetched = Repositories.deliveryTerms().findByName("TERM-V2-001");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Test Delivery Term");

        SSDeliveryTerm updated = fetched.get();
        updated.setDescription("Updated Delivery Term");
        Repositories.deliveryTerms().update(updated);

        Optional<SSDeliveryTerm> reloaded = Repositories.deliveryTerms().findByName("TERM-V2-001");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDescription()).isEqualTo("Updated Delivery Term");

        Repositories.deliveryTerms().delete(reloaded.get());
        assertThat(Repositories.deliveryTerms().findByName("TERM-V2-001")).isEmpty();
    }

    @Test
    void findAllDeliveryTermsViaRepository() {
        SSDeliveryTerm termOne = new SSDeliveryTerm();
        termOne.setName("TERM-V2-002");
        termOne.setDescription("Delivery Term One");

        SSDeliveryTerm termTwo = new SSDeliveryTerm();
        termTwo.setName("TERM-V2-003");
        termTwo.setDescription("Delivery Term Two");

        Repositories.deliveryTerms().add(termOne);
        Repositories.deliveryTerms().add(termTwo);

        List<SSDeliveryTerm> allTerms = Repositories.deliveryTerms().findAll();
        assertThat(allTerms).extracting(SSDeliveryTerm::getName)
                .contains("TERM-V2-002", "TERM-V2-003");

        Repositories.deliveryTerms().delete(termOne);
        Repositories.deliveryTerms().delete(termTwo);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static Integer createCompany() throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName("V2 Masterdata Repo Test AB");
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}
