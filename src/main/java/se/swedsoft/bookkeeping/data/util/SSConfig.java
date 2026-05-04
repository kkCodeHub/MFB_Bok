package se.swedsoft.bookkeeping.data.util;


import org.fribok.bookkeeping.app.Path;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-mar-13
 * Time: 09:11:46
 * @version $Id$
 */
public class SSConfig implements Serializable {    private static final Logger LOG = LoggerFactory.getLogger(SSConfig.class);


    // / Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    // The config instance
    private static SSConfig cInstance;

    /**
     * Get the current instance
     * @return
     */
    public static SSConfig getInstance() {
        if (cInstance == null) {
            if (CONFIG_FILE.exists() && CONFIG_FILE.length() != 0) {
                loadConfig();
            } else {
                newConfig();
            }
        }
        return cInstance;
    }

    // The settings file
    private static File CONFIG_FILE = new File(Path.get(Path.USER_CONF),
            "bookkeeping.config");

    private Map<String, Object> iSettings;

    /**
     * Constructor
     */
    private SSConfig() {
        iSettings = new HashMap<>();
    }

    // ////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param pProperty
     * @return
     */
    public synchronized Object get(String pProperty) {
        return iSettings.get(pProperty);
    }

    /**
     *
     * @param pProperty
     * @param pDefault
     * @return
     */
    public synchronized Object get(String pProperty, Object pDefault) {
        Object iValue = iSettings.get(pProperty);

        return iValue != null ? iValue : pDefault;
    }

    /**
     *
     * @param pProperty
     * @param pValue
     */
    public synchronized void set(String pProperty, Object pValue) {
        iSettings.put(pProperty, pValue);
        storeConfig();
    }

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new database
     */
    private static synchronized void newConfig() {
        cInstance = new SSConfig();
    }

    /**
     * Loads the database
     *
     */
    private static synchronized void loadConfig() {
        try (ObjectInputStream iObjectInputStream = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(CONFIG_FILE)))) {
            cInstance = (SSConfig) iObjectInputStream.readObject();
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        } catch (ClassNotFoundException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /**
     * Store the database
     *
     */
    private static synchronized void storeConfig() {
        try (ObjectOutputStream iObjectOutputStream = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(CONFIG_FILE)))) {
            iObjectOutputStream.writeObject(cInstance);
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.util.SSConfig");
        sb.append("{iSettings=").append(iSettings);
        sb.append('}');
        return sb.toString();
    }
}
