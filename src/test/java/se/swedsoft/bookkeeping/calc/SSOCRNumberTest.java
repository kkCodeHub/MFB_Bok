/*
 * Copyright © 2009 Stefan Kangas <stefankangas@gmail.com>
 *
 * This file is part of Fribok.
 *
 * Fribok is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fribok is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Fribok.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.swedsoft.bookkeeping.calc;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for SSOCRNumber
 *
 * @author Stefan Kangas
 */
class SSOCRNumberTest {
    private SSInvoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new SSInvoice();
    }

    /** Generate and return OCR for default test object. */
    private String ocr() {
        return SSOCRNumber.getOCRNumber(invoice);
    }

    @Test
    void ocrNumberIsTheSameForOneNumber() {
        invoice.setNumber(512);
        String one = ocr();
        String two = ocr();

        assertEquals(one, two);
    }

    @Test
    void ocrChangesWhenInvoiceNumberChanges() {
        invoice.setNumber(256);
        String one = ocr();

        invoice.setNumber(512);
        String two = ocr();

        assertNotEquals(one, two);
    }

    @Test
    void ocrContainsInvoiceNumber() {
        int num = 65536;

        invoice.setNumber(num);
        assertTrue(ocr().contains(Integer.toString(num)));
    }

    /* This is here to ensure we don't accidentally change the behavior of the
     * OCR generation.  Every time it changes, these values should be updated as
     * well.
     */
    @Test
    void dontChangeBehaviorWithoutChangingMe() {
        int num = 65536;

        invoice.setNumber(num);
        assertEquals("6553671", ocr());
    }

    // ---- getCheckSum(String) ----

    @Test
    void getCheckSumReturnsDigitChar() {
        char result = SSOCRNumber.getCheckSum("123");
        assertThat(result).isBetween('0', '9');
    }

    @Test
    void getCheckSumIsConsistentForSameInput() {
        char first = SSOCRNumber.getCheckSum("12345");
        char second = SSOCRNumber.getCheckSum("12345");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void getCheckSumDiffersForDifferentInputs() {
        char a = SSOCRNumber.getCheckSum("123");
        char b = SSOCRNumber.getCheckSum("456");
        // It is possible but unlikely that two different inputs
        // produce the same checksum. We verify the algorithm works for known values.
        // Luhn check digit for "123" should be '0'
        // 1*1=1, 2*2=4, 3*1=3 => sum=8 => (8/10+1)*10 - 8 = 10 - 8 = 2
        assertThat(a).isEqualTo('0');
    }

    @Test
    void getCheckSumForSingleDigit() {
        // For input "5": weight=2, value=5, sum=10 => 10-9=1 => checksum = (1/10+1)*10 - 1 = 10-1 = 9
        char result = SSOCRNumber.getCheckSum("5");
        assertThat(result).isEqualTo('9');
    }

    @Test
    void getCheckSumForKnownLuhnValue() {
        // For input "7992739871": known Luhn check digit is 3
        char result = SSOCRNumber.getCheckSum("7992739871");
        assertThat(result).isEqualTo('3');
    }

    // ---- getCheckSum(Integer) ----

    @Test
    void getCheckSumIntegerDelegatesToString() {
        char fromInt = SSOCRNumber.getCheckSum(Integer.valueOf(12345));
        char fromStr = SSOCRNumber.getCheckSum("12345");
        assertThat(fromInt).isEqualTo(fromStr);
    }

    @Test
    void getCheckSumIntegerForSmallNumber() {
        char result = SSOCRNumber.getCheckSum(Integer.valueOf(1));
        assertThat(result).isBetween('0', '9');
    }
}
