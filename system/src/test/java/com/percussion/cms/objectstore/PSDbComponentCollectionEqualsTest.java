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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests order-independent equality/hash helpers on {@link PSDbComponentCollection} after generics
 * parameterization (#2296).
 */
public class PSDbComponentCollectionEqualsTest {

  @Test
  public void equalsIgnoreOrderSameElementsDifferentOrder() {
    List<String> a = Arrays.asList("x", "y", "z");
    List<String> b = Arrays.asList("z", "x", "y");
    assertTrue(PSDbComponentCollection.equalsIgnoreOrder(a.iterator(), b.iterator()));
  }

  @Test
  public void equalsIgnoreOrderDetectsMissingAndExtra() {
    List<String> a = Arrays.asList("x", "y");
    List<String> b = Arrays.asList("x", "z");
    assertFalse(PSDbComponentCollection.equalsIgnoreOrder(a.iterator(), b.iterator()));
  }

  @Test
  public void equalsIgnoreOrderHandlesDuplicates() {
    List<String> a = Arrays.asList("x", "x", "y");
    List<String> b = Arrays.asList("x", "y", "x");
    List<String> c = Arrays.asList("x", "y", "y");
    assertTrue(PSDbComponentCollection.equalsIgnoreOrder(a.iterator(), b.iterator()));
    assertFalse(PSDbComponentCollection.equalsIgnoreOrder(a.iterator(), c.iterator()));
  }

  @Test
  public void hashCodeIgnoresOrder() {
    List<String> a = Arrays.asList("a", "b", "c");
    List<String> b = Arrays.asList("c", "a", "b");
    assertEquals(
        PSDbComponentCollection.hashCodeIgnoresOrder(a.iterator()),
        PSDbComponentCollection.hashCodeIgnoresOrder(b.iterator()));
    assertEquals(0, PSDbComponentCollection.hashCodeIgnoresOrder(Collections.emptyIterator()));
  }

  @Test
  public void hashCodeNullIteratorRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> PSDbComponentCollection.hashCodeIgnoresOrder(null));
  }

  @Test
  public void stringCtorRejectsClassThatDoesNotImplementIpsDbComponent() {
    // Class loads successfully but is not IPSDbComponent — wrapped as ClassNotFoundException
    // so callers that only catch ClassNotFoundException still see the failure path.
    ClassNotFoundException ex =
        assertThrows(
            ClassNotFoundException.class,
            () -> new PSDbComponentCollection(String.class.getName(), "String"));
    assertTrue(
        ex.getMessage().contains("IPSDbComponent") || ex.getCause() instanceof ClassCastException);
  }

  @Test
  public void stringCtorRejectsMissingClass() {
    assertThrows(
        ClassNotFoundException.class,
        () -> new PSDbComponentCollection("com.percussion.does.not.Exist", "X"));
  }
}
