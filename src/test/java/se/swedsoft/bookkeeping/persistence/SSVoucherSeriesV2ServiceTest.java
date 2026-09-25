package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherEventTypeRepository;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherSeriesService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
class SSVoucherSeriesV2ServiceTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_voucher_series_service";
    private static Connection connection;
    private static V2VoucherSeriesService service;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);
        Repositories.init(SSDB.getInstance());

        SSNewCompany company = SSDB.getInstance().getCurrentCompany();
        if (company == null) {
            company = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies().get(0);
            SSDB.getInstance().setCurrentCompany(company);
        }
        Optional<SSNewAccountingYear> currentYear = Repositories.accountingYears().findCurrent();
        if (currentYear.isEmpty()) {
            SSNewAccountingYear year = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext
                    .getYearsForCompany(company).get(0);
            SSDB.getInstance().setCurrentYear(year);
        }

        service = new V2VoucherSeriesService(connection);
    }

    @AfterAll
    static void teardown() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @Test
    void startupSeedStoresSystemAndActiveFlagsForVoucherEventType() throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT system, active FROM PUBLIC.tbl_voucher_event_type WHERE event_code = ?")) {
            statement.setString(1, "KF");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("system")).isTrue();
                assertThat(resultSet.getBoolean("active")).isTrue();
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM PUBLIC.tbl_voucher_event_type WHERE system = FALSE AND event_code IS NULL")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isGreaterThan(0);
            }
        }
    }

    @Test
    void initializeDefaultMappingsCarriesActiveFlagFromEventType() throws Exception {
        V2VoucherEventTypeRepository eventTypeRepository = Repositories.voucherEventTypes();
        eventTypeRepository.upsertFromSeed("ZX", "Zx test event", "Z", false, false);

        SSNewAccountingYear year = createEmptyYear(LocalDate.of(2037, 1, 1), LocalDate.of(2037, 12, 31));
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT active FROM tbl_year_voucher_event_series_map WHERE year_id = ? AND event_code = ?")) {
            statement.setInt(1, year.getId());
            statement.setString(2, "ZX");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean("active")).isFalse();
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM tbl_year_voucher_event_series_map WHERE year_id = ? AND is_custom = TRUE AND event_code IS NULL")) {
            statement.setInt(1, year.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isGreaterThan(0);
            }
        }
    }

    @Test
    void customSeriesDeleteIsBlockedWhenVouchersExistForThatSeries() throws Exception {
        SSNewAccountingYear year = createEmptyYear(LocalDate.of(2039, 1, 1), LocalDate.of(2039, 12, 31));
        service.addCustomMapping(year.getId(), "Test egen serie", "Q");

        int mappingId;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_year_voucher_event_series_map WHERE year_id = ? AND is_custom = TRUE AND series_code = ?")) {
            statement.setInt(1, year.getId());
            statement.setString(2, "Q");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                mappingId = resultSet.getInt("id");
            }
        }

        try (PreparedStatement insertVoucher = connection.prepareStatement(
                "INSERT INTO tbl_voucher(series, number, yearid) VALUES (?, ?, ?)")) {
            insertVoucher.setString(1, "Q");
            insertVoucher.setInt(2, 1);
            insertVoucher.setInt(3, year.getId());
            insertVoucher.executeUpdate();
            connection.commit();
        }

        assertThat(service.canDeleteCustomMapping(mappingId)).isFalse();
        assertThatThrownBy(() -> service.deleteCustomMapping(mappingId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    void systemSeriesCodeChangeIsBlockedWhenYearHasVouchers() throws Exception {
        SSNewAccountingYear year = createEmptyYear(LocalDate.of(2038, 1, 1), LocalDate.of(2038, 12, 31));

        assertThat(service.canChangeSystemSeriesCode(year.getId())).isTrue();
        service.updateSystemMappingSeriesCode(year.getId(), "KF", "Y");

        try (PreparedStatement insertVoucher = connection.prepareStatement(
                "INSERT INTO tbl_voucher(series, number, yearid) VALUES (?, ?, ?)")) {
            insertVoucher.setString(1, "A");
            insertVoucher.setInt(2, 1);
            insertVoucher.setInt(3, year.getId());
            insertVoucher.executeUpdate();
            connection.commit();
        }

        assertThat(service.canChangeSystemSeriesCode(year.getId())).isFalse();
        assertThatThrownBy(() -> service.updateSystemMappingSeriesCode(year.getId(), "KF", "X"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be changed");
    }

    private SSNewAccountingYear createEmptyYear(LocalDate from, LocalDate to) {
        SSNewAccountingYear templateYear = Repositories.accountingYears().findCurrent()
                .orElseThrow(() -> new IllegalStateException("No current year found"));

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(from);
        year.setLocalTo(to);
        year.setAccountPlan(templateYear.getAccountPlan());
        Repositories.accountingYears().add(year);
        return year;
    }
}
