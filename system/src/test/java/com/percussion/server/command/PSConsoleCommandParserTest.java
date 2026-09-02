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
package com.percussion.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.IPSConsoleCommand;
import com.percussion.server.IPSServerErrors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for the typed console command map after #3213 Xlint cleanup.
 */
class PSConsoleCommandParserTest {

  @Test
  @DisplayName("parse rejects a null command")
  void parseRejectsNull() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse(null));
    assertEquals(IPSServerErrors.RCONSOLE_CMD_EMPTY, ex.getErrorCode());
    assertSame(ServerErrorCodes.RCONSOLE_CMD_EMPTY, ex.getTypedErrorCode());
  }

  @Test
  @DisplayName("parse rejects a blank command")
  void parseRejectsBlank() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse("   "));
    assertEquals(IPSServerErrors.RCONSOLE_CMD_EMPTY, ex.getErrorCode());
    assertSame(ServerErrorCodes.RCONSOLE_CMD_EMPTY, ex.getTypedErrorCode());
  }

  @Test
  @DisplayName("parse looks up a leaf command class from the typed command map")
  void parseLeafCommand() throws PSIllegalArgumentException {
    IPSConsoleCommand cmd = PSConsoleCommandParser.parse("show version");
    assertInstanceOf(PSConsoleCommandShowVersion.class, cmd);
  }

  @Test
  @DisplayName("parse requires a sub-command when the map value is a prompt string")
  void parseRequiresSubCommand() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse("start"));
    assertEquals(IPSServerErrors.RCONSOLE_SUBCMD_REQD, ex.getErrorCode());
    assertSame(ServerErrorCodes.RCONSOLE_SUBCMD_REQD, ex.getTypedErrorCode());
  }

  @Test
  @DisplayName("parse rejects an unknown base command")
  void parseRejectsUnknownBase() {
    PSIllegalArgumentException ex =
        assertThrows(PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse("bogus"));
    assertEquals(IPSServerErrors.RCONSOLE_INVALID_CMD, ex.getErrorCode());
    assertSame(ServerErrorCodes.RCONSOLE_INVALID_CMD, ex.getTypedErrorCode());
  }

  @Test
  @DisplayName("parse rejects an unknown sub-command")
  void parseRejectsUnknownSubCommand() {
    PSIllegalArgumentException ex =
        assertThrows(
            PSIllegalArgumentException.class, () -> PSConsoleCommandParser.parse("start widgets"));
    assertEquals(IPSServerErrors.RCONSOLE_INVALID_SUBCMD, ex.getErrorCode());
    assertSame(ServerErrorCodes.RCONSOLE_INVALID_SUBCMD, ex.getTypedErrorCode());
  }

  @Test
  @DisplayName("parse constructs flush cache with typed key arguments")
  void parseFlushCacheCommand() throws PSIllegalArgumentException {
    IPSConsoleCommand cmd = PSConsoleCommandParser.parse("flush cache");
    assertInstanceOf(PSConsoleCommandFlushCache.class, cmd);
  }
}
