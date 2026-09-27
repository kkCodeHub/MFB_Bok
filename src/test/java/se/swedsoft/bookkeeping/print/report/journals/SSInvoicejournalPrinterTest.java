package se.swedsoft.bookkeeping.print.report.journals;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoicejournalPrinterTest {

    @Test
    void signedAmountIsNegativeForCreditInvoice() {
        SSCreditInvoice creditInvoice = new SSCreditInvoice();
        BigDecimal signed = SSInvoicejournalPrinter.toSignedJournalAmount(
                creditInvoice, new BigDecimal("125.50"));

        assertThat(signed).isEqualByComparingTo("-125.50");
    }

    @Test
    void signedAmountIsPositiveForInvoice() {
        SSInvoice invoice = new SSInvoice();
        BigDecimal signed = SSInvoicejournalPrinter.toSignedJournalAmount(
                invoice, new BigDecimal("125.50"));

        assertThat(signed).isEqualByComparingTo("125.50");
    }

    @Test
    void signedAmountsReduceTotalWhenCreditInvoiceIsIncluded() {
        SSInvoice invoice = new SSInvoice();
        SSCreditInvoice creditInvoice = new SSCreditInvoice();

        BigDecimal total = List.of(
                SSInvoicejournalPrinter.toSignedJournalAmount(invoice, new BigDecimal("1000.00")),
                SSInvoicejournalPrinter.toSignedJournalAmount(creditInvoice, new BigDecimal("250.00")))
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(total).isEqualByComparingTo("750.00");
    }
}
