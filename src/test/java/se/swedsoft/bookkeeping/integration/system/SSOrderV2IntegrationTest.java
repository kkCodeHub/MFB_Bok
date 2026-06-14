package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;

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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration slice for order CRUD against schema V2.
 */
@Tag("integration")
class SSOrderV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-o-r-d-e-r-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Order Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Order Test Company AB");
        company.setCurrency(new SSCurrency("SEK", "SEK"));
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
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
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchOrderWithRowsInSchemaV2() {
        SSOrder order = order("ORD-V2-CUST-001", "V2 Order Customer AB");
        order.getRows().add(orderRow("P-ORD-001", "Implementation", new BigDecimal("1250.00"), 20, 3010));
        order.getRows().add(orderRow("P-ORD-002", "Support", new BigDecimal("500.00"), 10, 3041));

        SSSalesContext.addOrder(order);

        assertThat(order.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> fetched = SSSalesContext.getOrder(order);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerNr()).isEqualTo("ORD-V2-CUST-001");
        assertThat(fetched.get().getCustomerName()).isEqualTo("V2 Order Customer AB");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 20));
        assertThat(fetched.get().getText()).isEqualTo("Order text in V2");
        assertThat(fetched.get().getYourOrderNumber()).isEqualTo("ORDER-V2-ORD-001");
        assertThat(fetched.get().getEstimatedDelivery()).isEqualTo("2025-08-15");
        assertThat(fetched.get().getInvoiceNr()).isEqualTo(91001);
        assertThat(fetched.get().getPeriodicInvoiceNr()).isEqualTo(92001);
        assertThat(fetched.get().getPurchaseOrderNr()).isEqualTo(93001);
        assertThat(fetched.get().getHideUnitprice()).isTrue();
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.50");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Implementation");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("1250.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(20);

        SSSalesContext.deleteOrder(order);
    }

    @Test
    void updateAndDeleteOrderInSchemaV2() {
        SSOrder order = order("ORD-V2-CUST-002", "Before V2 Order Update");
        order.getRows().add(orderRow("P-ORD-003", "Initial row", new BigDecimal("100.00"), 10, 3010));
        SSSalesContext.addOrder(order);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> fetched = SSSalesContext.getOrder(order);
        assertThat(fetched).isPresent();

        SSOrder updatedOrder = fetched.get();
        updatedOrder.setCustomerName("After V2 Order Update");
        updatedOrder.setText("Updated order text in V2");
        updatedOrder.setEstimatedDelivery("2025-09-10");
        updatedOrder.setHideUnitprice(false);
        updatedOrder.setInvoiceNr(91002);
        updatedOrder.setPeriodicInvoiceNr(92002);
        updatedOrder.setPurchaseOrder(null);
        updatedOrder.getRows().clear();
        updatedOrder.getRows().add(orderRow("P-ORD-004", "Updated row", new BigDecimal("750.00"), 30, 3041));
        SSSalesContext.updateOrder(updatedOrder);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOrder> updated = SSSalesContext.getOrder(order);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After V2 Order Update");
        assertThat(updated.get().getText()).isEqualTo("Updated order text in V2");
        assertThat(updated.get().getEstimatedDelivery()).isEqualTo("2025-09-10");
        assertThat(updated.get().getHideUnitprice()).isFalse();
        assertThat(updated.get().getInvoiceNr()).isEqualTo(91002);
        assertThat(updated.get().getPeriodicInvoiceNr()).isEqualTo(92002);
        assertThat(updated.get().getPurchaseOrderNr()).isNull();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(30);

        Integer orderNumber = updated.get().getNumber();
        SSSalesContext.deleteOrder(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSOrder> all = SSSalesContext.getOrders();
        assertThat(all).extracting(SSOrder::getNumber).doesNotContain(orderNumber);
    }

    @Test
    void creatingOrderHandlesMissingCompanyCurrencyInSchemaV2() {
        SSNewCompany originalCompany = SSDB.getInstance().getCurrentCompany();

        SSNewCompany companyWithoutCurrency = new SSNewCompany();
        companyWithoutCurrency.setId(originalCompany.getId());
        companyWithoutCurrency.setName(originalCompany.getName());

        SSDB.getInstance().setCurrentCompany(companyWithoutCurrency);
        try {
            assertThatCode(SSOrder::new).doesNotThrowAnyException();
        } finally {
            SSDB.getInstance().setCurrentCompany(originalCompany);
        }
    }

    private static SSOrder order(String customerNr, String customerName) {
        SSOrder order = new SSOrder();
        order.setCurrency(null);
        order.setCustomerNr(customerNr);
        order.setCustomerName(customerName);
        order.setLocalDate(LocalDate.of(2025, 7, 20));
        order.setText("Order text in V2");
        order.setYourOrderNumber("ORDER-V2-ORD-001");
        order.setEstimatedDelivery("2025-08-15");
        order.setInvoiceNr(91001);
        order.setPeriodicInvoiceNr(92001);
        SSPurchaseOrder purchaseOrder = new SSPurchaseOrder();
        purchaseOrder.setNumber(93001);
        order.setPurchaseOrder(purchaseOrder);
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
        throw new IllegalStateException("Could not create test company for schema V2 order test");
    }
}




