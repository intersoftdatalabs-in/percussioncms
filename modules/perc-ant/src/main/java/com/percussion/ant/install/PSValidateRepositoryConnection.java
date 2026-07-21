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

import com.percussion.install.InstallUtil;
import com.percussion.install.PSLogger;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Validates that the repository described by install-root {@code
 * rxconfig/Installer/rxrepository.properties} can be reached with the configured credentials.
 *
 * <p>Intended for <strong>new install</strong> only (wired behind {@code do.install}). Does not log
 * or echo passwords.
 *
 * <pre>
 *   &lt;PSValidateRepositoryConnection rootDir="${install.dir}" loginTimeoutSeconds="15"/&gt;
 * </pre>
 */
public class PSValidateRepositoryConnection extends Task {

  private String rootDir;
  private int loginTimeoutSeconds = 15;

  public void setRootDir(String rootDir) {
    this.rootDir = rootDir;
  }

  public String getRootDir() {
    return rootDir;
  }

  public void setLoginTimeoutSeconds(int loginTimeoutSeconds) {
    this.loginTimeoutSeconds = loginTimeoutSeconds;
  }

  public int getLoginTimeoutSeconds() {
    return loginTimeoutSeconds;
  }

  @Override
  public void execute() throws BuildException {
    if (StringUtils.isBlank(rootDir)) {
      throw new BuildException("rootDir is required for PSValidateRepositoryConnection");
    }

    File propFile =
        new File(rootDir + File.separator + "rxconfig/Installer/rxrepository.properties");
    if (!(propFile.exists() && propFile.isFile())) {
      throw new BuildException(
          "Repository properties file not found for connection validation: "
              + propFile.getAbsolutePath());
    }

    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(propFile)) {
      props.load(in);
    } catch (IOException e) {
      throw new BuildException(
          "Unable to read repository properties for connection validation: "
              + propFile.getAbsolutePath(),
          e);
    }

    String backend = props.getProperty("DB_BACKEND", "");
    String driverName = props.getProperty("DB_DRIVER_NAME", "");
    String driverClass = props.getProperty("DB_DRIVER_CLASS_NAME", "");
    String server = props.getProperty("DB_SERVER", "");
    String database = props.getProperty("DB_NAME", "");
    String uid = props.getProperty("UID", "");

    // Never include password in log lines
    PSLogger.logInfo(
        "Validating repository connection: backend="
            + backend
            + " driver="
            + driverName
            + " server="
            + server
            + " database="
            + database
            + " uid="
            + uid);

    // Register JARs for InstallUtil's custom loader. Do not Class.forName here — drivers often
    // live only under jetty/base/lib/jdbc and are loaded the same way as PSExecSQLStmt.
    registerJdbcDriversFromInstall(rootDir);

    try {
      // rootDir already validated non-blank above
      InstallUtil.setRootDir(rootDir);
      // PSJdbcDbmsDef static init and password handling resolve the install root via PathUtils
      // (rxdeploydir). Match PSAction.setRootDir so a stale/deleted temp path from earlier Ant
      // tasks or unit tests cannot abort validation with ExceptionInInitializerError.
      bindInstallRoot(rootDir);

      PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);
      String driver = dbmsDef.getDriver();
      String serverName = dbmsDef.getServer();
      String db = dbmsDef.getDataBase();
      String user = dbmsDef.getUserId();
      String password = dbmsDef.getPassword();

