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

import com.percussion.cms.IPSConstants;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.PSWorkflowServiceLocator;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransitionRole;
import com.percussion.util.PSPreparedStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class that provides methods for accessing and setting fields in the transitions' context. The
 * table joins are hidden from the user.
 *
 * @author Rammohan Vangapalli
 * @version 1.0
 * @since 2.0
 */
public class PSTransitionsContext implements IPSTransitionsContext {
  private int workflowID = 0;
  private PreparedStatement statement;
  private Connection connection;
  private ResultSet m_Rs;
  private int m_nCount = 0;

  private int m_nTransitionID = 0;
  private String m_sTransitionLabel = "";
  private String m_sTransitionPrompt = "";
  private String m_sTransitionDesc = "";
  private int m_nTransitionFromStateID = 0;
  private int m_nTransitionToStateID = 0;
  private String m_sTransitionTrigger = "";
  private int m_nTransitionApprovalsRequired = 0;
  private String m_sTransitionComment = "";
  private String m_sTransitionActions = "";
  private String m_sTransitionRoles = "";
  private List<String> m_TransitionRoleNames_List = null;
  private List<Integer> m_TransitionRoleIds_List = null;
  private Map<Integer, String> m_transitionRoleNamesIdMap = new HashMap<>();

  private List<String> m_TransitionActions_List = null;
  private int m_nTransitionType = 0;
  private int m_nAgingType = 0;
  private int m_nAgingInterval = 0;
  private String m_sSystemField = "";
  private boolean m_bIsAgingTransition = false;

  /**
   * Pre-loaded rows used by the Hibernate-backed factory methods ({@link #loadFromHibernate(int,
   * int)}, {@link #loadFromHibernate(int, String, int)}, and {@link #loadAllFromHibernate(int,
   * int)}). Remains {@code null} when the context was created via the raw-JDBC constructors. Added
   * for #1561 Phase 4d-1a.
   */
  private List<PSTransition> m_hRows = null;

  /**
   * Current cursor position into {@link #m_hRows}; {@code -1} before the first {@link #moveNext()}
   * call. Only used by the Hibernate-backed factories. Added for #1561 Phase 4d-1a.
   */
  private int m_hCursorIndex = -1;

  private static final Logger log = LogManager.getLogger(IPSConstants.WORKFLOW_LOG);

  /**
   * Constructor specifying the transition ID.
   *
   * @param transitionID ID of the transition
   * @param connection database connection
   * @throws SQLException if an SQL error occurs
   * @throws PSEntryNotFoundException if no records were returned corresponding to this set of data.
   */
  public PSTransitionsContext(int transitionID, int workflowid, Connection connection)
      throws SQLException, PSEntryNotFoundException {
    this.connection = connection;
    try {
      statement = PSPreparedStatement.getPreparedStatement(connection, QRYSTRING_WITH_TRANSITIONID);
      statement.clearParameters();
      statement.setInt(1, transitionID);
      statement.setInt(2, workflowid);
      m_Rs = statement.executeQuery();
      if (!moveNext()) {
        close();
        throw new PSEntryNotFoundException(IPSExtensionErrors.NO_RECORDS);
      }
      m_nCount = 1;
    } catch (SQLException e) {
      close();
      throw e;
    }
  }

  /**
   * Constructor specifying the workFlowID, connection, transition trigger, and state which the
   * content item is transitioning from.
   *
   * @param workFlowID ID of the workflow for this item
   * @param connection database connection
   * @param transitionTrigger trigger for the action associated with this transition, e.g. "reject"
   * @param transitionFromStateID state which item is transitioning from
   * @throws SQLException if an SQL error occurs
   * @throws PSEntryNotFoundException if no records were returned corresponding to this set of data.
   */
  public PSTransitionsContext(
      int workFlowID, Connection connection, String transitionTrigger, int transitionFromStateID)
      throws SQLException, PSEntryNotFoundException {
    this.workflowID = workFlowID;
    this.connection = connection;

    try {
      statement = PSPreparedStatement.getPreparedStatement(connection, QRYSTRING_FOR_ASSIGNMENT);
      statement.clearParameters();
      statement.setInt(1, workflowID);
      statement.setString(2, transitionTrigger);
      statement.setInt(3, transitionFromStateID);
      m_Rs = statement.executeQuery();
      if (!moveNext()) {
        close();
        throw new PSEntryNotFoundException(IPSExtensionErrors.NO_RECORDS);
      }
      m_nCount = 1;
    } finally {
      close();
    }
  }

