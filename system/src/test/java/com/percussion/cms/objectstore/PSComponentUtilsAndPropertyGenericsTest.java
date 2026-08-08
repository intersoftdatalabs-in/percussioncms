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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for {@link PSComponentUtils} and {@link PSCmsProperty} generics
 * parameterization (issue #2376 cms.objectstore rawtypes batch 2d).
 */
public class PSComponentUtilsAndPropertyGenericsTest {

  @Test
  public void testGetEnumeratedAttribute() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = doc.createElement("Test");
    List<String> allowed = Arrays.asList("alpha", "beta", "gamma");

    // missing attribute → first allowed value
    assertEquals("alpha", PSComponentUtils.getEnumeratedAttribute(el, "kind", allowed));

    el.setAttribute("kind", "beta");
    assertEquals("beta", PSComponentUtils.getEnumeratedAttribute(el, "kind", allowed));

    el.setAttribute("kind", "delta");
    assertThrows(
        PSUnknownNodeTypeException.class,
        () -> PSComponentUtils.getEnumeratedAttribute(el, "kind", allowed));
  }

  @Test
  public void testGetChildElementsTyped() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement("Root");
    Element a = doc.createElement("Child");
    a.setAttribute("id", "1");
    Element b = doc.createElement("Child");
    b.setAttribute("id", "2");
    Element nested = doc.createElement("Nested");
    Element deep = doc.createElement("Child");
    deep.setAttribute("id", "deep");
    nested.appendChild(deep);
    root.appendChild(a);
    root.appendChild(b);
    root.appendChild(nested);
    doc.appendChild(root);

    Iterator<Element> children = PSComponentUtils.getChildElements(root, "Child");
    assertTrue(children.hasNext());
    assertEquals("1", children.next().getAttribute("id"));
    assertTrue(children.hasNext());
    assertEquals("2", children.next().getAttribute("id"));
    assertFalse(children.hasNext()); // nested Child is not immediate
  }

  @Test
  public void testCmsPropertyValueRoundTrip() throws Exception {
    // concrete subclass of abstract PSCmsProperty
    PSDFProperty prop = new PSDFProperty("sys_title", "Hello");
    assertEquals("sys_title", prop.getName());
    assertEquals("Hello", prop.getValue());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = prop.toXml(doc);
    PSDFProperty restored = new PSDFProperty(xml);
    assertEquals(prop.getName(), restored.getName());
    assertEquals(prop.getValue(), restored.getValue());
  }
}
