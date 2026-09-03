package se.swedsoft.bookkeeping.testsupport.system;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Shared schema-V2 database fixture helpers for integration and repository tests.
 */
public final class SSV2DatabaseFixture {

    private SSV2DatabaseFixture() {}

    public static Connection openDatabase(String pJdbcUrl) throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        Connection iConnection = DriverManager.getConnection(pJdbcUrl, "sa", "");
        SSSystemConfigContext.startupLocal(iConnection);
        return iConnection;
    }

    public static void closeDatabase(Connection pConnection) throws Exception {
        try {
            if (pConnection != null && !pConnection.isClosed()) {
                pConnection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    public static void clearState() {
        SSEventTriggerSyncContext.clearCachedLists();
    }

    public static Integer createCompany(Connection pConnection, String pName) throws SQLException {
        return CompanySchemaFixture.createCompany(pConnection, pName);
    }

    public static SSNewCompany setCurrentCompany(Integer pCompanyId, String pName) {
        SSNewCompany iCompany = new SSNewCompany();
        iCompany.setId(pCompanyId);
        iCompany.setName(pName);
        SSCompanyYearContext.setCurrentCompany(iCompany);
        return iCompany;
    }

    public static SSNewAccountingYear createAndSetCurrentYear(LocalDate pFrom, LocalDate pTo) {
        SSNewAccountingYear iYear = new SSNewAccountingYear();
        iYear.setLocalFrom(pFrom);
        iYear.setLocalTo(pTo);
        SSAccountingContext.addAccountingYear(iYear);
        SSCompanyYearContext.setCurrentYear(iYear);
        return iYear;
    }
}