  /**
   * Constructor specifying the workFlowID, connection, and state which the content item is
   * transitioning from.
   *
   * @param workFlowID ID of the workflow for this item
   * @param connection database connection
   * @param transitionFromStateID state which item is transitioning from
   * @throws SQLException if an SQL error occurs
   * @throws PSEntryNotFoundException if no records were returned corresponding to this set of data.
   */
  public PSTransitionsContext(int workFlowID, Connection connection, int transitionFromStateID)
      throws SQLException, PSEntryNotFoundException {
    this.workflowID = workFlowID;
    this.connection = connection;
    try {
      statement =
          PSPreparedStatement.getPreparedStatement(connection, QRYSTRING_FOR_POSSIBLE_TRANSITIONS);
      statement.clearParameters();
      statement.setInt(1, workflowID);
      statement.setInt(2, transitionFromStateID);
      m_Rs = statement.executeQuery();

      while (m_Rs.next()) {
        m_nCount++;
      }

      if (0 == m_nCount) {
        close();
        throw new PSEntryNotFoundException(IPSExtensionErrors.NO_RECORDS);
      }

      try {
        m_Rs.close();
        statement.close();
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
      }

      // redo the whole thing!!!
      statement =
          PSPreparedStatement.getPreparedStatement(connection, QRYSTRING_FOR_POSSIBLE_TRANSITIONS);
      statement.clearParameters();
      statement.setInt(1, workflowID);
      statement.setInt(2, transitionFromStateID);
      m_Rs = statement.executeQuery();

      moveNext();
    } catch (SQLException e) {
      close();
      throw e;
    }
  }

  /**
   * Hibernate-backed factory added for #1561 Phase 4d-1a. Loads a single non-aging transition by
   * its (workflowId, transitionId) tuple via the shared Hibernate session — no second pool
   * connection.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSTransitionsContext(int, int, Connection)}
   * but participates in the surrounding Spring transaction so the dual-connection defect fixed in
   * Phase 2/3 does not re-emerge for this code path.
   *
   * @param workflowId the workflow id; must be {@code > 0}.
   * @param transitionId the transition id; must be {@code > 0}.
   * @return the populated context, never {@code null}; may have count 0 if no transition matches.
   *     When a row is found the cursor is already on that row (JDBC constructor parity).
   */
  public static PSTransitionsContext loadFromHibernate(int workflowId, int transitionId) {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (transitionId <= 0) {
      throw new IllegalArgumentException("transitionId must be > 0");
    }

    IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
    PSTransition row = wfSvc.loadWorkflowTransition(workflowId, transitionId);

    PSTransitionsContext ctx = new PSTransitionsContext();
    if (row == null) {
      ctx.m_nCount = 0;
      return ctx;
    }
    java.util.List<PSTransition> rows = new ArrayList<>(1);
    rows.add(row);
    ctx.populateFromHibernate(workflowId, rows);
    return ctx;
  }

