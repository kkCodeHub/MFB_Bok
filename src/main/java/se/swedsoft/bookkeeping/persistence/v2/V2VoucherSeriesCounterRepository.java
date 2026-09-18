package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for tbl_voucher_series_counter (Company schema).
 * Tracks the last/next voucher number per series per year.
 * Used in FAS 2 for voucher numbering logic.
 */
public class V2VoucherSeriesCounterRepository {
    private static final Logger LOG = LoggerFactory.getLogger(V2VoucherSeriesCounterRepository.class);

    private final Connection connection;

    public V2VoucherSeriesCounterRepository(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    /**
     * Find counter by year and series code.
     */
    public Optional<Map<String, Object>> findByYearAndSeries(Integer yearId, String seriesCode) 
            throws SQLException {
        if (yearId == null || seriesCode == null) {
            return Optional.empty();
        }

        String sql = "SELECT year_id, series_code, last_number " +
                     "FROM tbl_voucher_series_counter " +
                     "WHERE year_id = ? AND series_code = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, seriesCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("year_id", rs.getInt("year_id"));
                    row.put("series_code", rs.getString("series_code"));
                    row.put("last_number", rs.getInt("last_number"));
                    return Optional.of(row);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Create counter if missing.
     * Idempotent: does nothing if already exists.
     */
    public void createIfMissing(Integer yearId, String seriesCode, Integer startValue) 
            throws SQLException {
        if (yearId == null || seriesCode == null) {
            throw new IllegalArgumentException("yearId and seriesCode must not be null");
        }
        
        int initialValue = startValue != null ? startValue : 0;

        Optional<Map<String, Object>> existing = findByYearAndSeries(yearId, seriesCode);
        if (existing.isPresent()) {
            LOG.debug("Counter already exists for year {} series {}", yearId, seriesCode);
            return;
        }

        String sql = "INSERT INTO tbl_voucher_series_counter " +
                     "(year_id, series_code, last_number) " +
                     "VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, seriesCode);
            stmt.setInt(3, initialValue);
            stmt.executeUpdate();
            LOG.debug("Created counter for year {} series {} with start value {}", yearId, seriesCode, initialValue);
        }
    }

    /**
     * Increment and return next number for a series.
     * Used when creating a new voucher.
     */
    public Integer getNextNumber(Integer yearId, String seriesCode) throws SQLException {
        if (yearId == null || seriesCode == null) {
            throw new IllegalArgumentException("yearId and seriesCode must not be null");
        }

        createIfMissing(yearId, seriesCode, 0);

        String sql = "UPDATE tbl_voucher_series_counter " +
                     "SET last_number = last_number + 1 " +
                     "WHERE year_id = ? AND series_code = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, yearId);
            stmt.setString(2, seriesCode);
            stmt.executeUpdate();
        }

        Optional<Map<String, Object>> counter = findByYearAndSeries(yearId, seriesCode);
        if (counter.isPresent()) {
            return (Integer) counter.get().get("last_number");
        }
        throw new SQLException("Failed to get next number for year " + yearId + " series " + seriesCode);
    }

    /**
     * Update last_number directly.
     * Used for manual correction or import scenarios.
     */
    public void updateLastNumber(Integer yearId, String seriesCode, Integer lastNumber) 
            throws SQLException {
        if (yearId == null || seriesCode == null || lastNumber == null) {
            throw new IllegalArgumentException("yearId, seriesCode, and lastNumber must not be null");
        }

        createIfMissing(yearId, seriesCode, 0);

        String sql = "UPDATE tbl_voucher_series_counter " +
                     "SET last_number = ? " +
                     "WHERE year_id = ? AND series_code = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lastNumber);
            stmt.setInt(2, yearId);
            stmt.setString(3, seriesCode);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LOG.debug("Updated counter for year {} series {} to last_number {}", yearId, seriesCode, lastNumber);
            }
        }
    }
}
