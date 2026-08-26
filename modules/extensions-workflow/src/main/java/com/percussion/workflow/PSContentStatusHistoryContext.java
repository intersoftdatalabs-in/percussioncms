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
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.utils.guid.IPSGuid;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * A class that provides methods for creating new content status history records and accessing
 * fields in existing ones, and that is a wrapper for the backend table 'CONTENTSTATUSHISTORY'.
 *
 * @author Rammohan Vangapalli
 * @version 1.0
 * @since 2.0
 */
@Deprecated
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public final class PSContentStatusHistoryContext implements IPSContentStatusHistoryContext {

  /**
   * Constructor specifying the workFlowID, connection, and contentID, used for read-only access to
   * content status history records.
   *
   * <p><strong>Deprecated.</strong> Since #1561 Phase 3 the rows are loaded via the {@link
   * PSSystemServiceLocator#getSystemService()#findContentStatusHistory(IPSGuid)} service
   * (Hibernate) rather than the legacy raw-JDBC context. The {@code connection} parameter is
   * accepted for binary compatibility but is no longer used.
   *
   * @param workFlowID ID of the workflow for this item
   * @param connection data base connection (ignored; preserved for binary compatibility)
   * @param contentID ID of the content item
   * @throws SQLException if a service-level SQL error occurs
   * @throws PSEntryNotFoundException if no records were returned
   */
  public PSContentStatusHistoryContext(int workFlowID, Connection connection, int contentID)
      throws SQLException, PSEntryNotFoundException {
    m_nWorkflowID = workFlowID;
    m_nContentID = contentID;

    IPSSystemService svc = PSSystemServiceLocator.getSystemService();
    IPSGuid id = new PSGuid(PSTypeEnum.LEGACY_CONTENT, contentID);
    m_rows = svc.findContentStatusHistory(id);
    if (m_rows == null || m_rows.isEmpty()) {
      m_nCount = 0;
      throw new PSEntryNotFoundException(ExtensionErrorCodes.NO_RECORDS);
    }
    m_nCount = m_rows.size();
    m_rowIndex = 0;
    moveNext();
  }

  /**
   * Constructor for a new ContentStatusHistory entry specifying all required data; commits the
   * record to the data base via the shared Hibernate session used by the rest of the CMS.
   *
   * <p><strong>Deprecated.</strong> New in-product callers should construct a {@link
   * com.percussion.services.system.data.PSContentStatusHistory} entity and call {@link
   * com.percussion.services.system.IPSSystemService#saveContentStatusHistory} via {@link
   * com.percussion.services.system.PSSystemServiceLocator}. This constructor is preserved only so
   * legacy callers (notably the {@code sys_wfUpdateHistory} exit before it was migrated in Phase 2
   * of #1561, and the legacy {@code PSContentStatusHistoryContextTest} integration harness) keep
   * compiling and behaving identically.
   *
   * <p>The {@code connection} parameter is accepted for backward compatibility with the original
   * signature but is no longer used: the write goes through the Spring-managed datasource via
   * {@link PSSystemServiceLocator#getSystemService()} rather than through this argument.
   *
   * @param contentStatusHistoryID ignored when {@code < 1}; otherwise treated as the desired ID.
   *     Pass {@code -1} (or any non-positive value) to let Hibernate allocate a new {@code
   *     CONTENTSTATUSHISTORYID} via the global GUID manager.
   * @param workFlowID ID of the workflow for this item
   * @param connection data base connection (ignored; preserved for binary compatibility)
   * @param contentID ID of the content item
   * @param contentStatusContext PSContentStatusContext for the content item
   * @param statesContext the PSStatesContext for the content state
   * @param transitionContext the PSTransitionsContext for the transition
   * @param transitionComment descriptive comment for this action
   * @param actorName the name of the user that performed this transition, check in or check out.
   * @param sessionID the SessionID for this content status history entry.
   * @param roleName list of names of assignee roles for the state of the content item
   * @param baseRevisionNum the base revision of the content item for checkouts, otherwise the
   *     current revision.
   * @throws SQLException never thrown by this constructor; preserved in the signature for binary
   *     compatibility.
   * @throws PSEntryNotFoundException never thrown by this constructor; preserved in the signature
   *     for binary compatibility.
   * @throws IllegalArgumentException if any of the input parameters is not valid.
   */
  public PSContentStatusHistoryContext(
      int contentStatusHistoryID,
      int workFlowID,
      Connection connection,
      int contentID,
      PSContentStatusContext contentStatusContext,
      IPSStatesContext statesContext,
      PSTransitionsContext transitionContext,
      String transitionComment,
      String actorName,
      String sessionID,
      String roleName,
      int baseRevisionNum)
      throws SQLException, PSEntryNotFoundException {
    // The connection argument is intentionally unused — see class-level Javadoc.
    // Kept in the signature only for binary compatibility with legacy callers.

    if (null == contentStatusContext) {
      throw new IllegalArgumentException(
          "contentStatusContext is null in " + "PSContentStatusHistoryContext.");
    }

    if (null == statesContext) {
      throw new IllegalArgumentException(
          "statesContext is null in " + "PSContentStatusHistoryContext.");
    }

    PSContentStatusHistory entity =
        PSContentStatusHistoryEntityBuilder.build(
            contentStatusHistoryID,
            workFlowID,
            contentID,
            sessionID,
            actorName,
            baseRevisionNum,
            roleName,
            transitionComment,
            contentStatusContext,
            statesContext,
            transitionContext);
    java.util.Date eventTime = entity.getEventTime();

    IPSSystemService svc = PSSystemServiceLocator.getSystemService();
    svc.saveContentStatusHistory(entity);

    // Mirror the persisted entity into the legacy fields so the interface getters
    // (still exercised by the legacy PSContentStatusHistoryContextTest harness) keep
    // returning the values that match what was just written.
    setContentStatusHistoryID((int) entity.getId());
    setWorkFlowID(workFlowID);
    setContentID(contentID);
    setContentIsValid(statesContext.getIsValid());
    setStateName(statesContext.getStateName());
    setTitle(contentStatusContext.getTitle());
    setContentStateID(contentStatusContext.getContentStateID());
    setContentCheckoutUserName(contentStatusContext.getContentCheckedOutUserName());
    setContentLastModifierName(contentStatusContext.getContentLastModifierName());
    setContentLastModifiedDate(
        entity.getLastModifiedDate() == null
            ? null
            : new java.sql.Date(entity.getLastModifiedDate().getTime()));
    setTransitionID(entity.getTransitionId());
    setTransitionLabel(entity.getTransitionLabel());
    setTransitionComment(transitionComment);
    setActorName(actorName);
    setSessionID(sessionID);
    setContentStateRoleName(roleName);
    setRevision(baseRevisionNum);
    m_EventTime = eventTime == null ? null : new java.sql.Date(eventTime.getTime());
  }

  /**
   * Sets the content status history ID for this entry.
   *
   * @param contentStatusHistoryID the content status history ID for this entry. Must be <CODE>> 0
   *     </CODE>.
   */
  private void setContentStatusHistoryID(int contentStatusHistoryID) {
    m_nContentStatusHistoryID = contentStatusHistoryID;
  }

  /**
   * Sets the ContentID for the content item acted on.
   *
   * @param contentID the ContentID for the content item. Must be <CODE>> 0</CODE>.
   */
  private void setContentID(int contentID) {
    m_nContentID = contentID;
  }

  /**
   * Sets the workFlowID for the content item acted on.
   *
   * @param workFlowID the workFlowID for the content item.
   */
  private void setWorkFlowID(int workFlowID) {
    m_nWorkflowID = workFlowID;
  }

  /**
   * Sets the revision for the content item acted on.
   *
   * @param revision the revision for the content item. For checkout this is the base revision,
   *     which is either 1, or the revision of the content item copied to create the revision
   *     checked out.
   */
  private void setRevision(int revision) {
    m_nRevision = revision;
  }

  /**
   * Sets the title of the content item for this content status history entry.
   *
   * @param title the title of the content item May not be more than 255 characters
   */
  private void setTitle(String title) {
    m_sTitle = title;
  }

  /**
   * Sets the SessionID for this content status history entry.
   *
   * @param sessionID the SessionID for this content status history entry.
   */
  private void setSessionID(String sessionID) {
    m_sSessionID = sessionID;
  }

  /**
   * Sets the name of the user that performed this transition or action including check in and check
   * out.
   *
   * @param actorName the name of the user that performed this action.
   */
  private void setActorName(String actorName) {
    m_sActor = actorName;
  }

  /**
   * Sets the TransitionID for this content status history entry.
   *
   * @param transitionID the TransitionID for this content status history entry. 0 for checkin or
   *     checkout.
   */
  private void setTransitionID(int transitionID) {
    m_nTransitionID = transitionID;
  }

  /**
   * Sets the Transition Label for this content status history entry.
   *
   * @param transitionLabel if this is a transition, the label for the transition. It may not be
   *     more than 50 characters otherwise <CODE>null</CODE> for a check in or checkout. The
   *     transition label will be set to "CheckIn" for a check in, "CheckOut for a check out
   */
  private void setTransitionLabel(String transitionLabel) {
    /*
     * for a transition, use the transition label, set special strings for
     *  check in or check out.
     */
    if (null == transitionLabel) {
      /*
       * It is a check in if the name of the user that checked out this
       * content item is null.
       */
      if (null == getContentCheckoutUserName()) {
        m_sTransitionLabel = "CheckIn";
      } else {
        m_sTransitionLabel = "CheckOut";
      }
    } else {
      m_sTransitionLabel = transitionLabel;
    }
  }

  /**
   * Sets indicator as to whether this content is publishable
   *
   * @param isValid <CODE>true</CODE> if content is publishable else <CODE>false</CODE>
   */
  private void setContentIsValid(boolean isValid) {
    m_bValid = isValid;
  }

  /**
   * Sets the ContentStateID for this content status history entry.
   *
   * @param contentStateID the ID of the current state at completion of transition or action.
   */
  private void setContentStateID(int contentStateID) {
    m_nStateID = contentStateID;
  }

  /**
   * Sets the name of the current state at completion of transition or action.
   *
   * @param stateName the content state name for this content status history entry.
   */
  private void setStateName(String stateName) {
    m_sStateName = stateName;
  }

  /**
   * Sets the assigned role name for the state of the content item for this content status history
   * entry.
   *
   * @param roleName the role name the state of the content item for this content status history
   *     entry. May not be more than 50 characters
   */
  private void setContentStateRoleName(String roleName) {
    m_sRoleName = roleName;
  }

  /**
   * Sets the name of the user that checked out this content item.
   *
   * @param userName name of the user that checked out this content item <CODE>null</CODE> if item
   *     is not checked out. May not be more than 255 characters.
   */
  private void setContentCheckoutUserName(String userName) {
    m_sCheckOutUserName = userName;
  }

  /**
   * Sets the name of the user that last modified out this content item.
   *
   * @param lastModifierName name of the user that last modified this content item. May not be more
   *     than 255 characters. Initial and final whitespace will be trimmed.
   */
  private void setContentLastModifierName(String lastModifierName) {
    m_sLastModifierName = lastModifierName;
  }

  /**
   * Sets the date this content item was last modified.
   *
   * @param date the date this content item was last modified
   */
  private void setContentLastModifiedDate(Date date) {
    m_LastModifiedDate = date;
  }

  /**
   * Sets the descriptive comment for this transition in the content status history entry.
   *
   * @param transitionComment the descriptive comment the transition May be <CODE>null</CODE>. May
   *     not be more than 255 characters.
   */
  private void setTransitionComment(String transitionComment) {
    m_sTransitionComment = transitionComment;
  }

  /*
   * The following get methods all implement the
   * PSContentStatusHistoryContext interface.
   */

  public int getContentStatusHistoryID() {
    return m_nContentStatusHistoryID;
  }

  public int getContentID() {
    return m_nContentID;
  }

  public int getRevision() {
    return m_nRevision;
  }

  public String getTitle() {
    return m_sTitle;
  }

  public String getSessionID() {
    return m_sSessionID;
  }

  public String getActorName() {
    return m_sActor;
  }

  public int getTransitionID() {
    return m_nTransitionID;
  }

  public boolean getContentIsValid() {
    return m_bValid;
  }

  public int getContentStateID() {
    return m_nStateID;
  }

  public String getContentStateName() {
    return m_sStateName;
  }

  public String getTransitionLabel() {
    return m_sTransitionLabel;
  }

  public String getContentStateRoleName() {
    return m_sRoleName;
  }

  public String getContentCheckoutUserName() {
    return m_sCheckOutUserName;
  }

  public String getContentLastModifierName() {
    return m_sLastModifierName;
  }

  public Date getContentLastModifiedDate() {
    return m_LastModifiedDate;
  }

  public Date getEventTime() {
    return m_EventTime;
  }

  public String getTransitionComment() {
    return m_sTransitionComment;
  }

  /*
   * Release JDBC statement and result set if necessary, turn on auto commit
   * if necessary.
   */
  public void close() {
    // Phase 3 (#1561): rows are loaded into memory by Hibernate; nothing to free here.
    // We keep the signature for binary compatibility with legacy callers.
    m_rows = null;
  }

  /*
   * Advances the in-memory cursor over the list returned by Hibernate and copies the next row's
   * columns into member variables. Strings are trimmed; nulls are mapped to empty (or
   * {@code null} for the checkout user, which the legacy interface uses to signal "not checked
   * out"). Throws {@link SQLException} only for the rare case where {@link Date#getTime()} on a
   * non-null date fails (kept in the signature for binary compatibility with the legacy cursor
   * pattern).
   */
  public boolean moveNext() throws SQLException {
    if (m_rows == null || m_rowIndex >= m_rows.size()) {
      return false;
    }
    PSContentStatusHistory row = m_rows.get(m_rowIndex++);
    String valid = row.getIsValidValue();
    m_sSessionID = PSWorkFlowUtils.trimmedOrEmptyString(row.getSessionId());
    m_nContentStatusHistoryID = (int) row.getId();
    m_nContentID = row.getContentId();
    m_nStateID = row.getStateId();
    m_sActor = PSWorkFlowUtils.trimmedOrEmptyString(row.getActor());
    m_nTransitionID = row.getTransitionId();
    valid = PSWorkFlowUtils.trimmedOrEmptyString(row.getIsValidValue());
    m_bValid = (null != valid && valid.equalsIgnoreCase("Y"));
    m_sRoleName = PSWorkFlowUtils.trimmedOrEmptyString(row.getRoleName());
    // A null string is used to indicate an item is not checked out.
    m_sCheckOutUserName = PSWorkFlowUtils.trimmedOrNullString(row.getCheckoutUserName());
    m_sLastModifierName = PSWorkFlowUtils.trimmedOrEmptyString(row.getLastModifierName());
    /*
     * The Hibernate entity returns java.util.Date; the legacy fields are java.sql.Date
     * (because the old PSContentStatusContext used getTimestamp). Narrow via getTime().
     */
    m_LastModifiedDate =
        row.getLastModifiedDate() == null ? null : new Date(row.getLastModifiedDate().getTime());
    m_EventTime = row.getEventTime() == null ? null : new Date(row.getEventTime().getTime());

    m_sTransitionComment = PSWorkFlowUtils.trimmedOrEmptyString(row.getTransitionComment());
    m_nRevision = row.getRevision();
    m_sTitle = PSWorkFlowUtils.trimmedOrEmptyString(row.getTitle());
    m_sTransitionLabel = PSWorkFlowUtils.trimmedOrEmptyString(row.getTransitionLabel());
    m_sStateName = PSWorkFlowUtils.trimmedOrEmptyString(row.getStateName());

    return true;
  }

  public boolean isEmpty() {
    return (0 == m_nCount);
  }

  /*
   * Creates a string containing the values of all fields in the current
   * status history record (the one to which the database cursor points), one
   * to record per line.
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    try {
      buf.append("\nPSContentStatusHistoryContext: ");
      buf.append("\nRecord Count = ");
      buf.append(m_nCount);
      buf.append("\nWorkFlowAppID = ");
      buf.append(m_nWorkflowID);
      buf.append("\nSessionID  = ");
      buf.append(getSessionID());
      buf.append("\nID  = ");
      buf.append(getContentStatusHistoryID());
      buf.append("\nContent ID = ");
      buf.append(getContentID());
      buf.append("\nRevision Number = ");
      buf.append(getRevision());
      buf.append("\nTitle = ");
      buf.append(getTitle());
      buf.append("\nStateID = ");
      buf.append(getContentStateID());
      buf.append("\nActor = ");
      buf.append(getActorName());
      buf.append("\nTransition ID = ");
      buf.append(getTransitionID());
      buf.append("\nValid = ");
      buf.append(getContentIsValid());
      buf.append("\nRole Name = ");
      buf.append(getContentStateRoleName());
      buf.append("\nCheckout user name = ");
      buf.append(getContentCheckoutUserName());
      buf.append("\nLast modifier = ");
      buf.append(getContentLastModifierName());
      buf.append("\nLast modified = ");
      /* Use Timestamp to get minutes and seconds. */
      if (null == getContentLastModifiedDate()) {
        buf.append("null");
      } else {
        buf.append(new Timestamp(getContentLastModifiedDate().getTime()));
      }

      buf.append("\nEvent Time = ");
      if (null == getEventTime()) {
        buf.append("null");
      } else {
        buf.append(new Timestamp(getEventTime().getTime()));
      }
      buf.append("\nTransition Comment = ");
      buf.append(getTransitionComment());
      buf.append("\nTransition Label = ");
      buf.append(getTransitionLabel());
      buf.append("\nState Name = ");
      buf.append(getContentStateName());
      buf.append("\n");
    } catch (Exception e) {
      return "PSContentStatusHistoryContext toString: Caught exception: " + e;
    }
    return buf.toString();
  }

  /******** Context Defining Members ********/

  /** ID of the workflow for this item */
  private int m_nWorkflowID = 0;

  /** ID of the content item for this item */
  private int m_nContentID = 0;

  /** ID for this content status history record */
  private int m_nContentStatusHistoryID = 0;

  /******** Context Data Members ********/

  /** The SessionID for this content status history entry. May not be more than 40 characters. */
  private String m_sSessionID = "";

  /** The ID of the current state at completion of transition or action */
  private int m_nStateID = 0;

  /**
   * Name of the current state at the time of transition or action. May not be more than 50
   * characters.
   */
  private String m_sStateName = "";

  /** name of the user that performed this transition or action */
  private String m_sActor = null;

  /**
   * the transition ID if the content item has undergone a transition 0 for check in and check out.
   */
  private int m_nTransitionID = 0;

  /** <CODE>true</CODE> if content is publishable else <CODE>false</CODE> */
  private boolean m_bValid = false;

  /**
   * comma-separated list of assigned role names for the content item's current state May not be
   * more than 255 characters.
   */
  private String m_sRoleName = null;

  /**
   * Name of the user that checked out this content item, or <CODE>null</CODE> item is not checked
   * out May not be more than 255 characters.
   */
  private String m_sCheckOutUserName = null;

  /** name of the user that last modified this content item May not be more than 255 characters. */
  private String m_sLastModifierName = null;

  /**
   * The revision for the content item. For checkout the base revision, which is either 1, or the
   * revision of the content item copied to create the revision checked out.
   */
  private int m_nRevision = 0;

  /** title of the content item May not be more than 40 characters. */
  private String m_sTitle = null;

  /** the date/time this content item was last modified */
  private Date m_LastModifiedDate = null;

  /** date/time when this transition or action took place */
  private Date m_EventTime = null;

  /** the descriptive comment for this transition. May not be more than 255 characters. */
  private String m_sTransitionComment = "";

  /** label of the transition or action */
  private String m_sTransitionLabel = "";

  /******** Database Related Variables ********/

  /**
   * Rows loaded into memory by Hibernate via {@code IPSSystemService#findContentStatusHistory}. The
   * cursor pattern ({@link #moveNext()}) now iterates this list. Null after {@link #close()}.
   */
  private List<PSContentStatusHistory> m_rows = null;

  /** Zero-based cursor position over {@link #m_rows}. */
  private int m_rowIndex = 0;

  /** Number of database records found */
  private int m_nCount = 0;

  /**
   * Qualified table name only (e.g. {@code PUBLIC.CONTENTSTATUSHISTORY} on H2). Do
   * <strong>not</strong> use this as a column qualifier — {@code schema.table.column} is rejected
   * by H2 as {@code Column "PUBLIC" not found}. Columns in the statements below are unqualified.
   */
  private static final String TABLE_CSHC = "CONTENTSTATUSHISTORY";

  /** SQL query string to get data base records for the content status history. */
  private static final String QRYSTRING =
      "SELECT SESSIONID, CONTENTSTATUSHISTORYID, CONTENTID, STATEID, ACTOR, TRANSITIONID, VALID, "
          + "ROLENAME, CHECKOUTUSERNAME, LASTMODIFIERNAME, LASTMODIFIEDDATE, EVENTTIME, "
          + "TRANSITIONCOMMENT, REVISIONID, TITLE, TRANSITIONLABEL, STATENAME "
          + "FROM "
          + TABLE_CSHC
          + " WHERE (WORKFLOWAPPID=? AND CONTENTID=?)";
}
