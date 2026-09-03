package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSNewCompany;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * V2 inpayment repository backed by the normalized V2 schema.
 */
public class V2InpaymentRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2InpaymentRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 inpayment repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2InpaymentRepository(Connection connection,
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

    public List<SSInpayment> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSInpayment> inpayments = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_inpayment WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            inpayments.add(mapInpaymentV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return inpayments;
        } catch (SQLException e) {
            throw handleFailure("load inpayments", e);
        }
    }

    public Optional<SSInpayment> findByInpayment(SSInpayment inpayment) {
        if (inpayment == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_inpayment WHERE number=? AND companyid=?")) {
                statement.setObject(1, inpayment.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapInpaymentV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find inpayment '" + inpayment.getNumber() + "'", e);
        }
    }

    public void add(SSInpayment inpayment) {
        if (inpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer companyNumber = currentCompany.getAutoIncrement().getNumber("inpayment");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_inpayment WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Integer number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            inpayment.setNumber(number + 1);
                        } else {
                            inpayment.setNumber(companyNumber + 1);
                        }
                    } else {
                        inpayment.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer inpaymentId = null;
            Integer voucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, inpayment.getVoucher());
            Integer differenceVoucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, inpayment.getDifference());
            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO tbl_inpayment(number,companyid,vdate,itext,entered,voucher_id,difference_voucher_id) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStatement.setObject(1, inpayment.getNumber());
                insertStatement.setObject(2, currentCompany.getId());
                bindLocalDateV2(insertStatement, 3, inpayment.getLocalDate());
                insertStatement.setObject(4, inpayment.getText());
                insertStatement.setObject(5, inpayment.isEntered());
                insertStatement.setObject(6, voucherId);
                insertStatement.setObject(7, differenceVoucherId);
                insertStatement.executeUpdate();

                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        inpaymentId = keys.getInt(1);
                    }
                }
            }

            if (inpaymentId == null) {
                inpaymentId = getInpaymentIdV2(inpayment.getNumber(), currentCompany.getId());
            }
            if (inpaymentId != null) {
                replaceInpaymentRowsV2(inpaymentId, inpayment);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWINPAYMENT", "TBL_INPAYMENT",
                    String.valueOf(inpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add inpayment '" + inpayment.getNumber() + "'", e);
        }
    }

    public void update(SSInpayment inpayment) {
        if (inpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer voucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, inpayment.getVoucher());
            Integer differenceVoucherId = V2RepositoryHelpers.getVoucherIdByNumber(connection, inpayment.getDifference());
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_inpayment SET vdate=?,itext=?,entered=?,voucher_id=?,difference_voucher_id=? WHERE number=? AND companyid=?")) {
                bindLocalDateV2(statement, 1, inpayment.getLocalDate());
                statement.setObject(2, inpayment.getText());
                statement.setObject(3, inpayment.isEntered());
                statement.setObject(4, voucherId);
                statement.setObject(5, differenceVoucherId);
                statement.setObject(6, inpayment.getNumber());
                statement.setObject(7, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer inpaymentId = getInpaymentIdV2(inpayment.getNumber(), currentCompany.getId());
            if (inpaymentId != null) {
                replaceInpaymentRowsV2(inpaymentId, inpayment);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITINPAYMENT", "TBL_INPAYMENT",
                    String.valueOf(inpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update inpayment '" + inpayment.getNumber() + "'", e);
        }
    }

    public void delete(SSInpayment inpayment) {
        if (inpayment == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer inpaymentId = getInpaymentIdV2(inpayment.getNumber(), currentCompany.getId());
            if (inpaymentId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_inpayment_row WHERE inpayment_id=?")) {
                    deleteRows.setObject(1, inpaymentId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_inpayment WHERE number=? AND companyid=?")) {
                statement.setObject(1, inpayment.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEINPAYMENT", "TBL_INPAYMENT",
                    String.valueOf(inpayment.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete inpayment '" + inpayment.getNumber() + "'", e);
        }
    }

    private List<SSInpaymentRow> getInpaymentRowsV2(Integer inpaymentId) throws SQLException {
        List<SSInpaymentRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_inpayment_row WHERE inpayment_id=? ORDER BY id")) {
            statement.setObject(1, inpaymentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSInpaymentRow row = new SSInpaymentRow();
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

    private void replaceInpaymentRowsV2(Integer inpaymentId, SSInpayment inpayment) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_inpayment_row WHERE inpayment_id=?")) {
            delete.setObject(1, inpaymentId);
            delete.executeUpdate();
        }

        for (SSInpaymentRow row : inpayment.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_inpayment_row(inpayment_id,invoice_nr,invoice_currency_code,invoice_currency_rate,value,currency_rate) VALUES(?,?,?,?,?,?)")) {
                insert.setObject(1, inpaymentId);
                insert.setObject(2, row.getInvoiceNr());
                insert.setObject(3, row.getInvoiceCurrency() == null ? null : row.getInvoiceCurrency().getName());
                insert.setObject(4, row.getInvoiceCurrencyRate());
                insert.setObject(5, row.getValue());
                insert.setObject(6, row.getCurrencyRate());
                insert.executeUpdate();
            }
        }
    }

    private Integer getInpaymentIdV2(Integer inpaymentNumber, Integer companyId) throws SQLException {
        if (inpaymentNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_inpayment WHERE number=? AND companyid=?")) {
            statement.setObject(1, inpaymentNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private SSInpayment mapInpaymentV2(ResultSet resultSet) throws SQLException {
        SSInpayment inpayment = new SSInpayment();
        inpayment.setNumber((Integer) resultSet.getObject("number"));

        Date date = resultSet.getDate("vdate");
        if (date != null) {
            inpayment.setLocalDate(date.toLocalDate());
        }

        inpayment.setText(resultSet.getString("itext"));
        inpayment.setEntered(resultSet.getBoolean("entered"));

        Integer voucherId = (Integer) resultSet.getObject("voucher_id");
        Integer voucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, voucherId);
        if (voucherNumber != null) {
            inpayment.setVoucher(new SSVoucher(voucherNumber));
        }

        Integer differenceVoucherId = (Integer) resultSet.getObject("difference_voucher_id");
        Integer differenceVoucherNumber = V2RepositoryHelpers.getVoucherNumberForId(connection, differenceVoucherId);
        if (differenceVoucherNumber != null) {
            inpayment.setDifference(new SSVoucher(differenceVoucherNumber));
        }

        inpayment.getRows().clear();
        inpayment.getRows().addAll(getInpaymentRowsV2(resultSet.getInt("id")));
        return inpayment;
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
            LOG.error("Failed to rollback inpayment transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}

