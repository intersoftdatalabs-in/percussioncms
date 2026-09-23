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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemEditorNumericTest {

  @Test
  void blankAndInRangeIntegerAreAccepted() {
    assertNull(PSItemEditorNumeric.rejectionMessage("qty", "", "integer", "0", "10"));
    assertNull(PSItemEditorNumeric.rejectionMessage("qty", " 10 ", "number", "0", "10"));
    assertNull(PSItemEditorNumeric.rejectionMessage("qty", "4", "integer", null, null));
  }

  @Test
  void nonNumericAndOutOfRangeAreRejected() {
    String bad = PSItemEditorNumeric.rejectionMessage("qty", "abc", "integer", "0", "10");
    assertTrue(bad.contains("'qty'"));
    assertTrue(bad.contains("not a valid number"));
    String range = PSItemEditorNumeric.rejectionMessage("qty", "11", "integer", "0", "10");
    assertTrue(range.contains("outside the allowed range"));
    assertTrue(
        PSItemEditorNumeric.rejectionMessage("qty", "1.5", "number", null, null)
            .contains("not a valid number"));
    assertTrue(
        PSItemEditorNumeric.rejectionMessage("rate", "nope", "float", null, null)
            .contains("not a valid number"));
  }

  @Test
  void floatFractionInsideBoundsIsAccepted() {
    assertNull(PSItemEditorNumeric.rejectionMessage("rate", "1.5", "float", "0", "2"));
    assertNull(PSItemEditorNumeric.rejectionMessage("title", "12", "text", null, null));
  }

  @Test
  void integerBeyondLongIsOutOfRangeWhenUnbounded() {
    String huge = "9223372036854775808";
    String message = PSItemEditorNumeric.rejectionMessage("qty", huge, "integer", null, null);
    assertTrue(message.contains("outside the allowed range"));
  }
}
