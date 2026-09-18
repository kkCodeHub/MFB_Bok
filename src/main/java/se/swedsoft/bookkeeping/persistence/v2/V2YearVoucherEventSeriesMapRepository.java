package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for tbl_year_voucher_event_series_map (Company schema).
 * Manages year-specific mappings of event codes to voucher series.
 */
public class V2YearVoucherEventSeriesMapRepository {
    private static final Logger LOG = LoggerFactory.getLogger(V2YearVoucherEventSeriesMapRepository.class);

    private final Connection connection;

    public V2YearVoucherEventSeriesMapRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Find all mappings for a year, ordered by ID.
     */
    public List<Map<String, Object>> findByYearOrderedById(Integer yearId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, year_id, event_code, event_name, series_code, is_custom " +
                     "FROM tbl_year_voucher_event_series_map " +
                     "WHERE year_id = ? " +
                     "ORDER BY id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("year_id", rs.getInt("year_id"));
                    row.put("event_code", rs.getString("event_code"));
                    row.put("event_name", rs.getString("event_name"));
                    row.put("series_code", rs.getString("series_code"));
                    row.put("is_custom", rs.getBoolean("is_custom"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * Upsert system mapping (from PUBLIC.tbl_voucher_event_type).
     * Updates series_code if already exists, otherwise inserts.
     */
    public void upsertSystemMapping(Integer yearId, String eventCode, String seriesCode)
            throws SQLException {
        upsertSystemMapping(yearId, eventCode, seriesCode, null);
    }

    public void upsertSystemMapping(Integer yearId, String eventCode, String seriesCode, String eventName)
            throws SQLException {
        if (yearId == null || eventCode == null || seriesCode == null) {
            throw new IllegalArgumentException("yearId, eventCode, and seriesCode must not be null");
        }

        String checkSql = "SELECT id FROM tbl_year_voucher_event_series_map " +
                         "WHERE year_id = ? AND event_code = ? AND is_custom = FALSE";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, yearId);
            checkStmt.setString(2, eventCode);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    updateSystemMapping(rs.getInt("id"), seriesCode, eventName);
                } else {
                    insertSystemMapping(yearId, eventCode, seriesCode, eventName);
                }
            }
        }
    }

    private void insertSystemMapping(Integer yearId, String eventCode, String seriesCode, String eventName)
            throws SQLException {
        String sql = "INSERT INTO tbl_year_voucher_event_series_map " +
                     "(year_id, event_code, event_name, series_code, is_custom) " +
                     "VALUES (?, ?, ?, ?, FALSE)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, eventCode);
            if (eventName == null) {
                stmt.setNull(3, Types.VARCHAR);
            } else {
                stmt.setString(3, eventName);
            }
            stmt.setString(4, seriesCode);
            stmt.executeUpdate();
            LOG.debug("Inserted system mapping for year {} event {} -> series {}", yearId, eventCode, seriesCode);
        }
    }

    private void updateSystemMapping(Integer mappingId, String seriesCode, String eventName) throws SQLException {
        String sql = "UPDATE tbl_year_voucher_event_series_map " +
                     "SET series_code = ?, event_name = ? " +
                     "WHERE id = ? AND is_custom = FALSE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, seriesCode);
            if (eventName == null) {
                stmt.setNull(2, Types.VARCHAR);
            } else {
                stmt.setString(2, eventName);
            }
            stmt.setInt(3, mappingId);
            stmt.executeUpdate();
            LOG.debug("Updated system mapping id {} to series {}", mappingId, seriesCode);
        }
    }

    /**
     * Insert custom mapping (user-added event).
     */
    public void addCustomMapping(Integer yearId, String customEventName, String seriesCode)
            throws SQLException {
        String sql = "INSERT INTO tbl_year_voucher_event_series_map " +
                     "(year_id, event_name, series_code, is_custom) " +
                     "VALUES (?, ?, ?, TRUE)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, customEventName);
            stmt.setString(3, seriesCode);
            stmt.executeUpdate();
            LOG.debug("Inserted custom mapping for year {} {} -> series {}", yearId, customEventName, seriesCode);
        }
    }

    /**
     * Update custom mapping.
     */
    public void updateCustomMapping(Integer mappingId, String customEventName, String seriesCode)
            throws SQLException {
        String sql = "UPDATE tbl_year_voucher_event_series_map " +
                     "SET event_name = ?, series_code = ? " +
                     "WHERE id = ? AND is_custom = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customEventName);
            stmt.setString(2, seriesCode);
            stmt.setInt(3, mappingId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No custom mapping found for id " + mappingId);
            }
            LOG.debug("Updated custom mapping id {}", mappingId);
        }
    }

    /**
     * Delete custom mapping.
     */
    public void deleteCustomMapping(Integer mappingId) throws SQLException {
        String sql = "DELETE FROM tbl_year_voucher_event_series_map " +
                     "WHERE id = ? AND is_custom = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, mappingId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LOG.debug("Deleted custom mapping id {}", mappingId);
            }
        }
    }

    /**
     * Initialize default system mappings for a new year from PUBLIC.tbl_voucher_event_type.
     * Reads all active event types and creates system rows with default series codes.
     */
    public void initializeDefaultMappingsForYear(Integer yearId) throws SQLException {
        V2VoucherEventTypeRepository eventTypeRepo = new V2VoucherEventTypeRepository(connection);
        List<Map<String, Object>> activeEvents = eventTypeRepo.findAllActiveOrderedById();

        for (Map<String, Object> event : activeEvents) {
            String eventCode = (String) event.get("event_code");
            String eventName = (String) event.get("event_name");
            String defaultSeriesCode = (String) event.get("default_series_code");
            upsertSystemMapping(yearId, eventCode, defaultSeriesCode, eventName);
        }
        LOG.info("Initialized default voucher series mappings for year {}", yearId);
    }
}
