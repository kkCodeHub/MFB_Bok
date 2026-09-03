package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
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
 * V2 periodic-invoice repository backed by the normalized V2 schema.
 */
public class V2PeriodicInvoiceRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2PeriodicInvoiceRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    public V2PeriodicInvoiceRepository(Connection connection,
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

    public List<SSPeriodicInvoice> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSPeriodicInvoice> periodicInvoices = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_periodicinvoice WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            periodicInvoices.add(mapPeriodicInvoiceV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return periodicInvoices;
        } catch (SQLException e) {
            throw handleFailure("load periodic invoices", e);
        }
    }

    public Optional<SSPeriodicInvoice> findByPeriodicInvoice(SSPeriodicInvoice periodicInvoice) {
        if (periodicInvoice == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_periodicinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, periodicInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapPeriodicInvoiceV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find periodic invoice '" + periodicInvoice.getNumber() + "'", e);
        }
    }

    public void add(SSPeriodicInvoice periodicInvoice) {
        if (periodicInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("periodicinvoice");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_periodicinvoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            periodicInvoice.setNumber(number + 1);
                        } else {
                            periodicInvoice.setNumber(companyNumber + 1);
                        }
                    } else {
                        periodicInvoice.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer periodicInvoiceId = null;
            String placeholders = String.join(",", Collections.nCopies(39, "?"));
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_periodicinvoice(" +
                            "number,companyid,vdate,count,period,description,period_start,period_end," +
                            "append_period,append_information,information,customer_nr,customer_name," +
                            "our_contact,your_contact,delay_interest,currency_code,payment_term," +
                            "delivery_term,delivery_way,tax_free,sale_text,printed,currency_rate," +
                            "payment_day,your_order_number,stock_influencing,inv_addr_name,inv_addr_address," +
                            "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                            "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                            "VALUES(" + placeholders + ")",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, periodicInvoice.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindPeriodicInvoiceColumnsV2(insertStatement, i, periodicInvoice);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        periodicInvoiceId = keys.getInt(1);
                    }
                }
            }
            if (periodicInvoiceId == null) {
                periodicInvoiceId = getPeriodicInvoiceIdV2(periodicInvoice.getNumber(), currentCompany.getId());
            }
            if (periodicInvoiceId != null) {
                replacePeriodicInvoiceRowsV2(periodicInvoiceId, periodicInvoice);
                replacePeriodicInvoiceAddedV2(periodicInvoiceId, periodicInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWPERIODICINVOICE", "TBL_PERIODICINVOICE",
                    String.valueOf(periodicInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add periodic invoice '" + periodicInvoice.getNumber() + "'", e);
        }
    }

    public void update(SSPeriodicInvoice periodicInvoice) {
        if (periodicInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_periodicinvoice SET " +
                            "vdate=?,count=?,period=?,description=?,period_start=?,period_end=?," +
                            "append_period=?,append_information=?,information=?,customer_nr=?,customer_name=?," +
                            "our_contact=?,your_contact=?,delay_interest=?,currency_code=?,payment_term=?," +
                            "delivery_term=?,delivery_way=?,tax_free=?,sale_text=?,printed=?,currency_rate=?," +
                            "payment_day=?,your_order_number=?,stock_influencing=?,inv_addr_name=?," +
                            "inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?,inv_addr_city=?," +
                            "inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                            "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindPeriodicInvoiceColumnsV2(statement, 1, periodicInvoice);
                statement.setObject(i++, periodicInvoice.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer periodicInvoiceId = getPeriodicInvoiceIdV2(periodicInvoice.getNumber(), currentCompany.getId());
            if (periodicInvoiceId != null) {
                replacePeriodicInvoiceRowsV2(periodicInvoiceId, periodicInvoice);
                replacePeriodicInvoiceAddedV2(periodicInvoiceId, periodicInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITPERIODICINVOICE", "TBL_PERIODICINVOICE",
                    String.valueOf(periodicInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update periodic invoice '" + periodicInvoice.getNumber() + "'", e);
        }
    }

    public void delete(SSPeriodicInvoice periodicInvoice) {
        if (periodicInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer periodicInvoiceId = getPeriodicInvoiceIdV2(periodicInvoice.getNumber(), currentCompany.getId());
            if (periodicInvoiceId != null) {
                try (PreparedStatement deleteAdded = connection.prepareStatement(
                        "DELETE FROM tbl_periodicinvoice_added WHERE periodicinvoice_id=?")) {
                    deleteAdded.setObject(1, periodicInvoiceId);
                    deleteAdded.executeUpdate();
                }

                try (PreparedStatement clearInvoices = connection.prepareStatement(
                        "UPDATE tbl_invoice SET periodicinvoice_id=NULL WHERE periodicinvoice_id=?")) {
                    clearInvoices.setObject(1, periodicInvoiceId);
                    clearInvoices.executeUpdate();
                }

                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=?")) {
                    deleteRows.setObject(1, periodicInvoiceId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_periodicinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, periodicInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEPERIODICINVOICE", "TBL_PERIODICINVOICE",
                    String.valueOf(periodicInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete periodic invoice '" + periodicInvoice.getNumber() + "'", e);
        }
    }

    private List<SSSaleRow> getPeriodicInvoiceRowsV2(Integer periodicInvoiceId) throws SQLException {
        List<SSSaleRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=? ORDER BY id")) {
            statement.setObject(1, periodicInvoiceId);
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

    private void replacePeriodicInvoiceRowsV2(Integer periodicInvoiceId, SSPeriodicInvoice periodicInvoice)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_periodicinvoice_row WHERE periodicinvoice_id=?")) {
            delete.setObject(1, periodicInvoiceId);
            delete.executeUpdate();
        }

        for (SSSaleRow row : periodicInvoice.getTemplate().getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_periodicinvoice_row(periodicinvoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, periodicInvoiceId);
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

    private void replacePeriodicInvoiceAddedV2(Integer periodicInvoiceId, SSPeriodicInvoice periodicInvoice)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_periodicinvoice_added WHERE periodicinvoice_id=?")) {
            delete.setObject(1, periodicInvoiceId);
            delete.executeUpdate();
        }

        for (SSInvoice invoice : periodicInvoice.getInvoices()) {
            Integer invoiceNr = invoice.getNumber();
            boolean isAdded = periodicInvoice.isAdded(invoice);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_periodicinvoice_added(periodicinvoice_id, invoice_nr, added) VALUES(?,?,?)")) {
                insert.setObject(1, periodicInvoiceId);
                insert.setObject(2, invoiceNr);
                insert.setObject(3, isAdded);
                insert.executeUpdate();
            }
        }
    }

    private void loadPeriodicInvoiceAddedV2(Integer periodicInvoiceId, SSPeriodicInvoice periodicInvoice)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT invoice_nr, added FROM tbl_periodicinvoice_added WHERE periodicinvoice_id=?")) {
            statement.setObject(1, periodicInvoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int invoiceNr = resultSet.getInt("invoice_nr");
                    boolean isAdded = resultSet.getBoolean("added");
                    for (SSInvoice invoice : periodicInvoice.getInvoices()) {
                        if (invoice.getNumber() != null && invoiceNr == invoice.getNumber()) {
                            if (isAdded) {
                                periodicInvoice.setAdded(invoice);
                            } else {
                                periodicInvoice.setNotAdded(invoice);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private Integer getPeriodicInvoiceIdV2(Integer periodicInvoiceNumber, Integer companyId) throws SQLException {
        if (periodicInvoiceNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_periodicinvoice WHERE number=? AND companyid=?")) {
            statement.setObject(1, periodicInvoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindPeriodicInvoiceColumnsV2(PreparedStatement statement,
                                             int index,
                                             SSPeriodicInvoice periodicInvoice) throws SQLException {
        SSInvoice template = periodicInvoice.getTemplate();

        bindLocalDateV2(statement, index++, periodicInvoice.getLocalDate());
        statement.setObject(index++, periodicInvoice.getCount());
        statement.setObject(index++, periodicInvoice.getPeriod());
        statement.setObject(index++, periodicInvoice.getDescription());
        bindLocalDateV2(statement, index++, periodicInvoice.getLocalPeriodStart());
        bindLocalDateV2(statement, index++, periodicInvoice.getLocalPeriodEnd());
        statement.setObject(index++, periodicInvoice.getAppendPeriod());
        statement.setObject(index++, periodicInvoice.isAppendInformation());
        statement.setObject(index++, periodicInvoice.getInformation());

        statement.setObject(index++, template.getCustomerNr());
        statement.setObject(index++, template.getCustomerName());
        statement.setObject(index++, template.getOurContactPerson());
        statement.setObject(index++, template.getYourContactPerson());
        statement.setObject(index++, template.getDelayInterest());
        statement.setObject(index++, getCurrencyCodeV2(template.getCurrency()));
        statement.setObject(index++, template.getPaymentTerm() == null ? null : template.getPaymentTerm().getName());
        statement.setObject(index++, template.getDeliveryTerm() == null ? null : template.getDeliveryTerm().getName());
        statement.setObject(index++, template.getDeliveryWay() == null ? null : template.getDeliveryWay().getName());
        statement.setObject(index++, template.getTaxFree());
        statement.setObject(index++, template.getText());
        statement.setObject(index++, template.isPrinted());
        statement.setObject(index++, template.getCurrencyRate());
        bindLocalDateV2(statement, index++, template.getLocalDueDate());
        statement.setObject(index++, template.getYourOrderNumber());
        statement.setObject(index++, template.isStockInfluencing());
        index = V2RepositoryHelpers.bindAddress(statement, index, template.getInvoiceAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, template.getDeliveryAddress());
    }

    private SSPeriodicInvoice mapPeriodicInvoiceV2(ResultSet resultSet) throws SQLException {
        SSPeriodicInvoice periodicInvoice = new SSPeriodicInvoice() {
            @Override
            public void doAutoIncrecement() {
                // Avoid recursive repository calls during DB row mapping.
            }
        };
        periodicInvoice.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            periodicInvoice.setLocalDate(date.toLocalDate());
        }

        periodicInvoice.setCount((Integer) resultSet.getObject("count"));
        periodicInvoice.setPeriod((Integer) resultSet.getObject("period"));
        periodicInvoice.setDescription(resultSet.getString("description"));

        Date periodStart = resultSet.getDate("period_start");
        if (periodStart != null) {
            periodicInvoice.setLocalPeriodStart(periodStart.toLocalDate());
        }

        Date periodEnd = resultSet.getDate("period_end");
        if (periodEnd != null) {
            periodicInvoice.setLocalPeriodEnd(periodEnd.toLocalDate());
        }

        periodicInvoice.setAppendPeriod(resultSet.getBoolean("append_period"));
        periodicInvoice.setAppendInformation(resultSet.getBoolean("append_information"));
        periodicInvoice.setInformation(resultSet.getString("information"));

        SSInvoice template = periodicInvoice.getTemplate();
        template.setCustomerNr(resultSet.getString("customer_nr"));
        template.setCustomerName(resultSet.getString("customer_name"));
        template.setOurContactPerson(resultSet.getString("our_contact"));
        template.setYourContactPerson(resultSet.getString("your_contact"));
        template.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        template.setTaxFree(resultSet.getBoolean("tax_free"));
        template.setText(resultSet.getString("sale_text"));
        template.setPrinted(resultSet.getBoolean("printed"));
        template.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
        template.setYourOrderNumber(resultSet.getString("your_order_number"));
        template.setStockInfluencing(resultSet.getBoolean("stock_influencing"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            template.setCurrency(new SSCurrency(currencyCode, currencyCode));
        } else {
            template.setCurrency(null);
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            template.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        } else {
            template.setPaymentTerm(null);
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            template.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        } else {
            template.setDeliveryTerm(null);
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            template.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        } else {
            template.setDeliveryWay(null);
        }

        Date paymentDay = resultSet.getDate("payment_day");
        if (paymentDay != null) {
            template.setLocalDueDate(paymentDay.toLocalDate());
        }

        template.setInvoiceAddress(V2RepositoryHelpers.mapAddress(resultSet, "inv_addr"));
        template.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));

        Integer periodicInvoiceId = resultSet.getInt("id");
        template.getRows().clear();
        template.getRows().addAll(getPeriodicInvoiceRowsV2(periodicInvoiceId));

        periodicInvoice.createInvoices();
        loadPeriodicInvoiceAddedV2(periodicInvoiceId, periodicInvoice);

        return periodicInvoice;
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
            LOG.error("Failed to rollback periodic invoice transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
