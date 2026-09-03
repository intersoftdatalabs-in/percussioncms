/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class RelationshipTypeAdaptorSafeKeyTest {

  @Test
  void isSafeRelationshipTypeKey_allowsNameAndGuidRejectsTraversal() {
    assertTrue(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("ActiveAssembly"));
    assertTrue(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("0-11-1"));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("../x"));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("a/b"));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("a\\b"));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey(""));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("   "));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey("a\u0000b"));
    assertFalse(RelationshipTypeAdaptor.isSafeRelationshipTypeKey(null));
  }

  @Test
  void requireValidName_rejectsWhitespaceAndWildcards() {
    assertEquals("MyRel", RelationshipTypeAdaptor.requireValidName("MyRel"));
    assertThrows(
        IllegalArgumentException.class, () -> RelationshipTypeAdaptor.requireValidName(" "));
    assertThrows(
        IllegalArgumentException.class, () -> RelationshipTypeAdaptor.requireValidName("My Rel"));
    assertThrows(
        IllegalArgumentException.class, () -> RelationshipTypeAdaptor.requireValidName("My*Rel"));
  }
}
