package se.swedsoft.bookkeeping.importexport.util;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SSTestFileHelper {

    private SSTestFileHelper() {}

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }

    public static void withTempFile(String prefix, String suffix, ThrowingConsumer<Path> action)
            throws Exception {
        Path file = Files.createTempFile(prefix, suffix);
        try {
            action.accept(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public static Document parseXml(Path file) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file.toFile());
    }
}

