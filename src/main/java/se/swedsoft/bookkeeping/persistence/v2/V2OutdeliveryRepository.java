package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.SSOutdeliveryRow;
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
 * V2 outdelivery repository backed by the normalized V2 schema.
 */
public class V2OutdeliveryRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2OutdeliveryRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 outdelivery repository with explicit DB dependencies.
     *
     * @param connection             the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for current company; must not be {@code null}
     * @param rollbackHandler        rollback delegate; must not be {@code null}
     */
    public V2OutdeliveryRepository(Connection connection,
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

    /**
     * Returns all outdeliveries for the current company.
     *
     * @return list of outdeliveries, or empty list if no company is set
     */
    public List<SSOutdelivery> findAll() {
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSOutdelivery> outdeliveries = new LinkedList<>();
            int max = -1;
            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_outdelivery WHERE companyid=? AND id>?")) {
                    statement.setObject(1, company.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            outdeliveries.add(mapOutdelivery(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }
            return outdeliveries;
        } catch (SQLException e) {
            throw handleFailure("load outdeliveries", e);
        }
    }

    /**
     * Finds a single outdelivery by number for the current company.
     *
     * @param outdelivery the outdelivery with number set; must not be {@code null}
     * @return the found outdelivery, or empty
     */
    public Optional<SSOutdelivery> findByOutdelivery(SSOutdelivery outdelivery) {
        if (outdelivery == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_outdelivery WHERE number=? AND companyid=?")) {
                statement.setObject(1, outdelivery.getNumber());
                statement.setObject(2, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapOutdelivery(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find outdelivery '" + outdelivery.getNumber() + "'", e);
        }
    }

    /**
     * Creates a new empty outdelivery instance.
     *
     * @return new SSOutdelivery
     */
    public SSOutdelivery createNew() {
        SSOutdelivery outdelivery = new SSOutdelivery();
        outdelivery.doAutoIncrement();
        return outdelivery;
    }

    /**
     * Persists a new outdelivery and its rows.
     *
     * @param outdelivery the outdelivery to add; must not be {@code null}
     */
    public void add(SSOutdelivery outdelivery) {
        if (outdelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            int companyNumber = company.getAutoIncrement().getNumber("outdelivery");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_outdelivery WHERE companyid=?")) {
                statement.setObject(1, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            outdelivery.setNumber(number + 1);
                        } else {
                            outdelivery.setNumber(companyNumber + 1);
                        }
                    } else {
                        outdelivery.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer outdeliveryId = null;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_outdelivery(number,companyid,vdate,itext) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setObject(1, outdelivery.getNumber());
                insert.setObject(2, company.getId());
                bindLocalDate(insert, 3, outdelivery.getLocalDate());
                insert.setObject(4, outdelivery.getText());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        outdeliveryId = keys.getInt(1);
                    }
                }
            }

            if (outdeliveryId == null) {
                outdeliveryId = getOutdeliveryId(outdelivery.getNumber(), company.getId());
            }
            if (outdeliveryId != null) {
                replaceOutdeliveryRows(outdeliveryId, outdelivery);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWOUTDELIVERY", "TBL_OUTDELIVERY",
                    String.valueOf(outdelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add outdelivery '" + outdelivery.getNumber() + "'", e);
        }
    }

    /**
     * Updates an existing outdelivery and replaces its rows.
     *
     * @param outdelivery the outdelivery to update; must not be {@code null}
     */
    public void update(SSOutdelivery outdelivery) {
        if (outdelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_outdelivery SET vdate=?,itext=? WHERE number=? AND companyid=?")) {
                bindLocalDate(statement, 1, outdelivery.getLocalDate());
                statement.setObject(2, outdelivery.getText());
                statement.setObject(3, outdelivery.getNumber());
                statement.setObject(4, company.getId());
                statement.executeUpdate();
            }

            Integer outdeliveryId = getOutdeliveryId(outdelivery.getNumber(), company.getId());
            if (outdeliveryId != null) {
                replaceOutdeliveryRows(outdeliveryId, outdelivery);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITOUTDELIVERY", "TBL_OUTDELIVERY",
                    String.valueOf(outdelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update outdelivery '" + outdelivery.getNumber() + "'", e);
        }
    }

    /**
     * Deletes an outdelivery and its rows.
     *
     * @param outdelivery the outdelivery to delete; must not be {@code null}
     */
    public void delete(SSOutdelivery outdelivery) {
        if (outdelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            Integer outdeliveryId = getOutdeliveryId(outdelivery.getNumber(), company.getId());
            if (outdeliveryId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_outdelivery_row WHERE outdelivery_id=?")) {
                    deleteRows.setObject(1, outdeliveryId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_outdelivery WHERE number=? AND companyid=?")) {
                statement.setObject(1, outdelivery.getNumber());
                statement.setObject(2, company.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEOUTDELIVERY", "TBL_OUTDELIVERY",
                    String.valueOf(outdelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete outdelivery '" + outdelivery.getNumber() + "'", e);
        }
    }

    private SSOutdelivery mapOutdelivery(ResultSet resultSet) throws SQLException {
        SSOutdelivery outdelivery = new SSOutdelivery(false);
        outdelivery.setNumber((Integer) resultSet.getObject("number"));
        Date date = resultSet.getDate("vdate");
        if (date != null) {
            outdelivery.setLocalDate(date.toLocalDate());
        }
        outdelivery.setText(resultSet.getString("itext"));
        outdelivery.getRows().clear();
        outdelivery.getRows().addAll(getOutdeliveryRows(resultSet.getInt("id")));
        return outdelivery;
    }

    private List<SSOutdeliveryRow> getOutdeliveryRows(Integer outdeliveryId) throws SQLException {
        List<SSOutdeliveryRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_outdelivery_row WHERE outdelivery_id=? ORDER BY id")) {
            statement.setObject(1, outdeliveryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSOutdeliveryRow row = new SSOutdeliveryRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setChange((Integer) resultSet.getObject("change_qty"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceOutdeliveryRows(Integer outdeliveryId, SSOutdelivery outdelivery) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_outdelivery_row WHERE outdelivery_id=?")) {
            delete.setObject(1, outdeliveryId);
            delete.executeUpdate();
        }

        for (SSOutdeliveryRow row : outdelivery.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_outdelivery_row(outdelivery_id,product_nr,change_qty) VALUES(?,?,?)")) {
                insert.setObject(1, outdeliveryId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getChange());
                insert.executeUpdate();
            }
        }
    }

    private Integer getOutdeliveryId(Integer outdeliveryNumber, Integer companyId) throws SQLException {
        if (outdeliveryNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_outdelivery WHERE number=? AND companyid=?")) {
            statement.setObject(1, outdeliveryNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private void bindLocalDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        statement.setDate(index, date == null ? null : Date.valueOf(date));
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback outdelivery transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
