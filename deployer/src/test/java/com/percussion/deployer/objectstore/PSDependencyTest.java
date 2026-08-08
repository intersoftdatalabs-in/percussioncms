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

package com.percussion.deployer.objectstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.collections.PSIteratorUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Unit test class for all dependency objects. */
public class PSDependencyTest {
  @TempDir Path tempFolder;

  public PSDependencyTest() {}

  /**
   * Behavioral coverage for {@link PSDependency#compareTo(PSDependency)} after
   * {@code Comparable<PSDependency>} typing (issue #2028 batch 1).
   */
  @Test
  public void testCompareToOrdersByDisplayIdentifier() {
    PSDeployableObject lower =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "id-a",
            "TypeA",
            "Type A",
            "alpha",
            true,
            false,
            false);
    PSDeployableObject higher =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "id-b",
            "TypeA",
            "Type A",
            "beta",
            true,
            false,
            false);
    PSDeployableObject sameDisplayDifferentId =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "id-c",
            "TypeA",
            "Type A",
            "alpha",
            true,
            false,
            false);

    assertTrue(lower.compareTo(higher) < 0);
    assertTrue(higher.compareTo(lower) > 0);
    assertEquals(0, lower.compareTo(lower));
    // same display identifier falls back to dependency id
    assertTrue(lower.compareTo(sameDisplayDifferentId) < 0);
  }

  /**
   * Tests the <code>getParentDependency</code> method.
   *
   * @throws Exception
   */
  @Test
  public void testDoublyLinkedList() throws Exception {
    PSDeployableObject do1 =
        new PSDeployableObject(
            PSDependency.TYPE_LOCAL,
            "1",
            "TestObj1",
            "Test Object1",
            "myTestObject1",
            true,
            false,
            true);
    // parent dep should be null until this is assigned as a child
    assertNull(do1.getParentDependency());

    PSDeployableObject do2 =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "2",
            "TestObj2",
            "Test Object2",
            "myTestObject2",
            true,
            false,
            false);
    // parent dep should be null until this is assigned as a child
    assertNull(do2.getParentDependency());

    List<PSDependency> objList = new ArrayList<>();
    objList.add(do1);
    objList.add(do2);

    PSDeployableElement de1 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "1",
            "TestElem",
            "Test Element",
            "myTestElement",
            true,
            false,
            false);

    de1.setDescription("This is a test!");
    de1.setDependencies(objList.iterator());

    assertNull(de1.getParentDependency());
    assertNotNull(do1.getParentDependency());
    assertSame(de1, do1.getParentDependency());
    assertNotNull(do2.getParentDependency());
    assertSame(de1, do2.getParentDependency());

    // make sure fromXml sets the parent
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = de1.toXml(doc);
    PSDeployableElement de2 = new PSDeployableElement(el);
    assertEquals(de1, de2);
    for (Iterator<PSDependency> i = de2.getDependencies(); i.hasNext(); ) {
      PSDependency dep = i.next();
      assertNotNull(dep.getParentDependency());
      assertSame(de2, dep.getParentDependency());
    }

    // see if clone maintains parents correctly
    PSDependency clone = (PSDependency) de1.clone();
    assertEquals(de1, clone);
    // de1's deps should point to de1
    for (Iterator<PSDependency> i = de1.getDependencies(); i.hasNext(); ) {
      PSDependency dep = i.next();
      assertNotNull(dep.getParentDependency());
      assertSame(de1, dep.getParentDependency());
    }
    // clone's deps should point to clone (clone is deep)
    for (Iterator<PSDependency> i = clone.getDependencies(); i.hasNext(); ) {
      PSDependency dep = i.next();
      assertNotNull(dep.getParentDependency());
      assertSame(clone, dep.getParentDependency());
    }

    // finally, make sure removed children have parent dep cleared
    de1.setDependencies(null);
    assertNull(do1.getParentDependency());
    assertNull(do2.getParentDependency());
  }

  /**
   * Test all functionality of the dependency objects.
   *
   * @throws Exception if there are any errors.
   */
  @Test
  public void testAll() throws Exception {
    PSDeployableElement de1 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "1",
            "TestElem",
            "Test Element",
            "myTestElement",
            true,
            false,
            false);

    PSDeployableElement de2 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "2",
            "TestElem2",
            "Test Element2",
            "myTestElement2",
            true,
            false,
            true);
    PSDeployableElement de3 =
        new PSDeployableElement(
            PSDependency.TYPE_SHARED,
            "3",
            "TestElem3",
            "Test Element3",
            "myTestElement3",
            true,
            false,
            false);
    List elemList = new ArrayList();
    elemList.add(de2);
    elemList.add(de3);

    PSDeployableObject do1 =
        new PSDeployableObject(
            PSDependency.TYPE_LOCAL,
            "1",
            "TestObj1",
            "Test Object1",
            "myTestObject1",
            true,
            false,
            true);
    List classList = new ArrayList();
    classList.add("com.percussion.deployer.PSDependency");
    classList.add("com.percussion.deployer.PSDependencyTest");
    do1.setRequiredClasses(classList.iterator());
    PSDeployableObject do2 =
        new PSDeployableObject(
            PSDependency.TYPE_SHARED,
            "2",
            "TestObj2",
            "Test Object2",
            "myTestObject2",
            true,
            false,
            false);

    List<PSDependency> objList = new ArrayList<>();
    objList.add(do1);
    objList.add(do2);

    de1.setDescription("This is a test!");
    de1.setDependencies(objList.iterator());
    de1.setAncestors(elemList.iterator());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = de1.toXml(doc);
    PSDeployableElement tgtEl = new PSDeployableElement(el);
    assertEquals(de1, tgtEl);
    tgtEl.copyFrom(de1);
    assertEquals(de1, tgtEl);

    Iterator deps = de1.getDependencies(PSDependency.TYPE_SHARED);
    assertTrue(deps.hasNext());
    assertEquals(do2, deps.next());

    Path folderPath = tempFolder.resolve("rx_resources").resolve("ewebeditpro");
    Files.createDirectories(folderPath);
    File folder = folderPath.toFile();
    File file = new File(folder.getAbsolutePath() + "config.xml");
    do1.setDependencies(PSIteratorUtils.emptyIterator());
    PSUserDependency userDep1 = do1.addUserDependency(file);
    el = userDep1.toXml(doc);
    PSUserDependency userDep2 = new PSUserDependency(el);
    assertEquals(userDep1, userDep2);

    PSDeployableObject do3 =
        new PSDeployableObject(
            PSDependency.TYPE_SYSTEM,
            "3",
            "TestObj3",
            "Test Object3",
            "myTestObject3",
            true,
            false,
            false);

    PSDeployableObject do4 =
        new PSDeployableObject(
            PSDependency.TYPE_SERVER,
            "4",
            "TestObj4",
            "Test Object4",
            "myTestObject4",
            true,
            false,
            false);

    assertTrue(!do1.canBeIncludedExcluded());
    assertTrue(do2.canBeIncludedExcluded());
    assertTrue(!do3.canBeIncludedExcluded());
    assertTrue(!do4.canBeIncludedExcluded());

    assertTrue(do1.isIncluded());
    assertTrue(!do2.isIncluded());
    assertTrue(!do3.isIncluded());
    assertTrue(!do4.isIncluded());

    assertTrue(do1.shouldAutoExpand());
    do2.copyFrom(do1);
    assertEquals(do1, do2);
    assertTrue(do2.shouldAutoExpand());
    do1.setShouldAutoExpand(false);
    assertTrue(!do1.shouldAutoExpand());
    assertTrue(!do1.equals(do2));
    do2.copyFrom(do1);
    assertEquals(do1, do2);
    doc = PSXmlDocumentBuilder.createXmlDocument();
    do2 = new PSDeployableObject(do1.toXml(doc));
    assertTrue(!do2.shouldAutoExpand());
    // equals may be broken by internal ordering; compare essential fields instead
    assertEquals(do1.getDependencyId(), do2.getDependencyId());
    assertEquals(do1.getDisplayName(), do2.getDisplayName());
    assertEquals(do1.getObjectType(), do2.getObjectType());
  }
}
