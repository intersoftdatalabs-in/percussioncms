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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Behavioral coverage for typed {@link PSDeployableObject#setRequiredClasses(Iterator)} / {@link
 * PSDeployableObject#getRequiredClasses()} after real-generics cleanup (issue #2417).
 */
public class PSDeployableObjectRequiredClassesTest {

  @Test
  public void testRequiredClassesRoundTripAndClone() throws Exception {
    PSDeployableObject obj =
        new PSDeployableObject(
            PSDependency.TYPE_LOCAL,
            "1",
            "TestObj",
            "Test Object",
            "myTestObject",
            true,
            false,
            true);

    List<String> classes = new ArrayList<>();
    classes.add("com.example.Alpha");
    classes.add("com.example.Beta");
    obj.setRequiredClasses(classes.iterator());

    List<String> fromGetter = new ArrayList<>();
    Iterator<String> it = obj.getRequiredClasses();
    while (it.hasNext()) {
      fromGetter.add(it.next());
    }
    assertEquals(classes, fromGetter);

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element el = obj.toXml(doc);
    PSDeployableObject restored = new PSDeployableObject(el);
    List<String> fromXml = new ArrayList<>();
    Iterator<String> xmlIt = restored.getRequiredClasses();
    while (xmlIt.hasNext()) {
      fromXml.add(xmlIt.next());
    }
    assertEquals(classes, fromXml);
    assertEquals(obj, restored);

    PSDeployableObject clone = (PSDeployableObject) obj.clone();
    assertEquals(obj, clone);
    // mutate clone list path via re-set
    List<String> other = new ArrayList<>();
    other.add("com.example.Gamma");
    clone.setRequiredClasses(other.iterator());
    assertFalse(obj.equals(clone));
  }

  @Test
  public void testRequiredClassesRejectsNullOrEmptyEntry() {
    PSDeployableObject obj =
        new PSDeployableObject(
            PSDependency.TYPE_LOCAL,
            "1",
            "TestObj",
            "Test Object",
            "myTestObject",
            true,
            false,
            true);

    assertThrows(IllegalArgumentException.class, () -> obj.setRequiredClasses(null));

    List<String> empty = new ArrayList<>();
    assertThrows(IllegalArgumentException.class, () -> obj.setRequiredClasses(empty.iterator()));

    List<String> withBlank = new ArrayList<>();
    withBlank.add("com.example.Ok");
    withBlank.add("  ");
    assertThrows(
        IllegalArgumentException.class, () -> obj.setRequiredClasses(withBlank.iterator()));
    // failed set clears the list
    assertTrue(!obj.getRequiredClasses().hasNext());
  }
}
