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

import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;


/**
 * V2 payment-term repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes payment terms directly against {@code tbl_paymentterm}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2PaymentTermRepository {

    public static final String FAIL_UPDATE_PROPERTY = "fribok.test.paymentterm.update.fail.after.execute";

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2PaymentTermRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 payment-term repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2PaymentTermRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSPaymentTerm> findAll() {
        try {
            List<SSPaymentTerm> list = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description, days FROM PUBLIC.tbl_paymentterm");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSPaymentTerm paymentTerm = new SSPaymentTerm();
                    paymentTerm.setName(resultSet.getString("name"));
                    paymentTerm.setDescription(resultSet.getString("description"));
                    paymentTerm.setDays((Integer) resultSet.getObject("days"));
                    list.add(paymentTerm);
                }
            }
            return list;
        } catch (SQLException e) {
            throw handleFailure("load payment terms", e);
        }
    }

    public Optional<SSPaymentTerm> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            SSPaymentTerm paymentTerm = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, description, days FROM PUBLIC.tbl_paymentterm WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        paymentTerm = new SSPaymentTerm();
                        paymentTerm.setName(resultSet.getString("name"));
                        paymentTerm.setDescription(resultSet.getString("description"));
                        paymentTerm.setDays((Integer) resultSet.getObject("days"));
                    }
                }
            }
            return Optional.ofNullable(paymentTerm);
        } catch (SQLException e) {
            throw handleFailure("load payment term '" + name + "'", e);
        }
    }

    public void add(SSPaymentTerm paymentTerm) {
        if (paymentTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_paymentterm(name, description, days) VALUES(?,?,?)")) {
                statement.setString(1, paymentTerm.getName());
                statement.setString(2, paymentTerm.getDescription());
                statement.setObject(3, paymentTerm.getDays());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("add payment term '" + paymentTerm.getName() + "'", e);
        }
    }

    public void update(SSPaymentTerm paymentTerm) {
        if (paymentTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_paymentterm SET description=?, days=? WHERE name=?")) {
                statement.setString(1, paymentTerm.getDescription());
                statement.setObject(2, paymentTerm.getDays());
                statement.setString(3, paymentTerm.getName());
                statement.executeUpdate();
                if (Boolean.getBoolean(FAIL_UPDATE_PROPERTY)) {
                    throw new SQLException("Controlled test failure after payment term update execution");
                }
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("update payment term '" + paymentTerm.getName() + "'", e);
        }
    }

    public void delete(SSPaymentTerm paymentTerm) {
        if (paymentTerm == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_paymentterm WHERE name=?")) {
                statement.setString(1, paymentTerm.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for payment term '{}' because it is in use", paymentTerm.getName());
                return;
            }
            throw handleFailure("delete payment term '" + paymentTerm.getName() + "'", e);
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
            LOG.error("Failed to rollback payment term transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
