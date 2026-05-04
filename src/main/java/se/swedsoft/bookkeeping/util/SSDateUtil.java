package se.swedsoft.bookkeeping.util;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


/**
 * Adapter utility bridging the legacy {@link java.util.Date} API and the modern
 * {@link java.time} API (JSR-310).
 *
 * <p>All conversions use the JVM default time-zone ({@link ZoneId#systemDefault()}),
 * which is consistent with the rest of the application.  Null inputs are accepted
 * and always produce a null output so callers do not need separate null-checks.
 *
 * <p>This class is a pure static utility — it cannot be instantiated.
 */
public final class SSDateUtil {

    private SSDateUtil() {}

    // -------------------------------------------------------------------------
    // java.util.Date -> java.time
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link Date} to a {@link LocalDate} using the system default zone.
     *
     * @param date the legacy date, may be {@code null}
     * @return the equivalent {@link LocalDate}, or {@code null} if {@code date} is {@code null}
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        // java.sql.Date.toInstant() always throws UnsupportedOperationException;
        // use its own toLocalDate() instead.
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts a {@link Date} to a {@link LocalDateTime} using the system default zone.
     *
     * @param date the legacy date, may be {@code null}
     * @return the equivalent {@link LocalDateTime}, or {@code null} if {@code date} is {@code null}
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        // java.sql.Date.toInstant() always throws UnsupportedOperationException;
        // convert via toLocalDate() at start-of-day instead.
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate().atStartOfDay();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // -------------------------------------------------------------------------
    // java.time -> java.util.Date
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link LocalDate} to a {@link Date} at start-of-day in the system default zone.
     *
     * <p>This is required wherever legacy APIs (e.g. JasperReports, Swing date pickers)
     * still expect a {@link Date}.
     *
     * @param localDate the modern date, may be {@code null}
     * @return the equivalent {@link Date} at midnight, or {@code null} if {@code localDate} is {@code null}
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@link LocalDateTime} to a {@link Date} using the system default zone.
     *
     * @param localDateTime the modern date-time, may be {@code null}
     * @return the equivalent {@link Date}, or {@code null} if {@code localDateTime} is {@code null}
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // -------------------------------------------------------------------------
    // Serialization backward-compatibility helpers
    // -------------------------------------------------------------------------

    /**
     * Converts an object read from a serialized stream to {@link LocalDate}.
     *
     * <p>During deserialization, a date field may arrive as either a {@link LocalDate}
     * (new format) or a {@link Date} (old format, before the Step 16 migration).
     * This method handles both cases transparently.
     *
     * @param raw the value read via {@code ObjectInputStream.GetField.get()}, may be {@code null}
     * @return the equivalent {@link LocalDate}, or {@code null} if {@code raw} is {@code null}
     * @throws IllegalArgumentException if {@code raw} is neither {@code null},
     *         {@code LocalDate}, nor {@code Date}
     */
    public static LocalDate readLocalDate(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof LocalDate) {
            return (LocalDate) raw;
        }
        if (raw instanceof Date) {
            return toLocalDate((Date) raw);
        }
        throw new IllegalArgumentException(
                "Cannot convert " + raw.getClass().getName() + " to LocalDate");
    }

    // -------------------------------------------------------------------------
    // Convenience factories
    // -------------------------------------------------------------------------

    /**
     * Returns today's date as a {@link LocalDate}.
     *
     * <p>Use this in place of {@code new Date()} when only the calendar date
     * (year/month/day) is relevant and no time-of-day is needed.
     *
     * @return today's {@link LocalDate} in the system default zone
     */
    public static LocalDate today() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    /**
     * Returns the current date and time as a {@link LocalDateTime}.
     *
     * <p>Use this in place of {@code new Date()} when a full timestamp is needed.
     *
     * @return the current {@link LocalDateTime} in the system default zone
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }
}
