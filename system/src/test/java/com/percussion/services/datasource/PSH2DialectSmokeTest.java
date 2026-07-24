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
package com.percussion.services.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke tests for H2 as the #548 default embedded engine: dialect registration class, JDBC URL
 * construction, file-mode connect, and basic FOR UPDATE lock SQL.
 */
@DisplayName("H2 dialect / file-store smoke (#548)")
class PSH2DialectSmokeTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("H2Dialect loads and exposes for-update lock string")
  void h2Dialect_forUpdate_isPresent() {
    var dialect = new H2Dialect();
    var forUpdate = dialect.getForUpdateString();
    assertNotNull(forUpdate);
    assertTrue(
        forUpdate.toLowerCase().contains("for update") || forUpdate.toLowerCase().contains("for"),
        "Expected FOR UPDATE style lock SQL, got: " + forUpdate);
    assertFalse(forUpdate.contains("with rs with rs"), "No duplicate lock options");
  }

  @Test
  @DisplayName("PSHibernateDialectConfig resolves h2 driver to H2Dialect")
  void dialectConfig_mapsH2Driver() {
    var cfg = new PSHibernateDialectConfig();
    cfg.setDialect(PSJdbcUtils.H2_DRIVER, org.hibernate.dialect.H2Dialect.class.getName());
    assertEquals(
        org.hibernate.dialect.H2Dialect.class.getName(),
        cfg.getDialectClassName(PSJdbcUtils.H2_DRIVER));
  }

  @Test
  @DisplayName("File-mode H2 accepts product-style jdbc URL and basic DDL/DML")
  void fileModeH2_createAndQuery() throws Exception {
    Path dbDir = tempDir.resolve("h2cms");
    Files.createDirectories(dbDir);
    // Portable path for JDBC (H2 accepts forward slashes on all platforms)
    String path = dbDir.resolve("CMDB").toAbsolutePath().toString().replace('\\', '/');
    String serverFragment = "file:" + path + ";DB_CLOSE_ON_EXIT=FALSE";
    String url = PSJdbcUtils.getJdbcUrl(PSJdbcUtils.H2_DRIVER, serverFragment);
    assertTrue(url.startsWith("jdbc:h2:"), url);
    assertFalse(url.contains("create=true"), "Derby-style create flag must not be appended for H2");

    Class.forName(PSJdbcUtils.H2_DRIVER_CLASS);
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement st = c.createStatement()) {
      st.execute("CREATE TABLE T_H2_SMOKE (ID INT PRIMARY KEY, NAME VARCHAR(64))");
      st.execute("INSERT INTO T_H2_SMOKE VALUES (1, 'ok')");
      try (ResultSet rs = st.executeQuery("SELECT NAME FROM T_H2_SMOKE WHERE ID = 1")) {
        assertTrue(rs.next());
        assertEquals("ok", rs.getString(1));
      }
      // Pessimistic-style select; H2 supports FOR UPDATE
      try (ResultSet rs =
          st.executeQuery("SELECT NAME FROM T_H2_SMOKE WHERE ID = 1 FOR UPDATE")) {
        assertTrue(rs.next());
      }
    }
  }
}
