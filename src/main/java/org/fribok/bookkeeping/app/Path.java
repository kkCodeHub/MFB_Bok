/*
 * Copyright © 2010 Stefan Kangas <skangas@skangas.se>
 *
 * This file is part of Fribok.
 *
 * Fribok is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fribok is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Fribok.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fribok.bookkeeping.app;


import org.freedesktop.xdg.BaseDirs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;


/**
 * Return paths to directories depending on the running OS.  This class makes NO
 * GUARANTEES that these directories are neither readable nor writable by the
 * current user; this class should be regarded as nothing more than a set of
 * File objects available for use in different parts of the program.
 *
 * @author Stefan Kangas
 * @version $Id:$
 */
public enum Path {

    /** The application base directory */
    APP_BASE, /** The application data directory */ APP_DATA, /** The user configuration directory */ USER_CONF, /** The user data directory */ USER_DATA;

    private static final String APP_SUBDIR = "MFB_Bok";
    private static final Map<Path, File> path = new EnumMap<>(Path.class);

    static {
        File base = resolveAppBaseDirectory();
        File userRoot = resolveUserRootDirectory(base);

        path.put(APP_BASE, base);
        path.put(APP_DATA, new File(userRoot, "data"));
        path.put(USER_DATA, userRoot);
        path.put(USER_CONF, userRoot);
    }

    /**
     * Return path.
     *
     * @param name a path you want
     * @return the path
     */
    public static File get(Path name) {
        return path.get(name);
    }

    private static File resolveAppBaseDirectory() {
        try {
            File location = new File(Path.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile()) {
                File fromJavaCommand = resolveBaseFromJavaCommand(location.getAbsoluteFile());
                if (fromJavaCommand != null) {
                    return fromJavaCommand;
                }
                File parent = location.getParentFile();
                return parent == null ? location.getAbsoluteFile() : parent.getAbsoluteFile();
            } else if (location.isDirectory()) {
                File fromJavaCommand = resolveBaseFromJavaCommand(null);
                if (fromJavaCommand != null) {
                    return fromJavaCommand;
                }
                return resolveBaseFromDirectoryLocation(location);
            }
        } catch (URISyntaxException | SecurityException e) {
            // Fall back to current working directory.
        }
        File fromJavaCommand = resolveBaseFromJavaCommand(null);
        if (fromJavaCommand != null) {
            return fromJavaCommand;
        }
        return new File("").getAbsoluteFile();
    }

    private static File resolveBaseFromJavaCommand(File codeSourceJarFile) {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        String trimmed = command.trim();
        String firstToken;
        if (trimmed.charAt(0) == '"') {
            int closingQuote = trimmed.indexOf('"', 1);
            if (closingQuote <= 1) {
                return null;
            }
            firstToken = trimmed.substring(1, closingQuote);
        } else {
            int spaceIndex = trimmed.indexOf(' ');
            firstToken = spaceIndex < 0 ? trimmed : trimmed.substring(0, spaceIndex);
        }

        if (!firstToken.toLowerCase().endsWith(".jar")) {
            return null;
        }

        File jarFile = new File(firstToken);
        if (jarFile.isAbsolute()) {
            File parent = jarFile.getAbsoluteFile().getParentFile();
            return parent == null ? null : parent;
        }
        if (codeSourceJarFile != null && codeSourceJarFile.isFile()) {
            File parent = codeSourceJarFile.getParentFile();
            return parent == null ? null : parent.getAbsoluteFile();
        }

        File fromWorkingDir = new File(new File("").getAbsoluteFile(), firstToken);
        if (fromWorkingDir.isFile()) {
            File parent = fromWorkingDir.getParentFile();
            return parent == null ? null : parent.getAbsoluteFile();
        }
        return null;
    }

    private static File resolveBaseFromDirectoryLocation(File location) {
        File current = location.getAbsoluteFile();
        while (current != null) {
            if ("target".equalsIgnoreCase(current.getName())) {
                return current;
            }
            current = current.getParentFile();
        }
        if ("classes".equalsIgnoreCase(location.getName()) && location.getParentFile() != null) {
            return location.getParentFile().getAbsoluteFile();
        }
        return location.getAbsoluteFile();
    }

    private static File resolveUserRootDirectory(File appBase) {
        File preferred = new File(appBase, APP_SUBDIR);
        if (canUseDirectory(preferred)) {
            return preferred;
        }

        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            String appdata = System.getenv("LOCALAPPDATA");
            if (appdata == null || appdata.isEmpty()) {
                appdata = System.getenv("APPDATA");
            }
            if (appdata == null || appdata.isEmpty()) {
                appdata = System.getProperty("user.home");
            }
            File fallback = new File(appdata, APP_SUBDIR);
            if (canUseDirectory(fallback)) {
                return fallback;
            }
        } else if (os.startsWith("Mac OS")) {
            File fallback = new File(System.getProperty("user.home"),
                    "Library/Application Support/" + APP_SUBDIR);
            if (canUseDirectory(fallback)) {
                return fallback;
            }
        } else {
            BaseDirs baseDirs = new BaseDirs();
            File fallbackData = new File(baseDirs.getUserPath(BaseDirs.Resource.DATA), APP_SUBDIR);
            if (canUseDirectory(fallbackData)) {
                return fallbackData;
            }
        }
        return preferred.getAbsoluteFile();
    }

    private static boolean canUseDirectory(File directory) {
        try {
            if (directory.exists()) {
                return directory.isDirectory() && isWritableDirectory(directory);
            }
            File parent = directory.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            return directory.mkdirs() || directory.isDirectory();
        } catch (SecurityException e) {
            return false;
        }
    }

    private static boolean isWritableDirectory(File directory) {
        File probe = null;
        try {
            probe = File.createTempFile("mfb-bookkeeping", ".tmp", directory);
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        } finally {
            if (probe != null && probe.exists()) {
                probe.delete();
            }
        }
    }
}
