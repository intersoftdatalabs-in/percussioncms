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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.transformation.impl.PSTransformerFactory;
import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the {@link PSAutoTranslationConverter}.
 */
@Tag(IntegrationTest.class)
public class PSAutoTranslationConverterTest extends PSConverterTestBase {

    /**
     * Test the converter.
     */
    public void testConverter() throws Exception {
        var src = new PSAutoTranslation();
        src.setLocale("locale2");
        var tgt = (PSAutoTranslation) roundTripConversion(
                PSAutoTranslation.class,
                com.percussion.webservices.content.PSAutoTranslation.class, src);
        assertEquals(src, tgt);

        src.setCommunityId(1232);
        src.setCommunityName("comm2");
        src.setContentTypeId(4562);
        src.setContentTypeName("ctype2");
        src.setWorkflowId(7892);
        src.setWorkflowName("wf2");
        var srcList = new ArrayList<PSAutoTranslation>();
        srcList.add(src);
        srcList.add(tgt);

        var tgtList = roundTripListConversion(
                com.percussion.webservices.content.PSAutoTranslation[].class,
                srcList);
        assertEquals(srcList, tgtList);
    }

    /**
     * Negative values can't be tested with roundtrip because they have to contain
     * host information and this information is stripped during conversion.
     */
    public void testNegativeIdsConversion() {
        final long communityId = 5;
        final long contentTypeId = 6;
        final long workflowId = 7;

        var src = new com.percussion.webservices.content.PSAutoTranslation();
        src.setLocale("locale1");
        src.setCommunityId(getGuid(PSTypeEnum.COMMUNITY_DEF, communityId));
        src.setContentTypeId(getGuid(PSTypeEnum.NODEDEF, contentTypeId));
        src.setWorkflowId(getGuid(PSTypeEnum.WORKFLOW, workflowId));

        Converter converter = PSTransformerFactory.getInstance().getConverter(
                com.percussion.webservices.content.PSAutoTranslation.class);
        var dst = (PSAutoTranslation) converter.convert(PSAutoTranslation.class, src);
        assertEquals(communityId, dst.getCommunityId());
        assertEquals(contentTypeId, dst.getContentTypeId());
        assertEquals(workflowId, dst.getWorkflowId());
    }

    /**
     * Generates a negative GUID for the provided type and id.
     */
    private long getGuid(PSTypeEnum type, long id) {
        final long host = 92642436432L;
        var guid = new PSGuid(host, type, id).longValue();
        assertTrue(guid < 0);
        return guid;
    }
}
