/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
