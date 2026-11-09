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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the DCE login-panel credential hygiene helpers (#5796): blank detection
 * must not materialize the typed password as a {@link String}, and every consumed copy must be
 * zeroable.
 */
public class PSContentExplorerLoginPanelPasswordTest {

  @Test
  public void blankPasswordVariantsAreDetected() {
    assertTrue(PSContentExplorerLoginPanel.isPasswordBlank(null));
    assertTrue(PSContentExplorerLoginPanel.isPasswordBlank(new char[0]));
    assertTrue(PSContentExplorerLoginPanel.isPasswordBlank("   ".toCharArray()));
    assertTrue(PSContentExplorerLoginPanel.isPasswordBlank("\t \n".toCharArray()));
  }

  @Test
  public void nonBlankPasswordIsAccepted() {
    assertFalse(
        PSContentExplorerLoginPanel.isPasswordBlank(new char[] {'t', 'e', 's', 't', '-', 'p', 'w'}));
    assertFalse(
        PSContentExplorerLoginPanel.isPasswordBlank(
            new char[] {' ', ' ', 't', 'e', 's', 't', '-', 'p', 'w', ' ', ' '}));
  }

  @Test
  public void clearPasswordZeroesEveryChar() {
    char[] candidate = {'t', 'e', 's', 't', '-', 'p', 'w'};
    PSContentExplorerLoginPanel.clearPassword(candidate);
    for (char c : candidate) {
      assertTrue(c == '\0', "cleared password copy must contain only NUL chars");
    }
  }

  @Test
  public void clearPasswordToleratesNull() {
    PSContentExplorerLoginPanel.clearPassword(null);
  }
}
