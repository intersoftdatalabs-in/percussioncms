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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.itemmanagement.data.PSItemEditorField;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.itemmanagement.data.PSItemRevisionCompareResult;
import com.percussion.itemmanagement.data.PSItemRevisionFieldDiff;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemRevisionCompareTest {

  @Test
  void revisionGuidReplacesFirstSegment() {
    assertEquals("3-101-708", PSItemRevisionCompare.revisionGuid("1-101-708", 3));
    assertEquals("3-101-42", PSItemRevisionCompare.revisionGuid("42", 3));
  }

  @Test
  void revisionGuidRejectsBlankOrNonPositive() {
    assertThrows(IllegalArgumentException.class, () -> PSItemRevisionCompare.revisionGuid("", 1));
    assertThrows(IllegalArgumentException.class, () -> PSItemRevisionCompare.revisionGuid("42", 0));
  }

  @Test
  void diffMarksChangedAndUnchangedFields() {
    PSItemEditorFields left = new PSItemEditorFields();
    left.setFields(
        List.of(new PSItemEditorField("sys_title", "Home"), new PSItemEditorField("body", "old")));
    PSItemEditorFields right = new PSItemEditorFields();
    right.setFields(
        List.of(new PSItemEditorField("sys_title", "Home"), new PSItemEditorField("body", "new")));

    PSItemRevisionCompareResult out = PSItemRevisionCompare.diff("1-101-42", 1, 2, left, right);
    assertEquals("1-101-42", out.getItemId());
    assertEquals(1, out.getRev1());
    assertEquals(2, out.getRev2());
    assertEquals(2, out.getFields().size());
    PSItemRevisionFieldDiff body = out.getFields().get(0);
    assertEquals("body", body.getName());
    assertEquals("old", body.getLeftValue());
    assertEquals("new", body.getRightValue());
    assertTrue(body.isChanged());
    PSItemRevisionFieldDiff title = out.getFields().get(1);
    assertEquals("sys_title", title.getName());
    assertFalse(title.isChanged());
  }

  @Test
  void diffIncludesFieldsPresentOnOnlyOneSide() {
    PSItemEditorFields left = new PSItemEditorFields();
    left.setFields(List.of(new PSItemEditorField("onlyLeft", "a")));
    PSItemEditorFields right = new PSItemEditorFields();
    right.setFields(List.of(new PSItemEditorField("onlyRight", "b")));

    PSItemRevisionCompareResult out = PSItemRevisionCompare.diff("42", 1, 2, left, right);
    assertEquals(2, out.getFields().size());
    assertEquals("onlyLeft", out.getFields().get(0).getName());
    assertEquals("a", out.getFields().get(0).getLeftValue());
    assertEquals("", out.getFields().get(0).getRightValue());
    assertTrue(out.getFields().get(0).isChanged());
    assertEquals("onlyRight", out.getFields().get(1).getName());
    assertEquals("", out.getFields().get(1).getLeftValue());
    assertEquals("b", out.getFields().get(1).getRightValue());
  }
}
