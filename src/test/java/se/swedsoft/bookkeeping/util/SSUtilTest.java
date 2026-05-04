package se.swedsoft.bookkeeping.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SSUtil}.
 */
class SSUtilTest {

    // ---- isNullOrEmpty ----

    @Test
    void isNullOrEmptyReturnsTrueForNull() {
        assertThat(SSUtil.isNullOrEmpty(null)).isTrue();
    }

    @Test
    void isNullOrEmptyReturnsTrueForEmptyString() {
        assertThat(SSUtil.isNullOrEmpty("")).isTrue();
    }

    @Test
    void isNullOrEmptyReturnsFalseForNonEmptyString() {
        assertThat(SSUtil.isNullOrEmpty("hello")).isFalse();
    }

    @Test
    void isNullOrEmptyReturnsFalseForWhitespace() {
        assertThat(SSUtil.isNullOrEmpty(" ")).isFalse();
    }

    // ---- verifyArgument ----

    @Test
    void verifyArgumentDoesNothingWhenConditionIsTrue() {
        SSUtil.verifyArgument("should pass", true);
        // no exception means success
    }

    @Test
    void verifyArgumentThrowsIllegalArgumentExceptionWhenConditionIsFalse() {
        assertThatThrownBy(() -> SSUtil.verifyArgument("bad value", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bad value");
    }

    // ---- verifyNotNull ----

    @Test
    void verifyNotNullDoesNothingWhenAllNonNull() {
        SSUtil.verifyNotNull("should pass", "a", "b", "c");
        // no exception means success
    }

    @Test
    void verifyNotNullThrowsNullPointerExceptionWhenNullPresent() {
        assertThatThrownBy(() -> SSUtil.verifyNotNull("has null", "a", null, "c"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("has null");
    }

    @Test
    void verifyNotNullThrowsForFirstNull() {
        assertThatThrownBy(() -> SSUtil.verifyNotNull("first null", (Object) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("first null");
    }

    // ---- convertNullToEmpty ----

    @Test
    void convertNullToEmptyReturnsEmptyForNull() {
        assertThat(SSUtil.convertNullToEmpty(null)).isEqualTo("");
    }

    @Test
    void convertNullToEmptyReturnsOriginalForNonNull() {
        assertThat(SSUtil.convertNullToEmpty("hello")).isEqualTo("hello");
    }

    @Test
    void convertNullToEmptyReturnsEmptyStringUnchanged() {
        assertThat(SSUtil.convertNullToEmpty("")).isEqualTo("");
    }

    // ---- isInRage ----

    @Test
    void isInRageReturnsTrueWhenInRange() {
        assertThat(SSUtil.isInRage(5, 1, 10)).isTrue();
    }

    @Test
    void isInRageReturnsTrueAtLowerBound() {
        assertThat(SSUtil.isInRage(1, 1, 10)).isTrue();
    }

    @Test
    void isInRageReturnsTrueAtUpperBound() {
        assertThat(SSUtil.isInRage(10, 1, 10)).isTrue();
    }

    @Test
    void isInRageReturnsFalseBelowRange() {
        assertThat(SSUtil.isInRage(0, 1, 10)).isFalse();
    }

    @Test
    void isInRageReturnsFalseAboveRange() {
        assertThat(SSUtil.isInRage(11, 1, 10)).isFalse();
    }

    @Test
    void isInRageReturnsTrueWhenLowEqualsHigh() {
        assertThat(SSUtil.isInRage(5, 5, 5)).isTrue();
    }

    @Test
    void isInRageThrowsWhenLowGreaterThanHigh() {
        assertThatThrownBy(() -> SSUtil.isInRage(5, 10, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- readResourceToString ----

    @Test
    void readResourceToStringThrowsForMissingResource() {
        assertThatThrownBy(() -> SSUtil.readResourceToString("nonexistent/resource.txt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Resource not found");
    }
}
