/*
 * Copyright 1999-2026 Percussion Software, Inc. and Intersoft Data Labs, Inc.
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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for {@link PSFolderAcl} conversion from {@link PSObjectAcl} (#3077).
 *
 * <p>Regression: {@code new PSFolderAcl(objectAcl.toXml(...), contentId, communityId)} used to fail
 * with expected {@code PSXFolderAcl} vs found {@code PSXObjectAcl} because Element construction
 * derived the root name from the subclass while the wire format remains {@code PSXObjectAcl}.
 */
public class PSFolderAclConversionTest {

  private static PSObjectAcl sampleObjectAcl() {
    PSObjectAcl acl = new PSObjectAcl();
    acl.add(
        new PSObjectAclEntry(
            PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL,
            PSObjectAclEntry.ACL_ENTRY_EVERYONE,
            PSObjectAclEntry.ACCESS_WRITE));
    acl.add(
        new PSObjectAclEntry(
            PSObjectAclEntry.ACL_ENTRY_TYPE_USER, "admin1", PSObjectAclEntry.ACCESS_ADMIN));
    return acl;
  }

  @Test
  public void fromObjectAclCopiesEntriesAndIdsWithoutXml() {
    PSObjectAcl source = sampleObjectAcl();
    int contentId = 301;
    int communityId = 10;

    PSFolderAcl folderAcl = new PSFolderAcl(source, contentId, communityId);

    assertEquals(contentId, folderAcl.getContentId());
    assertEquals(communityId, folderAcl.getCommunityId());
    assertEquals(2, folderAcl.size());
    assertNotNull(
        folderAcl.getAclEntry(
            PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL));
    assertEquals(
        PSObjectAclEntry.ACCESS_WRITE,
        folderAcl
            .getAclEntry(
                PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL)
            .getPermissions());
    assertNotNull(folderAcl.getAclEntry("admin1", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));

    // Cloned entries: mutating the source must not change the folder ACL
    PSObjectAclEntry sourceEveryone =
        source.getAclEntry(
            PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL);
    sourceEveryone.setPermissions(PSObjectAclEntry.ACCESS_READ);
    assertEquals(
        PSObjectAclEntry.ACCESS_WRITE,
        folderAcl
            .getAclEntry(
                PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL)
            .getPermissions());
    assertNotSame(
        sourceEveryone,
        folderAcl.getAclEntry(
            PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL));
  }

  @Test
  public void fromObjectAclRejectsNullSource() {
    assertThrows(IllegalArgumentException.class, () -> new PSFolderAcl((PSObjectAcl) null, 1, 1));
  }

  @Test
  public void elementCtorAcceptsPsObjectAclToXmlOutput() throws Exception {
    PSObjectAcl source = sampleObjectAcl();
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = source.toXml(doc);

    assertEquals(PSObjectAcl.XML_NODE_NAME, xml.getNodeName());

    // Historic PSFolderEntry.updateFolder path — must not throw wrong-type on PSXObjectAcl
    PSFolderAcl fromXml = new PSFolderAcl(xml, 501, -1);

    assertEquals(501, fromXml.getContentId());
    assertEquals(-1, fromXml.getCommunityId());
    assertEquals(2, fromXml.size());
    assertNotNull(fromXml.getAclEntry("admin1", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
    assertNotNull(
        fromXml.getAclEntry(
            PSObjectAclEntry.ACL_ENTRY_EVERYONE, PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL));
  }

  @Test
  public void folderAclXmlRoundTripKeepsPsObjectAclRootAndIds() throws Exception {
    PSFolderAcl original = new PSFolderAcl(sampleObjectAcl(), 42, 7);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);

    assertEquals(PSObjectAcl.XML_NODE_NAME, xml.getNodeName());
    assertEquals("42", xml.getAttribute("contentId"));
    assertEquals("7", xml.getAttribute("communityId"));

    PSFolderAcl restored = new PSFolderAcl(xml);
    assertEquals(42, restored.getContentId());
    assertEquals(7, restored.getCommunityId());
    assertEquals(2, restored.size());
    assertTrue(original.equals(restored));
  }

  @Test
  public void emptyObjectAclConversionYieldsEmptyFolderAcl() {
    PSFolderAcl folderAcl = new PSFolderAcl(new PSObjectAcl(), 9, 3);
    assertEquals(9, folderAcl.getContentId());
    assertEquals(3, folderAcl.getCommunityId());
    assertEquals(0, folderAcl.size());
    assertNull(folderAcl.getAclEntry("anyone", PSObjectAclEntry.ACL_ENTRY_TYPE_USER));
  }
}
