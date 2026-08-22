/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
package com.percussion.pagemanagement.assembler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pagemanagement.data.PSPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * percPage dispatcher must not NPE with Validate.notNull's default message when FastForward
 * rffHome has no percPage template id (#3719).
 */
@Tag("UnitTest")
class PSAssemblyItemBridgeRequireTemplateIdTest {

  @Test
  @DisplayName("requirePercPageTemplateId returns the page template id")
  void returnsTemplateId() {
    PSPage page = new PSPage();
    page.setTemplateId("perc-template-1");
    assertEquals("perc-template-1", PSAssemblyItemBridge.requirePercPageTemplateId(page));
  }

  @Test
  @DisplayName("missing template id is a clear IllegalStateException, not Validate NPE")
  void missingTemplateId() {
    PSPage page = new PSPage();
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> PSAssemblyItemBridge.requirePercPageTemplateId(page));
    assertTrue(ex.getMessage().contains("FastForward"));
    assertTrue(
        ex.getMessage() == null
            || !ex.getMessage().contains("The validated object is null"));
  }

  @Test
  @DisplayName("null page is a clear IllegalStateException")
  void nullPage() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> PSAssemblyItemBridge.requirePercPageTemplateId(null));
    assertTrue(ex.getMessage().contains("page is null"));
  }
}
