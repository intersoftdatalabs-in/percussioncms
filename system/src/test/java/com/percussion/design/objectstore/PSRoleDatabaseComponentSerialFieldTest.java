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
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Java serialization surface for PSRole database-component fields after #2451 residual (#2677 /
 * parent #2022). Hierarchy under {@link PSDatabaseComponent} is product-safe {@link Serializable}
 * so {@code m_subjects} / {@code m_attributes} clear {@code -Xlint:serial} serial-field. Wire/XML
 * behavior is unchanged.
 */
public class PSRoleDatabaseComponentSerialFieldTest {

  @Test
  public void testDatabaseComponentHierarchyIsSerializable() {
    assertTrue(Serializable.class.isAssignableFrom(PSDatabaseComponent.class));
    assertTrue(Serializable.class.isAssignableFrom(PSDatabaseComponentCollection.class));
    assertTrue(Serializable.class.isAssignableFrom(PSAttributeList.class));
    assertTrue(Serializable.class.isAssignableFrom(PSAttribute.class));
    assertTrue(Serializable.class.isAssignableFrom(PSAttributeValue.class));
    assertTrue(Serializable.class.isAssignableFrom(PSSubject.class));
    assertTrue(Serializable.class.isAssignableFrom(PSRelativeSubject.class));
    assertTrue(Serializable.class.isAssignableFrom(PSGlobalSubject.class));
    assertTrue(Serializable.class.isAssignableFrom(PSRelation.class));
    assertTrue(Serializable.class.isAssignableFrom(PSRole.class));
  }

  @Test
  public void testPsRoleSubjectAndAttributeFieldsAreSerializableTypes() throws Exception {
    Class<?> subjectsType = PSRole.class.getDeclaredField("m_subjects").getType();
    Class<?> attributesType = PSRole.class.getDeclaredField("m_attributes").getType();

    assertEquals(PSDatabaseComponentCollection.class, subjectsType);
    assertEquals(PSAttributeList.class, attributesType);
    assertTrue(Serializable.class.isAssignableFrom(subjectsType));
    assertTrue(Serializable.class.isAssignableFrom(attributesType));
  }

  @Test
  public void testEmptyRoleJavaSerializationRoundTrip() throws Exception {
    PSRole role = new PSRole("Editors");
    PSRole ser = roundTrip(role);

    assertEquals(role.getName(), ser.getName());
    assertEquals(0, ser.getSubjects().size());
    assertEquals(0, ser.getAttributes().size());
    assertTrue(PSRoleTest.testRoleEquals(role, ser));
  }

  @Test
  public void testRoleWithSubjectsAndAttributesRoundTrip() throws Exception {
    PSRole role = new PSRole("Authors");
    role.getSubjects()
        .add(new PSRelativeSubject("alice", PSSubject.SUBJECT_TYPE_USER, new PSAttributeList()));
    role.getAttributes().setAttribute("dept", Arrays.asList("editorial"));

    PSRole ser = roundTrip(role);

    assertEquals("Authors", ser.getName());
    assertEquals(1, ser.getSubjects().size());
    assertTrue(
        role.containsCorrespondingSubject((PSSubject) ser.getSubjects().get(0))
            || ser.containsCorrespondingSubject(
                (PSSubject) role.getSubjects().get(0)));
    assertEquals(1, ser.getAttributes().size());
    PSAttribute attr = ser.getAttributes().getAttribute("dept");
    assertNotNull(attr);
    assertEquals(Arrays.asList("editorial"), attr.getValues());
    assertTrue(PSRoleTest.testRoleEquals(role, ser));
  }

  @SuppressWarnings("unchecked")
  private static <T> T roundTrip(T value) throws Exception {
    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
      bytes = bos.toByteArray();
    }
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (T) ois.readObject();
    }
  }
}
