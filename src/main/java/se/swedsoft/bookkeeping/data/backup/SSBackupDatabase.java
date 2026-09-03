package se.swedsoft.bookkeeping.data.backup;


import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.backup.util.SSBackupType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-mar-03
 * Time: 09:39:32
 */
public class SSBackupDatabase {    private static final Logger LOG = LoggerFactory.getLogger(SSBackupDatabase.class);
    private static final String KEY_COUNT = "count";
    private static final String KEY_FILENAME = "filename";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_TYPE = "type";

    private static final File iFile = new File(Path.get(Path.USER_DATA), "backup-history.properties");

    private static SSBackupDatabase cInstance;

    /**
     *
     * @return
     */
    public static SSBackupDatabase getInstance() {
        if (cInstance == null) {
            cInstance = new SSBackupDatabase();
        }
        return cInstance;
    }

    private List<SSBackup> iBackups;

    /**
     *
     */
    private SSBackupDatabase() {
        if (iFile.exists()) {
            loadDatabase();
        } else {
            newDatabase();
        }
    }

    /**
     * Notify that the database has been changed and need to be stored to file
     */
    public void notifyUpdated() {
        storeDatabase();
    }

    // ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new backupdatabase
     */
    private void newDatabase() {
        iBackups = new LinkedList<>();
    }

    /**
     * Loads the backupdatabase
     *
     */
    private void loadDatabase() {
        iBackups = new LinkedList<>();
        Properties properties = new Properties();
        try (FileInputStream iStream = new FileInputStream(iFile)) {
            properties.load(iStream);
            int count = Integer.parseInt(properties.getProperty(KEY_COUNT, "0"));
            for (int i = 0; i < count; i++) {
                String keyPrefix = "backup." + i + '.';
                String typeValue = properties.getProperty(keyPrefix + KEY_TYPE);
                if (typeValue == null) {
                    continue;
                }

                SSBackupType type = SSBackupType.valueOf(typeValue);
                SSBackup backup = new SSBackup(type);
                backup.setFilename(properties.getProperty(keyPrefix + KEY_FILENAME));

                String createdAt = properties.getProperty(keyPrefix + KEY_CREATED_AT);
                if (createdAt != null && !createdAt.isEmpty()) {
                    backup.setLocalDateTime(LocalDateTime.parse(createdAt));
                }

                iBackups.add(backup);
            }
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            newDatabase();
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
            newDatabase();
        }
    }

    /**
     * Store the backupdatabase
     *
     */
    private void storeDatabase() {
        Properties properties = new Properties();
        properties.setProperty(KEY_COUNT, Integer.toString(iBackups.size()));
        for (int i = 0; i < iBackups.size(); i++) {
            SSBackup backup = iBackups.get(i);
            String keyPrefix = "backup." + i + '.';
            properties.setProperty(keyPrefix + KEY_TYPE, backup.getType().name());
            if (backup.getFilename() != null) {
                properties.setProperty(keyPrefix + KEY_FILENAME, backup.getFilename());
            }
            if (backup.getLocalDateTime() != null) {
                properties.setProperty(keyPrefix + KEY_CREATED_AT, backup.getLocalDateTime().toString());
            }
        }

        try (FileOutputStream iStream = new FileOutputStream(iFile)) {
            properties.store(iStream, "V2 backup history");
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of all known backups
     *
     * @return list of backups
     */
    public List<SSBackup> getBackups() {
        if (iBackups == null) {
            throw new RuntimeException("Backupdatabase not loaded");
        }
        return iBackups;
    }

    public void add(SSBackup backup) {
        iBackups.add(backup);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.backup.SSBackupDatabase");
        sb.append("{iBackups=").append(iBackups);
        sb.append('}');
        return sb.toString();
    }
}
