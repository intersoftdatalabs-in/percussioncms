/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class ServerConfigAdaptorSafeKeyTest {

  @Test
  void isSafeConfigKey_allowsEnumNamesRejectsJunk() {
    assertTrue(ServerConfigAdaptor.isSafeConfigKey("LOG_CONFIG"));
    assertTrue(ServerConfigAdaptor.isSafeConfigKey("TIDY_CONFIG"));
    assertFalse(ServerConfigAdaptor.isSafeConfigKey("../x"));
    assertFalse(ServerConfigAdaptor.isSafeConfigKey("a/b"));
    assertFalse(ServerConfigAdaptor.isSafeConfigKey("a b"));
    assertFalse(ServerConfigAdaptor.isSafeConfigKey(""));
    assertFalse(ServerConfigAdaptor.isSafeConfigKey(null));
  }
}
