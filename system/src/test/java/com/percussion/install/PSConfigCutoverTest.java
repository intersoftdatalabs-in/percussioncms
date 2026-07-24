/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Multi-file cutover consistency (T048 / T060 / QC-009). */
@Tag("UnitTest")
public class PSConfigCutoverTest {

  @TempDir Path installRoot;

  @Test
  void cutoverUpdatesRxrepositoryAndPercDs() throws Exception {
    Path rx = installRoot.resolve(PSConfigCutover.RXREPOSITORY_RELATIVE);
    Files.createDirectories(rx.getParent());
    Files.writeString(
        rx,
        "DB_BACKEND=DERBY\nDB_DRIVER_NAME=derby\nDB_SERVER=//localhost:1527/CMDB\n",
        StandardCharsets.UTF_8);

    Path perc = installRoot.resolve(PSConfigCutover.PERC_DS_RELATIVE);
    Files.createDirectories(perc.getParent());
    Files.writeString(
        perc,
        "perc.ds.1.driver.name=derby\nperc.ds.1.driver.class=org.apache.derby.jdbc.EmbeddedDriver\n"
            + "perc.ds.1.server=//localhost:1527/CMDB\nperc.ds.1.uid=\nperc.ds.1.pwd=\n",
        StandardCharsets.UTF_8);

    Properties h2 = new Properties();
    h2.setProperty("DB_BACKEND", "H2");
    h2.setProperty("DB_DRIVER_NAME", "h2");
    h2.setProperty("DB_DRIVER_CLASS_NAME", "org.h2.Driver");
    h2.setProperty("DB_SERVER", "file:/tmp/test/CMDB;DB_CLOSE_ON_EXIT=FALSE");
    h2.setProperty("UID", "sa");
    h2.setProperty("PWD", "");

    PSConfigCutover.Result result = PSConfigCutover.cutoverToH2(installRoot, h2);
    assertTrue(result.filesWritten().size() >= 2);

    Properties liveRx = new Properties();
    try (var in = Files.newInputStream(rx)) {
      liveRx.load(in);
    }
    assertEquals("H2", liveRx.getProperty("DB_BACKEND"));
    assertEquals("h2", liveRx.getProperty("DB_DRIVER_NAME"));

    Properties livePerc = new Properties();
    try (var in = Files.newInputStream(perc)) {
      livePerc.load(in);
    }
    assertEquals("h2", livePerc.getProperty("perc.ds.1.driver.name"));
    assertEquals("org.h2.Driver", livePerc.getProperty("perc.ds.1.driver.class"));
    assertTrue(livePerc.getProperty("perc.ds.1.server").contains("file:"));
  }
}
