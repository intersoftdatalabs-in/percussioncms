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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for Oracle unwrap helpers used with pool proxies (Hikari-style {@code delegate}
 * field). Avoids a live Oracle driver so CI can run without XE (#2083).
 */
class PSOracleConnectionWrapperTest {

  @Test
  void extractDelegateConnectionWalksDelegateField() throws Exception {
    Connection physical = Mockito.mock(Connection.class);
    HikariStyleProxy proxy = new HikariStyleProxy(physical);

    Connection extracted = PSOracleConnectionWrapper.extractDelegateConnection(proxy);
    assertSame(physical, extracted);
  }

  @Test
  void unwrapOracleConnectionRejectsNonConnectionWrapper() {
    Wrapper notAConnection = Mockito.mock(Wrapper.class);
    SQLException ex =
        assertThrows(
            SQLException.class,
            () -> PSOracleConnectionWrapper.unwrapOracleConnection(notAConnection));
    assertTrue(ex.getMessage().toLowerCase().contains("failed to unwrap"));
  }

  /**
   * Minimal stand-in for HikariCP {@code ProxyConnection}: holds a package-visible {@code delegate}
   * field that the production unwrap walk must discover via reflection.
   */
  private static final class HikariStyleProxy implements Connection {
    @SuppressWarnings("unused") // read via reflection in extractDelegateConnection
    private final Connection delegate;

    HikariStyleProxy(Connection delegate) {
      this.delegate = delegate;
    }

    // --- Connection methods: all unsupported; only field layout matters for this unit test ---

    @Override
    public <T> T unwrap(Class<T> iface) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }

    @Override
    public java.sql.Statement createStatement() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.CallableStatement prepareCall(String sql) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String nativeSQL(String sql) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean getAutoCommit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.DatabaseMetaData getMetaData() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCatalog(String catalog) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getCatalog() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setTransactionIsolation(int level) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getTransactionIsolation() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.SQLWarning getWarnings() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clearWarnings() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(
        String sql, int resultSetType, int resultSetConcurrency) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.CallableStatement prepareCall(
        String sql, int resultSetType, int resultSetConcurrency) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Map<String, Class<?>> getTypeMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setTypeMap(java.util.Map<String, Class<?>> map) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setHoldability(int holdability) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getHoldability() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Savepoint setSavepoint() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Savepoint setSavepoint(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rollback(java.sql.Savepoint savepoint) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void releaseSavepoint(java.sql.Savepoint savepoint) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Statement createStatement(
        int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(
        String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.CallableStatement prepareCall(
        String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Clob createClob() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Blob createBlob() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.NClob createNClob() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.SQLXML createSQLXML() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid(int timeout) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setClientInfo(String name, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setClientInfo(java.util.Properties properties) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getClientInfo(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Properties getClientInfo() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Array createArrayOf(String typeName, Object[] elements) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.sql.Struct createStruct(String typeName, Object[] attributes) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setSchema(String schema) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getSchema() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void abort(java.util.concurrent.Executor executor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getNetworkTimeout() {
      throw new UnsupportedOperationException();
    }
  }
}
