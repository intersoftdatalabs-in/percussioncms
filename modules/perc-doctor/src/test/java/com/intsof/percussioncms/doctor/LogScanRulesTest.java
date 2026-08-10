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
package com.intsof.percussioncms.doctor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogScanRulesTest {

  @Test
  void cleanInfoOnly() {
    String text =
        "2026-08-09 01:00:00,001 INFO  [Server] Started @7879ms\n"
            + "2026-08-09 01:00:01,000 INFO  [PSServer] Server is ready\n";
    assertNull(LogScanRules.findStartupError(text));
    assertTrue(LogScanRules.isClean(text));
  }

  @Test
  void errorLineDetected() {
    String text = "INFO boot\n" + "2026-08-09 01:00:01,000 ERROR [PSSomething] boom failed\n";
    String match = LogScanRules.findStartupError(text);
    assertNotNull(match);
    assertTrue(match.contains("ERROR"));
    assertTrue(match.contains("boom failed"));
    assertFalse(LogScanRules.isClean(text));
  }

  @Test
  void fatalAndSevere() {
    assertNotNull(LogScanRules.findStartupError("FATAL [Init] cannot continue\n"));
    assertNotNull(LogScanRules.findStartupError("SEVERE: something broke\n"));
  }

  @Test
  void contextMarkers() {
    assertTrue(
        LogScanRules.findStartupError("WARN Failed startup of context Rhythmyx\n")
            .contains("Failed startup of context"));
    assertNotNull(
        LogScanRules.findStartupError("BeanCurrentlyInCreationException: folderHelper\n"));
  }

  @Test
  void emptyIsClean() {
    assertNull(LogScanRules.findStartupError(""));
    assertNull(LogScanRules.findStartupError(null));
    assertTrue(LogScanRules.isClean(null));
  }

  @Test
  void crlfNormalized() {
    String match = LogScanRules.findStartupError("INFO ok\r\nERROR [x] bad\r\n");
    assertNotNull(match);
    assertTrue(match.contains("ERROR"));
  }
}
