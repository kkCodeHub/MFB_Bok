package se.swedsoft.bookkeeping.print.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SSQuantityPrintUtilTest {

    @Test
    void toDisplayConvertsTenthsToDecimal() {
        assertThat(SSQuantityPrintUtil.toDisplay(25)).isEqualByComparingTo("2.5");
        assertThat(SSQuantityPrintUtil.toDisplay(0)).isEqualByComparingTo("0.0");
        assertThat(SSQuantityPrintUtil.toDisplay(null)).isNull();
    }
}

