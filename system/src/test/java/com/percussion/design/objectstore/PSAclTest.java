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

package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class PSAclTest {

  @Test
  public void testDefaultConstructor() throws Exception {
    PSAcl acl = new PSAcl();
    PSCollection entries = acl.getEntries();
    assertNotNull(entries, "Entries not null");
    assertEquals(0, entries.size(), "Entries empty");
    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    PSAcl otherAcl = new PSAcl();
    assertEquals(acl, otherAcl, "Two empty acl's are equal");
    assertEquals(otherAcl, acl);
  }

  @Test
  public void testGetSetMultiMemberAccess() throws Exception {
    PSAcl acl = new PSAcl();
    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    acl.setAccessForMultiMembershipMaximum();
    assertTrue(acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    acl.setAccessForMultiMembershipMergedMaximum();
    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    acl.setAccessForMultiMembershipMinimum();
    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    acl.setAccessForMultiMembershipMergedMinimum();
    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(acl.isAccessForMultiMembershipMergedMinimum());
  }

  @Test
  public void testXml() throws Exception {
    PSAcl acl = new PSAcl();
    PSAcl otherAcl = new PSAcl();
    assertEquals(acl, otherAcl);

    IPSDocument doc = new DocumentContainer();
    doc.toXml().appendChild(acl.toXml(doc.toXml()));
    otherAcl.fromXml(doc.toXml().getDocumentElement(), doc, null);
    assertEquals(acl, otherAcl);

    // block 1
    acl.setAccessForMultiMembershipMaximum();
    assertFalse(acl.equals(otherAcl));
    doc = new DocumentContainer();
    doc.toXml().appendChild(acl.toXml(doc.toXml()));
    otherAcl.fromXml(doc.toXml().getDocumentElement(), doc, null);
    assertTrue(acl.equals(otherAcl));

    assertTrue(acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    assertTrue(otherAcl.isAccessForMultiMembershipMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMinimum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMinimum());

    // block 2
    acl.setAccessForMultiMembershipMergedMaximum();
    assertFalse(acl.equals(otherAcl));
    doc = new DocumentContainer();
    doc.toXml().appendChild(acl.toXml(doc.toXml()));
    otherAcl.fromXml(doc.toXml().getDocumentElement(), doc, null);
    assertTrue(acl.equals(otherAcl));

    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    assertTrue(!otherAcl.isAccessForMultiMembershipMaximum());
    assertTrue(otherAcl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMinimum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMinimum());

    // block 3
    acl.setAccessForMultiMembershipMinimum();
    assertFalse(acl.equals(otherAcl));
    doc = new DocumentContainer();
    doc.toXml().appendChild(acl.toXml(doc.toXml()));
    otherAcl.fromXml(doc.toXml().getDocumentElement(), doc, null);
    assertTrue(acl.equals(otherAcl));

    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(acl.isAccessForMultiMembershipMinimum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMinimum());

    assertTrue(!otherAcl.isAccessForMultiMembershipMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(otherAcl.isAccessForMultiMembershipMinimum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMinimum());

    // block 4
    acl.setAccessForMultiMembershipMergedMinimum();
    assertFalse(acl.equals(otherAcl));
    doc = new DocumentContainer();
    doc.toXml().appendChild(acl.toXml(doc.toXml()));
    otherAcl.fromXml(doc.toXml().getDocumentElement(), doc, null);
    assertTrue(acl.equals(otherAcl));

    assertTrue(!acl.isAccessForMultiMembershipMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!acl.isAccessForMultiMembershipMinimum());
    assertTrue(acl.isAccessForMultiMembershipMergedMinimum());

    assertTrue(!otherAcl.isAccessForMultiMembershipMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMergedMaximum());
    assertTrue(!otherAcl.isAccessForMultiMembershipMinimum());
    assertTrue(otherAcl.isAccessForMultiMembershipMergedMinimum());
  }

  @Test
  public void testSetEntriesNull() throws Exception {
    boolean didThrow = false;
    PSAcl acl = new PSAcl();
    try {
      acl.setEntries(null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  @Test
  public void testSetEntriesWrongType() throws Exception {
    boolean didThrow = false;
    PSAcl acl = new PSAcl();
    try {
      acl.setEntries(new PSCollection(this.getClass().getName()));
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  @Test
  public void testSetEntriesWithDuplicates() throws Exception {
    boolean didThrow = false;
    PSAcl acl = new PSAcl();
    PSCollection entries = new PSCollection(com.percussion.design.objectstore.PSAclEntry.class);
    entries.add(new PSAclEntry());
    entries.add(new PSAclEntry());
    try {
      acl.setEntries(entries);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow, "Should throw when we have duplicate ACL entries");
  }

  class DocumentContainer implements IPSDocument {
    public DocumentContainer() {
      m_doc = PSXmlDocumentBuilder.createXmlDocument();
    }

    /**
     * This method is called to create an XML document with the appropriate format for the given
     * object.
     *
     * @return the newly created XML document
     */
    public Document toXml() {
      return m_doc;
    }

    /**
     * This method is called to populate an object from an XML document.
     *
     * @exception PSUnknownDocTypeException if the XML document does not represent a type supported
     *     by the class.
     */
    public void fromXml(Document sourceDoc)
        throws PSUnknownDocTypeException, PSUnknownNodeTypeException {
      m_doc = sourceDoc;
    }

    private Document m_doc;
  }
}
