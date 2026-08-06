/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class MainInstallExitCodeTest {

  @Test
  void successWhenZeroAndNoError() {
    assertEquals(0, Main.resolveInstallExitCode(0, false, 0));
    assertEquals(0, Main.resolveInstallExitCode(null, false, 0));
  }

  @Test
  void antNonZeroWins() {
    assertEquals(3, Main.resolveInstallExitCode(3, false, 0));
    assertEquals(2, Main.resolveInstallExitCode(2, true, 99));
  }

  @Test
  void errorFlagProducesNonZero() {
    assertEquals(1, Main.resolveInstallExitCode(0, true, 0));
    assertEquals(7, Main.resolveInstallExitCode(0, true, 7));
    assertEquals(5, Main.resolveInstallExitCode(null, true, 5));
  }

  @Test
  void sharedProcessCodeAloneIsHonored() {
    assertEquals(4, Main.resolveInstallExitCode(0, false, 4));
  }
}
