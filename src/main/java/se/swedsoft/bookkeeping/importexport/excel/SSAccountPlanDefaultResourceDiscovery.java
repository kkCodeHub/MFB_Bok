package se.swedsoft.bookkeeping.importexport.excel;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Discovers bundled default account-plan Excel resources.
 */
public final class SSAccountPlanDefaultResourceDiscovery {

    private static final String DEFAULT_DIR = "account/default";

    private SSAccountPlanDefaultResourceDiscovery() {
    }

    public static List<String> discoverDefaultExcelResources(ClassLoader classLoader) throws IOException {
        if (classLoader == null) {
            return Collections.emptyList();
        }

        Set<String> discovered = new LinkedHashSet<>();
        collectFromUrls(classLoader.getResources(DEFAULT_DIR), discovered);
        collectFromUrls(classLoader.getResources(DEFAULT_DIR + "/"), discovered);

        List<String> paths = new ArrayList<>(discovered);
        Collections.sort(paths);
        return paths;
    }

    private static void collectFromUrls(Enumeration<URL> urls, Set<String> sink) throws IOException {
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                collectFromFileUrl(url, sink);
            } else if ("jar".equals(protocol)) {
                collectFromJarUrl(url, sink);
            }
        }
    }

    private static void collectFromFileUrl(URL url, Set<String> sink) throws IOException {
        try {
            Path directory = Path.of(url.toURI());
            if (!Files.isDirectory(directory)) {
                return;
            }
            try (var stream = Files.list(directory)) {
                stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(SSAccountPlanDefaultResourceDiscovery::isExcelFileName)
                        .map(fileName -> DEFAULT_DIR + "/" + fileName)
                        .forEach(sink::add);
            }
        } catch (URISyntaxException e) {
            throw new IOException("Failed to parse default resource URL: " + url, e);
        }
    }

    private static void collectFromJarUrl(URL url, Set<String> sink) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        String prefix = connection.getEntryName();
        if (prefix == null) {
            prefix = DEFAULT_DIR;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix + "/")) {
                    continue;
                }

                String fileName = name.substring(name.lastIndexOf('/') + 1);
                if (fileName.isEmpty() || !isExcelFileName(fileName)) {
                    continue;
                }

                String relative = name.substring(prefix.length() + 1);
                if (relative.contains("/")) {
                    continue;
                }
                sink.add(prefix + "/" + relative);
            }
        }
    }

    private static boolean isExcelFileName(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }
}
