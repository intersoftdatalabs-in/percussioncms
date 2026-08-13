/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.server.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.extension.PSExtensionManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed action-set request-handler maps after #3213 Xlint cleanup.
 */
class PSActionSetRequestHandlerTypedTest {

  @Test
  @DisplayName("init accepts typed request roots and stores them for iteration")
  void initAcceptsTypedRequestRoots() throws Exception {
    PSActionSetRequestHandler handler =
        new PSActionSetRequestHandler(null, new PSExtensionManager());
    List<String> roots = new ArrayList<>();
    roots.add("sys_action");
    handler.init(roots, null);

    Iterator<String> it = handler.getRequestRoots();
    assertTrue(it.hasNext());
    assertEquals("sys_action", it.next());
    assertFalse(it.hasNext());
  }

  @Test
  @DisplayName("init rejects an empty request-root collection")
  void initRejectsEmptyRoots() {
    PSActionSetRequestHandler handler =
        new PSActionSetRequestHandler(null, new PSExtensionManager());
    assertThrows(IllegalArgumentException.class, () -> handler.init(new ArrayList<>(), null));
  }

  @Test
  @DisplayName("init rejects a non-String request root")
  void initRejectsNonStringRoot() {
    PSActionSetRequestHandler handler =
        new PSActionSetRequestHandler(null, new PSExtensionManager());
    List<Object> roots = new ArrayList<>();
    roots.add(42);
    assertThrows(IllegalArgumentException.class, () -> handler.init(roots, null));
  }

  @Test
  @DisplayName("constructor rejects a null extension manager")
  void ctorRejectsNullExtensionManager() {
    assertThrows(IllegalArgumentException.class, () -> new PSActionSetRequestHandler(null, null));
  }
}
