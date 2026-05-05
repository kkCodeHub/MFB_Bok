package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;

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
 * Integration tests for inventory repository wiring in schema V2.
 */
@Tag("integration")
class SSInventoryV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_inventory_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Inventory Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Inventory Repo Test AB");
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
    void repositoriesInitUsesV2InventoryRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.inventories()).isNotNull();
    }

    @Test
    void addAndFetchInventoryViaRepository() {
        SSInventory inventory = inventory("Repo inventory text");
        inventory.getRows().add(row("P-INVST-REPO-001", 10, 2));

        Repositories.inventories().add(inventory);
        assertThat(inventory.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSInventory> fetched = Repositories.inventories().findByInventory(inventory);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getText()).isEqualTo("Repo inventory text");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getProductNr()).isEqualTo("P-INVST-REPO-001");
        assertThat(fetched.get().getRows().get(0).getStockQuantity()).isEqualTo(10);
        assertThat(fetched.get().getRows().get(0).getChange()).isEqualTo(2);

        Repositories.inventories().delete(fetched.get());
    }

    @Test
    void updateAndDeleteInventoryViaRepository() {
        SSInventory inventory = inventory("Before inventory repo update");
        inventory.getRows().add(row("P-INVST-REPO-002", 8, 0));
        Repositories.inventories().add(inventory);

        SSDB.getInstance().clearLists();
        Optional<SSInventory> fetched = Repositories.inventories().findByInventory(inventory);
        assertThat(fetched).isPresent();

        SSInventory updatedInventory = fetched.get();
        updatedInventory.setText("After inventory repo update");
        updatedInventory.setLocalDate(LocalDate.of(2025, 9, 16));
        updatedInventory.getRows().clear();
        updatedInventory.getRows().add(row("P-INVST-REPO-003", 6, 3));
        Repositories.inventories().update(updatedInventory);

        SSDB.getInstance().clearLists();
        Optional<SSInventory> updated = Repositories.inventories().findByInventory(inventory);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After inventory repo update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 16));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getProductNr()).isEqualTo("P-INVST-REPO-003");
        assertThat(updated.get().getRows().get(0).getStockQuantity()).isEqualTo(6);
        assertThat(updated.get().getRows().get(0).getChange()).isEqualTo(3);

        Integer number = updated.get().getNumber();
        Repositories.inventories().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSInventory> all = Repositories.inventories().findAll();
        assertThat(all).extracting(SSInventory::getNumber).doesNotContain(number);
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
        throw new IllegalStateException("Could not create test company for inventory repository V2 integration test");
    }
}

