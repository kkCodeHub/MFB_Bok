package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSAccountingContextVoucherSeriesResolverTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_voucher_series_resolver";
    private static Connection connection;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("fribok.schema.version", "v2");
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        SSSystemConfigContext.startupLocal(connection);

        SSNewCompany company = new SSNewCompany();
        company.setName("Voucher Series Resolver Test AB");
        SSCompanyYearContext.addCompany(company);
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        SSCompanyYearContext.addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
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
    void shouldResolveSeriesFromCurrentYearMappingForKiEvent() {
        assertThat(SSAccountingContext.resolveVoucherSeriesForEventCode("KI")).isEqualTo("B");
    }

    @Test
    void shouldNormalizeEventCodeBeforeLookup() {
        assertThat(SSAccountingContext.resolveVoucherSeriesForEventCode(" ki ")).isEqualTo("B");
    }

    @Test
    void shouldFallbackToSeriesAWhenEventCodeIsUnknown() {
        assertThat(SSAccountingContext.resolveVoucherSeriesForEventCode("UNKNOWN")).isEqualTo("A");
    }
}
