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
package com.percussion.design.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for typed {@code parentComponents} on {@link IPSComponent} and core
 * design.objectstore fromXml parent-chain push/pop (#2936 / parent #2455).
 */
public class PSComponentParentChainTest {

  @Test
  public void testIpsComponentFromXmlUsesTypedParentList() throws Exception {
    Method fromXml =
        IPSComponent.class.getMethod(
            "fromXml", Element.class, IPSDocument.class, List.class);
    Type[] params = fromXml.getGenericParameterTypes();
    assertTrue(params[2] instanceof ParameterizedType, "parentComponents must be parameterized");
    ParameterizedType pt = (ParameterizedType) params[2];
    assertEquals(List.class, pt.getRawType());
    assertEquals(IPSComponent.class, pt.getActualTypeArguments()[0]);
  }

  @Test
  public void testUpdateAndResetParentListPushPop() {
    List<IPSComponent> parents = new ArrayList<>();
    ProbeComponent sentinel = new ProbeComponent(1);
    parents.add(sentinel);

    ProbeComponent child = new ProbeComponent(2);
    List<IPSComponent> afterPush = child.exposeUpdate(parents);
    assertSame(parents, afterPush);
    assertEquals(2, parents.size());
    assertSame(sentinel, parents.get(0));
    assertSame(child, parents.get(1));

    child.exposeReset(parents, 1);
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testUpdateParentListCreatesListWhenNull() {
    ProbeComponent child = new ProbeComponent(9);
    List<IPSComponent> created = child.exposeUpdate(null);
    assertEquals(1, created.size());
    assertSame(child, created.get(0));
    child.exposeReset(created, 0);
    assertTrue(created.isEmpty());
  }

  @Test
  public void testDisplayTextFromXmlRestoresParentChain() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSDisplayText original = new PSDisplayText("hello-parent-chain");
    Element elem = original.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(42);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSDisplayText restored = new PSDisplayText();
    restored.fromXml(elem, null, parents);

    assertEquals("hello-parent-chain", restored.getText());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testEntryFromXmlRestoresParentChainAfterNestedLabel() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSEntry original = new PSEntry("v1", "Label One");
    Element elem = original.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(7);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSEntry restored = new PSEntry(elem, null, parents);

    assertEquals("v1", restored.getValue());
    assertEquals("Label One", restored.getLabel().getText());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testCollectionComponentFromXmlRestoresParentChain() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSActionLinkList list = new PSActionLinkList();
    // Empty list still exercises updateParentList/resetParentList on fromXml.
    Element elem = list.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(3);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSActionLinkList restored = new PSActionLinkList(elem, null, parents);
    assertEquals(0, restored.size());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  /** Minimal {@link PSComponent} exposing parent-list helpers for direct push/pop tests. */
  private static final class ProbeComponent extends PSComponent {
    private static final long serialVersionUID = 1L;

    ProbeComponent(int id) {
      setId(id);
    }

    List<IPSComponent> exposeUpdate(List<IPSComponent> parents) {
      return updateParentList(parents);
    }

    void exposeReset(List<?> parents, int size) {
      resetParentList(parents, size);
    }

    @Override
    public Element toXml(Document doc) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void fromXml(
        Element sourceNode, IPSDocument parentDoc, List<IPSComponent> parentComponents) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void validate(IPSValidationContext cxt) {
      // no-op
    }
  }
}
