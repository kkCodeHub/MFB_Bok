package se.swedsoft.bookkeeping.data.system;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * System/Config domain facade for database lifecycle and bootstrap operations.
 */
public final class SSSystemConfigContext {

    private static final SSDB DB = SSDB.getInstance();

    private SSSystemConfigContext() {}

    public static void startupLocal(Connection pConnection) throws SQLException {
        DB.startupLocal(pConnection);
        SSCompanyYearContext.initializeCurrentCompanyAndYear();
    }

    public static void shutdown() {
        DB.shutdown();
    }

    public static void shutdownCompact() {
        DB.shutdownCompact();
    }

    public static void loadLocalDatabase() {
        DB.loadLocalDatabase();
        SSCompanyYearContext.initializeCurrentCompanyAndYear();
    }

    public static void deleteDatabaseFiles() {
        DB.deleteDatabaseFiles();
    }

    public static SSDB getDatabase() {
        return DB;
    }
}
