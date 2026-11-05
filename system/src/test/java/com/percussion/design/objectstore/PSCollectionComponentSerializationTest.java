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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.util.PSCollection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Serialization surface for {@link PSCollectionComponent} after {@code PSConcurrentList} became
 * {@link Serializable} (#2450 / parent #2022 residual). Concrete minimal subclass avoids XML/wire
 * dependencies while exercising id + collection payload round-trip.
 */
public class PSCollectionComponentSerializationTest {

  @Test
  public void testCollectionComponentDeclaresUidAndIsSerializable() throws Exception {
    assertTrue(Serializable.class.isAssignableFrom(PSCollectionComponent.class));
    assertTrue(Serializable.class.isAssignableFrom(PSCollection.class));
    Field f = PSCollectionComponent.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()));
    f.setAccessible(true);
    assertEquals(1L, f.getLong(null));
  }

  @Test
  public void testRoundTripPreservesIdMemberClassAndElements() throws Exception {
    TestCollectionComponent original = new TestCollectionComponent();
    original.setId(77);
    original.add("a");
    original.add("b");

    byte[] bytes;
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(original);
      bytes = bos.toByteArray();
    }

    TestCollectionComponent restored;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      restored = (TestCollectionComponent) ois.readObject();
    }

    assertNotNull(restored);
    assertEquals(77, restored.getId());
    assertEquals(String.class, restored.getMemberClassType());
    assertEquals(2, restored.size());
    assertEquals("a", restored.get(0));
    assertEquals("b", restored.get(1));
    // Locks reinitialized: mutation must succeed
    assertTrue(restored.add("c"));
    assertEquals(3, restored.size());
  }

  /** Minimal concrete {@link PSCollectionComponent} for Java serialization tests only. */
  private static final class TestCollectionComponent extends PSCollectionComponent {

    private static final long serialVersionUID = 1L;

    TestCollectionComponent() {
      super(String.class);
    }

    @Override
    public Element toXml(Document doc) {
      throw new UnsupportedOperationException("not used in serialization test");
    }

    @Override
    public void fromXml(
        Element sourceNode, IPSDocument parentDoc, List<IPSComponent> parentComponents) {
      throw new UnsupportedOperationException("not used in serialization test");
    }
  }
}
