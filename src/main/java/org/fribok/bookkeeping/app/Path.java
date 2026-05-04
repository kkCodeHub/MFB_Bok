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

    private static final String APP_SUBDIR = "fribok";
    private static final Map<Path, File> path = new EnumMap<>(Path.class);

    static {
        File base = new File(new File("").getAbsolutePath());

        path.put(APP_BASE, base);
        path.put(APP_DATA, new File(base, "data"));

        String os = System.getProperty("os.name");

        if (os.startsWith("Windows")) {
            // Use %LOCALAPPDATA%\fribok so data is stored in a writable,
            // per-user location regardless of the working directory at launch
            // (e.g. when started via an installer shortcut from C:\Windows\System32).
            String appdata = System.getenv("LOCALAPPDATA");
            if (appdata == null || appdata.isEmpty()) {
                // Fallback: %APPDATA% (roaming), then home dir
                appdata = System.getenv("APPDATA");
            }
            if (appdata == null || appdata.isEmpty()) {
                appdata = System.getProperty("user.home");
            }
            File winDataDir = new File(appdata, APP_SUBDIR);
            path.put(USER_DATA, winDataDir);
            path.put(USER_CONF, winDataDir);
        } else if (os.startsWith("Mac OS")) {
            // Use ~/Library/Application Support/fribok on macOS
            String home = System.getProperty("user.home");
            File macDataDir = new File(home, "Library/Application Support/" + APP_SUBDIR);
            path.put(USER_DATA, macDataDir);
            path.put(USER_CONF, macDataDir);
        } else { // assume freedesktop.org compliance
            BaseDirs iBaseDirs = new BaseDirs();
            String userData = iBaseDirs.getUserPath(BaseDirs.Resource.DATA);
            String userConf = iBaseDirs.getUserPath(BaseDirs.Resource.CONFIG);

            path.put(USER_DATA, new File(userData, APP_SUBDIR));
            path.put(USER_CONF, new File(userConf, APP_SUBDIR));
        }
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
}
