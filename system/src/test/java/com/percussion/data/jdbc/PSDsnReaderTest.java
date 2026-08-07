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
package com.percussion.data.jdbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link PSDsnReader} (typed DSN list parsing). */
@Tag("UnitTest")
class PSDsnReaderTest {

  @TempDir Path tempDir;

  @Test
  void returnsNullWhenPathMissing() {
    assertNull(new PSDsnReader(null).getDsnList());
    assertNull(new PSDsnReader("").getDsnList());
    assertNull(new PSDsnReader(tempDir.resolve("no-such.ini").toString()).getDsnList());
  }

  @Test
  void parsesUserAndSystemDsnSections() throws Exception {
    Path ini = tempDir.resolve("odbc.ini");
    String content =
        String.join(
            "\n",
            "[ODBC Data Sources]",
            "AppDb=PostgreSQL",
            "Archive=MySQL",
            "",
            "[AppDb]",
            "Driver=/usr/lib/libpsqlodbc.so",
            "",
            "[ODBC System Data Sources]",
            "SystemRx=Oracle",
            "",
            "[SystemRx]",
            "Driver=/usr/lib/libsqora.so",
            "");
    Files.writeString(ini, content, StandardCharsets.UTF_8);

    String[] dsns = new PSDsnReader(ini.toString()).getDsnList();
    assertArrayEquals(new String[] {"AppDb", "Archive", "SystemRx"}, dsns);
  }

  @Test
  void ignoresMalformedLinesInDsnSection() throws Exception {
    Path ini = tempDir.resolve("odbc.ini");
    String content =
        String.join(
            "\n",
            "[ODBC Data Sources]",
            "Good=Driver",
            "NoEqualsHere",
            "=emptyName",
            "AlsoGood=Other",
            "");
    Files.writeString(ini, content, StandardCharsets.UTF_8);

    String[] dsns = new PSDsnReader(ini.toString()).getDsnList();
    assertArrayEquals(new String[] {"Good", "AlsoGood"}, dsns);
  }
}
