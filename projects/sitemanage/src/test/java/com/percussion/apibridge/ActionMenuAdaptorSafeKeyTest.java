/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ActionMenuAdaptorSafeKeyTest {

  @Test
  void isSafeMenuKey_rejectsPathTraversal() {
    assertTrue(ActionMenuAdaptor.isSafeMenuKey("Edit"));
    assertTrue(ActionMenuAdaptor.isSafeMenuKey("301"));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey("../x"));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey("a/b"));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey("a\\b"));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey(""));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey("   "));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey("a\u0000b"));
    assertFalse(ActionMenuAdaptor.isSafeMenuKey(null));
  }
}
