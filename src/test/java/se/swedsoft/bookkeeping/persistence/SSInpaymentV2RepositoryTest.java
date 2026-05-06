package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.system.SSDB;

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
 * Integration tests for inpayment repository wiring in schema V2.
 */
@Tag("integration")
class SSInpaymentV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_inpayment_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Inpayment Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Inpayment Repo Test AB");
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
    void repositoriesInitUsesV2InpaymentRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.inpayments()).isNotNull();
    }

    @Test
    void addAndFetchInpaymentViaRepository() {
        SSInpayment inpayment = inpayment("Inpayment repo text");
        inpayment.getRows().add(row(11001, "SEK", "1.000000", "1200.00", "1.000000"));
        inpayment.getRows().add(row(11002, "EUR", "10.900000", "200.00", "11.100000"));

        Repositories.inpayments().add(inpayment);
        assertThat(inpayment.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> fetched = Repositories.inpayments().findByInpayment(inpayment);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getText()).isEqualTo("Inpayment repo text");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getInvoiceNr()).isEqualTo(11001);

        Repositories.inpayments().delete(fetched.get());
    }

    @Test
    void updateAndDeleteInpaymentViaRepository() {
        SSInpayment inpayment = inpayment("Before inpayment repo update");
        inpayment.getRows().add(row(11003, "SEK", "1.000000", "500.00", "1.000000"));
        Repositories.inpayments().add(inpayment);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> fetched = Repositories.inpayments().findByInpayment(inpayment);
        assertThat(fetched).isPresent();

        SSInpayment updatedInpayment = fetched.get();
        updatedInpayment.setText("After inpayment repo update");
        updatedInpayment.setLocalDate(LocalDate.of(2025, 9, 5));
        updatedInpayment.setEntered(true);
        updatedInpayment.getRows().clear();
        updatedInpayment.getRows().add(row(11004, "USD", "10.400000", "175.00", "10.600000"));
        Repositories.inpayments().update(updatedInpayment);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> updated = Repositories.inpayments().findByInpayment(inpayment);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After inpayment repo update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 5));
        assertThat(updated.get().isEntered()).isTrue();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getInvoiceNr()).isEqualTo(11004);

        Integer number = updated.get().getNumber();
        Repositories.inpayments().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSInpayment> all = Repositories.inpayments().findAll();
        assertThat(all).extracting(SSInpayment::getNumber).doesNotContain(number);
    }

    private static SSInpayment inpayment(String text) {
        SSInpayment inpayment = new SSInpayment();
        inpayment.setLocalDate(LocalDate.of(2025, 7, 30));
        inpayment.setText(text);
        return inpayment;
    }

    private static SSInpaymentRow row(Integer invoiceNr,
                                      String invoiceCurrencyCode,
                                      String invoiceCurrencyRate,
                                      String value,
                                      String currencyRate) {
        SSInpaymentRow row = new SSInpaymentRow();
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
        throw new IllegalStateException("Could not create test company for inpayment repository V2 integration test");
    }
}

