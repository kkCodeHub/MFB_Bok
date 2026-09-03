package se.swedsoft.bookkeeping.data.backup.util;


import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.backup.SSBackup;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.data.system.SSSystemCompany;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.frame.SSInternalFrame;
import se.swedsoft.bookkeeping.util.SSException;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static se.swedsoft.bookkeeping.data.backup.util.SSBackupZip.ArchiveFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-mar-03
 * Time: 11:14:09
 */
public class SSBackupFactory {    private static final Logger LOG = LoggerFactory.getLogger(SSBackupFactory.class);
    private static final String METADATA_ENTRY = "backup.properties";

    private SSBackupFactory() {}

    /**
     *
     * @return
     */
    public static String getDefaultFileName() {
        Date iDate = SSDateUtil.toDate(SSDateUtil.now());

        DateFormat iDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat iTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        String iFileName = "backup." + iDateFormat.format(iDate) + '.'
                + iTimeFormat.format(iDate) + ".zip";

        iFileName = iFileName.replace(":", "");
        iFileName = iFileName.replace("-", "");

        return iFileName;
    }

    /**
     *
     * @param iCompany
     * @return
     */
    public static String getDefaultFileName(SSNewCompany iCompany) {
        Date iDate = SSDateUtil.toDate(SSDateUtil.now());

        DateFormat iDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat iTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        String iFileName = "backup." + iCompany.getName() + '.'
                + iDateFormat.format(iDate) + '.' + iTimeFormat.format(iDate) + ".zip";

        iFileName = iFileName.replace(":", "");
        iFileName = iFileName.replace("-", "");

        return iFileName;
    }

    /**
     * Creates a full backup
     *
     * @param pFilename
     *
     * @return the backup
     */
    public static SSBackup createBackup(String pFilename) {
        SSBackup iBackup = new SSBackup(SSBackupType.FULL);
        File iBackupInfoFile = null;

        iBackup.setLocalDateTime(SSDateUtil.now());
        iBackup.setFilename(pFilename);

        // Get the database files
        List<ArchiveFile> iFiles = SSBackupUtils.getFiles();
        if (iFiles.isEmpty()) {
            LOG.error("Could not create backup: no database files found to archive");
            return null;
        }

        try {
            // Create a new temp file
            iBackupInfoFile = File.createTempFile("backup", null);

            SSBackup.storeBackup(iBackupInfoFile, iBackup);

            iFiles.add(new ArchiveFile(iBackupInfoFile, METADATA_ENTRY));
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
            return null;
        }

        LOG.info("Creating backup, adding files {");
        printFiles(iFiles);
        LOG.info("}");

        try {
            SSBackupZip.compressFiles(pFilename, iFiles);
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
            return null;
        } finally {
            if (iBackupInfoFile != null && iBackupInfoFile.exists()) {
                iBackupInfoFile.delete();
            }
        }

        return iBackup;
    }

    /**
     * Creates a backup for the supplied company
     *
     * @param pFilename
     * @param pCompany
     *
     * @return the backup
     */
    public static SSBackup createBackup(String pFilename, SSSystemCompany pCompany) {

        return null;
    }

    /**
     *
     * @param pFilename
     * @throws SSException
     */
    public static void restoreBackup(String pFilename) throws SSException {
        try {
            // Create a new temp file
            File iBackupFile = File.createTempFile("backup", null);

            // Read the backup file, if exists
            if (!SSBackupZip.extractFile(pFilename,
                    new ArchiveFile(iBackupFile, METADATA_ENTRY))) {
                throw new SSException(SSBundle.getBundle(),
                        "backupframe.importbackup.invalid");
            }

            // Load the backup
            SSBackup iBackup = SSBackup.loadBackup(iBackupFile);

            if (iBackup.getType() == SSBackupType.FULL) {
                restoreBackup(pFilename, iBackup);
            }

            iBackupFile.delete();

        } catch (IOException ex) {
            LOG.error("Unexpected error", ex);
        }

    }

    /**
     * Restores a full backup
     *
     * @param pFilename
     * @param iBackup
     * @throws IOException
     */
    private static void restoreBackup(String pFilename, SSBackup iBackup) throws IOException {
        SSInternalFrame.closeAllFrames();

        // Get the database directory
        String iDirectory = new File(Path.get(Path.USER_DATA), "db").getAbsolutePath() + File.separator;

        // Delete all old files
        SSSystemConfigContext.deleteDatabaseFiles();

        List<ArchiveFile> iFiles = SSBackupUtils.getFiles(pFilename, iDirectory);

        // Extract all files
        SSBackupZip.extractFiles(pFilename, iFiles);

        try {
            SSSystemConfigContext.loadLocalDatabase();
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
        }

    }

    /**
     * Restores a company backup
     *
     * @param pFilename
     * @param iBackup
     * @param iRestoredCompany
     */
    private static void restoreBackup(String pFilename, SSBackup iBackup, SSSystemCompany iRestoredCompany) {
        SSInternalFrame.closeAllFrames();

        // Get the database directory
        // String iDirectory = getDatabaseDirectory();

        // SSSystemCompany iCompany = findSystemCompany(iRestoredCompany);
        // Test if the company exists in the database
        /* if(iCompany != null ){
         iRestoredCompany.setCurrent( iCompany.isCurrent() );

         // Delete the company
         se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.deleteCompany(iCompany);
         } else {
         iRestoredCompany.setCurrent( false );
         } */

        // List<ArchiveFile> iFiles = SSBackupUtils.getFiles(pFilename, iDirectory);

        // Extract all files
        // SSBackupZip.extractFiles(pFilename, iFiles);

        // Add the company to the database
        // addSystemCompany(iRestoredCompany);

        if (iRestoredCompany.isCurrent()) {// se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.setCurrentCompany(iRestoredCompany);
        }
    }

    private static void printFiles(List<ArchiveFile> iFiles) {

        for (ArchiveFile iFile: iFiles) {
            LOG.info(" " + iFile);
        }

    }

}
