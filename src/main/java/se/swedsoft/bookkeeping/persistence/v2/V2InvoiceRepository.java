package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
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
 * V2 invoice repository backed by the normalized V2 schema.
 */
public class V2InvoiceRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2InvoiceRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 invoice repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2InvoiceRepository(Connection connection,
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

    public List<SSInvoice> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSInvoice> invoices = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_invoice WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            invoices.add(mapInvoiceV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return invoices;
        } catch (SQLException e) {
            throw handleFailure("load invoices", e);
        }
    }

    public Optional<SSInvoice> findByInvoice(SSInvoice invoice) {
        if (invoice == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_invoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, invoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapInvoiceV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find invoice '" + invoice.getNumber() + "'", e);
        }
    }

    public List<SSInvoice> findAll(List<SSInvoice> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSInvoice> invoices = new LinkedList<>();
            for (SSInvoice invoice : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_invoice WHERE number=? AND companyid=?")) {
                    statement.setObject(1, invoice.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            invoices.add(mapInvoiceV2(resultSet));
                        }
                    }
                }
            }
            return invoices;
        } catch (SQLException e) {
            throw handleFailure("filter invoices", e);
        }
    }

    public void add(SSInvoice invoice) {
        if (invoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("invoice");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_invoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            invoice.setNumber(number + 1);
                        } else {
                            invoice.setNumber(companyNumber + 1);
                        }
                    } else {
                        invoice.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer invoiceId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_invoice(" +
                            "number,companyid,vdate,customer_nr,customer_name,our_contact,your_contact," +
                            "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                            "sale_text,eu_sale_commodity,eu_sale_third_part,printed,invoice_type,currency_rate," +
                            "payment_day,your_order_number,ocr_number,entered,cancelled,num_reminders,interest_invoiced," +
                            "stock_influencing,order_numbers,journal_numbers,voucher_id,inv_addr_name,inv_addr_address," +
                            "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                            "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, invoice.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindInvoiceColumnsV2(insertStatement, i, invoice);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        invoiceId = keys.getInt(1);
                    }
                }
            }

            if (invoiceId == null) {
                invoiceId = getInvoiceIdV2(invoice.getNumber(), currentCompany.getId());
            }
            if (invoiceId != null) {
                replaceInvoiceRowsV2(invoiceId, invoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWINVOICE", "TBL_INVOICE", String.valueOf(invoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add invoice '" + invoice.getNumber() + "'", e);
        }
    }

    public void update(SSInvoice invoice) {
        if (invoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_invoice SET " +
                            "vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                            "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                            "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                            "invoice_type=?,currency_rate=?,payment_day=?,your_order_number=?,ocr_number=?," +
                            "entered=?,cancelled=?,num_reminders=?,interest_invoiced=?,stock_influencing=?,order_numbers=?," +
                            "journal_numbers=?,voucher_id=?,inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                            "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                            "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindInvoiceColumnsV2(statement, 1, invoice);
                statement.setObject(i++, invoice.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer invoiceId = getInvoiceIdV2(invoice.getNumber(), currentCompany.getId());
            if (invoiceId != null) {
                replaceInvoiceRowsV2(invoiceId, invoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITINVOICE", "TBL_INVOICE", String.valueOf(invoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update invoice '" + invoice.getNumber() + "'", e);
        }
    }

    public void delete(SSInvoice invoice) {
        if (invoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer invoiceId = getInvoiceIdV2(invoice.getNumber(), currentCompany.getId());
            if (invoiceId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_invoice_row WHERE invoice_id=?")) {
                    deleteRows.setObject(1, invoiceId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_invoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, invoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEINVOICE", "TBL_INVOICE", String.valueOf(invoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete invoice '" + invoice.getNumber() + "'", e);
        }
    }

    private List<SSSaleRow> getInvoiceRowsV2(Integer invoiceId) throws SQLException {
        List<SSSaleRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_invoice_row WHERE invoice_id=? ORDER BY id")) {
            statement.setObject(1, invoiceId);
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

    private void replaceInvoiceRowsV2(Integer invoiceId, SSInvoice invoice) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_invoice_row WHERE invoice_id=?")) {
            delete.setObject(1, invoiceId);
            delete.executeUpdate();
        }

        for (SSSaleRow row : invoice.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_invoice_row(invoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, invoiceId);
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

    private Integer getInvoiceIdV2(Integer invoiceNumber, Integer companyId) throws SQLException {
        if (invoiceNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_invoice WHERE number=? AND companyid=?")) {
            statement.setObject(1, invoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    /**
     * Returns the maximum invoice number from tbl_invoice for the current company.
     *
     * @return the highest invoice number, or -1 if no invoices exist
     */
    public int getMaxInvoiceId() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return -1;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS max_number FROM tbl_invoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Object maxNumber = resultSet.getObject("max_number");
                        if (maxNumber != null) {
                            return ((Number) maxNumber).intValue();
                        }
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw handleFailure("get max invoice number", e);
        }
    }

    private int bindInvoiceColumnsV2(PreparedStatement statement, int index, SSInvoice invoice) throws SQLException {
        bindLocalDateV2(statement, index++, invoice.getLocalDate());
        statement.setObject(index++, invoice.getCustomerNr());
        statement.setObject(index++, invoice.getCustomerName());
        statement.setObject(index++, invoice.getOurContactPerson());
        statement.setObject(index++, invoice.getYourContactPerson());
        statement.setObject(index++, invoice.getDelayInterest());
        statement.setObject(index++, getCurrencyCodeV2(invoice.getCurrency()));
        statement.setObject(index++, invoice.getPaymentTerm() == null ? null : invoice.getPaymentTerm().getName());
        statement.setObject(index++, invoice.getDeliveryTerm() == null ? null : invoice.getDeliveryTerm().getName());
        statement.setObject(index++, invoice.getDeliveryWay() == null ? null : invoice.getDeliveryWay().getName());
        statement.setObject(index++, invoice.getTaxFree());
        statement.setObject(index++, invoice.getText());
        statement.setObject(index++, invoice.getEuSaleCommodity());
        statement.setObject(index++, invoice.getEuSaleThirdPartCommodity());
        statement.setObject(index++, invoice.isPrinted());
        statement.setObject(index++, invoice.getType() == null ? null : invoice.getType().name());
        statement.setObject(index++, invoice.getCurrencyRate());
        bindLocalDateV2(statement, index++, invoice.getLocalDueDate());
        statement.setObject(index++, invoice.getYourOrderNumber());
        statement.setObject(index++, invoice.getOCRNumber());
        statement.setObject(index++, invoice.isEntered());
        statement.setObject(index++, invoice.isCancelled());
        statement.setObject(index++, invoice.getNumReminders());
        statement.setObject(index++, invoice.isInterestInvoiced());
        statement.setObject(index++, invoice.isStockInfluencing());
        statement.setObject(index++, invoice.getOrderNumbers());
        statement.setObject(index++, invoice.getJournalNumbers());
        statement.setObject(index++, V2RepositoryHelpers.getVoucherIdByNumber(connection, invoice.getVoucher()));
        index = V2RepositoryHelpers.bindAddress(statement, index, invoice.getInvoiceAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, invoice.getDeliveryAddress());
    }

    private SSInvoice mapInvoiceV2(ResultSet resultSet) throws SQLException {
        SSInvoice invoice = new SSInvoice();

        invoice.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            invoice.setLocalDate(date.toLocalDate());
        }

        invoice.setCustomerNr(resultSet.getString("customer_nr"));
        invoice.setCustomerName(resultSet.getString("customer_name"));
        invoice.setOurContactPerson(resultSet.getString("our_contact"));
        invoice.setYourContactPerson(resultSet.getString("your_contact"));
        invoice.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        invoice.setTaxFree(resultSet.getBoolean("tax_free"));
        invoice.setText(resultSet.getString("sale_text"));
        invoice.setEuSaleCommodity(resultSet.getBoolean("eu_sale_commodity"));
        invoice.setEuSaleYhirdPartCommodity(resultSet.getBoolean("eu_sale_third_part"));
        invoice.setPrinted(resultSet.getBoolean("printed"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            invoice.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            invoice.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            invoice.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            invoice.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }

        String invoiceType = resultSet.getString("invoice_type");
        if (invoiceType != null) {
            try {
                invoice.setType(SSInvoiceType.valueOf(invoiceType));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown enum values from partial migrations.
            }
        }

        invoice.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));

        Date paymentDay = resultSet.getDate("payment_day");
        if (paymentDay != null) {
            invoice.setLocalDueDate(paymentDay.toLocalDate());
        }

        invoice.setYourOrderNumber(resultSet.getString("your_order_number"));
        invoice.setOCRNumber(resultSet.getString("ocr_number"));
        invoice.setEntered(resultSet.getBoolean("entered"));
        invoice.setCancelled(resultSet.getBoolean("cancelled"));
        invoice.setNumRemainders(resultSet.getInt("num_reminders"));
        invoice.setInterestInvoiced(resultSet.getBoolean("interest_invoiced"));
        invoice.setStockInfluencing(resultSet.getBoolean("stock_influencing"));
        invoice.setOrderNumbers(resultSet.getString("order_numbers"));
        invoice.setJournalNumbers(resultSet.getString("journal_numbers"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
        if (voucherNumber != null) {
            invoice.setVoucher(new SSVoucher(voucherNumber));
        }

        invoice.setInvoiceAddress(V2RepositoryHelpers.mapAddress(resultSet, "inv_addr"));
        invoice.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));

        invoice.getRows().clear();
        invoice.getRows().addAll(getInvoiceRowsV2(resultSet.getInt("id")));
        return invoice;
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
            LOG.error("Failed to rollback invoice transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
