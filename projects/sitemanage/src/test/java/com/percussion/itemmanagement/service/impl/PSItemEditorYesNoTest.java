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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemEditorYesNoTest {

  @Test
  void booleanAndYesNoAliasesAreYesNoTypes() {
    assertTrue(PSItemEditorYesNo.isYesNoDataType("boolean"));
    assertTrue(PSItemEditorYesNo.isYesNoDataType("YES-NO"));
    assertTrue(PSItemEditorYesNo.isYesNoDataType("yesno"));
    assertFalse(PSItemEditorYesNo.isYesNoDataType("integer"));
    assertFalse(PSItemEditorYesNo.isYesNoDataType(""));
    assertFalse(PSItemEditorYesNo.isYesNoDataType(null));
  }

  @Test
  void blankYesAndNoAreCanonical() {
    assertEquals("", PSItemEditorYesNo.canonicalValue(null));
    assertEquals("", PSItemEditorYesNo.canonicalValue("  "));
    assertEquals("yes", PSItemEditorYesNo.canonicalValue("Yes"));
    assertEquals("no", PSItemEditorYesNo.canonicalValue("NO"));
    assertNull(PSItemEditorYesNo.canonicalValue("maybe"));
    assertNull(PSItemEditorYesNo.canonicalValue("true"));
  }

  @Test
  void blankYesAndNoAreAcceptedAndGarbageIsRejected() {
    assertNull(PSItemEditorYesNo.rejectionMessage("flag", "", "boolean"));
    assertNull(PSItemEditorYesNo.rejectionMessage("flag", "yes", "boolean"));
    assertNull(PSItemEditorYesNo.rejectionMessage("flag", " no ", "yes-no"));
    assertNull(PSItemEditorYesNo.rejectionMessage("qty", "maybe", "integer"));
    String bad = PSItemEditorYesNo.rejectionMessage("flag", "maybe", "boolean");
    assertTrue(bad.contains("'flag'"));
    assertTrue(bad.contains("not a valid yes-no value"));
  }
}
