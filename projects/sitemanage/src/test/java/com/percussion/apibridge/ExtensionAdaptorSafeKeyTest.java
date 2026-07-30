/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ExtensionAdaptorSafeKeyTest {

  @Test
  void isSafeExtensionKey_allowsFqnRejectsTraversal() {
    assertTrue(ExtensionAdaptor.isSafeExtensionKey("sys_add"));
    assertTrue(ExtensionAdaptor.isSafeExtensionKey("Java/global/percussion/sys_add"));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey("../x"));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey("a\\b"));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey(""));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey("   "));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey("a\u0000b"));
    assertFalse(ExtensionAdaptor.isSafeExtensionKey(null));
  }
}
