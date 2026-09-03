package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSOwnReportContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.data.SSOwnReportRow;
import se.swedsoft.bookkeeping.data.common.SSHeadingType;
import se.swedsoft.bookkeeping.gui.ownreport.util.SSOwnReportAccountRow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration slice for own-report CRUD against schema V2.
 */
@Tag("integration")
class SSOwnReportV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-o-w-n-r-e-p-o-r-t-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 OwnReport Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 OwnReport Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchOwnReportWithRowsInSchemaV2() {
        SSOwnReport ownReport = ownReport("OR-001", "PRJ-1", "RES-1");
        ownReport.getHeadings().add(heading(SSHeadingType.HEADING1, "Heading 1", 1910, 3010));

        SSOwnReportContext.addOwnReport(ownReport);
        assertThat(ownReport.getId()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOwnReport> fetched = SSOwnReportContext.getOwnReport(ownReport);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("OR-001");
        assertThat(fetched.get().getProjectNr()).isEqualTo("PRJ-1");
        assertThat(fetched.get().getResultUnitNr()).isEqualTo("RES-1");
        assertThat(fetched.get().getHeadings()).hasSize(1);
        assertThat(fetched.get().getHeadings().get(0).getHeading()).isEqualTo("Heading 1");
        assertThat(fetched.get().getHeadings().get(0).getType()).isEqualTo(SSHeadingType.HEADING1);
        assertThat(fetched.get().getHeadings().get(0).getAccountRows()).hasSize(2);
        assertThat(fetched.get().getHeadings().get(0).getAccountRows().get(0).getAccount().getNumber()).isEqualTo(1910);

        SSOwnReportContext.deleteOwnReport(fetched.get());
    }

    @Test
    void updateAndDeleteOwnReportInSchemaV2() {
        SSOwnReport ownReport = ownReport("OR-002", "PRJ-2", "RES-2");
        ownReport.getHeadings().add(heading(SSHeadingType.HEADING1, "Before heading", 2610));
        SSOwnReportContext.addOwnReport(ownReport);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOwnReport> fetched = SSOwnReportContext.getOwnReport(ownReport);
        assertThat(fetched).isPresent();

        SSOwnReport updated = fetched.get();
        updated.setName("OR-UPDATED");
        updated.setProjectNr("PRJ-UPDATED");
        updated.getHeadings().clear();
        updated.getHeadings().add(heading(SSHeadingType.HEADING3, "After heading", 4010));
        SSOwnReportContext.updateOwnReport(updated);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOwnReport> reloaded = SSOwnReportContext.getOwnReport(updated);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("OR-UPDATED");
        assertThat(reloaded.get().getProjectNr()).isEqualTo("PRJ-UPDATED");
        assertThat(reloaded.get().getHeadings()).hasSize(1);
        assertThat(reloaded.get().getHeadings().get(0).getHeading()).isEqualTo("After heading");
        assertThat(reloaded.get().getHeadings().get(0).getType()).isEqualTo(SSHeadingType.HEADING3);
        assertThat(reloaded.get().getHeadings().get(0).getAccountRows()).hasSize(1);
        assertThat(reloaded.get().getHeadings().get(0).getAccountRows().get(0).getAccount().getNumber()).isEqualTo(4010);

        Integer ownReportId = reloaded.get().getId();
        SSOwnReportContext.deleteOwnReport(reloaded.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSOwnReport> all = SSOwnReportContext.getOwnReports();
        assertThat(all).extracting(SSOwnReport::getId).doesNotContain(ownReportId);
    }

    private static SSOwnReport ownReport(String name, String projectNr, String resultUnitNr) {
        SSOwnReport ownReport = new SSOwnReport();
        ownReport.setName(name);
        ownReport.setProjectNr(projectNr);
        ownReport.setResultUnitNr(resultUnitNr);
        return ownReport;
    }

    private static SSOwnReportRow heading(SSHeadingType type, String heading, int... accountNumbers) {
        SSOwnReportRow row = new SSOwnReportRow();
        row.setType(type);
        row.setHeading(heading);
        for (int accountNumber : accountNumbers) {
            SSOwnReportAccountRow accountRow = new SSOwnReportAccountRow();
            accountRow.setAccount(new SSAccount(accountNumber));
            row.getAccountRows().add(accountRow);
        }
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


