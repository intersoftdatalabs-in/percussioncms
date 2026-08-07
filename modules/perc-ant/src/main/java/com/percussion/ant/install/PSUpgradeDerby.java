/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.ant.install;

import com.percussion.install.PSLogger;
import com.percussion.security.error.PSExceptionUtils;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Properties;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.BuildException;

/**
 * Provides an Ant task to upgrade a Derby database. Expects to be able to start the Derby database
 * in embedded mode (single user).
 */
public class PSUpgradeDerby extends PSAction {
  /** Creates a new Derby upgrade task. */
  public PSUpgradeDerby() {
    super();
  }

  private String targetVersion;

  /**
   * Returns the target version of the Derby upgrade.
   *
   * @return the target version, may be <code>null</code> when not configured
   */
  public String getTargetVersion() {
    return targetVersion;
  }

  /**
   * Sets the target version of the Derby upgrade.
   *
   * @param targetVersion the target Derby version string
   */
  public void setTargetVersion(String targetVersion) {
    this.targetVersion = targetVersion;
  }

  private String databasePath;

  /**
   * Returns the path to the Derby database that will be upgraded.
   *
   * @return the absolute path to the Derby database, may be <code>null</code>
   */
  public String getDatabasePath() {
    return databasePath;
  }

  /**
   * Sets the path to the Derby database that will be upgraded.
   *
   * @param databasePath the absolute path to the Derby database
   */
  public void setDatabasePath(String databasePath) {
    this.databasePath = databasePath;
  }

  private String backupDirectory;

  /**
   * Returns the directory where database backups will be written.
   *
   * @return the absolute path to the backup directory, may be <code>null</code>
   */
  public String getBackupDirectory() {
    return backupDirectory;
  }

  /**
   * Sets the directory where database backups will be written.
   *
   * @param backupDirectory the absolute path to the backup directory
   */
  public void setBackupDirectory(String backupDirectory) {
    this.backupDirectory = backupDirectory;
  }

  private String userName;
  private String password;
  private String schema;

  /**
   * Returns the user name used to connect to the Derby database.
   *
   * @return the Derby user name, may be <code>null</code>
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the user name used to connect to the Derby database.
   *
   * @param userName the Derby user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Returns the password used to connect to the Derby database.
   *
   * @return the Derby password, may be <code>null</code>
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password used to connect to the Derby database.
   *
   * @param password the Derby password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the schema used to connect to the Derby database.
   *
   * @return the Derby schema name, may be <code>null</code>
   */
  public String getSchema() {
    return schema;
  }

  /**
   * Sets the schema used to connect to the Derby database.
   *
   * @param schema the Derby schema name
   */
  public void setSchema(String schema) {
    this.schema = schema;
  }

  private static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";

