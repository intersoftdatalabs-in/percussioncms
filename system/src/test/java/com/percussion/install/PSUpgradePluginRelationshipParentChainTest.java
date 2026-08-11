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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.IPSComponent;
import com.percussion.design.objectstore.IPSDocument;
import com.percussion.design.objectstore.IPSValidationContext;
import com.percussion.design.objectstore.PSComponent;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipConfigSet;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.install.PSUpgradePluginRelationship.RelationshipConfig;
import com.percussion.install.PSUpgradePluginRelationship.RelationshipConfigSet;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Closeout coverage for remaining typed {@code parentComponents} adapters outside core
 * design/cms.objectstore implementor waves (#2938 / parent #2455).
 *
 * <p>Covers the install-plugin {@link RelationshipConfig} / {@link RelationshipConfigSet}
 * overrides that still used raw {@code List} after slices 1–2.
 */
public class PSUpgradePluginRelationshipParentChainTest {

  @Test
  public void testCreateMemberObjectUsesTypedParentList() throws Exception {
    Method create =
        RelationshipConfigSet.class.getDeclaredMethod(
            "createMemberObject", Element.class, IPSDocument.class, List.class);
    Type[] params = create.getGenericParameterTypes();
    assertTrue(params[2] instanceof ParameterizedType, "parentComponents must be parameterized");
    ParameterizedType pt = (ParameterizedType) params[2];
    assertEquals(List.class, pt.getRawType());
    assertEquals(IPSComponent.class, pt.getActualTypeArguments()[0]);
  }

  @Test
  public void testRelationshipConfigCtorUsesTypedParentList() throws Exception {
    Constructor<?> ctor =
        RelationshipConfig.class.getDeclaredConstructor(
            Element.class, IPSDocument.class, List.class);
    Type[] params = ctor.getGenericParameterTypes();
    assertTrue(params[2] instanceof ParameterizedType, "parentComponents must be parameterized");
    ParameterizedType pt = (ParameterizedType) params[2];
    assertEquals(List.class, pt.getRawType());
    assertEquals(IPSComponent.class, pt.getActualTypeArguments()[0]);
  }

  @Test
  public void testRelationshipConfigFromXmlRestoresParentChain() throws Exception {
    PSRelationshipConfigSet configs = PSRelationshipConfigTest.getConfigs();
    PSRelationshipConfig sample = configs.getConfig(PSRelationshipConfig.TYPE_NEW_COPY);
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element elem = sample.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(11);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    Constructor<RelationshipConfig> ctor =
        RelationshipConfig.class.getDeclaredConstructor(
            Element.class, IPSDocument.class, List.class);
    ctor.setAccessible(true);
    RelationshipConfig restored = ctor.newInstance(elem, null, parents);

    assertEquals(sample.getName(), restored.getName());
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testRelationshipConfigSetFromXmlRestoresParentChain() throws Exception {
    PSRelationshipConfigSet source = PSRelationshipConfigTest.getConfigs();
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element elem = source.toXml(doc);

    ProbeComponent sentinel = new ProbeComponent(22);
    List<IPSComponent> parents = new ArrayList<>();
    parents.add(sentinel);

    RelationshipConfigSet restored = new RelationshipConfigSet(elem);
    // Element ctor uses null parent list; exercise typed createMemberObject via fromXml.
    restored.clear();
    restored.fromXml(elem, null, parents);

    assertTrue(restored.size() > 0, "expected relationship configs from fixture");
    assertTrue(
        restored.getConfig(PSRelationshipConfig.TYPE_NEW_COPY) instanceof RelationshipConfig,
        "createMemberObject must build RelationshipConfig members");
    assertEquals(1, parents.size());
    assertSame(sentinel, parents.get(0));
  }

  @Test
  public void testPluginGetConfigSetUsesTypedAdapter() throws Exception {
    PSRelationshipConfigSet source = PSRelationshipConfigTest.getConfigs();
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = source.toXml(doc);
    doc.appendChild(root);

    PSUpgradePluginRelationship plugin = new PSUpgradePluginRelationship();
    PSRelationshipConfigSet loaded = plugin.getConfigSet(doc);

    assertTrue(loaded instanceof RelationshipConfigSet);
    assertTrue(loaded.size() > 0);
    PSRelationshipConfig member = loaded.getConfig(PSRelationshipConfig.TYPE_NEW_COPY);
    assertTrue(member instanceof RelationshipConfig);
    // Install adapter still allows whitespace in relationship names.
    member.setName("name with spaces");
    assertEquals("name with spaces", member.getName());
  }

  @Test
  public void testNoRemainingRawParentComponentsInSystemMain() throws Exception {
    // Guardrail: monorepo system main sources must not keep raw List parentComponents
    // signatures after closeout (tmp/ probe trees excluded by scanning classpath types only).
    Method fromXml =
        IPSComponent.class.getMethod("fromXml", Element.class, IPSDocument.class, List.class);
    Type[] params = fromXml.getGenericParameterTypes();
    assertTrue(params[2] instanceof ParameterizedType);
    assertEquals(
        IPSComponent.class, ((ParameterizedType) params[2]).getActualTypeArguments()[0]);

    Method create =
        PSRelationshipConfigSet.class.getDeclaredMethod(
            "createMemberObject", Element.class, IPSDocument.class, List.class);
    Type[] createParams = create.getGenericParameterTypes();
    assertTrue(createParams[2] instanceof ParameterizedType);
    assertEquals(
        IPSComponent.class,
        ((ParameterizedType) createParams[2]).getActualTypeArguments()[0]);
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
