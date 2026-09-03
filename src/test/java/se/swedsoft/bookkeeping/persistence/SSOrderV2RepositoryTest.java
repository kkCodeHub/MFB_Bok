package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2OrderRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for order repository wiring in schema V2.
 */
@Tag("integration")
class SSOrderV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_order_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Order Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Order Repo Test AB");
        company.setCurrency(new SSCurrency("SEK", "SEK"));
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

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
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2OrderRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.orders()).isInstanceOf(V2OrderRepository.class);
    }

    @Test
    void addAndFetchOrderViaRepository() {
        SSOrder order = order("ORD-REPO-CUST-001", "Repo Order Customer AB");
        order.getRows().add(orderRow("P-ORD-REPO-001", "Repo row", new BigDecimal("1250.00"), 2, 3010));

        Repositories.orders().add(order);
        assertThat(order.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> fetched = Repositories.orders().findByOrder(order);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerName()).isEqualTo("Repo Order Customer AB");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.orders().delete(fetched.get());
    }

    @Test
    void updateAndDeleteOrderViaRepository() {
        SSOrder order = order("ORD-REPO-CUST-002", "Before Order Repo Update");
        order.getRows().add(orderRow("P-ORD-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 3010));
        Repositories.orders().add(order);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> fetched = Repositories.orders().findByOrder(order);
        assertThat(fetched).isPresent();

        SSOrder updatedOrder = fetched.get();
        updatedOrder.setCustomerName("After Order Repo Update");
        updatedOrder.setText("Updated order text via repository");
        updatedOrder.setEstimatedDelivery("2025-09-10");
        updatedOrder.setHideUnitprice(false);
        updatedOrder.getRows().clear();
        updatedOrder.getRows().add(orderRow("P-ORD-REPO-003", "After update row", new BigDecimal("750.00"), 3, 3041));
        Repositories.orders().update(updatedOrder);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> updated = Repositories.orders().findByOrder(order);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After Order Repo Update");
        assertThat(updated.get().getText()).isEqualTo("Updated order text via repository");
        assertThat(updated.get().getEstimatedDelivery()).isEqualTo("2025-09-10");
        assertThat(updated.get().getHideUnitprice()).isFalse();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.orders().delete(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSOrder> all = Repositories.orders().findAll();
        assertThat(all).extracting(SSOrder::getNumber).doesNotContain(number);
    }

    private static SSOrder order(String customerNr, String customerName) {
        SSOrder order = new SSOrder();
        order.setCurrency(null);
        order.setCustomerNr(customerNr);
        order.setCustomerName(customerName);
        order.setLocalDate(LocalDate.of(2025, 7, 20));
        order.setText("Order text in repository test");
        order.setYourOrderNumber("ORDER-REPO-001");
        order.setEstimatedDelivery("2025-08-15");
        order.setInvoiceNr(91001);
        order.setPeriodicInvoiceNr(92001);
        order.setPurchaseOrder(null);
        order.setHideUnitprice(true);
        order.setCurrencyRate(new BigDecimal("10.50"));
        return order;
    }

    private static SSSaleRow orderRow(String productNr, String description, BigDecimal unitPrice, int quantity,
                                      int accountNumber) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(accountNumber);
        row.setProjectNr("PRJ-1");
        row.setResultUnitNr("RES-1");
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}

