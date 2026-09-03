package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSTender;
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
 * V2 tender repository backed by the normalized V2 schema.
 */
public class V2TenderRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2TenderRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 tender repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2TenderRepository(Connection connection,
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

    public List<SSTender> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSTender> tenders = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_tender WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            tenders.add(mapTenderV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return tenders;
        } catch (SQLException e) {
            throw handleFailure("load tenders", e);
        }
    }

    public Optional<SSTender> findByTender(SSTender tender) {
        if (tender == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_tender WHERE number=? AND companyid=?")) {
                statement.setObject(1, tender.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapTenderV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find tender '" + tender.getNumber() + "'", e);
        }
    }

    public List<SSTender> findAll(List<SSTender> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSTender> tenders = new LinkedList<>();
            for (SSTender tender : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_tender WHERE number=? AND companyid=?")) {
                    statement.setObject(1, tender.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            tenders.add(mapTenderV2(resultSet));
                        }
                    }
                }
            }
            return tenders;
        } catch (SQLException e) {
            throw handleFailure("filter tenders", e);
        }
    }

    public void add(SSTender tender) {
        if (tender == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("tender");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_tender WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            tender.setNumber(number + 1);
                        } else {
                            tender.setNumber(companyNumber + 1);
                        }
                    } else {
                        tender.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer tenderId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_tender(" +
                            "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                            "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                            "sale_text,eu_sale_commodity,eu_sale_third_part,printed,expires,order_nr," +
                            "currency_rate,inv_addr_name,inv_addr_address,inv_addr_street,inv_addr_zipcode," +
                            "inv_addr_city,inv_addr_country,del_addr_name,del_addr_address,del_addr_street," +
                            "del_addr_zipcode,del_addr_city,del_addr_country) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, tender.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindTenderColumnsV2(insertStatement, i, tender);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        tenderId = keys.getInt(1);
                    }
                }
            }

            if (tenderId == null) {
                tenderId = getTenderIdV2(tender.getNumber(), currentCompany.getId());
            }
            if (tenderId != null) {
                replaceTenderRowsV2(tenderId, tender);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWTENDER", "TBL_TENDER", String.valueOf(tender.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add tender '" + tender.getNumber() + "'", e);
        }
    }

    public void update(SSTender tender) {
        if (tender == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_tender SET " +
                            "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                            "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                            "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                            "expires=?,order_nr=?,currency_rate=?,inv_addr_name=?,inv_addr_address=?," +
                            "inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?,inv_addr_country=?," +
                            "del_addr_name=?,del_addr_address=?,del_addr_street=?,del_addr_zipcode=?," +
                            "del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindTenderColumnsV2(statement, 1, tender);
                statement.setObject(i++, tender.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer tenderId = getTenderIdV2(tender.getNumber(), currentCompany.getId());
            if (tenderId != null) {
                replaceTenderRowsV2(tenderId, tender);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITTENDER", "TBL_TENDER", String.valueOf(tender.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update tender '" + tender.getNumber() + "'", e);
        }
    }

    public void delete(SSTender tender) {
        if (tender == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer tenderId = getTenderIdV2(tender.getNumber(), currentCompany.getId());
            if (tenderId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_tender_row WHERE tender_id=?")) {
                    deleteRows.setObject(1, tenderId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_tender WHERE number=? AND companyid=?")) {
                statement.setObject(1, tender.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETETENDER", "TBL_TENDER", String.valueOf(tender.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete tender '" + tender.getNumber() + "'", e);
        }
    }

    private List<SSSaleRow> getTenderRowsV2(Integer tenderId) throws SQLException {
        List<SSSaleRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_tender_row WHERE tender_id=? ORDER BY id")) {
            statement.setObject(1, tenderId);
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

    private void replaceTenderRowsV2(Integer tenderId, SSTender tender) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_tender_row WHERE tender_id=?")) {
            delete.setObject(1, tenderId);
            delete.executeUpdate();
        }

        for (SSSaleRow row : tender.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_tender_row(tender_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, tenderId);
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

    private Integer getTenderIdV2(Integer tenderNumber, Integer companyId) throws SQLException {
        if (tenderNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_tender WHERE number=? AND companyid=?")) {
            statement.setObject(1, tenderNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindTenderColumnsV2(PreparedStatement statement, int index, SSTender tender) throws SQLException {
        bindLocalDateV2(statement, index++, tender.getLocalDate());
        statement.setObject(index++, tender.getCustomerNr());
        statement.setObject(index++, tender.getCustomerName());
        statement.setObject(index++, tender.getOurContactPerson());
        statement.setObject(index++, tender.getYourContactPerson());
        statement.setObject(index++, tender.getDelayInterest());
        statement.setObject(index++, getCurrencyCodeV2(tender.getCurrency()));
        statement.setObject(index++, tender.getPaymentTerm() == null ? null : tender.getPaymentTerm().getName());
        statement.setObject(index++, tender.getDeliveryTerm() == null ? null : tender.getDeliveryTerm().getName());
        statement.setObject(index++, tender.getDeliveryWay() == null ? null : tender.getDeliveryWay().getName());
        statement.setObject(index++, tender.getTaxFree());
        statement.setObject(index++, tender.getText());
        statement.setObject(index++, tender.getEuSaleCommodity());
        statement.setObject(index++, tender.getEuSaleThirdPartCommodity());
        statement.setObject(index++, tender.isPrinted());
        bindLocalDateV2(statement, index++, tender.getLocalExpires());
        statement.setObject(index++, tender.getOrderNr());
        statement.setObject(index++, tender.getCurrencyRate());
        index = V2RepositoryHelpers.bindAddress(statement, index, tender.getInvoiceAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, tender.getDeliveryAddress());
    }

    private SSTender mapTenderV2(ResultSet resultSet) throws SQLException {
        SSTender tender = new SSTender();
        tender.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            tender.setLocalDate(date.toLocalDate());
        }

        tender.setCustomerNr(resultSet.getString("customer_nr"));
        tender.setCustomerName(resultSet.getString("customer_name"));
        tender.setOurContactPerson(resultSet.getString("our_contact"));
        tender.setYourContactPerson(resultSet.getString("your_contact"));
        tender.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        tender.setTaxFree(resultSet.getBoolean("tax_free"));
        tender.setText(resultSet.getString("sale_text"));
        tender.setEuSaleCommodity(resultSet.getBoolean("eu_sale_commodity"));
        tender.setEuSaleYhirdPartCommodity(resultSet.getBoolean("eu_sale_third_part"));
        tender.setPrinted(resultSet.getBoolean("printed"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            tender.setCurrency(new SSCurrency(currencyCode, currencyCode));
        } else {
            tender.setCurrency(null);
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            tender.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        } else {
            tender.setPaymentTerm(null);
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            tender.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        } else {
            tender.setDeliveryTerm(null);
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            tender.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        } else {
            tender.setDeliveryWay(null);
        }

        Date expires = resultSet.getDate("expires");
        if (expires != null) {
            tender.setLocalExpires(expires.toLocalDate());
        }

        Integer orderNr = (Integer) resultSet.getObject("order_nr");
        if (orderNr != null) {
            SSOrder order = new SSOrder();
            order.setNumber(orderNr);
            tender.setOrder(order);
        }

        tender.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
        tender.setInvoiceAddress(V2RepositoryHelpers.mapAddress(resultSet, "inv_addr"));
        tender.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));

        tender.getRows().clear();
        tender.getRows().addAll(getTenderRowsV2(resultSet.getInt("id")));
        return tender;
    }

    private void bindLocalDateV2(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        statement.setDate(index, value == null ? null : Date.valueOf(value));
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
            LOG.error("Failed to rollback tender transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}