  /**
   * Hibernate-backed factory added for #1561 Phase 4d-1a. Loads a single non-aging transition by
   * its (workflowId, trigger, fromStateId) tuple via the shared Hibernate session — no second pool
   * connection. Used by {@code PSWorkflowCommandHandler.normalizeTransitionIdParameter}.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSTransitionsContext(int, Connection, String,
   * int)} but participates in the surrounding Spring transaction.
   *
   * @param workflowId the workflow id; must be {@code > 0}.
   * @param trigger the action trigger name; must not be {@code null} or empty.
   * @param fromStateId the source state id; must be {@code > 0}.
   * @return the populated context, never {@code null}; may have count 0 if no transition matches.
   *     When a row is found the cursor is already on that row (JDBC constructor parity).
   */
  public static PSTransitionsContext loadFromHibernate(
      int workflowId, String trigger, int fromStateId) {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (trigger == null || trigger.trim().isEmpty()) {
      throw new IllegalArgumentException("trigger must not be null or empty");
    }
    if (fromStateId <= 0) {
      throw new IllegalArgumentException("fromStateId must be > 0");
    }

    IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
    PSTransition row = wfSvc.findTransitionByTrigger(workflowId, trigger, fromStateId);

    PSTransitionsContext ctx = new PSTransitionsContext();
    if (row == null) {
      ctx.m_nCount = 0;
      return ctx;
    }
    java.util.List<PSTransition> rows = new ArrayList<>(1);
    rows.add(row);
    ctx.populateFromHibernate(workflowId, rows);
    return ctx;
  }

  /**
   * Hibernate-backed factory added for #1561 Phase 4d-1a. Loads all non-aging transitions for the
   * supplied (workflowId, fromStateId) tuple via the shared Hibernate session — no second pool
   * connection. Used by {@code PSExitAddPossibleTransitions} and {@code PSExitPerformTransition}.
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSTransitionsContext(int, Connection, int)}
   * but participates in the surrounding Spring transaction so the cursor iteration via {@link
   * #moveNext()} sees the same transition sequence in the same order.
   *
   * @param workflowId the workflow id; must be {@code > 0}.
   * @param fromStateId the source state id; must be {@code > 0}.
   * @return the populated context, never {@code null}; may be empty if no transitions are
   *     configured for the state. The cursor is already on the first row when the list is
   *     non-empty (same as the JDBC constructor). Call {@link #moveNext()} to advance.
   */
  public static PSTransitionsContext loadAllFromHibernate(int workflowId, int fromStateId) {
    if (workflowId <= 0) {
      throw new IllegalArgumentException("workflowId must be > 0");
    }
    if (fromStateId <= 0) {
      throw new IllegalArgumentException("fromStateId must be > 0");
    }

    IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
    java.util.List<PSTransition> rows = wfSvc.findTransitionsByState(workflowId, fromStateId);

    PSTransitionsContext ctx = new PSTransitionsContext();
    ctx.populateFromHibernate(workflowId, rows);
    return ctx;
  }

  /**
   * Package-private default constructor used by the Hibernate {@code loadFromHibernate*} and {@link
   * #loadAllFromHibernate} factories. Field initialization is performed by {@link
   * #populateFromHibernate}.
   */
  PSTransitionsContext() {}

  /**
   * Populates the in-memory state from a list of {@link PSTransition} ({@code TRANSITIONS}) rows.
   * Mirrors the raw-JDBC constructors, which call {@link #moveNext()} before returning so getters
   * ({@link #getTransitionFromStateID()}, {@link #getTransitionActionTrigger()}) already reflect
   * the first row. {@code PSExitPerformTransition} reads those getters without a further {@code
   * moveNext()} — leaving the cursor at {@code -1} made from-state {@code 0} and every user trigger
   * (including Explorer Expire) fail with {@code INVALID_TRANSITION} (#3668).
   *
   * @param workflowId the workflow id.
   * @param rows the Hibernate rows; may be empty but never {@code null}.
   */
  void populateFromHibernate(int workflowId, java.util.List<PSTransition> rows) {
    this.workflowID = workflowId;
    java.util.List<PSTransition> list = rows == null ? java.util.List.of() : rows;
    this.m_hRows = list;
    this.m_nCount = list.size();
    if (list.isEmpty()) {
      this.m_hCursorIndex = -1;
      return;
    }
    this.m_hCursorIndex = 0;
    populateRowFromHibernate(list.get(0));
  }

