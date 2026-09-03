package se.swedsoft.bookkeeping.data.backup.util;


import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.system.SSSystemCompany;
import se.swedsoft.bookkeeping.data.system.SSSystemYear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static se.swedsoft.bookkeeping.data.backup.util.SSBackupZip.ArchiveFile;


/**
 * Date: 2006-mar-03
 * Time: 13:00:54
 */
public class SSBackupUtils {
    private SSBackupUtils() {}

    /**
     *
     * @return
     */
    public static List<ArchiveFile> getFiles() {

        List<ArchiveFile> iFiles = new LinkedList<>();

        // Add the database files
        File dbDir = new File(Path.get(Path.USER_DATA), "db");
        addIfExists(iFiles, new File(dbDir, "JFSDB.properties"));
        addIfExists(iFiles, new File(dbDir, "JFSDB.script"));
        addIfExists(iFiles, new File(dbDir, "JFSDB.data"));
        addIfExists(iFiles, new File(dbDir, "JFSDB.backup"));
        addIfExists(iFiles, new File(dbDir, "JFSDB.log"));
        addIfExists(iFiles, new File(dbDir, "JFSDB.lobs"));

        return iFiles;
    }

    private static void addIfExists(List<ArchiveFile> iFiles, File iFile) {
        if (iFile.exists()) {
            iFiles.add(new ArchiveFile(iFile));
        }
    }

    /**
     *
     * @param pCompany
     * @return
     */
    public static List<ArchiveFile> getFiles(SSSystemCompany pCompany) {
        List<ArchiveFile> iFiles = new LinkedList<>();

        // Add the company
        // iFiles.add(new ArchiveFile(getCompanyBackupFile(pCompany.getId())));

        // Loop through all years
        for (SSSystemYear iYear: pCompany.getYears()) {// Add the year
            // iFiles.add(new ArchiveFile(getYearBackupFile(iYear.getId())));
        }

        return iFiles;
    }

    /**
     *
     * @param pFilename
     * @param iDirectory
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<ArchiveFile> getFiles(String pFilename, String iDirectory) throws FileNotFoundException, IOException {

        List<ArchiveFile> iFiles = new LinkedList<>();

        // Get the names of the files in the zip file
        for (String iName: SSBackupZip.getFiles(pFilename)) {

            // Don't extract the backup metadata file
            if (iName.equals("backup.properties")) {
                continue;
            }

            File iFile = new File(iDirectory + iName);

            iFiles.add(new ArchiveFile(iFile, iName));
        }
        return iFiles;
    }
}
