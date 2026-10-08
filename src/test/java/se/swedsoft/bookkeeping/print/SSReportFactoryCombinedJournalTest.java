package se.swedsoft.bookkeeping.print;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSVoucher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSReportFactoryCombinedJournalTest {

    @Test
    void filterInvoicesForCombinedJournalIncludesInvoiceAndCreditInvoiceInPeriod() {
        SSInvoice invoice = invoice(LocalDate.of(2025, 1, 10), false, false);
        SSCreditInvoice creditInvoice = creditInvoice(LocalDate.of(2025, 1, 11), false, false);

        List<SSInvoice> filtered = SSReportFactory.filterInvoicesForCombinedJournal(
                List.of(invoice),
                List.of(creditInvoice),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(filtered).hasSize(2);
        assertThat(filtered).extracting(SSInvoice::getNumber).containsExactly(1010, 2011);
    }

    @Test
    void filterInvoicesForCombinedJournalExcludesEnteredAndOutOfPeriodButKeepsCancelled() {
        SSInvoice enteredInvoice = invoice(LocalDate.of(2025, 1, 10), true, false);
        SSInvoice cancelledInvoice = invoice(LocalDate.of(2025, 1, 10), false, true);
        SSInvoice outsidePeriodInvoice = invoice(LocalDate.of(2025, 2, 1), false, false);
        SSCreditInvoice enteredCreditInvoice = creditInvoice(LocalDate.of(2025, 1, 10), true, false);
        SSCreditInvoice outsidePeriodCreditInvoice = creditInvoice(LocalDate.of(2025, 2, 1), false, false);
        SSCreditInvoice eligibleCreditInvoice = creditInvoice(LocalDate.of(2025, 1, 12), false, false);

        List<SSInvoice> filtered = SSReportFactory.filterInvoicesForCombinedJournal(
                List.of(enteredInvoice, cancelledInvoice, outsidePeriodInvoice),
                List.of(enteredCreditInvoice, outsidePeriodCreditInvoice, eligibleCreditInvoice),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(filtered).extracting(SSInvoice::getNumber).containsExactly(1010, 2012);
    }

    @Test
    void filterInvoicesForCombinedJournalHandlesNullLists() {
        List<SSInvoice> filtered = SSReportFactory.filterInvoicesForCombinedJournal(
                null,
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(filtered).isEmpty();
    }

    @Test
    void filterInvoicesForCombinedJournalKeepsInvoiceAndCreditAsSeparateEntries() {
        SSInvoice invoice = invoice(LocalDate.of(2025, 1, 10), false, false);
        SSCreditInvoice creditInvoice = creditInvoice(LocalDate.of(2025, 1, 10), false, false);
        invoice.setNumber(1500);
        creditInvoice.setNumber(1500);
        invoice.setCustomerNr("C-1");
        creditInvoice.setCustomerNr("C-1");

        List<SSInvoice> filtered = SSReportFactory.filterInvoicesForCombinedJournal(
                List.of(invoice),
                List.of(creditInvoice),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(filtered).hasSize(2);
        assertThat(filtered.get(0)).isSameAs(invoice);
        assertThat(filtered.get(1)).isSameAs(creditInvoice);
    }

    @Test
    void registerCombinedJournalEntriesAppliesCommonJournalAndVoucherToBothTypes() {
        SSInvoice invoice = invoice(LocalDate.of(2025, 1, 10), false, false);
        SSCreditInvoice creditInvoice = creditInvoice(LocalDate.of(2025, 1, 11), false, false);
        SSVoucher voucher = new SSVoucher();
        voucher.setNumber(777);
        List<SSInvoice> persisted = new ArrayList<>();

        SSReportFactory.registerCombinedJournalEntries(
                List.of(invoice, creditInvoice),
                "FA777",
                voucher,
                persisted::add);

        assertThat(invoice.getJournalNumbers()).isEqualTo("FA777");
        assertThat(creditInvoice.getJournalNumbers()).isEqualTo("FA777");
        assertThat(invoice.isEntered()).isTrue();
        assertThat(creditInvoice.isEntered()).isTrue();
        assertThat(invoice.getVoucher()).isSameAs(voucher);
        assertThat(creditInvoice.getVoucher()).isSameAs(voucher);
        assertThat(persisted).extracting(SSInvoice::getNumber).containsExactly(1010, 2011);
    }

    @Test
    void registerCombinedJournalEntriesSkipsAlreadyEnteredInvoices() {
        SSInvoice enteredInvoice = invoice(LocalDate.of(2025, 1, 10), true, false);
        SSCreditInvoice eligibleCreditInvoice = creditInvoice(LocalDate.of(2025, 1, 12), false, false);
        SSVoucher voucher = new SSVoucher();
        List<SSInvoice> persisted = new ArrayList<>();

        SSReportFactory.registerCombinedJournalEntries(
                List.of(enteredInvoice, eligibleCreditInvoice),
                "FA778",
                voucher,
                persisted::add);

        assertThat(enteredInvoice.getJournalNumbers()).isNull();
        assertThat(eligibleCreditInvoice.getJournalNumbers()).isEqualTo("FA778");
        assertThat(persisted).extracting(SSInvoice::getNumber).containsExactly(2012);
    }

    @Test
    void registerCombinedJournalEntriesRegistersCancelledInvoicesToo() {
        SSInvoice cancelledInvoice = invoice(LocalDate.of(2025, 1, 10), false, true);
        SSCreditInvoice cancelledCreditInvoice = creditInvoice(LocalDate.of(2025, 1, 11), false, true);
        SSInvoice eligibleInvoice = invoice(LocalDate.of(2025, 1, 12), false, false);
        SSVoucher voucher = new SSVoucher();
        List<SSInvoice> persisted = new ArrayList<>();

        SSReportFactory.registerCombinedJournalEntries(
                List.of(cancelledInvoice, cancelledCreditInvoice, eligibleInvoice),
                "FA779",
                voucher,
                persisted::add);

        assertThat(cancelledInvoice.getJournalNumbers()).isEqualTo("FA779");
        assertThat(cancelledCreditInvoice.getJournalNumbers()).isEqualTo("FA779");
        assertThat(eligibleInvoice.getJournalNumbers()).isEqualTo("FA779");
        assertThat(cancelledInvoice.isEntered()).isTrue();
        assertThat(cancelledCreditInvoice.isEntered()).isTrue();
        assertThat(eligibleInvoice.isEntered()).isTrue();
        assertThat(persisted).extracting(SSInvoice::getNumber).containsExactly(1010, 2011, 1012);
    }

    @Test
    void canRegisterCombinedJournalBlocksWhenNoOutputWasProduced() {
        final boolean[] messageShown = {false};

        boolean canRegister = SSReportFactory.canRegisterCombinedJournal(false, () -> messageShown[0] = true);

        assertThat(canRegister).isFalse();
        assertThat(messageShown[0]).isTrue();
    }

    @Test
    void canRegisterCombinedJournalAllowsPostingAfterOutputWasProduced() {
        final boolean[] messageShown = {false};

        boolean canRegister = SSReportFactory.canRegisterCombinedJournal(true, () -> messageShown[0] = true);

        assertThat(canRegister).isTrue();
        assertThat(messageShown[0]).isFalse();
    }

    private static SSInvoice invoice(LocalDate date, boolean entered, boolean cancelled) {
        SSInvoice invoice = new SSInvoice();
        invoice.setNumber(1000 + date.getDayOfMonth());
        invoice.setLocalDate(date);
        invoice.setEntered(entered);
        invoice.setCancelled(cancelled);
        return invoice;
    }

    private static SSCreditInvoice creditInvoice(LocalDate date, boolean entered, boolean cancelled) {
        SSCreditInvoice creditInvoice = new SSCreditInvoice();
        creditInvoice.setNumber(2000 + date.getDayOfMonth());
        creditInvoice.setLocalDate(date);
        creditInvoice.setEntered(entered);
        creditInvoice.setCancelled(cancelled);
        return creditInvoice;
    }
}
