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

import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PSAclEntryTest {

  @BeforeEach
  public void setUp() {}

  @Test
  public void testEmptyEquals() throws Exception {
    PSAclEntry entry = new PSAclEntry();
    PSAclEntry otherEntry = new PSAclEntry();
    assertEquals(entry, otherEntry);
  }

  @Test
  public void testNameTypeConstructor() throws Exception {
    PSAclEntry entry = new PSAclEntry("foo", PSAclEntry.ACE_TYPE_USER);

    assertEquals(entry.getName(), "foo");
    assertTrue(entry.isUser());

    PSAclEntry otherEntry = new PSAclEntry("foo", PSAclEntry.ACE_TYPE_USER);

    assertEquals(entry, otherEntry);

    boolean didThrow = false;
    try {
      entry = new PSAclEntry(null, PSAclEntry.ACE_TYPE_USER);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      entry = new PSAclEntry("", PSAclEntry.ACE_TYPE_USER);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      String name = "0123456789";
      for (int i = 0; i < 100; i++) {
        name += "0123456789";
      }
      entry = new PSAclEntry(name, PSAclEntry.ACE_TYPE_USER);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);
  }

  @Test
  public void testGetSetName() throws Exception {
    PSAclEntry entry = new PSAclEntry();
    boolean didThrow = false;
    try {
      entry.setName(null);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      entry.setName("");
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    didThrow = false;
    try {
      String name = "0123456789";
      for (int i = 0; i < 100; i++) {
        name += "0123456789";
      }
      entry.setName(name);
    } catch (IllegalArgumentException e) {
      didThrow = true;
    }
    assertTrue(didThrow);

    entry.setName("foobar");
    assertEquals(entry.getName(), "foobar");
  }

  @Test
  public void testXml() throws Exception {
    PSAclEntry entry = new PSAclEntry();
    PSAclEntry otherEntry = new PSAclEntry();
    assertEquals(entry, otherEntry);

    // block 1
    entry.setAccessLevel(PSAclEntry.SACE_ACCESS_DATA);
    entry.setName("foobar");
    assertFalse(entry.equals(otherEntry));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = entry.toXml(doc);
    doc.appendChild(el);

    otherEntry.fromXml(el, null, null);
    assertEquals(entry, otherEntry);

    // block 2
    entry.setAccessLevel(PSAclEntry.AACE_DATA_QUERY);
    entry.setName("taebo");
    assertFalse(entry.equals(otherEntry));

    doc = PSXmlDocumentBuilder.createXmlDocument();
    el = entry.toXml(doc);
    doc.appendChild(el);

    otherEntry.fromXml(el, null, null);
    assertEquals(entry, otherEntry);
  }
}
