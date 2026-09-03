package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 inventory repository backed by the normalized V2 schema.
 */
public class V2InventoryRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2InventoryRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 inventory repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2InventoryRepository(Connection connection,
                                 Supplier<SSNewCompany> currentCompanySupplier,
                                 RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (currentCompanySupplier == null) {
            throw new NullPointerException("currentCompanySupplier must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.currentCompanySupplier = currentCompanySupplier;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSInventory> findAll() {
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSInventory> inventories = new LinkedList<>();
            int max = -1;
            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_inventory WHERE companyid=? AND id>?")) {
                    statement.setObject(1, company.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            inventories.add(mapInventory(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }
            return inventories;
        } catch (SQLException e) {
            throw handleFailure("load inventories", e);
        }
    }

    public Optional<SSInventory> findByInventory(SSInventory inventory) {
        if (inventory == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_inventory WHERE number=? AND companyid=?")) {
                statement.setObject(1, inventory.getNumber());
                statement.setObject(2, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapInventory(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find inventory '" + inventory.getNumber() + "'", e);
        }
    }

    public SSInventory createNew() {
        SSInventory inventory = new SSInventory();
        inventory.doAutoIncrement();
        return inventory;
    }

    public void add(SSInventory inventory) {
        if (inventory == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            int companyNumber = company.getAutoIncrement().getNumber("inventory");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_inventory WHERE companyid=?")) {
                statement.setObject(1, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            inventory.setNumber(number + 1);
                        } else {
                            inventory.setNumber(companyNumber + 1);
                        }
                    } else {
                        inventory.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer inventoryId = null;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_inventory(number,companyid,vdate,itext) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setObject(1, inventory.getNumber());
                insert.setObject(2, company.getId());
                bindLocalDate(insert, 3, inventory.getLocalDate());
                insert.setObject(4, inventory.getText());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        inventoryId = keys.getInt(1);
                    }
                }
            }

            if (inventoryId == null) {
                inventoryId = getInventoryId(inventory.getNumber(), company.getId());
            }
            if (inventoryId != null) {
                replaceInventoryRows(inventoryId, inventory);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWINVENTORY", "TBL_INVENTORY", String.valueOf(inventory.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add inventory '" + inventory.getNumber() + "'", e);
        }
    }

    public void update(SSInventory inventory) {
        if (inventory == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_inventory SET vdate=?,itext=? WHERE number=? AND companyid=?")) {
                bindLocalDate(statement, 1, inventory.getLocalDate());
                statement.setObject(2, inventory.getText());
                statement.setObject(3, inventory.getNumber());
                statement.setObject(4, company.getId());
                statement.executeUpdate();
            }

            Integer inventoryId = getInventoryId(inventory.getNumber(), company.getId());
            if (inventoryId != null) {
                replaceInventoryRows(inventoryId, inventory);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITINVENTORY", "TBL_INVENTORY", String.valueOf(inventory.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update inventory '" + inventory.getNumber() + "'", e);
        }
    }

    public void delete(SSInventory inventory) {
        if (inventory == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            Integer inventoryId = getInventoryId(inventory.getNumber(), company.getId());
            if (inventoryId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_inventory_row WHERE inventory_id=?")) {
                    deleteRows.setObject(1, inventoryId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_inventory WHERE number=? AND companyid=?")) {
                statement.setObject(1, inventory.getNumber());
                statement.setObject(2, company.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEINVENTORY", "TBL_INVENTORY", String.valueOf(inventory.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete inventory '" + inventory.getNumber() + "'", e);
        }
    }

    private SSInventory mapInventory(ResultSet resultSet) throws SQLException {
        SSInventory inventory = new SSInventory(false);
        inventory.setNumber((Integer) resultSet.getObject("number"));
        Date date = resultSet.getDate("vdate");
        if (date != null) {
            inventory.setLocalDate(date.toLocalDate());
        }
        inventory.setText(resultSet.getString("itext"));
        inventory.getRows().clear();
        inventory.getRows().addAll(getInventoryRows(resultSet.getInt("id")));
        return inventory;
    }

    private List<SSInventoryRow> getInventoryRows(Integer inventoryId) throws SQLException {
        List<SSInventoryRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_inventory_row WHERE inventory_id=? ORDER BY id")) {
            statement.setObject(1, inventoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSInventoryRow row = new SSInventoryRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setStockQuantity((Integer) resultSet.getObject("quantity"));
                    row.setChange((Integer) resultSet.getObject("change_qty"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceInventoryRows(Integer inventoryId, SSInventory inventory) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_inventory_row WHERE inventory_id=?")) {
            delete.setObject(1, inventoryId);
            delete.executeUpdate();
        }

        for (SSInventoryRow row : inventory.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_inventory_row(inventory_id,product_nr,quantity,change_qty) VALUES(?,?,?,?)")) {
                insert.setObject(1, inventoryId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getStockQuantity());
                insert.setObject(4, row.getChange());
                insert.executeUpdate();
            }
        }
    }

    private Integer getInventoryId(Integer inventoryNumber, Integer companyId) throws SQLException {
        if (inventoryNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_inventory WHERE number=? AND companyid=?")) {
            statement.setObject(1, inventoryNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private void bindLocalDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        statement.setDate(index, date == null ? null : Date.valueOf(date));
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback inventory transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}


