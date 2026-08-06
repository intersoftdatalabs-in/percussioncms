/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ViewAdaptorSafeKeyTest {

  @Test
  void isSafeViewKey_rejectsPathTraversal() {
    assertTrue(ViewAdaptor.isSafeViewKey("My View"));
    assertTrue(ViewAdaptor.isSafeViewKey("0-11-301"));
    assertFalse(ViewAdaptor.isSafeViewKey("../x"));
    assertFalse(ViewAdaptor.isSafeViewKey("a/b"));
    assertFalse(ViewAdaptor.isSafeViewKey("a\\b"));
    assertFalse(ViewAdaptor.isSafeViewKey(""));
    assertFalse(ViewAdaptor.isSafeViewKey("   "));
    assertFalse(ViewAdaptor.isSafeViewKey("a\u0000b"));
    assertFalse(ViewAdaptor.isSafeViewKey(null));
  }
}
