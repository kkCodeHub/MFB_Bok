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
        String sql = "SELECT id, year_id, event_code, event_name, series_code, is_custom, active " +
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
                    row.put("active", rs.getBoolean("active"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    /**
     * Find all active mappings for a year, ordered by ID.
     */
    public List<Map<String, Object>> findActiveByYearOrderedById(Integer yearId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, year_id, event_code, event_name, series_code, is_custom, active " +
                     "FROM tbl_year_voucher_event_series_map " +
                     "WHERE year_id = ? AND active = TRUE " +
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
                    row.put("active", rs.getBoolean("active"));
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
        upsertSystemMapping(yearId, eventCode, seriesCode, null, null);
    }

    public void upsertSystemMapping(Integer yearId, String eventCode, String seriesCode, String eventName)
            throws SQLException {
        upsertSystemMapping(yearId, eventCode, seriesCode, eventName, null);
    }

    public void upsertSystemMapping(Integer yearId, String eventCode, String seriesCode, String eventName,
                                    Boolean active)
            throws SQLException {
        String normalizedEventCode = normalizeEventCode(eventCode);
        if (yearId == null || normalizedEventCode == null || seriesCode == null) {
            throw new IllegalArgumentException("yearId, eventCode, and seriesCode must not be null");
        }

        String checkSql = "SELECT id FROM tbl_year_voucher_event_series_map " +
                         "WHERE year_id = ? AND event_code = ? AND is_custom = FALSE";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, yearId);
            checkStmt.setString(2, normalizedEventCode);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    updateSystemMapping(rs.getInt("id"), seriesCode, eventName, active);
                } else {
                    insertSystemMapping(yearId, normalizedEventCode, seriesCode, eventName, active == null || active);
                }
            }
        }
    }

    private void insertSystemMapping(Integer yearId, String eventCode, String seriesCode, String eventName,
                                     boolean active)
            throws SQLException {
        String sql = "INSERT INTO tbl_year_voucher_event_series_map " +
                     "(year_id, event_code, event_name, series_code, is_custom, active) " +
                     "VALUES (?, ?, ?, ?, FALSE, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, eventCode);
            if (eventName == null) {
                stmt.setNull(3, Types.VARCHAR);
            } else {
                stmt.setString(3, eventName);
            }
            stmt.setString(4, seriesCode);
            stmt.setBoolean(5, active);
            stmt.executeUpdate();
            LOG.debug("Inserted system mapping for year {} event {} -> series {}", yearId, eventCode, seriesCode);
        }
    }

    private void updateSystemMapping(Integer mappingId, String seriesCode, String eventName, Boolean active)
            throws SQLException {
        if (active == null) {
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
            }
        } else {
            String sql = "UPDATE tbl_year_voucher_event_series_map " +
                         "SET series_code = ?, event_name = ?, active = ? " +
                         "WHERE id = ? AND is_custom = FALSE";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, seriesCode);
                if (eventName == null) {
                    stmt.setNull(2, Types.VARCHAR);
                } else {
                    stmt.setString(2, eventName);
                }
                stmt.setBoolean(3, active);
                stmt.setInt(4, mappingId);
                stmt.executeUpdate();
            }
        }
        LOG.debug("Updated system mapping id {} to series {}", mappingId, seriesCode);
    }

    /**
     * Insert custom mapping (user-added event).
     */
    public void addCustomMapping(Integer yearId, String customEventName, String seriesCode)
            throws SQLException {
        addCustomMapping(yearId, customEventName, seriesCode, true);
    }

    public void addCustomMapping(Integer yearId, String customEventName, String seriesCode, boolean active)
            throws SQLException {
        String sql = "INSERT INTO tbl_year_voucher_event_series_map " +
                     "(year_id, event_name, series_code, is_custom, active) " +
                     "VALUES (?, ?, ?, TRUE, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, customEventName);
            stmt.setString(3, seriesCode);
            stmt.setBoolean(4, active);
            stmt.executeUpdate();
            LOG.debug("Inserted custom mapping for year {} {} -> series {} (active={})",
                    yearId, customEventName, seriesCode, active);
        }
    }

    /**
     * Update custom mapping.
     */
    public void updateCustomMapping(Integer mappingId, String customEventName, String seriesCode)
            throws SQLException {
        updateCustomMapping(mappingId, customEventName, seriesCode, true);
    }

    public void updateCustomMapping(Integer mappingId, String customEventName, String seriesCode, boolean active)
            throws SQLException {
        String sql = "UPDATE tbl_year_voucher_event_series_map " +
                     "SET event_name = ?, series_code = ?, active = ? " +
                     "WHERE id = ? AND is_custom = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, customEventName);
            stmt.setString(2, seriesCode);
            stmt.setBoolean(3, active);
            stmt.setInt(4, mappingId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No custom mapping found for id " + mappingId);
            }
            LOG.debug("Updated custom mapping id {} (active={})", mappingId, active);
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
     * Reads all event types and creates rows with default series codes and active flags.
     * system=true maps to is_custom=false, system=false maps to is_custom=true.
     */
    public void initializeDefaultMappingsForYear(Integer yearId) throws SQLException {
        V2VoucherEventTypeRepository eventTypeRepo = new V2VoucherEventTypeRepository(connection);
        List<Map<String, Object>> events = eventTypeRepo.findAllOrderedById();

        for (Map<String, Object> event : events) {
            String eventCode = normalizeEventCode((String) event.get("event_code"));
            String eventName = (String) event.get("event_name");
            String defaultSeriesCode = (String) event.get("default_series_code");
            boolean active = Boolean.TRUE.equals(event.get("active"));
            boolean system = Boolean.TRUE.equals(event.get("system"));
            if (system) {
                upsertSystemMapping(yearId, eventCode, defaultSeriesCode, eventName, active);
            } else {
                upsertSeededCustomMapping(yearId, eventCode, eventName, defaultSeriesCode, active);
            }
        }
        LOG.info("Initialized default voucher series mappings for year {}", yearId);
    }

    private void upsertSeededCustomMapping(Integer yearId, String eventCode, String eventName, String seriesCode,
                                           boolean active) throws SQLException {
        Integer existingId = findSeededCustomMappingId(yearId, eventCode, eventName);
        if (existingId == null) {
            insertSeededCustomMapping(yearId, eventCode, eventName, seriesCode, active);
        } else {
            updateSeededCustomMapping(existingId, eventCode, eventName, seriesCode, active);
        }
    }

    private Integer findSeededCustomMappingId(Integer yearId, String eventCode, String eventName) throws SQLException {
        String sql;
        if (eventCode == null) {
            sql = "SELECT id FROM tbl_year_voucher_event_series_map " +
                  "WHERE year_id = ? AND is_custom = TRUE AND event_code IS NULL AND event_name = ?";
        } else {
            sql = "SELECT id FROM tbl_year_voucher_event_series_map " +
                  "WHERE year_id = ? AND is_custom = TRUE AND event_code = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            if (eventCode == null) {
                stmt.setString(2, eventName);
            } else {
                stmt.setString(2, eventCode);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private void insertSeededCustomMapping(Integer yearId, String eventCode, String eventName, String seriesCode,
                                           boolean active) throws SQLException {
        String sql = "INSERT INTO tbl_year_voucher_event_series_map " +
                     "(year_id, event_code, event_name, series_code, is_custom, active) " +
                     "VALUES (?, ?, ?, ?, TRUE, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            if (eventCode == null) {
                stmt.setNull(2, Types.VARCHAR);
            } else {
                stmt.setString(2, eventCode);
            }
            stmt.setString(3, eventName);
            stmt.setString(4, seriesCode);
            stmt.setBoolean(5, active);
            stmt.executeUpdate();
        }
    }

    private void updateSeededCustomMapping(Integer mappingId, String eventCode, String eventName, String seriesCode,
                                           boolean active) throws SQLException {
        String sql = "UPDATE tbl_year_voucher_event_series_map " +
                     "SET event_code = ?, event_name = ?, series_code = ?, active = ? " +
                     "WHERE id = ? AND is_custom = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (eventCode == null) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, eventCode);
            }
            stmt.setString(2, eventName);
            stmt.setString(3, seriesCode);
            stmt.setBoolean(4, active);
            stmt.setInt(5, mappingId);
            stmt.executeUpdate();
        }
    }

    private String normalizeEventCode(String eventCode) {
        if (eventCode == null) {
            return null;
        }
        String normalized = eventCode.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
