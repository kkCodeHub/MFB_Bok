package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class SSCompanyLegacySerializationCompatibilityTest {

    @Test
    void shouldDeserializeLegacyCompatibleCompanyPayload() throws Exception {
        SSCompany original = new SSCompany();
        Map<SSStandardText, String> standardTexts = new HashMap<>();
        standardTexts.put(SSStandardText.Tender, "Legacy payload text");

        setField(original, "iId", new UID());
        setField(original, "iName", "Legacy AB");
        setField(original, "iTaxRegistered", true);
        setField(original, "iStandardTexts", standardTexts);
        setField(original, "iProjects", new LinkedList<SSProject>());

        byte[] payload = serialize(original);
        SSCompany deserialized = deserialize(payload);

        assertThat(deserialized).isNotNull();
        assertThat(fieldValue(deserialized, "iName")).isEqualTo("Legacy AB");
        assertThat(fieldValue(deserialized, "iTaxRegistered")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<SSStandardText, String> deserializedTexts =
                (Map<SSStandardText, String>) fieldValue(deserialized, "iStandardTexts");

        assertThat(deserializedTexts)
                .containsEntry(SSStandardText.Tender, "Legacy payload text");
        assertThat(fieldValue(deserialized, "iProjects")).isInstanceOf(LinkedList.class);
    }

    private static byte[] serialize(SSCompany company) throws Exception {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(company);
            out.flush();
            return bytes.toByteArray();
        }
    }

    private static SSCompany deserialize(byte[] payload) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            return (SSCompany) in.readObject();
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object fieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
