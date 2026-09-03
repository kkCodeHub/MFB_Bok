package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SSDBFacadeUsageBoundaryTest {

    private static final Path MAIN_JAVA = Paths.get("src", "main", "java");
    private static final Path SSDB_FILE = MAIN_JAVA.resolve(Paths.get(
            "se", "swedsoft", "bookkeeping", "data", "system", "SSDB.java"));

    /**
     * Phase 1 guardrail: direct SSDB singleton access must never grow while facade teardown is ongoing.
     */
    private static final int MAX_SSDB_GET_INSTANCE_OCCURRENCES = 1;

    /**
     * Existing stop rule: SSDB facade public API must not grow.
     */
    private static final int MAX_SSDB_PUBLIC_METHOD_DECLARATIONS = 204;

    private static final Pattern SSDB_PUBLIC_METHOD_PATTERN =
            Pattern.compile("(?m)^\\s*public\\s+(?!class\\b)[^=;\\n]*\\(");

    @Test
    void shouldNotIncreaseDirectSsdBGetInstanceUsageInProductionCode() throws IOException {
        int occurrences = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            occurrences = files.filter(path -> path.toString().endsWith(".java"))
                    .mapToInt(SSDBFacadeUsageBoundaryTest::countGetInstanceCalls)
                    .sum();
        }

        assertThat(occurrences)
                .withFailMessage("Direct SSDB.getInstance() usage grew above phase-5 option-A baseline (%s): %s",
                        MAX_SSDB_GET_INSTANCE_OCCURRENCES, occurrences)
                .isLessThanOrEqualTo(MAX_SSDB_GET_INSTANCE_OCCURRENCES);
    }

    @Test
    void shouldNotExpandSsdBPublicApiSurface() {
        String ssdbContent = read(SSDB_FILE);
        Matcher matcher = SSDB_PUBLIC_METHOD_PATTERN.matcher(ssdbContent);
        int declarations = 0;
        while (matcher.find()) {
            declarations++;
        }

        assertThat(declarations)
                .withFailMessage("SSDB public API grew above stop-rule baseline (%s): %s",
                        MAX_SSDB_PUBLIC_METHOD_DECLARATIONS, declarations)
                .isLessThanOrEqualTo(MAX_SSDB_PUBLIC_METHOD_DECLARATIONS);
    }

    private static int countGetInstanceCalls(Path path) {
        String content = read(path);
        return countOccurrences(content, "SSDB.getInstance(");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }

    private static int countOccurrences(String content, String needle) {
        int count = 0;
        int fromIndex = 0;
        while (true) {
            int found = content.indexOf(needle, fromIndex);
            if (found < 0) {
                return count;
            }
            count++;
            fromIndex = found + needle.length();
        }
    }
}
