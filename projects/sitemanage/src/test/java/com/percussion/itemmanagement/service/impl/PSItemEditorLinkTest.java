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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Syntax rules for EditorHost link fields (#4753). */
@Tag("UnitTest")
class PSItemEditorLinkTest {

  @Test
  void blankAndKnownShapesAreAccepted() {
    assertNull(PSItemEditorLink.syntaxRejection("page", "", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("page", "  ", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("page", "594", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("page", "0-101-594", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("page", "//Sites/Example/index", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("page", "/Sites/Example/index", "link"));
    assertNull(PSItemEditorLink.syntaxRejection("title", "not-a-link", "text"));
  }

  @Test
  void schemesAndTraversalAreRejected() {
    String bad = PSItemEditorLink.syntaxRejection("page", "javascript:alert(1)", "link");
    assertTrue(bad != null && bad.contains("page"));
    assertTrue(PSItemEditorLink.syntaxRejection("page", "//Sites/../secret", "link") != null);
    assertTrue(PSItemEditorLink.syntaxRejection("page", "https://example.test/a", "link") != null);
    assertTrue(PSItemEditorLink.syntaxRejection("page", "not a path", "link") != null);
  }

  @Test
  void pathVersusId() {
    assertTrue(PSItemEditorLink.isPath("//Sites/Example/index"));
    assertTrue(PSItemEditorLink.isPath("/Sites/Example"));
    assertFalse(PSItemEditorLink.isPath("594"));
    assertFalse(PSItemEditorLink.isPath("0-101-594"));
  }
}
