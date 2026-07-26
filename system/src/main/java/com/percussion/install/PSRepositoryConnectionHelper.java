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

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * Opens JDBC connections from {@code rxrepository.properties}-style configuration for migration
 * (#548). Uses portable path resolution for file-based embedded engines.
 */
public final class PSRepositoryConnectionHelper {

  public static final String KEY_UID = "UID";
  public static final String KEY_PWD = "PWD";
  public static final String KEY_DB_SERVER = "DB_SERVER";
  public static final String KEY_DB_DRIVER_NAME = "DB_DRIVER_NAME";
  public static final String KEY_DB_DRIVER_CLASS = "DB_DRIVER_CLASS_NAME";
  public static final String KEY_DB_SCHEMA = "DB_SCHEMA";

  private PSRepositoryConnectionHelper() {}

  /**
   * Open a connection using repository properties.
   *
   * @param repositoryProperties rxrepository-style props
   * @param installRoot install root for resolving relative file paths; may be null if server is
   *     absolute/networked
   * @return open connection (caller closes)
   * @throws SQLException on connect failure
   * @throws ClassNotFoundException if driver class missing
   */
  public static Connection open(Properties repositoryProperties, Path installRoot)
      throws SQLException, ClassNotFoundException {
    Objects.requireNonNull(repositoryProperties, "repositoryProperties");
    String driverName = require(repositoryProperties, KEY_DB_DRIVER_NAME);
    String driverClass = require(repositoryProperties, KEY_DB_DRIVER_CLASS);
    String server = require(repositoryProperties, KEY_DB_SERVER);
    String uid = nullToEmpty(repositoryProperties.getProperty(KEY_UID));
    String pwd = nullToEmpty(repositoryProperties.getProperty(KEY_PWD));

    // Explicit pre-load is best-effort. Derby 10.17.x removed org.apache.derby.jdbc.EmbeddedDriver
    // in favor of org.apache.derby.iapi.jdbc.AutoloadedDriver (registered via the JDBC service
    // loader on META-INF/services/java.sql.Driver); legacy config values still reference the old
    // name. Fall through to DriverManager.getConnection which will pick up the auto-registered
    // driver when the explicit class load fails.
    try {
      Class.forName(driverClass);
    } catch (ClassNotFoundException e) {
      if (!PSJdbcUtils.DERBY_DRIVER.equalsIgnoreCase(driverName)) {
        throw e;
      }
    }
    String resolvedServer = resolveServerFragment(server, installRoot, driverName);
    String url = PSJdbcUtils.getJdbcUrl(driverName, resolvedServer);
    return DriverManager.getConnection(url, uid, pwd);
  }

  /**
   * Build H2 target repository properties for a new file database under the install tree.
   *
   * @param installRoot install root
   * @param h2DatabaseBase absolute path of H2 database base name (no {@code .mv.db} suffix)
   * @return properties suitable for cutover into {@code rxrepository.properties}
   */
  public static Properties buildH2TargetProperties(Path installRoot, Path h2DatabaseBase) {
    Objects.requireNonNull(h2DatabaseBase, "h2DatabaseBase");
    String abs = h2DatabaseBase.toAbsolutePath().normalize().toString().replace('\\', '/');
    String server = "file:" + abs + ";DB_CLOSE_ON_EXIT=FALSE";

    Properties p = new Properties();
    p.setProperty(PSEmbeddedRepositoryDetector.KEY_DB_BACKEND, PSJdbcUtils.H2_DB_BACKEND);
    p.setProperty(KEY_DB_DRIVER_NAME, PSJdbcUtils.H2_DRIVER);
    p.setProperty(KEY_DB_DRIVER_CLASS, PSJdbcUtils.H2_DRIVER_CLASS);
    p.setProperty(KEY_DB_SERVER, server);
    p.setProperty(KEY_DB_SCHEMA, "PUBLIC");
    p.setProperty(KEY_UID, "sa");
    p.setProperty(KEY_PWD, "");
    p.setProperty("DSCONFIG_NAME", "PercussionData");
    p.setProperty("DB_NAME", "");
    if (installRoot != null) {
      // retained for operators; not used by H2 URL
      p.setProperty("INSTALL_ROOT_HINT", installRoot.toAbsolutePath().normalize().toString());
    }
    return p;
  }

  /**
   * Default H2 database base path under install root: {@code Repository/CMDB}.
   *
   * @param installRoot install root
   * @return path used as H2 file base name
   */
  public static Path defaultH2DatabaseBase(Path installRoot) {
    Objects.requireNonNull(installRoot, "installRoot");
    return installRoot.resolve("Repository").resolve("CMDB");
  }

  /**
   * Resolve a {@code DB_SERVER} fragment for JDBC URL construction.
   *
   * <p>For H2 {@code file:} fragments that are relative (e.g. {@code file:../../Repository/CMDB}),
   * resolve against {@code installRoot/jetty/base} when present, else {@code installRoot}.
   */
  static String resolveServerFragment(String server, Path installRoot, String driverName) {
    if (server == null) {
      return null;
    }
    String s = server.trim();
    if (installRoot == null) {
      return s;
    }
    if (PSJdbcUtils.H2_DRIVER.equalsIgnoreCase(driverName)
        && s.regionMatches(true, 0, "file:", 0, 5)) {
      String pathAndParams = s.substring(5);
      int semi = pathAndParams.indexOf(';');
      String pathPart = semi >= 0 ? pathAndParams.substring(0, semi) : pathAndParams;
      String params = semi >= 0 ? pathAndParams.substring(semi) : "";
      Path path = Path.of(pathPart);
      if (!path.isAbsolute()) {
        Path base = installRoot.resolve("jetty").resolve("base");
        if (!java.nio.file.Files.isDirectory(base)) {
          base = installRoot;
        }
        path = base.resolve(pathPart).normalize();
      }
      return "file:" + path.toAbsolutePath().toString().replace('\\', '/') + params;
    }
    if (PSJdbcUtils.DERBY_DRIVER.equalsIgnoreCase(driverName)
        && !s.startsWith("//")
        && !s.startsWith("memory:")
        && !s.contains(":")) {
      // bare directory name → absolute under installRoot/Repository when relative
      Path path = Path.of(s);
      if (!path.isAbsolute()) {
        path = installRoot.resolve("Repository").resolve(s).normalize();
      }
      return path.toAbsolutePath().toString().replace('\\', '/');
    }
    return s;
  }

  private static String require(Properties p, String key) throws SQLException {
    String v = p.getProperty(key);
    if (v == null || v.isBlank()) {
      throw new SQLException("Missing required repository property: " + key);
    }
    return v.trim();
  }

  private static String nullToEmpty(String v) {
    return v == null ? "" : v;
  }
}
