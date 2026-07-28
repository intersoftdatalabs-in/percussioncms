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
package com.percussion.ant.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for upgrade-safe JDBC driver merge in {@link PSConfigureDatasource}. Pre-#548 installs
 * preserve {@code config.xml} without H2/PostgreSQL entries; install must still configure H2.
 */
@Tag("UnitTest")
class PSConfigureDatasourceTest {

  @TempDir Path tempDir;

  /** Mirrors a pre-#548 field {@code config.xml} (drivers through derby only). */
  private static final String PRE_H2_CONFIG =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <PSXServerConfiguration id="1" serverType="System Master">
         <requestRoot>Rhythmyx</requestRoot>
         <JdbcDriverConfigs>
            <PSXJdbcDriverConfig className="oracle.jdbc.OracleDriver" containerTypeMapping="Oracle8" driverName="oracle:thin"/>
            <PSXJdbcDriverConfig className="net.sourceforge.jtds.jdbc.Driver" containerTypeMapping="MS SQLSERVER2000" driverName="jtds:sqlserver"/>
            <PSXJdbcDriverConfig className="com.ibm.db2.jcc.DB2Driver" containerTypeMapping="DB2" driverName="db2"/>
            <PSXJdbcDriverConfig className="com.mysql.jdbc.Driver" containerTypeMapping="MYSQL" driverName="mysql"/>
            <PSXJdbcDriverConfig className="com.microsoft.sqlserver.jdbc.SQLServerDriver" containerTypeMapping="MSSQLSERVER" driverName="sqlserver"/>
            <PSXJdbcDriverConfig className="org.apache.derby.jdbc.EmbeddedDriver" containerTypeMapping="DERBY" driverName="derby"/>
         </JdbcDriverConfigs>
      </PSXServerConfiguration>
      """;

  @Test
  void ensureAddsH2AndPostgresqlToPre548Config() throws Exception {
    Path config = tempDir.resolve("config.xml");
    Files.writeString(config, PRE_H2_CONFIG, StandardCharsets.UTF_8);

    PSConfigureDatasource.ResolvedJdbcDriver resolved =
        PSConfigureDatasource.ensureProductJdbcDriversAndResolve(
            config, PSJdbcUtils.H2_DRIVER, PSJdbcUtils.H2_DRIVER_CLASS);

    assertEquals(PSJdbcUtils.H2_DRIVER_CLASS, resolved.className());
    assertEquals("H2", resolved.containerTypeMapping());

    String written = Files.readString(config, StandardCharsets.UTF_8);
    assertTrue(
        written.contains("driverName=\"h2\"") || written.contains("driverName='h2'"),
        "h2 entry must be written back to config.xml");
    assertTrue(
        written.contains(PSJdbcUtils.H2_DRIVER_CLASS), "h2 driver class must be in config.xml");
    assertTrue(
        written.contains("driverName=\"postgresql\"")
            || written.contains("driverName='postgresql'"),
        "postgresql entry must also be merged for #1500");
    // Pre-existing drivers retained
    assertTrue(written.contains("driverName=\"derby\"") || written.contains("driverName='derby'"));
  }

  @Test
  void ensureIsIdempotentWhenDriversAlreadyPresent() throws Exception {
    Path config = tempDir.resolve("config.xml");
    Files.writeString(config, PRE_H2_CONFIG, StandardCharsets.UTF_8);

    PSConfigureDatasource.ensureProductJdbcDriversAndResolve(
        config, PSJdbcUtils.H2_DRIVER, PSJdbcUtils.H2_DRIVER_CLASS);
    String afterFirst = Files.readString(config, StandardCharsets.UTF_8);

    PSConfigureDatasource.ResolvedJdbcDriver second =
        PSConfigureDatasource.ensureProductJdbcDriversAndResolve(
            config, PSJdbcUtils.H2_DRIVER, PSJdbcUtils.H2_DRIVER_CLASS);
    String afterSecond = Files.readString(config, StandardCharsets.UTF_8);

    assertEquals(PSJdbcUtils.H2_DRIVER_CLASS, second.className());
    assertEquals("H2", second.containerTypeMapping());
    // No duplicate h2 elements: count occurrences
    assertEquals(
        countOccurrences(afterFirst, "driverName=\"h2\""),
        countOccurrences(afterSecond, "driverName=\"h2\""));
  }

  @Test
  void ensureResolvesPostgresqlWithPreferredClass() throws Exception {
    Path config = tempDir.resolve("config.xml");
    Files.writeString(config, PRE_H2_CONFIG, StandardCharsets.UTF_8);

    PSConfigureDatasource.ResolvedJdbcDriver resolved =
        PSConfigureDatasource.ensureProductJdbcDriversAndResolve(
            config, PSJdbcUtils.POSTGRES_DRIVER, PSJdbcUtils.POSTGRES_DRIVER_CLASS);

    assertEquals(PSJdbcUtils.POSTGRES_DRIVER_CLASS, resolved.className());
    assertEquals("POSTGRES", resolved.containerTypeMapping());
  }

  @Test
  void ensureCreatesJdbcDriverConfigsWhenMissing() throws Exception {
    Path config = tempDir.resolve("config.xml");
    Files.writeString(
        config,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <PSXServerConfiguration id="1">
           <requestRoot>Rhythmyx</requestRoot>
        </PSXServerConfiguration>
        """,
        StandardCharsets.UTF_8);

    PSConfigureDatasource.ResolvedJdbcDriver resolved =
        PSConfigureDatasource.ensureProductJdbcDriversAndResolve(
            config, PSJdbcUtils.H2_DRIVER, null);

    assertEquals(PSJdbcUtils.H2_DRIVER_CLASS, resolved.className());
    assertEquals("H2", resolved.containerTypeMapping());
    String written = Files.readString(config, StandardCharsets.UTF_8);
    assertTrue(written.contains("JdbcDriverConfigs"));
    assertTrue(written.contains("driverName=\"h2\"") || written.contains("driverName='h2'"));
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = haystack.indexOf(needle, idx)) >= 0) {
      count++;
      idx += needle.length();
    }
    return count;
  }
}
