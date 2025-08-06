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
package com.percussion.security;

import com.percussion.design.objectstore.PSConditional;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSBackEndDirectoryCataloger}.
 */
@Tag("IntegrationTest")
public class PSBackEndDirectoryCatalogerTest {

    /**
     * Test the cataloger.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testCataloger() throws Exception {
        var cat = new PSBackEndDirectoryCataloger();

        Collection<String> attrs = new ArrayList<>(1);
        attrs.add("sys_defaultcommunity");

        PSConditional cond;
        Collection<?> subs;
        // test find user with user name
        cond = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin1"));
        subs = cat.findUsers(new PSConditional[]{cond}, attrs);
        assertEquals(1, subs.size());

        // test find user with like
        cond = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_LIKE, new PSTextLiteral("admin%"));
        subs = cat.findUsers(new PSConditional[]{cond}, attrs);
        assertEquals(2, subs.size());

        // test find user with equals and bad criteria
        cond = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin3"));
        subs = cat.findUsers(new PSConditional[]{cond}, attrs);
        assertTrue(subs.isEmpty());

        // test find user with like and no attributes
        cond = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_LIKE, new PSTextLiteral("admin%"));
        subs = cat.findUsers(new PSConditional[]{cond}, null);
        assertTrue(subs.size() > 1);

        // test find user with bad criteria and no attributes
        cond = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin3"));
        subs = cat.findUsers(new PSConditional[]{cond}, null);
        assertTrue(subs.isEmpty());

        // test multiple
        PSConditional[] conds = new PSConditional[4];
        conds[0] = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin1"));
        conds[1] = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin2"));
        conds[2] = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_LIKE, new PSTextLiteral("editor%"));
        conds[3] = new PSConditional(new PSTextLiteral(cat.getObjectAttributeName()),
                PSConditional.OPTYPE_EQUALS, new PSTextLiteral("admin3"));

        subs = cat.findUsers(conds, attrs);
        assertEquals(4, subs.size());
    }
}
