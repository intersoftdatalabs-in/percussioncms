/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class DisplayFormatAdaptorSafeKeyTest {

  @Test
  void isSafeDisplayFormatKey_rejectsPathTraversal() {
    assertTrue(DisplayFormatAdaptor.isSafeDisplayFormatKey("Default"));
    assertTrue(DisplayFormatAdaptor.isSafeDisplayFormatKey("0-11-301"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("../x"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a/b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a\\b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey(""));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("   "));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey("a\u0000b"));
    assertFalse(DisplayFormatAdaptor.isSafeDisplayFormatKey(null));
  }
}