  /**
   * This makes a request to get the roles, names and ids of the transition. A join would work but
   * there are far too many changes to do at this time. This must be called after the <code>
   * connection</code> is set.
   */
  private void buildRolesList(int transitionID, int workflowid) throws SQLException {
    PreparedStatement stmt;
    String queryString =
        "SELECT ROLES.ROLENAME, "
            + "TRANSITIONROLES.TRANSITIONROLEID "
            + "FROM ROLES, TRANSITIONROLES WHERE ROLES.WORKFLOWAPPID = "
            + "TRANSITIONROLES.WORKFLOWAPPID AND ROLES.ROLEID = "
            + "TRANSITIONROLES.TRANSITIONROLEID "
            + "AND TRANSITIONROLES.TRANSITIONID = ? AND "
            + "TRANSITIONROLES.WORKFLOWAPPID = ? ";

    ResultSet resSet;

    try {
      stmt = PSPreparedStatement.getPreparedStatement(connection, queryString);
      stmt.clearParameters();
      stmt.setInt(1, transitionID);
      stmt.setInt(2, workflowid);
      resSet = stmt.executeQuery();

      m_TransitionRoleNames_List = new ArrayList<>();
      m_TransitionRoleIds_List = new ArrayList<>();
      while (resSet.next()) {
        /**
         * @todo refactor to remove these lists and get from Map
         */
        m_TransitionRoleNames_List.add(resSet.getString("ROLENAME"));
        m_TransitionRoleIds_List.add(resSet.getInt("TRANSITIONROLEID"));

        m_transitionRoleNamesIdMap.put(
            resSet.getInt("TRANSITIONROLEID"), resSet.getString("ROLENAME"));
      }
      try {
        resSet.close();
        stmt.close();
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
      }
    } finally {
    }
  }

  public int getTransitionID() {
    return this.m_nTransitionID;
  }

  public String getTransitionLabel() {
    return this.m_sTransitionLabel;
  }

  public String getTransitionPrompt() {
    return this.m_sTransitionPrompt;
  }

  public String getTransitionDescription() {
    return this.m_sTransitionDesc;
  }

  public int getTransitionFromStateID() {
    return this.m_nTransitionFromStateID;
  }

  public int getTransitionToStateID() {
    return this.m_nTransitionToStateID;
  }

  public String getTransitionActionTrigger() {
    return this.m_sTransitionTrigger;
  }

  public int getTransitionApprovalsRequired() {
    return this.m_nTransitionApprovalsRequired;
  }

  public boolean isTransitionToInitialState() {
    return (0 == m_nTransitionFromStateID);
  }

  public boolean isTransitionToDifferentState() {
    return (m_nTransitionToStateID != m_nTransitionFromStateID);
  }

  public boolean isSelfTransition() {
    return (m_nTransitionToStateID == m_nTransitionFromStateID);
  }

  public boolean isAgingTransition() {
    return m_bIsAgingTransition;
  }

  public int getAgingType() {
    return m_nAgingType;
  }

  public int getAgingInterval() {
    return m_nAgingInterval;
  }

  public String getSystemField() {
    return m_sSystemField;
  }

  public int getTransitionCount() {
    return m_nCount;
  }

  public boolean isTransitionCommentRequired() {
    return !m_bIsAgingTransition
        && m_sTransitionComment != null
        && m_sTransitionComment.trim().equalsIgnoreCase("y");
  }

  public List<String> getTransitionActions() {
    return m_TransitionActions_List;
  }

  /**
   * Returns a list of role names for this transition
   *
   * @return may be <code>null</code>
   */
  public List<String> getTransitionRoles() {
    return m_TransitionRoleNames_List;
  }

  /**
   * This is returns the value of the TRANSITIONS.TRANSITIONROLE column.
   *
   * @return never <code>null</code> may be empty.
   */
  public String getTransitionRoleColumnValue() {
    return m_sTransitionRoles;
  }

  /**
   * Returns a list of role id for this transition
   *
   * @return may be <code>null</code>
   */
  public List<Integer> getTransitionRolesIds() {
    return m_TransitionRoleIds_List;
  }

  /**
   * Returns a map of the roles in this transition, with the roleid as the key and the rolename as
   * the value.
   *
   * @return the map, may be <code>null</code>.
   */
  public Map<Integer, String> getTransitionRoleNameIdMap() {
    return m_transitionRoleNamesIdMap;
  }

  public String getTransitionComment() {
    return m_sTransitionComment;
  }

