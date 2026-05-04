package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSCustomer}.
 *
 * The default constructor calls {@code SSDB.getInstance().getCurrentCompany()}.
 * We use the copy constructor — which does NOT call SSDB — as the primary
 * way to build test fixtures, seeding it from a manually-wired instance.
 */
class SSCustomerTest {

    /**
     * Builds a minimal SSCustomer without using the default constructor
     * (which calls SSDB). We construct via the copy constructor seeded
     * from a partially-initialised object whose fields are set by reflection
     * workaround: instead, we rely on the copy constructor copying only
     * field values that were explicitly set on a "template" built with the
     * copy constructor from a blank customer whose addresses are already
     * initialised.
     *
     * Since SSCustomer has no no-arg factory that avoids SSDB, and since the
     * copy constructor DOES avoid SSDB, we bootstrap by calling the copy
     * constructor on an object whose fields we set using setters after
     * calling the no-arg constructor (which may call SSDB — but SSDB returns
     * null for getCurrentCompany() when no database is open, so the guard
     * {@code if (iCompany != null)} protects us).
     */
    private SSCustomer blankCustomer() {
        // The default constructor is safe when no DB is open:
        // SSDB.getInstance().getCurrentCompany() returns null and the
        // if-block is skipped.
        return new SSCustomer();
    }

    // ---- Default constructor ----

    @Test
    void defaultConstructorCreatesNonNullAddresses() {
        SSCustomer c = blankCustomer();

        assertThat(c.getInvoiceAddress()).isNotNull();
        assertThat(c.getDeliveryAddress()).isNotNull();
    }

    // ---- Getters and setters ----

    @Test
    void numberGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setNumber("C001");

