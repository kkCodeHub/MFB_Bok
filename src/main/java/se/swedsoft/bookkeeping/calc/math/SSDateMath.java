package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;


/**
 * Date utility class providing date arithmetic operations.
 *
 * <p>New code should use the {@link LocalDate} overloads.  The legacy
 * {@link Date} methods are retained for backward compatibility and
 * delegate to their {@code LocalDate} counterparts via {@link SSDateUtil}.
 */
public class SSDateMath {

    private SSDateMath() {}

    // -----------------------------------------------------------------------
    // Legacy Date methods (backward-compatible, delegate to LocalDate)
    // -----------------------------------------------------------------------

    /**
     * Sets the time parts of a date to 00:00:00 (start of day).
     *
     * @param pDate the date
     * @return a new {@link Date} at midnight of the same calendar day
     * @deprecated Use {@link LocalDate} directly; LocalDate has no time component.
     */
    @Deprecated
    public static Date floor(Date pDate) {
        LocalDate ld = SSDateUtil.toLocalDate(pDate);
        return SSDateUtil.toDate(ld);
    }

    /**
     * Sets the time parts of a date to 23:59:59.999 (end of day).
     *
     * @param pDate the date
     * @return a new {@link Date} at end-of-day of the same calendar day
     * @deprecated Use {@link LocalDate} directly; for range queries prefer
     *             exclusive upper bounds instead of end-of-day timestamps.
     */
    @Deprecated
    public static Date ceil(Date pDate) {
        LocalDate ld = SSDateUtil.toLocalDate(pDate);
        return SSDateUtil.toDate(ld.atTime(23, 59, 59, 999_000_000));
    }

    /**
     * Returns the first day of the month for the given date, at midnight.
     *
     * @param pDate the date
     * @return a new {@link Date} for the first day of that month
     * @deprecated Use {@link #getFirstDayInMonth(LocalDate)} instead.
     */
    @Deprecated
    public static Date getFirstDayInMonth(Date pDate) {
        return SSDateUtil.toDate(getFirstDayInMonth(SSDateUtil.toLocalDate(pDate)));
    }

    /**
     * Returns the last day of the month for the given date, at end-of-day.
     *
     * @param pDate the date
     * @return a new {@link Date} for the last day of that month
     * @deprecated Use {@link #getLastDayInMonth(LocalDate)} instead.
     */
    @Deprecated
    public static Date getLastDayMonth(Date pDate) {
        LocalDate last = getLastDayInMonth(SSDateUtil.toLocalDate(pDate));
        return SSDateUtil.toDate(last.atTime(23, 59, 59, 999_000_000));
    }

    /**
     * Counts the number of months from {@code iFrom} forward until reaching {@code iTo}.
     *
     * <p><strong>Note:</strong> this method returns 0 when {@code iFrom} is before
     * {@code iTo} (legacy quirk preserved for backward compatibility).
     *
     * @param iFrom the start date
     * @param iTo   the end date
     * @return the number of months, or 0 if {@code iFrom} is before {@code iTo}
     * @deprecated Use {@link #getMonthsBetween(LocalDate, LocalDate)} instead.
     */
    @Deprecated
    public static int getMonthsBetween(Date iFrom, Date iTo) {
        return getMonthsBetween(SSDateUtil.toLocalDate(iFrom), SSDateUtil.toLocalDate(iTo));
    }

    /**
     * Counts the number of days between two dates.
     *
     * @param iFrom the start date
     * @param iTo   the end date
     * @return the number of days, or 0 if {@code iTo} is before {@code iFrom}
     * @deprecated Use {@link #getDaysBetween(LocalDate, LocalDate)} instead.
     */
    @Deprecated
    public static int getDaysBetween(Date iFrom, Date iTo) {
        return getDaysBetween(SSDateUtil.toLocalDate(iFrom), SSDateUtil.toLocalDate(iTo));
    }

    /**
     * Adds the specified number of months to the given date.
     *
     * @param iDate  the base date
     * @param iCount the number of months to add (may be negative)
     * @return the new date
     * @deprecated Use {@link #addMonths(LocalDate, int)} instead.
     */
    @Deprecated
    public static Date addMonths(Date iDate, Integer iCount) {
        return SSDateUtil.toDate(addMonths(SSDateUtil.toLocalDate(iDate), iCount));
    }

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