  public boolean moveNext() throws SQLException {
    // Hibernate-backed cursor path — added for #1561 Phase 4d-1a.
    if (m_hRows != null) {
      m_hCursorIndex++;
      if (m_hCursorIndex >= m_hRows.size()) {
        return false;
      }
      populateRowFromHibernate(m_hRows.get(m_hCursorIndex));
      return true;
    }

    boolean bSuccess = m_Rs.next();
    if (!bSuccess) {
      return bSuccess;
    }

    m_nTransitionID = m_Rs.getInt("TRANSITIONID");
    m_sTransitionLabel = m_Rs.getString("TRANSITIONLABEL");
    m_sTransitionPrompt = m_Rs.getString("TRANSITIONPROMPT");
    m_sTransitionDesc = m_Rs.getString("TRANSITIONDESC");
    m_nTransitionFromStateID = m_Rs.getInt("TRANSITIONFROMSTATEID");
    m_nTransitionToStateID = m_Rs.getInt("TRANSITIONTOSTATEID");
    m_sTransitionTrigger = m_Rs.getString("TRANSITIONACTIONTRIGGER");
    m_nTransitionType = m_Rs.getInt("TRANSITIONTYPE");
    m_nAgingType = m_Rs.getInt("AGINGTYPE");
    m_nAgingInterval = m_Rs.getInt("AGINGINTERVAL");
    m_sSystemField = PSWorkFlowUtils.trimmedOrEmptyString(m_Rs.getString("SYSTEMFIELD"));
    m_bIsAgingTransition = (IPSTransitionsContext.AGING_TRANSITION == m_nTransitionType);
    m_nTransitionApprovalsRequired = m_Rs.getInt("TRANSITIONAPPROVALSREQUIRED");
    m_sTransitionComment = m_Rs.getString("TRANSITIONCOMMENTREQUIRED");
    if (m_sTransitionComment == null) {
      m_sTransitionComment = "n";
    }
    m_sTransitionActions = m_Rs.getString("TRANSITIONACTIONS");
    m_sTransitionRoles = m_Rs.getString("TRANSITIONROLES");

    if (null == m_sTransitionActions || m_sTransitionActions.trim().length() == 0) {

      m_TransitionActions_List = null;
    } else {
      m_TransitionActions_List =
          PSWorkFlowUtils.tokenizeString(m_sTransitionActions, PSWorkFlowUtils.ROLE_DELIMITER);
    }

    // Transition roles do not make sense for aging transitions
    if (m_bIsAgingTransition
        || null == m_sTransitionRoles
        || IPSTransitionsContext.NO_TRANSITION_ROLE_RESTRICTION.equals(m_sTransitionRoles)
        || m_sTransitionRoles.trim().length() == 0) {
      m_TransitionRoleNames_List = null;
    } else {
      buildRolesList(m_nTransitionID, workflowID);
    }

    return bSuccess;
  }

