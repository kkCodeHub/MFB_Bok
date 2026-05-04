package org.fribok.bookkeeping.app;

import java.util.ResourceBundle;

/**
 * Class holding constants with version information.
 */
public final class Version {
    public final static String APP_TITLE = "Fribok";

    public final static String APP_BUILD = buildDateTime();
    public final static String APP_VERSION = version();

    public static String buildDateTime() {
	return ResourceBundle.getBundle("version").getString("build.date");
    }

    public static String version() {
	ResourceBundle bundle = ResourceBundle.getBundle("version");
	String ver = bundle.getString("version");
	String hash = bundle.getString("git.hash");
	if (hash != null && !hash.isEmpty() && !hash.startsWith("${")) {
	    return ver + " (" + hash + ")";
	}
	return ver;
    }

    public final static boolean CAN_DELETE_VOUCHERS = false;

    private Version() {} // prevents instantiation
}
