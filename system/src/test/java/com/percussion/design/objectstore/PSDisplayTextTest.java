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

import com.percussion.utils.testing.IPSReflectionFilter;
import com.percussion.utils.testing.PSReflectionHelper;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Test case
public class PSDisplayTextTest {
  @Test
  public void testEquals() throws Exception {
    PSDisplayText AAA = new PSDisplayText("AAA");
    PSDisplayText AAA_2 = new PSDisplayText("AAA");
    PSDisplayText aaa = new PSDisplayText("aaa");
    PSDisplayText BBB = new PSDisplayText("BBB");

    assertEquals(AAA, AAA_2);
    assertFalse(AAA.equals(aaa));
    assertFalse(AAA.equals(BBB));

    PSReflectionHelper.testEquals(
        AAA,
        AAA_2,
        new IPSReflectionFilter() {
          public boolean acceptMethod(String methodname) {
            return !methodname.contains("Id");
          }
        });
  }

  @Test
  public void testXml() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = PSXmlDocumentBuilder.createRoot(doc, "Test");

    // create test object
    PSDisplayText testTo = new PSDisplayText("text");
    Element elem = testTo.toXml(doc);
    root.appendChild(elem);

    // create a new object and populate it from our testTo element
    PSDisplayText testFrom = new PSDisplayText(elem, null, null);
    assertEquals(testTo, testFrom);
  }
}
