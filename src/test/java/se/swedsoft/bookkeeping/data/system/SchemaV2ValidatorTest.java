package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that the multischema V2 SQL scripts can be executed without errors
 * against an HSQLDB in-memory database.
 */
class SchemaV2ValidatorTest {

    @Test
    void schemaV2ShouldCreateAllTablesWithoutErrors() throws Exception {
        String url = "jdbc:hsqldb:mem:schema_v2_test_" + System.currentTimeMillis() + ";shutdown=true";

        try (Connection conn = DriverManager.getConnection(url, "SA", "")) {
            int executed = 0;
            int skipped = 0;
            StringBuilder errors = new StringBuilder();

            String[] schemaScripts = {
                    "/sql/create_tables_v2_Public.sql",
                    "/sql/create_tables_v2_Company.sql"
            };

            for (String schemaScript : schemaScripts) {
                InputStream is = getClass().getResourceAsStream(schemaScript);
                if (is == null) {
                    fail(schemaScript + " not found on classpath under /sql/");
                }
                String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // Remove single-line comments before splitting on ";"
                // so that semicolons inside comments do not create false splits.
                String stripped = Arrays.stream(sql.split("\n"))
                        .filter(line -> !line.trim().startsWith("--"))
                        .reduce("", (a, b) -> a + "\n" + b);

                String[] statements = stripped.split(";");

                for (String stmt : statements) {
                    String trimmed = stmt.trim();
                    if (trimmed.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    try (Statement s = conn.createStatement()) {
                        s.execute(trimmed);
                        executed++;
                    } catch (Exception e) {
                        errors.append("\n[FAILED] ")
                                .append(schemaScript)
                                .append(": ")
                                .append(trimmed, 0, Math.min(100, trimmed.length()))
                                .append("\n  -> ")
                                .append(e.getMessage())
                                .append("\n");
                    }
                }
            }            

            if (!errors.isEmpty()) {
                fail("Schema V2 validation failed (" + executed + " ok, "
                        + skipped + " skipped):" + errors);
            }

            assertThat(hasColumn(conn, "PUBLIC", "TBL_VOUCHER_EVENT_TYPE", "SYSTEM")).isTrue();
            assertThat(hasColumn(conn, "PUBLIC", "TBL_YEAR_VOUCHER_EVENT_SERIES_MAP", "ACTIVE")).isTrue();
            assertThat(isColumnNullable(conn, "PUBLIC", "TBL_VOUCHER_EVENT_TYPE", "EVENT_CODE")).isTrue();

            System.out.println("SchemaV2ValidatorTest: " + executed
                    + " statements executed successfully, " + skipped + " skipped.");
        }
    }

    private boolean hasColumn(Connection conn, String schema, String table, String column) throws Exception {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean isColumnNullable(Connection conn, String schema, String table, String column) throws Exception {
        String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return "YES".equalsIgnoreCase(resultSet.getString(1));
            }
        }
    }
}
