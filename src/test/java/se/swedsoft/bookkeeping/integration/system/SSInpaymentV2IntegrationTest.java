package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSPaymentContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-i-n-p-a-y-m-e-n-t-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Inpayment Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Inpayment Test Company AB");
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
    void addAndFetchInpaymentWithRowsInSchemaV2() {
        SSInpayment inpayment = inpayment("Initial inpayment text");
        inpayment.getRows().add(row(10101, "SEK", "1.000000", "1200.00", "1.000000"));
        inpayment.getRows().add(row(10102, "EUR", "11.200000", "200.00", "11.350000"));

        SSPaymentContext.addInpayment(inpayment);

        assertThat(inpayment.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInpayment> fetched = SSPaymentContext.getInpayment(inpayment);
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

        SSPaymentContext.deleteInpayment(inpayment);
    }

    @Test
    void updateAndDeleteInpaymentInSchemaV2() {
        SSInpayment inpayment = inpayment("Before inpayment update");
        inpayment.getRows().add(row(10103, "SEK", "1.000000", "500.00", "1.000000"));
        SSPaymentContext.addInpayment(inpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInpayment> fetched = SSPaymentContext.getInpayment(inpayment);
        assertThat(fetched).isPresent();

        SSInpayment updatedInpayment = fetched.get();
        updatedInpayment.setText("After inpayment update");
        updatedInpayment.setLocalDate(LocalDate.of(2025, 9, 18));
        updatedInpayment.setEntered(true);
        updatedInpayment.getRows().clear();
        updatedInpayment.getRows().add(row(10104, "USD", "10.500000", "150.00", "10.700000"));
        SSPaymentContext.updateInpayment(updatedInpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInpayment> updated = SSPaymentContext.getInpayment(inpayment);
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
        SSPaymentContext.deleteInpayment(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSInpayment> all = SSPaymentContext.getInpayments();
        assertThat(all).extracting(SSInpayment::getNumber).doesNotContain(inpaymentNumber);
    }

    @Test
    void deleteInpaymentTwiceKeepsListConsistentInSchemaV2() {
        SSInpayment inpayment = inpayment("Delete twice inpayment");
        inpayment.getRows().add(row(10105, "SEK", "1.000000", "250.00", "1.000000"));
        SSPaymentContext.addInpayment(inpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInpayment> stored = SSPaymentContext.getInpayment(inpayment);
        assertThat(stored).isPresent();

        Integer inpaymentNumber = stored.get().getNumber();

        SSPaymentContext.deleteInpayment(stored.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        // Simulate a second delete attempt from a stale UI row reference.
        SSPaymentContext.deleteInpayment(stored.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        Optional<SSInpayment> deleted = SSPaymentContext.getInpayment(stored.get());
        assertThat(deleted).isEmpty();

        List<SSInpayment> all = SSPaymentContext.getInpayments();
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
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


