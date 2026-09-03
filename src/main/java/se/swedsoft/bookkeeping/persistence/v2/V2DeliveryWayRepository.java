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

import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;


/**
 * V2 delivery-way repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes delivery ways directly against {@code tbl_deliveryway}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2DeliveryWayRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2DeliveryWayRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 delivery-way repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2DeliveryWayRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSDeliveryWay> findAll() {
        try {
            List<SSDeliveryWay> list = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_deliveryway");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSDeliveryWay d = new SSDeliveryWay();
                    d.setName(resultSet.getString("name"));
                    d.setDescription(resultSet.getString("description"));
                    list.add(d);
                }
            }
            return list;
        } catch (SQLException e) {
            throw handleFailure("load delivery ways", e);
        }
    }

    public Optional<SSDeliveryWay> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            SSDeliveryWay d = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description FROM PUBLIC.tbl_deliveryway WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        d = new SSDeliveryWay();
                        d.setName(resultSet.getString("name"));
                        d.setDescription(resultSet.getString("description"));
                    }
                }
            }
            return Optional.ofNullable(d);
        } catch (SQLException e) {
            throw handleFailure("load delivery way '" + name + "'", e);
        }
    }

    public void add(SSDeliveryWay deliveryWay) {
        if (deliveryWay == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_deliveryway(name, description) VALUES(?,?)")) {
                statement.setString(1, deliveryWay.getName());
                statement.setString(2, deliveryWay.getDescription());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("add delivery way '" + deliveryWay.getName() + "'", e);
        }
    }

    public void update(SSDeliveryWay deliveryWay) {
        if (deliveryWay == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_deliveryway SET description=? WHERE name=?")) {
                statement.setString(1, deliveryWay.getDescription());
                statement.setString(2, deliveryWay.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("update delivery way '" + deliveryWay.getName() + "'", e);
        }
    }

    public void delete(SSDeliveryWay deliveryWay) {
        if (deliveryWay == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_deliveryway WHERE name=?")) {
                statement.setString(1, deliveryWay.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for delivery way '{}' because it is in use", deliveryWay.getName());
                return;
            }
            throw handleFailure("delete delivery way '" + deliveryWay.getName() + "'", e);
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
            LOG.error("Failed to rollback delivery way transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
