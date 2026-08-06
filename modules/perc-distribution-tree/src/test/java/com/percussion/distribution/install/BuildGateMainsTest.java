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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Ensures build-gate mains do not call {@link System#exit} under the default (Maven in-process)
 * policy — that would kill {@code exec-maven-plugin:java} and abort the reactor after a successful
 * JDBC verify.
 */
class BuildGateMainsTest {

  @AfterEach
  void clearProp() {
    System.clearProperty("perc.build.gate.systemExit");
  }

  @Test
  void complete_success_returnsWithoutThrowing() {
    System.clearProperty("perc.build.gate.systemExit");
    assertDoesNotThrow(() -> BuildGateMains.complete(0, "TestGate"));
  }

  @Test
  void complete_failure_throwsWithExitCode() {
    System.clearProperty("perc.build.gate.systemExit");
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> BuildGateMains.complete(2, "TestGate"));
    assertTrue(ex.getMessage().contains("TestGate"));
    assertTrue(ex.getMessage().contains("2"));
  }
}
