package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSMonth}.
 */
class SSMonthTest {

    private static Date makeDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // ---- Constructors and getters ----

    @Test
    void localDateSingleConstructorSetsFromAndToNull() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        SSMonth month = new SSMonth(date);

        assertThat(month.getLocalFrom()).isEqualTo(date);
        assertThat(month.getLocalTo()).isNull();
    }

    @Test
    void localDateTwoArgConstructorSetsBothDates() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2024, 6, 30);
        SSMonth month = new SSMonth(from, to);

        assertThat(month.getLocalFrom()).isEqualTo(from);
        assertThat(month.getLocalTo()).isEqualTo(to);
    }

    // ---- isBetween ----

    @Test
    void isBetweenReturnsTrueWhenFromDateIsInRange() {
        LocalDate monthDate = LocalDate.of(2024, 6, 15);
        SSMonth month = new SSMonth(monthDate);

        LocalDate rangeFrom = LocalDate.of(2024, 6, 1);
        LocalDate rangeTo = LocalDate.of(2024, 6, 30);

        assertThat(month.isBetween(rangeFrom, rangeTo)).isTrue();
    }

    @Test
    void isBetweenReturnsTrueWhenFromDateEqualsRangeStart() {
        LocalDate monthDate = LocalDate.of(2024, 6, 1);
        SSMonth month = new SSMonth(monthDate);

        LocalDate rangeFrom = LocalDate.of(2024, 6, 1);
        LocalDate rangeTo = LocalDate.of(2024, 6, 30);

        assertThat(month.isBetween(rangeFrom, rangeTo)).isTrue();
    }

    @Test
    void isBetweenReturnsTrueWhenFromDateEqualsRangeEnd() {
        LocalDate monthDate = LocalDate.of(2024, 6, 30);
        SSMonth month = new SSMonth(monthDate);

        LocalDate rangeFrom = LocalDate.of(2024, 6, 1);
        LocalDate rangeTo = LocalDate.of(2024, 6, 30);

        assertThat(month.isBetween(rangeFrom, rangeTo)).isTrue();
    }

    @Test
    void isBetweenReturnsFalseWhenFromDateIsOutsideRange() {
        LocalDate monthDate = LocalDate.of(2024, 7, 15);
        SSMonth month = new SSMonth(monthDate);

        LocalDate rangeFrom = LocalDate.of(2024, 6, 1);
        LocalDate rangeTo = LocalDate.of(2024, 6, 30);

        assertThat(month.isBetween(rangeFrom, rangeTo)).isFalse();
    }

    // ---- isDateInMonth ----

    @Test
    void isDateInMonthReturnsTrueForSameMonth() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1));

        assertThat(month.isDateInMonth(makeDate(2024, 6, 15))).isTrue();
    }

    @Test
    void isDateInMonthReturnsFalseForDifferentMonth() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1));

        assertThat(month.isDateInMonth(makeDate(2024, 7, 15))).isFalse();
    }

    @Test
    void isDateInMonthReturnsFalseForDifferentYear() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1));

        assertThat(month.isDateInMonth(makeDate(2025, 6, 15))).isFalse();
    }

    // ---- equals and hashCode ----

    @Test
    void equalMonthsSameYearAndMonth() {
        SSMonth a = new SSMonth(LocalDate.of(2024, 6, 1));
        SSMonth b = new SSMonth(LocalDate.of(2024, 6, 15));

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void differentMonthsNotEqual() {
        SSMonth a = new SSMonth(LocalDate.of(2024, 6, 1));
        SSMonth b = new SSMonth(LocalDate.of(2024, 7, 1));

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void hashCodeConsistentForSameMonth() {
        SSMonth a = new SSMonth(LocalDate.of(2024, 6, 1));
        SSMonth b = new SSMonth(LocalDate.of(2024, 6, 20));

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void notEqualToDifferentType() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1));

        assertThat(month.equals("not a month")).isFalse();
    }

    // ---- getName ----

    @Test
    void getNameContainsYear() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1));

        assertThat(month.getName()).contains("2024");
    }

    // ---- splitYearIntoMonths ----

    @Test
    void splitYearIntoMonthsReturnsCorrectNumberOfMonths() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        List<SSMonth> months = SSMonth.splitYearIntoMonths(from, to);

        assertThat(months).hasSize(12);
    }

    @Test
    void splitYearIntoMonthsSetsFromAndToForEachMonth() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        List<SSMonth> months = SSMonth.splitYearIntoMonths(from, to);

        assertThat(months).hasSize(3);

        // Each month should have both from and to set
        for (SSMonth m : months) {
            assertThat(m.getLocalFrom()).isNotNull();
            assertThat(m.getLocalTo()).isNotNull();
        }
    }

    @Test
    void splitYearIntoMonthsFirstMonthStartsOnFirstDay() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        List<SSMonth> months = SSMonth.splitYearIntoMonths(from, to);

        assertThat(months.get(0).getLocalFrom().getMonthValue()).isEqualTo(1);
        assertThat(months.get(0).getLocalFrom().getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void splitYearIntoMonthsHandlesPartialYear() {
        LocalDate from = LocalDate.of(2024, 7, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        List<SSMonth> months = SSMonth.splitYearIntoMonths(from, to);

        assertThat(months).hasSize(6);
    }

    @Test
    void splitYearIntoMonthsHandlesBrokenFiscalYear() {
        // A fiscal year from September to August
        LocalDate from = LocalDate.of(2023, 9, 1);
        LocalDate to = LocalDate.of(2024, 8, 31);

        List<SSMonth> months = SSMonth.splitYearIntoMonths(from, to);

        assertThat(months).hasSize(12);
    }

    @Test
    void compatibilityDateConstructorEqualsLocalDateConstructor() {
        SSMonth fromDate = new SSMonth(makeDate(2024, 6, 1), makeDate(2024, 6, 30));
        SSMonth fromLocalDate = new SSMonth(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        assertThat(fromDate).isEqualTo(fromLocalDate);
        assertThat(fromDate.hashCode()).isEqualTo(fromLocalDate.hashCode());
    }

    void compatibilityDateAccessorsMirrorLocalDateAccessors() {
        SSMonth month = new SSMonth(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        assertThat(month.getFrom()).isEqualTo(makeDate(2024, 6, 1));
        assertThat(month.getDate()).isEqualTo(makeDate(2024, 6, 1));
        assertThat(month.getTo()).isEqualTo(makeDate(2024, 6, 30));
    }
}
