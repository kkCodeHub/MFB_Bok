package se.swedsoft.bookkeeping.print.util;


import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SSPrintFileNameTest {

    @Test
    void buildsInvoiceNameWithNumberCustomerAndYear() {
        assertEquals("Kundfaktura 1001, 10 2026",
                SSPrintFileName.build("Kundfaktura", 1001, "10", LocalDate.of(2026, 3, 5)));
    }

    @Test
    void buildsCreditInvoiceName() {
        assertEquals("Kreditfaktura 2001, K7 2025",
                SSPrintFileName.build("Kreditfaktura", 2001, "K7", LocalDate.of(2025, 12, 31)));
    }

    @Test
    void buildsCashReceiptNameWithoutCustomer() {
        assertEquals("Kontantkvitto 1002 2026",
                SSPrintFileName.build("Kontantkvitto", 1002, "", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void removesParenthesesAndIllegalCharacters() {
        assertEquals("Kundfaktura 5, AB 2026",
                SSPrintFileName.build("Kundfaktura", 5, "A/B(*)", LocalDate.of(2026, 1, 1)));
    }
}
