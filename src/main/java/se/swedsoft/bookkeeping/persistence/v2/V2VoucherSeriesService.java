package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service/facade for voucher series configuration (Val 8).
 * Wraps repositories to provide business logic for managing event-to-series mappings.
 */
public class V2VoucherSeriesService {
    private static final Logger LOG = LoggerFactory.getLogger(V2VoucherSeriesService.class);

    private final Connection connection;
    private final V2VoucherEventTypeRepository eventTypeRepository;
    private final V2YearVoucherEventSeriesMapRepository mappingRepository;
    private final V2VoucherSeriesCounterRepository counterRepository;

    public V2VoucherSeriesService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
        this.eventTypeRepository = Repositories.voucherEventTypes();
        this.mappingRepository = Repositories.yearVoucherEventSeriesMaps();
        this.counterRepository = Repositories.voucherSeriesCounters();
    }

    /**
     * Get all voucher event types (for reference data).
     */
    public List<Map<String, Object>> getAllEventTypes() throws SQLException {
        return eventTypeRepository.findAllActiveOrderedById();
    }

    /**
     * Get all mappings for a specific year.
     * Returns: [{ id, event_code, event_name, current_series_code, is_custom }, ...]
     */
    public List<Map<String, Object>> getMappingsForYear(int yearId) throws SQLException {
        List<Map<String, Object>> mappings = mappingRepository.findByYearOrderedById(yearId);
        Map<String, Map<String, Object>> eventTypesByCode = new HashMap<>();
        for (Map<String, Object> eventType : eventTypeRepository.findAllOrderedById()) {
            Object eventCode = eventType.get("event_code");
            if (eventCode == null) {
                continue;
            }
            eventTypesByCode.put(String.valueOf(eventCode), eventType);
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> mapping : mappings) {
            Map<String, Object> row = new HashMap<>(mapping);
            boolean isCustom = Boolean.TRUE.equals(mapping.getOrDefault("is_custom", false));

            String eventName = (String) mapping.getOrDefault("event_name", "");
            String eventCode = (String) mapping.get("event_code");
            Map<String, Object> eventType = eventCode == null ? null : eventTypesByCode.get(eventCode);

            if ((eventName == null || eventName.trim().isEmpty()) && !isCustom && eventType != null) {
                eventName = (String) eventType.get("event_name");
            }
            if (eventName == null) {
                eventName = "";
            }

            boolean system = !isCustom;
            row.put("system", system);
            row.put("display_event_name", eventName);
            enriched.add(row);
        }

        return enriched;
    }

    /**
     * Add custom mapping (non-event-based).
     */
    public void addCustomMapping(int yearId, String customEventName, String seriesCode) throws SQLException {
        addCustomMapping(yearId, customEventName, seriesCode, true);
    }

    public void addCustomMapping(int yearId, String customEventName, String seriesCode, boolean active) throws SQLException {
        if (yearId < 0) {
            throw new IllegalArgumentException("yearId must be >= 0");
        }
        if (customEventName == null || customEventName.trim().isEmpty()) {
            throw new IllegalArgumentException("customEventName must not be empty");
        }
        if (seriesCode == null || seriesCode.length() != 1) {
            throw new IllegalArgumentException("seriesCode must be single A-Z letter");
        }

        mappingRepository.addCustomMapping(yearId, customEventName, seriesCode, active);
        counterRepository.createIfMissing(yearId, seriesCode, 0);

        LOG.info("Added custom mapping: yearId={}, customName={}, seriesCode={}, active={}",
                 yearId, customEventName, seriesCode, active);
    }

    /**
     * Update custom mapping.
     */
    public void updateCustomMapping(int mappingId, String customEventName, String seriesCode)
            throws SQLException {
        updateCustomMapping(mappingId, customEventName, seriesCode, true);
    }

    public void updateCustomMapping(int mappingId, String customEventName, String seriesCode, boolean active)
            throws SQLException {
        if (mappingId <= 0) {
            throw new IllegalArgumentException("mappingId must be > 0");
        }
        if (customEventName == null || customEventName.trim().isEmpty()) {
            throw new IllegalArgumentException("customEventName must not be empty");
        }
        if (seriesCode == null || seriesCode.length() != 1) {
            throw new IllegalArgumentException("seriesCode must be single A-Z letter");
        }

        if (hasVouchersForCustomMapping(mappingId)) {
            throw new IllegalStateException("Custom series cannot be changed when vouchers exist for the series");
        }

        mappingRepository.updateCustomMapping(mappingId, customEventName, seriesCode, active);

        LOG.info("Updated custom mapping: mappingId={}, newSeriesCode={}, active={}", mappingId, seriesCode, active);
    }

    /**
     * Delete custom mapping.
     */
    public void deleteCustomMapping(int mappingId) throws SQLException {
        if (mappingId <= 0) {
            throw new IllegalArgumentException("mappingId must be > 0");
        }
        if (hasVouchersForCustomMapping(mappingId)) {
            throw new IllegalStateException("Custom series cannot be deleted when vouchers exist for the series");
        }

        mappingRepository.deleteCustomMapping(mappingId);

        LOG.info("Deleted custom mapping: mappingId={}", mappingId);
    }

    public boolean canEditCustomMapping(int mappingId) throws SQLException {
        if (mappingId <= 0) {
            throw new IllegalArgumentException("mappingId must be > 0");
        }
        return !hasVouchersForCustomMapping(mappingId);
    }

    public boolean canDeleteCustomMapping(int mappingId) throws SQLException {
        if (mappingId <= 0) {
            throw new IllegalArgumentException("mappingId must be > 0");
        }
        return !hasVouchersForCustomMapping(mappingId);
    }

    /**
     * Update system mapping series code (only if changed).
     */
    public void updateSystemMappingSeriesCode(int yearId, String eventCode, String newSeriesCode)
            throws SQLException {
        if (yearId < 0) {
            throw new IllegalArgumentException("yearId must be >= 0");
        }
        if (eventCode == null || eventCode.isEmpty()) {
            throw new IllegalArgumentException("eventCode must not be empty");
        }
        if (newSeriesCode == null || newSeriesCode.length() != 1) {
            throw new IllegalArgumentException("newSeriesCode must be single A-Z letter");
        }
        if (hasVouchersInYear(yearId)) {
            throw new IllegalStateException("System series code cannot be changed when vouchers exist in the year");
        }

        mappingRepository.upsertSystemMapping(yearId, eventCode, newSeriesCode);
        counterRepository.createIfMissing(yearId, newSeriesCode, 0);

        LOG.info("Updated system mapping series: yearId={}, eventCode={}, newSeriesCode={}",
                 yearId, eventCode, newSeriesCode);
    }

    public boolean canChangeSystemSeriesCode(int yearId) throws SQLException {
        if (yearId < 0) {
            throw new IllegalArgumentException("yearId must be >= 0");
        }
        return !hasVouchersInYear(yearId);
    }

    /**
     * Get next series number for a specific year and series code.
     */
    public int getNextSeriesNumber(int yearId, String seriesCode) throws SQLException {
        if (yearId < 0) {
            throw new IllegalArgumentException("yearId must be >= 0");
        }
        if (seriesCode == null || seriesCode.length() != 1) {
            throw new IllegalArgumentException("seriesCode must be single A-Z letter");
        }

        return counterRepository.getNextNumber(yearId, seriesCode);
    }

    /**
     * Get current counter for a series (for display/info purposes).
     */
    public int getCurrentSeriesNumber(int yearId, String seriesCode) throws SQLException {
        if (yearId < 0) {
            throw new IllegalArgumentException("yearId must be >= 0");
        }
        if (seriesCode == null || seriesCode.length() != 1) {
            throw new IllegalArgumentException("seriesCode must be single A-Z letter");
        }

        Optional<Map<String, Object>> counter = counterRepository.findByYearAndSeries(yearId, seriesCode);
        if (counter.isPresent()) {
            Object lastNum = counter.get().get("last_number");
            return lastNum instanceof Number ? ((Number) lastNum).intValue() : 0;
        }
        return 0;
    }

    private boolean hasVouchersInYear(int yearId) throws SQLException {
        String sql = "SELECT COUNT(*) AS voucher_count FROM tbl_voucher WHERE yearid = ?";
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("voucher_count") > 0;
                }
            }
        }
        return false;
    }

    private boolean hasVouchersForCustomMapping(int mappingId) throws SQLException {
        String sql = "SELECT COUNT(*) AS voucher_count " +
                     "FROM tbl_voucher v " +
                     "JOIN tbl_year_voucher_event_series_map m ON m.id = ? AND m.is_custom = TRUE " +
                     "WHERE v.yearid = m.year_id AND v.series = m.series_code";
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, mappingId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("voucher_count") > 0;
                }
            }
        }
        return false;
    }
}
