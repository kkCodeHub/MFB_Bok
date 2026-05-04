package se.swedsoft.bookkeeping.print.util;


import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.fribok.bookkeeping.app.Path;
import org.fribok.bookkeeping.app.Version;
import se.swedsoft.bookkeeping.util.SSException;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-14
 * Time: 17:01:15
 * @version $Id$
 */
public class SSReportCache {    private static final Logger LOG = LoggerFactory.getLogger(SSReportCache.class);

    private static final File REPORT_DIR = new File(Path.get(Path.APP_DATA), "report");
    private static final File COMPILED_DIR = new File(REPORT_DIR, "compiled");
    private static final String REPORT_RESOURCE = "/reports/report/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // The report cache with compiled report definitions.
    private Map<String, JasperReport> iReportCache;

    // our instance
    private static SSReportCache cInstance;

    /**
     * Get the instance of this class
     * @return The instance
     */
    public static SSReportCache getInstance() {
        if (cInstance == null) {
            cInstance = new SSReportCache();
        }
        return cInstance;
    }

    /**
     *
     */
    private SSReportCache() {
        iReportCache = new HashMap<>();
    }

    /**
     * This function will load a report, either from the runtime cache, a
     * precompiled version or from the report source.
     *
     * @param pReportName The name of the report to load, ie vatcontrol.jrxml.
     *
     * @return The JasperReport object
     * @throws SSException
     */
    public JasperReport getReport(String pReportName) throws SSException {
        // Try to get the report from cache
        JasperReport pReport = iReportCache.get(pReportName);

        if (pReport == null) {
            try {
                pReport = loadReport(pReportName);
            } catch (FileNotFoundException ex) {
                throw new SSException(ex.getLocalizedMessage());
            }
            iReportCache.put(pReportName, pReport);
        }
        return pReport;
    }

    /**
     *
     * @param pReportName
     * @return
     * @throws FileNotFoundException
     */
    private JasperReport loadReport(String pReportName) throws FileNotFoundException {
        File iReportFile = new File(REPORT_DIR, pReportName);
        File iCompiledFile = new File(COMPILED_DIR,
                pReportName.replace(".jrxml", ".jasperreport"));
	String iReportResource = REPORT_RESOURCE + pReportName;

        try {
            // If the report exists on disk, load it...
            if (iCompiledFile.exists()) {
		try {
		    LocalDateTime iReportDate = LocalDateTime.parse(Version.APP_BUILD, DATE_TIME_FORMATTER);
		    LocalDateTime iCompiledDate = LocalDateTime.ofInstant(
		            Instant.ofEpochMilli(iCompiledFile.lastModified()),
		            ZoneId.systemDefault());
		    // if the report file hasn't been changes since the last compile,
		    // load the compiled file, else fall through to the compile code
		    if (iReportDate.compareTo(iCompiledDate) <= 0) {
			LOG.info("Loading precompiled report {} from disk...", iCompiledFile);
			return loadCompiledReport(iCompiledFile);
		    }
		} catch (DateTimeParseException ex)  {
		    // ta bort den kompilerade versionen?
		    LOG.info(ex.getMessage());
		}
		
                LOG.info("Precompiled report exists, but report is changed ...");
            }

            // .. we need to recompile the report
	    LOG.info("Compiling and saving report {} to disk...", iReportResource);

	    InputStream is = getClass().getResourceAsStream(iReportResource);

	    JasperReport iReport = JasperCompileManager.compileReport(is);
            // Make the output directory
            iCompiledFile.getParentFile().mkdirs();

            return saveCompiledReport(iCompiledFile, iReport);
        } catch (JRException ex) {
            LOG.error("Unexpected error", ex);
        }
        return null;
    }

    /**
     *
     * @param pCompiledFile
     *
     * @return The report
     */
    private JasperReport loadCompiledReport(File pCompiledFile) {
        try {
            FileInputStream iFileInputStream = new FileInputStream(pCompiledFile);

            ObjectInputStream iObjectInputStream = new ObjectInputStream(
                    new BufferedInputStream(iFileInputStream));

            return (JasperReport) iObjectInputStream.readObject();

        } catch (IOException ex) {
            LOG.error("Unexpected error", ex);
        } catch (ClassNotFoundException ex) {
            LOG.error("Unexpected error", ex);
        }
        return null;
    }

    /**
     *
     * @param pCompiledFile
     * @param pReport
     *
     * @return The report
     */
    private JasperReport saveCompiledReport(File pCompiledFile, JasperReport pReport) {
        try {
            FileOutputStream iFileOutputStream = new FileOutputStream(pCompiledFile);

            ObjectOutputStream iObjectOutputStream = new ObjectOutputStream(
                    new BufferedOutputStream(iFileOutputStream));

            iObjectOutputStream.writeObject(pReport);
            iObjectOutputStream.flush();

            return pReport;
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.print.util.SSReportCache");
        sb.append("{iReportCache=").append(iReportCache);
        sb.append('}');
        return sb.toString();
    }
}
