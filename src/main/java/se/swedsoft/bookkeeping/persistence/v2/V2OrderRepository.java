package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 order repository backed by the normalized V2 schema.
 */
public class V2OrderRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2OrderRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 order repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2OrderRepository(Connection connection,
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

    public List<SSOrder> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSOrder> orders = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_order WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            orders.add(mapOrderV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return orders;
        } catch (SQLException e) {
            throw handleFailure("load orders", e);
        }
    }

    public Optional<SSOrder> findByOrder(SSOrder order) {
        if (order == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_order WHERE number=? AND companyid=?")) {
                statement.setObject(1, order.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapOrderV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find order '" + order.getNumber() + "'", e);
        }
    }

    public List<SSOrder> findAll(List<SSOrder> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSOrder> orders = new LinkedList<>();
            for (SSOrder order : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_order WHERE number=? AND companyid=?")) {
                    statement.setObject(1, order.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            orders.add(mapOrderV2(resultSet));
                        }
                    }
                }
            }
            return orders;
        } catch (SQLException e) {
            throw handleFailure("filter orders", e);
        }
    }

    public void add(SSOrder order) {
        if (order == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("order");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_order WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            order.setNumber(number + 1);
                        } else {
                            order.setNumber(companyNumber + 1);
                        }
                    } else {
                        order.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer orderId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_order(" +
                            "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                            "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                            "sale_text,eu_sale_commodity,eu_sale_third_part,printed,your_order_number," +
                            "estimated_delivery,invoice_nr,periodicinvoice_nr,purchaseorder_nr,hide_unitprice," +
                            "currency_rate,inv_addr_name,inv_addr_address,inv_addr_street,inv_addr_zipcode," +
                            "inv_addr_city,inv_addr_country,del_addr_name,del_addr_address,del_addr_street," +
                            "del_addr_zipcode,del_addr_city,del_addr_country) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, order.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindOrderColumnsV2(insertStatement, i, order);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        orderId = keys.getInt(1);
                    }
                }
            }

            if (orderId == null) {
                orderId = getOrderIdV2(order.getNumber(), currentCompany.getId());
            }
            if (orderId != null) {
                replaceOrderRowsV2(orderId, order);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWORDER", "TBL_ORDER", String.valueOf(order.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add order '" + order.getNumber() + "'", e);
        }
    }

    public void update(SSOrder order) {
        if (order == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_order SET " +
                            "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                            "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                            "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                            "your_order_number=?,estimated_delivery=?,invoice_nr=?,periodicinvoice_nr=?," +
                            "purchaseorder_nr=?,hide_unitprice=?,currency_rate=?,inv_addr_name=?," +
                            "inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?," +
                            "inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                            "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindOrderColumnsV2(statement, 1, order);
                statement.setObject(i++, order.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer orderId = getOrderIdV2(order.getNumber(), currentCompany.getId());
            if (orderId != null) {
                replaceOrderRowsV2(orderId, order);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITORDER", "TBL_ORDER", String.valueOf(order.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update order '" + order.getNumber() + "'", e);
        }
    }

    public void delete(SSOrder order) {
        if (order == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer orderId = getOrderIdV2(order.getNumber(), currentCompany.getId());
            if (orderId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_order_row WHERE order_id=?")) {
                    deleteRows.setObject(1, orderId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_order WHERE number=? AND companyid=?")) {
                statement.setObject(1, order.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEORDER", "TBL_ORDER", String.valueOf(order.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete order '" + order.getNumber() + "'", e);
        }
    }

    private List<SSSaleRow> getOrderRowsV2(Integer orderId) throws SQLException {
        List<SSSaleRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_order_row WHERE order_id=? ORDER BY id")) {
            statement.setObject(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSSaleRow row = new SSSaleRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setDescription(resultSet.getString("description"));
                    row.setUnitprice(resultSet.getBigDecimal("unitprice"));
                    row.setQuantity((Integer) resultSet.getObject("count"));

                    String unit = resultSet.getString("unit");
                    if (unit != null) {
                        row.setUnit(new SSUnit(unit, unit));
                    }

                    row.setDiscount(resultSet.getBigDecimal("discount"));

                    String taxCode = resultSet.getString("tax_code");
                    if (taxCode != null) {
                        try {
                            row.setTaxCode(SSTaxCode.valueOf(taxCode));
                        } catch (IllegalArgumentException ignored) {
                            // Ignore unknown enum values from partial migrations.
                        }
                    }

                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    row.setProjectNr(resultSet.getString("project_number"));
                    row.setResultUnitNr(resultSet.getString("result_unit_number"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceOrderRowsV2(Integer orderId, SSOrder order) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_order_row WHERE order_id=?")) {
            delete.setObject(1, orderId);
            delete.executeUpdate();
        }

        for (SSSaleRow row : order.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_order_row(order_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, orderId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getDescription());
                insert.setObject(4, row.getUnitprice());
                insert.setObject(5, row.getQuantity());
                insert.setObject(6, row.getUnit() == null ? null : row.getUnit().getName());
                insert.setObject(7, row.getDiscount());
                insert.setObject(8, row.getTaxCode() == null ? null : row.getTaxCode().name());
                insert.setObject(9, row.getAccountNr());
                insert.setObject(10, row.getProjectNr());
                insert.setObject(11, row.getResultUnitNr());
                insert.executeUpdate();
            }
        }
    }

    private Integer getOrderIdV2(Integer orderNumber, Integer companyId) throws SQLException {
        if (orderNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_order WHERE number=? AND companyid=?")) {
            statement.setObject(1, orderNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindOrderColumnsV2(PreparedStatement statement, int index, SSOrder order) throws SQLException {
        bindLocalDateV2(statement, index++, order.getLocalDate());
        statement.setObject(index++, order.getCustomerNr());
        statement.setObject(index++, order.getCustomerName());
        statement.setObject(index++, order.getOurContactPerson());
        statement.setObject(index++, order.getYourContactPerson());
        statement.setObject(index++, order.getDelayInterest());
        statement.setObject(index++, getCurrencyCodeV2(order.getCurrency()));
        statement.setObject(index++, order.getPaymentTerm() == null ? null : order.getPaymentTerm().getName());
        statement.setObject(index++, order.getDeliveryTerm() == null ? null : order.getDeliveryTerm().getName());
        statement.setObject(index++, order.getDeliveryWay() == null ? null : order.getDeliveryWay().getName());
        statement.setObject(index++, order.getTaxFree());
        statement.setObject(index++, order.getText());
        statement.setObject(index++, order.getEuSaleCommodity());
        statement.setObject(index++, order.getEuSaleThirdPartCommodity());
        statement.setObject(index++, order.isPrinted());
        statement.setObject(index++, order.getYourOrderNumber());
        statement.setObject(index++, order.getEstimatedDelivery());
        statement.setObject(index++, order.getInvoiceNr());
        statement.setObject(index++, order.getPeriodicInvoiceNr());
        statement.setObject(index++, order.getPurchaseOrderNr());
        statement.setObject(index++, order.getHideUnitprice());
        statement.setObject(index++, order.getCurrencyRate());
        index = V2RepositoryHelpers.bindAddress(statement, index, order.getInvoiceAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, order.getDeliveryAddress());
    }

    private SSOrder mapOrderV2(ResultSet resultSet) throws SQLException {
        SSOrder order = new SSOrder();

        order.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            order.setLocalDate(date.toLocalDate());
        }

        order.setCustomerNr(resultSet.getString("customer_nr"));
        order.setCustomerName(resultSet.getString("customer_name"));
        order.setOurContactPerson(resultSet.getString("our_contact"));
        order.setYourContactPerson(resultSet.getString("your_contact"));
        order.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        order.setTaxFree(resultSet.getBoolean("tax_free"));
        order.setText(resultSet.getString("sale_text"));
        order.setEuSaleCommodity(resultSet.getBoolean("eu_sale_commodity"));
        order.setEuSaleYhirdPartCommodity(resultSet.getBoolean("eu_sale_third_part"));
        order.setPrinted(resultSet.getBoolean("printed"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            order.setCurrency(new SSCurrency(currencyCode, currencyCode));
        } else {
            order.setCurrency(null);
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            order.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        } else {
            order.setPaymentTerm(null);
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            order.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        } else {
            order.setDeliveryTerm(null);
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            order.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        } else {
            order.setDeliveryWay(null);
        }

        order.setYourOrderNumber(resultSet.getString("your_order_number"));
        order.setEstimatedDelivery(resultSet.getString("estimated_delivery"));
        order.setInvoiceNr((Integer) resultSet.getObject("invoice_nr"));
        order.setPeriodicInvoiceNr((Integer) resultSet.getObject("periodicinvoice_nr"));
        Integer purchaseOrderNr = (Integer) resultSet.getObject("purchaseorder_nr");
        if (purchaseOrderNr != null) {
            SSPurchaseOrder purchaseOrder = new SSPurchaseOrder();
            purchaseOrder.setNumber(purchaseOrderNr);
            order.setPurchaseOrder(purchaseOrder);
        }
        order.setHideUnitprice(resultSet.getBoolean("hide_unitprice"));
        order.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));

        order.setInvoiceAddress(V2RepositoryHelpers.mapAddress(resultSet, "inv_addr"));
        order.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));

        order.getRows().clear();
        order.getRows().addAll(getOrderRowsV2(resultSet.getInt("id")));
        return order;
    }

    private void bindLocalDateV2(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        statement.setDate(index, date == null ? null : Date.valueOf(date));
    }

    private String getCurrencyCodeV2(SSCurrency currency) {
        try {
            return currency == null ? null : currency.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback order transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}

