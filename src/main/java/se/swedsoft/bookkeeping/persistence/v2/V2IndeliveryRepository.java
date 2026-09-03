package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSIndeliveryRow;
import se.swedsoft.bookkeeping.data.SSNewCompany;
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
 * V2 indelivery repository backed by the normalized V2 schema.
 */
public class V2IndeliveryRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2IndeliveryRepository.class);
    private static final ThreadLocal<Boolean> SUPPRESS_FIND_ALL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 indelivery repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2IndeliveryRepository(Connection connection,
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

    public List<SSIndelivery> findAll() {
        if (Boolean.TRUE.equals(SUPPRESS_FIND_ALL.get())) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSIndelivery> indeliveries = new LinkedList<>();
            int max = -1;
            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_indelivery WHERE companyid=? AND id>?")) {
                    statement.setObject(1, company.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            indeliveries.add(mapIndelivery(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }
            return indeliveries;
        } catch (SQLException e) {
            throw handleFailure("load indeliveries", e);
        }
    }

    public Optional<SSIndelivery> findByIndelivery(SSIndelivery indelivery) {
        if (indelivery == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_indelivery WHERE number=? AND companyid=?")) {
                statement.setObject(1, indelivery.getNumber());
                statement.setObject(2, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapIndelivery(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find indelivery '" + indelivery.getNumber() + "'", e);
        }
    }

    public SSIndelivery createNew() {
        SSIndelivery indelivery = new SSIndelivery();
        indelivery.doAutoIncrement();
        return indelivery;
    }

    public void add(SSIndelivery indelivery) {
        if (indelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            int companyNumber = company.getAutoIncrement().getNumber("indelivery");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT MAX(number) AS maxnum FROM tbl_indelivery WHERE companyid=?")) {
                statement.setObject(1, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int number = resultSet.getInt("maxnum");
                        if (number > companyNumber) {
                            indelivery.setNumber(number + 1);
                        } else {
                            indelivery.setNumber(companyNumber + 1);
                        }
                    } else {
                        indelivery.setNumber(companyNumber + 1);
                    }
                }
            }

            Integer indeliveryId = null;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_indelivery(number,companyid,vdate,itext) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setObject(1, indelivery.getNumber());
                insert.setObject(2, company.getId());
                bindLocalDate(insert, 3, indelivery.getLocalDate());
                insert.setObject(4, indelivery.getText());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        indeliveryId = keys.getInt(1);
                    }
                }
            }

            if (indeliveryId == null) {
                indeliveryId = getIndeliveryId(indelivery.getNumber(), company.getId());
            }
            if (indeliveryId != null) {
                replaceIndeliveryRows(indeliveryId, indelivery);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWINDELIVERY", "TBL_INDELIVERY", String.valueOf(indelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add indelivery '" + indelivery.getNumber() + "'", e);
        }
    }

    public void update(SSIndelivery indelivery) {
        if (indelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_indelivery SET vdate=?,itext=? WHERE number=? AND companyid=?")) {
                bindLocalDate(statement, 1, indelivery.getLocalDate());
                statement.setObject(2, indelivery.getText());
                statement.setObject(3, indelivery.getNumber());
                statement.setObject(4, company.getId());
                statement.executeUpdate();
            }

            Integer indeliveryId = getIndeliveryId(indelivery.getNumber(), company.getId());
            if (indeliveryId != null) {
                replaceIndeliveryRows(indeliveryId, indelivery);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITINDELIVERY", "TBL_INDELIVERY", String.valueOf(indelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update indelivery '" + indelivery.getNumber() + "'", e);
        }
    }

    public void delete(SSIndelivery indelivery) {
        if (indelivery == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try {
            Integer indeliveryId = getIndeliveryId(indelivery.getNumber(), company.getId());
            if (indeliveryId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_indelivery_row WHERE indelivery_id=?")) {
                    deleteRows.setObject(1, indeliveryId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_indelivery WHERE number=? AND companyid=?")) {
                statement.setObject(1, indelivery.getNumber());
                statement.setObject(2, company.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEINDELIVERY", "TBL_INDELIVERY", String.valueOf(indelivery.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete indelivery '" + indelivery.getNumber() + "'", e);
        }
    }

    private SSIndelivery mapIndelivery(ResultSet resultSet) throws SQLException {
        SSIndelivery indelivery;
        Boolean previousFlag = SUPPRESS_FIND_ALL.get();
        SUPPRESS_FIND_ALL.set(Boolean.TRUE);
        try {
            indelivery = new SSIndelivery();
        } finally {
            SUPPRESS_FIND_ALL.set(previousFlag);
        }
        indelivery.setNumber((Integer) resultSet.getObject("number"));
        Date date = resultSet.getDate("vdate");
        if (date != null) {
            indelivery.setLocalDate(date.toLocalDate());
        }
        indelivery.setText(resultSet.getString("itext"));
        indelivery.getRows().clear();
        indelivery.getRows().addAll(getIndeliveryRows(resultSet.getInt("id")));
        return indelivery;
    }

    private List<SSIndeliveryRow> getIndeliveryRows(Integer indeliveryId) throws SQLException {
        List<SSIndeliveryRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_indelivery_row WHERE indelivery_id=? ORDER BY id")) {
            statement.setObject(1, indeliveryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSIndeliveryRow row = new SSIndeliveryRow();
                    row.setProductNr(resultSet.getString("product_nr"));
                    row.setChange((Integer) resultSet.getObject("change_qty"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceIndeliveryRows(Integer indeliveryId, SSIndelivery indelivery) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_indelivery_row WHERE indelivery_id=?")) {
            delete.setObject(1, indeliveryId);
            delete.executeUpdate();
        }

        for (SSIndeliveryRow row : indelivery.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_indelivery_row(indelivery_id,product_nr,change_qty) VALUES(?,?,?)")) {
                insert.setObject(1, indeliveryId);
                insert.setObject(2, row.getProductNr());
                insert.setObject(3, row.getChange());
                insert.executeUpdate();
            }
        }
    }

    private Integer getIndeliveryId(Integer indeliveryNumber, Integer companyId) throws SQLException {
        if (indeliveryNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_indelivery WHERE number=? AND companyid=?")) {
            statement.setObject(1, indeliveryNumber);
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
            LOG.error("Failed to rollback indelivery transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
