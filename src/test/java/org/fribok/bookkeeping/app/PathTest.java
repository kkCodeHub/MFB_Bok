/*
 * Copyright © 2010 Stefan Kangas <skangas@skangas.se>
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
package org.fribok.bookkeeping.app;


import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for Path
 *
 * @author Stefan Kangas
 */
public class PathTest {
    @Test
    void valuesNotNull() {
        for (Path path : Path.values()) {
            assertNotNull(Path.get(path));
        }
    }

    @Test
    void userDataFolderIsMfbBok() {
        assertEquals("MFB_Bok", Path.get(Path.USER_DATA).getName());
    }

    @Test
    void appDataIsDataFolderUnderUserData() {
        File expected = new File(Path.get(Path.USER_DATA), "data");
        assertEquals(expected.getAbsolutePath(), Path.get(Path.APP_DATA).getAbsolutePath());
        assertTrue(Path.get(Path.APP_DATA).getAbsolutePath().endsWith(File.separator + "data"));
    }
}
