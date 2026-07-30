/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ItemFilterAdaptorSafeKeyTest {

  @Test
  void isSafeFilterKey_rejectsPathTraversal() {
    assertTrue(ItemFilterAdaptor.isSafeFilterKey("public"));
    assertTrue(ItemFilterAdaptor.isSafeFilterKey("0-11-301"));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey("../x"));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey("a/b"));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey("a\\b"));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey(""));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey("   "));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey("a\u0000b"));
    assertFalse(ItemFilterAdaptor.isSafeFilterKey(null));
  }
}