  /**
   * Populates the in-memory state fields from a single Hibernate {@link PSTransition} DTO row.
   * Mirrors the population logic performed by the legacy {@link #moveNext()} when reading from the
   * raw {@code ResultSet}. Phase 4d-1a only supports non-aging transitions (the {@code
   * findTransitionsByState} service query filters by {@code transitionType = TRANSITION}).
   *
   * @param row the Hibernate row, never {@code null}.
   */
  private void populateRowFromHibernate(PSTransition row) {
    m_nTransitionID = (int) row.getGUID().longValue();
    m_sTransitionLabel = row.getLabel() == null ? "" : row.getLabel();
    m_sTransitionPrompt = ""; // not persisted on the Hibernate entity — legacy default
    m_sTransitionDesc = row.getDescription() == null ? "" : row.getDescription();
    m_nTransitionFromStateID = (int) row.getStateId();
    m_nTransitionToStateID = (int) row.getToState();
    m_sTransitionTrigger = row.getTrigger() == null ? "" : row.getTrigger();
    m_nTransitionType = IPSTransitionsContext.NORMAL_TRANSITION;
    m_nAgingType = 0;
    m_nAgingInterval = 0;
    m_sSystemField = "";
    m_bIsAgingTransition = false;
    m_nTransitionApprovalsRequired = row.getApprovals();
    m_sTransitionComment = row.getRequiresComment().getTypeValue();
    m_sTransitionActions = row.getTransitionAction() == null ? "" : row.getTransitionAction();

    if (m_sTransitionActions.trim().length() == 0) {
      m_TransitionActions_List = null;
    } else {
      m_TransitionActions_List =
          PSWorkFlowUtils.tokenizeString(m_sTransitionActions, PSWorkFlowUtils.ROLE_DELIMITER);
    }

    // Determine the transition-role column value. The Hibernate entity exposes both:
    //   - a string column (transitions_roles) which carries *ALL* vs SPECIFIED marker
    //   - a List<PSTransitionRole> (the actual role IDs)
    // The legacy raw-JDBC code keys off the column string; mirror that.
    boolean isAllowAllRoles = row.isAllowAllRoles();
    m_sTransitionRoles =
        isAllowAllRoles
            ? IPSTransitionsContext.NO_TRANSITION_ROLE_RESTRICTION
            : IPSTransitionsContext.SPECIFIED_ROLE_TRANSITION_RESTRICTION;

    if (isAllowAllRoles) {
      m_TransitionRoleNames_List = null;
      m_TransitionRoleIds_List = null;
      m_transitionRoleNamesIdMap = new HashMap<>();
    } else {
      java.util.List<PSTransitionRole> roles = row.getTransitionRoles();
      m_TransitionRoleIds_List = new ArrayList<>(roles.size());
      for (PSTransitionRole tr : roles) {
        m_TransitionRoleIds_List.add((int) tr.getRoleId());
      }
      // Resolve role names via the workflow service (uses ROLES table).
      java.util.Set<Long> roleIds = new java.util.HashSet<>();
      for (PSTransitionRole tr : roles) {
        roleIds.add(tr.getRoleId());
      }
      m_TransitionRoleNames_List = new ArrayList<>(roles.size());
      m_transitionRoleNamesIdMap = new HashMap<>();
      if (!roleIds.isEmpty()) {
        IPSWorkflowService wfSvc = PSWorkflowServiceLocator.getWorkflowService();
        java.util.List<com.percussion.services.workflow.data.PSWorkflowRole> namedRoles =
            wfSvc.findWorkflowRoles(workflowID, roleIds);
        java.util.Map<Long, String> nameById = new HashMap<>();
        for (com.percussion.services.workflow.data.PSWorkflowRole nr : namedRoles) {
          nameById.put(nr.getGUID().longValue(), nr.getName());
        }
        for (PSTransitionRole tr : roles) {
          long rid = tr.getRoleId();
          String rname = nameById.get(rid);
          if (rname == null) {
            throw new IllegalStateException(
                "Workflow role name not found for workflowId=" + workflowID + ", roleId=" + rid);
          }
          m_TransitionRoleNames_List.add(rname);
          m_transitionRoleNamesIdMap.put((int) rid, rname);
        }
      }
    }
  }

  public boolean isEmpty() {
    return (0 == m_nCount);
  }

  public void close() {
    // release resources
    try {
      if (null != connection && !connection.getAutoCommit()) connection.setAutoCommit(true);
    } catch (SQLException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
    }

    try {
      if (null != m_Rs) m_Rs.close();
      m_Rs = null;
    } catch (SQLException e) {
      // quiet close
    }

    try {

      if (null != statement) statement.close();
      statement = null;
    } catch (SQLException e) {
      // quiet close
    }
  }

  /**
   * Produce a string describing the transition aging related context values, one to a line.
   *
   * @return string describing the transition aging related context values.
   */
  public String toString(boolean agingOnly) {
    if (!agingOnly) {
      return this.toString();
    }

    return "PSTransitionsContext: "
        + "\n"
        + "Transition   ID = "
        + m_nTransitionID
        + "\n"
        + "Transition   Label = "
        + m_sTransitionLabel
        + "\n"
        + "Transition   Type  = "
        + m_nTransitionType
        + "\n"
        + "Transition   Aging Type  = "
        + m_nAgingType
        + "\n"
        + "Transition   Aging Interval  = "
        + m_nAgingInterval
        + "\n"
        + "Transition   Aging System Field  = "
        + m_sSystemField
        + "\n";
  }

