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
 * Fribok distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Fribok.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.swedsoft.bookkeeping.calc.util;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for SSAutoIncrement
 *
 * @author Stefan Kangas
 */
class SSAutoIncrementTest {

    private SSAutoIncrement ainc;
    private String key;
    private int value;

    @BeforeEach
    void setUp() {
        ainc = new SSAutoIncrement();
        key = "KEY";
        value = 3141593;
    }

    @Test
    void getNumberIsZero() {
        assertEquals(0, ainc.getNumber(key));
    }

    @Test
    void incrementAndGet() {
        ainc.doAutoIncrement(key);
        assertEquals(1, ainc.getNumber(key));
    }

    @Test
    void setAndGet() {
        ainc.setNumber(key, value);
        assertEquals(value, ainc.getNumber(key));
    }

    @Test
    void setIncrementAndGet() {
        ainc.setNumber(key, value);
        ainc.doAutoIncrement(key);
        assertEquals(value + 1, ainc.getNumber(key));
    }

    @Test
    void incrementSetAndGet() {
        ainc.doAutoIncrement(key);
        ainc.setNumber(key, value);
        assertEquals(value, ainc.getNumber(key));
    }

    @Test
    void setAndGetThreeDifferent() {
        ainc.setNumber(key + "a", value + 1);
        ainc.setNumber(key + "b", value + 2);
        ainc.setNumber(key + "c", value + 3);
        assertEquals(value + 1, ainc.getNumber(key + "a"));
        assertEquals(value + 2, ainc.getNumber(key + "b"));
        assertEquals(value + 3, ainc.getNumber(key + "c"));
    }

    @Test
    void stringValueMatchesWhatWePutIn() {
        ainc.setNumber(key + "a", value + 1);
        ainc.setNumber(key + "b", value + 2);
        ainc.setNumber(key + "c", value + 3);
        String valueString = String.valueOf(value);
        Pattern pattern = Pattern.compile(
                key + "[a-c]" + " " + valueString.substring(0, valueString.length() - 1));

        String result = ainc.toString();
        Matcher matcher = pattern.matcher(result);

        assertTrue(matcher.find());
        assertTrue(matcher.find());
        assertTrue(matcher.find());
    }

    @Test
    void negativeNumberIsStoredAndRetrieved() {
        ainc.setNumber(key, -5);
        assertEquals(-5, ainc.getNumber(key));
    }

    @Test
    void negativeNumberIncrementsTowardZero() {
        ainc.setNumber(key, -1);
        ainc.doAutoIncrement(key);
        assertEquals(0, ainc.getNumber(key));
    }

    @Test
    void negativeNumberIncrementsThroughZero() {
        ainc.setNumber(key, -2);
        ainc.doAutoIncrement(key);
        ainc.doAutoIncrement(key);
        ainc.doAutoIncrement(key);
        assertEquals(1, ainc.getNumber(key));
    }
}
