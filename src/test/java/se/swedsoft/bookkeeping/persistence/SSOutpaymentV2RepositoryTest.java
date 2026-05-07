package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2OutpaymentRepository;

import java.math.BigDecimal;
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
 * Integration tests for outpayment repository wiring in schema V2.
 */
@Tag("integration")
class SSOutpaymentV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_outpayment_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Outpayment Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Outpayment Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
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
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2OutpaymentRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.outpayments()).isInstanceOf(V2OutpaymentRepository.class);
    }

    @Test
    void addAndFetchOutpaymentViaRepository() {
        SSOutpayment outpayment = outpayment("Outpayment repo text");
        outpayment.getRows().add(row(21001, "SEK", "1.000000", "850.00", "1.000000"));
        outpayment.getRows().add(row(21002, "EUR", "10.700000", "125.00", "10.900000"));

        Repositories.outpayments().add(outpayment);
        assertThat(outpayment.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> fetched = Repositories.outpayments().findByOutpayment(outpayment);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getText()).isEqualTo("Outpayment repo text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getInvoiceNr()).isEqualTo(21001);

        Repositories.outpayments().delete(fetched.get());
    }

    @Test
    void updateAndDeleteOutpaymentViaRepository() {
        SSOutpayment outpayment = outpayment("Before outpayment repo update");
        outpayment.getRows().add(row(21003, "SEK", "1.000000", "300.00", "1.000000"));
        Repositories.outpayments().add(outpayment);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> fetched = Repositories.outpayments().findByOutpayment(outpayment);
        assertThat(fetched).isPresent();

        SSOutpayment updatedOutpayment = fetched.get();
        updatedOutpayment.setText("After outpayment repo update");
        updatedOutpayment.setLocalDate(LocalDate.of(2025, 10, 1));
        updatedOutpayment.setEntered(true);
        updatedOutpayment.getRows().clear();
        updatedOutpayment.getRows().add(row(21004, "USD", "10.200000", "95.00", "10.450000"));
        Repositories.outpayments().update(updatedOutpayment);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> updated = Repositories.outpayments().findByOutpayment(outpayment);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After outpayment repo update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(updated.get().isEntered()).isTrue();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getInvoiceNr()).isEqualTo(21004);

        Integer number = updated.get().getNumber();
        Repositories.outpayments().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSOutpayment> all = Repositories.outpayments().findAll();
        assertThat(all).extracting(SSOutpayment::getNumber).doesNotContain(number);
    }

    private static SSOutpayment outpayment(String text) {
        SSOutpayment outpayment = new SSOutpayment();
        outpayment.setLocalDate(LocalDate.of(2025, 8, 12));
        outpayment.setText(text);
        return outpayment;
    }

    private static SSOutpaymentRow row(Integer invoiceNr,
                                       String invoiceCurrencyCode,
                                       String invoiceCurrencyRate,
                                       String value,
                                       String currencyRate) {
        SSOutpaymentRow row = new SSOutpaymentRow();
        row.setInvoiceNr(invoiceNr);

        SSCurrency currency = new SSCurrency();
        currency.setName(invoiceCurrencyCode);
        currency.setDescription(invoiceCurrencyCode);
        row.setInvoiceCurrency(currency);

        row.setInvoiceCurrencyRate(new BigDecimal(invoiceCurrencyRate));
        row.setValue(new BigDecimal(value));
        row.setCurrencyRate(new BigDecimal(currencyRate));
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Could not create test company for outpayment repository V2 integration test");
    }
}

