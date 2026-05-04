package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSSupplier}.
 *
 * The default constructor calls {@code SSDB.getInstance().getCurrentCompany()}.
 * When no database is open, {@code getCurrentCompany()} returns {@code null}
 * and the guard {@code if (iCompany != null)} protects the body, so the
 * constructor is safe to use in unit tests without a running database.
 */
class SSSupplierTest {

    private SSSupplier blankSupplier() {
        return new SSSupplier();
    }

    // ---- Default constructor ----

    @Test
    void defaultConstructorCreatesNonNullAddress() {
        SSSupplier s = blankSupplier();

        assertThat(s.getAddress()).isNotNull();
    }

    // ---- Getters and setters ----

    @Test
    void numberGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setNumber("S001");

        assertThat(s.getNumber()).isEqualTo("S001");
    }

    @Test
    void nameGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setName("Leverantör AB");

        assertThat(s.getName()).isEqualTo("Leverantör AB");
    }

    @Test
    void phone1GetterSetter() {
        SSSupplier s = blankSupplier();
        s.setPhone1("08-111111");

        assertThat(s.getPhone1()).isEqualTo("08-111111");
    }

    @Test
    void phone2GetterSetter() {
        SSSupplier s = blankSupplier();
        s.setPhone2("070-222222");

        assertThat(s.getPhone2()).isEqualTo("070-222222");
    }

    @Test
    void telefaxGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setTelefax("08-333333");

        assertThat(s.getTelefax()).isEqualTo("08-333333");
    }

    @Test
    void emailGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setEMail("info@supplier.se");

        assertThat(s.getEMail()).isEqualTo("info@supplier.se");
    }

    @Test
    void homepageGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setHomepage("https://supplier.se");

        assertThat(s.getHomepage()).isEqualTo("https://supplier.se");
    }

    @Test
    void registrationNumberGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setRegistrationNumber("556001-0001");

        assertThat(s.getRegistrationNumber()).isEqualTo("556001-0001");
    }

    @Test
    void yourContactGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setYourContact("Eve");

        assertThat(s.getYourContact()).isEqualTo("Eve");
    }

    @Test
    void ourContactGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setOurContact("Frank");

        assertThat(s.getOurContact()).isEqualTo("Frank");
    }

    @Test
    void bankgiroGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setBankGiro("1234-5678");

        assertThat(s.getBankgiro()).isEqualTo("1234-5678");
    }

    @Test
    void plusgiroGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setPlusGiro("12 34 56-7");

        assertThat(s.getPlusgiro()).isEqualTo("12 34 56-7");
    }

    @Test
    void outpaymentNumberGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setOutpaymentNumber(42);

        assertThat(s.getOutpaymentNumber()).isEqualTo(42);
    }

    @Test
    void outpaymentNumberDefaultIsNull() {
        SSSupplier s = blankSupplier();

        assertThat(s.getOutpaymentNumber()).isNull();
    }

    @Test
    void ourCustomerNrGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setOurCustomerNr("C-EXT-001");

        assertThat(s.getOurCustomerNr()).isEqualTo("C-EXT-001");
    }

    @Test
    void commentGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setComment("Preferred supplier");

        assertThat(s.getComment()).isEqualTo("Preferred supplier");
    }

    @Test
    void paymentTermGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setPaymentTerm(null);

        assertThat(s.getPaymentTerm()).isNull();
    }

    @Test
    void deliveryTermGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setDeliveryTerm(null);

        assertThat(s.getDeliveryTerm()).isNull();
    }

    @Test
    void deliveryWayGetterSetter() {
        SSSupplier s = blankSupplier();
        s.setDeliveryWay(null);

        assertThat(s.getDeliveryWay()).isNull();
    }

    // ---- Copy constructor ----

    @Test
    void copyConstructorCopiesScalarFields() {
        SSSupplier original = blankSupplier();
        original.setNumber("S002");
        original.setName("Gamma Leverans AB");
        original.setPhone1("08-999888");
        original.setPhone2("073-777666");
        original.setTelefax("08-999887");
        original.setEMail("gamma@gamma.se");
        original.setHomepage("https://gamma.se");
        original.setRegistrationNumber("556777-0002");
        original.setYourContact("Hanna");
        original.setOurContact("Ivan");
        original.setOurCustomerNr("C-EXT-002");
        original.setBankGiro("5678-1234");
        original.setPlusGiro("56 78 12-3");

        SSSupplier copy = new SSSupplier(original);

        assertThat(copy.getNumber()).isEqualTo("S002");
        assertThat(copy.getName()).isEqualTo("Gamma Leverans AB");
        assertThat(copy.getPhone1()).isEqualTo("08-999888");
        assertThat(copy.getPhone2()).isEqualTo("073-777666");
        assertThat(copy.getTelefax()).isEqualTo("08-999887");
        assertThat(copy.getEMail()).isEqualTo("gamma@gamma.se");
        assertThat(copy.getHomepage()).isEqualTo("https://gamma.se");
        assertThat(copy.getRegistrationNumber()).isEqualTo("556777-0002");
        assertThat(copy.getYourContact()).isEqualTo("Hanna");
        assertThat(copy.getOurContact()).isEqualTo("Ivan");
        assertThat(copy.getOurCustomerNr()).isEqualTo("C-EXT-002");
        assertThat(copy.getBankgiro()).isEqualTo("5678-1234");
        assertThat(copy.getPlusgiro()).isEqualTo("56 78 12-3");
    }

    @Test
    void copyConstructorDeepCopiesAddress() {
        SSSupplier original = blankSupplier();
        original.getAddress().setAddress1("Old Road 1");

        SSSupplier copy = new SSSupplier(original);
        copy.getAddress().setAddress1("New Road 2");

        assertThat(original.getAddress().getAddress1()).isEqualTo("Old Road 1");
    }

    @Test
    void copyConstructorDoesNotCopyOutpaymentNumber() {
        // This documents a known bug: iOutpaymentNumber is not copied.
        SSSupplier original = blankSupplier();
        original.setOutpaymentNumber(99);

        SSSupplier copy = new SSSupplier(original);

        assertThat(copy.getOutpaymentNumber()).isNull();
    }

    @Test
    void copyConstructorDoesNotCopyComment() {
        // This documents a known bug: iComment is not copied.
        SSSupplier original = blankSupplier();
        original.setComment("Important note");

        SSSupplier copy = new SSSupplier(original);

        assertThat(copy.getComment()).isNull();
    }

    // ---- equals ----

    @Test
    void equalsByNumber() {
        SSSupplier a = blankSupplier();
        a.setNumber("S100");
        SSSupplier b = blankSupplier();
        b.setNumber("S100");

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void notEqualForDifferentNumbers() {
        SSSupplier a = blankSupplier();
        a.setNumber("S100");
        SSSupplier b = blankSupplier();
        b.setNumber("S200");

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equalsWhenNumberIsNullDelegatesToIdentity() {
        SSSupplier a = blankSupplier();  // iNumber == null
        SSSupplier b = blankSupplier();  // iNumber == null

        // super.equals == identity comparison; a and b are different objects
        assertThat(a.equals(b)).isFalse();
        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void notEqualToNonSupplier() {
        SSSupplier s = blankSupplier();
        s.setNumber("S100");

        assertThat(s.equals("not a supplier")).isFalse();
    }

    // ---- hashCode ----

    @Test
    void hashCodeIsNumberHashCode() {
        SSSupplier s = blankSupplier();
        s.setNumber("S100");

        assertThat(s.hashCode()).isEqualTo("S100".hashCode());
    }

    @Test
    void equalSuppliersHaveSameHashCode() {
        SSSupplier a = blankSupplier();
        a.setNumber("S100");
        SSSupplier b = blankSupplier();
        b.setNumber("S100");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ---- toRenderString ----

    @Test
    void toRenderStringReturnsNumber() {
        SSSupplier s = blankSupplier();
        s.setNumber("S999");

        assertThat(s.toRenderString()).isEqualTo("S999");
    }

    // ---- toString ----

    @Test
    void toStringContainsNumberAndName() {
        SSSupplier s = blankSupplier();
        s.setNumber("S001");
        s.setName("Supplier One");

        assertThat(s.toString()).isEqualTo("S001, Supplier One");
    }
}
