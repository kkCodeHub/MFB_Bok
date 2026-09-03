package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSPurchaseOrderRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 purchase-order repository backed by the normalized V2 schema.
 */
public class V2PurchaseOrderRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2PurchaseOrderRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    public V2PurchaseOrderRepository(Connection connection,
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

    public List<SSPurchaseOrder> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSPurchaseOrder> purchaseOrders = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_purchaseorder WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            purchaseOrders.add(mapPurchaseOrderV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return purchaseOrders;
        } catch (SQLException e) {
            throw handleFailure("load purchase orders", e);
        }
    }

    public Optional<SSPurchaseOrder> findByPurchaseOrder(SSPurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_purchaseorder WHERE number=? AND companyid=?")) {
                statement.setObject(1, purchaseOrder.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapPurchaseOrderV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find purchase order '" + purchaseOrder.getNumber() + "'", e);
        }
    }

    public List<SSPurchaseOrder> findAll(List<SSPurchaseOrder> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSPurchaseOrder> purchaseOrders = new LinkedList<>();
            for (SSPurchaseOrder purchaseOrder : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_purchaseorder WHERE number=? AND companyid=?")) {
                    statement.setObject(1, purchaseOrder.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            purchaseOrders.add(mapPurchaseOrderV2(resultSet));
                        }
                    }
                }
            }
            return purchaseOrders;
        } catch (SQLException e) {
            throw handleFailure("filter purchase orders", e);
        }
    }

    public void add(SSPurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("purchaseorder");

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_purchaseorder WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            purchaseOrder.setNumber(number + 1);
                        } else {
                            purchaseOrder.setNumber(companyNumber + 1);
                        }
                    } else {
                        purchaseOrder.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer purchaseOrderId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_purchaseorder(" +
                            "number,companyid,invoice_nr,vdate,supplier_nr,supplier_name,estimated_delivery," +
                            "payment_term,delivery_term,delivery_way,our_contact,your_contact,currency_code," +
                            "currency_rate,sale_text,printed,stock_influencing,del_addr_name,del_addr_address," +
                            "del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country,supp_addr_name," +
                            "supp_addr_address,supp_addr_street,supp_addr_zipcode,supp_addr_city,supp_addr_country) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, purchaseOrder.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindPurchaseOrderColumnsV2(insertStatement, i, purchaseOrder);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        purchaseOrderId = keys.getInt(1);
                    }
                }
            }

            if (purchaseOrderId == null) {
                purchaseOrderId = getPurchaseOrderIdV2(purchaseOrder.getNumber(), currentCompany.getId());
            }
            if (purchaseOrderId != null) {
                replacePurchaseOrderRowsV2(purchaseOrderId, purchaseOrder);
                replacePurchaseOrderDefaultAccountsV2(purchaseOrderId, purchaseOrder);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWPURCHASEORDER", "TBL_PURCHASEORDER",
                    String.valueOf(purchaseOrder.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add purchase order '" + purchaseOrder.getNumber() + "'", e);
        }
    }

    public void update(SSPurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_purchaseorder SET " +
                            "invoice_nr=?,vdate=?,supplier_nr=?,supplier_name=?,estimated_delivery=?," +
                            "payment_term=?,delivery_term=?,delivery_way=?,our_contact=?,your_contact=?," +
                            "currency_code=?,currency_rate=?,sale_text=?,printed=?,stock_influencing=?," +
                            "del_addr_name=?,del_addr_address=?,del_addr_street=?,del_addr_zipcode=?,del_addr_city=?," +
                            "del_addr_country=?,supp_addr_name=?,supp_addr_address=?,supp_addr_street=?,supp_addr_zipcode=?," +
                            "supp_addr_city=?,supp_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindPurchaseOrderColumnsV2(statement, 1, purchaseOrder);
                statement.setObject(i++, purchaseOrder.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer purchaseOrderId = getPurchaseOrderIdV2(purchaseOrder.getNumber(), currentCompany.getId());
            if (purchaseOrderId != null) {
                replacePurchaseOrderRowsV2(purchaseOrderId, purchaseOrder);
                replacePurchaseOrderDefaultAccountsV2(purchaseOrderId, purchaseOrder);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITPURCHASEORDER", "TBL_PURCHASEORDER",
                    String.valueOf(purchaseOrder.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update purchase order '" + purchaseOrder.getNumber() + "'", e);
        }
    }

    public void delete(SSPurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer purchaseOrderId = getPurchaseOrderIdV2(purchaseOrder.getNumber(), currentCompany.getId());
            if (purchaseOrderId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_purchaseorder_row WHERE purchaseorder_id=?")) {
                    deleteRows.setObject(1, purchaseOrderId);
                    deleteRows.executeUpdate();
                }

                try (PreparedStatement deleteDefaults = connection.prepareStatement(
                        "DELETE FROM tbl_purchaseorder_account WHERE purchaseorder_id=?")) {
                    deleteDefaults.setObject(1, purchaseOrderId);
                    deleteDefaults.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_purchaseorder WHERE number=? AND companyid=?")) {
                statement.setObject(1, purchaseOrder.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEPURCHASEORDER", "TBL_PURCHASEORDER",
                    String.valueOf(purchaseOrder.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete purchase order '" + purchaseOrder.getNumber() + "'", e);
        }
    }

    private List<SSPurchaseOrderRow> getPurchaseOrderRowsV2(Integer purchaseOrderId) throws SQLException {
        List<SSPurchaseOrderRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_purchaseorder_row WHERE purchaseorder_id=? ORDER BY id")) {
            statement.setObject(1, purchaseOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSPurchaseOrderRow row = new SSPurchaseOrderRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setDescription(resultSet.getString("description"));
                    row.setSupplierArticleNr(resultSet.getString("supplier_article_nr"));
                    row.setUnitPrice(resultSet.getBigDecimal("unitprice"));
                    row.setQuantity((Integer) resultSet.getObject("quantity"));

                    String unit = resultSet.getString("unit");
                    if (unit != null) {
                        row.setUnit(new SSUnit(unit, unit));
                    }

                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replacePurchaseOrderRowsV2(Integer purchaseOrderId, SSPurchaseOrder purchaseOrder)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_purchaseorder_row WHERE purchaseorder_id=?")) {
            delete.setObject(1, purchaseOrderId);
            delete.executeUpdate();
        }

        for (SSPurchaseOrderRow row : purchaseOrder.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_purchaseorder_row(purchaseorder_id,product_nr,description,supplier_article_nr,unitprice,quantity,unit,account_nr) VALUES(?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, purchaseOrderId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getDescription());
                insert.setObject(4, row.getSupplierArticleNr());
                insert.setObject(5, row.getUnitPrice());
                insert.setObject(6, row.getQuantity());
                insert.setObject(7, row.getUnit() == null ? null : row.getUnit().getName());
                insert.setObject(8, row.getAccountNr());
                insert.executeUpdate();
            }
        }
    }

    private Map<SSDefaultAccount, Integer> getPurchaseOrderDefaultAccountsV2(Integer purchaseOrderId)
            throws SQLException {
        Map<SSDefaultAccount, Integer> map = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_type,account_nr FROM tbl_purchaseorder_account WHERE purchaseorder_id=?")) {
            statement.setObject(1, purchaseOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("account_type");
                    Integer accountNr = (Integer) resultSet.getObject("account_nr");
                    if (type == null || accountNr == null) {
                        continue;
                    }
                    try {
                        map.put(SSDefaultAccount.valueOf(type), accountNr);
                    } catch (IllegalArgumentException ignored) {
                        // Ignore unknown enum values from partial migrations.
                    }
                }
            }
        }
        return map;
    }

    private void replacePurchaseOrderDefaultAccountsV2(Integer purchaseOrderId, SSPurchaseOrder purchaseOrder)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_purchaseorder_account WHERE purchaseorder_id=?")) {
            delete.setObject(1, purchaseOrderId);
            delete.executeUpdate();
        }

        Map<SSDefaultAccount, Integer> defaults = purchaseOrder.getDefaultAccounts();
        if (defaults == null || defaults.isEmpty()) {
            return;
        }

        for (Map.Entry<SSDefaultAccount, Integer> entry : defaults.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_purchaseorder_account(purchaseorder_id,account_type,account_nr) VALUES(?,?,?)")) {
                insert.setObject(1, purchaseOrderId);
                insert.setObject(2, entry.getKey().name());
                insert.setObject(3, entry.getValue());
                insert.executeUpdate();
            }
        }
    }

    private Integer getPurchaseOrderIdV2(Integer purchaseOrderNumber, Integer companyId) throws SQLException {
        if (purchaseOrderNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_purchaseorder WHERE number=? AND companyid=?")) {
            statement.setObject(1, purchaseOrderNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindPurchaseOrderColumnsV2(PreparedStatement statement,
                                           int index,
                                           SSPurchaseOrder purchaseOrder) throws SQLException {
        statement.setObject(index++, purchaseOrder.getInvoiceNr());
        bindLocalDateV2(statement, index++, purchaseOrder.getLocalDate());
        statement.setObject(index++, purchaseOrder.getSupplierNr());
        statement.setObject(index++, purchaseOrder.getSupplierName());
        bindLocalDateV2(statement, index++, purchaseOrder.getLocalEstimatedDelivery());
        statement.setObject(index++, purchaseOrder.getPaymentTerm() == null
                ? null
                : purchaseOrder.getPaymentTerm().getName());
        statement.setObject(index++, purchaseOrder.getDeliveryTerm() == null
                ? null
                : purchaseOrder.getDeliveryTerm().getName());
        statement.setObject(index++, purchaseOrder.getDeliveryWay() == null
                ? null
                : purchaseOrder.getDeliveryWay().getName());
        statement.setObject(index++, purchaseOrder.getOurContact());
        statement.setObject(index++, purchaseOrder.getYourContact());
        statement.setObject(index++, getCurrencyCodeV2(purchaseOrder.getCurrency()));
        statement.setObject(index++, purchaseOrder.getCurrencyRate());
        statement.setObject(index++, purchaseOrder.getText());
        statement.setObject(index++, purchaseOrder.isPrinted());
        statement.setObject(index++, purchaseOrder.isStockInfluencing());
        index = V2RepositoryHelpers.bindAddress(statement, index, purchaseOrder.getDeliveryAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, purchaseOrder.getSupplierAddress());
    }

    private SSPurchaseOrder mapPurchaseOrderV2(ResultSet resultSet) throws SQLException {
        SSPurchaseOrder purchaseOrder = new SSPurchaseOrder();

        purchaseOrder.setNumber((Integer) resultSet.getObject("number"));
        purchaseOrder.setInvoiceNr((Integer) resultSet.getObject("invoice_nr"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            purchaseOrder.setLocalDate(date.toLocalDate());
        }

        purchaseOrder.setSupplierNr(resultSet.getString("supplier_nr"));
        purchaseOrder.setSupplierName(resultSet.getString("supplier_name"));

        Date estimatedDelivery = resultSet.getDate("estimated_delivery");
        if (estimatedDelivery != null) {
            purchaseOrder.setLocalEstimatedDelivery(estimatedDelivery.toLocalDate());
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            purchaseOrder.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            purchaseOrder.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            purchaseOrder.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }

        purchaseOrder.setOurContact(resultSet.getString("our_contact"));
        purchaseOrder.setYourContact(resultSet.getString("your_contact"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            purchaseOrder.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        purchaseOrder.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
        purchaseOrder.setText(resultSet.getString("sale_text"));
        purchaseOrder.setPrinted(resultSet.getBoolean("printed"));
        purchaseOrder.setStockInfluencing(resultSet.getBoolean("stock_influencing"));
        purchaseOrder.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));
        purchaseOrder.setSupplierAddress(V2RepositoryHelpers.mapAddress(resultSet, "supp_addr"));

        Integer purchaseOrderId = resultSet.getInt("id");
        purchaseOrder.getRows().clear();
        purchaseOrder.getRows().addAll(getPurchaseOrderRowsV2(purchaseOrderId));
        purchaseOrder.setDefaultAccounts(getPurchaseOrderDefaultAccountsV2(purchaseOrderId));

        return purchaseOrder;
    }

    private String getCurrencyCodeV2(SSCurrency currency) {
        return currency == null ? null : currency.getName();
    }

    private void bindLocalDateV2(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        statement.setDate(index, date == null ? null : Date.valueOf(date));
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback purchase order transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
