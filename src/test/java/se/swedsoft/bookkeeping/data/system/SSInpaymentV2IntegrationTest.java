package se.swedsoft.bookkeeping.data.system;

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
 * Integration slice for inpayment CRUD against schema V2.
 */
@Tag("integration")
class SSInpaymentV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_inpayment";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Inpayment Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Inpayment Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
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
    void clearCaches() {
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void addAndFetchInpaymentWithRowsInSchemaV2() {
        SSInpayment inpayment = inpayment("Initial inpayment text");
        inpayment.getRows().add(row(10101, "SEK", "1.000000", "1200.00", "1.000000"));
        inpayment.getRows().add(row(10102, "EUR", "11.200000", "200.00", "11.350000"));

        SSDB.getInstance().addInpayment(inpayment);

        assertThat(inpayment.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> fetched = SSDB.getInstance().getInpayment(inpayment);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 30));
        assertThat(fetched.get().getText()).isEqualTo("Initial inpayment text");
        assertThat(fetched.get().isEntered()).isFalse();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getInvoiceNr()).isEqualTo(10101);
        assertThat(fetched.get().getRows().get(0).getInvoiceCurrency().getName()).isEqualTo("SEK");
        assertThat(fetched.get().getRows().get(0).getInvoiceCurrencyRate()).isEqualByComparingTo("1.000000");
        assertThat(fetched.get().getRows().get(0).getValue()).isEqualByComparingTo("1200.00");
        assertThat(fetched.get().getRows().get(0).getCurrencyRate()).isEqualByComparingTo("1.000000");

        SSDB.getInstance().deleteInpayment(inpayment);
    }

    @Test
    void updateAndDeleteInpaymentInSchemaV2() {
        SSInpayment inpayment = inpayment("Before inpayment update");
        inpayment.getRows().add(row(10103, "SEK", "1.000000", "500.00", "1.000000"));
        SSDB.getInstance().addInpayment(inpayment);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> fetched = SSDB.getInstance().getInpayment(inpayment);
        assertThat(fetched).isPresent();

        SSInpayment updatedInpayment = fetched.get();
        updatedInpayment.setText("After inpayment update");
        updatedInpayment.setLocalDate(LocalDate.of(2025, 9, 18));
        updatedInpayment.setEntered(true);
        updatedInpayment.getRows().clear();
        updatedInpayment.getRows().add(row(10104, "USD", "10.500000", "150.00", "10.700000"));
        SSDB.getInstance().updateInpayment(updatedInpayment);

        SSDB.getInstance().clearLists();
        Optional<SSInpayment> updated = SSDB.getInstance().getInpayment(inpayment);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After inpayment update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 9, 18));
        assertThat(updated.get().isEntered()).isTrue();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getInvoiceNr()).isEqualTo(10104);
        assertThat(updated.get().getRows().get(0).getInvoiceCurrency().getName()).isEqualTo("USD");
        assertThat(updated.get().getRows().get(0).getInvoiceCurrencyRate()).isEqualByComparingTo("10.500000");
        assertThat(updated.get().getRows().get(0).getValue()).isEqualByComparingTo("150.00");
        assertThat(updated.get().getRows().get(0).getCurrencyRate()).isEqualByComparingTo("10.700000");

        Integer inpaymentNumber = updated.get().getNumber();
        SSDB.getInstance().deleteInpayment(updated.get());
        SSDB.getInstance().clearLists();
        List<SSInpayment> all = SSDB.getInstance().getInpayments();
        assertThat(all).extracting(SSInpayment::getNumber).doesNotContain(inpaymentNumber);
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
        throw new IllegalStateException("Could not create test company for schema V2 inpayment test");
    }
}

