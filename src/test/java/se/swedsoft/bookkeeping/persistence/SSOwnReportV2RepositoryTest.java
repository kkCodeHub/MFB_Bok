package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOwnReport;
import se.swedsoft.bookkeeping.data.SSOwnReportRow;
import se.swedsoft.bookkeeping.data.common.SSHeadingType;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.ownreport.util.SSOwnReportAccountRow;
import se.swedsoft.bookkeeping.persistence.v2.V2OwnReportRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for own-report repository wiring in schema V2.
 */
@Tag("integration")
class SSOwnReportV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_ownreport_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 OwnReport Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 OwnReport Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
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
    void clearCaches() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2OwnReportRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.ownReports()).isInstanceOf(V2OwnReportRepository.class);
    }

    @Test
    void addFindSubsetUpdateDeleteOwnReportViaRepository() {
        SSOwnReport ownReport = ownReport("OR-REPO-001", "PRJ-1", "RES-1");
        ownReport.getHeadings().add(heading(SSHeadingType.HEADING1, "Repo heading", 1910, 3010));

        Repositories.ownReports().add(ownReport);
        assertThat(ownReport.getId()).isGreaterThanOrEqualTo(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOwnReport> fetched = Repositories.ownReports().findByOwnReport(ownReport);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("OR-REPO-001");
        assertThat(fetched.get().getProjectNr()).isEqualTo("PRJ-1");
        assertThat(fetched.get().getResultUnitNr()).isEqualTo("RES-1");
        assertThat(fetched.get().getHeadings()).hasSize(1);
        assertThat(fetched.get().getHeadings().get(0).getType()).isEqualTo(SSHeadingType.HEADING1);
        assertThat(fetched.get().getHeadings().get(0).getHeading()).isEqualTo("Repo heading");

        SSOwnReport probe = new SSOwnReport();
        probe.setId(fetched.get().getId());
        List<SSOwnReport> subset = Repositories.ownReports().findAll(Collections.singletonList(probe));
        assertThat(subset).hasSize(1);

        SSOwnReport updated = fetched.get();
        updated.setName("OR-REPO-UPDATED");
        updated.getHeadings().clear();
        updated.getHeadings().add(heading(SSHeadingType.HEADING3, "Repo heading updated", 4010));
        Repositories.ownReports().update(updated);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOwnReport> reloaded = Repositories.ownReports().findByOwnReport(updated);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("OR-REPO-UPDATED");
        assertThat(reloaded.get().getProjectNr()).isEqualTo("PRJ-1");
        assertThat(reloaded.get().getResultUnitNr()).isEqualTo("RES-1");
        assertThat(reloaded.get().getHeadings()).hasSize(1);
        assertThat(reloaded.get().getHeadings().get(0).getType()).isEqualTo(SSHeadingType.HEADING3);
        assertThat(reloaded.get().getHeadings().get(0).getHeading()).isEqualTo("Repo heading updated");
        assertThat(reloaded.get().getHeadings().get(0).getAccountRows()).hasSize(1);
        assertThat(reloaded.get().getHeadings().get(0).getAccountRows().get(0).getAccount().getNumber()).isEqualTo(4010);

        Integer ownReportId = reloaded.get().getId();
        Repositories.ownReports().delete(reloaded.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSOwnReport deletedProbe = new SSOwnReport();
        deletedProbe.setId(ownReportId);
        assertThat(Repositories.ownReports().findByOwnReport(deletedProbe)).isEmpty();
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




