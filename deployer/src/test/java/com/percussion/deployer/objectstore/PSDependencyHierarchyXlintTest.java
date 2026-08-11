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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Smoke coverage for hierarchy this-escape mitigation and leaf {@code final} (issue #2847 Xlint
 * residual after batch 5).
 */
public class PSDependencyHierarchyXlintTest {

  @Test
  public void deployableElementAndDependencyDataAreFinal() {
    assertTrue(Modifier.isFinal(PSDeployableElement.class.getModifiers()));
    assertTrue(Modifier.isFinal(PSDependencyData.class.getModifiers()));
    // Not final: PSUserDependency extends PSDeployableObject
    assertFalse(Modifier.isFinal(PSDeployableObject.class.getModifiers()));
  }

  @Test
  public void deployableElementXmlRoundTripViaElementCtor() throws Exception {
    PSDeployableElement original =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "app1",
            "Application",
            "Application",
            "App One",
            true,
            true,
            false);
    original.setDescription("desc");

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);
    PSDeployableElement restored = new PSDeployableElement(xml);

    assertEquals(original.getDependencyId(), restored.getDependencyId());
    assertEquals(original.getDisplayName(), restored.getDisplayName());
    assertEquals(original.getObjectType(), restored.getObjectType());
    assertEquals(original.getDescription(), restored.getDescription());
    assertEquals(original.getDependencyType(), restored.getDependencyType());
  }

  @Test
  public void deployableObjectXmlRoundTripViaElementCtor() throws Exception {
    PSDeployableObject original =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "obj1",
            "Schema",
            "Schema",
            "Table One",
            false,
            false,
            false);
    // TYPE_SHARED may toggle inclusion; TYPE_LOCAL always includes and rejects setIsIncluded
    original.setIsIncluded(true);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element xml = original.toXml(doc);
    PSDeployableObject restored = new PSDeployableObject(xml);

    assertEquals(original.getDependencyId(), restored.getDependencyId());
    assertEquals(original.getDisplayName(), restored.getDisplayName());
    assertEquals(original.getObjectType(), restored.getObjectType());
    assertEquals(original.getDependencyType(), restored.getDependencyType());
    assertTrue(restored.isIncluded());
  }

  @Test
  public void setDependencyTypeSideEffectsUnchanged() {
    PSDeployableObject local =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "x",
            "T",
            "T",
            "x",
            false,
            false,
            false);
    local.setDependencyType(PSDependency.TYPE_LOCAL);
    assertEquals(PSDependency.TYPE_LOCAL, local.getDependencyType());
    assertTrue(local.isIncluded());

    local.setDependencyType(PSDependency.TYPE_SYSTEM);
    assertEquals(PSDependency.TYPE_SYSTEM, local.getDependencyType());
    assertFalse(local.isIncluded());
  }

  @Test
  public void deployableElementChildIteratorIsTyped() {
    PSDeployableElement el =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "root",
            "Application",
            "Application",
            "Root",
            true,
            false,
            false);
    Iterator<PSDependency> children = el.getDependencies();
    assertFalse(children.hasNext());
  }
}
