package org.fribok.bookkeeping.app;


import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSInitDialog;


public final class SSDBUiInitializer {

    private SSDBUiInitializer() {}

    public static void init(boolean pShowDialog) {
        SSDB iDb = SSSystemConfigContext.getDatabase();
        if (iDb.getCurrentCompany() == null) {
            return;
        }

        if (pShowDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> iDb.init(false));
        } else {
            iDb.init(false);
        }
    }

    public static void initYear(boolean pShowDialog) {
        SSDB iDb = SSSystemConfigContext.getDatabase();
        if (iDb.getCurrentYear() == null) {
            return;
        }

        if (pShowDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> iDb.initYear(false));
        } else {
            iDb.initYear(false);
        }
    }

    /**
     * @deprecated use {@link #init(boolean)} to avoid exposing {@link SSDB} to callers.
     */
    @Deprecated
    public static void init(SSDB pDb, boolean pShowDialog) {
        if (pDb.getCurrentCompany() == null) {
            return;
        }

        if (pShowDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> pDb.init(false));
        } else {
            pDb.init(false);
        }
    }

    /**
     * @deprecated use {@link #initYear(boolean)} to avoid exposing {@link SSDB} to callers.
     */
    @Deprecated
    public static void initYear(SSDB pDb, boolean pShowDialog) {
        if (pDb.getCurrentYear() == null) {
            return;
        }

        if (pShowDialog && !java.awt.GraphicsEnvironment.isHeadless()) {
            SSInitDialog.runProgress(SSMainFrame.getInstance(), "Läser in data",
                    () -> pDb.initYear(false));
        } else {
            pDb.initYear(false);
        }
    }
}
