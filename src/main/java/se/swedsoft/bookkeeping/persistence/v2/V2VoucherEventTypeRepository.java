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
 * Repository for tbl_voucher_event_type (PUBLIC schema).
 * Manages global master data for voucher event codes.
 */
public class V2VoucherEventTypeRepository {
    private static final Logger LOG = LoggerFactory.getLogger(V2VoucherEventTypeRepository.class);

    private final Connection connection;

    public V2VoucherEventTypeRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Find all event types ordered by ID.
     */
    public List<Map<String, Object>> findAllOrderedById() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, event_code, event_name, default_series_code, system, active " +
                     "FROM PUBLIC.tbl_voucher_event_type " +
                     "ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("event_code", rs.getString("event_code"));
                row.put("event_name", rs.getString("event_name"));
                row.put("default_series_code", rs.getString("default_series_code"));
                row.put("system", rs.getBoolean("system"));
                row.put("active", rs.getBoolean("active"));
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Find all active event types ordered by ID.
     */
    public List<Map<String, Object>> findAllActiveOrderedById() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, event_code, event_name, default_series_code, system, active " +
                     "FROM PUBLIC.tbl_voucher_event_type " +
                     "WHERE active = TRUE " +
                     "ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("event_code", rs.getString("event_code"));
                row.put("event_name", rs.getString("event_name"));
                row.put("default_series_code", rs.getString("default_series_code"));
                row.put("system", rs.getBoolean("system"));
                row.put("active", rs.getBoolean("active"));
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Find event type by code.
     */
    public Optional<Map<String, Object>> findByCode(String eventCode) throws SQLException {
        String normalizedEventCode = normalizeEventCode(eventCode);
        String sql;
        if (normalizedEventCode == null) {
            sql = "SELECT id, event_code, event_name, default_series_code, system, active " +
                  "FROM PUBLIC.tbl_voucher_event_type " +
                  "WHERE event_code IS NULL";
        } else {
            sql = "SELECT id, event_code, event_name, default_series_code, system, active " +
                  "FROM PUBLIC.tbl_voucher_event_type " +
                  "WHERE event_code = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (normalizedEventCode != null) {
                stmt.setString(1, normalizedEventCode);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("event_code", rs.getString("event_code"));
                    row.put("event_name", rs.getString("event_name"));
                    row.put("default_series_code", rs.getString("default_series_code"));
                    row.put("system", rs.getBoolean("system"));
                    row.put("active", rs.getBoolean("active"));
                    return Optional.of(row);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> findByNameAndSeriesCode(String eventName, String defaultSeriesCode)
            throws SQLException {
        String sql = "SELECT id, event_code, event_name, default_series_code, system, active " +
                     "FROM PUBLIC.tbl_voucher_event_type " +
                     "WHERE event_name = ? AND default_series_code = ? AND event_code IS NULL";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, eventName);
            stmt.setString(2, defaultSeriesCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("event_code", rs.getString("event_code"));
                    row.put("event_name", rs.getString("event_name"));
                    row.put("default_series_code", rs.getString("default_series_code"));
                    row.put("system", rs.getBoolean("system"));
                    row.put("active", rs.getBoolean("active"));
                    return Optional.of(row);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Upsert (insert or update) event type by code.
     * If exists: update name/default_series_code/system/active.
     * If not: insert new row.
     */
    public void upsertFromSeed(String eventCode, String eventName, String defaultSeriesCode,
                               boolean system, boolean active)
            throws SQLException {
        String normalizedEventCode = normalizeEventCode(eventCode);
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("eventName must not be null or empty");
        }
        if (defaultSeriesCode == null || defaultSeriesCode.length() != 1) {
            throw new IllegalArgumentException("defaultSeriesCode must be single A-Z letter");
        }
        if (system && normalizedEventCode == null) {
            throw new IllegalArgumentException("eventCode must not be null or empty for system event types");
        }

        Optional<Map<String, Object>> existing = normalizedEventCode == null
                ? findByNameAndSeriesCode(eventName, defaultSeriesCode)
                : findByCode(normalizedEventCode);
        
        if (existing.isPresent()) {
            updateEventType(((Number) existing.get().get("id")).intValue(),
                    normalizedEventCode, eventName, defaultSeriesCode, system, active);
        } else {
            insertEventType(normalizedEventCode, eventName, defaultSeriesCode, system, active);
        }
    }

    private void insertEventType(String eventCode, String eventName, String defaultSeriesCode,
                                 boolean system, boolean active)
            throws SQLException {
        String sql = "INSERT INTO PUBLIC.tbl_voucher_event_type " +
                     "(event_code, event_name, default_series_code, system, active) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (eventCode == null) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, eventCode);
            }
            stmt.setString(2, eventName);
            stmt.setString(3, defaultSeriesCode);
            stmt.setBoolean(4, system);
            stmt.setBoolean(5, active);
            stmt.executeUpdate();
            LOG.debug("Inserted event type: {} -> {}", eventCode, eventName);
        }
    }

    private void updateEventType(Integer id, String eventCode, String eventName, String defaultSeriesCode,
                                 boolean system, boolean active)
            throws SQLException {
        String sql = "UPDATE PUBLIC.tbl_voucher_event_type " +
                     "SET event_code = ?, event_name = ?, default_series_code = ?, system = ?, active = ? " +
                     "WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (eventCode == null) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, eventCode);
            }
            stmt.setString(2, eventName);
            stmt.setString(3, defaultSeriesCode);
            stmt.setBoolean(4, system);
            stmt.setBoolean(5, active);
            stmt.setInt(6, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LOG.debug("Updated event type: {} -> {}", eventCode, eventName);
            }
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
