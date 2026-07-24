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
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * FR-015 / T080: enterprise dbprops samples for MySQL/MSSQL/Oracle remain present and retain
 * external backends (not rewritten to H2 defaults).
 */
@Tag("UnitTest")
public class ExternalDbSamplePropsPackagingTest {

  @Test
  void mysqlSampleIsExternalMysql() throws Exception {
    assertSample(
        Path.of("src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.mysql.properties"),
        "MYSQL",
        "mysql");
  }

  @Test
  void sqlServerSampleIsExternalMssql() throws Exception {
    assertSample(
        Path.of(
            "src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.sqlserver.properties"),
        "MSSQL",
        "sqlserver");
  }

  @Test
  void oracleSampleIsExternalOracle() throws Exception {
    assertSample(
        Path.of(
            "src/main/resources/distribution/rxconfig/Installer/samples/rxrepository.oracle.properties"),
        "ORACLE",
        "oracle:thin");
  }

  private static void assertSample(Path relative, String backend, String driverName)
      throws Exception {
    assertTrue(Files.isRegularFile(relative), "missing sample: " + relative);
    Properties p = new Properties();
    try (var in = Files.newInputStream(relative)) {
      p.load(in);
    }
    assertEquals(backend, p.getProperty("DB_BACKEND"));
    assertEquals(driverName, p.getProperty("DB_DRIVER_NAME"));
    assertTrue(
        p.getProperty("DB_DRIVER_CLASS_NAME") != null
            && !p.getProperty("DB_DRIVER_CLASS_NAME").toLowerCase().contains("h2"),
        "sample must not use H2 driver class");
  }
}