  public String toString() {
    return "PSTransitionsContext: "
        + "\n"
        + "Transition   ID = "
        + m_nTransitionID
        + "\n"
        + "Transition   Label = "
        + m_sTransitionLabel
        + "\n"
        + "Transition   Prompt = "
        + m_sTransitionPrompt
        + "\n"
        + "Transition   Desc = "
        + m_sTransitionDesc
        + "\n"
        + "Transition   FromStateID = "
        + m_nTransitionFromStateID
        + "\n"
        + "Transition   ToStateID = "
        + m_nTransitionToStateID
        + "\n"
        + "Transition   Trigger = "
        + m_sTransitionTrigger
        + "\n"
        + "Transition   Type  = "
        + m_nTransitionType
        + "\n"
        + "Transition   Aging Type  = "
        + m_nAgingType
        + "\n"
        + "Transition   Aging Interval  = "
        + m_nAgingInterval
        + "\n"
        + "Transition   Aging System Field  = "
        + m_sSystemField
        + "\n"
        + "Transition   ApprovalsRequired = "
        + m_nTransitionApprovalsRequired
        + "\n"
        + "Transition   CommentRequired = "
        + m_sTransitionComment
        + "\n"
        + "Transition   Actions = "
        + m_sTransitionActions
        + "\n"
        + "Transition   Roles = "
        + m_sTransitionRoles
        + "\n";
  }

  /** static constant string that represents the qualified table name. */
  private static final String TABLE_TC = "TRANSITIONS";

  private static final String TRANSITIONS_SELECT =
      "SELECT "
          + TABLE_TC
          + ".TRANSITIONID, "
          + TABLE_TC
          + ".TRANSITIONLABEL, "
          + TABLE_TC
          + ".TRANSITIONPROMPT, "
          + TABLE_TC
          + ".TRANSITIONDESC, "
          + TABLE_TC
          + ".TRANSITIONFROMSTATEID, "
          + TABLE_TC
          + ".TRANSITIONTOSTATEID, "
          + TABLE_TC
          + ".TRANSITIONACTIONTRIGGER, "
          + TABLE_TC
          + ".TRANSITIONTYPE, "
          + TABLE_TC
          + ".AGINGTYPE, "
          + TABLE_TC
          + ".AGINGINTERVAL, "
          + TABLE_TC
          + ".SYSTEMFIELD, "
          + TABLE_TC
          + ".TRANSITIONAPPROVALSREQUIRED, "
          + TABLE_TC
          + ".TRANSITIONCOMMENTREQUIRED, "
          + TABLE_TC
          + ".TRANSITIONACTIONS, "
          + TABLE_TC
          + ".TRANSITIONROLES ";

  private static final String QRYSTRING_FOR_ASSIGNMENT =
      TRANSITIONS_SELECT
          + "FROM "
          + TABLE_TC
          + " WHERE ("
          + TABLE_TC
          + ".WORKFLOWAPPID=? AND "
          + TABLE_TC
          + ".TRANSITIONACTIONTRIGGER=? "
          + "AND "
          + TABLE_TC
          + ".TRANSITIONFROMSTATEID=?)";

  private static final String QRYSTRING_WITH_TRANSITIONID =
      TRANSITIONS_SELECT
          + "FROM "
          + TABLE_TC
          + " WHERE ("
          + TABLE_TC
          + ".TRANSITIONID=? "
          + "AND "
          + TABLE_TC
          + ".WORKFLOWAPPID=?)";

  private static final String QRYSTRING_FOR_POSSIBLE_TRANSITIONS =
      TRANSITIONS_SELECT
          + "FROM "
          + TABLE_TC
          + " WHERE ("
          + TABLE_TC
          + ".WORKFLOWAPPID=? "
          + "AND "
          + TABLE_TC
          + ".TRANSITIONFROMSTATEID=?)";
}
