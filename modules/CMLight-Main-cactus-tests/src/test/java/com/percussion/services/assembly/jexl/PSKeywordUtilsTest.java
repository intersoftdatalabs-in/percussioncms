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
package com.percussion.services.assembly.jexl;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test keyword utilities.
 *
 * @author dougrand
 */
@Tag("IntegrationTest")
public class PSKeywordUtilsTest {

    @Test
    public void testRetrieval() {
        var ku = new PSKeywordUtils();
        var choices = ku.keywordChoices("Publishable");
        assertNotNull(choices);
        assertEquals(4, choices.size());
    }

    @Test
    public void testChoices() {
        var ku = new PSKeywordUtils();
        var compare = "<OPTION value='n'>Unpublish</OPTION>\n"
                + "<OPTION value='y'>Publish</OPTION>\n"
                + "<OPTION value='i'>Ignore</OPTION>\n"
                + "<OPTION value='u' selected='true'>Archive</OPTION>\n";
        var result = ku.keywordSelectChoices("Publishable", "u");
        assertEquals(compare, result);
    }

    @Test
    public void testLabel() {
        var ku = new PSKeywordUtils();
        assertEquals("Archive", ku.getLabel("Publishable", "u"));
        assertEquals("", ku.getLabel("Publishable", "z"));
    }

    @Test
    public void testLocaleLabel() {
        var ku = new PSKeywordUtils();
        assertEquals("Archive", ku.getLabel("Publishable", "u", "en_us"));
        assertEquals("", ku.getLabel("Publishable", "z", "en_us"));
    }

    @Test
    public void testFieldChoices() {
        var ku = new PSKeywordUtils();
        var ctn = "rffGeneric";
        assertEquals("Normal", ku.getChoiceLabel(ctn, "usage", "N"));
        assertEquals("Landing Page", ku.getChoiceLabel(ctn, "usage", "L"));
    }
}
