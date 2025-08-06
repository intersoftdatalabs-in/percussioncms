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

import com.percussion.cms.objectstore.PSAction;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.cms.objectstore.PSObjectPermissions;

import com.percussion.webservices.transformation.PSTransformationException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Tag;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSActionConverter} class.
 */
@Tag("IntegrationTest")
public class PSFolderConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object and vice versa.
     */
    public void testConversion() throws Exception {
        // Test with simple folder
        var folder = createFolder(10, "folder1", -1, 100);
        roundTripConversionAssert(folder);

        folder = createFolder(11, "folder2", 2, 101);
        roundTripConversionAssert(folder);
    }

    /**
     * Test a list of server object convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSFolder>();
        srcList.add(createFolder(11, "folder1", -1, 100));
        srcList.add(createFolder(12, "folder2", 1, 101));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.content.PSFolder[].class, srcList);

        assertEquals(srcList, srcList2);
    }

    private PSFolder createFolder(int id, String name, int communityId, int displayFormatId) {
        var f = new PSFolder(name, id, communityId,
                PSObjectPermissions.ACCESS_ADMIN, name);

        f.setFolderPath("//Folders/Test/" + name);
        f.setDisplayFormatId(displayFormatId);
        f.setDisplayFormatName(name + " display format");
        if (communityId != -1)
            f.setCommunityName(name + " community");

        var tgtAcls = new PSObjectAcl();
        var aclEntry = new PSObjectAclEntry(PSObjectAclEntry.ACL_ENTRY_TYPE_USER,
                "User " + name, PSObjectAclEntry.ACCESS_READ);
        tgtAcls.add(aclEntry);
        aclEntry = new PSObjectAclEntry(PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE,
                "Admin " + name, PSObjectAclEntry.ACCESS_ADMIN);
        tgtAcls.add(aclEntry);
        f.setAcl(tgtAcls);

        return f;
    }

    private void roundTripConversionAssert(PSFolder source) throws PSTransformationException {
        var target = (PSFolder) roundTripConversion(PSFolder.class,
                com.percussion.webservices.content.PSFolder.class, source);

        Document doc = PSXmlDocumentBuilder.createXmlDocument();
        // Uncomment for debugging:
        // System.out.println(PSXmlDocumentBuilder.toString(source.toXml(doc)));
        // System.out.println(PSXmlDocumentBuilder.toString(target.toXml(doc)));

        // Verify the round-trip object is equal to the source object
        assertTrue(source.equalsFull(target));
    }
}
