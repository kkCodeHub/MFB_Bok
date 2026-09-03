package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.data.SSOwnReportRow;
import se.swedsoft.bookkeeping.data.common.SSHeadingType;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.gui.ownreport.util.SSOwnReportAccountRow;

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
 * Full SQL V2 repository for the OwnReport domain.
 */
public class V2OwnReportRepository {

    /** Rollback delegate that may throw {@link SQLException}. */
    @FunctionalInterface
    public interface RollbackHandler {
        /** Rolls back the current transaction. */
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2OwnReportRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompany;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a repository backed by a JDBC connection.
     *
     * @param connection      the JDBC connection; {@code null} in no-connection init mode
     * @param currentCompany  supplier for the currently active company
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2OwnReportRepository(Connection connection,
                                  Supplier<SSNewCompany> currentCompany,
                                  RollbackHandler rollbackHandler) {
        this.connection = connection;
        this.currentCompany = currentCompany;
        this.rollbackHandler = rollbackHandler;
    }

    /**
     * Returns all own reports for the current company.
     *
     * @return list; never {@code null}
     */
    public List<SSOwnReport> findAll() {
        List<SSOwnReport> results = new LinkedList<>();
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return results;
        }
        try {
            Integer iMax = -1;
            while (true) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_ownreport WHERE companyid=? AND id>?")) {
                    st.setObject(1, company.getId());
                    st.setObject(2, iMax);
                    st.setMaxRows(1024);
                    int i = 0;
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            iMax = rs.getInt(1);
                            results.add(mapOwnReport(rs));
                            i++;
                        }
                    }
                    if (i != 1024) {
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to load own reports: " + e.getMessage(), e);
        }
        return results;
    }

    /**
     * Finds a single own report by id.
     *
     * @param ownReport template with id set
     * @return {@code Optional} with the found report, or empty
     */
    public Optional<SSOwnReport> findByOwnReport(SSOwnReport ownReport) {
        if (ownReport == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT * FROM tbl_ownreport WHERE id=? AND companyid=?")) {
                st.setObject(1, ownReport.getId());
                st.setObject(2, company.getId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapOwnReport(rs));
                    }
                }
            }
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to find own report: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Finds a single own report by numeric id.
     *
     * @param number the id to look up
     * @return {@code Optional} with the found report, or empty
     */
    public Optional<SSOwnReport> findByNumber(Integer number) {
        if (number == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT * FROM tbl_ownreport WHERE id=? AND companyid=?")) {
                st.setObject(1, number);
                st.setObject(2, company.getId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapOwnReport(rs));
                    }
                }
            }
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to find own report by number: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Returns only those own reports from {@code subset} that exist in the DB.
     *
     * @param subset candidate list; may be {@code null}
     * @return filtered list; never {@code null}
     */
    public List<SSOwnReport> findAll(List<SSOwnReport> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return Collections.emptyList();
        }
        List<SSOwnReport> results = new LinkedList<>();
        try {
            for (SSOwnReport candidate : subset) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_ownreport WHERE id=? AND companyid=?")) {
                    st.setObject(1, candidate.getId());
                    st.setObject(2, company.getId());
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            results.add(mapOwnReport(rs));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to filter own reports: " + e.getMessage(), e);
        }
        return results;
    }

    /**
     * Inserts a new own report.
     *
     * @param ownReport the report to add; must not be {@code null}
     */
    public void add(SSOwnReport ownReport) {
        if (ownReport == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return;
        }
        try {
            Integer ownReportId = null;
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO tbl_ownreport(companyid,name,project_nr,result_unit_nr) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                st.setObject(1, company.getId());
                st.setObject(2, ownReport.getName());
                st.setObject(3, ownReport.getProjectNr());
                st.setObject(4, ownReport.getResultUnitNr());
                st.executeUpdate();
                try (ResultSet keys = st.getGeneratedKeys()) {
                    if (keys.next()) {
                        ownReportId = keys.getInt(1);
                    }
                }
            }
            if (ownReportId != null) {
                ownReport.setId(ownReportId);
                replaceOwnReportRows(ownReportId, ownReport);
            }
            connection.commit();
            SSEventTriggerSyncContext.triggerAction("NEWOWNREPORT", "TBL_OWNREPORT",
                    String.valueOf(ownReport.getId()));
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to add own report '" + ownReport.getName() + "'.", e);
        }
    }

    /**
     * Updates an existing own report.
     *
     * @param ownReport the report to update; must not be {@code null}
     */
    public void update(SSOwnReport ownReport) {
        if (ownReport == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE tbl_ownreport SET name=?,project_nr=?,result_unit_nr=? WHERE id=? AND companyid=?")) {
                st.setObject(1, ownReport.getName());
                st.setObject(2, ownReport.getProjectNr());
                st.setObject(3, ownReport.getResultUnitNr());
                st.setObject(4, ownReport.getId());
                st.setObject(5, company.getId());
                st.executeUpdate();
            }
            if (ownReport.getId() != null) {
                replaceOwnReportRows(ownReport.getId(), ownReport);
            }
            connection.commit();
            SSEventTriggerSyncContext.triggerAction("EDITOWNREPORT", "TBL_OWNREPORT",
                    String.valueOf(ownReport.getId()));
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to update own report '" + ownReport.getName() + "'.", e);
        }
    }

    /**
     * Deletes an own report and its rows.
     *
     * @param ownReport the report to delete; must not be {@code null}
     */
    public void delete(SSOwnReport ownReport) {
        if (ownReport == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (connection == null || company == null) {
            return;
        }
        try {
            if (ownReport.getId() != null) {
                try (PreparedStatement del = connection.prepareStatement(
                        "DELETE FROM tbl_ownreport_account_row WHERE ownreport_row_id IN "
                                + "(SELECT id FROM tbl_ownreport_row WHERE ownreport_id=?)")) {
                    del.setObject(1, ownReport.getId());
                    del.executeUpdate();
                }
                try (PreparedStatement del = connection.prepareStatement(
                        "DELETE FROM tbl_ownreport_row WHERE ownreport_id=?")) {
                    del.setObject(1, ownReport.getId());
                    del.executeUpdate();
                }
            }
            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM tbl_ownreport WHERE id=? AND companyid=?")) {
                del.setObject(1, ownReport.getId());
                del.setObject(2, company.getId());
                del.executeUpdate();
            }
            connection.commit();
            SSEventTriggerSyncContext.triggerAction("DELETEOWNREPORT", "TBL_OWNREPORT",
                    String.valueOf(ownReport.getId()));
        } catch (SQLException e) {
            safeRollback();
            throw new IllegalStateException("Failed to delete own report '" + ownReport.getName() + "'.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void safeRollback() {
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException ex) {
            LOG.warn("Rollback failed", ex);
        }
    }

    private SSOwnReport mapOwnReport(ResultSet rs) throws SQLException {
        SSOwnReport report = new SSOwnReport();
        report.setId(rs.getInt("id"));
        report.setName(rs.getString("name"));
        report.setProjectNr(rs.getString("project_nr"));
        report.setResultUnitNr(rs.getString("result_unit_nr"));
        report.getHeadings().clear();
        report.getHeadings().addAll(getOwnReportRows(report.getId()));
        return report;
    }

    private List<SSOwnReportRow> getOwnReportRows(Integer ownReportId) throws SQLException {
        List<SSOwnReportRow> rows = new LinkedList<>();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM tbl_ownreport_row WHERE ownreport_id=? ORDER BY row_order,id")) {
            st.setObject(1, ownReportId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    SSOwnReportRow row = new SSOwnReportRow();
                    String headingType = rs.getString("heading_type");
                    if (headingType != null) {
                        try {
                            row.setType(SSHeadingType.valueOf(headingType));
                        } catch (IllegalArgumentException ignored) {
                            // ignore unknown enum values
                        }
                    }
                    row.setHeading(rs.getString("heading"));
                    row.getAccountRows().addAll(getOwnReportAccountRows(rs.getInt("id")));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private List<SSOwnReportAccountRow> getOwnReportAccountRows(Integer ownReportRowId) throws SQLException {
        List<SSOwnReportAccountRow> rows = new LinkedList<>();
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM tbl_ownreport_account_row WHERE ownreport_row_id=? ORDER BY id")) {
            st.setObject(1, ownReportRowId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Integer accountFrom = (Integer) rs.getObject("account_from");
                    Integer accountTo = (Integer) rs.getObject("account_to");
                    Integer accountNr = accountFrom == null ? accountTo : accountFrom;
                    if (accountNr == null) {
                        continue;
                    }
                    SSOwnReportAccountRow row = new SSOwnReportAccountRow();
                    row.setAccount(new SSAccount(accountNr));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceOwnReportRows(Integer ownReportId, SSOwnReport ownReport) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM tbl_ownreport_account_row WHERE ownreport_row_id IN "
                        + "(SELECT id FROM tbl_ownreport_row WHERE ownreport_id=?)")) {
            del.setObject(1, ownReportId);
            del.executeUpdate();
        }
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM tbl_ownreport_row WHERE ownreport_id=?")) {
            del.setObject(1, ownReportId);
            del.executeUpdate();
        }
        int order = 0;
        for (SSOwnReportRow row : ownReport.getHeadings()) {
            Integer rowId = null;
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO tbl_ownreport_row(ownreport_id,row_order,heading_type,heading) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setObject(1, ownReportId);
                ins.setObject(2, order++);
                ins.setObject(3, row.getType() == null ? null : row.getType().name());
                ins.setObject(4, row.getHeading());
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) {
                        rowId = keys.getInt(1);
                    }
                }
            }
            if (rowId != null) {
                replaceOwnReportAccountRows(rowId, row);
            }
        }
    }

    private void replaceOwnReportAccountRows(Integer ownReportRowId, SSOwnReportRow row) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM tbl_ownreport_account_row WHERE ownreport_row_id=?")) {
            del.setObject(1, ownReportRowId);
            del.executeUpdate();
        }
        for (SSOwnReportAccountRow accountRow : row.getAccountRows()) {
            SSAccount account = accountRow.getAccount();
            Integer accountNr = account == null ? null : account.getNumber();
            if (accountNr == null) {
                continue;
            }
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO tbl_ownreport_account_row(ownreport_row_id,account_from,account_to,negate)"
                            + " VALUES(?,?,?,?)")) {
                ins.setObject(1, ownReportRowId);
                ins.setObject(2, accountNr);
                ins.setObject(3, accountNr);
                ins.setObject(4, false);
                ins.executeUpdate();
            }
        }
    }
}
