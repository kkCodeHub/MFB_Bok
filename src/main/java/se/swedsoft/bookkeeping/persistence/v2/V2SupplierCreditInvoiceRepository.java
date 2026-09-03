package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 supplier-credit-invoice repository backed by the normalized V2 schema.
 */
public class V2SupplierCreditInvoiceRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2SupplierCreditInvoiceRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    public V2SupplierCreditInvoiceRepository(Connection connection,
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

    public List<SSSupplierCreditInvoice> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSSupplierCreditInvoice> supplierCreditInvoices = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_suppliercreditinvoice WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            supplierCreditInvoices.add(mapSupplierCreditInvoiceV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return supplierCreditInvoices;
        } catch (SQLException e) {
            throw handleFailure("load supplier credit invoices", e);
        }
    }

    public Optional<SSSupplierCreditInvoice> findBySupplierCreditInvoice(SSSupplierCreditInvoice supplierCreditInvoice) {
        if (supplierCreditInvoice == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_suppliercreditinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, supplierCreditInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapSupplierCreditInvoiceV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find supplier credit invoice '" + supplierCreditInvoice.getNumber() + "'", e);
        }
    }

    public void add(SSSupplierCreditInvoice supplierCreditInvoice) {
        if (supplierCreditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("suppliercreditinvoice");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_suppliercreditinvoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            supplierCreditInvoice.setNumber(number + 1);
                        } else {
                            supplierCreditInvoice.setNumber(companyNumber + 1);
                        }
                    } else {
                        supplierCreditInvoice.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer supplierCreditInvoiceId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_suppliercreditinvoice(" +
                            "number,companyid,crediting_nr,vdate,due_date,supplier_nr,supplier_name,reference_number," +
                            "currency_code,currency_rate,payment_term,tax_sum,rounding_sum,entered,stock_influencing," +
                            "bgc_entered,voucher_id,correction_voucher_id) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, supplierCreditInvoice.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindSupplierCreditInvoiceColumnsV2(insertStatement, i, supplierCreditInvoice);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        supplierCreditInvoiceId = keys.getInt(1);
                    }
                }
            }

            if (supplierCreditInvoiceId == null) {
                supplierCreditInvoiceId = getSupplierCreditInvoiceIdV2(supplierCreditInvoice.getNumber(), currentCompany.getId());
            }
            if (supplierCreditInvoiceId != null) {
                replaceSupplierCreditInvoiceRowsV2(supplierCreditInvoiceId, supplierCreditInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWSUPPLIERCREDITINVOICE", "TBL_SUPPLIERCREDITINVOICE",
                    String.valueOf(supplierCreditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add supplier credit invoice '" + supplierCreditInvoice.getNumber() + "'", e);
        }
    }

    public void update(SSSupplierCreditInvoice supplierCreditInvoice) {
        if (supplierCreditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_suppliercreditinvoice SET " +
                            "crediting_nr=?,vdate=?,due_date=?,supplier_nr=?,supplier_name=?,reference_number=?," +
                            "currency_code=?,currency_rate=?,payment_term=?,tax_sum=?,rounding_sum=?,entered=?," +
                            "stock_influencing=?,bgc_entered=?,voucher_id=?,correction_voucher_id=? " +
                            "WHERE number=? AND companyid=?")) {
                int i = bindSupplierCreditInvoiceColumnsV2(statement, 1, supplierCreditInvoice);
                statement.setObject(i++, supplierCreditInvoice.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer supplierCreditInvoiceId = getSupplierCreditInvoiceIdV2(
                    supplierCreditInvoice.getNumber(), currentCompany.getId());
            if (supplierCreditInvoiceId != null) {
                replaceSupplierCreditInvoiceRowsV2(supplierCreditInvoiceId, supplierCreditInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITSUPPLIERCREDITINVOICE", "TBL_SUPPLIERCREDITINVOICE",
                    String.valueOf(supplierCreditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update supplier credit invoice '" + supplierCreditInvoice.getNumber() + "'", e);
        }
    }

    public void delete(SSSupplierCreditInvoice supplierCreditInvoice) {
        if (supplierCreditInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer supplierCreditInvoiceId = getSupplierCreditInvoiceIdV2(
                    supplierCreditInvoice.getNumber(), currentCompany.getId());
            if (supplierCreditInvoiceId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_suppliercreditinvoice_row WHERE suppliercreditinvoice_id=?")) {
                    deleteRows.setObject(1, supplierCreditInvoiceId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_suppliercreditinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, supplierCreditInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETESUPPLIERCREDITINVOICE", "TBL_SUPPLIERCREDITINVOICE",
                    String.valueOf(supplierCreditInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete supplier credit invoice '" + supplierCreditInvoice.getNumber() + "'", e);
        }
    }

    private List<SSSupplierInvoiceRow> getSupplierCreditInvoiceRowsV2(Integer supplierCreditInvoiceId)
            throws SQLException {
        List<SSSupplierInvoiceRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_suppliercreditinvoice_row WHERE suppliercreditinvoice_id=? ORDER BY id")) {
            statement.setObject(1, supplierCreditInvoiceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setDescription(resultSet.getString("description"));
                    row.setUnitprice(resultSet.getBigDecimal("unitprice"));
                    row.setQuantity((Integer) resultSet.getObject("quantity"));

                    String unit = resultSet.getString("unit");
                    if (unit != null) {
                        row.setUnit(new SSUnit(unit, unit));
                    }

                    row.setUnitFreight(resultSet.getBigDecimal("unit_freight"));
                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    row.setProjectNr(resultSet.getString("project_number"));
                    row.setResultUnitNr(resultSet.getString("result_unit_number"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceSupplierCreditInvoiceRowsV2(Integer supplierCreditInvoiceId,
                                                    SSSupplierCreditInvoice supplierCreditInvoice) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_suppliercreditinvoice_row WHERE suppliercreditinvoice_id=?")) {
            delete.setObject(1, supplierCreditInvoiceId);
            delete.executeUpdate();
        }

        for (SSSupplierInvoiceRow row : supplierCreditInvoice.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_suppliercreditinvoice_row(" +
                            "suppliercreditinvoice_id,product_nr,description,unitprice,quantity,unit,unit_freight," +
                            "account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, supplierCreditInvoiceId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getDescription());
                insert.setObject(4, row.getUnitprice());
                insert.setObject(5, row.getQuantity());
                insert.setObject(6, row.getUnit() == null ? null : row.getUnit().getName());
                insert.setObject(7, row.getUnitFreight());
                insert.setObject(8, row.getAccountNr());
                insert.setObject(9, row.getProjectNr());
                insert.setObject(10, row.getResultUnitNr());
                insert.executeUpdate();
            }
        }
    }

    private Integer getSupplierCreditInvoiceIdV2(Integer supplierCreditInvoiceNumber, Integer companyId)
            throws SQLException {
        if (supplierCreditInvoiceNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_suppliercreditinvoice WHERE number=? AND companyid=?")) {
            statement.setObject(1, supplierCreditInvoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindSupplierCreditInvoiceColumnsV2(
            PreparedStatement statement,
            int index,
            SSSupplierCreditInvoice supplierCreditInvoice) throws SQLException {
        statement.setObject(index++, supplierCreditInvoice.getCreditingNr());
        bindLocalDateV2(statement, index++, supplierCreditInvoice.getLocalDate());
        bindLocalDateV2(statement, index++, supplierCreditInvoice.getLocalDueDate());
        statement.setObject(index++, supplierCreditInvoice.getSupplierNr());
        statement.setObject(index++, supplierCreditInvoice.getSupplierName());
        statement.setObject(index++, supplierCreditInvoice.getReferencenumber());
        statement.setObject(index++, getCurrencyCodeV2(supplierCreditInvoice.getCurrency()));
        statement.setObject(index++, supplierCreditInvoice.getCurrencyRate());
        // SSSupplierInvoice currently has no public payment-term getter.
        statement.setObject(index++, null);
        statement.setObject(index++, supplierCreditInvoice.getTaxSum());
        statement.setObject(index++, supplierCreditInvoice.getRoundingSum());
        statement.setObject(index++, supplierCreditInvoice.isEntered());
        statement.setObject(index++, supplierCreditInvoice.isStockInfluencing());
        statement.setObject(index++, supplierCreditInvoice.isBGCEntered());
        statement.setObject(index++, getVoucherIdByNumberV2(supplierCreditInvoice.getVoucher()));
        statement.setObject(index++, getVoucherIdByNumberV2(supplierCreditInvoice.getCorrection()));
        return index;
    }

    private SSSupplierCreditInvoice mapSupplierCreditInvoiceV2(ResultSet resultSet) throws SQLException {
        SSSupplierCreditInvoice supplierCreditInvoice = new SSSupplierCreditInvoice();
        supplierCreditInvoice.setNumber((Integer) resultSet.getObject("number"));
        supplierCreditInvoice.setCreditingNr((Integer) resultSet.getObject("crediting_nr"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            supplierCreditInvoice.setLocalDate(date.toLocalDate());
        }

        Date dueDate = resultSet.getDate("due_date");
        if (dueDate != null) {
            supplierCreditInvoice.setLocalDueDate(dueDate.toLocalDate());
        }

        supplierCreditInvoice.setSupplierNr(resultSet.getString("supplier_nr"));
        supplierCreditInvoice.setSupplierName(resultSet.getString("supplier_name"));
        supplierCreditInvoice.setReferencenumber(resultSet.getString("reference_number"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            supplierCreditInvoice.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            supplierCreditInvoice.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        supplierCreditInvoice.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
        supplierCreditInvoice.setTaxSum(resultSet.getBigDecimal("tax_sum"));
        supplierCreditInvoice.setRoundingSum(resultSet.getBigDecimal("rounding_sum"));
        supplierCreditInvoice.setEntered(resultSet.getBoolean("entered"));
        supplierCreditInvoice.setStockInfluencing(resultSet.getBoolean("stock_influencing"));
        supplierCreditInvoice.setBGCEntered(resultSet.getBoolean("bgc_entered"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = getVoucherNumberForIdV2(voucherId);
        if (voucherNumber != null) {
            supplierCreditInvoice.setVoucher(new SSVoucher(voucherNumber));
        }

        Integer correctionVoucherId = (Integer) resultSet.getObject("correction_voucher_id");
        Integer correctionVoucherNumber = getVoucherNumberForIdV2(correctionVoucherId);
        if (correctionVoucherNumber != null) {
            supplierCreditInvoice.setCorrection(new SSVoucher(correctionVoucherNumber));
        }

        supplierCreditInvoice.getRows().clear();
        supplierCreditInvoice.getRows().addAll(getSupplierCreditInvoiceRowsV2(resultSet.getInt("id")));
        return supplierCreditInvoice;
    }

    private Integer getVoucherIdByNumberV2(SSVoucher voucher) throws SQLException {
        return V2RepositoryHelpers.getVoucherIdByNumber(connection, voucher);
    }

    private Integer getVoucherNumberForIdV2(Integer voucherId) throws SQLException {
        return V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
    }

    private String getCurrencyCodeV2(SSCurrency currency) {
        return currency == null ? null : currency.getName();
    }

    private void bindLocalDateV2(PreparedStatement statement, int index, LocalDate localDate) throws SQLException {
        if (localDate == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(localDate));
        }
    }

    private RuntimeException handleFailure(String action, SQLException e) {
        LOG.error("Unexpected error while trying to {}", action, e);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            LOG.warn("Rollback failed after {}", action, rollbackException);
        }
        return new RuntimeException("Could not " + action, e);
    }
}
