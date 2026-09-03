package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSDateMath}.
 */
class SSDateMathTest {

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
}
