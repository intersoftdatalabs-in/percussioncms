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
package com.percussion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Java serialization checks for {@link PSConcurrentList} / {@link PSCollection} (#2450 / parent
 * #2022 residual). Locks must reinitialize; list contents and member class must round-trip.
 */
public class PSConcurrentListSerializationTest {

  @Test
  public void testImplementsSerializableAndDeclaresUid() throws Exception {
    assertTrue(Serializable.class.isAssignableFrom(PSConcurrentList.class));
    assertTrue(Serializable.class.isAssignableFrom(PSCollection.class));

    Field listUid = PSConcurrentList.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(listUid.getModifiers()) && Modifier.isFinal(listUid.getModifiers()));
    listUid.setAccessible(true);
    assertEquals(1L, listUid.getLong(null));

    Field collUid = PSCollection.class.getDeclaredField("serialVersionUID");
    assertTrue(Modifier.isStatic(collUid.getModifiers()) && Modifier.isFinal(collUid.getModifiers()));
    collUid.setAccessible(true);
    assertEquals(1L, collUid.getLong(null));
  }

  @Test
  public void testConcurrentListRoundTripPreservesOrderAndContents() throws Exception {
    PSConcurrentList<String> original = new PSConcurrentList<>();
    original.add("alpha");
    original.add("beta");
    original.add("gamma");

    PSConcurrentList<String> restored = roundTrip(original);

    assertNotNull(restored);
    assertEquals(3, restored.size());
    assertEquals(Arrays.asList("alpha", "beta", "gamma"), List.copyOf(restored));
    // Post-deser mutability / lock reinit: write path must not NPE
    assertTrue(restored.add("delta"));
    assertEquals(4, restored.size());
    assertEquals("delta", restored.get(3));
  }

  @Test
  public void testEmptyConcurrentListRoundTrip() throws Exception {
    PSConcurrentList<Integer> original = new PSConcurrentList<>();
    PSConcurrentList<Integer> restored = roundTrip(original);
    assertNotNull(restored);
    assertTrue(restored.isEmpty());
    assertTrue(restored.add(42));
    assertEquals(1, restored.size());
  }

  @Test
  public void testCollectionRoundTripPreservesMemberClassAndElements() throws Exception {
    PSCollection original = new PSCollection(String.class);
    original.add("one");
    original.add("two");

    PSCollection restored = roundTrip(original);

    assertNotNull(restored);
    assertEquals(String.class, restored.getMemberClassType());
    assertEquals(2, restored.size());
    assertEquals("one", restored.get(0));
    assertEquals("two", restored.get(1));
    // Type guard still works after deserialization
    restored.add("three");
    assertEquals(3, restored.size());
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
