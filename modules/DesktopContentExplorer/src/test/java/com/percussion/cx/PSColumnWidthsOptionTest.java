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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed column-width option map used by the applet display panels. */
public class PSColumnWidthsOptionTest {

  @Test
  public void optionClassIsFinal() {
    assertTrue(
        Modifier.isFinal(PSColumnWidthsOption.class.getModifiers()),
        "PSColumnWidthsOption must be final to avoid this-escape in the Element ctor");
  }

  @Test
  public void addGetAndRemoveItemColumnWidths() {
    PSColumnWidthsOption option = new PSColumnWidthsOption();
    assertFalse(option.haveColumnWidths());

    List<String> widths = Arrays.asList("100", "200", "50");
    option.addItemColumnWidths("//Sites/Home", widths);

    assertTrue(option.haveColumnWidths());
    List<String> stored = option.getItemColumnWidths("//Sites/Home");
    assertEquals(widths, stored);

    option.removeItemColumnWidths("//Sites/Home");
    assertNull(option.getItemColumnWidths("//Sites/Home"));
    assertFalse(option.haveColumnWidths());
  }

  @Test
  public void addItemColumnWidthsRejectsBlankPath() {
    PSColumnWidthsOption option = new PSColumnWidthsOption();
    assertThrows(
        IllegalArgumentException.class, () -> option.addItemColumnWidths("  ", Arrays.asList("1")));
    assertThrows(
        IllegalArgumentException.class, () -> option.addItemColumnWidths(null, Arrays.asList("1")));
  }
}
