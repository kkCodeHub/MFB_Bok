package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

class SSInventorySerializationContractTest {

    @Test
    void inventoryIsNotSerializable() {
        assertThat(Serializable.class.isAssignableFrom(SSInventory.class)).isFalse();
    }
}
