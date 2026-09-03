package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SSProductTest {

    @Test
    void equalProductsHaveSameHashCode() {
        SSProduct first = new SSProduct();
        first.setNumber("P-100");

        SSProduct second = new SSProduct();
        second.setNumber("P-100");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void hashMapLookupWorksForSeparateInstancesWithSameProductNumber() {
        SSProduct stored = new SSProduct();
        stored.setNumber("P-200");

        SSProduct lookup = new SSProduct();
        lookup.setNumber("P-200");

        Map<SSProduct, Integer> quantities = new HashMap<>();
        quantities.put(stored, 7);

        assertThat(quantities.get(lookup)).isEqualTo(7);
    }
}

