/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ControlAdaptorSafeKeyTest {

  @Test
  void isSafeControlKey_allowsNameRejectsTraversal() {
    assertTrue(ControlAdaptor.isSafeControlKey("sys_EditBox"));
    assertTrue(ControlAdaptor.isSafeControlKey("myCustomControl"));
    assertTrue(ControlAdaptor.isSafeControlKey("sys.foo-bar"));
    assertFalse(ControlAdaptor.isSafeControlKey("../x"));
    assertFalse(ControlAdaptor.isSafeControlKey("a/b"));
    assertFalse(ControlAdaptor.isSafeControlKey("a\\b"));
    assertFalse(ControlAdaptor.isSafeControlKey(""));
    assertFalse(ControlAdaptor.isSafeControlKey("   "));
    assertFalse(ControlAdaptor.isSafeControlKey("a\u0000b"));
    assertFalse(ControlAdaptor.isSafeControlKey(null));
    assertFalse(ControlAdaptor.isSafeControlKey("has space"));
    assertFalse(ControlAdaptor.isSafeControlKey("ctrl\u00e9"));
  }
}
