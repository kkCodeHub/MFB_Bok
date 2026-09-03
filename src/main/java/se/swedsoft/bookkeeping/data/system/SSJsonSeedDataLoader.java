package se.swedsoft.bookkeeping.data.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Utility class for loading and parsing JSON seed data files.
 * Uses Jackson for robust deserialization that handles varying numbers of objects.
 */
public class SSJsonSeedDataLoader {
    private static final Logger LOG = LoggerFactory.getLogger(SSJsonSeedDataLoader.class);
    
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Load seed data from a JSON file.
     *
     * @param filePath path to the JSON file
     * @return JsonNode representing the parsed JSON structure
     * @throws IOException if file cannot be read or JSON is invalid
     */
    public static JsonNode loadSeedFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            String resourcePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            try (InputStream inputStream = SSJsonSeedDataLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new IOException("Seed file not found: " + filePath);
                }
                return MAPPER.readTree(inputStream);
            }
        }
        return MAPPER.readTree(file);
    }

    /**
     * Get all objects from a named array in the JSON structure.
     * Handles any number of objects robustly—if count changes between runs, iteration adapts automatically.
     *
     * @param seedData the parsed JSON root object
     * @param arrayKey the name of the array (e.g., "Valuta", "Standardenhet")
     * @return list of objects from the array; empty list if key doesn't exist or is not an array
     */
    public static List<JsonNode> getArrayObjects(JsonNode seedData, String arrayKey) {
        List<JsonNode> result = new ArrayList<>();
        
        if (seedData == null || !seedData.has(arrayKey)) {
            return result;
        }
        
        JsonNode arrayNode = seedData.get(arrayKey);
        if (!arrayNode.isArray()) {
            return result;
        }
        
        arrayNode.forEach(result::add);
        return result;
    }

    /**
     * Extract a field value from a JSON object node.
     * Safely handles missing fields by returning empty Optional.
     *
     * @param node the JSON object node
     * @param fieldName the field name to extract
     * @return Optional containing field value as string, or empty if not present
     */
    public static Optional<String> getStringField(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return Optional.empty();
        }
        JsonNode field = node.get(fieldName);
        if (field.isTextual()) {
            return Optional.of(field.asText());
        }
        return Optional.empty();
    }

    /**
     * Extract a field value from a JSON object node.
     * Safely handles missing fields by returning null.
     *
     * @param node the JSON object node
     * @param fieldName the field name to extract
     * @return field value as string, or null if not present
     */
    public static String getStringFieldOrNull(JsonNode node, String fieldName) {
        return getStringField(node, fieldName).orElse(null);
    }
}
