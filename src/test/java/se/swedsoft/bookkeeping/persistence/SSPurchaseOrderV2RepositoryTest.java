package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSPurchaseOrderRow;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;

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
 * Integration tests for purchase-order repository wiring in schema V2.
 */
@Tag("integration")
class SSPurchaseOrderV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_purchaseorder_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Purchase Order Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Purchase Order Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
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
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2PurchaseOrderRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.purchaseOrders()).isNotNull();
    }

    @Test
    void addAndFetchPurchaseOrderViaRepository() {
        SSPurchaseOrder purchaseOrder = purchaseOrder("SUP-PO-REPO-001", "Repo Purchase Supplier AB");
        purchaseOrder.getRows().add(orderRow("P-PO-REPO-001", "Repo row", new BigDecimal("125.00"), 2, 4010));

        Repositories.purchaseOrders().add(purchaseOrder);
        assertThat(purchaseOrder.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSPurchaseOrder> fetched = Repositories.purchaseOrders().findByPurchaseOrder(purchaseOrder);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierName()).isEqualTo("Repo Purchase Supplier AB");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.purchaseOrders().delete(fetched.get());
    }

    @Test
    void updateAndDeletePurchaseOrderViaRepository() {
        SSPurchaseOrder purchaseOrder = purchaseOrder("SUP-PO-REPO-002", "Before PO Repo Update");
        purchaseOrder.getRows().add(orderRow("P-PO-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 4010));
        Repositories.purchaseOrders().add(purchaseOrder);

        SSDB.getInstance().clearLists();
        Optional<SSPurchaseOrder> fetched = Repositories.purchaseOrders().findByPurchaseOrder(purchaseOrder);
        assertThat(fetched).isPresent();

        SSPurchaseOrder updatedPurchaseOrder = fetched.get();
        updatedPurchaseOrder.setSupplierName("After PO Repo Update");
        updatedPurchaseOrder.setText("Updated purchase-order text via repository");
        updatedPurchaseOrder.setLocalEstimatedDelivery(LocalDate.of(2025, 9, 15));
        updatedPurchaseOrder.getRows().clear();
        updatedPurchaseOrder.getRows().add(
                orderRow("P-PO-REPO-003", "After update row", new BigDecimal("750.00"), 3, 4041));
        Repositories.purchaseOrders().update(updatedPurchaseOrder);

        SSDB.getInstance().clearLists();
        Optional<SSPurchaseOrder> updated = Repositories.purchaseOrders().findByPurchaseOrder(purchaseOrder);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After PO Repo Update");
        assertThat(updated.get().getText()).isEqualTo("Updated purchase-order text via repository");
        assertThat(updated.get().getLocalEstimatedDelivery()).isEqualTo(LocalDate.of(2025, 9, 15));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.purchaseOrders().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSPurchaseOrder> all = Repositories.purchaseOrders().findAll();
        assertThat(all).extracting(SSPurchaseOrder::getNumber).doesNotContain(number);
    }

    private static SSPurchaseOrder purchaseOrder(String supplierNr, String supplierName) {
        SSPurchaseOrder purchaseOrder = new SSPurchaseOrder();
        purchaseOrder.setSupplierNr(supplierNr);
        purchaseOrder.setSupplierName(supplierName);
        purchaseOrder.setLocalDate(LocalDate.of(2025, 7, 12));
        purchaseOrder.setLocalEstimatedDelivery(LocalDate.of(2025, 8, 1));
        purchaseOrder.setCurrencyRate(new BigDecimal("10.00"));
        purchaseOrder.setText("Purchase-order text in repository test");
        purchaseOrder.setPrinted(false);
        purchaseOrder.setStockInfluencing(true);
        return purchaseOrder;
    }

    private static SSPurchaseOrderRow orderRow(
            String productNr,
            String description,
            BigDecimal unitPrice,
            int quantity,
            int accountNumber) {
        SSPurchaseOrderRow row = new SSPurchaseOrderRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setSupplierArticleNr("ART-" + productNr);
        row.setUnitPrice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setAccountNr(accountNumber);
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
        throw new IllegalStateException("Could not create test company for purchase-order repository V2 integration test");
    }
}

