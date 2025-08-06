// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.assetmanagement.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Basic tests for the CSV Row base class.
 * @author natechadwick
 */
class PSAbstractBaseCSVReportRowTest {

    private PSTestCSVReportRow row = null;

    @BeforeEach
    void setUp() {
        row = new PSTestCSVReportRow();
        row.col1 = "A1";
        row.col2 = "A2";
        row.col3multiline = "A3a\r\nA3b\r\nA3c";
        row.col4empty = "";
    }

    @Test
    void testToCSVRow() {
        var test = row.toCSVRow();
        assertEquals("\"A1\",\"A2\",\"A3a\r\nA3b\r\nA3c\",\"\"\r\n", test, "Values should match");
    }

    @Test
    void testDelimitValue() {
        var test = row.delimitValue("myval");
        assertEquals("\"myval\"", test, "Values should match");
    }

    @Test
    void testCSVEscapeString() {
        var test = row.csvEscapeString("The world is a \"vampire\"");
        assertTrue(!test.contains("\""), "String shouldn't have any quotes");
    }
}
