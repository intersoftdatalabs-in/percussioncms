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

import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSNotification;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

/**
 * The PSTransitionNotificationsContext class is a wrapper class providing access to the records and
 * fields of the backend table 'TRANSITIONNOTIFICATIONS'.
 *
 * @deprecated
 */
@Deprecated
public class PSTransitionNotificationsContext extends PSAbstractMultipleRecordWorkflowContext
    implements IPSTransitionNotificationsContext {
  /**
   * Hibernate-backed factory added for #1561 Phase 4c. Loads the {@code TRANSITIONNOTIFICATIONS}
   * rows for the supplied (workflowId, transitionId) via the shared Hibernate session — no second
   * pool connection.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSTransitionNotificationsContext(int, int,
   * Connection)} but participates in the surrounding Spring transaction so the dual-connection
   * defect fixed in Phase 2/3 does not re-emerge for this code path.
   *
   * @param workflowId the workflow id; must be {@code > 0}.
   * @param transitionId the transition id; must be {@code > 0}.
   * @return the populated context, never {@code null}; may have count 0 if no notifications are
   *     configured for the transition.
   */
  public static PSTransitionNotificationsContext loadFromHibernate(
      int workflowId, int transitionId) {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (transitionId <= 0) {
      throw new IllegalArgumentException("transitionId must be > 0");
    }

    IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
    List<PSNotification> rows = wfSvc.findTransitionNotifications(workflowId, transitionId);

    PSTransitionNotificationsContext ctx = new PSTransitionNotificationsContext();
    ctx.populateFromHibernate(workflowId, transitionId, rows);
    return ctx;
  }

  /** Package-private default constructor used by {@link #loadFromHibernate}. */
  PSTransitionNotificationsContext() {}

  /**
   * Populates the in-memory state from a list of {@link PSNotification} ({@code
   * TRANSITIONNOTIFICATIONS}) rows. Mirrors the cursor accumulated in the raw-JDBC constructor's
   * {@link #AccumulateCurrentDataSet()} / {@link #MoveAccumulatedDataSet(int)} pattern so consumers
   * iterating via {@link #moveNext()} see the same { @code notificationId / recipientType /
   * additionalRecipientList / ccList } sequence.
   *
   * @param workflowId the workflow id.
   * @param transitionId the transition id.
   * @param rows the Hibernate rows; may be empty but never {@code null}.
   */
  private void populateFromHibernate(int workflowId, int transitionId, List<PSNotification> rows) {
    m_nWorkflowID = workflowId;
    m_nTransitionID = transitionId;
    m_nCount = 0;
    // Reset the boolean flags so the new AccumulateCurrentDataSet re-derives them.
    m_bRequireToStateRoles = false;
    m_bRequireFromStateRoles = false;
    for (PSNotification row : rows) {
      m_nNotificationID = (int) row.getNotificationId();
      m_nStateRoleRecipientTypes = row.getStateRoleRecipientType().getValue();
      m_sAdditionalRecipientList =
          row.getRecipients().isEmpty() ? "" : String.join(",", row.getRecipients());
      m_sCCList = row.getCCRecipients().isEmpty() ? "" : String.join(",", row.getCCRecipients());
      AccumulateCurrentDataSet();
      m_nCount++;
    }
    if (m_nCount > 1) {
      MoveAccumulatedDataSet(0);
    }
  }

  /**
   * Constructor specifying the workflowID and transition ID for the collection of notifications.
   *
   * @param workflowID ID of the workflow for these notifications
   * @param transitionID ID of the transition for these notifications
   * @param connection data base connection
   * @throws SQLException if an SQL error occurs
   * @throws IllegalArgumentException if the connection is <CODE>null</CODE>
   * @throws NamingException if a datasource cannot be resolved
   */
  public PSTransitionNotificationsContext(int workflowID, int transitionID, Connection connection)
      throws SQLException, NamingException {
    // Validate the input
    if (null == connection) {
      throw new IllegalArgumentException("Connection cannot be null");
    }

    // Assign the member variables
    m_Connection = connection;
    m_nWorkflowID = workflowID;
    m_nTransitionID = transitionID;

    // Get the data from the database
    getBackEndData();
  }

  /* IMPLEMENTATION OF METHODS IN CLASS PSAbstractWorkflowContext  */

  protected void reinitializeDataMembers() {
    m_nNotificationID = 0;
    m_nStateRoleRecipientTypes = 0;
    m_sAdditionalRecipientList = null;
    m_sCCList = null;
  }

  protected String getQueryString() {
    return QRYSTRING;
  }

  protected void setQueryParameters() throws SQLException {
    int nLoc = 0;
    m_Statement.setInt(++nLoc, m_nWorkflowID);
    m_Statement.setInt(++nLoc, m_nTransitionID);
  }

  /*
   * Gets the next JDBC result set, and moves the data into member variables;
   * strings are trimmed.
   */
  protected void resultSetMove() throws SQLException {
    // These must be in the same order as in the query
    m_nNotificationID = m_Rs.getInt("NOTIFICATIONID");
    m_nStateRoleRecipientTypes = m_Rs.getInt("STATEROLERECIPIENTTYPES");
    m_sAdditionalRecipientList =
        PSWorkFlowUtils.trimmedOrEmptyString(m_Rs.getString("ADDITIONALRECIPIENTLIST"));
    m_sCCList = PSWorkFlowUtils.trimmedOrEmptyString(m_Rs.getString("CCLIST"));
  }

  /* IMPLEMENTATION OF PSAbstractMultipleRecordWorkflowContext METHODS */

  protected void AccumulateCurrentDataSet() {
    m_nNotificationIDList.add(Integer.valueOf(m_nNotificationID));
    m_nStateRoleRecipientTypesList.add(Integer.valueOf(m_nStateRoleRecipientTypes));
    m_sAdditionalRecipientListList.add(m_sAdditionalRecipientList);
    m_sCCListList.add(m_sCCList);

    if (!m_bRequireToStateRoles) {
      m_bRequireToStateRoles = notifyToStateRoles();
    }
    if (!m_bRequireFromStateRoles) {
      m_bRequireFromStateRoles = notifyFromStateRoles();
    }
  }

  protected boolean MoveAccumulatedDataSet(int i) {
    if (i >= m_nCount || i < 0) {
      return false;
    }
    m_nNotificationID = ((Integer) m_nNotificationIDList.get(i)).intValue();
    m_nStateRoleRecipientTypes = ((Integer) m_nStateRoleRecipientTypesList.get(i)).intValue();
    m_sAdditionalRecipientList = (String) m_sAdditionalRecipientListList.get(i);
    m_sCCList = (String) m_sCCListList.get(i);

    return true;
  }

  /* IMPLEMENTATION OF IPSTransitionNotificationsContext INTERFACE */

  public int getWorkflowID() {
    return m_nWorkflowID;
  }

  public int getTransitionID() {
    return m_nTransitionID;
  }

  public int getNotificationID() {
    return m_nNotificationID;
  }

  public int getStateRoleRecipientTypes() {
    return 1;
  }

  public boolean notifyFromStateRoles() {
    return (IPSTransitionNotificationsContext.ONLY_OLD_STATE_ROLE_RECIPIENTS
            == m_nStateRoleRecipientTypes
        || IPSTransitionNotificationsContext.OLD_AND_NEW_STATE_ROLE_RECIPIENTS
            == m_nStateRoleRecipientTypes);
  }

  public boolean notifyToStateRoles() {
    return (IPSTransitionNotificationsContext.ONLY_NEW_STATE_ROLE_RECIPIENTS
            == m_nStateRoleRecipientTypes
        || IPSTransitionNotificationsContext.OLD_AND_NEW_STATE_ROLE_RECIPIENTS
            == m_nStateRoleRecipientTypes);
  }

  public boolean requireFromStateRoles() {
    return m_bRequireFromStateRoles;
  }

  public boolean requireToStateRoles() {
    return m_bRequireToStateRoles;
  }

  public String getAdditionalRecipientList() {
    return m_sAdditionalRecipientList;
  }

  public String getCCList() {
    return m_sCCList;
  }

  /******** Context Defining Members ********/

  /** ID of the workflow for this item. */
  private int m_nWorkflowID = 0;

  /** ID of the transition for this item. */
  private int m_nTransitionID = 0;

  /******** Context Data Members ********/

  /** the notification ID */
  private int m_nNotificationID = 0;

  /**
   * value indicating which state role recipients should receive notification: none, from-state,
   * to-state or both
   */
  private int m_nStateRoleRecipientTypes = 0;

  /** comma-delimited list of additional notification recipients */
  private String m_sAdditionalRecipientList = null;

  /** comma-delimited list of CC notification recipients */
  private String m_sCCList = null;

  /** List of notification IDs */
  private ArrayList m_nNotificationIDList = new ArrayList();

  /**
   * List of values indicating which state role recipients should receive notification: none,
   * from-state, to-state or both
   */
  private ArrayList m_nStateRoleRecipientTypesList = new ArrayList();

  /** List of comma-delimited lists of additional notification recipients */
  private ArrayList m_sAdditionalRecipientListList = new ArrayList();

  /** List of comma-delimited lists of CC notification recipients */
  private ArrayList m_sCCListList = new ArrayList();

  /**
   * <CODE>true</CODE> if from-state role recipients should receive at least one notification, else
   * <CODE>false</CODE>
   */
  private boolean m_bRequireFromStateRoles = false;

  /**
   * <CODE>true</CODE> if to-state role recipients should receive at least one notification, else
   * <CODE>false</CODE>
   */
  private boolean m_bRequireToStateRoles = false;

  /******** Database Related Variables ********/

  /**
   * Qualified table name only (e.g. {@code PUBLIC.TRANSITIONNOTIFICATIONS} on H2). Do
   * <strong>not</strong> use this as a column qualifier — {@code schema.table.column} is rejected
   * by H2 as {@code Column "PUBLIC" not found}. Columns in the statement below are unqualified.
   */
  private static final String TABLE_TNC = "TRANSITIONNOTIFICATIONS";

  /** SQL query — unqualified column names for H2-safe schema-qualified table names. */
  private static final String QRYSTRING =
      "SELECT NOTIFICATIONID, STATEROLERECIPIENTTYPES, ADDITIONALRECIPIENTLIST, CCLIST "
          + "FROM "
          + TABLE_TNC
          + " WHERE (WORKFLOWAPPID=? AND TRANSITIONID=?)";
}
