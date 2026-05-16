package se.swedsoft.bookkeeping.persistence.snapshot;

import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes account plans to/from compressed snapshots.
 * <p>
 * Format: CSV-like structure (one account per line) with gzip compression.
 * Metadata: schemaVersion, compressionFlag, checksum (SHA-256).
 * </p>
 */
public class AccountPlanSnapshot {

    private static final int SCHEMA_VERSION = 1;
    private static final String COMPRESSION_GZIP = "gzip";
    private static final String COMPRESSION_NONE = "none";

    /**
     * Serializes an account plan to a byte array (compressed).
     *
     * @param accountPlan the plan to serialize
     * @param compressionFlag "gzip" or "none"
     * @return serialized + compressed bytes, or null on error
     */
    public static byte[] serialize(SSAccountPlan accountPlan, String compressionFlag) {
        if (accountPlan == null) {
            return null;
        }

        try {
            String csv = serializeToCSV(accountPlan);
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

            if (COMPRESSION_GZIP.equalsIgnoreCase(compressionFlag)) {
                return compressGzip(bytes);
            } else {
                return bytes;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize account plan", e);
        }
    }

    /**
     * Deserializes a byte array back to an account plan.
     *
     * @param data the serialized bytes
     * @param compressionFlag "gzip" or "none"
     * @return reconstructed account plan, or null on error
     */
    public static SSAccountPlan deserialize(byte[] data, String compressionFlag) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            byte[] decompressed;
            if (COMPRESSION_GZIP.equalsIgnoreCase(compressionFlag)) {
                decompressed = decompressGzip(data);
            } else {
                decompressed = data;
            }

            String csv = new String(decompressed, StandardCharsets.UTF_8);
            return deserializeFromCSV(csv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize account plan", e);
        }
    }

    /**
     * Calculates SHA-256 checksum of data.
     *
     * @param data the data to hash
     * @return hex-encoded SHA-256 hash
     */
    public static String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Serializes a plan to CSV format.
     * Format: number|description|vatCode|sruCode|reportCode|active|projectRequired|resultUnitRequired
     */
    private static String serializeToCSV(SSAccountPlan accountPlan) {
        StringBuilder sb = new StringBuilder();

        // Header with schema version
        sb.append("# schemaVersion=").append(SCHEMA_VERSION).append("\n");
        sb.append("# name=").append(escape(accountPlan.getName())).append("\n");
        sb.append("# baseName=").append(escape(accountPlan.getBaseName())).append("\n");
        sb.append("# assessmentYear=").append(escape(accountPlan.getAssessementYear())).append("\n");
        sb.append("# type=").append(escape(accountPlan.getType() != null ? accountPlan.getType().toString() : "")).append("\n");

        // Account rows
        List<SSAccount> accounts = accountPlan.getAccounts();
        if (accounts != null) {
            for (SSAccount acc : accounts) {
                if (acc != null && acc.getNumber() != null) {
                    sb.append(acc.getNumber()).append("|")
                      .append(escape(acc.getDescription())).append("|")
                      .append(escape(acc.getVATCode())).append("|")
                      .append(escape(acc.getSRUCode())).append("|")
                      .append(escape(acc.getReportCode())).append("|")
                      .append(acc.isActive()).append("|")
                      .append(acc.isProjectRequired()).append("|")
                      .append(acc.isResultUnitRequired()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Deserializes from CSV format back to account plan.
     */
    private static SSAccountPlan deserializeFromCSV(String csv) {
        SSAccountPlan plan = new SSAccountPlan();
        List<SSAccount> accounts = new ArrayList<>();

        String[] lines = csv.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                // Parse metadata
                if (line.startsWith("# name=")) {
                    plan.setName(unescape(line.substring(7)));
                } else if (line.startsWith("# baseName=")) {
                    plan.setBaseName(unescape(line.substring(11)));
                } else if (line.startsWith("# assessmentYear=")) {
                    plan.setAssessementYear(unescape(line.substring(17)));
                }
                continue;
            }

            // Parse account row
            String[] fields = line.split("\\|", -1);
            if (fields.length >= 8) {
                try {
                    SSAccount acc = new SSAccount();
                    acc.setNumber(Integer.parseInt(fields[0]));
                    acc.setDescription(unescape(fields[1]));
                    acc.setVATCode(unescape(fields[2]));
                    acc.setSRUCode(unescape(fields[3]));
                    acc.setReportCode(unescape(fields[4]));
                    acc.setActive(Boolean.parseBoolean(fields[5]));
                    acc.setProjectRequired(Boolean.parseBoolean(fields[6]));
                    acc.setResultUnitRequired(Boolean.parseBoolean(fields[7]));
                    accounts.add(acc);
                } catch (NumberFormatException e) {
                    // Skip malformed row
                }
            }
        }

        plan.setAccounts(accounts);
        return plan;
    }

    /**
     * Escapes pipe characters in strings for CSV serialization.
     */
    private static String escape(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("|", "\\|").replace("\n", "\\n");
    }

    /**
     * Unescapes pipe characters from CSV.
     */
    private static String unescape(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        String result = str.replace("\\|", "|").replace("\\n", "\n");
        return result.isEmpty() ? null : result;
    }

    /**
     * Compresses bytes using gzip.
     */
    private static byte[] compressGzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    /**
     * Decompresses gzip bytes.
     */
    private static byte[] decompressGzip(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
        }
        return bos.toByteArray();
    }

    public static int getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public static String getCompressionGzip() {
        return COMPRESSION_GZIP;
    }

    public static String getCompressionNone() {
        return COMPRESSION_NONE;
    }
}

