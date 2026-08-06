/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.percussion.design.objectstore.PSAttributeList;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Regression: empty Attribute shells from outer-join subject catalogs must not fail findUsers
 * (PSRoleMgr log spam: Attribute @name null).
 */
public class PSBackendCatalogerAttributesTest {

  @Test
  void getAttributesSkipsNamelessAttributeNodes() throws Exception {
    String xml =
        """
        <Subject name="Admin" type="1">
          <Attribute context="global"/>
          <Attribute name="sys_defaultCommunity">
            <Value>Default</Value>
          </Attribute>
        </Subject>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
    Element subject = doc.getDocumentElement();

    PSAttributeList attrs = PSBackendCataloger.getAttributes(subject);

    assertEquals(1, attrs.size());
    assertFalse(attrs.getAttribute("sys_defaultCommunity") == null);
  }

  @Test
  void getAttributesEmptyWhenOnlyNamelessNodes() throws Exception {
    String xml =
        """
        <Subject name="Admin" type="1">
          <Attribute context="global"/>
        </Subject>
        """;
    Document doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));

    PSAttributeList attrs = PSBackendCataloger.getAttributes(doc.getDocumentElement());
    assertEquals(0, attrs.size());
  }
}
