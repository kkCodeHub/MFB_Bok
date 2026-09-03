package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSInventoryDeliveriesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;

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
 * Integration slice for inventory CRUD against schema V2.
 */
@Tag("integration")
class SSInventoryV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-i-n-v-e-n-t-o-r-y-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Inventory Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Inventory Test Company AB");
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
    void addAndFetchInventoryWithRowsInSchemaV2() {
        SSInventory inventory = inventory("Initial inventory text");
        inventory.getRows().add(row("P-INVST-001", 10, 2));
        inventory.getRows().add(row("P-INVST-002", 4, -1));

        SSInventoryDeliveriesContext.addInventory(inventory);

        assertThat(inventory.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInventory> fetched = SSInventoryDeliveriesContext.getInventory(inventory);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 28));
        assertThat(fetched.get().getText()).isEqualTo("Initial inventory text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-INVST-001");
        assertThat(fetched.get().getRows().get(0).getStockQuantity()).isEqualTo(10);
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(2);

        SSInventoryDeliveriesContext.deleteInventory(inventory);
    }

    @Test
    void updateAndDeleteInventoryInSchemaV2() {
        SSInventory inventory = inventory("Before inventory update");
        inventory.getRows().add(row("P-INVST-003", 8, 0));
        SSInventoryDeliveriesContext.addInventory(inventory);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInventory> fetched = SSInventoryDeliveriesContext.getInventory(inventory);
        assertThat(fetched).isPresent();

        SSInventory updatedInventory = fetched.get();
        updatedInventory.setText("After inventory update");
        updatedInventory.setLocalDate(LocalDate.of(2025, 9, 16));
        updatedInventory.getRows().clear();
        updatedInventory.getRows().add(row("P-INVST-004", 6, 3));
        SSInventoryDeliveriesContext.updateInventory(updatedInventory);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInventory> updated = SSInventoryDeliveriesContext.getInventory(inventory);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After inventory update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 16));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-INVST-004");
        assertThat(updated.get().getRows().get(0).getStockQuantity()).isEqualTo(6);
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(3);

        Integer inventoryNumber = updated.get().getNumber();
        SSInventoryDeliveriesContext.deleteInventory(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSInventory> all = SSInventoryDeliveriesContext.getInventories();
        assertThat(all).extracting(SSInventory::getNumber).doesNotContain(inventoryNumber);
    }

    private static SSInventory inventory(String text) {
        SSInventory inventory = new SSInventory();
        inventory.setLocalDate(LocalDate.of(2025, 7, 28));
        inventory.setText(text);
        return inventory;
    }

    private static SSInventoryRow row(String productNr, Integer stockQuantity, Integer change) {
        SSInventoryRow row = new SSInventoryRow();
        row.setProductNr(productNr);
        row.setStockQuantity(stockQuantity);
        row.setChange(change);
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


