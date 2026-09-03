package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 result-unit repository backed by the normalized V2 schema.
 */
public class V2ResultUnitRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2ResultUnitRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2ResultUnitRepository(Connection connection,
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

    public List<SSNewResultUnit> findAll() {
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSNewResultUnit> resultUnits = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_resultunit WHERE companyid=?")) {
                statement.setObject(1, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        resultUnits.add(mapResultUnit(resultSet));
                    }
                }
            }
            return resultUnits;
        } catch (SQLException e) {
            throw handleFailure("load result units", e);
        }
    }

    public List<SSNewResultUnit> findAll(List<SSNewResultUnit> pResultUnits) {
        if (pResultUnits == null) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSNewResultUnit> resultUnits = new LinkedList<>();
            for (SSNewResultUnit resultUnit : pResultUnits) {
                if (resultUnit == null || resultUnit.getNumber() == null) {
                    continue;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_resultunit WHERE number=? AND companyid=?")) {
                    statement.setObject(1, resultUnit.getNumber());
                    statement.setObject(2, company.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            resultUnits.add(mapResultUnit(resultSet));
                        }
                    }
                }
            }
            return resultUnits;
        } catch (SQLException e) {
            throw handleFailure("filter result units", e);
        }
    }

    public Optional<SSNewResultUnit> findById(SSNewResultUnit pResultUnit) {
        if (pResultUnit == null) {
            return Optional.empty();
        }
        return findByNumber(pResultUnit.getNumber());
    }

    public Optional<SSNewResultUnit> findByNumber(String pResultUnitNumber) {
        if (pResultUnitNumber == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_resultunit WHERE number=? AND companyid=?")) {
                statement.setObject(1, pResultUnitNumber);
                statement.setObject(2, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultUnit(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find result unit by number '" + pResultUnitNumber + "'", e);
        }
    }

    public void add(SSNewResultUnit pResultUnit) {
        if (pResultUnit == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_resultunit(number, companyid, name, description) VALUES(?,?,?,?)")) {
            statement.setObject(1, pResultUnit.getNumber());
            statement.setObject(2, company.getId());
            statement.setObject(3, pResultUnit.getName());
            statement.setObject(4, pResultUnit.getDescription());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWRESULTUNIT", "TBL_RESULTUNIT", pResultUnit.getNumber());
        } catch (SQLException e) {
            throw handleFailure("add result unit '" + pResultUnit.getNumber() + "'", e);
        }
    }

    public void update(SSNewResultUnit pResultUnit) {
        if (pResultUnit == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tbl_resultunit SET name=?, description=? WHERE number=? AND companyid=?")) {
            statement.setObject(1, pResultUnit.getName());
            statement.setObject(2, pResultUnit.getDescription());
            statement.setObject(3, pResultUnit.getNumber());
            statement.setObject(4, company.getId());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITRESULTUNIT", "TBL_RESULTUNIT", pResultUnit.getNumber());
        } catch (SQLException e) {
            throw handleFailure("update result unit '" + pResultUnit.getNumber() + "'", e);
        }
    }

    public void delete(SSNewResultUnit pResultUnit) {
        if (pResultUnit == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tbl_resultunit WHERE number=? AND companyid=?")) {
            statement.setObject(1, pResultUnit.getNumber());
            statement.setObject(2, company.getId());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETERESULTUNIT", "TBL_RESULTUNIT", pResultUnit.getNumber());
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for result unit '{}' because it is in use", pResultUnit.getNumber());
                return;
            }
            throw handleFailure("delete result unit '" + pResultUnit.getNumber() + "'", e);
        }
    }

    private SSNewResultUnit mapResultUnit(ResultSet resultSet) throws SQLException {
        SSNewResultUnit resultUnit = new SSNewResultUnit();
        resultUnit.setNumber(resultSet.getString("number"));
        resultUnit.setName(resultSet.getString("name"));
        resultUnit.setDescription(resultSet.getString("description"));
        return resultUnit;
    }

    private boolean isForeignKeyConstraintViolation(SQLException e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLException) {
                String state = ((SQLException) current).getSQLState();
                if (state != null && state.startsWith("23")) {
                    return true;
                }
            }
        }
        return false;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback result unit transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}

