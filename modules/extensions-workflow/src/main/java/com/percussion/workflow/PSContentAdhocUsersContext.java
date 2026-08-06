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

import com.percussion.extension.IPSExtensionErrors;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.workflow.data.PSContentAdhocUser;
import com.percussion.util.PSPreparedStatement;
import com.percussion.util.PSSqlHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PSContentAdhocUsersContext class is a wrapper class providing access to the records and fields of
 * the backend table 'CONTENTADHOCUSERS'.
 */
@Deprecated
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class PSContentAdhocUsersContext implements IPSContentAdhocUsersContext {

  private static final Logger log = LogManager.getLogger(PSContentAdhocUsersContext.class);

  /**
   * Constructor specifying content ID, used to create a context with no content adhoc user
   * information in its local variables.
   *
   * @param contentID content ID of the item/document
   */
  public PSContentAdhocUsersContext(int contentID) {
    m_nContentID = contentID;
  }

  /**
   * Hibernate-backed factory added for #1561 Phase 4b. Loads the adhoc-user data for the specified
   * content id via the shared Hibernate session (no second pool connection).
   *
   * <p>Equivalent to the raw-JDBC constructor {@link #PSContentAdhocUsersContext(int, Connection)}
   * but participates in the surrounding Spring transaction so the dual-connection defect fixed in
   * Phase 2/3 does not re-emerge for this code path.
   *
   * @param contentID the content id of the item, must be {@code > 0}.
   * @return a context populated with the rows for {@code contentID}; never {@code null}.
   */
  public static PSContentAdhocUsersContext loadFromHibernate(int contentID) {
    if (contentID <= 0) {
      throw new IllegalArgumentException("contentID must be > 0");
    }
    PSContentAdhocUsersContext ctx = new PSContentAdhocUsersContext(contentID);
    List<PSContentAdhocUser> rows =
        PSSystemServiceLocator.getSystemService().findContentAdhocUsers(contentID);
    ctx.populateFromHibernate(rows);
    return ctx;
  }

  /**
   * Populates the in-memory state from a list of {@link PSContentAdhocUser} rows. Mirrors the
   * raw-JDBC constructor's classification into {@code m_userNameToAdhocNormalRoleIDMap} / {@code
   * m_adhocAnonymousRoleIDs} / {@code m_adhocAnonymousUserNames} / etc. so the legacy {@link
   * PSWorkflowRoleInfoStatic#getActorRoles(String, String, PSStateRolesContext,
   * PSContentAdhocUsersContext, boolean)} no-connection overload works the same way it does against
   * the raw-JDBC-populated state.
   *
   * @param rows the Hibernate rows, may be empty but never {@code null}.
   */
  private void populateFromHibernate(List<PSContentAdhocUser> rows) {
    if (rows == null) return;
    for (PSContentAdhocUser row : rows) {
      int roleID = (int) row.getRoleId();
      int adhocType = row.getAdhocType();
      String userName = row.getUser();
      if (userName == null || userName.isEmpty()) {
        throw new IllegalStateException(
            "Content adhoc user has empty username for content " + m_nContentID);
      }
      String lowerCaseUserName = userName.toLowerCase();
      m_nCount++;

      if (!m_adhocRoleIDtoAdhocTypeMap.containsKey(roleID)) {
        m_adhocRoleIDtoAdhocTypeMap.put(roleID, adhocType);
      }

      if (adhocType == PSWorkFlowUtils.ADHOC_ENABLED) {
        List<Integer> workingList;
        if (!m_userNameToAdhocNormalRoleIDMap.containsKey(lowerCaseUserName)) {
          m_adhocNormalUserNames.add(userName);
          workingList = new ArrayList<>();
        } else {
          workingList = m_userNameToAdhocNormalRoleIDMap.get(lowerCaseUserName);
        }
        workingList.add(roleID);
        m_userNameToAdhocNormalRoleIDMap.put(lowerCaseUserName, workingList);
      } else if (adhocType == PSWorkFlowUtils.ADHOC_ANONYMOUS) {
        if (!m_lowerCaseUserNameToUserNameMap.containsKey(lowerCaseUserName)) {
          m_adhocAnonymousUserNames.add(userName);
          m_lowerCaseUserNameToUserNameMap.put(lowerCaseUserName, userName);
        }
        if (!m_adhocAnonymousRoleIDs.contains(roleID)) {
          m_adhocAnonymousRoleIDs.add(roleID);
        }
      } else {
        // ADHOC_DISABLED rows are not present in CONTENTADHOCUSERS by definition;
        // throw to mirror the raw-JDBC behaviour if the schema is ever extended.
        throw new IllegalStateException(
            "Invalid adhoc type "
                + adhocType
                + " for role "
                + roleID
                + " in content "
                + m_nContentID);
      }
    }
  }

  /**
   * Get the content id supplied during construction.
   *
   * @return The content id.
   */
  public int getContentId() {
    return m_nContentID;
  }

  /**
   * Constructor specifying content ID and database connection, retrieves information from the
   * database.
   *
   * @param contentID content ID of the current item/document
   * @param connection database connection - must not be <CODE>null</CODE>
   * @throws SQLException if an SQL error occurs
   * @throws IllegalArgumentException if the connection is <CODE>null</CODE>
   * @throws PSRoleException if a workflow role error occurs while loading the adhoc users
   */
  @Deprecated // TODO: This class needs refactored to use spring  hibernate
  public PSContentAdhocUsersContext(int contentID, Connection connection)
      throws PSRoleException, SQLException {
    this(contentID);
    String lowerCaseUserName;
    List<Integer> workingList;
    Integer roleID;
    if (null == connection) {
      throw new IllegalArgumentException("Connection cannot be null: " + TABLE_CAU);
    }
    this.m_connection = connection;
    try {
      m_Statement = PSPreparedStatement.getPreparedStatement(m_connection, QRYSTRING);
      m_Statement.clearParameters();
      m_Statement.setInt(1, contentID);

      // collect a list of usernames and assignment types
      m_Rs = m_Statement.executeQuery();
      m_nCount = 0;
      while (moveNext()) {
        if (m_sUserName.length() == 0) {
          throw new PSRoleException(IPSExtensionErrors.USERNAME_NULL_EMPTY_TRIM);
        }
        m_nCount++;

        lowerCaseUserName = m_sUserName.toLowerCase();
        roleID = m_nRoleID;

        if (!m_adhocRoleIDtoAdhocTypeMap.containsKey(roleID)) {
          m_adhocRoleIDtoAdhocTypeMap.put(roleID, m_nAdhocType);
        }

        if (m_nAdhocType == PSWorkFlowUtils.ADHOC_ENABLED) {
          if (!m_userNameToAdhocNormalRoleIDMap.containsKey(lowerCaseUserName)) {
            m_adhocNormalUserNames.add(m_sUserName);
            workingList = new ArrayList<>();
          } else {
            workingList = m_userNameToAdhocNormalRoleIDMap.get(lowerCaseUserName);
          }
          workingList.add(roleID);
          m_userNameToAdhocNormalRoleIDMap.put(lowerCaseUserName, workingList);
        } else if (m_nAdhocType == PSWorkFlowUtils.ADHOC_ANONYMOUS) {
          if (!m_lowerCaseUserNameToUserNameMap.containsKey(lowerCaseUserName)) {
            m_adhocAnonymousUserNames.add(m_sUserName);
            m_lowerCaseUserNameToUserNameMap.put(lowerCaseUserName, m_sUserName);
          }

          if (!m_adhocAnonymousRoleIDs.contains(roleID)) {
            m_adhocAnonymousRoleIDs.add(roleID);
          }
        } else {
          throw new PSRoleException(IPSExtensionErrors.INVALID_ADHOC);
        }
      }
    } finally {
      close();
    }
  }

  /**
   * Moves the cursor to next record in the resultset and updates the current column values.
   *
   * @return <CODE>true</CODE> if another record is read else <CODE>false</CODE>
   * @throws SQLException A SQL error
   */
  private boolean moveNext() throws SQLException {
    boolean bSuccess = m_Rs.next();
    if (!bSuccess) {
      return bSuccess;
    }
    m_sUserName = PSWorkFlowUtils.trimmedOrEmptyString(m_Rs.getString(USERNAME));
    m_nAdhocType = m_Rs.getInt(ADHOCTYPE);
    m_nRoleID = m_Rs.getInt(ROLEID);

    return bSuccess;
  }

  /** Closes the result set and statement if necessary */
  private void close() {
    // release resources
    try {
      if (null != m_Rs) {
        m_Rs.close();
        m_Rs = null;
      }

      if (null != m_Statement) {
        m_Statement.close();
        m_Statement = null;
      }
    } catch (SQLException e) {
      //  we ignore this exception, since we are trying to clean up
    }
  }

  /**
   * Deletes database entries for all adhoc assignees for this content item, optionally clearing
   * context variables containing this information.
   *
   * @param clearStateVariables <CODE>true</CODE> if context variables should be cleared, else
   *     <CODE>false</CODE>.
   * @return number of entries deleted
   * @throws SQLException if an SQL error occurs
   */
  private int emptyAdhocUserEntries(boolean clearStateVariables) throws SQLException {
    int numEntriesDeleted;
    boolean modifiedCommit = false;
    try {
      if (m_connection.getAutoCommit()) {
        m_connection.setAutoCommit(false);
        modifiedCommit = true;
      }
      m_Statement = null;
      m_Statement = PSPreparedStatement.getPreparedStatement(m_connection, DELETESTRING);
      m_Statement.clearParameters();
      m_Statement.setInt(1, m_nContentID);

      numEntriesDeleted = m_Statement.executeUpdate();
      PSSqlHelper.commit(m_connection);

      if (clearStateVariables) {
        m_nCount = 0;
        if (null != m_userNameToAdhocNormalRoleIDMap) {
          m_userNameToAdhocNormalRoleIDMap.clear();
        }

        if (null != m_adhocNormalUserNames) {
          m_lowerCaseUserNameToUserNameMap.clear();
        }

        if (null != m_adhocNormalUserNames) {
          m_adhocNormalUserNames.clear();
        }

        if (null != m_adhocAnonymousUserNames) {
          m_adhocAnonymousUserNames.clear();
        }

        if (null != m_adhocAnonymousRoleIDs) {
          m_adhocAnonymousRoleIDs.clear();
        }
      }
      return numEntriesDeleted;
    } catch (NullPointerException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } finally {
      try {
        if (modifiedCommit) m_connection.setAutoCommit(true);
        close();
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  /* **** Implementation of the IPSContentAdhocUsersContext interface**** */

  public int commit(Connection connection) throws SQLException {
    if (m_dataOutOfSync)
      throw new IllegalStateException("Cannot call commit if data is out of sync");

    Iterator<String> nameIter;
    Iterator<Integer> roleIDIter;

    if (m_connection != null) {
      m_connection = connection;
      // Do not clear the state variables
      this.emptyAdhocUserEntries(false);
    }

    m_connection = connection;
    int nRows;

    try {
      /*
       * Insert the records for the adhoc normal users.
       * Each user has a list of adhoc normal roles.
       */

      m_nAdhocType = PSWorkFlowUtils.ADHOC_ENABLED;
      nameIter = m_adhocNormalUserNames.iterator();
      while (nameIter.hasNext()) {
        m_sUserName = nameIter.next();
        String lowerCaseUserName = m_sUserName.toLowerCase();
        List<Integer> workingList = m_userNameToAdhocNormalRoleIDMap.get(lowerCaseUserName);
        if (null == workingList) {
          // shouldn't happen
          continue;
        }

        roleIDIter = workingList.iterator();
        // the repeated code below could go into a method
        while (roleIDIter.hasNext()) {
          m_nRoleID = roleIDIter.next();
          m_Statement = null;
          m_Statement = PSPreparedStatement.getPreparedStatement(m_connection, INSERTSTRING);
          m_Statement.clearParameters();
          m_Statement.setInt(1, m_nContentID);
          m_Statement.setString(2, m_sUserName);
          m_Statement.setInt(3, m_nAdhocType);
          m_Statement.setInt(4, m_nRoleID);
          nRows = m_Statement.executeUpdate();

          if (nRows > 0) // Should always be true, no exception was thrown
          {
            m_nCount += nRows;
          }
        }
      }

      /*
       * Insert the records for the adhoc anonymous users.
       * Each user acts in all the adhoc anonymous roles.
       */

      m_nAdhocType = PSWorkFlowUtils.ADHOC_ANONYMOUS;
      nameIter = m_adhocAnonymousUserNames.iterator();

      while (nameIter.hasNext()) {
        m_sUserName = nameIter.next();
        roleIDIter = m_adhocAnonymousRoleIDs.iterator();
        // this repeats code above, could go into a method
        while (roleIDIter.hasNext()) {
          m_nRoleID = roleIDIter.next();
          m_Statement = null;
          m_Statement = PSPreparedStatement.getPreparedStatement(m_connection, INSERTSTRING);
          m_Statement.clearParameters();
          m_Statement.setInt(1, m_nContentID);
          m_Statement.setString(2, m_sUserName);
          m_Statement.setInt(3, m_nAdhocType);
          m_Statement.setInt(4, m_nRoleID);
          nRows = m_Statement.executeUpdate();

          if (nRows > 0) // Should always be true, no exception was thrown
          {
            m_nCount += nRows;
          }
        }
      }
      PSSqlHelper.commit(m_connection);
      return m_nCount;
    } catch (NullPointerException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } finally {
      try {
        close();
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  /**
   * Hibernate-backed #1561 Phase 4d-1b write path. Inserts the in-memory adhoc user rows ({@code
   * m_adhocNormalUserNames} + {@code m_adhocAnonymousUserNames}) into {@code CONTENTADHOCUSERS} via
   * {@code IPSSystemService.saveContentAdhocUsers} on the shared Hibernate session — no second pool
   * connection. The legacy raw-JDBC {@code commit(Connection)} overload above is preserved for any
   * external caller that still passes a JDBC {@code Connection}.
   *
   * @return the number of rows inserted.
   */
  public int commit() {
    if (m_dataOutOfSync) {
      throw new IllegalStateException("Cannot call commit if data is out of sync");
    }
    List<PSContentAdhocUser> rows = new ArrayList<>();
    // Adhoc-normal users: one row per (user, role) pair.
    int adhocTypeNormal = PSWorkFlowUtils.ADHOC_ENABLED;
    for (String userName : m_adhocNormalUserNames) {
      List<Integer> roleIDs = m_userNameToAdhocNormalRoleIDMap.get(userName.toLowerCase());
      if (roleIDs == null) continue;
      for (Integer roleId : roleIDs) {
        PSContentAdhocUser row = new PSContentAdhocUser();
        row.setContentId(m_nContentID);
        row.setUser(userName);
        row.setRoleId(roleId);
        row.setAdhocType(adhocTypeNormal);
        rows.add(row);
      }
    }
    // Adhoc-anonymous users: one row per (user, role) pair.
    int adhocTypeAnon = PSWorkFlowUtils.ADHOC_ANONYMOUS;
    for (String userName : m_adhocAnonymousUserNames) {
      for (Integer roleId : m_adhocAnonymousRoleIDs) {
        PSContentAdhocUser row = new PSContentAdhocUser();
        row.setContentId(m_nContentID);
        row.setUser(userName);
        row.setRoleId(roleId);
        row.setAdhocType(adhocTypeAnon);
        rows.add(row);
      }
    }
    PSSystemServiceLocator.getSystemService().saveContentAdhocUsers(rows);
    m_nCount = rows.size();
    return m_nCount;
  }

  @Override
  public void addUserAdhocNormalRoleIDs(String userName, List<Integer> roleIDs) {
    String lowerCaseUserName;
    List<Integer> workingList;
    // validate
    if ((null == userName) || userName.length() == 0) {
      throw new IllegalArgumentException(
          "The user name must not be null or empty in " + "addUserAdhocNormalRoleIDs");
    }
    userName = userName.trim();
    if (userName.length() == 0) {
      throw new IllegalArgumentException(
          "The user name must not be " + "empty after trimming in addUserAdhocNormalRoleIDs");
    }
    lowerCaseUserName = userName.toLowerCase();
    if (null == roleIDs || roleIDs.isEmpty()) {
      throw new IllegalArgumentException(
          "The user role ID list  must not be " + "null or empty in addUserAdhocNormalRoleIDs");
    }

    if (!m_userNameToAdhocNormalRoleIDMap.containsKey(lowerCaseUserName)) {
      m_adhocNormalUserNames.add(userName);
      workingList = roleIDs;
    } else {
      workingList = m_userNameToAdhocNormalRoleIDMap.get(lowerCaseUserName);
      workingList.addAll(roleIDs);
    }
    m_userNameToAdhocNormalRoleIDMap.put(lowerCaseUserName, workingList);
  }

  @Override
  public void setAdhocAnonymousUsersAndRoles(List<String> userNames, List<Integer> roleIDs) {

    // validate
    if ((null != userNames) && userNames.isEmpty()) {
      throw new IllegalArgumentException(
          "The user name list must not be empty " + "in addAdhocAnonymousUsers");
    }

    if (null != roleIDs && roleIDs.isEmpty()) {
      throw new IllegalArgumentException(
          "The user role ID list  must not be " + "empty in addAdhocAnonymousUsers");
    }

    if (!((null == userNames) == (null == roleIDs))) {
      throw new IllegalArgumentException(
          "It is not allowed for only one of the lists of user names "
              + "and role IDs to be null.");
    }
    m_adhocAnonymousUserNames = userNames;
    m_adhocAnonymousRoleIDs = roleIDs;
  }

  public List<Integer> getUserAdhocNormalRoleIDs(String userName) {
    if ((null == userName) || userName.length() == 0) {
      throw new IllegalArgumentException("The user name must not be null");
    }
    userName = userName.trim().toLowerCase();
    if (userName.length() == 0) {
      throw new IllegalArgumentException("The user name must not be empty after trimming");
    }

    return m_userNameToAdhocNormalRoleIDMap.get(userName);
  }

  public List<String> getAdhocNormalUserNames() {
    return m_adhocNormalUserNames;
  }

  public List<String> getAdhocAnonymousUserNames() {
    return m_adhocAnonymousUserNames;
  }

  public List<Integer> getAdhocAnonymousRoleIDs() {
    return m_adhocAnonymousRoleIDs;
  }

  public int getContentAdhocNormalUserCount() {
    return m_adhocNormalUserNames.size();
  }

  public int getContentAdhocAnonymousUserCount() {
    return m_adhocAnonymousUserNames.size();
  }

  /**
   * Returns the role IDs from the supplied list that have no adhoc users assigned. The input list
   * is not modified.
   *
   * @param roleIDList list of role IDs to check, may be <code>null</code> or empty.
   * @return a new list containing the role IDs that have no adhoc users, never <code>null</code>.
   */
  public List<Integer> getEmptyAdhocRoles(List<Integer> roleIDList) {
    List<Integer> emptyAdhocRoles = new ArrayList<>();
    Integer roleID;
    if (null == roleIDList || roleIDList.isEmpty()) {
      return emptyAdhocRoles;
    }

    for (Integer integer : roleIDList) {
      roleID = integer;
      if (null != roleID && !hasAdhocUsers(roleID)) {
        emptyAdhocRoles.add(roleID);
      }
    }
    return emptyAdhocRoles;
  }

  /**
   * Determines whether the given role has any adhoc users assigned.
   *
   * @param roleID the role ID to check.
   * @return <code>true</code> if the role has adhoc users assigned, otherwise <code>false</code>.
   */
  public boolean hasAdhocUsers(int roleID) {
    return (m_adhocRoleIDtoAdhocTypeMap.containsKey(roleID));
  }

  public boolean isEmpty() {
    return (0 == m_nCount);
  }

  public int emptyAdhocUserEntries(Connection connection) throws SQLException {
    return emptyAdhocUserEntries(connection, true);
  }

  public int emptyAdhocUserEntries(Connection connection, boolean clearState) throws SQLException {
    if (m_dataOutOfSync)
      throw new IllegalStateException("Cannot call emptyAdhocUserEntries() if data is out of sync");

    m_connection = connection;
    int result = this.emptyAdhocUserEntries(clearState);
    m_dataOutOfSync = !clearState;

    return result;
  }

  /**
   * Hibernate-backed #1561 Phase 4d-1b write path. Deletes all {@code CONTENTADHOCUSERS} rows for
   * the current {@code contentID} via {@code IPSSystemService.deleteContentAdhocUsers} on the
   * shared Hibernate session — no second pool connection. The legacy raw-JDBC {@code
   * emptyAdhocUserEntries(Connection, boolean)} overload above is preserved for any external caller
   * that still passes a JDBC {@code Connection}.
   *
   * @param clearState <CODE>true</CODE> if context variables should be cleared, else <CODE>false
   *     </CODE>.
   * @return number of entries deleted.
   */
  public int emptyAdhocUserEntriesViaHibernate(boolean clearState) {
    int result = PSSystemServiceLocator.getSystemService().deleteContentAdhocUsers(m_nContentID);
    if (clearState) {
      clearStateVariables();
    } else {
      m_dataOutOfSync = true;
    }
    return result;
  }

  private void clearStateVariables() {
    m_adhocNormalUserNames.clear();
    m_adhocAnonymousUserNames.clear();
    m_adhocAnonymousRoleIDs.clear();
    m_userNameToAdhocNormalRoleIDMap.clear();
    m_dataOutOfSync = false;
  }

  /* ** End Implementation of the IPSContentAdhocUsersContext interface ** */

  /**
   * Produces a string representation of the content adhoc users context.
   *
   * @return a string representation of the content adhoc users context
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("\nPSContentAdhocUsersContext:");
    if (!m_userNameToAdhocNormalRoleIDMap.isEmpty()) {
      buf.append("\nMap Adhoc normal usernames -> role IDs = ");
      buf.append(m_userNameToAdhocNormalRoleIDMap);
    }

    if (m_adhocNormalUserNames.isEmpty()) {
      buf.append("\nNo adhoc normal assignees");
    } else {
      buf.append("\nUser names of adhoc normal assignees = ");
      buf.append(getAdhocNormalUserNames().toString());
    }

    if (m_adhocAnonymousUserNames.isEmpty()) {
      buf.append("\nNo adhoc anonymous assignees");
    } else {
      buf.append("\nUser names of adhoc anonymous assignees = ");
      buf.append(getAdhocAnonymousUserNames().toString());
    }

    if (!m_adhocAnonymousRoleIDs.isEmpty()) {
      buf.append("\nRole IDs for adhoc anonymous assignees = ");
      buf.append(getAdhocAnonymousRoleIDs().toString());
    }
    return buf.toString();
  }

  /** Reusable prepared statement object. */
  private PreparedStatement m_Statement = null;

  /** Local variable referencing the opened connection. */
  private Connection m_connection = null;

  /** Reusable ResultSet object. */
  private ResultSet m_Rs = null;

  /** Number of adhoc user records found. */
  private int m_nCount = 0;

  /** Content ID for which adhoc users are valid */
  private int m_nContentID;

  /** Username at the current cursor position - updated every time moveNext(). is called */
  private String m_sUserName = "";

  /**
   * Adhoc type at the current cursor position - updated every time moveNext() is called, valid
   * values in the database are <CODE>PSWorkFlowUtils.ADHOC_ENABLED</CODE> and <CODE>
   * PSWorkFlowUtils.ADHOC_ANONYMOUS</CODE>
   */
  private int m_nAdhocType = PSWorkFlowUtils.ADHOC_DISABLED;

  /** RoleID at the current cursor position - updated every time moveNext(). is called. */
  private int m_nRoleID = 0;

  /** Map of role IDs for adhoc normal assignees with trimmed lowercase username as key */
  private Map<String, List<Integer>> m_userNameToAdhocNormalRoleIDMap = new HashMap<>();

  /** Map from adhoc role IDs to adhoc type */
  private Map<Integer, Integer> m_adhocRoleIDtoAdhocTypeMap = new HashMap<>();

  /** Map of usernames for adhoc anonymous assignees with trimmed lowercase username as key */
  private Map<String, String> m_lowerCaseUserNameToUserNameMap = new HashMap<>();

  /** List of usernames of adhoc normal assignees */
  private List<String> m_adhocNormalUserNames = new ArrayList<>();

  /** List of usernames of adhoc anonymous assignees */
  private List<String> m_adhocAnonymousUserNames = new ArrayList<>();

  /** List of role IDs for adhoc anonymous assignees */
  private List<Integer> m_adhocAnonymousRoleIDs = new ArrayList<>();

  /**
   * Flag to indicate if the in memory data is out of sync with the repository after calling a
   * method to save state to the database. This may happen if {@link
   * #emptyAdhocUserEntries(Connection, boolean)} is called passing <code>false</code> for the
   * <code>clearState</code> param.
   */
  private boolean m_dataOutOfSync = false;

  /** static constant string that represents the column name in the table */
  private static final String USERNAME = "USERNAME";

  /** static constant string that represents the column name in the table */
  private static final String ADHOCTYPE = "ADHOCTYPE";

  /** static constant string that represents the column name in the table */
  private static final String ROLEID = "ROLEID";

  /** static constant string that represents the unqualified table name. */
  private static final String CONTENTADHOCUSERS = "CONTENTADHOCUSERS";

  /**
   * Qualified table name only. Do not use as a column qualifier — {@code schema.table.column} fails
   * on H2 ({@code Column "PUBLIC" not found}).
   */
  private static final String TABLE_CAU = "CONTENTADHOCUSERS";

  /** SQL query — unqualified column names for H2-safe schema-qualified table names. */
  private static final String QRYSTRING =
      "SELECT USERNAME, ADHOCTYPE, ROLEID FROM " + TABLE_CAU + " WHERE (CONTENTID=?)";

  /** SQL insert — unqualified column names for H2-safe schema-qualified table names. */
  private static final String INSERTSTRING =
      "INSERT INTO " + TABLE_CAU + " (CONTENTID, USERNAME, ADHOCTYPE, ROLEID) VALUES(?,?,?,?)";

  /**
   * SQL string to delete all data base record for approvals for given content item with a given
   * initial state.
   */
  private static final String DELETESTRING = "DELETE FROM " + TABLE_CAU + " WHERE (CONTENTID=?)";
}