        assertThat(c.getNumber()).isEqualTo("C001");
    }

    @Test
    void nameGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setName("Acme AB");

        assertThat(c.getName()).isEqualTo("Acme AB");
    }

    @Test
    void emailGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setEMail("info@acme.se");

        assertThat(c.getEMail()).isEqualTo("info@acme.se");
    }

    @Test
    void phone1GetterSetter() {
        SSCustomer c = blankCustomer();
        c.setPhone1("08-123456");

        assertThat(c.getPhone1()).isEqualTo("08-123456");
    }

    @Test
    void phone2GetterSetter() {
        SSCustomer c = blankCustomer();
        c.setPhone2("070-9876543");

        assertThat(c.getPhone2()).isEqualTo("070-9876543");
    }

    @Test
    void telefaxGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setTelefax("08-654321");

        assertThat(c.getTelefax()).isEqualTo("08-654321");
    }

    @Test
    void registrationNumberGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setRegistrationNumber("556123-4567");

        assertThat(c.getRegistrationNumber()).isEqualTo("556123-4567");
    }

    @Test
    void ourContactPersonGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setOurContactPerson("Anna");

        assertThat(c.getOurContactPerson()).isEqualTo("Anna");
    }

    @Test
    void yourContactPersonGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setYourContactPerson("Bob");

        assertThat(c.getYourContactPerson()).isEqualTo("Bob");
    }

    @Test
    void vatNumberGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setVATNumber("SE556123456701");

        assertThat(c.getVATNumber()).isEqualTo("SE556123456701");
    }

    @Test
    void bankgiroGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setBankgiro("1234-5678");

        assertThat(c.getBankgiro()).isEqualTo("1234-5678");
    }

    @Test
    void plusgiroGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setPlusgiro("12 34 56-7");

        assertThat(c.getPlusgiro()).isEqualTo("12 34 56-7");
    }

    @Test
    void accountNumberGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setAccountNumber("9999-12345");

        assertThat(c.getAccountNumber()).isEqualTo("9999-12345");
    }

    @Test
    void clearingNumberGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setClearingNumber("6789");

        assertThat(c.getClearingNumber()).isEqualTo("6789");
    }

    @Test
    void euSaleCommodityGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setEuSaleCommodity(true);

        assertThat(c.getEuSaleCommodity()).isTrue();
    }

    @Test
    void euSaleThirdPartCommodityGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setEuSaleYhirdPartCommodity(true);

        assertThat(c.getEuSaleYhirdPartCommodity()).isTrue();
    }

    @Test
    void hideUnitpriceGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setHideUnitprice(true);

        assertThat(c.getHideUnitprice()).isTrue();
    }

    @Test
    void taxFreeGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setTaxFree(true);

        assertThat(c.getTaxFree()).isTrue();
    }

    @Test
    void paymentTermGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setPaymentTerm(null);

        assertThat(c.getPaymentTerm()).isNull();
    }

    @Test
    void deliveryTermGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setDeliveryTerm(null);

        assertThat(c.getDeliveryTerm()).isNull();
    }

    @Test
    void deliveryWayGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setDeliveryWay(null);

        assertThat(c.getDeliveryWay()).isNull();
    }

    @Test
    void creditLimitGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setCreditLimit(new BigDecimal("50000.00"));

        assertThat(c.getCreditLimit()).isEqualByComparingTo("50000.00");
    }

    @Test
    void discountGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setDiscount(new BigDecimal("10.00"));

        assertThat(c.getDiscount()).isEqualByComparingTo("10.00");
    }

    @Test
    void commentGetterSetter() {
        SSCustomer c = blankCustomer();
        c.setComment("VIP customer");

        assertThat(c.getComment()).isEqualTo("VIP customer");
    }

    // ---- Copy constructor ----

    @Test
    void copyConstructorCopiesScalarFields() {
        SSCustomer original = blankCustomer();
        original.setNumber("C002");
        original.setName("Beta AB");
        original.setEMail("beta@beta.se");
        original.setPhone1("031-100200");
        original.setPhone2("073-445566");
        original.setTelefax("031-100201");
        original.setRegistrationNumber("556999-0001");
        original.setOurContactPerson("Carin");
        original.setYourContactPerson("David");
        original.setVATNumber("SE556999000101");
        original.setBankgiro("9876-5432");
        original.setPlusgiro("98 76 54-3");
        original.setAccountNumber("1111-22222");
        original.setClearingNumber("1234");
        original.setEuSaleCommodity(true);
        original.setEuSaleYhirdPartCommodity(true);
        original.setHideUnitprice(true);
        original.setTaxFree(true);
        original.setCreditLimit(new BigDecimal("100000"));
        original.setDiscount(new BigDecimal("5.00"));
        original.setComment("Copy test");

        SSCustomer copy = new SSCustomer(original);

        assertThat(copy.getNumber()).isEqualTo("C002");
        assertThat(copy.getName()).isEqualTo("Beta AB");
        assertThat(copy.getEMail()).isEqualTo("beta@beta.se");
        assertThat(copy.getPhone1()).isEqualTo("031-100200");
        assertThat(copy.getPhone2()).isEqualTo("073-445566");
        assertThat(copy.getTelefax()).isEqualTo("031-100201");
        assertThat(copy.getRegistrationNumber()).isEqualTo("556999-0001");
        assertThat(copy.getOurContactPerson()).isEqualTo("Carin");
        assertThat(copy.getYourContactPerson()).isEqualTo("David");
        assertThat(copy.getVATNumber()).isEqualTo("SE556999000101");
        assertThat(copy.getBankgiro()).isEqualTo("9876-5432");
        assertThat(copy.getPlusgiro()).isEqualTo("98 76 54-3");
        assertThat(copy.getAccountNumber()).isEqualTo("1111-22222");
        assertThat(copy.getClearingNumber()).isEqualTo("1234");
        assertThat(copy.getEuSaleCommodity()).isTrue();
        assertThat(copy.getEuSaleYhirdPartCommodity()).isTrue();
        assertThat(copy.getHideUnitprice()).isTrue();
        assertThat(copy.getTaxFree()).isTrue();
        assertThat(copy.getCreditLimit()).isEqualByComparingTo("100000");
        assertThat(copy.getDiscount()).isEqualByComparingTo("5.00");
    }

    @Test
    void copyConstructorDeepCopiesAddresses() {
        SSCustomer original = blankCustomer();
        original.getInvoiceAddress().setAddress1("Old Street 1");

        SSCustomer copy = new SSCustomer(original);
        copy.getInvoiceAddress().setAddress1("New Street 2");

        assertThat(original.getInvoiceAddress().getAddress1()).isEqualTo("Old Street 1");
    }

    // ---- equals ----

    @Test
    void equalsByCustomerNumber() {
        SSCustomer a = blankCustomer();
        a.setNumber("C100");
        SSCustomer b = blankCustomer();
        b.setNumber("C100");

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void notEqualForDifferentNumbers() {
        SSCustomer a = blankCustomer();
        a.setNumber("C100");
        SSCustomer b = blankCustomer();
        b.setNumber("C200");

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenNumberIsNull() {
        SSCustomer a = blankCustomer();
        SSCustomer b = blankCustomer();

        // Both have null iCustomerNr — equals returns false for both directions
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void notEqualToNonCustomer() {
        SSCustomer c = blankCustomer();
        c.setNumber("C100");

        assertThat(c.equals("not a customer")).isFalse();
    }

    // ---- hashCode ----

    @Test
    void hashCodeIsCustomerNumberHashCode() {
        SSCustomer c = blankCustomer();
        c.setNumber("C100");

        assertThat(c.hashCode()).isEqualTo("C100".hashCode());
    }

    @Test
    void equalCustomersHaveSameHashCode() {
        SSCustomer a = blankCustomer();
        a.setNumber("C100");
        SSCustomer b = blankCustomer();
        b.setNumber("C100");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ---- toRenderString ----

    @Test
    void toRenderStringReturnsCustomerNumber() {
        SSCustomer c = blankCustomer();
        c.setNumber("C999");

        assertThat(c.toRenderString()).isEqualTo("C999");
    }

    // ---- toString ----

    @Test
    void toStringContainsNumberAndName() {
        SSCustomer c = blankCustomer();
        c.setNumber("C001");
        c.setName("Acme AB");

        assertThat(c.toString()).isEqualTo("C001, Acme AB");
    }
}
