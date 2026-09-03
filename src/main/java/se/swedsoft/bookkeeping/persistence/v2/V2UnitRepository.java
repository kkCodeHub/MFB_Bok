package se.swedsoft.bookkeeping.persistence.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.common.SSUnit;


/**
 * V2 unit repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes units directly against {@code tbl_unit}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2UnitRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2UnitRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 unit repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2UnitRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSUnit> findAll() {
        try {
            List<SSUnit> units = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_unit");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSUnit unit = new SSUnit();
                    unit.setName(resultSet.getString("name"));
                    unit.setDescription(resultSet.getString("description"));
                    units.add(unit);
                }
            }
            return units;
        } catch (SQLException e) {
            throw handleFailure("load units", e);
        }
    }

    public Optional<SSUnit> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            SSUnit unit = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_unit WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        unit = new SSUnit();
                        unit.setName(resultSet.getString("name"));
                        unit.setDescription(resultSet.getString("description"));
                    }
                }
            }
            return Optional.ofNullable(unit);
        } catch (SQLException e) {
            throw handleFailure("load unit '" + name + "'", e);
        }
    }

    public void add(SSUnit unit) {
        if (unit == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_unit(name, description) VALUES(?,?)")) {
                statement.setString(1, unit.getName());
                statement.setString(2, unit.getDescription());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("add unit '" + unit.getName() + "'", e);
        }
    }

    public void update(SSUnit unit) {
        if (unit == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_unit SET description=? WHERE name=?")) {
                statement.setString(1, unit.getDescription());
                statement.setString(2, unit.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("update unit '" + unit.getName() + "'", e);
        }
    }

    public void delete(SSUnit unit) {
        if (unit == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_unit WHERE name=?")) {
                statement.setString(1, unit.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for unit '{}' because it is in use", unit.getName());
                return;
            }
            throw handleFailure("delete unit '" + unit.getName() + "'", e);
        }
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
            LOG.error("Failed to rollback unit transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
