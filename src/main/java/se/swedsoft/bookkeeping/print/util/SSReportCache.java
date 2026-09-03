package se.swedsoft.bookkeeping.print.util;


import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import se.swedsoft.bookkeeping.util.SSException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-14
 * Time: 17:01:15
 * @version $Id$
 */
public class SSReportCache {    private static final Logger LOG = LoggerFactory.getLogger(SSReportCache.class);

    private static final String REPORT_RESOURCE = "/reports/report/";

    static {
        registerBundledAwtFonts();
    }

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
	String iReportResource = REPORT_RESOURCE + pReportName;

        try {
            // Always compile from source; do not load or save precompiled reports
            // to disk to ensure reports are always up-to-date with the current
            // JasperReports version and avoid classpath/bytecode compatibility issues
            // across version upgrades.
            LOG.info("Compiling report {} from source...", iReportResource);

	    InputStream is = getClass().getResourceAsStream(iReportResource);

      return JasperCompileManager.compileReport(is);
        } catch (JRException ex) {
            LOG.error("Unexpected error", ex);
        }
        return null;
    }

    private static void registerBundledAwtFonts() {
        registerBundledAwtFont("/org/fribok/fonts/OCRA.ttf");
        registerBundledAwtFont("/org/fribok/fonts/OCRB.ttf");
    }

    private static void registerBundledAwtFont(String resourcePath) {
        try (InputStream stream = SSReportCache.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOG.warn("Bundled font resource not found: {}", resourcePath);
                return;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            LOG.info("Registered bundled AWT font: {}", font.getFontName());
        } catch (IOException | FontFormatException e) {
            LOG.warn("Failed to register bundled AWT font resource: {}", resourcePath, e);
        }
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
