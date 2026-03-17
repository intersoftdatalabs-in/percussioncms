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
import org.w3c.dom.Document;

/**
 * Tests the XML serialisation functionality for <code>PSMacroDefinitionSet</code> and <code>
 * PSMacroDefinition</code> objectstore classes.
 */
public class PSMacroDefinitionSetTest {
  /**
   * Construct a new test for the supplied name.
   *
   * @param name the test name, assumed not <code>null</code> or empty.
   */

  /**
   * Test XML serialization for code>PSMacroSet</code> and <code>PSMacro</code>.
   *
   * @throws Exception for any error.
   */
  public void testXml() throws Exception {
    PSMacroDefinition macro1 = new PSMacroDefinition("$macro1", PSMacroDefinition.class.getName());
    macro1.setDescription("description 1");

    PSMacroDefinition macro2 =
        new PSMacroDefinition("$macro2", PSMacroDefinitionSet.class.getName());

    PSMacroDefinitionSet macros1 = new PSMacroDefinitionSet();
    macros1.add(macro1);
    macros1.add(macro2);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    System.out.println(
        "macroDefinitionSet 1:\n" + PSXmlDocumentBuilder.toString(macros1.toXml(doc)));

    PSMacroDefinitionSet macros2 = new PSMacroDefinitionSet(macros1.toXml(doc));
    System.out.println(
        "macroDefinitionSet 2:\n" + PSXmlDocumentBuilder.toString(macros2.toXml(doc)));

    assertEquals(macros1, macros2);
  }

  /**
   * The test suit that specifies all axecuted tests.
   *
   * @return the test suit, never <code>null</code>.
   */
}
