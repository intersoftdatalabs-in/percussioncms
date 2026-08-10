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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Java serialization surface checks for design.objectstore serial-field cleanup on hottest content
 * editor types (#2405 / parent #2022). Field declared types use concrete {@link Serializable}
 * collections/maps; companion holders implement {@link Serializable}.
 */
public class PSObjectStoreSerialFieldEditorsTest {

  @Test
  public void testReplacementValueInterfaceIsSerializable() {
    assertTrue(
        Serializable.class.isAssignableFrom(IPSReplacementValue.class),
        "IPSReplacementValue must extend Serializable so IPSBackEndMapping/locator fields clear"
            + " serial-field");
    assertTrue(Serializable.class.isAssignableFrom(IPSBackEndMapping.class));
  }

  @Test
  public void testViewAndViewSetJavaSerialization() throws Exception {
    PSView view = new PSView("sys_All", Collections.singletonList("sys_title").iterator());
    PSViewSet viewSet = new PSViewSet();
    viewSet.addView(view);

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(view);
      oos.writeObject(viewSet);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSView serView = (PSView) ois.readObject();
      PSViewSet serSet = (PSViewSet) ois.readObject();

      assertEquals(view, serView);
      assertEquals("sys_All", serView.getName());
      assertEquals(view, serSet.getView("sys_All"));
      assertEquals(view, serSet.getView("SYS_ALL"));
    }
  }

  @Test
  public void testControlDependencyMapEmptyRoundTrip() throws Exception {
    PSControlDependencyMap map = new PSControlDependencyMap();

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(map);
      bytes = bos.toByteArray();
    }

    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      PSControlDependencyMap ser = (PSControlDependencyMap) ois.readObject();
      assertNotNull(ser);
      assertNotNull(ser.getInputDataExtensions());
      assertTrue(ser.getInputDataExtensions().isEmpty());
    }
  }

  @Test
  public void testConcreteFieldTypesOnHottestEditors() throws Exception {
    // Reflect declared field types so regressions to Map/List/Collection interfaces fail the suite.
    assertEquals(
        ArrayList.class, PSContentEditor.class.getDeclaredField("m_customActionGroups").getType());
    assertEquals(HashMap.class, PSField.class.getDeclaredField("m_occurrenceSettings").getType());
    assertEquals(HashMap.class, PSField.class.getDeclaredField("m_properties").getType());
    assertEquals(TreeMap.class, PSFieldSet.class.getDeclaredField("m_fields").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_params").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_dependencies").getType());
    assertEquals(ArrayList.class, PSControlMeta.class.getDeclaredField("m_files").getType());
    assertEquals(
        ArrayList.class, PSControlParameter.class.getDeclaredField("m_choiceList").getType());
    assertEquals(ArrayList.class, PSView.class.getDeclaredField("m_fields").getType());
    assertEquals(HashMap.class, PSViewSet.class.getDeclaredField("m_views").getType());
    assertEquals(HashMap.class, PSViewSet.class.getDeclaredField("m_conditionalViews").getType());
    assertEquals(
        HashMap.class,
        PSControlDependencyMap.class.getDeclaredField("m_controlDependencies").getType());
  }
}
