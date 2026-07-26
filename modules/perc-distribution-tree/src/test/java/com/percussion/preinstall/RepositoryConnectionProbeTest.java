/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class RepositoryConnectionProbeTest {

  @Test
  void embeddedH2IsSkipped() {
    Map<String, String> props = Map.of("perc.db.type", "h2");
    RepositoryConnectionProbe.ProbeResult r = RepositoryConnectionProbe.probe(props, 5);
    assertEquals(RepositoryConnectionProbe.ProbeStatus.SKIPPED_EMBEDDED, r.status());
    assertTrue(r.isSuccess());
  }

  @Test
  void missingDriverIsSkippedNotFailed() {
    Map<String, String> props = new HashMap<>();
    props.put("perc.db.type", "mysql");
    props.put("perc.db.host", "db.example.com");
    props.put("perc.db.port", "3306");
    props.put("perc.db.name", "percussion");
    props.put("perc.db.user", "cms");
    props.put("perc.db.password", "secret-password-value");
    props.put("perc.db.cms.driverClass", "com.example.NonExistentDriver");
    props.put("perc.db.dts.jdbcUrl", "jdbc:mysql://db.example.com:3306/percussion");

    RepositoryConnectionProbe.ProbeResult r = RepositoryConnectionProbe.probe(props, 5);
    assertEquals(RepositoryConnectionProbe.ProbeStatus.SKIPPED, r.status());
    assertFalse(r.message().contains("secret-password-value"));
    assertTrue(r.message().toLowerCase().contains("classpath") || r.message().contains("driver"));
  }

  @Test
  void buildJdbcUrlForStructuredBackends() {
    Map<String, String> mysql = new HashMap<>();
    mysql.put("perc.db.host", "h");
    mysql.put("perc.db.port", "3306");
    mysql.put("perc.db.name", "db");
    assertEquals(
        "jdbc:mysql://h:3306/db", RepositoryConnectionProbe.buildJdbcUrl("mysql", mysql));

    Map<String, String> pg = new HashMap<>();
    pg.put("perc.db.host", "h");
    pg.put("perc.db.port", "5432");
    pg.put("perc.db.name", "db");
    assertEquals(
        "jdbc:postgresql://h:5432/db",
        RepositoryConnectionProbe.buildJdbcUrl("postgresql", pg));

    Map<String, String> mssql = new HashMap<>();
    mssql.put("perc.db.host", "h");
    mssql.put("perc.db.port", "1433");
    mssql.put("perc.db.name", "db");
    assertEquals(
        "jdbc:sqlserver://h:1433;databaseName=db",
        RepositoryConnectionProbe.buildJdbcUrl("sqlserver", mssql));
  }

  @Test
  void safeSqlMessageRedactsPasswordFragments() {
    java.sql.SQLException ex =
        new java.sql.SQLException("login failed password=supersecret; host=x");
    String msg = RepositoryConnectionProbe.safeSqlMessage(ex);
    assertFalse(msg.contains("supersecret"));
    assertTrue(msg.contains("***"));
  }
}
