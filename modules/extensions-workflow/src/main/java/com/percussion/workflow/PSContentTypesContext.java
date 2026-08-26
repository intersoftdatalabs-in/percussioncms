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
package com.percussion.workflow;

import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.data.PSNodeDefinition;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.util.PSPreparedStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Concrete implementation of {@link IPSContentTypesContext} that loads content type metadata from
 * the database.
 */
@Deprecated
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public final class PSContentTypesContext implements IPSContentTypesContext {
  private boolean invokedStandalone = false;

  private int contentTypeID = 0;
  private PreparedStatement statement = null;
  private Connection connection = null;
  private ResultSet rs = null;

  String m_sContentTypeName = "";
  String m_sContentTypeDescrition = "";
  String m_sContentTypeNewRequest = "";
  String m_sContentTypeQueryRequest = "";
  String m_sContentTypeUpdateRequest = "";

  /**
   * Hibernate-backed factory added for #1561 Phase 4d-1a. Loads a single content type's metadata
   * from the shared Hibernate session — no second pool connection.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSContentTypesContext(Connection, int)} but
   * participates in the surrounding Spring transaction.
   *
   * @param contentTypeID the ID of the content type to load; must be {@code > 0}.
   * @return the populated context, never {@code null}; empty (all fields {@code ""}) when no row
   *     matches — use {@link #isEmpty()} to detect missing rows.
   */
  public static PSContentTypesContext loadFromHibernate(int contentTypeID) {
    if (contentTypeID <= 0) {
      throw new IllegalArgumentException("contentTypeID must be > 0");
    }

    PSContentTypesContext ctx = new PSContentTypesContext();
    ctx.contentTypeID = contentTypeID;
    PSNodeDefinition found = null;
    try {
      java.util.List<com.percussion.utils.guid.IPSGuid> ids =
          Collections.singletonList(
              PSGuidManagerLocator.getGuidMgr().makeGuid(contentTypeID, PSTypeEnum.NODEDEF));
      java.util.List<IPSNodeDefinition> defs =
          PSContentMgrLocator.getContentMgr().loadNodeDefinitions(ids);
      found =
          defs.stream()
              .filter(nd -> nd instanceof PSNodeDefinition)
              .map(nd -> (PSNodeDefinition) nd)
              .findFirst()
              .orElse(null);
    } catch (Exception ignore) {
      // loadNodeDefinitions throws NoSuchNodeTypeException when no row matches; treat as
      // "no content type with this id" and return an empty context.
    }
    if (found != null) {
      ctx.m_sContentTypeName = found.getName() == null ? "" : found.getName();
      ctx.m_sContentTypeDescrition = found.getDescription() == null ? "" : found.getDescription();
      ctx.m_sContentTypeNewRequest = found.getNewRequest() == null ? "" : found.getNewRequest();
      ctx.m_sContentTypeQueryRequest =
          found.getQueryRequest() == null ? "" : found.getQueryRequest();
      ctx.m_sContentTypeUpdateRequest =
          found.getUpdateRequest() == null ? "" : found.getUpdateRequest();
    }
    return ctx;
  }

  /**
   * Package-private default constructor used by the Hibernate {@link #loadFromHibernate(int)}
   * factory. Field initialization is performed by the factory.
   */
  PSContentTypesContext() {}

  /**
   * Constructs a content types context for the supplied content type by loading its row from the
   * database.
   *
   * @param connection the database connection to use, must not be <code>null</code>.
   * @param contentTypeID the ID of the content type to load.
   * @throws SQLException if an SQL error occurs while loading the row.
   * @throws PSEntryNotFoundException if no row exists for the supplied content type ID.
   */
  public PSContentTypesContext(Connection connection, int contentTypeID)
      throws SQLException, PSEntryNotFoundException {
    this.contentTypeID = contentTypeID;
    try {
      statement = PSPreparedStatement.getPreparedStatement(connection, QRYSTRING);
      statement.clearParameters();
      statement.setInt(1, contentTypeID);
      rs = statement.executeQuery();

      if (false == rs.next()) {
        close();
        throw new PSEntryNotFoundException(ExtensionErrorCodes.NO_RECORDS);
      }

      m_sContentTypeName = rs.getString("CONTENTTYPENAME");
      m_sContentTypeDescrition = rs.getString("CONTENTTYPEDESC");
      m_sContentTypeNewRequest = rs.getString("CONTENTTYPENEWREQUEST");
      m_sContentTypeQueryRequest = rs.getString("CONTENTTYPEQUERYREQUEST");
      m_sContentTypeUpdateRequest = rs.getString("CONTENTTYPEUPDATEREQUEST");

    } catch (SQLException e) {
      close();
      throw e;
    }
  }

  public String getContentTypeQueryRequest() throws SQLException {
    return m_sContentTypeQueryRequest;
  }

  public String getContentTypeUpdateRequest() throws SQLException {
    return m_sContentTypeUpdateRequest;
  }

  public String getContentTypeNewRequest() throws SQLException {
    return m_sContentTypeNewRequest;
  }

  public String getContentTypeName() throws SQLException {
    return m_sContentTypeName;
  }

  public String getContentTypeDescription() throws SQLException {
    return m_sContentTypeDescrition;
  }

  public void close() {
    // release resouces
    try {
      if (null != connection && false == connection.getAutoCommit()) connection.setAutoCommit(true);
    } catch (SQLException e) {
    }
    try {
      if (null != rs) rs.close();
      rs = null;

      if (null != statement) statement.close();
      statement = null;
    } catch (SQLException e) {
    }
  }

  /**
   * Qualified table name only (e.g. {@code PUBLIC.CONTENTTYPES} on H2). Do <strong>not</strong> use
   * this as a column qualifier — {@code schema.table.column} is rejected by H2 as {@code Column
   * "PUBLIC" not found}. Columns in the statement below are unqualified.
   */
  private static final String TABLE_CTC = "CONTENTTYPES";

  /** SQL query — unqualified column names for H2-safe schema-qualified table names. */
  private static final String QRYSTRING =
      "SELECT CONTENTTYPENAME, CONTENTTYPEDESC, CONTENTTYPENEWREQUEST, "
          + "CONTENTTYPEQUERYREQUEST, CONTENTTYPEUPDATEREQUEST "
          + "FROM "
          + TABLE_CTC
          + " WHERE (CONTENTTYPEID=?)";
}
