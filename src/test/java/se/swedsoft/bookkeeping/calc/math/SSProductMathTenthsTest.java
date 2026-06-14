package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SSProductMathTenthsTest {

    @Test
    void quantityToDecimalConvertsTenths() {
        assertThat(SSProductMath.quantityToDecimal(25)).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(SSProductMath.quantityToDecimal(10)).isEqualByComparingTo(new BigDecimal("1.0"));
    }
}


