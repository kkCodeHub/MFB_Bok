package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSVoucherRow;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSVoucherMath}.
 * Only tests row-level methods that do NOT depend on SSDB.
 */
class SSVoucherMathTest {

    // ---- getCreditMinusDebet(SSVoucherRow) ----

    @Test
    void getCreditMinusDebetWithBothValues() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("100.00"));
        row.setCredit(new BigDecimal("150.00"));

        BigDecimal result = SSVoucherMath.getCreditMinusDebet(row);

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void getCreditMinusDebetWithOnlyDebet() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("100.00"));

        BigDecimal result = SSVoucherMath.getCreditMinusDebet(row);

        assertThat(result).isEqualByComparingTo("-100.00");
    }

    @Test
    void getCreditMinusDebetWithOnlyCredit() {
        SSVoucherRow row = new SSVoucherRow();
        row.setCredit(new BigDecimal("100.00"));

        BigDecimal result = SSVoucherMath.getCreditMinusDebet(row);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void getCreditMinusDebetWithBothNull() {
        SSVoucherRow row = new SSVoucherRow();

        BigDecimal result = SSVoucherMath.getCreditMinusDebet(row);

        assertThat(result).isEqualByComparingTo("0");
    }

    // ---- getDebetMinusCredit(SSVoucherRow) ----

    @Test
    void getDebetMinusCreditWithBothValues() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("150.00"));
        row.setCredit(new BigDecimal("100.00"));

        BigDecimal result = SSVoucherMath.getDebetMinusCredit(row);

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void getDebetMinusCreditWithOnlyDebet() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("100.00"));

        BigDecimal result = SSVoucherMath.getDebetMinusCredit(row);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void getDebetMinusCreditWithOnlyCredit() {
        SSVoucherRow row = new SSVoucherRow();
        row.setCredit(new BigDecimal("100.00"));

        BigDecimal result = SSVoucherMath.getDebetMinusCredit(row);

        assertThat(result).isEqualByComparingTo("-100.00");
    }

    @Test
    void getDebetMinusCreditWithBothNull() {
        SSVoucherRow row = new SSVoucherRow();

        BigDecimal result = SSVoucherMath.getDebetMinusCredit(row);

        assertThat(result).isEqualByComparingTo("0");
    }

    // ---- setDebetMinusCredit ----

    @Test
    void setDebetMinusCreditPositiveSetsDebet() {
        SSVoucherRow row = new SSVoucherRow();

        SSVoucherMath.setDebetMinusCredit(row, new BigDecimal("100.00"));

        assertThat(row.getDebet()).isEqualByComparingTo("100.00");
        assertThat(row.getCredit()).isNull();
    }

    @Test
    void setDebetMinusCreditNegativeSetsCredit() {
        SSVoucherRow row = new SSVoucherRow();

        SSVoucherMath.setDebetMinusCredit(row, new BigDecimal("-100.00"));

        assertThat(row.getCredit()).isEqualByComparingTo("100.00");
        assertThat(row.getDebet()).isNull();
    }

    @Test
    void setDebetMinusCreditNullDoesNothing() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("50.00"));

        SSVoucherMath.setDebetMinusCredit(row, null);

        assertThat(row.getDebet()).isEqualByComparingTo("50.00");
    }

    // ---- setCreditMinusDebet ----

    @Test
    void setCreditMinusDebetPositiveSetsCredit() {
        SSVoucherRow row = new SSVoucherRow();

        SSVoucherMath.setCreditMinusDebet(row, new BigDecimal("100.00"));

        assertThat(row.getCredit()).isEqualByComparingTo("100.00");
        assertThat(row.getDebet()).isNull();
    }

    @Test
    void setCreditMinusDebetNegativeSetsDebet() {
        SSVoucherRow row = new SSVoucherRow();

        SSVoucherMath.setCreditMinusDebet(row, new BigDecimal("-100.00"));

        assertThat(row.getDebet()).isEqualByComparingTo("100.00");
        assertThat(row.getCredit()).isNull();
    }

    @Test
    void setCreditMinusDebetNullDoesNothing() {
        SSVoucherRow row = new SSVoucherRow();
        row.setCredit(new BigDecimal("50.00"));

        SSVoucherMath.setCreditMinusDebet(row, null);

        assertThat(row.getCredit()).isEqualByComparingTo("50.00");
    }

    // ---- Symmetry: getDebetMinusCredit is negation of getCreditMinusDebet ----

    @Test
    void debetMinusCreditIsNegationOfCreditMinusDebet() {
        SSVoucherRow row = new SSVoucherRow();
        row.setDebet(new BigDecimal("300.00"));
        row.setCredit(new BigDecimal("200.00"));

        BigDecimal dmc = SSVoucherMath.getDebetMinusCredit(row);
        BigDecimal cmd = SSVoucherMath.getCreditMinusDebet(row);

        assertThat(dmc).isEqualByComparingTo(cmd.negate());
    }
}
