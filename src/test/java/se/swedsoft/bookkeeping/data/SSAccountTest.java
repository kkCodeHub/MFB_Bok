package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSAccount}.
 */
class SSAccountTest {

    // ---- Default constructor ----

    @Test
    void defaultConstructorSetsNumberToNegativeOne() {
        SSAccount account = new SSAccount();

        // getNumber() returns null for iNumber < 1
        assertThat(account.getNumber()).isNull();
    }

    @Test
    void defaultConstructorSetsActiveTrue() {
        SSAccount account = new SSAccount();

        assertThat(account.isActive()).isTrue();
    }

    @Test
    void defaultConstructorSetsProjectRequiredFalse() {
        SSAccount account = new SSAccount();

        assertThat(account.isProjectRequired()).isFalse();
    }

    @Test
    void defaultConstructorSetsResultUnitRequiredFalse() {
        SSAccount account = new SSAccount();

        assertThat(account.isResultUnitRequired()).isFalse();
    }

    // ---- Number constructor ----

    @Test
    void numberConstructorSetsNumber() {
        SSAccount account = new SSAccount(1000);

        assertThat(account.getNumber()).isEqualTo(1000);
    }

    // ---- getNumber returns null for numbers < 1 ----

    @Test
    void getNumberReturnsNullForZero() {
        SSAccount account = new SSAccount(0);

        assertThat(account.getNumber()).isNull();
    }

    @Test
    void getNumberReturnsNullForNegative() {
        SSAccount account = new SSAccount(-5);

        assertThat(account.getNumber()).isNull();
    }

    @Test
    void getNumberReturnsValueForPositive() {
        SSAccount account = new SSAccount(1);

        assertThat(account.getNumber()).isEqualTo(1);
    }

    // ---- setNumber ----

    @Test
    void setNumberUpdatesNumber() {
        SSAccount account = new SSAccount();
        account.setNumber(2000);

        assertThat(account.getNumber()).isEqualTo(2000);
    }

    @Test
    void setNumberWithNullSetsToNegativeOneInternally() {
        SSAccount account = new SSAccount(1000);
        account.setNumber(null);

        assertThat(account.getNumber()).isNull();
    }

    // ---- Copy constructor ----

    @Test
    void copyConstructorCopiesAllFields() {
        SSAccount original = new SSAccount(1000);
        original.setDescription("Test account");
        original.setSRUCode("SRU1");
        original.setVATCode("VAT1");
        original.setReportCode("REP1");
        original.setActive(false);
        original.setProjectRequired(true);
        original.setResultUnitRequired(true);

        SSAccount copy = new SSAccount(original);

        assertThat(copy.getNumber()).isEqualTo(1000);
        assertThat(copy.getDescription()).isEqualTo("Test account");
        assertThat(copy.getSRUCode()).isEqualTo("SRU1");
        assertThat(copy.getVATCode()).isEqualTo("VAT1");
        assertThat(copy.getReportCode()).isEqualTo("REP1");
        assertThat(copy.isActive()).isFalse();
        assertThat(copy.isProjectRequired()).isTrue();
        assertThat(copy.isResultUnitRequired()).isTrue();
    }

    // ---- copyFrom ----

    @Test
    void copyFromUpdatesAllFields() {
        SSAccount source = new SSAccount(2000);
        source.setDescription("Source account");
        source.setSRUCode("SRU2");

        SSAccount target = new SSAccount();
        target.copyFrom(source);

        assertThat(target.getNumber()).isEqualTo(2000);
        assertThat(target.getDescription()).isEqualTo("Source account");
        assertThat(target.getSRUCode()).isEqualTo("SRU2");
    }

    // ---- Getters and setters for description, codes, flags ----

    @Test
    void descriptionGetterSetter() {
        SSAccount account = new SSAccount();
        account.setDescription("Revenue");

        assertThat(account.getDescription()).isEqualTo("Revenue");
    }

    @Test
    void sruCodeGetterSetter() {
        SSAccount account = new SSAccount();
        account.setSRUCode("7000");

        assertThat(account.getSRUCode()).isEqualTo("7000");
    }

    @Test
    void vatCodeGetterSetter() {
        SSAccount account = new SSAccount();
        account.setVATCode("25");

        assertThat(account.getVATCode()).isEqualTo("25");
    }

    @Test
    void reportCodeGetterSetter() {
        SSAccount account = new SSAccount();
        account.setReportCode("R1");

        assertThat(account.getReportCode()).isEqualTo("R1");
    }

    @Test
    void activeGetterSetter() {
        SSAccount account = new SSAccount();
        account.setActive(false);

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void projectRequiredGetterSetter() {
        SSAccount account = new SSAccount();
        account.setProjectRequired(true);

        assertThat(account.isProjectRequired()).isTrue();
    }

    @Test
    void resultUnitRequiredGetterSetter() {
        SSAccount account = new SSAccount();
        account.setResultUnitRequired(true);

        assertThat(account.isResultUnitRequired()).isTrue();
    }

    // ---- equals ----

    @Test
    void equalsByAccountNumber() {
        SSAccount a = new SSAccount(1000);
        SSAccount b = new SSAccount(1000);

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void notEqualForDifferentNumbers() {
        SSAccount a = new SSAccount(1000);
        SSAccount b = new SSAccount(2000);

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void notEqualToNull() {
        SSAccount account = new SSAccount(1000);

        assertThat(account.equals(null)).isFalse();
    }

    @Test
    void notEqualToDifferentType() {
        SSAccount account = new SSAccount(1000);

        assertThat(account.equals("not an account")).isFalse();
    }

    // ---- hashCode ----

    @Test
    void hashCodeEqualsAccountNumber() {
        SSAccount account = new SSAccount(1000);

        assertThat(account.hashCode()).isEqualTo(1000);
    }

    @Test
    void equalObjectsHaveSameHashCode() {
        SSAccount a = new SSAccount(1000);
        SSAccount b = new SSAccount(1000);

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ---- toString ----

    @Test
    void toStringContainsNumberAndDescription() {
        SSAccount account = new SSAccount(1000);
        account.setDescription("Cash");

        assertThat(account.toString()).isEqualTo("1000 - Cash");
    }

    // ---- toRenderString ----

    @Test
    void toRenderStringReturnsNumberAsString() {
        SSAccount account = new SSAccount(1000);

        assertThat(account.toRenderString()).isEqualTo("1000");
    }
}
