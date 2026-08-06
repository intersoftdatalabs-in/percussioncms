/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.system.utils;

import com.percussion.util.PSSqlHelper;
import com.percussion.utils.jdbc.PSDriverHelper;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;

/**
 * Datasource which creates a database connection using a driver loaded from an external file.
 *
 * @author peterfrontiero
 */
public class PSDataSource implements DataSource {
  private static final int DEFAULT_LOGIN_TIMEOUT = 300;

  private final String url;
  private final String database;
  private final String username;
  private final String password;
  private final String driverClass;
  private final String driverLocation;

  private Driver driver;
  private int loginTimeout = DEFAULT_LOGIN_TIMEOUT;
  private PrintWriter logWriter = new PrintWriter(System.out);

  /**
   * Creates a datasource object.
   *
   * @param url for the jdbc connection. Never blank.
   * @param database never blank.
   * @param username never blank.
   * @param password never blank.
   * @param driverClass never blank.
   * @param driverLocation the absolute path to the file in which the driver is contained. Never
   *     blank.
   */
  public PSDataSource(
      String url,
      String database,
      String username,
      String password,
      String driverClass,
      String driverLocation) {
    this.url = Objects.requireNonNull(StringUtils.trimToNull(url), "url may not be blank");
    this.database =
        Objects.requireNonNull(StringUtils.trimToNull(database), "database may not be blank");
    this.username =
        Objects.requireNonNull(StringUtils.trimToNull(username), "username may not be blank");
    this.password =
        Objects.requireNonNull(StringUtils.trimToNull(password), "password may not be blank");
    this.driverClass =
        Objects.requireNonNull(StringUtils.trimToNull(driverClass), "driverClass may not be blank");
    this.driverLocation =
        Objects.requireNonNull(
            StringUtils.trimToNull(driverLocation), "driverLocation may not be blank");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    Objects.requireNonNull(iface, "Interface class cannot be null");
    if (isWrapperFor(iface)) {
      return iface.cast(this);
    }
    throw new SQLException("Cannot unwrap to " + iface.getName());
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return iface != null && iface.isAssignableFrom(getClass());
  }

  @Override
  public Connection getConnection() throws SQLException {
    return createConnection(null, null);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return createConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() {
    return logWriter;
  }

  @Override
  public int getLoginTimeout() {
    return loginTimeout;
  }

  @Override
  public void setLogWriter(PrintWriter out) {
    this.logWriter = out;
  }

  @Override
  public void setLoginTimeout(int seconds) {
    this.loginTimeout = seconds;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException("getParentLogger is not supported");
  }

  /**
   * Creates a connection based on the current properties of this datasource, overriding the
   * username and password properties.
   *
   * @param user The username override. May be <code>null</code> to use the configured username.
   * @param pwd The password override. May be <code>null</code> to user the configured password.
   * @return a connection object, never <code>null</code>.
   * @throws SQLException if an error occurs.
   */
  private Connection createConnection(String user, String pwd) throws SQLException {
    var effectiveUsername = user != null ? user : this.username;
    var effectivePassword = pwd != null ? pwd : this.password;

    var props =
        PSSqlHelper.makeConnectProperties(
            this.url, this.database, effectiveUsername, effectivePassword);

    try {
      if (this.driver == null) {
        this.driver = PSDriverHelper.getDriver(this.driverClass, this.driverLocation);
      }

      var conn = this.driver.connect(this.url, props);
      if (conn == null) {
        throw new SQLException("Driver returned null connection for URL: " + this.url);
      }
      return conn;
    } catch (Exception e) {
      throw new SQLException("Failed to create connection: " + e.getMessage(), e);
    }
  }

  // Getters for immutable fields
  public String getUrl() {
    return url;
  }

  public String getDatabase() {
    return database;
  }

  public String getUsername() {
    return username;
  }

  @SuppressWarnings("unused") // May be used by reflection or future code
  public String getDriverClass() {
    return driverClass;
  }

  @SuppressWarnings("unused") // May be used by reflection or future code
  public String getDriverLocation() {
    return driverLocation;
  }

  @Override
  public String toString() {
    return "PSDataSource{"
        + "url='"
        + url
        + '\''
        + ", database='"
        + database
        + '\''
        + ", username='"
        + username
        + '\''
        + ", driverClass='"
        + driverClass
        + '\''
        + ", driverLocation='"
        + driverLocation
        + '\''
        + '}';
  }
}
