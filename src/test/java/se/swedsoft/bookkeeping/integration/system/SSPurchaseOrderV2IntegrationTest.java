package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSPurchaseContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSPurchaseOrderRow;
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

/**
 * Integration slice for purchase-order CRUD against schema V2.
 */
@Tag("integration")
class SSPurchaseOrderV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-p-u-r-c-h-a-s-e-o-r-d-e-r-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Purchase Order Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Purchase Order Test Company AB");
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
    void addAndFetchPurchaseOrderWithRowsInSchemaV2() {
        SSPurchaseOrder purchaseOrder = purchaseOrder("SUP-PO-001", "Supplier PO AB");
        purchaseOrder.getRows().add(orderRow("P-PO-001", "Hardware", new BigDecimal("125.00"), 4, 4010));
        purchaseOrder.getRows().add(orderRow("P-PO-002", "Service", new BigDecimal("500.00"), 1, 4041));

        SSPurchaseContext.addPurchaseOrder(purchaseOrder);

        assertThat(purchaseOrder.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPurchaseOrder> fetched = SSPurchaseContext.getPurchaseOrder(purchaseOrder);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierNr()).isEqualTo("SUP-PO-001");
        assertThat(fetched.get().getSupplierName()).isEqualTo("Supplier PO AB");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 12));
        assertThat(fetched.get().getLocalEstimatedDelivery()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.00");
        assertThat(fetched.get().isStockInfluencing()).isTrue();
        assertThat(fetched.get().isPrinted()).isFalse();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Hardware");
        assertThat(fetched.get().getRows().get(0).getUnitPrice()).isEqualByComparingTo("125.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(4);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(4010);

        SSPurchaseContext.deletePurchaseOrder(purchaseOrder);
    }

    @Test
    void updateAndDeletePurchaseOrderInSchemaV2() {
        SSPurchaseOrder purchaseOrder = purchaseOrder("SUP-PO-002", "Before PO Update");
        purchaseOrder.getRows().add(orderRow("P-PO-003", "Initial row", new BigDecimal("100.00"), 1, 4010));
        SSPurchaseContext.addPurchaseOrder(purchaseOrder);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPurchaseOrder> fetched = SSPurchaseContext.getPurchaseOrder(purchaseOrder);
        assertThat(fetched).isPresent();

        SSPurchaseOrder updatedPurchaseOrder = fetched.get();
        updatedPurchaseOrder.setSupplierName("After PO Update");
        updatedPurchaseOrder.setText("Updated purchase-order text in V2");
        updatedPurchaseOrder.setLocalEstimatedDelivery(LocalDate.of(2025, 9, 10));
        updatedPurchaseOrder.setStockInfluencing(false);
        updatedPurchaseOrder.setPrinted(true);
        updatedPurchaseOrder.getRows().clear();
        updatedPurchaseOrder.getRows().add(
                orderRow("P-PO-004", "Updated row", new BigDecimal("750.00"), 3, 4041));
        SSPurchaseContext.updatePurchaseOrder(updatedPurchaseOrder);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPurchaseOrder> updated = SSPurchaseContext.getPurchaseOrder(purchaseOrder);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After PO Update");
        assertThat(updated.get().getText()).isEqualTo("Updated purchase-order text in V2");
        assertThat(updated.get().getLocalEstimatedDelivery()).isEqualTo(LocalDate.of(2025, 9, 10));
        assertThat(updated.get().isStockInfluencing()).isFalse();
        assertThat(updated.get().isPrinted()).isTrue();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer purchaseOrderNumber = updated.get().getNumber();
        SSPurchaseContext.deletePurchaseOrder(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSPurchaseOrder> all = SSPurchaseContext.getPurchaseOrders();
        assertThat(all).extracting(SSPurchaseOrder::getNumber).doesNotContain(purchaseOrderNumber);
    }

    private static SSPurchaseOrder purchaseOrder(String supplierNr, String supplierName) {
        SSPurchaseOrder purchaseOrder = new SSPurchaseOrder();
        purchaseOrder.setSupplierNr(supplierNr);
        purchaseOrder.setSupplierName(supplierName);
        purchaseOrder.setLocalDate(LocalDate.of(2025, 7, 12));
        purchaseOrder.setLocalEstimatedDelivery(LocalDate.of(2025, 8, 1));
        purchaseOrder.setCurrencyRate(new BigDecimal("10.00"));
        purchaseOrder.setText("Purchase-order text in V2");
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
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


