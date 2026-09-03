package org.fribok.bookkeeping;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.FontPolicy;
import com.jgoodies.looks.FontPolicies;
import com.jgoodies.looks.FontSet;
import com.jgoodies.looks.FontSets;

import org.fribok.bookkeeping.api.CompanyApiServer;
import org.fribok.bookkeeping.app.Path;
import org.fribok.bookkeeping.app.SSDBUiInitializer;
import org.fribok.bookkeeping.app.Version;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.data.util.SSConfig;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.gui.util.graphics.SSIcon;

import javax.swing.*;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @version $Id$
 */
public class Bookkeeping {    private static final Logger LOG = LoggerFactory.getLogger(Bookkeeping.class);


    public static boolean iRunning;

    private Bookkeeping() {}

    /**
     *
     */
    private static boolean loadHsqldbDriver() {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            return true;
        } catch (ClassNotFoundException e) {
            LOG.info("ERROR: failed to load HSQLDB JDBC driver.");
            LOG.error("Unexpected error", e);
            return false;
        }
    }

    private static boolean startupDatabase() {
        LOG.info("KK StartupDatabase, Bookkeeping.java 57");
        if (!loadHsqldbDriver()) {
            return false;
        }

        try {
            File dbDir = new File(Path.get(Path.USER_DATA), "db");
            Connection iConnection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + dbDir.getAbsolutePath() + File.separator + "JFSDB", "sa", "");

            SSSystemConfigContext.startupLocal(iConnection);
            return true;

        } catch (SQLException e) {
            LOG.error("Failed to start local database", e);
            return false;
        }
    }

    /**
     * The main method of the program.
     *
     * @param args The arguments to the program.
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--version")) {
            LOG.info(Version.APP_TITLE + " " + Version.APP_VERSION);
            return;
        }

        try {
	    String os = System.getProperty("os.name");
	    FontSet fontSet = null;
	    if (os.startsWith("Windows")) {
		fontSet = FontSets.createDefaultFontSet(new Font(
				   "arial unicode MS", Font.PLAIN, 13));
	    } else {
		fontSet = FontSets.createDefaultFontSet(new Font(
				   "arial unicode", Font.PLAIN, 13));
	    }
	    FontPolicy fixedPolicy = FontPolicies.createFixedPolicy(fontSet);
	    Plastic3DLookAndFeel.setFontPolicy(fixedPolicy);
	    //Plastic3DLookAndFeel.setHighContrastFocusColorsEnabled(true);
	    String lnfClassName = Plastic3DLookAndFeel.class.getName();
	    if (os.startsWith("Mac OS") || os.startsWith("Windows")) {
		lnfClassName = UIManager.getSystemLookAndFeelClassName();
	    } else {
		String xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP");
		if ("Unity".equalsIgnoreCase(xdgCurrentDesktop)
				|| "XFCE".equalsIgnoreCase(xdgCurrentDesktop)
				|| "GNOME".equalsIgnoreCase(xdgCurrentDesktop)
				|| "X-Cinnamon".equalsIgnoreCase(xdgCurrentDesktop)
				|| "LXDE".equalsIgnoreCase(xdgCurrentDesktop)
				) {
			//lnfClassName = UIManager.getSystemLookAndFeelClassName();
			//lnfClassName = PlasticLookAndFeel.class.getName();
		} else {
			lnfClassName = Plastic3DLookAndFeel.class.getName();
		}
	    }

            UIManager.setLookAndFeel(lnfClassName);
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Unexpected error", e);
        }
        iRunning = true;

        // Print information to ease debugging
        LOG.info("Starting up...");
        LOG.info("Title : " + Version.APP_TITLE);
        LOG.info("Version : " + Version.APP_VERSION);
        LOG.info("Build : " + Version.APP_BUILD);
        LOG.info("Directory : " + Path.get(Path.APP_BASE));
        LOG.info("");
        LOG.info("Operating system: " + System.getProperty("os.name"));
        LOG.info("Architecture : " + System.getProperty("os.arch"));
        LOG.info("Java version : " + System.getProperty("java.version"));
        LOG.info("");
        LOG.info("Paths:");
        for (Path name : Path.values()) {
            LOG.info(String.format("   %-12s = %s", name, Path.get(name)));
        }

        String warning = null;

        // Create paths as needed, warning the user on failure
        for (Path name : Path.values()) {
            File dir = Path.get(name);

            if (!dir.exists()) {
                try {
                    if (dir.mkdirs()) {
                        LOG.info("Created " + dir);
                    } else {
                        warning = "unable to create";
                    }
                } catch (SecurityException e) {
                    LOG.error("Unexpected error", e);
                }
            } else if (!dir.isDirectory()) {
                warning = "exists but is not a directory";
            }
            if (warning != null) {
                LOG.info(" !! WARNING: " + dir + ' ' + warning);
                warning = null;
            }
        }

        // Create and display the main iMainFrame.
        SSMainFrame iMainFrame = SSMainFrame.getInstance();

        UIManager.put("InternalFrame.icon", SSIcon.getIcon("ICON_FRAME"));
        UIManager.put("InternalFrame.inactiveIcon", SSIcon.getIcon("ICON_FRAME"));
        if (!startupDatabase()) {
            LOG.error("Startup aborted because the local database could not be initialized.");
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Kunde inte starta den lokala databasen. Kontrollera loggen och datakatalogen.",
                        "Databasfel",
                        JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        CompanyApiServer apiServer;
        try {
            apiServer = CompanyApiServer.start(SSSystemConfigContext.getDatabase().getConnection());
        } catch (IOException | RuntimeException e) {
            LOG.error("Startup aborted because the API server could not be initialized.", e);
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        null,
                        "Kunde inte starta API-servern. Kontrollera loggen och portinställningarna.",
                        "API-fel",
                        JOptionPane.ERROR_MESSAGE);
            }
            SSSystemConfigContext.shutdown();
            return;
        }

        // Display the main frame.
        iMainFrame.setVisible(true);

        // Only display the company iMainFrame if there are no companies defined.
        // I would prefer to only open the select company iMainFrame if there are no companies.
        // But Fredrik and Joakim wants it to displayed every time.
        if ((Boolean) SSConfig.getInstance().get("companyframe.showatstart", true)
                || SSCompanyYearContext.getCurrentCompany() == null) {
            iMainFrame.showCompanyFrame();
        }

        SSDBUiInitializer.init(true);

        CompanyApiServer finalApiServer = apiServer;
        // Perhaps add some type of shut down hook.
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {

                                SSFrameManager.getInstance().storeAllFrames();

                                iRunning = false;
                                finalApiServer.stop();
                                SSSystemConfigContext.shutdown();

                            }));
    }

}
