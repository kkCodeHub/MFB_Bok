package se.swedsoft.bookkeeping.gui.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SSQuantityPresentationUtilTest {

    @Test
    void testToDisplayQuantityFromTenths() {
        assertThat(SSQuantityPresentationUtil.toDisplayQuantity(25)).isEqualByComparingTo("2.5");
        assertThat(SSQuantityPresentationUtil.toDisplayQuantity(null)).isNull();
    }

    @Test
    void testToStoredTenthsFromBigDecimal() {
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("2.5"))).isEqualTo(25);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("2.55"))).isEqualTo(26);
    }

    @Test
    void testToStoredTenthsFromObject() {
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("3.0"))).isEqualTo(30);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(Integer.valueOf(7))).isEqualTo(7);
        assertThat(SSQuantityPresentationUtil.toStoredTenths((Object) null)).isNull();
    }

    @Test
    void testToDisplayQuantityEdgeCases() {
        assertThat(SSQuantityPresentationUtil.toDisplayQuantity(0)).isEqualByComparingTo("0.0");
        assertThat(SSQuantityPresentationUtil.toDisplayQuantity(-25)).isEqualByComparingTo("-2.5");
        assertThat(SSQuantityPresentationUtil.toDisplayQuantity(123456789)).isEqualByComparingTo("12345678.9");
    }

    @Test
    void testToStoredTenthsRoundingBoundaries() {
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("2.44"))).isEqualTo(24);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("2.45"))).isEqualTo(25);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("2.55"))).isEqualTo(26);
    }

    @Test
    void testToStoredTenthsNegativeAndLargeValues() {
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("-2.44"))).isEqualTo(-24);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("-2.45"))).isEqualTo(-25);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("-2.55"))).isEqualTo(-26);
        assertThat(SSQuantityPresentationUtil.toStoredTenths(new BigDecimal("12345678.95"))).isEqualTo(123456790);
    }
}

