package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Full SQL V2 repository for the Voucher domain.
 */
public class V2VoucherRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2VoucherRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final Supplier<SSNewAccountingYear> currentYearSupplier;
    private final RollbackHandler rollbackHandler;

    public V2VoucherRepository(Connection connection,
                               Supplier<SSNewCompany> currentCompanySupplier,
                               Supplier<SSNewAccountingYear> currentYearSupplier,
                               RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (currentCompanySupplier == null) {
            throw new NullPointerException("currentCompanySupplier must not be null");
        }
        if (currentYearSupplier == null) {
            throw new NullPointerException("currentYearSupplier must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.currentCompanySupplier = currentCompanySupplier;
        this.currentYearSupplier = currentYearSupplier;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSVoucher> findByYear(SSNewAccountingYear year) {
        if (year == null || year.getId() == null) {
            return Collections.emptyList();
        }
        try {
            List<SSVoucher> vouchers = new LinkedList<>();
            Integer maxId = -1;
            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_voucher WHERE yearid=? AND id>?")) {
                    statement.setObject(1, year.getId());
                    statement.setObject(2, maxId);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            maxId = resultSet.getInt("id");
                            vouchers.add(mapVoucherV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }
            return vouchers;
        } catch (SQLException e) {
            throw handleFailure("load vouchers for year '" + year.getId() + "'", e);
        }
    }

    public Optional<SSVoucher> findByNumber(SSNewAccountingYear year, int number) {
        if (year == null || year.getId() == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_voucher WHERE number=? AND yearid=?")) {
                statement.setObject(1, number);
                statement.setObject(2, year.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapVoucherV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find voucher '" + number + "'", e);
        }
    }

    public Optional<SSVoucher> findVoucher(SSVoucher voucher) {
        if (voucher == null) {
            return Optional.empty();
        }
        SSNewAccountingYear currentYear = currentYearSupplier.get();
        if (currentYear == null || currentYear.getId() == null) {
            return Optional.empty();
        }
        return findByNumber(currentYear, voucher.getNumber());
    }

    public void add(SSVoucher voucher) {
        save(voucher, true);
    }

    public void update(SSVoucher voucher) {
        SSNewAccountingYear year = resolveYear(voucher);
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_voucher SET vdate=?,description=?,corrects_id=?,corrected_by_id=? WHERE number=? AND yearid=?")) {
                statement.setObject(1, java.sql.Date.valueOf(voucher.getLocalDate()));
                statement.setObject(2, voucher.getDescription());
                statement.setObject(3, getVoucherIdByNumberV2(voucher.getCorrects(), year.getId()));
                statement.setObject(4, getVoucherIdByNumberV2(voucher.getCorrectedBy(), year.getId()));
                statement.setObject(5, voucher.getNumber());
                statement.setObject(6, year.getId());
                statement.executeUpdate();
            }

            Integer voucherId = getVoucherIdV2(voucher.getNumber(), year.getId());
            if (voucherId != null) {
                replaceVoucherRowsV2(voucherId, voucher);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction(
                    "EDITVOUCHER", "TBL_VOUCHER", String.valueOf(voucher.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update voucher '" + voucher.getNumber() + "'", e);
        }
    }

    public void delete(SSVoucher voucher) {
        SSNewAccountingYear year = resolveYear(voucher);
        try {
            Integer voucherId = getVoucherIdV2(voucher.getNumber(), year.getId());
            if (voucherId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_voucher_row WHERE voucher_id=?")) {
                    deleteRows.setObject(1, voucherId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_voucher WHERE number=? AND yearid=?")) {
                statement.setObject(1, voucher.getNumber());
                statement.setObject(2, year.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction(
                    "DELETEVOUCHER", "TBL_VOUCHER", String.valueOf(voucher.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete voucher '" + voucher.getNumber() + "'", e);
        }
    }

    public void addWithAutoNumber(SSVoucher voucher) {
        save(voucher, false);
    }

    public int findLastNumber() {
        SSNewAccountingYear currentYear = currentYearSupplier.get();
        if (currentYear == null || currentYear.getId() == null) {
            return 0;
        }
        try {
            return findLastNumber(currentYear.getId());
        } catch (SQLException e) {
            throw handleFailure("find last voucher number", e);
        }
    }

    private void save(SSVoucher voucher, boolean hasNumber) {
        SSNewAccountingYear year = resolveYear(voucher);
        try {
            if (!hasNumber) {
                voucher.setNumber(findLastNumber(year.getId()) + 1);
            }

            Integer voucherId = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_voucher(number,yearid,vdate,description,corrects_id,corrected_by_id) VALUES(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setObject(1, voucher.getNumber());
                statement.setObject(2, year.getId());
                statement.setObject(3, java.sql.Date.valueOf(voucher.getLocalDate()));
                statement.setObject(4, voucher.getDescription());
                statement.setObject(5, getVoucherIdByNumberV2(voucher.getCorrects(), year.getId()));
                statement.setObject(6, getVoucherIdByNumberV2(voucher.getCorrectedBy(), year.getId()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        voucherId = keys.getInt(1);
                    }
                }
            }

            if (voucherId == null) {
                voucherId = getVoucherIdV2(voucher.getNumber(), year.getId());
            }
            if (voucherId != null) {
                replaceVoucherRowsV2(voucherId, voucher);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction(
                    "NEWVOUCHER", "TBL_VOUCHER", String.valueOf(voucher.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add voucher '" + voucher.getNumber() + "'", e);
        }
    }

    private SSNewAccountingYear resolveYear(SSVoucher voucher) {
        if (voucher == null) {
            throw new NullPointerException("voucher must not be null");
        }
        LocalDate voucherDate = voucher.getLocalDate();
        if (voucherDate == null) {
            throw new IllegalArgumentException("voucher date must be set to resolve accounting year");
        }

        SSNewAccountingYear currentYear = currentYearSupplier.get();
        if (matchesYear(currentYear, voucherDate)) {
            return currentYear;
        }

        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            throw new IllegalArgumentException("current company must be set to resolve accounting year");
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,from_date,to_date FROM tbl_accountingyear WHERE companyid=? AND from_date<=? AND to_date>=? ORDER BY from_date DESC")) {
            statement.setObject(1, company.getId());
            statement.setDate(2, java.sql.Date.valueOf(voucherDate));
            statement.setDate(3, java.sql.Date.valueOf(voucherDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    SSNewAccountingYear year = new SSNewAccountingYear();
                    year.setId(resultSet.getInt("id"));
                    java.sql.Date from = resultSet.getDate("from_date");
                    java.sql.Date to = resultSet.getDate("to_date");
                    if (from != null) {
                        year.setLocalFrom(from.toLocalDate());
                    }
                    if (to != null) {
                        year.setLocalTo(to.toLocalDate());
                    }
                    return year;
                }
            }
        } catch (SQLException e) {
            throw handleFailure("resolve accounting year for voucher date '" + voucherDate + "'", e);
        }

        throw new IllegalArgumentException("No accounting year matches voucher date: " + voucherDate);
    }

    private boolean matchesYear(SSNewAccountingYear year, LocalDate voucherDate) {
        if (year == null) {
            return false;
        }
        LocalDate from = year.getLocalFrom();
        LocalDate to = year.getLocalTo();
        return from != null && to != null && !voucherDate.isBefore(from) && !voucherDate.isAfter(to);
    }

    private int findLastNumber(Integer yearId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(number) AS maxnum FROM tbl_voucher WHERE yearid=?")) {
            statement.setObject(1, yearId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("maxnum");
                }
                return 0;
            }
        }
    }

    private Integer getVoucherIdByNumberV2(SSVoucher voucher, Integer yearId) throws SQLException {
        if (voucher == null) {
            return null;
        }
        return getVoucherIdV2(voucher.getNumber(), yearId);
    }

    private Integer getVoucherIdV2(Integer voucherNumber, Integer yearId) throws SQLException {
        if (voucherNumber == null || yearId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_voucher WHERE number=? AND yearid=?")) {
            statement.setObject(1, voucherNumber);
            statement.setObject(2, yearId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private Integer getVoucherNumberForIdV2(Integer voucherId) throws SQLException {
        if (voucherId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT number FROM tbl_voucher WHERE id=?")) {
            statement.setObject(1, voucherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private List<SSVoucherRow> getVoucherRowsV2(Integer voucherId) throws SQLException {
        List<SSVoucherRow> rows = new LinkedList<>();
        if (voucherId == null) {
            return rows;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_voucher_row WHERE voucher_id=? ORDER BY id")) {
            statement.setObject(1, voucherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSVoucherRow row = new SSVoucherRow();
                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    row.setProjectNr(resultSet.getString("project_number"));
                    row.setResultUnitNr(resultSet.getString("result_unit_number"));
                    row.setDebet(resultSet.getBigDecimal("debet"));
                    row.setCredit(resultSet.getBigDecimal("credit"));
                    row.setEditedDate(resultSet.getDate("edited_date"));
                    row.setEditedSignature(resultSet.getString("edited_signature"));
                    row.setCrossed(resultSet.getBoolean("crossed"));
                    row.setAdded(resultSet.getBoolean("added"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceVoucherRowsV2(Integer voucherId, SSVoucher voucher) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_voucher_row WHERE voucher_id=?")) {
            delete.setObject(1, voucherId);
            delete.executeUpdate();
        }

        if (voucher.getRows() == null) {
            return;
        }

        for (SSVoucherRow row : voucher.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_voucher_row(voucher_id,account_nr,project_number,result_unit_number,debet,credit,edited_date,edited_signature,crossed,added) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, voucherId);
                insert.setObject(2, row.getAccountNr());
                insert.setObject(3, row.getProjectNr());
                insert.setObject(4, row.getResultUnitNr());
                insert.setObject(5, row.getDebet());
                insert.setObject(6, row.getCredit());
                if (row.getEditedDate() == null) {
                    insert.setNull(7, Types.DATE);
                } else {
                    insert.setObject(7, new java.sql.Date(row.getEditedDate().getTime()));
                }
                insert.setObject(8, row.getEditedSignature());
                insert.setObject(9, row.isCrossed());
                insert.setObject(10, row.isAdded());
                insert.executeUpdate();
            }
        }
    }

    private SSVoucher mapVoucherV2(ResultSet resultSet) throws SQLException {
        SSVoucher voucher = new SSVoucher(resultSet.getInt("number"), true);
        java.sql.Date date = resultSet.getDate("vdate");
        if (date != null) {
            voucher.setLocalDate(date.toLocalDate());
        }
        voucher.setDescription(resultSet.getString("description"));

        Integer correctsId = (Integer) resultSet.getObject("corrects_id");
        Integer correctedById = (Integer) resultSet.getObject("corrected_by_id");
        Integer correctsNumber = getVoucherNumberForIdV2(correctsId);
        Integer correctedByNumber = getVoucherNumberForIdV2(correctedById);
        if (correctsNumber != null) {
            voucher.setCorrects(new SSVoucher(correctsNumber, true));
        }
        if (correctedByNumber != null) {
            voucher.setCorrectedBy(new SSVoucher(correctedByNumber, true));
        }

        voucher.getRows().clear();
        voucher.getRows().addAll(getVoucherRowsV2(resultSet.getInt("id")));
        return voucher;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback voucher transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
