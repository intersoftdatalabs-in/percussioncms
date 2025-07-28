// REFACTORED: CP-JAVA11

/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.server.PSDependencyManager;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.content.IPSContentService;
import com.percussion.services.content.PSContentServiceLocator;
import com.percussion.services.content.data.PSKeyword;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link PSKeywordDependencyHandler}.
 */
@Tag("IntegrationTest")
public class PSKeywordDependencyHandlerTest {

    /**
     * Test the handler.
     *
     * @throws Exception if the test fails
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testHandler() throws Exception {
        var contentSvc = PSContentServiceLocator.getContentService();
        var keywords = contentSvc.findKeywordsByLabel(null, null);
        assertTrue(keywords.size() > 0);

        // test does dependency exist
        var keyword = keywords.get(0);

        var hdlr =
                PSDependencyManager.getInstance().getDependencyHandler(
                        PSKeywordDependencyHandler.DEPENDENCY_TYPE);

        var tok = new PSSecurityToken("test");
        assertTrue(hdlr.doesDependencyExist(tok, keyword.getValue()));
        assertFalse(hdlr.doesDependencyExist(tok, "9999"));

        // test get dependency, dependencies
        Set<PSDependency> keywordDeps = new HashSet<>();
        for (var k : keywords) {
            var dep = hdlr.getDependency(tok, k.getValue());
            assertNotNull(dep);
            keywordDeps.add(dep);
        }

        Iterator<?> depIter = hdlr.getDependencies(tok);
        int i = 0;
        while (depIter.hasNext()) {
            assertTrue(keywordDeps.contains(depIter.next()));
            i++;
        }

        assertEquals(keywordDeps.size(), i);
    }
}