  private void dropPercMetadataPropertiesTable(Connection conn) {
    org.apache.derby.impl.jdbc.EmbedStatement stmt = null;
    try {
      stmt = (org.apache.derby.impl.jdbc.EmbedStatement) conn.createStatement();
      String sql2 = "DROP TABLE PERC_PAGE_METADATA_PROPERTIES";
      stmt.executeUpdate(sql2);

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_PAGE_METADATA_PROPERTIES failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
    try {
      String sql = "DROP TABLE PERC_PAGE_METADATA";
      stmt.executeUpdate(sql);

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_PAGE_METADATA failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
    try {
      String sql = "SELECT COUNT(*) from PERC_COOKIE_CONSENT";
      org.apache.derby.impl.jdbc.EmbedStatement prepStmt =
          (org.apache.derby.impl.jdbc.EmbedStatement) conn.createStatement();
      ResultSet rs = prepStmt.executeQuery(sql);
      rs.next();
      int count = rs.getInt(1);
      rs.close();
      PSLogger.logInfo("Got Result in PERC_COOKIE_CONSENT.");
      if (count == 0) {
        PSLogger.logInfo("Rows in PERC_COOKIE_CONSENT are 0 thus deleting table.");
        String sql2 = "DROP TABLE PERC_COOKIE_CONSENT";
        stmt.execute(sql2);
      }

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for PERC_COOKIE_CONSENT failed, may be doesn't exist: "
              + PSExceptionUtils.getMessageForLog(e));
    }

    try {
      String sql = "SELECT COUNT(*) from BLOG_POST_VISIT";
      org.apache.derby.impl.jdbc.EmbedStatement prepStmt =
          (org.apache.derby.impl.jdbc.EmbedStatement) conn.createStatement();
      ResultSet rs = prepStmt.executeQuery(sql);
      rs.next();
      int count = rs.getInt(1);
      rs.close();
      PSLogger.logInfo("Got Result in BLOG_POST_VISIT.");
      if (count == 0) {
        PSLogger.logInfo("Rows in BLOG_POST_VISIT are 0 thus deleting table.");
        String sql2 = "DROP TABLE BLOG_POST_VISIT";
        stmt.execute(sql2);
      }

    } catch (SQLException e) {
      PSLogger.logWarn(
          "SQL State: Drop table for BLOG_POST_VISIT failed, may be doesn't exist"
              + PSExceptionUtils.getMessageForLog(e));
    }
  }

  /**
   * Locates the Derby JDBC driver jar shipped under {@code Deployment/Server/common/lib} and adds
   * it to the current thread's context class loader via reflection. No-op if no matching jar is
   * found.
   *
   * @throws MalformedURLException if the located driver file cannot be converted to a URL
   * @throws NoSuchMethodException if {@code URLClassLoader.addURL} cannot be located via reflection
   * @throws InvocationTargetException if the reflective {@code addURL} invocation throws
   * @throws IllegalAccessException if the reflective {@code addURL} invocation is denied
   */
  public synchronized void loadDerbyJDBCJar()
      throws MalformedURLException,
          NoSuchMethodException,
          InvocationTargetException,
          IllegalAccessException {
    PSLogger.logInfo("Loading DerbyDriver at RunTime");
    File derbyJDBCDriver = null;
    File dir = new File(getRootDir() + File.separator + "Deployment/Server/common/lib");
    FileFilter fileFilter = new WildcardFileFilter("derby-*.jar");
    File[] files = dir.listFiles(fileFilter);
    if (files != null) {
      if (files.length == 1) {
        derbyJDBCDriver = files[0];
      } else {
        derbyJDBCDriver = files[0];
        PSLogger.logError("Multiple versions of DerbyDriver Exist in " + dir.toString());
      }
    }
    if (derbyJDBCDriver == null) {
      PSLogger.logError("DerbyDriver is Missing");
      return;
    }
    PSLogger.logInfo("Loading DerbyDriver File " + derbyJDBCDriver.toString());
    java.net.URL url = derbyJDBCDriver.toURI().toURL();
    java.lang.reflect.Method method =
        java.net.URLClassLoader.class.getDeclaredMethod(
            "addURL", new Class<?>[] {java.net.URL.class});
    method.setAccessible(true); /*promote the method to public access*/
    method.invoke(Thread.currentThread().getContextClassLoader(), new Object[] {url});
  }

  @Override
  public void execute() throws BuildException {

    if (!Files.exists(Paths.get(databasePath))) {
      throw new BuildException("Database " + databasePath + " does not exist!");
    }

    if (!Files.exists(Paths.get(backupDirectory))) {
      throw new BuildException("Backup directory does not exist!");
    }

    try {
      Class.forName(driver).newInstance();
    } catch (ClassNotFoundException e) {
      try {
        loadDerbyJDBCJar();
        Class.forName(driver).newInstance();
      } catch (Exception ex) {
        throw new BuildException("Unable to load embedded Derby driver");
      }
    } catch (InstantiationException | IllegalAccessException ex) {
      throw new BuildException("Unable to load embedded Derby driver");
    }

    // Connection properties
    Properties props = new Properties();
    props.setProperty("user", userName);
    props.setProperty("password", password);
    props.setProperty("upgrade", "true");
    String connectionUrl = "jdbc:derby:" + databasePath;
    Connection conn;
    try {
      conn = DriverManager.getConnection(connectionUrl, props);
    } catch (SQLException e) {
      throw new BuildException(e);
    }

    try {
      if (getDatabasePath().contains("percmetadata")) {
        // Drop Metadata Tables on upgrade.
        dropPercMetadataPropertiesTable(conn);
      }
      DatabaseMetaData meta = conn.getMetaData();
      PSLogger.logInfo(
          "Derby database version: " + meta.getDatabaseProductVersion() + " detected...");
      conn.close();
      props.remove("upgrade");
      props.putIfAbsent("shutdown", "true");
      PSLogger.logInfo("Shutting down database :" + databasePath);
      conn = DriverManager.getConnection(connectionUrl, props);
      conn.close();

    } catch (SQLNonTransientConnectionException e) {
      PSLogger.logWarn("SQL State:" + e.getSQLState());
      PSLogger.logWarn("SQL Error Code:" + e.getErrorCode());
      if (e.getErrorCode() == 45000 && e.getSQLState().equals("08006")) {
        PSLogger.logInfo("Database shutdown successfully.");
      } else {
        throw new BuildException(e);
      }
    } catch (SQLException e) {
      PSLogger.logWarn("SQL State:" + e.getSQLState());
      PSLogger.logWarn("SQL Error Code:" + e.getErrorCode());
      if (e.getErrorCode() == 45000 && e.getSQLState().equals("08006")) {
        PSLogger.logInfo("Database shutdown successfully.");
      } else {
        throw new BuildException(e);
      }
    } finally {
      try {
        conn.close();
      } catch (Exception e) {
        // do nada}
      }
    }
  }
}
