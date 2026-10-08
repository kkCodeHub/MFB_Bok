package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSMonth;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("integration")
class SSSalevaluesV2IntegrationTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(
                "jdbc:hsqldb:mem:salevalues-report-" + System.nanoTime() + ";sql.syntax_mys=true");
        Integer companyId = SSV2DatabaseFixture.createCompany(connection, "Testbolaget");
        SSV2DatabaseFixture.setCurrentCompany(companyId, "Testbolaget");
        SSV2DatabaseFixture.createAndSetCurrentYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    }

    @AfterEach
    void tearDown() throws Exception {
        SSV2DatabaseFixture.clearState();
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @Test
    void monthlyInvoiceValueExcludesCancelledInvoicesAndCreditInvoices() {
        SSInvoice activeInvoice = invoice("INV-ACT", false, "100.00", "2.00");
        SSInvoice cancelledInvoice = invoice("INV-CAN", true, "300.00", "2.00");
        SSCreditInvoice activeCreditInvoice = creditInvoice("CR-ACT", false, "10.00", "2.00");
        SSCreditInvoice cancelledCreditInvoice = creditInvoice("CR-CAN", true, "50.00", "2.00");

        SSSalesContext.addInvoice(activeInvoice);
        SSSalesContext.addInvoice(cancelledInvoice);
        SSSalesContext.addCreditInvoice(activeCreditInvoice);
        SSSalesContext.addCreditInvoice(cancelledCreditInvoice);

        SSMonth month = new SSMonth(LocalDate.of(2025, 7, 1));
        Double value = SSCompanyYearContext.getCurrentCompany().getInvoiceValueForMonth(month);
        double expected = salesValue(activeInvoice) - salesValue(activeCreditInvoice);

        assertThat(value).isCloseTo(expected, within(0.0001));
    }

    private static SSInvoice invoice(String customerNr, boolean cancelled, String amount, String rate) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerNr);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 15));
        invoice.setCurrencyRate(new BigDecimal(rate));
        invoice.setType(SSInvoiceType.CASH);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(amount));
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(String customerNr, boolean cancelled, String amount, String rate) {
        SSCreditInvoice invoice = new SSCreditInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerNr);
        invoice.setCreditingNr(1);
        invoice.setLocalDate(LocalDate.of(2025, 7, 20));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 20));
        invoice.setCurrencyRate(new BigDecimal(rate));
        invoice.setType(SSInvoiceType.CASH);
        invoice.setCancelled(cancelled);
        invoice.getRows().add(row(amount));
        return invoice;
    }

    private static SSSaleRow row(String unitPrice) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr("P-1");
        row.setDescription("Test row");
        row.setQuantity(1);
        row.setUnitprice(new BigDecimal(unitPrice));
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
        row.setAccountNr(3010);
        return row;
    }

    private static double salesValue(SSInvoice sale) {
        double net = 0.0;
        for (SSSaleRow row : sale.getRows()) {
            if (row.getSum().isPresent()) {
                net += row.getSum().get().doubleValue();
            }
        }
        return net * sale.getCurrencyRate().doubleValue();
    }
}
