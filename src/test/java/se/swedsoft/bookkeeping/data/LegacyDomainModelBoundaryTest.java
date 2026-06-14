package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyDomainModelBoundaryTest {

    private static final Path MAIN_JAVA = Paths.get("src", "main", "java");

    private static final Map<String, Pattern> LEGACY_TYPE_PATTERNS = Map.of(
            "SSCompany", Pattern.compile("\\bSSCompany\\b"),
            "SSAccountingYear", Pattern.compile("\\bSSAccountingYear\\b"),
            "SSProject", Pattern.compile("\\bSSProject\\b"),
            "SSResultUnit", Pattern.compile("\\bSSResultUnit\\b"));

    private static final Set<Path> LEGACY_COMPATIBILITY_FILES = Set.of(
            MAIN_JAVA.resolve(Paths.get("se", "swedsoft", "bookkeeping", "data", "SSCompany.java")),
            MAIN_JAVA.resolve(Paths.get("se", "swedsoft", "bookkeeping", "data", "SSAccountingYear.java")),
            MAIN_JAVA.resolve(Paths.get("se", "swedsoft", "bookkeeping", "data", "SSProject.java")),
            MAIN_JAVA.resolve(Paths.get("se", "swedsoft", "bookkeeping", "data", "SSResultUnit.java")));

    @Test
    void shouldKeepLegacyDomainModelsInsideCompatibilityBoundaryOnly() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations)
                .withFailMessage("Legacy domain model references escaped the compatibility boundary:%n%s",
                        String.join(System.lineSeparator(), violations))
                .isEmpty();
    }

    private static void collectViolations(Path path, List<String> violations) {
        if (LEGACY_COMPATIBILITY_FILES.contains(path)) {
            return;
        }

        String content = read(path);
        String relativePath = MAIN_JAVA.relativize(path).toString().replace('\\', '/');

        for (Map.Entry<String, Pattern> entry : LEGACY_TYPE_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(content);
            while (matcher.find()) {
                violations.add(relativePath + ":" + lineNumber(content, matcher.start())
                        + " -> " + entry.getKey());
            }
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }

    private static int lineNumber(String content, int offset) {
        int line = 1;
        for (int index = 0; index < offset; index++) {
            if (content.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }
}

