package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * V2 account-plan repository backed by direct SQL.
 */
public class V2AccountPlanRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2AccountPlanRepository.class);

    private final Connection connection;
    private final RollbackHandler rollbackHandler;

    public V2AccountPlanRepository(Connection connection, RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSAccountPlan> findAll() {
        List<SSAccountPlan> plans = new LinkedList<>();
        try {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM PUBLIC.tbl_accountplan");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    plans.add(mapAccountPlan(resultSet));
                }
            }
        } catch (SQLException e) {
            handleSqlError(e);
        }
        return plans;
    }

    public Optional<SSAccountPlan> findById(int id) {
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM PUBLIC.tbl_accountplan WHERE id=?")) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapAccountPlan(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            handleSqlError(e);
        }
        return Optional.empty();
    }

    public void add(SSAccountPlan plan) {
        if (plan == null) {
            return;
        }
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO PUBLIC.tbl_accountplan(name,base_name,assessment_year,plan_type,excel_path,is_default) VALUES(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setObject(1, plan.getName());
                statement.setObject(2, plan.getBaseName());
                statement.setObject(3, plan.getAssessementYear());
                statement.setObject(4, plan.getType() == null ? null : plan.getType().getName());
                statement.setObject(5, plan.getExcelPath());
                statement.setObject(6, plan.isDefaultPlan());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        plan.setId(keys.getInt(1));
                    }
                }
            }
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void update(SSAccountPlan plan) {
        if (plan == null) {
            return;
        }
        try {
            Integer planId = plan.getId();
            if (planId == null) {
                planId = resolveAccountPlanIdByName(plan.getName());
                plan.setId(planId);
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_accountplan SET name=?,base_name=?,assessment_year=?,plan_type=?,excel_path=?,is_default=? WHERE id=?")) {
                statement.setObject(1, plan.getName());
                statement.setObject(2, plan.getBaseName());
                statement.setObject(3, plan.getAssessementYear());
                statement.setObject(4, plan.getType() == null ? null : plan.getType().getName());
                statement.setObject(5, plan.getExcelPath());
                statement.setObject(6, plan.isDefaultPlan());
                statement.setObject(7, planId);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void delete(SSAccountPlan plan) {
        if (plan == null) {
            return;
        }
        try {
            Integer planId = plan.getId();
            if (planId == null) {
                planId = resolveAccountPlanIdByName(plan.getName());
            }
            if (planId == null) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM PUBLIC.tbl_accountplan WHERE id=?")) {
                statement.setObject(1, planId);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    private SSAccountPlan mapAccountPlan(ResultSet resultSet) throws SQLException {
        SSAccountPlan accountPlan = new SSAccountPlan();
        accountPlan.setId(resultSet.getInt("id"));
        accountPlan.setName(resultSet.getString("name"));
        accountPlan.setBaseName(resultSet.getString("base_name"));
        accountPlan.setAssessementYear(resultSet.getString("assessment_year"));
        accountPlan.setExcelPath(resultSet.getString("excel_path"));
        accountPlan.setDefaultPlan(resultSet.getBoolean("is_default"));

        String planType = resultSet.getString("plan_type");
        if (planType != null) {
            accountPlan.setType(planType);
        }
        return accountPlan;
    }

    private Integer resolveAccountPlanIdByName(String planName) throws SQLException {
        if (planName == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM PUBLIC.tbl_accountplan WHERE name=?")) {
            statement.setObject(1, planName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private void handleSqlError(SQLException e) {
        LOG.error("Unexpected error", e);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException ignored) {
            // best effort rollback
        }
        SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error", e.getMessage());
    }
}
