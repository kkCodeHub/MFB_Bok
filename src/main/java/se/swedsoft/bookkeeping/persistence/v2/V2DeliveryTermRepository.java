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

import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;


/**
 * V2 delivery-term repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes delivery terms directly against {@code tbl_deliveryterm}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2DeliveryTermRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2DeliveryTermRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 delivery-term repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2DeliveryTermRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSDeliveryTerm> findAll() {
        try {
            List<SSDeliveryTerm> list = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_deliveryterm");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSDeliveryTerm d = new SSDeliveryTerm();
                    d.setName(resultSet.getString("name"));
                    d.setDescription(resultSet.getString("description"));
                    list.add(d);
                }
            }
            return list;
        } catch (SQLException e) {
            throw handleFailure("load delivery terms", e);
        }
    }

    public Optional<SSDeliveryTerm> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            SSDeliveryTerm d = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_deliveryterm WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        d = new SSDeliveryTerm();
                        d.setName(resultSet.getString("name"));
                        d.setDescription(resultSet.getString("description"));
                    }
                }
            }
            return Optional.ofNullable(d);
        } catch (SQLException e) {
            throw handleFailure("load delivery term '" + name + "'", e);
        }
    }

    public void add(SSDeliveryTerm deliveryTerm) {
        if (deliveryTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_deliveryterm(name, description) VALUES(?,?)")) {
                statement.setString(1, deliveryTerm.getName());
                statement.setString(2, deliveryTerm.getDescription());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("add delivery term '" + deliveryTerm.getName() + "'", e);
        }
    }

    public void update(SSDeliveryTerm deliveryTerm) {
        if (deliveryTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_deliveryterm SET description=? WHERE name=?")) {
                statement.setString(1, deliveryTerm.getDescription());
                statement.setString(2, deliveryTerm.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("update delivery term '" + deliveryTerm.getName() + "'", e);
        }
    }

    public void delete(SSDeliveryTerm deliveryTerm) {
        if (deliveryTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_deliveryterm WHERE name=?")) {
                statement.setString(1, deliveryTerm.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for delivery term '{}' because it is in use", deliveryTerm.getName());
                return;
            }
            throw handleFailure("delete delivery term '" + deliveryTerm.getName() + "'", e);
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
            LOG.error("Failed to rollback delivery term transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
