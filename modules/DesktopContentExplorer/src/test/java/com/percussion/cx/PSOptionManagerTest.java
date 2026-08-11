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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cx.error.PSContentExplorerException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral tests for pure helpers on {@link PSOptionManager} after rawtypes cleanup (#3012). Does
 * not open the Swing applet (requires live server resources).
 */
public class PSOptionManagerTest {

  @Test
  public void toXmlCollectionRejectsNullArgs() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement("root");
    List<IPSClientObjects> empty = Collections.emptyList();

    assertThrows(
        IllegalArgumentException.class, () -> PSOptionManager.toXmlCollection(null, doc, empty));
    assertThrows(
        IllegalArgumentException.class, () -> PSOptionManager.toXmlCollection(root, null, empty));
    assertThrows(
        IllegalArgumentException.class, () -> PSOptionManager.toXmlCollection(root, doc, null));
  }

  @Test
  public void toXmlCollectionEmptyLeavesParentUnchanged() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);

    PSOptionManager.toXmlCollection(root, doc, Collections.emptyList());

    assertEquals(0, root.getChildNodes().getLength());
  }

  @Test
  public void toXmlCollectionAppendsElementsFromTypedCollection() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement("root");
    doc.appendChild(root);

    List<IPSClientObjects> items = new ArrayList<>();
    items.add(new StubClientObject("first"));
    items.add(new StubClientObject("second"));

    PSOptionManager.toXmlCollection(root, doc, items);

    assertEquals(2, root.getChildNodes().getLength());
    assertEquals("first", ((Element) root.getChildNodes().item(0)).getTagName());
    assertEquals("second", ((Element) root.getChildNodes().item(1)).getTagName());
  }

  @Test
  public void toXmlCollectionRejectsNullElementInCollection() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = doc.createElement("root");
    List<IPSClientObjects> items = new ArrayList<>();
    items.add(null);

    assertThrows(
        IllegalArgumentException.class, () -> PSOptionManager.toXmlCollection(root, doc, items));
  }

  @Test
  public void compareBothNullIsTrue() {
    assertTrue(PSOptionManager.compare(null, null));
  }

  @Test
  public void compareOneNullIsFalse() {
    assertFalse(PSOptionManager.compare("a", null));
    assertFalse(PSOptionManager.compare(null, "b"));
  }

  @Test
  public void compareStringsIgnoreCase() {
    assertTrue(PSOptionManager.compare("Abc", "abc"));
    assertFalse(PSOptionManager.compare("Abc", "abd"));
  }

  @Test
  public void compareObjectArrays() {
    assertTrue(PSOptionManager.compare(new String[] {"a", "b"}, new String[] {"a", "b"}));
    assertFalse(PSOptionManager.compare(new String[] {"a"}, new String[] {"b"}));
  }

  @Test
  public void compareUsesEqualsForNonStringObjects() {
    assertTrue(PSOptionManager.compare(Integer.valueOf(7), Integer.valueOf(7)));
    assertFalse(PSOptionManager.compare(Integer.valueOf(7), Integer.valueOf(8)));
    assertTrue(PSOptionManager.compare(Arrays.asList("x"), Arrays.asList("x")));
  }

  /** Minimal {@link IPSClientObjects} that emits a named empty element. */
  private static final class StubClientObject implements IPSClientObjects {
    private final String tagName;

    StubClientObject(String tagName) {
      this.tagName = tagName;
    }

    @Override
    public void fromXml(Element sourceNode) throws PSContentExplorerException {
      // not needed for toXml tests
    }

    @Override
    public Element toXml(Document doc) {
      return doc.createElement(tagName);
    }
  }
}
