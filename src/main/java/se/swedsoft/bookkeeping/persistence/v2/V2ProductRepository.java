package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSProductRow;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * V2 product repository backed by the normalized V2 schema.
 */
public class V2ProductRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2ProductRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 product repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2ProductRepository(Connection connection, Supplier<SSNewCompany> currentCompanySupplier,
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

    public List<SSProduct> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSProduct> products = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_product WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            products.add(mapProductV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return products;
        } catch (SQLException e) {
            throw handleFailure("load products", e);
        }
    }

    public Optional<SSProduct> findByNumber(String productNumber) {
        if (productNumber == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_product WHERE LOWER(number)=LOWER(?) AND companyid=?")) {
                statement.setObject(1, productNumber);
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapProductV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("load product '" + productNumber + "'", e);
        }
    }

    public Optional<SSProduct> findByProduct(SSProduct product) {
        if (product == null) {
            return Optional.empty();
        }
        return findByNumber(product.getNumber());
    }

    public List<SSProduct> findAll(List<SSProduct> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        List<SSProduct> products = new LinkedList<>();
        for (SSProduct product : subset) {
            findByProduct(product).ifPresent(products::add);
        }
        return products;
    }

    public void add(SSProduct product) {
        if (product == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer productId = null;

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_product(" +
                            "number,companyid,description,unitprice,tax_code,warehouse_location," +
                            "orderpoint,ordercount,purchase_price,stock_price,freight,supplier_nr," +
                            "supplier_product_nr,expired,stock_goods,only_whole_quantity,unit,weight,volume,project_number" +
                            ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                bindProductV2(statement, product, currentCompany.getId());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        productId = keys.getInt(1);
                    }
                }
            }

            if (productId == null) {
                productId = getProductIdV2(product.getNumber(), currentCompany.getId());
            }
            if (productId != null) {
                replaceProductRowsV2(productId, product);
                replaceProductAccountsV2(productId, product);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWPRODUCT", "TBL_PRODUCT", product.getNumber());
        } catch (SQLException e) {
            throw handleFailure("add product '" + product.getNumber() + "'", e);
        }
    }

    public void update(SSProduct product) {
        if (product == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_product SET " +
                            "description=?,unitprice=?,tax_code=?,warehouse_location=?,orderpoint=?," +
                            "ordercount=?,purchase_price=?,stock_price=?,freight=?,supplier_nr=?," +
                            "supplier_product_nr=?,expired=?,stock_goods=?,only_whole_quantity=?,unit=?,weight=?,volume=?," +
                            "project_number=? WHERE number=? AND companyid=?")) {

                int i = 1;
                statement.setObject(i++, product.getDescription());
                statement.setObject(i++, product.getSellingPrice());
                statement.setObject(i++, product.getTaxCode() == null ? null : product.getTaxCode().name());
                statement.setObject(i++, product.getWarehouseLocation());
                statement.setObject(i++, product.getOrderpoint());
                statement.setObject(i++, product.getOrdercount());
                statement.setObject(i++, product.getPurchasePrice());
                statement.setObject(i++, product.getStockPrice());
                statement.setObject(i++, product.getUnitFreight());
                statement.setObject(i++, product.getSupplierNr());
                statement.setObject(i++, product.getSupplierProductNr());
                statement.setObject(i++, product.isExpired());
                statement.setObject(i++, product.isStockProduct());
                statement.setObject(i++, product.isOnlyWholeQuantity());
                statement.setObject(i++, product.getUnit() == null ? null : product.getUnit().getName());
                statement.setObject(i++, product.getWeight());
                statement.setObject(i++, product.getVolume());
                statement.setObject(i++, product.getProjectNr());
                statement.setObject(i++, product.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer productId = getProductIdV2(product.getNumber(), currentCompany.getId());
            if (productId != null) {
                replaceProductRowsV2(productId, product);
                replaceProductAccountsV2(productId, product);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITPRODUCT", "TBL_PRODUCT", product.getNumber());
        } catch (SQLException e) {
            throw handleFailure("update product '" + product.getNumber() + "'", e);
        }
    }

    public void delete(SSProduct product) {
        if (product == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer productId = getProductIdV2(product.getNumber(), currentCompany.getId());
            if (productId != null) {
                deleteProductAccountsV2(productId);
                deleteProductRowsV2(productId);
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_product WHERE number=? AND companyid=?")) {
                statement.setObject(1, product.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEPRODUCT", "TBL_PRODUCT", product.getNumber());
        } catch (SQLException e) {
            throw handleFailure("delete product '" + product.getNumber() + "'", e);
        }
    }

    private void bindProductV2(PreparedStatement statement, SSProduct product, Integer companyId) throws SQLException {
        int i = 1;
        statement.setObject(i++, product.getNumber());
        statement.setObject(i++, companyId);
        statement.setObject(i++, product.getDescription());
        statement.setObject(i++, product.getSellingPrice());
        statement.setObject(i++, product.getTaxCode() == null ? null : product.getTaxCode().name());
        statement.setObject(i++, product.getWarehouseLocation());
        statement.setObject(i++, product.getOrderpoint());
        statement.setObject(i++, product.getOrdercount());
        statement.setObject(i++, product.getPurchasePrice());
        statement.setObject(i++, product.getStockPrice());
        statement.setObject(i++, product.getUnitFreight());
        statement.setObject(i++, product.getSupplierNr());
        statement.setObject(i++, product.getSupplierProductNr());
        statement.setObject(i++, product.isExpired());
        statement.setObject(i++, product.isStockProduct());
        statement.setObject(i++, product.isOnlyWholeQuantity());
        statement.setObject(i++, product.getUnit() == null ? null : product.getUnit().getName());
        statement.setObject(i++, product.getWeight());
        statement.setObject(i++, product.getVolume());
        statement.setObject(i, product.getProjectNr());
    }

    private Integer getProductIdV2(String productNumber, Integer companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_product WHERE number=? AND companyid=?")) {
            statement.setObject(1, productNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private void deleteProductAccountsV2(Integer productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tbl_product_account WHERE product_id=?")) {
            statement.setObject(1, productId);
            statement.executeUpdate();
        }
    }

    private void replaceProductAccountsV2(Integer productId, SSProduct product) throws SQLException {
        deleteProductAccountsV2(productId);

        for (SSDefaultAccount defaultAccount : SSDefaultAccount.values()) {
            Integer account = product.getDefaultAccount(defaultAccount, null);
            if (account == null) {
                continue;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_product_account(product_id,account_type,account_nr) VALUES(?,?,?)")) {
                statement.setObject(1, productId);
                statement.setObject(2, defaultAccount.name());
                statement.setObject(3, account);
                statement.executeUpdate();
            }
        }
    }

    private void deleteProductRowsV2(Integer productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tbl_product_row WHERE product_id=?")) {
            statement.setObject(1, productId);
            statement.executeUpdate();
        }
    }

    private void replaceProductRowsV2(Integer productId, SSProduct product) throws SQLException {
        deleteProductRowsV2(productId);

        List<SSProductRow> rows = product.getParcelRows();
        if (rows == null) {
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            SSProductRow row = rows.get(i);
            if (row == null) {
                continue;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_product_row(product_id,row_index,product_nr,description,quantity) VALUES(?,?,?,?,?)")) {
                statement.setObject(1, productId);
                statement.setObject(2, i);
                statement.setObject(3, row.getProductNr());
                statement.setObject(4, row.getDescription());
                statement.setObject(5, row.getQuantity());
                statement.executeUpdate();
            }
        }
    }

    private SSProduct mapProductV2(ResultSet resultSet) throws SQLException {
        SSProduct product = new SSProduct();

        product.setNumber(resultSet.getString("number"));
        product.setDescription(resultSet.getString("description"));
        product.setSellingPrice(resultSet.getBigDecimal("unitprice"));

        String taxCode = resultSet.getString("tax_code");
        if (taxCode != null) {
            try {
                product.setTaxCode(SSTaxCode.valueOf(taxCode));
            } catch (IllegalArgumentException ignored) {
                // Keep null tax code if database contains unknown enum value.
            }
        }

        product.setWarehouseLocation(resultSet.getString("warehouse_location"));
        product.setOrderpoint((Integer) resultSet.getObject("orderpoint"));
        product.setOrdercount((Integer) resultSet.getObject("ordercount"));
        product.setPurchasePrice(resultSet.getBigDecimal("purchase_price"));
        product.setStockPrice(resultSet.getBigDecimal("stock_price"));
        product.setUnitFreight(resultSet.getBigDecimal("freight"));
        product.setSupplierNr(resultSet.getString("supplier_nr"));
        product.setSupplierProductNr(resultSet.getString("supplier_product_nr"));
        product.setExpired(resultSet.getBoolean("expired"));
        product.setStockProduct(resultSet.getBoolean("stock_goods"));
        product.setOnlyWholeQuantity(resultSet.getBoolean("only_whole_quantity"));

        String unit = resultSet.getString("unit");
        if (unit != null) {
            product.setUnit(new SSUnit(unit, unit));
        }

        product.setWeight(resultSet.getBigDecimal("weight"));
        product.setVolume(resultSet.getBigDecimal("volume"));
        product.setProjectNr(resultSet.getString("project_number"));

        Integer productId = resultSet.getInt("id");
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_type,account_nr FROM tbl_product_account WHERE product_id=?")) {
            statement.setObject(1, productId);
            try (ResultSet accounts = statement.executeQuery()) {
                while (accounts.next()) {
                    try {
                        SSDefaultAccount defaultAccount = SSDefaultAccount.valueOf(accounts.getString("account_type"));
                        product.setDefaultAccount(defaultAccount, accounts.getInt("account_nr"));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore unknown account type values.
                    }
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT product_nr,description,quantity FROM tbl_product_row WHERE product_id=? ORDER BY row_index")) {
            statement.setObject(1, productId);
            try (ResultSet rows = statement.executeQuery()) {
                List<SSProductRow> productRows = new LinkedList<>();
                while (rows.next()) {
                    SSProductRow row = new SSProductRow();
                    row.setProduct(rows.getString("product_nr"));
                    row.setDescription(rows.getString("description"));
                    row.setQuantity((Integer) rows.getObject("quantity"));
                    productRows.add(row);
                }
                product.setParcelRows(productRows);
            }
        }

        return product;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback product transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
