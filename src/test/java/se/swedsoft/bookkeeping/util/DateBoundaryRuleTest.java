package se.swedsoft.bookkeeping.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the date boundary rule: {@code java.util.Date} may only be used in {@code gui}, {@code print},
 * {@code importexport} (Apache POI) and {@code SSDateUtil}. The domain layers {@code data}, {@code calc}
 * and {@code persistence} must use {@code java.time}.
 */
class DateBoundaryRuleTest {

    private static final Path BASE = Paths.get("src", "main", "java", "se", "swedsoft", "bookkeeping");

    private static final String[] GUARDED_PACKAGES = {"data", "calc", "persistence"};

    private static final Pattern QUALIFIED = Pattern.compile("import\\s+java\\.util\\.Date\\s*;|java\\.util\\.Date\\b");

    // Simple-name use, which may also be a wildcard import of java.util.*
    private static final Pattern SIMPLE = Pattern.compile(
            "(?<![\\w.])Date\\s+\\w+\\s*[=;,)(]|\\(\\s*Date\\s*\\)|<\\s*Date\\s*>|new\\s+Date\\s*\\(");

    @Test
    void domainLayersDoNotUseJavaUtilDate() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String pkg : GUARDED_PACKAGES) {
            try (Stream<Path> files = Files.walk(BASE.resolve(pkg))) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    collectViolations(file, violations);
                }
            }
        }

        assertThat(violations).as("java.util.Date is only allowed in gui/print/importexport").isEmpty();
    }

    private static void collectViolations(Path file, List<String> violations) throws IOException {
        List<String> lines = Files.readAllLines(file);
        boolean sqlDate = lines.stream().anyMatch(l -> l.trim().equals("import java.sql.Date;"));
        boolean inBlockComment = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (inBlockComment) {
                inBlockComment = !line.contains("*/");
                continue;
            }
            if (line.startsWith("/*")) {
                inBlockComment = !line.contains("*/");
                continue;
            }
            if (line.startsWith("//") || line.startsWith("*")) {
                continue;
            }
            if (QUALIFIED.matcher(line).find() || (!sqlDate && SIMPLE.matcher(line).find())) {
                violations.add(BASE.relativize(file) + ":" + (i + 1) + ": " + line);
            }
        }
    }
}
