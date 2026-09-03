package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
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
 * V2 supplier-invoice repository backed by the normalized V2 schema.
 */
public class V2SupplierInvoiceRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2SupplierInvoiceRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    public V2SupplierInvoiceRepository(Connection connection,
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

    public List<SSSupplierInvoice> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSSupplierInvoice> supplierInvoices = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_supplierinvoice WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            supplierInvoices.add(mapSupplierInvoiceV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return supplierInvoices;
        } catch (SQLException e) {
            throw handleFailure("load supplier invoices", e);
        }
    }

    public Optional<SSSupplierInvoice> findBySupplierInvoice(SSSupplierInvoice supplierInvoice) {
        if (supplierInvoice == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_supplierinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, supplierInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapSupplierInvoiceV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find supplier invoice '" + supplierInvoice.getNumber() + "'", e);
        }
    }

    public List<SSSupplierInvoice> findAll(List<SSSupplierInvoice> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSSupplierInvoice> supplierInvoices = new LinkedList<>();
            for (SSSupplierInvoice supplierInvoice : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_supplierinvoice WHERE number=? AND companyid=?")) {
                    statement.setObject(1, supplierInvoice.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            supplierInvoices.add(mapSupplierInvoiceV2(resultSet));
                        }
                    }
                }
            }
            return supplierInvoices;
        } catch (SQLException e) {
            throw handleFailure("filter supplier invoices", e);
        }
    }

    public void add(SSSupplierInvoice supplierInvoice) {
        if (supplierInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("supplierinvoice");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_supplierinvoice WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            supplierInvoice.setNumber(number + 1);
                        } else {
                            supplierInvoice.setNumber(companyNumber + 1);
                        }
                    } else {
                        supplierInvoice.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer supplierInvoiceId = null;
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_supplierinvoice(" +
                            "number,companyid,vdate,due_date,supplier_nr,supplier_name,reference_number," +
                            "currency_code,currency_rate,payment_term,tax_sum,rounding_sum,entered," +
                            "stock_influencing,bgc_entered,voucher_id,correction_voucher_id) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStatement.setObject(i++, supplierInvoice.getNumber());
                insertStatement.setObject(i++, currentCompany.getId());
                bindSupplierInvoiceColumnsV2(insertStatement, i, supplierInvoice);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        supplierInvoiceId = keys.getInt(1);
                    }
                }
            }

            if (supplierInvoiceId == null) {
                supplierInvoiceId = getSupplierInvoiceIdV2(supplierInvoice.getNumber(), currentCompany.getId());
            }
            if (supplierInvoiceId != null) {
                replaceSupplierInvoiceRowsV2(supplierInvoiceId, supplierInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWSUPPLIERINVOICE", "TBL_SUPPLIERINVOICE",
                    String.valueOf(supplierInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add supplier invoice '" + supplierInvoice.getNumber() + "'", e);
        }
    }

    public void update(SSSupplierInvoice supplierInvoice) {
        if (supplierInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_supplierinvoice SET " +
                            "vdate=?,due_date=?,supplier_nr=?,supplier_name=?,reference_number=?," +
                            "currency_code=?,currency_rate=?,payment_term=?,tax_sum=?,rounding_sum=?," +
                            "entered=?,stock_influencing=?,bgc_entered=?,voucher_id=?,correction_voucher_id=? " +
                            "WHERE number=? AND companyid=?")) {
                int i = bindSupplierInvoiceColumnsV2(statement, 1, supplierInvoice);
                statement.setObject(i++, supplierInvoice.getNumber());
                statement.setObject(i, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer supplierInvoiceId = getSupplierInvoiceIdV2(supplierInvoice.getNumber(), currentCompany.getId());
            if (supplierInvoiceId != null) {
                replaceSupplierInvoiceRowsV2(supplierInvoiceId, supplierInvoice);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITSUPPLIERINVOICE", "TBL_SUPPLIERINVOICE",
                    String.valueOf(supplierInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update supplier invoice '" + supplierInvoice.getNumber() + "'", e);
        }
    }

    public void delete(SSSupplierInvoice supplierInvoice) {
        if (supplierInvoice == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer supplierInvoiceId = getSupplierInvoiceIdV2(supplierInvoice.getNumber(), currentCompany.getId());
            if (supplierInvoiceId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_supplierinvoice_row WHERE supplierinvoice_id=?")) {
                    deleteRows.setObject(1, supplierInvoiceId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_supplierinvoice WHERE number=? AND companyid=?")) {
                statement.setObject(1, supplierInvoice.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETESUPPLIERINVOICE", "TBL_SUPPLIERINVOICE",
                    String.valueOf(supplierInvoice.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete supplier invoice '" + supplierInvoice.getNumber() + "'", e);
        }
    }

    private List<SSSupplierInvoiceRow> getSupplierInvoiceRowsV2(Integer supplierInvoiceId) throws SQLException {
        List<SSSupplierInvoiceRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_supplierinvoice_row WHERE supplierinvoice_id=? ORDER BY id")) {
            statement.setObject(1, supplierInvoiceId);
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

    private void replaceSupplierInvoiceRowsV2(Integer supplierInvoiceId, SSSupplierInvoice supplierInvoice)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_supplierinvoice_row WHERE supplierinvoice_id=?")) {
            delete.setObject(1, supplierInvoiceId);
            delete.executeUpdate();
        }

        for (SSSupplierInvoiceRow row : supplierInvoice.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_supplierinvoice_row(supplierinvoice_id,product_nr,description,unitprice,quantity,unit,unit_freight,account_nr,project_number,result_unit_number) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, supplierInvoiceId);
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

    private Integer getSupplierInvoiceIdV2(Integer supplierInvoiceNumber, Integer companyId) throws SQLException {
        if (supplierInvoiceNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_supplierinvoice WHERE number=? AND companyid=?")) {
            statement.setObject(1, supplierInvoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private int bindSupplierInvoiceColumnsV2(PreparedStatement statement,
                                             int index,
                                             SSSupplierInvoice supplierInvoice) throws SQLException {
        bindLocalDateV2(statement, index++, supplierInvoice.getLocalDate());
        bindLocalDateV2(statement, index++, supplierInvoice.getLocalDueDate());
        statement.setObject(index++, supplierInvoice.getSupplierNr());
        statement.setObject(index++, supplierInvoice.getSupplierName());
        statement.setObject(index++, supplierInvoice.getReferencenumber());
        statement.setObject(index++, getCurrencyCodeV2(supplierInvoice.getCurrency()));
        statement.setObject(index++, supplierInvoice.getCurrencyRate());
        // SSSupplierInvoice currently has no public payment-term getter.
        statement.setObject(index++, null);
        statement.setObject(index++, supplierInvoice.getTaxSum());
        statement.setObject(index++, supplierInvoice.getRoundingSum());
        statement.setObject(index++, supplierInvoice.isEntered());
        statement.setObject(index++, supplierInvoice.isStockInfluencing());
        statement.setObject(index++, supplierInvoice.isBGCEntered());
        statement.setObject(index++, V2RepositoryHelpers.getVoucherIdByNumber(connection, supplierInvoice.getVoucher()));
        statement.setObject(index++, V2RepositoryHelpers.getVoucherIdByNumber(connection, supplierInvoice.getCorrection()));
        return index;
    }

    private SSSupplierInvoice mapSupplierInvoiceV2(ResultSet resultSet) throws SQLException {
        SSSupplierInvoice supplierInvoice = new SSSupplierInvoice() {
            @Override
            public LocalDate getLastLocalDate() {
                return null;
            }
        };
        supplierInvoice.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            supplierInvoice.setLocalDate(date.toLocalDate());
        }

        Date dueDate = resultSet.getDate("due_date");
        if (dueDate != null) {
            supplierInvoice.setLocalDueDate(dueDate.toLocalDate());
        }

        supplierInvoice.setSupplierNr(resultSet.getString("supplier_nr"));
        supplierInvoice.setSupplierName(resultSet.getString("supplier_name"));
        supplierInvoice.setReferencenumber(resultSet.getString("reference_number"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            supplierInvoice.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        String paymentTerm = resultSet.getString("payment_term");
        if (paymentTerm != null) {
            supplierInvoice.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        supplierInvoice.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
        supplierInvoice.setTaxSum(resultSet.getBigDecimal("tax_sum"));
        supplierInvoice.setRoundingSum(resultSet.getBigDecimal("rounding_sum"));
        supplierInvoice.setEntered(resultSet.getBoolean("entered"));
        supplierInvoice.setStockInfluencing(resultSet.getBoolean("stock_influencing"));
        supplierInvoice.setBGCEntered(resultSet.getBoolean("bgc_entered"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
        if (voucherNumber != null) {
            supplierInvoice.setVoucher(new SSVoucher(voucherNumber));
        }

        Integer correctionVoucherId = (Integer) resultSet.getObject("correction_voucher_id");
        Integer correctionVoucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, correctionVoucherId);
        if (correctionVoucherNumber != null) {
            supplierInvoice.setCorrection(new SSVoucher(correctionVoucherNumber));
        }

        supplierInvoice.getRows().clear();
        supplierInvoice.getRows().addAll(getSupplierInvoiceRowsV2(resultSet.getInt("id")));
        return supplierInvoice;
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
            LOG.error("Failed to rollback supplier invoice transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
