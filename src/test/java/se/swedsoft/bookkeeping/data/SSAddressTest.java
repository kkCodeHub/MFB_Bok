package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSAddress}.
 */
class SSAddressTest {

    // ---- Default constructor ----

    @Test
    void defaultConstructorSetsAllFieldsToEmpty() {
        SSAddress addr = new SSAddress();

        assertThat(addr.getName()).isEmpty();
        assertThat(addr.getAddress1()).isEmpty();
        assertThat(addr.getAddress2()).isEmpty();
        assertThat(addr.getZipCode()).isEmpty();
        assertThat(addr.getCity()).isEmpty();
        assertThat(addr.getCountry()).isEmpty();
    }

    // ---- Parameterized constructor ----

    @Test
    void parameterizedConstructorSetsAllFields() {
        SSAddress addr = new SSAddress("Name", "Addr1", "Street1", "12345", "Stockholm", "Sweden");

        assertThat(addr.getName()).isEqualTo("Name");
        assertThat(addr.getAddress1()).isEqualTo("Addr1");
        assertThat(addr.getAddress2()).isEqualTo("Street1");
        assertThat(addr.getZipCode()).isEqualTo("12345");
        assertThat(addr.getCity()).isEqualTo("Stockholm");
        assertThat(addr.getCountry()).isEqualTo("Sweden");
    }

    // ---- Copy constructor ----

    @Test
    void copyConstructorCopiesAllFields() {
        SSAddress original = new SSAddress("Name", "Addr1", "Street1", "12345", "Stockholm", "Sweden");
        SSAddress copy = new SSAddress(original);

        assertThat(copy.getName()).isEqualTo("Name");
        assertThat(copy.getAddress1()).isEqualTo("Addr1");
        assertThat(copy.getAddress2()).isEqualTo("Street1");
        assertThat(copy.getZipCode()).isEqualTo("12345");
        assertThat(copy.getCity()).isEqualTo("Stockholm");
        assertThat(copy.getCountry()).isEqualTo("Sweden");
    }

    // ---- Setters ----

    @Test
    void settersUpdateFields() {
        SSAddress addr = new SSAddress();

        addr.setName("New Name");
        addr.setAddress1("New Addr");
        addr.setAddress2("New Street");
        addr.setZipCode("99999");
        addr.setCity("Gothenburg");
        addr.setCountry("Norway");

        assertThat(addr.getName()).isEqualTo("New Name");
        assertThat(addr.getAddress1()).isEqualTo("New Addr");
        assertThat(addr.getAddress2()).isEqualTo("New Street");
        assertThat(addr.getZipCode()).isEqualTo("99999");
        assertThat(addr.getCity()).isEqualTo("Gothenburg");
        assertThat(addr.getCountry()).isEqualTo("Norway");
    }

    // ---- getPostalAddress ----

    @Test
    void getPostalAddressCombinesZipCodeAndCity() {
        SSAddress addr = new SSAddress("Name", "Addr", "Street", "12345", "Stockholm", "Sweden");

        assertThat(addr.getPostalAddress()).isEqualTo("12345 Stockholm");
    }

    @Test
    void getPostalAddressWithEmptyFieldsReturnsSpaceSeparated() {
        SSAddress addr = new SSAddress();

        assertThat(addr.getPostalAddress()).isEqualTo(" ");
    }

    // ---- equals ----

    @Test
    void equalAddressesAreEqual() {
        SSAddress a = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");
        SSAddress b = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");

        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void differentAddressesAreNotEqual() {
        SSAddress a = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");
        SSAddress b = new SSAddress("Other", "Addr", "Street", "12345", "City", "Country");

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void addressNotEqualToNull() {
        SSAddress addr = new SSAddress();

        assertThat(addr.equals(null)).isFalse();
    }

    @Test
    void addressNotEqualToDifferentType() {
        SSAddress addr = new SSAddress();

        assertThat(addr.equals("not an address")).isFalse();
    }

    @Test
    void equalityIsReflexive() {
        SSAddress addr = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");

        assertThat(addr.equals(addr)).isTrue();
    }

    // ---- clone ----

    @Test
    void cloneCreatesEqualCopy() {
        SSAddress original = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");
        SSAddress cloned = original.clone();

        assertThat(cloned.equals(original)).isTrue();
        assertThat(cloned).isNotSameAs(original);
    }

    // ---- toString ----

    @Test
    void toStringContainsAllFields() {
        SSAddress addr = new SSAddress("John", "Box 1", "Main St", "12345", "Stockholm", "Sweden");
        String result = addr.toString();

        assertThat(result).contains("John");
        assertThat(result).contains("Box 1");
        assertThat(result).contains("Main St");
        assertThat(result).contains("12345");
        assertThat(result).contains("Stockholm");
        assertThat(result).contains("Sweden");
    }

    // ---- dispose ----

    @Test
    void disposeNullsAllFields() {
        SSAddress addr = new SSAddress("Name", "Addr", "Street", "12345", "City", "Country");

        addr.dispose();

        assertThat(addr.getName()).isNull();
        assertThat(addr.getAddress1()).isNull();
        assertThat(addr.getAddress2()).isNull();
        assertThat(addr.getZipCode()).isNull();
        assertThat(addr.getCity()).isNull();
        assertThat(addr.getCountry()).isNull();
    }
}
