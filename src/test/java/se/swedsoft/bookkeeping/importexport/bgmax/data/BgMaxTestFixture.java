package se.swedsoft.bookkeeping.importexport.bgmax.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared BGMax fixture loader for sample files in test resources.
 */
final class BgMaxTestFixture {
    private static final int MIN_SAMPLE_FILE = 1;
    private static final int MAX_SAMPLE_FILE = 4;
    private static final String RESOURCE_PREFIX = "/se/swedsoft/bookkeeping/importexport/bgmax/data/BgMaxTestFile";
    private static final String RESOURCE_SUFFIX = ".ut";

    private BgMaxTestFixture() {}

    static List<String> readSampleFile(int pFileNumber) throws IOException {
        if (pFileNumber < MIN_SAMPLE_FILE || pFileNumber > MAX_SAMPLE_FILE) {
            throw new IllegalArgumentException("Unsupported BGMax sample file number: " + pFileNumber);
        }

        String iResourcePath = RESOURCE_PREFIX + pFileNumber + RESOURCE_SUFFIX;
        InputStream iStream = BgMaxTestFixture.class.getResourceAsStream(iResourcePath);
        if (iStream == null) {
            throw new IOException("Missing BGMax fixture resource: " + iResourcePath);
        }

        try (BufferedReader iReader = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8))) {
            List<String> iLines = new ArrayList<>();
            String iLine;
            while ((iLine = iReader.readLine()) != null) {
                iLines.add(iLine);
            }
            return iLines;
        }
    }

    static List<List<String>> readAllSampleFiles() throws IOException {
        List<List<String>> iFiles = new ArrayList<>();
        for (int i = MIN_SAMPLE_FILE; i <= MAX_SAMPLE_FILE; i++) {
            iFiles.add(readSampleFile(i));
        }
        return iFiles;
    }
}
