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

import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for child extractor.
 *
 * @author dougrand
 */
@Tag("IntegrationTest")
public class PSChildExtractionTest {

    /**
     * Test assumes that you've set up a specific test case. Create a
     * multivalued child of Generic called Author with fields "firstName"
     * and "lastName". Create children for
     * the given Generic object. The following simply tests that we get
     * back values, hand inspect to ensure that the correct values are
     * present.
     *
     * The given content id below is for the content item found in:
     * //Sites/EnterpriseInvestments/ProductsAndServices/Funds/EI Resources Fund
     */
    @Test
    public void testChildExtractor() throws Exception {
        IPSContentMgr cmgr = PSContentMgrLocator.getContentMgr();
        var asmutils = new PSAssemblerUtils();

        List<IPSGuid> guids = new ArrayList<>();
        guids.add(new PSLegacyGuid(397, 4));

        List<Node> nodes = cmgr.findItemsByGUID(guids, null);

        Node anode = nodes.get(0);

        List<Object> fnvals = asmutils.childValues(anode, "Author", "rx:firstName");
        List<Object> lnvals = asmutils.childValues(anode, "Author", "rx:lastName");

        assertEquals(fnvals.size(), lnvals.size());
        assertTrue(fnvals.size() > 0);
    }
}
