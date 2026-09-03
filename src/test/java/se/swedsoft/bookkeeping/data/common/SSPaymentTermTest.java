package se.swedsoft.bookkeeping.data.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SSPaymentTermTest {

    @Test
    void addDaysUsesProvidedDateAndExplicitDays() {
        SSPaymentTerm paymentTerm = new SSPaymentTerm();
        paymentTerm.setName("Vid leverans");
        paymentTerm.setDays(10);

        LocalDate baseDate = LocalDate.of(2026, 8, 18);

        assertThat(paymentTerm.addDaysToLocalDate(baseDate))
                .isEqualTo(LocalDate.of(2026, 8, 28));
    }

    @Test
    void decodeValueFallsBackToNameWhenDaysMissing() {
        SSPaymentTerm paymentTerm = new SSPaymentTerm();
        paymentTerm.setName("30");

        assertThat(paymentTerm.getDays()).isNull();
        assertThat(paymentTerm.decodeValue()).isEqualTo(30);
    }
}
