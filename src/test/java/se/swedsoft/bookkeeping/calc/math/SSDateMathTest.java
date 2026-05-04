package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSDateMath}.
 */
class SSDateMathTest {

    /**
     * Helper: create a Date for the given year/month/day at the given hour/min/sec.
     */
    private static Date makeDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // Calendar months are 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date makeDate(int year, int month, int day) {
        return makeDate(year, month, day, 12, 0, 0);
    }

    // ---- floor / ceil compatibility ----

    @Test
    void floorSetsTimeTo000000() {
        Date input = makeDate(2024, 6, 15, 14, 30, 45);
        Date result = SSDateMath.floor(input);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);

        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0);
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0);
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0);
    }

    @Test
    void floorPreservesDatePart() {
        Date input = makeDate(2024, 6, 15, 14, 30, 45);
        Date result = SSDateMath.floor(input);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);

        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2024);
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(Calendar.JUNE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(15);
    }

    @Test
    void ceilCompatibilitySetsTimeTo235959() {
        Date input = makeDate(2024, 6, 15, 10, 0, 0);
        Date result = SSDateMath.ceil(input);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);

        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(59);
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(59);
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(999);
    }

    // ---- getFirstDayInMonth (LocalDate) ----

    @Test
    void getFirstDayInMonthLocalDateReturnsFirstDay() {
        LocalDate input = LocalDate.of(2024, 6, 15);
        LocalDate result = SSDateMath.getFirstDayInMonth(input);

        assertThat(result).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    // ---- getLastDayInMonth (LocalDate) ----

    @Test
    void getLastDayInMonthLocalDateReturnsLastDay() {
        LocalDate input = LocalDate.of(2024, 6, 10);
        LocalDate result = SSDateMath.getLastDayInMonth(input);

        assertThat(result).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    void getLastDayInMonthForFebruaryLeapYearLocalDate() {
        LocalDate input = LocalDate.of(2024, 2, 5);
        LocalDate result = SSDateMath.getLastDayInMonth(input);

        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void getLastDayInMonthForFebruaryNonLeapYearLocalDate() {
        LocalDate input = LocalDate.of(2023, 2, 5);
        LocalDate result = SSDateMath.getLastDayInMonth(input);

        assertThat(result).isEqualTo(LocalDate.of(2023, 2, 28));
    }

    // ---- getDaysBetween (LocalDate) ----

    @Test
    void getDaysBetweenLocalDateForSameDate() {
        LocalDate date = LocalDate.of(2024, 6, 15);

        assertThat(SSDateMath.getDaysBetween(date, date)).isEqualTo(0);
    }

    @Test
    void getDaysBetweenLocalDateForConsecutiveDays() {
        LocalDate from = LocalDate.of(2024, 6, 15);
        LocalDate to = LocalDate.of(2024, 6, 16);

        assertThat(SSDateMath.getDaysBetween(from, to)).isEqualTo(1);
    }

    @Test
    void getDaysBetweenLocalDateForOneWeek() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 8);

        assertThat(SSDateMath.getDaysBetween(from, to)).isEqualTo(7);
    }

    @Test
    void getDaysBetweenLocalDateReturnsZeroWhenToBeforeFrom() {
        LocalDate from = LocalDate.of(2024, 6, 15);
        LocalDate to = LocalDate.of(2024, 6, 10);

        assertThat(SSDateMath.getDaysBetween(from, to)).isEqualTo(0);
    }

    // ---- getMonthsBetween (LocalDate) ----

    @Test
    void getMonthsBetweenLocalDateReturnsZeroWhenFromBeforeTo() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 6, 1);

        assertThat(SSDateMath.getMonthsBetween(from, to)).isEqualTo(0);
    }

    @Test
    void getMonthsBetweenLocalDateCountsMonthsWhenFromAfterTo() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 1, 1);

        assertThat(SSDateMath.getMonthsBetween(from, to)).isEqualTo(5);
    }

    // ---- addMonths (LocalDate) ----

    @Test
    void addMonthsLocalDateAddsPositiveMonths() {
        LocalDate input = LocalDate.of(2024, 1, 15);
        LocalDate result = SSDateMath.addMonths(input, 3);

        assertThat(result).isEqualTo(LocalDate.of(2024, 4, 15));
    }

    @Test
    void addMonthsLocalDateSubtractsNegativeMonths() {
        LocalDate input = LocalDate.of(2024, 6, 15);
        LocalDate result = SSDateMath.addMonths(input, -3);

        assertThat(result).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void addMonthsLocalDateWrapsToNextYear() {
        LocalDate input = LocalDate.of(2024, 11, 15);
        LocalDate result = SSDateMath.addMonths(input, 3);

        assertThat(result).isEqualTo(LocalDate.of(2025, 2, 15));
    }

    @Test
    void addMonthsLocalDateWithZeroReturnsSameMonth() {
        LocalDate input = LocalDate.of(2024, 6, 15);
        LocalDate result = SSDateMath.addMonths(input, 0);

        assertThat(result).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    void compatibilityMethodsMatchLocalDateConversions() {
        Date input = makeDate(2024, 6, 15, 14, 30, 45);
        LocalDate localDate = SSDateUtil.toLocalDate(input);

        assertThat(SSDateMath.getFirstDayInMonth(localDate))
                .isEqualTo(SSDateUtil.toLocalDate(SSDateMath.getFirstDayInMonth(input)));
        assertThat(SSDateMath.getLastDayInMonth(localDate))
                .isEqualTo(SSDateUtil.toLocalDate(SSDateMath.getLastDayMonth(input)));
        assertThat(SSDateMath.getDaysBetween(localDate, localDate.plusDays(7)))
                .isEqualTo(SSDateMath.getDaysBetween(input, SSDateUtil.toDate(localDate.plusDays(7))));
        assertThat(SSDateMath.getMonthsBetween(localDate.plusMonths(5), localDate))
                .isEqualTo(SSDateMath.getMonthsBetween(SSDateUtil.toDate(localDate.plusMonths(5)), input));
        assertThat(SSDateMath.addMonths(localDate, 3))
                .isEqualTo(SSDateUtil.toLocalDate(SSDateMath.addMonths(input, 3)));
    }
}
