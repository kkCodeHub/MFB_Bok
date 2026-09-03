package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSAutoDistRow;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 auto-distribution repository backed by the normalized V2 schema.
 */
public class V2AutoDistRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2AutoDistRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 auto-distribution repository.
     *
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2AutoDistRepository(Connection connection,
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

    public List<SSAutoDist> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSAutoDist> autoDists = new LinkedList<>();
            int max = -1;

            while (true) {
                int count = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_autodist WHERE companyid=? AND id>?")) {
                    statement.setObject(1, currentCompany.getId());
                    statement.setObject(2, max);
                    statement.setMaxRows(1024);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            max = resultSet.getInt(1);
                            autoDists.add(mapAutoDistV2(resultSet));
                            count++;
                        }
                    }
                }
                if (count != 1024) {
                    break;
                }
            }

            return autoDists;
        } catch (SQLException e) {
            throw handleFailure("load autodists", e);
        }
    }

    public Optional<SSAutoDist> findByAutoDist(SSAutoDist autoDist) {
        if (autoDist == null) {
            return Optional.empty();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_autodist WHERE number=? AND companyid=?")) {
                statement.setObject(1, autoDist.getNumber());
                statement.setObject(2, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapAutoDistV2(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find autodist '" + autoDist.getNumber() + "'", e);
        }
    }

    public List<SSAutoDist> findAll(List<SSAutoDist> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSAutoDist> autoDists = new LinkedList<>();
            for (SSAutoDist autoDist : subset) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_autodist WHERE number=? AND companyid=?")) {
                    statement.setObject(1, autoDist.getNumber());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            autoDists.add(mapAutoDistV2(resultSet));
                        }
                    }
                }
            }
            return autoDists;
        } catch (SQLException e) {
            throw handleFailure("filter autodists", e);
        }
    }

    public void add(SSAutoDist autoDist) {
        if (autoDist == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer autoDistId = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_autodist(number,companyid,account_nr,description,amount) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setObject(1, autoDist.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.setObject(3, autoDist.getNumber());
                statement.setObject(4, autoDist.getDescription());
                statement.setObject(5, autoDist.getAmount());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        autoDistId = keys.getInt(1);
                    }
                }
            }

            if (autoDistId == null) {
                autoDistId = getAutoDistIdV2(autoDist.getNumber(), currentCompany.getId());
            }
            if (autoDistId != null) {
                replaceAutoDistRowsV2(autoDistId, autoDist);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWAUTODIST", "TBL_AUTODIST", String.valueOf(autoDist.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("add autodist '" + autoDist.getNumber() + "'", e);
        }
    }

    public void update(SSAutoDist autoDist, SSAutoDist original) {
        if (autoDist == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tbl_autodist SET number=?,account_nr=?,description=?,amount=? WHERE number=? AND companyid=?")) {
                statement.setObject(1, autoDist.getNumber());
                statement.setObject(2, autoDist.getNumber());
                statement.setObject(3, autoDist.getDescription());
                statement.setObject(4, autoDist.getAmount());
                statement.setObject(5, original == null ? autoDist.getNumber() : original.getNumber());
                statement.setObject(6, currentCompany.getId());
                statement.executeUpdate();
            }

            Integer autoDistId = getAutoDistIdV2(autoDist.getNumber(), currentCompany.getId());
            if (autoDistId != null) {
                replaceAutoDistRowsV2(autoDistId, autoDist);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITAUTODIST", "TBL_AUTODIST", String.valueOf(autoDist.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("update autodist '" + autoDist.getNumber() + "'", e);
        }
    }

    public void delete(SSAutoDist autoDist) {
        if (autoDist == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer autoDistId = getAutoDistIdV2(autoDist.getNumber(), currentCompany.getId());
            if (autoDistId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_autodist_row WHERE autodist_id=?")) {
                    deleteRows.setObject(1, autoDistId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_autodist WHERE number=? AND companyid=?")) {
                statement.setObject(1, autoDist.getNumber());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEAUTODIST", "TBL_AUTODIST", String.valueOf(autoDist.getNumber()));
        } catch (SQLException e) {
            throw handleFailure("delete autodist '" + autoDist.getNumber() + "'", e);
        }
    }

    private List<SSAutoDistRow> getAutoDistRowsV2(Integer autoDistId) throws SQLException {
        List<SSAutoDistRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_autodist_row WHERE autodist_id=? ORDER BY id")) {
            statement.setObject(1, autoDistId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSAutoDistRow row = new SSAutoDistRow();
                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    row.setDescription(resultSet.getString("description"));
                    row.setPercentage(resultSet.getBigDecimal("percentage"));
                    row.setDebet(resultSet.getBigDecimal("debet"));
                    row.setCredit(resultSet.getBigDecimal("credit"));
                    row.setProjectNr(resultSet.getString("project_nr"));
                    row.setResultUnitNr(resultSet.getString("result_unit_nr"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceAutoDistRowsV2(Integer autoDistId, SSAutoDist autoDist) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_autodist_row WHERE autodist_id=?")) {
            delete.setObject(1, autoDistId);
            delete.executeUpdate();
        }

        for (SSAutoDistRow row : autoDist.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_autodist_row" +
                            "(autodist_id,account_nr,description,percentage,debet,credit,project_nr,result_unit_nr)" +
                            " VALUES(?,?,?,?,?,?,?,?)")) {
                insert.setObject(1, autoDistId);
                insert.setObject(2, row.getAccountNr());
                insert.setObject(3, row.getDescription());
                insert.setObject(4, row.getPercentage());
                insert.setObject(5, row.getDebet());
                insert.setObject(6, row.getCredit());
                insert.setObject(7, row.getProjectNr());
                insert.setObject(8, row.getResultUnitNr());
                insert.executeUpdate();
            }
        }
    }

    private Integer getAutoDistIdV2(Integer accountNumber, Integer companyId) throws SQLException {
        if (accountNumber == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_autodist WHERE number=? AND companyid=?")) {
            statement.setObject(1, accountNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private SSAutoDist mapAutoDistV2(ResultSet resultSet) throws SQLException {
        SSAutoDist autoDist = new SSAutoDist();
        autoDist.setAccountNumber((Integer) resultSet.getObject("account_nr"));
        autoDist.setDescrition(resultSet.getString("description"));
        autoDist.setAmount(resultSet.getBigDecimal("amount"));
        autoDist.getRows().clear();
        autoDist.getRows().addAll(getAutoDistRowsV2(resultSet.getInt("id")));
        return autoDist;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback autodist transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}



