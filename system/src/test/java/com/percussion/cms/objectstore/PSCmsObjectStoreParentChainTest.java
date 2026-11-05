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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.IPSComponent;
import com.percussion.design.objectstore.IPSDocument;
import com.percussion.design.objectstore.IPSValidationContext;
import com.percussion.design.objectstore.PSComponent;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSLocator;
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
 * Behavioral coverage for typed {@code parentComponents} on cms.objectstore implementors of {@link
 * IPSComponent} (#2937 / parent #2455).
 */
public class PSCmsObjectStoreParentChainTest {

  @Test
  public void testCmsFromXmlSignaturesUseTypedParentList() throws Exception {
    Class<?>[] types = {
      PSAaRelationshipList.class,
      PSActiveAssemblerHandlerRequest.class,
      PSCloneSiteFolderRequest.class,
      PSCloningOptions.class,
      PSDependent.class,
      PSDependentSet.class,
      PSItemDefinition.class,
      PSSite.class,
      PSSlot.class
    };
    for (Class<?> type : types) {
      Method fromXml =
          type.getMethod("fromXml", Element.class, IPSDocument.class, List.class);
      Type[] params = fromXml.getGenericParameterTypes();
      assertTrue(
          params[2] instanceof ParameterizedType,
          type.getSimpleName() + " parentComponents must be parameterized");
      ParameterizedType pt = (ParameterizedType) params[2];
      assertEquals(List.class, pt.getRawType(), type.getSimpleName());
      assertEquals(
          IPSComponent.class, pt.getActualTypeArguments()[0], type.getSimpleName());
    }
  }

  @Test
  public void testDependentSetFromXmlRestoresParentChain() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSDependentSet original = new PSDependentSet();
    original.add(new PSDependent(11, new PSLocator(100, 1)));
    original.add(new PSDependent(22, new PSLocator(200, 2)));
    Element elem = original.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(7);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSDependentSet restored = new PSDependentSet(elem, null, parents);
    assertEquals(2, restored.size());
    assertEquals(11, ((PSDependent) restored.get(0)).getRelationshipId());
    assertEquals(22, ((PSDependent) restored.get(1)).getRelationshipId());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testDependentFromXmlLeavesCallerParentChainIntact() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSDependent original = new PSDependent(5, new PSLocator(50, 1));
    Element elem = original.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(3);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    // Seed instance then replace via typed fromXml (no public no-arg ctor).
    PSDependent restored = new PSDependent(0, new PSLocator(1, 1));
    restored.fromXml(elem, null, parents);

    assertEquals(5, restored.getRelationshipId());
    assertEquals(50, restored.getLocator().getId());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testCloningOptionsFromXmlLeavesCallerParentChainIntact() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCloningOptions original =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folderName",
            PSCloningOptions.COPY_NO_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            null);
    Element elem = original.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(9);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSCloningOptions restored = new PSCloningOptions(elem, null, parents);
    assertEquals(original, restored);
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testAaRelationshipListEmptyFromXmlRestoresParentChain() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSAaRelationshipList list = new PSAaRelationshipList();
    Element elem = list.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(4);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    PSAaRelationshipList restored = new PSAaRelationshipList(elem, null, parents);
    assertEquals(0, restored.size());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testDesignSentinelStillCompatibleAsParentListEntry() {
    // cms.objectstore lists accept design.objectstore IPSComponent entries
    List<IPSComponent> parents = new ArrayList<>();
    PSDisplayText label = new PSDisplayText("cms-parent-chain");
    parents.add(label);
    assertEquals(1, parents.size());
    assertSame(label, parents.get(0));
  }

  /** Minimal {@link PSComponent} used as a parent-list sentinel. */
  private static final class ProbeComponent extends PSComponent {
    private static final long serialVersionUID = 1L;

    ProbeComponent(int id) {
      setId(id);
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
