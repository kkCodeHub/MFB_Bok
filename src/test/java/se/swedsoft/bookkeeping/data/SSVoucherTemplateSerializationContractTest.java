package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

class SSVoucherTemplateSerializationContractTest {

    @Test
    void voucherTemplateIsNotSerializable() {
        assertThat(Serializable.class.isAssignableFrom(SSVoucherTemplate.class)).isFalse();
    }

    @Test
    void voucherTemplateRowIsNotSerializable() {
        assertThat(Serializable.class.isAssignableFrom(SSVoucherTemplate.SSVoucherTemplateRow.class)).isFalse();
    }
}
