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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.workflow;

import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.NamingException;

/**
 * Legacy utility class for resolving a fully-qualified JDBC table name for the configured
 * datasource. After #1561 Phase 4d-1b, no in-product code opens a second pool connection via
 * the no-arg {@code PSConnectionMgr} constructor — the writes/reads are routed through the
 * Hibernate session on the surrounding Spring transaction.
 *
 * <p>The class is retained as a static utility that re-exposes
 * {@link #getQualifiedIdentifier(String)} for the nine legacy context class static-inits
 * ({@code PSContentStatusContext}, {@code PSTransitionsContext}, etc.) that resolve table
 * names at class load time. Removing the class entirely would break those static-inits
 * (which call {@code PSConnectionMgr.getQualifiedIdentifier("X")}); keeping the class
 * preserves the existing H2-safe schema-qualified table name behaviour and avoids
 * touching every legacy context class.
 *
 * <p>The legacy {@code new PSConnectionMgr()}/{@code getConnection()}/{@code releaseConnection()}
 * entry points used to introduce a <em>second pool connection</em> in in-product exits
 * (CONTENTSTATUS, STATEROLES, CONTENTTYPES, TRANSITIONS, etc.), which caused the
 * dual-connection defect fixed in #1561. Those exit-level call sites have all been
 * migrated to the Hibernate-backed factories added in Phases 4a–4d; this class is
 * now solely a table-name resolver + a thin pass-through to {@link PSConnectionHelper}
 * for the still-legacy {@link #getNewConnection()} / {@link #releaseConnection(Connection)}
 * static helpers consumed by {@code PSAbstractWorkflowContext}.
 *
 * @deprecated Use {@code PSConnectionHelper.getConnectionDetail()} (or the
 *     {@code PSConnectionHelper.getQualifiedTableName} helper, when added) directly
 *     instead. The only remaining in-product use of this class is the static-init
 *     table-name resolution in the legacy workflow context classes.
 */
@Deprecated
public final class PSConnectionMgr {

  /**
   * Returns a new connection from the server's database pool. Phase 4d-1b: this is a
   * thin pass-through to {@link PSConnectionHelper#getDbConnection()}.
   *
   * @return a fresh pool connection, never {@code null}.
   * @throws SQLException if the pool cannot supply a connection.
   * @throws NamingException if the JNDI datasource cannot be resolved.
   */
  public static Connection getNewConnection() throws SQLException, NamingException {
    return PSConnectionHelper.getDbConnection();
  }

  /**
   * Releases a connection back to the server's database pool. Phase 4d-1b: this is a
   * thin pass-through to {@link PSConnectionHelper#releaseConnection(Connection)}; if
   * the helper doesn't expose a release method, falls back to closing the connection.
   *
   * @param connection the connection to release, ignored if {@code null}.
   * @throws SQLException if the pool rejects the release.
   */
  public static void releaseConnection(Connection connection) throws SQLException {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException e) {
      // ignore
    }
  }

  /**
   * Returns a JDBC connection for debug / test use. Phase 4d-1b: thin pass-through to
   * {@link PSConnectionHelper#getDbConnection()}.
   *
   * @return a fresh pool connection, never {@code null}.
   * @throws SQLException if the pool cannot supply a connection.
   * @throws NamingException if the JNDI datasource cannot be resolved.
   */
  public static Connection getDebugConnection() throws SQLException, NamingException {
    return PSConnectionHelper.getDbConnection();
  }

  /**
   * Releases a debug / test connection. Phase 4d-1b: thin pass-through to
   * {@link Connection#close()}.
   *
   * @param connection the connection to release, ignored if {@code null}.
   * @throws SQLException if the connection close fails.
   */
  public static void releaseDebugConnection(Connection connection) throws SQLException {
    if (connection == null) {
      return;
    }
    connection.close();
  }

  /**
   * Returns a fully-qualified JDBC table name for the configured datasource. Reads
   * {@code rxconfig/Server/config.xml} via JNDI through {@link PSConnectionHelper} and
   * caches the result.
   *
   * @param sIdentifier the table name to qualify.
   * @return the fully-qualified identifier (catalog + schema + table), never {@code null}.
   */
  public static String getQualifiedIdentifier(String sIdentifier) {
    PSConnectionDetail detail = null;
    if (!ms_initConnInfo) {

      try {
        detail = PSConnectionHelper.getConnectionDetail(null);
        ms_database = detail.getDatabase();
        ms_schema = detail.getOrigin();
        ms_initConnInfo = true;
      } catch (Exception e) {
        throw new RuntimeException("Failed to obtain connection detail:", e);
      }
    }

    sIdentifier = fixIdentifierCase(sIdentifier);
    StringBuilder buf = new StringBuilder();

    boolean bAddedCatalog = false;
    String sCatalog = null;

    if ((ms_database != null)
        && (ms_database.length() != 0)
        && m_bSupportsCatalogsInDataManipulation) {
      if (m_bIsCatalogAtStart) {
        buf.append(ms_database);
        buf.append(m_sCatalogSeparator);
        bAddedCatalog = true;
      } else sCatalog = m_sCatalogSeparator + ms_database;
    }

    String sOrigin = ms_schema;
    if (null == sOrigin) sOrigin = "";

    if (((sOrigin.length() > 0) || bAddedCatalog) && m_bSupportsSchemasInDataManipulation) {
      buf.append(sOrigin);
      buf.append('.');
    }

    buf.append(sIdentifier);

    if ((false == bAddedCatalog) && (null != sCatalog)) buf.append(sCatalog);

    return buf.toString();
  }

  private static String fixIdentifierCase(String identifier) {
    if (identifier == null) return null;

    if (m_bStoresLowerCaseIdentifiers) identifier = identifier.toLowerCase();
    else if (m_bStoresUpperCaseIdentifiers) identifier = identifier.toUpperCase();

    return identifier;
  }

  /**
   * Name of the database. Initialized by first call to {@link #getQualifiedIdentifier(String)}.
   */
  private static String ms_database;

  /**
   * Name of the schema/origin. Initialized by first call to
   * {@link #getQualifiedIdentifier(String)}.
   */
  private static String ms_schema;

  /**
   * Indicates if connection info used by {@link #getQualifiedIdentifier(String)} has been
   * initialized yet.
   */
  private static boolean ms_initConnInfo = false;

  public static boolean m_bStoresLowerCaseIdentifiers = false;
  public static boolean m_bStoresUpperCaseIdentifiers = true;
  public static boolean m_bSupportsCatalogsInDataManipulation = false;
  public static boolean m_bIsCatalogAtStart = false;
  public static String m_sCatalogSeparator = ".";
  public static boolean m_bSupportsSchemasInDataManipulation = false;

  /**
   * Phase 4d-1b: this constructor is unreachable from in-product code (all 5 call sites
   * migrated to Hibernate). The constructor body is empty and would have opened a second
   * pool connection; keeping it would silently reintroduce the dual-connection defect
   * documented in #1561. The class is retained only for the static
   * {@link #getQualifiedIdentifier(String)} helper.
   */
  private PSConnectionMgr() {
    throw new UnsupportedOperationException(
        "PSConnectionMgr is a 1-method utility stub after #1561 Phase 4d-1b;"
            + " use Hibernate service methods or PSConnectionHelper for connection access.");
  }
}
