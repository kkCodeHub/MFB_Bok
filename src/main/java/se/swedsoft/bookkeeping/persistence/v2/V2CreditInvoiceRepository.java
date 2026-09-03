package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
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
 * V2 credit-invoice repository backed by the normalized V2 schema.
 */
public class V2CreditInvoiceRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2CreditInvoiceRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 credit-invoice repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2CreditInvoiceRepository(Connection connection,
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

    public List<SSCreditInvoice> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSCreditInvoice> creditInvoices = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_creditinvoice WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            creditInvoices.add(mapCreditInvoiceV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return creditInvoices;
        } catch (SQLException e) {
            throw handleFailure("load credit invoices", e);
        }
    }

    public Optional<SSCreditInvoice> findByCreditInvoice(SSCreditInvoice creditInvoice) {
        if (creditInvoice == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_creditinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, creditInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapCreditInvoiceV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find credit invoice '" + creditInvoice.getNumber() + "'", e);
        }
    }

    public List<SSCreditInvoice> findAll(List<SSCreditInvoice> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSCreditInvoice> creditInvoices = new LinkedList<>();
            for (SSCreditInvoice creditInvoice : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_creditinvoice WHERE number=? AND companyid=?")) {
                    statement.setObject(1, creditInvoice.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            creditInvoices.add(mapCreditInvoiceV2(resultSet));
                        }
                    }
                }
            }
            return creditInvoices;
        } catch (SQLException e) {
            throw handleFailure("filter credit invoices", e);
        }
    }

    public void add(SSCreditInvoice creditInvoice) {
        if (creditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("creditinvoice");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_creditinvoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            creditInvoice.setNumber(number + 1);
                        } else {
                            creditInvoice.setNumber(companyNumber + 1);
                        }
                    } else {
                        creditInvoice.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer creditInvoiceId = null;
            String placeholders = String.join(",", Collections.nCopies(41, "?"));
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_creditinvoice(" +
                            "number,companyid,crediting_nr,vdate,customer_nr,customer_name,our_contact,your_contact," +
                            "delay_interest,currency_code,payment_term,delivery_term,delivery_way,tax_free," +
                            "sale_text,eu_sale_commodity,eu_sale_third_part,printed,invoice_type,currency_rate," +
                            "payment_day,your_order_number,ocr_number,entered,num_reminders,interest_invoiced," +
                            "stock_influencing,order_numbers,voucher_id,inv_addr_name,inv_addr_address," +
                            "inv_addr_street,inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name," +
                            "del_addr_address,del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                            "VALUES(" + placeholders + ")",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, creditInvoice.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindCreditInvoiceColumnsV2(insertStatement, i, creditInvoice);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        creditInvoiceId = keys.getInt(1);
                    }
                }
            }

            if (creditInvoiceId == null) {
                creditInvoiceId = getCreditInvoiceIdV2(creditInvoice.getNumber(), currentCompany.getId());
            }
            if (creditInvoiceId != null) {
                replaceCreditInvoiceRowsV2(creditInvoiceId, creditInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWCREDITINVOICE", "TBL_CREDITINVOICE",
                    String.valueOf(creditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add credit invoice '" + creditInvoice.getNumber() + "'", e);
        }
    }

    public void update(SSCreditInvoice creditInvoice) {
        if (creditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_creditinvoice SET " +
                            "crediting_nr=?,vdate=?,customer_nr=?,customer_name=?,our_contact=?,your_contact=?," +
                            "delay_interest=?,currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                            "tax_free=?,sale_text=?,eu_sale_commodity=?,eu_sale_third_part=?,printed=?," +
                            "invoice_type=?,currency_rate=?,payment_day=?,your_order_number=?,ocr_number=?," +
                            "entered=?,num_reminders=?,interest_invoiced=?,stock_influencing=?,order_numbers=?," +
                            "voucher_id=?,inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                            "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?,del_addr_street=?," +
                            "del_addr_zipcode=?,del_addr_city=?,del_addr_country=? WHERE number=? AND companyid=?")) {
                int i = bindCreditInvoiceColumnsV2(statement, 1, creditInvoice);
                statement.setObject(i++, creditInvoice.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer creditInvoiceId = getCreditInvoiceIdV2(creditInvoice.getNumber(), currentCompany.getId());
            if (creditInvoiceId != null) {
                replaceCreditInvoiceRowsV2(creditInvoiceId, creditInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITCREDITINVOICE", "TBL_CREDITINVOICE",
                    String.valueOf(creditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update credit invoice '" + creditInvoice.getNumber() + "'", e);
        }
    }

    public void delete(SSCreditInvoice creditInvoice) {
        if (creditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer creditInvoiceId = getCreditInvoiceIdV2(creditInvoice.getNumber(), currentCompany.getId());
            if (creditInvoiceId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_creditinvoice_row WHERE creditinvoice_id=?")) {
                    deleteRows.setObject(1, creditInvoiceId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_creditinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, creditInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETECREDITINVOICE", "TBL_CREDITINVOICE",
                    String.valueOf(creditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete credit invoice '" + creditInvoice.getNumber() + "'", e);
        }
    }

    private List<SSSaleRow> getCreditInvoiceRowsV2(Integer creditInvoiceId) throws SQLException {
        List<SSSaleRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_creditinvoice_row WHERE creditinvoice_id=? ORDER BY id")) {
            statement.setObject(1, creditInvoiceId);
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

    private void replaceCreditInvoiceRowsV2(Integer creditInvoiceId, SSCreditInvoice creditInvoice) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_creditinvoice_row WHERE creditinvoice_id=?")) {
            delete.setObject(1, creditInvoiceId);
            delete.executeUpdate();
        }

        for (SSSaleRow row : creditInvoice.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_creditinvoice_row(creditinvoice_id,product_nr,description,unitprice,count,unit,discount,tax_code,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, creditInvoiceId);
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

    private Integer getCreditInvoiceIdV2(Integer creditInvoiceNumber, Integer companyId) throws SQLException {
        if (creditInvoiceNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_creditinvoice WHERE number=? AND companyid=?")) {
            statement.setObject(1, creditInvoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindCreditInvoiceColumnsV2(PreparedStatement statement, int index, SSCreditInvoice creditInvoice)
            throws SQLException {
        statement.setObject(index++, creditInvoice.getCreditingNr());
        bindLocalDateV2(statement, index++, creditInvoice.getLocalDate());
        statement.setObject(index++, creditInvoice.getCustomerNr());
        statement.setObject(index++, creditInvoice.getCustomerName());
        statement.setObject(index++, creditInvoice.getOurContactPerson());
        statement.setObject(index++, creditInvoice.getYourContactPerson());
        statement.setObject(index++, creditInvoice.getDelayInterest());
        statement.setObject(index++, getCurrencyCodeV2(creditInvoice.getCurrency()));
        statement.setObject(index++, creditInvoice.getPaymentTerm() == null ? null : creditInvoice.getPaymentTerm().getName());
        statement.setObject(index++, creditInvoice.getDeliveryTerm() == null ? null : creditInvoice.getDeliveryTerm().getName());
        statement.setObject(index++, creditInvoice.getDeliveryWay() == null ? null : creditInvoice.getDeliveryWay().getName());
        statement.setObject(index++, creditInvoice.getTaxFree());
        statement.setObject(index++, creditInvoice.getText());
        statement.setObject(index++, creditInvoice.getEuSaleCommodity());
        statement.setObject(index++, creditInvoice.getEuSaleThirdPartCommodity());
        statement.setObject(index++, creditInvoice.isPrinted());
        statement.setObject(index++, creditInvoice.getType() == null ? null : creditInvoice.getType().name());
        statement.setObject(index++, creditInvoice.getCurrencyRate());
        bindLocalDateV2(statement, index++, creditInvoice.getLocalDueDate());
        statement.setObject(index++, creditInvoice.getYourOrderNumber());
        statement.setObject(index++, creditInvoice.getOCRNumber());
        statement.setObject(index++, creditInvoice.isEntered());
        statement.setObject(index++, creditInvoice.getNumReminders());
        statement.setObject(index++, creditInvoice.isInterestInvoiced());
        statement.setObject(index++, creditInvoice.isStockInfluencing());
        statement.setObject(index++, creditInvoice.getOrderNumbers());
        statement.setObject(index++, V2RepositoryHelpers.getVoucherIdByNumber(connection, creditInvoice.getVoucher()));
        index = V2RepositoryHelpers.bindAddress(statement, index, creditInvoice.getInvoiceAddress());
        return V2RepositoryHelpers.bindAddress(statement, index, creditInvoice.getDeliveryAddress());
    }

    private SSCreditInvoice mapCreditInvoiceV2(ResultSet resultSet) throws SQLException {
        SSCreditInvoice creditInvoice = new SSCreditInvoice();

        creditInvoice.setNumber((Integer) resultSet.getObject("number"));
        creditInvoice.setCreditingNr((Integer) resultSet.getObject("crediting_nr"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            creditInvoice.setLocalDate(date.toLocalDate());
        }

        creditInvoice.setCustomerNr(resultSet.getString("customer_nr"));
        creditInvoice.setCustomerName(resultSet.getString("customer_name"));
        creditInvoice.setOurContactPerson(resultSet.getString("our_contact"));
        creditInvoice.setYourContactPerson(resultSet.getString("your_contact"));
        creditInvoice.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        creditInvoice.setTaxFree(resultSet.getBoolean("tax_free"));
        creditInvoice.setText(resultSet.getString("sale_text"));
        creditInvoice.setEuSaleCommodity(resultSet.getBoolean("eu_sale_commodity"));
        creditInvoice.setEuSaleYhirdPartCommodity(resultSet.getBoolean("eu_sale_third_part"));
        creditInvoice.setPrinted(resultSet.getBoolean("printed"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            creditInvoice.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            creditInvoice.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        String deliveryTerm = resultSet.getString("delivery_term");
        if (deliveryTerm != null) {
            creditInvoice.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }

        String deliveryWay = resultSet.getString("delivery_way");
        if (deliveryWay != null) {
            creditInvoice.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }

        String invoiceType = resultSet.getString("invoice_type");
        if (invoiceType != null) {
            try {
                creditInvoice.setType(SSInvoiceType.valueOf(invoiceType));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown enum values from partial migrations.
            }
        }

        creditInvoice.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));

        Date paymentDay = resultSet.getDate("payment_day");
        if (paymentDay != null) {
            creditInvoice.setLocalDueDate(paymentDay.toLocalDate());
        }

        creditInvoice.setYourOrderNumber(resultSet.getString("your_order_number"));
        creditInvoice.setOCRNumber(resultSet.getString("ocr_number"));
        creditInvoice.setEntered(resultSet.getBoolean("entered"));
        creditInvoice.setNumRemainders(resultSet.getInt("num_reminders"));
        creditInvoice.setInterestInvoiced(resultSet.getBoolean("interest_invoiced"));
        creditInvoice.setStockInfluencing(resultSet.getBoolean("stock_influencing"));
        creditInvoice.setOrderNumbers(resultSet.getString("order_numbers"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
        if (voucherNumber != null) {
            creditInvoice.setVoucher(new SSVoucher(voucherNumber));
        }

        creditInvoice.setInvoiceAddress(V2RepositoryHelpers.mapAddress(resultSet, "inv_addr"));
        creditInvoice.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));

        creditInvoice.getRows().clear();
        creditInvoice.getRows().addAll(getCreditInvoiceRowsV2(resultSet.getInt("id")));
        return creditInvoice;
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
            LOG.error("Failed to rollback credit-invoice transaction after {}", operation,
                    rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
