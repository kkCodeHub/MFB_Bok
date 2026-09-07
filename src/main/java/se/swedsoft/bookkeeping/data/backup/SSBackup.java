package se.swedsoft.bookkeeping.data.backup;


import se.swedsoft.bookkeeping.data.backup.util.SSBackupType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;


/**
 * Date: 2006-mar-03
 * Time: 09:03:45
 */
public class SSBackup  {

    static final long serialVersionUID = 1L;
    private static final String KEY_FILENAME = "filename";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_TYPE = "type";

    // The filename of the backup
    private String      iFilename;

    // The date of the backup
    private LocalDateTime iDate;

    // The type of the backup
    private SSBackupType iType;

    /**
     *
     * @param pType
     */
    public SSBackup(SSBackupType pType) {
        iType = pType;
    }

    /**
     *
     * @return the filename
     */
    public String getFilename() {
        return iFilename;
    }

    /**
     *
     * @param iFilename
     */
    public void setFilename(String iFilename) {
        this.iFilename = iFilename;
    }

    // ///////////////////////////////////////////////////////////////////

    public LocalDateTime getLocalDateTime() {
        return iDate;
    }

    public void setLocalDateTime(LocalDateTime iDate) {
        this.iDate = iDate;
    }

    // ///////////////////////////////////////////////////////////////////

    /**
     *
     * @return the type
     */
    public SSBackupType getType() {
        return iType;
    }

    /**
     *
     * @param iType
     */
    public void setType(SSBackupType iType) {
        this.iType = iType;
    }

    // ///////////////////////////////////////////////////////////////////

    // ///////////////////////////////////////////////////////////////////

    /**
     * Removes the backup from disk
     */
    public void delete() {
        File iFile = new File(iFilename);

        if (iFile.exists()) {
            iFile.delete();
        }
    }

    /**
     *
     * @return if the backup exists
     */
    public boolean exists() {
        File iFile = new File(iFilename);

        return iFile.exists();
    }

    public static SSBackup loadBackup(File iFile) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream iStream = new FileInputStream(iFile)) {
            properties.load(iStream);
        }

        String typeValue = properties.getProperty(KEY_TYPE);
        if (typeValue == null) {
            throw new IOException("Invalid backup metadata: missing type");
        }

        SSBackupType type;
        try {
            type = SSBackupType.valueOf(typeValue);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid backup metadata: unknown type " + typeValue, e);
        }

        SSBackup backup = new SSBackup(type);
        backup.setFilename(properties.getProperty(KEY_FILENAME));

        String createdAt = properties.getProperty(KEY_CREATED_AT);
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                backup.setLocalDateTime(LocalDateTime.parse(createdAt));
            } catch (RuntimeException e) {
                throw new IOException("Invalid backup metadata: invalid createdAt " + createdAt, e);
            }
        }

        return backup;
    }

    public static void storeBackup(File iFile, SSBackup iBackup) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_TYPE, iBackup.getType().name());
        if (iBackup.getFilename() != null) {
            properties.setProperty(KEY_FILENAME, iBackup.getFilename());
        }
        if (iBackup.getLocalDateTime() != null) {
            properties.setProperty(KEY_CREATED_AT, iBackup.getLocalDateTime().toString());
        }

        try (FileOutputStream iStream = new FileOutputStream(iFile)) {
            properties.store(iStream, "V2 backup metadata");
        }
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(iFilename);
        sb.append(", ");
        sb.append(iDate);
        sb.append(", ");
        sb.append(iType);

        return super.toString();
    }

}
