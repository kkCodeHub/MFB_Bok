/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.gui.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


/**
 * The resourcebundle for this application
 */
public class SSBundle extends ResourceBundle {

    /** */
    private static SSBundle cBundle;

    static {
        try {
            // Load book.properties explicitly with UTF-8 encoding to support Swedish characters
            InputStream in = SSBundle.class.getResourceAsStream("/book.properties");
            if (in != null) {
                cBundle = new SSBundle(new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8)));
            } else {
                // Fallback to default ResourceBundle if resource not found
                cBundle = new SSBundle(ResourceBundle.getBundle("book"));
            }
        } catch (IOException e) {
            // Fallback to default ResourceBundle on error
            cBundle = new SSBundle(ResourceBundle.getBundle("book"));
        }
    }

    /** */
    private ResourceBundle iBundle;

    /**
     * Default constructor.
     * @param bundle the bundle to use and to protect us from
     */
    public SSBundle(ResourceBundle bundle) {
        iBundle = bundle;
    }

    /**
     * Returns the bundle to use.
     * @return a "Safe" resource bundle
     */
    public static SSBundle getBundle() {
        return cBundle;
    }

    @Override
    public Enumeration<String> getKeys() {
        return iBundle.getKeys();
    }

    /**
     *
     * @param key
     * @return true if the key exists in the bundle
     */
    public boolean hasKey(String key) {
        try {
            iBundle.getObject(key);
        } catch (MissingResourceException e) {
            return false;
        }
        return true;
    }

    @Override
    protected Object handleGetObject(String key) {

        Object o = null;

        try {
            o = iBundle.getObject(key);
        } catch (MissingResourceException e) {
            o = "???" + key + "???";
        }

        return o;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.gui.util.SSBundle");
        sb.append("{iBundle=").append(iBundle);
        sb.append('}');
        return sb.toString();
    }
}
