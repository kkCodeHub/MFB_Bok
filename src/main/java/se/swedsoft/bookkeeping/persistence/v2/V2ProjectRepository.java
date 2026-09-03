package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * V2 project repository backed by the normalized V2 schema.
 */
public class V2ProjectRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2ProjectRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    /**
     * @param connection the active database connection; must not be {@code null}
     * @param currentCompanySupplier supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2ProjectRepository(Connection connection,
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

    public List<SSNewProject> findAll() {
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSNewProject> projects = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_project WHERE companyid=?")) {
                statement.setObject(1, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        projects.add(mapProject(resultSet));
                    }
                }
            }
            return projects;
        } catch (SQLException e) {
            throw handleFailure("load projects", e);
        }
    }

    public List<SSNewProject> findAll(List<SSNewProject> pProjects) {
        if (pProjects == null) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSNewProject> projects = new LinkedList<>();
            for (SSNewProject project : pProjects) {
                if (project == null || project.getNumber() == null) {
                    continue;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_project WHERE number=? AND companyid=?")) {
                    statement.setObject(1, project.getNumber());
                    statement.setObject(2, company.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            projects.add(mapProject(resultSet));
                        }
                    }
                }
            }
            return projects;
        } catch (SQLException e) {
            throw handleFailure("filter projects", e);
        }
    }

    public Optional<SSNewProject> findById(SSNewProject pProject) {
        if (pProject == null) {
            return Optional.empty();
        }
        return findByNumber(pProject.getNumber());
    }

    public Optional<SSNewProject> findByNumber(String pProjectNumber) {
        if (pProjectNumber == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return Optional.empty();
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_project WHERE number=? AND companyid=?")) {
                statement.setObject(1, pProjectNumber);
                statement.setObject(2, company.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapProject(resultSet));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find project by number '" + pProjectNumber + "'", e);
        }
    }

    public void add(SSNewProject pProject) {
        if (pProject == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_project(number, companyid, name, description, concluded, concluded_date) VALUES(?,?,?,?,?,?)")) {
            statement.setObject(1, pProject.getNumber());
            statement.setObject(2, company.getId());
            statement.setObject(3, pProject.getName());
            statement.setObject(4, pProject.getDescription());
            statement.setObject(5, pProject.getConcluded());
            bindLocalDate(statement, 6, pProject.getLocalConcludedDate());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWPROJECT", "TBL_PROJECT", pProject.getNumber());
        } catch (SQLException e) {
            throw handleFailure("add project '" + pProject.getNumber() + "'", e);
        }
    }

    public void update(SSNewProject pProject) {
        if (pProject == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tbl_project SET name=?, description=?, concluded=?, concluded_date=? WHERE number=? AND companyid=?")) {
            statement.setObject(1, pProject.getName());
            statement.setObject(2, pProject.getDescription());
            statement.setObject(3, pProject.getConcluded());
            bindLocalDate(statement, 4, pProject.getLocalConcludedDate());
            statement.setObject(5, pProject.getNumber());
            statement.setObject(6, company.getId());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITPROJECT", "TBL_PROJECT", pProject.getNumber());
        } catch (SQLException e) {
            throw handleFailure("update project '" + pProject.getNumber() + "'", e);
        }
    }

    public void delete(SSNewProject pProject) {
        if (pProject == null) {
            return;
        }
        SSNewCompany company = currentCompanySupplier.get();
        if (company == null || company.getId() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tbl_project WHERE number=? AND companyid=?")) {
            statement.setObject(1, pProject.getNumber());
            statement.setObject(2, company.getId());
            statement.executeUpdate();

            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEPROJECT", "TBL_PROJECT", pProject.getNumber());
        } catch (SQLException e) {
            throw handleFailure("delete project '" + pProject.getNumber() + "'", e);
        }
    }

    private SSNewProject mapProject(ResultSet resultSet) throws SQLException {
        SSNewProject project = new SSNewProject();
        project.setNumber(resultSet.getString("number"));
        project.setName(resultSet.getString("name"));
        project.setDescription(resultSet.getString("description"));
        project.setConcluded(resultSet.getBoolean("concluded"));

        Date concludedDate = resultSet.getDate("concluded_date");
        if (concludedDate != null) {
            project.setLocalConcludedDate(concludedDate.toLocalDate());
        }
        return project;
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
            LOG.error("Failed to rollback project transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}

