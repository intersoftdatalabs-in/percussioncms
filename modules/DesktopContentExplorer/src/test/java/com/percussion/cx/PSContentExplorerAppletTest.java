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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Behavioral tests for typed flagged-folder APIs on {@link PSContentExplorerApplet}. */
public class PSContentExplorerAppletTest {

  @Test
  public void toggleFlaggedFolderAddsAndRemoves() {
    PSContentExplorerApplet applet = new PSContentExplorerApplet(true);

    applet.toggleFlaggedFolder("42", true);
    Set<String> flagged = applet.getFlaggedFolderSet();
    assertTrue(flagged.contains("42"));
    assertEquals(1, flagged.size());

    // Defensive copy: mutating returned set must not affect applet state
    flagged.clear();
    assertTrue(applet.getFlaggedFolderSet().contains("42"));

    applet.toggleFlaggedFolder("42", false);
    assertFalse(applet.getFlaggedFolderSet().contains("42"));
    assertTrue(applet.getFlaggedFolderSet().isEmpty());
  }

  @Test
  public void toggleFlaggedFolderRejectsInvalidIds() {
    PSContentExplorerApplet applet = new PSContentExplorerApplet(true);

    assertThrows(IllegalArgumentException.class, () -> applet.toggleFlaggedFolder(null, true));
    assertThrows(IllegalArgumentException.class, () -> applet.toggleFlaggedFolder("  ", true));
    assertThrows(IllegalArgumentException.class, () -> applet.toggleFlaggedFolder("abc", true));
  }
}
