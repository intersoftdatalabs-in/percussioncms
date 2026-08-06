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

package com.percussion.services.datasource.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.Objects;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleConnectionWrapper;

/**
 * Oracle-specific connection wrapper that provides enhanced Oracle database functionality with
 * modern Java validation patterns.
 *
 * <p>This wrapper extends Oracle's {@link OracleConnectionWrapper} to provide consistent connection
 * management while maintaining Oracle-specific features and ensuring proper resource cleanup.
 *
 * <p>Unwrap targets the public JDBC interface {@link OracleConnection}, not {@code
 * oracle.jdbc.driver.OracleConnection}. HikariCP and other pools often require walking a {@code
 * delegate} field because the physical driver connection's {@code unwrap(OracleConnection.class)}
 * can throw ORA-17177 even when it implements the interface (#2083 live Oracle matrix).
 *
 * @author Percussion Software
 * @since 6.0
 */
public class PSOracleConnectionWrapper extends OracleConnectionWrapper {

  /** The pool/proxy connection to close (returns the handle to the pool). */
  private final Connection delegate;

  /**
   * Constructs a new Oracle connection wrapper with enhanced validation.
   *
   * @param delegate The wrapper containing the Oracle connection, may not be null
   * @throws SQLException if the Oracle connection cannot be unwrapped
   * @throws IllegalArgumentException if delegate is null
   */
  public PSOracleConnectionWrapper(Wrapper delegate) throws SQLException {
    super(unwrapOracleConnection(delegate));
    this.delegate =
        (Connection) Objects.requireNonNull(delegate, "delegate may not be null");
  }

  /**
   * Safely unwrap the Oracle connection with enhanced error handling.
   *
   * <p>Package-visible for unit tests.
   *
   * @param wrapper The wrapper to unwrap
   * @return The unwrapped Oracle connection
   * @throws SQLException if unwrapping fails
   * @throws IllegalArgumentException if wrapper is null
   */
  static OracleConnection unwrapOracleConnection(Wrapper wrapper) throws SQLException {
    Objects.requireNonNull(wrapper, "wrapper may not be null");

    if (wrapper instanceof OracleConnection oracleConnection) {
      return oracleConnection;
    }
    if (!(wrapper instanceof Connection connection)) {
      throw new SQLException(
          "Failed to unwrap Oracle connection: not a java.sql.Connection ("
              + wrapper.getClass().getName()
              + ")");
    }

    SQLException firstFailure = null;

    // 1) Standard JDBC unwrap (works for many pools).
    try {
      if (connection.isWrapperFor(OracleConnection.class)) {
        return connection.unwrap(OracleConnection.class);
      }
    } catch (SQLException e) {
      firstFailure = e;
    }

    // 2) Walk common pool/proxy delegate fields (HikariCP ProxyConnection.delegate).
    Connection physical = extractDelegateConnection(connection);
    if (physical instanceof OracleConnection oracleConnection) {
      return oracleConnection;
    }

    // 3) DatabaseMetaData#getConnection often returns the vendor connection.
    try {
      Connection fromMeta = connection.getMetaData().getConnection();
      if (fromMeta instanceof OracleConnection oracleConnection) {
        return oracleConnection;
      }
      Connection nested = extractDelegateConnection(fromMeta);
      if (nested instanceof OracleConnection oracleConnection) {
        return oracleConnection;
      }
    } catch (SQLException e) {
      if (firstFailure == null) {
        firstFailure = e;
      } else {
        firstFailure.addSuppressed(e);
      }
    }

    // 4) Reflective cast when class name is the Oracle driver connection but ClassLoaders differ
    //    enough that instanceof OracleConnection failed (rare; still try direct class match).
    OracleConnection viaName = castOracleByClassName(physical != null ? physical : connection);
    if (viaName != null) {
      return viaName;
    }

    SQLException failure =
        new SQLException(
            "Failed to unwrap Oracle connection from " + connection.getClass().getName());
    if (firstFailure != null) {
      failure.initCause(firstFailure);
    }
    throw failure;
  }

  /**
   * Follow {@code delegate} / {@code _conn} / {@code connection} fields used by common JDBC pools.
   */
  static Connection extractDelegateConnection(Connection connection) {
    if (connection == null) {
      return null;
    }
    Connection current = connection;
    // Bound walk so a cycle cannot spin forever.
    for (int depth = 0; depth < 8; depth++) {
      Connection next = readConnectionField(current, "delegate");
      if (next == null) {
        next = readConnectionField(current, "_conn");
      }
      if (next == null) {
        next = readConnectionField(current, "connection");
      }
      if (next == null || next == current) {
        return current;
      }
      current = next;
      if (current instanceof OracleConnection) {
        return current;
      }
    }
    return current;
  }

  private static Connection readConnectionField(Connection connection, String fieldName) {
    for (Class<?> type = connection.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(connection);
        if (value instanceof Connection conn) {
          return conn;
        }
      } catch (NoSuchFieldException ignored) {
        // try superclass
      } catch (ReflectiveOperationException | SecurityException ignored) {
        return null;
      }
    }
    return null;
  }

  /**
   * When {@code instanceof OracleConnection} fails due to split class loaders, try the driver's
   * {@code unwrap()} method that returns {@code oracle.jdbc.OracleConnection} without our
   * Class token, or cast if the type name matches.
   */
  private static OracleConnection castOracleByClassName(Connection connection) {
    if (connection == null) {
      return null;
    }
    String name = connection.getClass().getName();
    if ("oracle.jdbc.driver.OracleConnection".equals(name)
        || "oracle.jdbc.OracleConnection".equals(name)
        || name.startsWith("oracle.jdbc.driver.") && name.endsWith("Connection")) {
      try {
        return (OracleConnection) connection;
      } catch (ClassCastException ignored) {
        // Split class loader — try reflective invoke of unwrap() no-arg on OracleConnectionWrapper
        // style APIs.
      }
    }
    try {
      Method unwrap = connection.getClass().getMethod("unwrap");
      Object result = unwrap.invoke(connection);
      if (result instanceof OracleConnection oracleConnection) {
        return oracleConnection;
      }
    } catch (ReflectiveOperationException ignored) {
      // not available
    }
    return null;
  }

  /**
   * Closes the delegate connection ensuring proper resource cleanup.
   *
   * @throws SQLException if closing the connection fails
   */
  @Override
  public void close() throws SQLException {
    try {
      delegate.close();
    } catch (SQLException e) {
      throw new SQLException("Failed to close Oracle connection", e);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "PSOracleConnectionWrapper{delegate=%s}",
        delegate != null ? delegate.getClass().getSimpleName() : "null");
  }
}
