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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Vector;
import org.junit.jupiter.api.Test;

/** Pure helpers for {@link PSFolderPropertiesPanel} after rawtypes cleanup. */
public class PSFolderPropertiesPanelTest {

  @Test
  public void stringCellReadsAndNullSafe() {
    Vector<Object> row = new Vector<>();
    row.add("name");
    row.add("value");
    row.add(null);

    assertEquals("name", PSFolderPropertiesPanel.stringCell(row, 0));
    assertEquals("value", PSFolderPropertiesPanel.stringCell(row, 1));
    assertNull(PSFolderPropertiesPanel.stringCell(row, 2));
    assertNull(PSFolderPropertiesPanel.stringCell(row, 99));
    assertNull(PSFolderPropertiesPanel.stringCell(null, 0));
  }
}