      int previousTimeout = DriverManager.getLoginTimeout();
      try {
        if (loginTimeoutSeconds > 0) {
          DriverManager.setLoginTimeout(loginTimeoutSeconds);
        }
        try (Connection conn =
            InstallUtil.createLoadedConnection(driver, serverName, db, user, password)) {
          if (conn == null) {
            // createLoadedConnection may return null for driver/loader failures without throwing
            throw new BuildException(
                failureMessage(
                    backend,
                    server,
                    driverClass,
                    "Connection returned null (driver may be missing or failed to load)."
                        + " Check JDBC drivers under jetty/base/lib/jdbc and credentials."));
          }
          if (conn.isClosed()) {
            throw new BuildException(
                failureMessage(
                    backend,
                    server,
                    driverClass,
                    "Connection was closed immediately after open. Check host, credentials,"
                        + " and database provisioning."));
          }
          PSLogger.logInfo("Repository connection validation succeeded for backend=" + backend);
        }
      } finally {
        DriverManager.setLoginTimeout(previousTimeout);
      }
    } catch (BuildException be) {
      throw be;
    } catch (PSJdbcTableFactoryException | SQLException e) {
      throw new BuildException(
          failureMessage(backend, server, driverClass, safeExceptionMessage(e)), e);
    } catch (Exception e) {
      throw new BuildException(
          failureMessage(backend, server, driverClass, safeExceptionMessage(e)), e);
    } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
      // PathUtils / crypto static init failures are Errors, not Exceptions
      throw new BuildException(
          failureMessage(backend, server, driverClass, safeExceptionMessage(e)), e);
    }
  }

  /**
   * Point {@code rxdeploydir} / {@link PathUtils} at the install root before code that depends on
   * them during class or instance initialization (notably {@link PSJdbcDbmsDef}).
   *
   * @param installRoot absolute or relative install root; must exist as a directory
   */
  static void bindInstallRoot(String installRoot) {
    File root = new File(installRoot).getAbsoluteFile();
    if (!root.isDirectory()) {
      throw new BuildException(
          "Install root for repository connection validation does not exist or is not a"
              + " directory: "
              + root.getAbsolutePath());
    }
    System.setProperty(PathUtils.DEPLOY_DIR_PROP, root.getAbsolutePath());
    PathUtils.clearRxDir();
    PathUtils.setRxDir(root);
  }

  /**
   * Build a user-facing failure message that never intentionally embeds secrets. When the root
   * cause looks like a missing driver, append FR-012-style remediation.
   */
  static String failureMessage(
      String backend, String server, String driverClass, String detail) {
    StringBuilder msg = new StringBuilder();
    msg.append("Repository connection validation failed for backend=")
        .append(backend == null ? "" : backend)
        .append(" server=")
        .append(server == null ? "" : server);
    if (StringUtils.isNotBlank(detail)) {
      msg.append(": ").append(detail);
    }
    if (looksLikeMissingDriver(detail, driverClass)) {
      msg.append(" Place the appropriate JDBC driver JAR in jetty/base/lib/jdbc and retry.");
      if (StringUtils.isNotBlank(driverClass)) {
        msg.append(" Expected driver class: ").append(driverClass).append('.');
      }
    }
    return msg.toString();
  }

  static boolean looksLikeMissingDriver(String detail, String driverClass) {
    if (detail == null) {
      return false;
    }
    String lower = detail.toLowerCase();
    if (lower.contains("classnotfound")
        || lower.contains("no suitable driver")
        || lower.contains("driver not found")
        || lower.contains("driver may be missing")
        || lower.contains("failed to load")
        || lower.contains("is not supported by the current installer")
        || (lower.contains("cannot find") && lower.contains("driver"))
        || (lower.contains("returned null") && lower.contains("driver"))) {
      return true;
    }
    return StringUtils.isNotBlank(driverClass)
        && detail.contains(driverClass)
        && (lower.contains("not found") || lower.contains("not supported"));
  }

  /** Prefer getMessage(); never append nested causes that might echo credentials. */
  static String safeExceptionMessage(Throwable e) {
    if (e == null) {
      return "";
    }
    String m = e.getMessage();
    return m == null ? e.getClass().getSimpleName() : m;
  }

  /**
   * Register JARs from the install tree's JDBC driver drop directory so DriverManager can load
   * them during fresh install validation.
   */
  static void registerJdbcDriversFromInstall(String installRoot) {
    Path jdbcDir =
        Paths.get(installRoot, "jetty", "base", "lib", "jdbc");
    if (!Files.isDirectory(jdbcDir)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(jdbcDir, "*.jar")) {
      for (Path jar : stream) {
        InstallUtil.addJarFileUrl(jar.toAbsolutePath().toString());
      }
    } catch (IOException e) {
      PSLogger.logInfo("Unable to enumerate JDBC drivers in " + jdbcDir + ": " + e.getMessage());
    }
  }

  /**
   * Test helper: true when a message appears free of an explicit secret sample.
   *
   * @param message user-facing message
   * @param secret password that must not appear
   * @return true if secret is blank or not contained in message
   */
  static boolean messageDoesNotContainSecret(String message, String secret) {
    if (secret == null || secret.isEmpty()) {
      return true;
    }
    return message == null || !message.contains(secret);
  }
}
