package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SSInpaymentRowMathTest {

    @Test
    void localValueMapsDirectlyToPaymentValue() {
        SSInpaymentRow iRow = new SSInpaymentRow();

        iRow.setLocalValue(new BigDecimal("60.00"));

        assertThat(iRow.getLocalValue()).isPresent();
        assertThat(iRow.getLocalValue().orElseThrow()).isEqualByComparingTo("60.00");
        assertThat(iRow.getValue()).isEqualByComparingTo("60.00");
    }

    @Test
    void getLocalValueReturnsStoredPaymentValue() {
        SSInpaymentRow iRow = new SSInpaymentRow();
        iRow.setValue(new BigDecimal("25.00"));

        assertThat(iRow.getValue()).isEqualByComparingTo("25.00");
        assertThat(iRow.getLocalValue().orElseThrow()).isEqualByComparingTo("25.00");
    }
}
