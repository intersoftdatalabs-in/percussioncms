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
package com.percussion.pagemanagement.resource.data;

import static java.util.Arrays.asList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFolderResource;
import com.percussion.share.dao.PSSerializerUtils;

public class PSResourcesSerializationTest {

    @Test
    public void testXmlResources() throws Exception {
        var rs = new PSResourceDefinitionGroup();
        var fr = new PSFileResource();
        var ar = new PSAssetResource();
        var f = new PSFolderResource();
        ar.setId("ben");
        ar.setLegacyTemplate("percBinary");
        ar.setContentType("percBinary");
        fr.setId("adam");
        fr.setType(PSResourceDefinitionGroup.PSFileResource.PSFileResourceType.css);
        fr.setFile("/my/file.css");
        f.setId("peter");
        f.setPath("/Stuff/My");
        rs.setFileResources(asList(fr));
        rs.setAssetResources(asList(ar));
        rs.setFolderResources(asList(f));
        var actual = PSSerializerUtils.marshal(rs);
        log.debug(actual);
    }

    /**
     * The log instance to use for this class, never null.
     */
    private static final Logger log = LogManager.getLogger(PSResourcesSerializationTest.class);
}
