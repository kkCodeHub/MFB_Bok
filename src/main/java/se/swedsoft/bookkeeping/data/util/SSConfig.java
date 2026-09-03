package se.swedsoft.bookkeeping.data.util;


import org.fribok.bookkeeping.app.Path;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
    private static final String TYPE_STRING = "string";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_DOUBLE = "double";
    private static final String TYPE_POINT = "point";
    private static final String TYPE_DIMENSION = "dimension";

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
        try (FileInputStream iInputStream = new FileInputStream(CONFIG_FILE)) {
            Properties properties = new Properties();
            properties.load(iInputStream);

            SSConfig config = new SSConfig();
            for (String key : properties.stringPropertyNames()) {
                config.iSettings.put(key, decodeValue(properties.getProperty(key)));
            }
            cInstance = config;
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
            newConfig();
        }
    }

    /**
     * Store the database
     *
     */
    private static synchronized void storeConfig() {
        try (FileOutputStream iOutputStream = new FileOutputStream(CONFIG_FILE)) {
            Properties properties = new Properties();
            for (Map.Entry<String, Object> entry : cInstance.iSettings.entrySet()) {
                properties.setProperty(entry.getKey(), encodeValue(entry.getValue()));
            }
            properties.store(iOutputStream, "Bookkeeping configuration (V2 explicit format)");
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private static String encodeValue(Object value) {
        if (value == null) {
            return TYPE_STRING + ":";
        }
        if (value instanceof Boolean) {
            return TYPE_BOOLEAN + ":" + value;
        }
        if (value instanceof Integer) {
            return TYPE_INTEGER + ":" + value;
        }
        if (value instanceof Long) {
            return TYPE_LONG + ":" + value;
        }
        if (value instanceof Double) {
            return TYPE_DOUBLE + ":" + value;
        }
        if (value instanceof Point) {
            Point point = (Point) value;
            return TYPE_POINT + ":" + point.x + "," + point.y;
        }
        if (value instanceof Dimension) {
            Dimension dimension = (Dimension) value;
            return TYPE_DIMENSION + ":" + dimension.width + "," + dimension.height;
        }
        return TYPE_STRING + ":" + value;
    }

    private static Object decodeValue(String value) {
        if (value == null) {
            return null;
        }
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0) {
            return value;
        }

        String type = value.substring(0, separatorIndex);
        String payload = value.substring(separatorIndex + 1);

        try {
            switch (type) {
                case TYPE_BOOLEAN:
                    return Boolean.valueOf(payload);
                case TYPE_INTEGER:
                    return Integer.valueOf(payload);
                case TYPE_LONG:
                    return Long.valueOf(payload);
                case TYPE_DOUBLE:
                    return Double.valueOf(payload);
                case TYPE_POINT: {
                    String[] parts = payload.split(",", -1);
                    return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
                case TYPE_DIMENSION: {
                    String[] parts = payload.split(",", -1);
                    return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
                case TYPE_STRING:
                    return payload;
                default:
                    return value;
            }
        } catch (RuntimeException e) {
            return payload;
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
