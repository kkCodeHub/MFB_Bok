package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSVoucher}.
 *
 * The default constructor calls {@code doAutoIncrecement()} which in turn
 * calls {@code SSVoucherMath.getMaxNumber()} — an SSDB-dependent operation.
 * We avoid that by using the {@code SSVoucher(Integer)} constructor and by
 * calling methods that do not touch SSDB.
 */
class SSVoucherTest {

    /** Creates a minimal voucher without triggering the SSDB auto-increment. */
    private SSVoucher newVoucher(int number) {
        SSVoucher v = new SSVoucher(number);
        v.setLocalDate(LocalDate.of(1970, 1, 1));
        return v;
    }

    // ---- SSVoucher(Integer) constructor ----

    @Test
    void integerConstructorSetsNumber() {
        SSVoucher v = new SSVoucher(42);

        assertThat(v.getNumber()).isEqualTo(42);
    }

    @Test
    void integerConstructorInitialisesEmptyRowList() {
        SSVoucher v = new SSVoucher(1);

        assertThat(v.getRows()).isNotNull().isEmpty();
    }

    // ---- setNumber / getNumber ----

    @Test
    void setNumberUpdatesNumber() {
        SSVoucher v = newVoucher(1);
        v.setNumber(99);

        assertThat(v.getNumber()).isEqualTo(99);
    }

    // ---- setLocalDate / getLocalDate ----

    @Test
    void setLocalDateUpdatesDate() {
        SSVoucher v = newVoucher(1);
        LocalDate d = LocalDate.of(2024, 6, 15);
        v.setLocalDate(d);

        assertThat(v.getLocalDate()).isEqualTo(d);
    }

    // ---- setDescription / getDescription ----

    @Test
    void setDescriptionUpdatesDescription() {
        SSVoucher v = newVoucher(1);
        v.setDescription("Test voucher");

        assertThat(v.getDescription()).isEqualTo("Test voucher");
    }

    // ---- setCorrects / getCorrects ----

    @Test
    void setCorrectsStoresReference() {
        SSVoucher v = newVoucher(1);
        SSVoucher corrects = newVoucher(2);
        v.setCorrects(corrects);

        assertThat(v.getCorrects()).isSameAs(corrects);
    }

    // ---- setCorrectedBy / getCorrectedBy ----

    @Test
    void setCorrectedByStoresReference() {
        SSVoucher v = newVoucher(1);
        SSVoucher correctedBy = newVoucher(3);
        v.setCorrectedBy(correctedBy);

        assertThat(v.getCorrectedBy()).isSameAs(correctedBy);
    }

    // ---- addVoucherRow(SSVoucherRow) ----

    @Test
    void addVoucherRowAppendsRow() {
        SSVoucher v = newVoucher(1);
        SSVoucherRow row = new SSVoucherRow();
        v.addVoucherRow(row);

        assertThat(v.getRows()).hasSize(1).contains(row);
    }

    // ---- addVoucherRow(SSAccount, BigDecimal, BigDecimal) ----

    @Test
    void addVoucherRowWithDebetAndCreditSetsFields() {
        SSVoucher v = newVoucher(1);
        SSAccount account = new SSAccount(1000);
        v.addVoucherRow(account, new BigDecimal("100.00"), new BigDecimal("50.00"));

        List<SSVoucherRow> rows = v.getRows();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDebet()).isEqualByComparingTo("100.00");
        assertThat(rows.get(0).getCredit()).isEqualByComparingTo("50.00");
    }

    // ---- addVoucherRow(SSAccount, BigDecimal value) ----

    @Test
    void addVoucherRowWithPositiveValueSetsDebet() {
        SSVoucher v = newVoucher(1);
        SSAccount account = new SSAccount(1000);
        v.addVoucherRow(account, new BigDecimal("200.00"));

        SSVoucherRow row = v.getRows().get(0);
        assertThat(row.getDebet()).isEqualByComparingTo("200.00");
        assertThat(row.getCredit()).isNull();
    }

    @Test
    void addVoucherRowWithNegativeValueSetsCredit() {
        SSVoucher v = newVoucher(1);
        SSAccount account = new SSAccount(1000);
        v.addVoucherRow(account, new BigDecimal("-150.00"));

        SSVoucherRow row = v.getRows().get(0);
        assertThat(row.getCredit()).isEqualByComparingTo("150.00");
        assertThat(row.getDebet()).isNull();
    }

    @Test
    void addVoucherRowWithZeroValueDoesNotAddRow() {
        SSVoucher v = newVoucher(1);
        v.addVoucherRow(new SSAccount(1000), BigDecimal.ZERO);

        assertThat(v.getRows()).isEmpty();
    }

    @Test
    void addVoucherRowWithNullValueDoesNotAddRow() {
        SSVoucher v = newVoucher(1);
        v.addVoucherRow(new SSAccount(1000), (BigDecimal) null);

        assertThat(v.getRows()).isEmpty();
    }

    // ---- removeVoucherRow ----

    @Test
    void removeVoucherRowRemovesExistingRow() {
        SSVoucher v = newVoucher(1);
        SSVoucherRow row = new SSVoucherRow();
        v.addVoucherRow(row);

        boolean removed = v.removeVoucherRow(row);

        assertThat(removed).isTrue();
        assertThat(v.getRows()).isEmpty();
    }

    @Test
    void removeVoucherRowReturnsFalseForAbsentRow() {
        SSVoucher v = newVoucher(1);
        SSVoucherRow row = new SSVoucherRow();

        boolean removed = v.removeVoucherRow(row);

        assertThat(removed).isFalse();
    }

    // ---- setVoucherRows ----

    @Test
    void setVoucherRowsReplacesRowList() {
        SSVoucher v = newVoucher(1);
        v.addVoucherRow(new SSVoucherRow());

        List<SSVoucherRow> newRows = List.of(new SSVoucherRow(), new SSVoucherRow());
        v.setVoucherRows(newRows);

        assertThat(v.getRows()).hasSize(2);
    }

    // ---- copyFrom / copy constructor ----

    @Test
    void copyFromCopiesAllScalarFields() {
        SSVoucher original = newVoucher(7);
        original.setDescription("Original");
        // Use a day-boundary Date since fields are now LocalDate internally
        LocalDate date = LocalDate.of(2024, 3, 10);
        original.setLocalDate(date);

        SSVoucher copy = new SSVoucher(original);

        assertThat(copy.getNumber()).isEqualTo(7);
        assertThat(copy.getDescription()).isEqualTo("Original");
        assertThat(copy.getLocalDate()).isEqualTo(date);
    }

    @Test
    void copyFromDeepCopiesVoucherRows() {
        SSVoucher original = newVoucher(7);
        SSVoucherRow row = new SSVoucherRow();
        original.addVoucherRow(row);

        SSVoucher copy = new SSVoucher(original);
        copy.getRows().clear();

        assertThat(original.getRows()).hasSize(1);
    }

    // ---- equals ----

    @Test
    void equalsByNumber() {
        SSVoucher a = newVoucher(10);
        SSVoucher b = newVoucher(10);

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void notEqualForDifferentNumbers() {
        SSVoucher a = newVoucher(10);
        SSVoucher b = newVoucher(20);

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void notEqualToNonVoucher() {
        SSVoucher v = newVoucher(10);

        assertThat(v.equals("not a voucher")).isFalse();
    }

    // ---- toRenderString ----

    @Test
    void toRenderStringReturnsNumberAsString() {
        SSVoucher v = newVoucher(42);

        assertThat(v.toRenderString()).isEqualTo("42");
    }
}
