import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class ConvertProperties {
    public static void main(String[] args) throws Exception {
        Path file = Paths.get("src/main/resources/book.properties");
        // Read with ISO-8859-1 (original encoding)
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, "ISO-8859-1");

        // Write with UTF-8
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Successfully converted book.properties to UTF-8");

        // Verify
        String verified = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        System.out.println("First Swedish word: " + verified.substring(130, 145));
    }
}

