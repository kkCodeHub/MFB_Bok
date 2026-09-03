package se.swedsoft.bookkeeping.persistence.v2;

import java.math.BigDecimal;
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

import se.swedsoft.bookkeeping.data.common.SSCurrency;


/**
 * V2 currency repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes currencies directly against {@code tbl_currency}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2CurrencyRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2CurrencyRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 currency repository with explicit DB dependencies.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2CurrencyRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSCurrency> findAll() {
        try {
            List<SSCurrency> list = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT code, description, exchange_rate FROM PUBLIC.tbl_currency");
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSCurrency c = new SSCurrency();
                    c.setName(resultSet.getString("code"));
                    c.setDescription(resultSet.getString("description"));
                    BigDecimal rate = resultSet.getBigDecimal("exchange_rate");
                    c.setExchangeRate(rate != null ? rate : BigDecimal.ONE);
                    list.add(c);
                }
            }
            return list;
        } catch (SQLException e) {
            throw handleFailure("load currencies", e);
        }
    }

    public Optional<SSCurrency> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        try {
            SSCurrency c = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT code, description, exchange_rate FROM PUBLIC.tbl_currency WHERE code=?")) {
                statement.setString(1, code);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        c = new SSCurrency();
                        c.setName(resultSet.getString("code"));
                        c.setDescription(resultSet.getString("description"));
                        BigDecimal rate = resultSet.getBigDecimal("exchange_rate");
                        c.setExchangeRate(rate != null ? rate : BigDecimal.ONE);
                    }
                }
            }
            return Optional.ofNullable(c);
        } catch (SQLException e) {
            throw handleFailure("load currency '" + code + "'", e);
        }
    }

    public void add(SSCurrency currency) {
        if (currency == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_currency(code, description, exchange_rate) VALUES(?,?,?)")) {
                statement.setString(1, currency.getName());
                statement.setString(2, currency.getDescription());
                statement.setBigDecimal(3, currency.getExchangeRate());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("add currency '" + currency.getName() + "'", e);
        }
    }

    public void update(SSCurrency currency) {
        if (currency == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_currency SET description=?, exchange_rate=? WHERE code=?")) {
                statement.setString(1, currency.getDescription());
                statement.setBigDecimal(2, currency.getExchangeRate());
                statement.setString(3, currency.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            throw handleFailure("update currency '" + currency.getName() + "'", e);
        }
    }

    public void delete(SSCurrency currency) {
        if (currency == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_currency WHERE code=?")) {
                statement.setString(1, currency.getName());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for currency '{}' because it is in use", currency.getName());
                return;
            }
            throw handleFailure("delete currency '" + currency.getName() + "'", e);
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
            LOG.error("Failed to rollback currency transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
