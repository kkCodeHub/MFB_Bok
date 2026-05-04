package se.swedsoft.bookkeeping.util;


import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link SSDateUtil}.
 */
class SSDateUtilTest {

    // -------------------------------------------------------------------------
    // toLocalDate(Date)
    // -------------------------------------------------------------------------

    @Test
    void toLocalDateReturnsNullForNull() {
        assertThat(SSDateUtil.toLocalDate(null)).isNull();
    }

    @Test
    void toLocalDateConvertsKnownDate() {
        LocalDate expected = LocalDate.of(2024, 6, 15);
        Date legacy = Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant());

        assertThat(SSDateUtil.toLocalDate(legacy)).isEqualTo(expected);
    }

    @Test
    void toLocalDateRoundTrip() {
        LocalDate original = LocalDate.of(2000, 1, 1);
        Date legacy = SSDateUtil.toDate(original);

        assertThat(SSDateUtil.toLocalDate(legacy)).isEqualTo(original);
    }

    // -------------------------------------------------------------------------
    // toLocalDateTime(Date)
    // -------------------------------------------------------------------------

    @Test
    void toLocalDateTimeReturnsNullForNull() {
        assertThat(SSDateUtil.toLocalDateTime(null)).isNull();
    }

    @Test
    void toLocalDateTimeConvertsKnownDateTime() {
        LocalDateTime expected = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
        Date legacy = Date.from(expected.atZone(ZoneId.systemDefault()).toInstant());

        assertThat(SSDateUtil.toLocalDateTime(legacy)).isEqualTo(expected);
    }

    @Test
    void toLocalDateTimeRoundTrip() {
        LocalDateTime original = LocalDateTime.of(1999, 12, 31, 23, 59, 59);
        Date legacy = SSDateUtil.toDate(original);

        assertThat(SSDateUtil.toLocalDateTime(legacy)).isEqualTo(original);
    }

    // -------------------------------------------------------------------------
    // toDate(LocalDate)
    // -------------------------------------------------------------------------

    @Test
    void toDateFromLocalDateReturnsNullForNull() {
        assertThat(SSDateUtil.toDate((LocalDate) null)).isNull();
    }

    @Test
    void toDateFromLocalDateIsAtStartOfDay() {
        LocalDate ld = LocalDate.of(2023, 3, 7);
        Date result = SSDateUtil.toDate(ld);

        LocalDateTime resultDateTime = result.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        assertThat(resultDateTime.getHour()).isZero();
        assertThat(resultDateTime.getMinute()).isZero();
        assertThat(resultDateTime.getSecond()).isZero();
    }

    @Test
    void toDateFromLocalDatePreservesDate() {
        LocalDate ld = LocalDate.of(2023, 3, 7);
        Date result = SSDateUtil.toDate(ld);

        assertThat(SSDateUtil.toLocalDate(result)).isEqualTo(ld);
    }

    // -------------------------------------------------------------------------
    // toDate(LocalDateTime)
    // -------------------------------------------------------------------------

    @Test
    void toDateFromLocalDateTimeReturnsNullForNull() {
        assertThat(SSDateUtil.toDate((LocalDateTime) null)).isNull();
    }

    @Test
    void toDateFromLocalDateTimePreservesDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2021, 11, 23, 14, 55, 0);
        Date result = SSDateUtil.toDate(ldt);

        assertThat(SSDateUtil.toLocalDateTime(result)).isEqualTo(ldt);
    }

    // -------------------------------------------------------------------------
    // today() / now()
    // -------------------------------------------------------------------------

    @Test
    void todayReturnsCurrentDate() {
        LocalDate before = LocalDate.now(ZoneId.systemDefault());
        LocalDate result = SSDateUtil.today();
        LocalDate after = LocalDate.now(ZoneId.systemDefault());

        assertThat(result).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void nowReturnsCurrentDateTime() {
        LocalDateTime before = LocalDateTime.now(ZoneId.systemDefault()).minusSeconds(1);
        LocalDateTime result = SSDateUtil.now();
        LocalDateTime after = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(1);

        assertThat(result).isAfter(before).isBefore(after);
    }
}
