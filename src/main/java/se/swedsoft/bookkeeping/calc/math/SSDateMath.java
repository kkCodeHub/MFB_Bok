package se.swedsoft.bookkeeping.calc.math;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;


/**
 * Date utility class providing date arithmetic operations.
 *
 * <p>All methods operate on {@link java.time.LocalDate} values.
 */
public class SSDateMath {

    private SSDateMath() {}


    // -----------------------------------------------------------------------
    // Modern LocalDate methods
    // -----------------------------------------------------------------------

    /**
     * Returns the first day of the month for the given date.
     *
     * @param date the date
     * @return the first day of the month
     */
    public static LocalDate getFirstDayInMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Returns the last day of the month for the given date.
     *
     * @param date the date
     * @return the last day of the month
     */
    public static LocalDate getLastDayInMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Counts the number of months between two dates.
     *
     * <p><strong>Note:</strong> returns 0 when {@code from} is before {@code to}
     * (legacy quirk preserved for backward compatibility).
     *
     * @param from the start date
     * @param to   the end date
     * @return the number of months, or 0 if {@code from} is before {@code to}
     */
    public static int getMonthsBetween(LocalDate from, LocalDate to) {
        if (from.isBefore(to)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(to, from);
    }

    /**
     * Counts the number of days between two dates.
     *
     * @param from the start date
     * @param to   the end date
     * @return the number of days, or 0 if {@code to} is before {@code from}
     */
    public static int getDaysBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(from, to);
    }

    /**
     * Adds the specified number of months to the given date.
     *
     * @param date  the base date
     * @param count the number of months to add (may be negative)
     * @return the new date
     */
    public static LocalDate addMonths(LocalDate date, int count) {
        return date.plusMonths(count);
    }
}
