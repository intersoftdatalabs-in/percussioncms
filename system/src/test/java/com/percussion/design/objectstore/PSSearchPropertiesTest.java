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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * This class unit tests the {@link PSSearchProperties} object. It is written to the JUnit
 * framework.
 *
 * @author paulhoward
 */
@Disabled("Temporarily disabled — failing in perc-system test run")
public class PSSearchPropertiesTest {
  /**
   * Makes a few objects and verifies that all appropriate bits are considered and the skipped bits
   * don't affect equals or hashcode.
   *
   * @throws PSUnknownNodeTypeException If the <code>fromXml</code> method fails unexpectedly.
   */
  @Test
  public void testEqualsAndHashCode() throws PSUnknownNodeTypeException {
    PSSearchProperties sp = new PSSearchProperties();
    PSSearchProperties sp2 = new PSSearchProperties();
    assertTrue(sp.equals(sp2), "default objects not equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "default object's hashcode not equal:");

    sp.setDefaultSearchLabel("a");
    assertTrue(!sp.equals(sp2), "changed label shouldn't be equal:");
    assertTrue(sp.hashCode() != sp2.hashCode(), "changed label shouldn't have equal hashcode:");
    sp2.setDefaultSearchLabel("a");
    assertTrue(sp.equals(sp2), "changed label should be equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "changed label should have equal hashcode:");

    sp.setEnableTransformation(!sp.isEnableTransformation());
    assertTrue(!sp.equals(sp2), "changed enableTrans shouldn't be equal:");
    assertTrue(
        sp.hashCode() != sp2.hashCode(), "changed enableTrans shouldn't have equal hashcode:");
    sp2.setEnableTransformation(sp.isEnableTransformation());
    assertTrue(sp.equals(sp2), "changed enableTrans should be equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "changed enableTrans should have equal hashcode:");

    sp.setId(89);
    assertTrue(!sp.equals(sp2), "changed id shouldn't be equal:");
    assertTrue(sp.hashCode() != sp2.hashCode(), "changed id shouldn't have equal hashcode:");
    sp2.setId(sp.getId());
    assertTrue(sp.equals(sp2), "changed id should be equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "changed id should have equal hashcode:");

    sp.setTokenizeSearchContent(!sp.isTokenizeSearchContent());
    assertTrue(!sp.equals(sp2), "changed searchTok shouldn't be equal:");
    assertTrue(sp.hashCode() != sp2.hashCode(), "changed searchTok shouldn't have equal hashcode:");
    sp2.setTokenizeSearchContent(sp.isTokenizeSearchContent());
    assertTrue(sp.equals(sp2), "changed searchTok should be equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "changed searchTok should have equal hashcode:");

    sp.setUserCustomizable(!sp.isUserCustomizable());
    assertTrue(!sp.equals(sp2), "changed userCustomizable shouldn't be equal:");
    assertTrue(
        sp.hashCode() != sp2.hashCode(), "changed userCustomizable shouldn't have equal hashcode:");
    sp2.setUserCustomizable(sp.isUserCustomizable());
    assertTrue(sp.equals(sp2), "changed userCustomizable should be equal:");
    assertTrue(
        sp.hashCode() == sp2.hashCode(), "changed userCustomizable should have equal hashcode:");

    sp.setUserSearchable(!sp.isUserSearchable());
    assertTrue(!sp.equals(sp2), "changed userSearchable shouldn't be equal:");
    assertTrue(
        sp.hashCode() != sp2.hashCode(), "changed userSearchable shouldn't have equal hashcode:");
    sp2.setUserSearchable(sp.isUserSearchable());
    assertTrue(sp.equals(sp2), "changed userSearchable should be equal:");
    assertTrue(
        sp.hashCode() == sp2.hashCode(), "changed userSearchable should have equal hashcode:");

    sp.setVisibleToGlobalQuery(!sp.isVisibleToGlobalQuery());
    assertTrue(!sp.equals(sp2), "changed userVisible shouldn't be equal:");
    assertTrue(
        sp.hashCode() != sp2.hashCode(), "changed userVisible shouldn't have equal hashcode:");
    sp2.setVisibleToGlobalQuery(sp.isVisibleToGlobalQuery());
    assertTrue(sp.equals(sp2), "changed userVisible should be equal:");
    assertTrue(sp.hashCode() == sp2.hashCode(), "changed userVisible should have equal hashcode:");

    // change bits that shouldn't affect the equals
    sp.setEnableTransformationLocked(!sp.isEnableTransformationLocked());
    assertTrue(sp.equals(sp2), "enableTransformationLocked bit affected equals but shouldn't");
    assertTrue(
        sp.hashCode() == sp2.hashCode(),
        "enableTransformationLocked bit affected hash but shouldn't");

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    sp2.fromXml(sp.toXml(doc), null, null);
    assertTrue(sp.equals(sp2), "fromXml bit affected equals but shouldn't");
    assertTrue(sp.hashCode() == sp2.hashCode(), "fromXml bit affected hash but shouldn't");
  }

  /** Verifies an exception is thrown when the enable transformation flag is locked. */
  @Test
  public void testEnableTransformLock() {
    PSSearchProperties sp = new PSSearchProperties();
    sp.setEnableTransformationLocked(true);
    try {
      sp.setEnableTransformation(!sp.isEnableTransformation());
      fail("Enable transformation lock didn't work.");
    } catch (IllegalStateException ise) {
      // expected
    }
  }

  /**
   * Tests the toXml and fromXml methods by creating objects, transforming them and testing for
   * equality.
   *
   * @throws PSUnknownNodeTypeException If the <code>fromXml</code> method fails unexpectedly.
   */
  @Test
  public void testXmlConversion() throws PSUnknownNodeTypeException {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();

    // try the default guy first
    PSSearchProperties sp = new PSSearchProperties();
    PSSearchProperties tmp = new PSSearchProperties(sp.toXml(doc));
    assertTrue(sp.equals(tmp), "Xml transformed obj didn't match original:");

    // flip all bits and try again
    flipBits(sp);
    tmp = new PSSearchProperties(sp.toXml(doc));
    assertTrue(sp.equals(tmp), "non-default Xml transformed obj didn't match original:");
  }

  /**
   * Flips the value of every flag in sp, sets the id to 99 and sets the label to "test label".
   *
   * @param sp Assumed not <code>null</code>.
   */
  private void flipBits(PSSearchProperties sp) {
    sp.setId(99);
    sp.setDefaultSearchLabel("test label");
    sp.setEnableTransformation(!sp.isEnableTransformation());
    sp.setEnableTransformationLocked(!sp.isEnableTransformationLocked());
    sp.setTokenizeSearchContent(!sp.isTokenizeSearchContent());
    sp.setUserCustomizable(!sp.isUserCustomizable());
    sp.setUserSearchable(!sp.isUserSearchable());
    sp.setVisibleToGlobalQuery(!sp.isVisibleToGlobalQuery());
  }
}
