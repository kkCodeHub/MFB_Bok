package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validates that create_tables_v2.sql can be executed without errors
 * against an HSQLDB in-memory database.
 *
 * <p>Steg 2.2 — Schema V2 syntax validation.</p>
 */
class SchemaV2ValidatorTest {

    @Test
    void schemaV2ShouldCreateAllTablesWithoutErrors() throws Exception {
        String url = "jdbc:hsqldb:mem:schema_v2_test_" + System.currentTimeMillis() + ";shutdown=true";

        try (Connection conn = DriverManager.getConnection(url, "SA", "")) {
            InputStream is = getClass().getResourceAsStream("/sql/create_tables_v2.sql");
            if (is == null) {
                fail("create_tables_v2.sql not found on classpath under /sql/");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Remove single-line comments before splitting on ";"
            // so that semicolons inside comments do not create false splits.
            String stripped = Arrays.stream(sql.split("\n"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b);

            String[] statements = stripped.split(";");
            int executed = 0;
            int skipped = 0;
            StringBuilder errors = new StringBuilder();

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
                          .append(trimmed, 0, Math.min(100, trimmed.length()))
                          .append("\n  -> ")
                          .append(e.getMessage())
                          .append("\n");
                }
            }

            if (!errors.isEmpty()) {
                fail("Schema V2 validation failed (" + executed + " ok, "
                        + skipped + " skipped):" + errors);
            }

            System.out.println("SchemaV2ValidatorTest: " + executed
                    + " statements executed successfully, " + skipped + " skipped.");
        }
    }
}


