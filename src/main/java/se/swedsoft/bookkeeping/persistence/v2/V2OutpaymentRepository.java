package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
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
 * V2 outpayment repository backed by the normalized V2 schema.
 */
public class V2OutpaymentRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2OutpaymentRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 outpayment repository.
     *
     * @param connection             the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler        rollback delegate; must not be {@code null}
     */
    public V2OutpaymentRepository(Connection connection,
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

    public List<SSOutpayment> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSOutpayment> outpayments = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_outpayment WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            outpayments.add(mapOutpaymentV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return outpayments;
        } catch (SQLException e) {
            throw handleFailure("load outpayments", e);
        }
    }

    public Optional<SSOutpayment> findByOutpayment(SSOutpayment outpayment) {
        if (outpayment == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_outpayment WHERE number=? AND companyid=?")) {
                statement.setObject(1, outpayment.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapOutpaymentV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find outpayment '" + outpayment.getNumber() + "'", e);
        }
    }

    public void add(SSOutpayment outpayment) {
        if (outpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("outpayment");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_outpayment WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            outpayment.setNumber(number + 1);
                        } else {
                            outpayment.setNumber(companyNumber + 1);
                        }
                    } else {
                        outpayment.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer outpaymentId = null;
            Integer voucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, outpayment.getVoucher());
            Integer differenceVoucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, outpayment.getDifference());
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_outpayment(number,companyid,vdate,itext,entered,voucher_id,difference_voucher_id) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStatement.setObject(1, outpayment.getNumber());
                insertStatement.setObject(2, currentCompany.getId());
                bindLocalDateV2(insertStatement, 3, outpayment.getLocalDate());
                insertStatement.setObject(4, outpayment.getText());
                insertStatement.setObject(5, outpayment.isEntered());
                insertStatement.setObject(6, voucherId);
                insertStatement.setObject(7, differenceVoucherId);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        outpaymentId = keys.getInt(1);
                    }
                }
            }

            if (outpaymentId == null) {
                outpaymentId = getOutpaymentIdV2(outpayment.getNumber(), currentCompany.getId());
            }
            if (outpaymentId != null) {
                replaceOutpaymentRowsV2(outpaymentId, outpayment);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWOUTPAYMENT", "TBL_OUTPAYMENT",
                    String.valueOf(outpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add outpayment '" + outpayment.getNumber() + "'", e);
        }
    }

    public void update(SSOutpayment outpayment) {
        if (outpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer voucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, outpayment.getVoucher());
            Integer differenceVoucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, outpayment.getDifference());
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_outpayment SET vdate=?,itext=?,entered=?,voucher_id=?,difference_voucher_id=? WHERE number=? AND companyid=?")) {
                bindLocalDateV2(statement, 1, outpayment.getLocalDate());
                statement.setObject(2, outpayment.getText());
                statement.setObject(3, outpayment.isEntered());
                statement.setObject(4, voucherId);
                statement.setObject(5, differenceVoucherId);
                statement.setObject(6, outpayment.getNumber());
                statement.setObject(7, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer outpaymentId = getOutpaymentIdV2(outpayment.getNumber(), currentCompany.getId());
            if (outpaymentId != null) {
                replaceOutpaymentRowsV2(outpaymentId, outpayment);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITOUTPAYMENT", "TBL_OUTPAYMENT",
                    String.valueOf(outpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update outpayment '" + outpayment.getNumber() + "'", e);
        }
    }

    public void delete(SSOutpayment outpayment) {
        if (outpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer outpaymentId = getOutpaymentIdV2(outpayment.getNumber(), currentCompany.getId());
            if (outpaymentId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_outpayment_row WHERE outpayment_id=?")) {
                    deleteRows.setObject(1, outpaymentId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_outpayment WHERE number=? AND companyid=?")) {
                statement.setObject(1, outpayment.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEOUTPAYMENT", "TBL_OUTPAYMENT",
                    String.valueOf(outpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete outpayment '" + outpayment.getNumber() + "'", e);
        }
    }

    private List<SSOutpaymentRow> getOutpaymentRowsV2(Integer outpaymentId) throws SQLException {
        List<SSOutpaymentRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_outpayment_row WHERE outpayment_id=? ORDER BY id")) {
            statement.setObject(1, outpaymentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSOutpaymentRow row = new SSOutpaymentRow();
                    row.setInvoiceNr((Integer) resultSet.getObject("invoice_nr"));

                    String currencyCode = resultSet.getString("invoice_currency_code");
                    if (currencyCode != null) {
                        row.setInvoiceCurrency(new SSCurrency(currencyCode, currencyCode));
                    }

                    row.setInvoiceCurrencyRate(resultSet.getBigDecimal("invoice_currency_rate"));
                    row.setValue(resultSet.getBigDecimal("value"));
                    row.setCurrencyRate(resultSet.getBigDecimal("currency_rate"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceOutpaymentRowsV2(Integer outpaymentId, SSOutpayment outpayment) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_outpayment_row WHERE outpayment_id=?")) {
            delete.setObject(1, outpaymentId);
            delete.executeUpdate();
        }

        for (SSOutpaymentRow row : outpayment.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_outpayment_row(outpayment_id,invoice_nr,invoice_currency_code,invoice_currency_rate,value,currency_rate) VALUES(?,?,?,?,?,?)")) {
                insert.setObject(1, outpaymentId);
                insert.setObject(2, row.getInvoiceNr());
                insert.setObject(3, row.getInvoiceCurrency() == null ? null : row.getInvoiceCurrency().getName());
                insert.setObject(4, row.getInvoiceCurrencyRate());
                insert.setObject(5, row.getValue());
                insert.setObject(6, row.getCurrencyRate());
                insert.executeUpdate();
            }
        }
    }

    private Integer getOutpaymentIdV2(Integer outpaymentNumber, Integer companyId) throws SQLException {
        if (outpaymentNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_outpayment WHERE number=? AND companyid=?")) {
            statement.setObject(1, outpaymentNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private SSOutpayment mapOutpaymentV2(ResultSet resultSet) throws SQLException {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            outpayment.setLocalDate(date.toLocalDate());
        }

        outpayment.setText(resultSet.getString("itext"));
        outpayment.setEntered(resultSet.getBoolean("entered"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
        if (voucherNumber != null) {
            outpayment.setVoucher(new SSVoucher(voucherNumber));
        }

        Integer differenceVoucherId = (Integer) resultSet.getObject("difference_voucher_id");
        Integer differenceVoucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, differenceVoucherId);
        if (differenceVoucherNumber != null) {
            outpayment.setDifference(new SSVoucher(differenceVoucherNumber));
        }

        outpayment.getRows().clear();
        outpayment.getRows().addAll(getOutpaymentRowsV2(resultSet.getInt("id")));
        return outpayment;
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
            LOG.error("Failed to rollback outpayment transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
