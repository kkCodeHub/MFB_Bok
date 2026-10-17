package se.swedsoft.bookkeeping.gui.invoice.panel;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SSInvoiceSearchPanelTest {

    @Test
    void filterInvoicesMatchesInterestInvoiceType() {
        SSInvoice interestInvoice = new SSInvoice();
        interestInvoice.setNumber(1001);
        interestInvoice.setType(SSInvoiceType.INTEREST);
        interestInvoice.setCustomerName("Kund A");

        SSInvoice normalInvoice = new SSInvoice();
        normalInvoice.setNumber(1002);
        normalInvoice.setType(SSInvoiceType.NORMAL);
        normalInvoice.setCustomerName("Kund B");

        List<SSInvoice> filtered = SSInvoiceSearchPanel.filterInvoices(
                List.of(interestInvoice, normalInvoice), "interest");

        assertThat(filtered).containsExactly(interestInvoice);
    }

    @Test
    void filterInvoicesMatchesOriginInvoiceNumber() {
        SSInvoice interestInvoice = new SSInvoice();
        interestInvoice.setNumber(2001);
        interestInvoice.setType(SSInvoiceType.INTEREST);
        interestInvoice.setOriginInvoiceNumber(999);

        SSInvoice normalInvoice = new SSInvoice();
        normalInvoice.setNumber(2002);
        normalInvoice.setType(SSInvoiceType.NORMAL);

        List<SSInvoice> filtered = SSInvoiceSearchPanel.filterInvoices(
                List.of(interestInvoice, normalInvoice), "999");

        assertThat(filtered).containsExactly(interestInvoice);
    }
}
