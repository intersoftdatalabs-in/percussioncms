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

import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.workflow.data.PSNotificationDef;
import com.percussion.tablefactory.PSJdbcTableFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.NamingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the <CODE>IPSNotificationsContext</CODE> which provides methods for accessing
 * the subject and content of notifications associated with a particular workflow and transition.
 *
 * @deprecated
 */
@Deprecated
public final class PSNotificationsContext extends PSAbstractWorkflowContext
    implements IPSNotificationsContext {

  private static final Logger log = LogManager.getLogger(PSNotificationsContext.class);

  /**
   * Hibernate-backed factory added for #1561 Phase 4c. Loads the notification subject + body for
   * the supplied (workflowId, notificationId) via the shared Hibernate session — no second pool
   * connection.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSNotificationsContext(int, int, Connection)}
   * but participates in the surrounding Spring transaction so the dual-connection defect fixed in
   * Phase 2/3 does not re-emerge for this code path.
   *
   * @param workflowId the workflow id; must be {@code > 0}.
   * @param notificationId the notification id; must be {@code > 0}.
   * @return the populated context, never {@code null}; throws {@link PSEntryNotFoundException} when
   *     no row matches.
   */
  public static PSNotificationsContext loadFromHibernate(int workflowId, int notificationId)
      throws PSEntryNotFoundException {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (notificationId <= 0) {
      throw new IllegalArgumentException("notificationId must be > 0");
    }

    PSNotificationDef hib =
        PSSystemServiceLocator.getSystemService().findNotificationDef(workflowId, notificationId);
    if (hib == null) {
      throw new PSEntryNotFoundException(
          "No notification with notification ID = "
              + notificationId
              + " exists in the workflow "
              + workflowId
              + ".");
    }

    PSNotificationsContext ctx = new PSNotificationsContext();
    ctx.populateFromHibernate(hib);
    return ctx;
  }

  /** Package-private default constructor used by {@link #loadFromHibernate}. */
  PSNotificationsContext() {}

  /**
   * Populates the in-memory subject + body from a {@link PSNotificationDef} row.
   *
   * @param hib the Hibernate-loaded notification definition row; must not be {@code null}.
   */
  private void populateFromHibernate(PSNotificationDef hib) {
    m_nWorkflowID = (int) hib.getWorkflowId();
    m_nNotificationID = (int) hib.getGUID().longValue();
    m_sSubject = hib.getSubject();
    m_sBody = hib.getBody();
  }

  /**
   * Constructor specifying the workflowID and notification ID.
   *
   * @param workflowID ID of the workflow for this notification
   * @param notificationID database notification ID
   * @param connection data base connection
   * @throws SQLException if an error occurs
   * @throws NamingException if a datasource cannot be resolved
   * @throws IllegalArgumentException if the connection is <CODE>null</CODE>
   * @throws PSEntryNotFoundException if no notification row is found for the supplied IDs.
   */
  public PSNotificationsContext(int workflowID, int notificationID, Connection connection)
      throws SQLException, PSEntryNotFoundException, NamingException {
    // Validate and set the defining variables

    if (null == connection) {
      throw new IllegalArgumentException("Connection cannot be null");
    }
    m_nWorkflowID = workflowID;
    m_nNotificationID = notificationID;
    m_Connection = connection;

    // Get the data from the database
    getBackEndData();

    // Throw error if no database record was found
    throwErrorIfEntryNotFound(
        "No notification with notification ID = "
            + notificationID
            + " exists in the workflow "
            + workflowID
            + ".");
  }

  /* IMPLEMENTATION OF METHODS IN CLASS PSAbstractWorkflowContext  */

  public void reinitializeDataMembers() {
    m_sSubject = null;
    m_sBody = null;
  }

  protected String getQueryString() {
    return QRYSTRING;
  }

  protected void setQueryParameters() throws SQLException {
    m_Statement.setInt(1, m_nWorkflowID);
    m_Statement.setInt(2, m_nNotificationID);
  }

  protected void resultSetMove() throws SQLException {
    // These must be in the same order as in the query
    m_sSubject = PSWorkFlowUtils.trimmedOrNullString(m_Rs.getString("SUBJECT"));
    try {
      // We always assume that BODY field is of type CLOB
      m_sBody = PSJdbcTableFactory.getClobColumnData(m_Rs, "BODY");
    } catch (IOException ioe) {
      log.error(ioe.getMessage());
      log.debug(ioe.getMessage());
    }
  }

  /* IMPLEMENTATION OF IPSNotificationsContext INTERFACE */

  public String getSubject() {
    return m_sSubject;
  }

  public String getBody() {
    return m_sBody;
  }

  /******** Context Defining Members ********/

  /** ID of the workflow for this item. */
  private int m_nWorkflowID = 0;

  /** the notification ID */
  private int m_nNotificationID = 0;

  /******** Context Data Members ********/

  /** Subject of the mail notification */
  String m_sSubject = null;

  /** Body of the mail notification */
  private String m_sBody = null;

  /******** Database Related Variables ********/

  /**
   * Qualified table name only (e.g. {@code PUBLIC.NOTIFICATIONS} on H2). Do <strong>not</strong>
   * use this as a column qualifier — {@code schema.table.column} is rejected by H2 as {@code Column
   * "PUBLIC" not found}. Columns in the statement below are unqualified.
   */
  private static final String TABLE_NC = "NOTIFICATIONS";

  /** SQL query — unqualified column names for H2-safe schema-qualified table names. */
  private static final String QRYSTRING =
      "SELECT SUBJECT, BODY FROM " + TABLE_NC + " WHERE (WORKFLOWAPPID=? AND NOTIFICATIONID=?)";
}
