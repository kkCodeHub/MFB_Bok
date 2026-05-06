package se.swedsoft.bookkeeping.data.system;

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
 * Integration slice for outpayment CRUD against schema V2.
 */
@Tag("integration")
class SSOutpaymentV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_outpayment";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Outpayment Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Outpayment Test Company AB");
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
    void addAndFetchOutpaymentWithRowsInSchemaV2() {
        SSOutpayment outpayment = outpayment("Initial outpayment text");
        outpayment.getRows().add(row(20101, "SEK", "1.000000", "1000.00", "1.000000"));
        outpayment.getRows().add(row(20102, "EUR", "11.200000", "175.00", "11.350000"));

        SSDB.getInstance().addOutpayment(outpayment);

        assertThat(outpayment.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> fetched = SSDB.getInstance().getOutpayment(outpayment);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 8, 12));
        assertThat(fetched.get().getText()).isEqualTo("Initial outpayment text");
        assertThat(fetched.get().isEntered()).isFalse();
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getInvoiceNr()).isEqualTo(20101);
        assertThat(fetched.get().getRows().get(0).getInvoiceCurrency().getName()).isEqualTo("SEK");
        assertThat(fetched.get().getRows().get(0).getInvoiceCurrencyRate()).isEqualByComparingTo("1.000000");
        assertThat(fetched.get().getRows().get(0).getValue()).isEqualByComparingTo("1000.00");
        assertThat(fetched.get().getRows().get(0).getCurrencyRate()).isEqualByComparingTo("1.000000");

        SSDB.getInstance().deleteOutpayment(outpayment);
    }

    @Test
    void updateAndDeleteOutpaymentInSchemaV2() {
        SSOutpayment outpayment = outpayment("Before outpayment update");
        outpayment.getRows().add(row(20103, "SEK", "1.000000", "300.00", "1.000000"));
        SSDB.getInstance().addOutpayment(outpayment);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> fetched = SSDB.getInstance().getOutpayment(outpayment);
        assertThat(fetched).isPresent();

        SSOutpayment updatedOutpayment = fetched.get();
        updatedOutpayment.setText("After outpayment update");
        updatedOutpayment.setLocalDate(LocalDate.of(2025, 10, 1));
        updatedOutpayment.setEntered(true);
        updatedOutpayment.getRows().clear();
        updatedOutpayment.getRows().add(row(20104, "USD", "10.500000", "120.00", "10.700000"));
        SSDB.getInstance().updateOutpayment(updatedOutpayment);

        SSDB.getInstance().clearLists();
        Optional<SSOutpayment> updated = SSDB.getInstance().getOutpayment(outpayment);
        assertThat(updated).isPresent();
        assertThat(updated.get().getText()).isEqualTo("After outpayment update");
        assertThat(updated.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(updated.get().isEntered()).isTrue();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getInvoiceNr()).isEqualTo(20104);
        assertThat(updated.get().getRows().get(0).getInvoiceCurrency().getName()).isEqualTo("USD");
        assertThat(updated.get().getRows().get(0).getInvoiceCurrencyRate()).isEqualByComparingTo("10.500000");
        assertThat(updated.get().getRows().get(0).getValue()).isEqualByComparingTo("120.00");
        assertThat(updated.get().getRows().get(0).getCurrencyRate()).isEqualByComparingTo("10.700000");

        Integer outpaymentNumber = updated.get().getNumber();
        SSDB.getInstance().deleteOutpayment(updated.get());
        SSDB.getInstance().clearLists();
        List<SSOutpayment> all = SSDB.getInstance().getOutpayments();
        assertThat(all).extracting(SSOutpayment::getNumber).doesNotContain(outpaymentNumber);
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
        throw new IllegalStateException("Could not create test company for schema V2 outpayment test");
    }
}

