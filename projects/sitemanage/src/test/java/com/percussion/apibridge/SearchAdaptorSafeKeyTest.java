/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class SearchAdaptorSafeKeyTest {

  @Test
  void isSafeSearchKey_rejectsPathTraversal() {
    assertTrue(SearchAdaptor.isSafeSearchKey("All Content"));
    assertTrue(SearchAdaptor.isSafeSearchKey("0-11-301"));
    assertFalse(SearchAdaptor.isSafeSearchKey("../x"));
    assertFalse(SearchAdaptor.isSafeSearchKey("a/b"));
    assertFalse(SearchAdaptor.isSafeSearchKey("a\\b"));
    assertFalse(SearchAdaptor.isSafeSearchKey(""));
    assertFalse(SearchAdaptor.isSafeSearchKey("   "));
    assertFalse(SearchAdaptor.isSafeSearchKey("a\u0000b"));
    assertFalse(SearchAdaptor.isSafeSearchKey(null));
  }
}
