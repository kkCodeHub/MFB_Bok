package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSPaymentContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-o-u-t-p-a-y-m-e-n-t-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Outpayment Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Outpayment Test Company AB");
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
    void addAndFetchOutpaymentWithRowsInSchemaV2() {
        SSOutpayment outpayment = outpayment("Initial outpayment text");
        outpayment.getRows().add(row(20101, "SEK", "1.000000", "1000.00", "1.000000"));
        outpayment.getRows().add(row(20102, "EUR", "11.200000", "175.00", "11.350000"));

        SSPaymentContext.addOutpayment(outpayment);

        assertThat(outpayment.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutpayment> fetched = SSPaymentContext.getOutpayment(outpayment);
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

        SSPaymentContext.deleteOutpayment(outpayment);
    }

    @Test
    void updateAndDeleteOutpaymentInSchemaV2() {
        SSOutpayment outpayment = outpayment("Before outpayment update");
        outpayment.getRows().add(row(20103, "SEK", "1.000000", "300.00", "1.000000"));
        SSPaymentContext.addOutpayment(outpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutpayment> fetched = SSPaymentContext.getOutpayment(outpayment);
        assertThat(fetched).isPresent();

        SSOutpayment updatedOutpayment = fetched.get();
        updatedOutpayment.setText("After outpayment update");
        updatedOutpayment.setLocalDate(LocalDate.of(2025, 10, 1));
        updatedOutpayment.setEntered(true);
        updatedOutpayment.getRows().clear();
        updatedOutpayment.getRows().add(row(20104, "USD", "10.500000", "120.00", "10.700000"));
        SSPaymentContext.updateOutpayment(updatedOutpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutpayment> updated = SSPaymentContext.getOutpayment(outpayment);
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
        SSPaymentContext.deleteOutpayment(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSOutpayment> all = SSPaymentContext.getOutpayments();
        assertThat(all).extracting(SSOutpayment::getNumber).doesNotContain(outpaymentNumber);
    }

    @Test
    void deleteOutpaymentTwiceKeepsListConsistentInSchemaV2() {
        SSOutpayment outpayment = outpayment("Delete twice outpayment");
        outpayment.getRows().add(row(20105, "SEK", "1.000000", "275.00", "1.000000"));
        SSPaymentContext.addOutpayment(outpayment);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSOutpayment> stored = SSPaymentContext.getOutpayment(outpayment);
        assertThat(stored).isPresent();

        Integer outpaymentNumber = stored.get().getNumber();

        SSPaymentContext.deleteOutpayment(stored.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        // Simulate a second delete attempt from a stale UI row reference.
        SSPaymentContext.deleteOutpayment(stored.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        Optional<SSOutpayment> deleted = SSPaymentContext.getOutpayment(stored.get());
        assertThat(deleted).isEmpty();

        List<SSOutpayment> all = SSPaymentContext.getOutpayments();
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
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


