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
package com.percussion.wrapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PercArgs} flag parsing, including independent flag handling after
 * the fall-through fix from issue #2025 / PR #2057.
 */
public class PercArgsTest {

  @Test
  @DisplayName("--help alone enables help without start/stop")
  void helpFlagOnly() {
    PercArgs args = new PercArgs(new String[] {"--help"});
    assertTrue(args.isHelp());
    assertFalse(args.isStartServer());
    assertFalse(args.isForce());
    assertFalse(args.isDebugStartup());
  }

  @Test
  @DisplayName("--force and --debugWrapper do not fall through into --start")
  void forceAndDebugDoNotFallThrough() {
    PercArgs args = new PercArgs(new String[] {"--force", "--debugWrapper", "--status"});
    assertTrue(args.isForce());
    assertTrue(args.isDebugStartup());
    assertTrue(args.isStatus());
    assertFalse(args.isStartServer());
    assertFalse(args.isStartDTS());
    assertFalse(args.isHelp());
  }

  @Test
  @DisplayName("--start enables all start flags")
  void startEnablesAll() {
    PercArgs args = new PercArgs(new String[] {"--start"});
    assertTrue(args.isStartServer());
    assertTrue(args.isStartDTS());
    assertTrue(args.isStartStagingDTS());
    assertFalse(args.isHelp());
  }

  @Test
  @DisplayName("unknown args are retained in filteredArgs")
  void unknownArgsFiltered() {
    PercArgs args = new PercArgs(new String[] {"--startServer", "-Dfoo=bar", "extra"});
    assertTrue(args.isStartServer());
    assertArrayEquals(new String[] {"-Dfoo=bar", "extra"}, args.getFilteredArgs());
  }

  @Test
  @DisplayName("--jettyHelp is rewritten to --help in filtered args")
  void jettyHelpRewritten() {
    PercArgs args = new PercArgs(new String[] {"--jettyHelp", "--status"});
    assertTrue(args.isStatus());
    assertArrayEquals(new String[] {"--help"}, args.getFilteredArgs());
  }
}
