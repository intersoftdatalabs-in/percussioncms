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

package com.percussion.tablefactory;

import com.percussion.security.error.PSExceptionUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a place holder for several global constants or variables that need to be evaluated
 * only once.
 */
public class RxTableInstallLogic {

  /** Creates a table install logic holder. */
  public RxTableInstallLogic() {
    super();
  }

  private static final Logger log = LogManager.getLogger(RxTableInstallLogic.class);

  /** Role delimiter (in role list) */
  private static final String ROLE_DELIMITER = ",";

  /** Name of the properties file. */
  public static final String FILE_PROPERTIES = "rxworkflow.properties";

  /** Debug flag, when set to true additonal logging is done. */
  public static boolean m_bDebug = false;

  /** Name fo the file for logging. This typycally specified in the Properties file */
  public static String m_sLogFile = null;

  /** Default name for storing the new state id when transition takes place. */
  public static String DEFAULT_NEWSTATEID_NAME = "newstateid";

  /** Default name for storing the transition id. */
  public static String DEFAULT_TRANSITIONID_NAME = "transitionid";

  /**
   * Installer Options Table works as followed : Yes No Ignore No Action === == ======== ===========
   * CREATE 1 2 3 0 ALTER 1 2 3 0 DELETE 1 2 3 0
   */
  public static final int CREATE_NONE = 0;

  /** Installer option value indicating create yes. */
  public static final int CREATE_YES = 1;
  /** Installer option value indicating create no. */
  public static final int CREATE_NO = 2;
  /** Installer option value indicating create without drop. */
  public static final int CREATE_NODROP = 3;
  /** Installer option value indicating alter none. */
  public static final int ALTER_NONE = 0;
  /** Installer option value indicating alter yes. */
  public static final int ALTER_YES = 1;
  /** Installer option value indicating alter no. */
  public static final int ALTER_NO = 2;
  /** Installer option value indicating alter not applicable. */
  public static final int ALTER_NOT = 3;
  /** Installer option value indicating delete none. */
  public static final int DELETE_NONE = 0;
  /** Installer option value indicating delete yes. */
  public static final int DELETE_YES = 1;
  /** Installer option value indicating delete no. */
  public static final int DELETE_NO = 2;
  /** Installer option value indicating delete not applicable. */
  public static final int DELETE_NOT = 3;

  /** Error Messages ref write option to Disk. */
  public static final int SUCCESS = 0;

  /** Write to disk result code indicating a write error. */
  public static final int WRITE_ERRROR = 1;
  /** Write to disk result code indicating a security failure. */
  public static final int SECURITY_FAILURE = 2;
  /** Write to disk result code indicating the file already exists. */
  public static final int FILE_ALREADY_EXIST = 3;
  /** Write to disk result code indicating the file was not found. */
  public static final int FILE_NOT_FOUND = 4;
  /** Write to disk result code indicating an unknown failure. */
  public static final int UNKNOWN_FAILURE = 5;

  /**
   * Flags for writing to disk CREATE_NEW_OVERWRITE = Create new file. Overwrite the old file.
   * CREATE_NEW_OLD = Create new, rename old file with ".old" ext if exists APPEND_CREATE = Append
   * to file. Create file if doesn't exist APPEND = Append to existing file. Fail if doen't exist.
   */
  public static final int CREATE_NEW_OVERWRITE = 0;

  /** Flag for writing to disk: create new, rename old file with ".old" extension if it exists. */
  public static final int CREATE_NEW_OLD = 1;
  /** Flag for writing to disk: append to file, create file if it does not exist. */
  public static final int APPEND_CREATE = 2;
  /** Flag for writing to disk: append to existing file, fail if it does not exist. */
  public static final int APPEND = 3;

  /** Table install option indicating no action should be taken. */
  public static final int TABLE_NO_ACTION = 0;
  /** Table install option indicating the table should be created. */
  public static final int TABLE_CREATE_YES = 1;
  /** Table install option indicating the table should not be created. */
  public static final int TABLE_CREATE_NO = 2;
  /** Table install option indicating the table should be altered. */
  public static final int TABLE_ALTER_YES = 4;
  /** Table install option indicating the table should not be altered. */
  public static final int TABLE_ALTER_NO = 5;

  /**
   * Preset file name which the old data is stored in if system fails to insert them back into the
   * new tables.
   */
  public static String PS_OLD_DATA_FILENAME = new String("ps_org_tbl_data.xml");

  /** Preset file name which the old table definition is stored in if system fails to insert it back into the new tables. */
  public static String PS_OLD_DTD_FILENAME = new String("ps_org_tbl_def.xml");

  /** error constants */
  public static final int ERROR_INVALID_NMBER_OF_PARAMETERS = 7006;

  /** Error code indicating an invalid parameter type. */
  public static final int ERROR_INVALID_PARAMETER_TYPE = 7003;
  /** Error code indicating an authorization failure. */
  public static final int ERROR_AUTHORIZATION_FAILURE = 1203;

  /** Instantiation of empty properties. File will be loaded later. */
  public static Properties properties = new Properties();

  /**
   * AssignmentType: 0 - Not in workflow - Either the user is not in any role, or all of his roles
   * are not listed in the workflow definition 1 - None - User is in a role, but the role is not
   * assigned for this state 2 - Reader - User is in a role assigned as Reader 3 - Assignee - User
   * is assigned to transition the document 4 - Admin - User is an administrator in this Workflow.
   * Administrators have all rights
   */
  public static final int ASSIGNMENT_TYPE_NOT_IN_WORKFLOW = 0;

  /** Assignment type indicating the user is in a role, but the role is not assigned for this state. */
  public static final int ASSIGNMENT_TYPE_NONE = 1;
  /** Assignment type indicating the user is in a role assigned as reader. */
  public static final int ASSIGNMENT_TYPE_READER = 2;
  /** Assignment type indicating the user is assigned to transition the document. */
  public static final int ASSIGNMENT_TYPE_ASSIGNEE = 3;
  /** Assignment type indicating the user is a workflow administrator. */
  public static final int ASSIGNMENT_TYPE_ADMIN = 4;

  /**
   * HTML Parameter name that stores the current user's assignment type. authenticateUser exit adds
   * this to the list and performTransition may modify, if required.
   */
  public static final String ASSIGNMENT_TYPE_CURRENT_USER = "assignmenttypecurrentuser";

  /**
   * Document check out status values 0 - Not checked-out by anybody 1 - Checked out by current user
   * 2 - Checked out by some body else
   */
  public static final int CHECKOUT_STATUS_NONE = 0;

  /** Document checkout status indicating the document is checked out by the current user. */
  public static final int CHECKOUT_STATUS_CURRENT_USER = 1;
  /** Document checkout status indicating the document is checked out by somebody else. */
  public static final int CHECKOUT_STATUS_OTHER = 2;

  /**
   * HTML Parameter name that stores the current document's checkout status. authenticateUser exit
   * adds this to the list and performTransition may modify, if required.
   */
  public static final String CHECKOUT_STATUS_CURRENT_DOCUMENT = "checkoutstatuscurrentdocument";

  /**
   * HTML Parameter name that stores the current document's checkout user name. authenticateUser
   * exit adds this to the list and performTransition may modify, if required.
   */
  public static final String CHECKOUT_USER_NAME = "checkoutusername";

  /** HTML Parameter name that stores the content status history id after writing the history. */
  public static final String HTML_PARAM_CONTENTSTATUSHISTORYID = "contentstatushistoryid";

  /**
  * Ad-hoc options:
  *  0 - No Ad-Hoc
  *   1 - Ad-Hoc enabled
  *   2 - Anonymous Ad-Hoc
  *
  */
  public static final int ADHOC_DISABLED = 0;
  /** Ad-hoc option indicating ad-hoc is enabled. */
  public static final int ADHOC_ENABLED = 1;
  /** Ad-hoc option indicating anonymous ad-hoc is enabled. */
  public static final int ADHOC_ANONYMOUS = 2;

  /** HTML parameter name storing the new state id key. */
  public static final String SNEWSTATEIDKEY = "sNewStateIDKey";

  /** Name of the action trigger. */
  public static final String ACTION_TRIGGER_NAME = "ACTION_TRIGGER_NAME";
  /** Default action trigger name. */
  public static final String DEFAULT_ACTION_TRIGGER_NAME = "WFAction";

  /** Name of the request. */
  public static final String REQUEST_NAME = "REQUEST_NAME";
  /** Default request name. */
  public static final String DEFAULT_REQUEST_NAME = "request";

  /** Name of the action list element. */
  public static final String ACTION_LIST_ELEMENT_NAME = "ACTION_LIST_ELEMENT_NAME";
  /** Default action list element name. */
  public static final String DEFAULT_ACTION_LIST_ELEMENT_NAME = "ActionList";

  /** Name of the action element. */
  public static final String ACTION_ELEMENT_NAME = "ACTION_ELEMENT_NAME";
  /** Default action element name. */
  public static final String DEFAULT_ACTION_ELEMENT_NAME = "Action";

  /** Name of the content history request. */
  public static final String CONTENT_HISTORY_REQUEST = "CONTENT_HISTORY_REQUEST";
  /** Name of the history action element. */
  public static final String HISTORY_ACTION_ELEMENT_NAME = "HistoryAction";
  /** Name of the view action element. */
  public static final String VIEW_ACTION_ELEMENT_NAME = "ViewAction";
  /** Name of the edit action element. */
  public static final String EDIT_ACTION_ELEMENT_NAME = "EditAction";
  /** Name of the check-in/check-out action element. */
  public static final String CHECKINOUT_ACTION_ELEMENT_NAME = "CheckInOutAction";

  /** Name of the check-in trigger. */
  public static final String TRIGGER_CHECK_IN = "TRIGGER_CHECK_IN";
  /** Default check-in trigger name. */
  public static final String DEFAULT_TRIGGER_CHECK_IN = "CheckIn";

  /** Name of the check-out trigger. */
  public static final String TRIGGER_CHECK_OUT = "TRIGGER_CHECK_OUT";
  /** Default check-out trigger name. */
  public static final String DEFAULT_TRIGGER_CHECK_OUT = "CheckOut";

  /** Check-in/check-out condition indicating the condition should be ignored. */
  public static final String CHECKINOUT_CONDITION_IGNORE = "ignore";
  /** Check-in/check-out condition indicating check-in. */
  public static final String CHECKINOUT_CONDITION_CHECKIN = "checkin";
  /** Check-in/check-out condition indicating check-out. */
  public static final String CHECKINOUT_CONDITION_CHECKOUT = "checkout";

  private static ResourceBundle m_ResourceBundle = null;

  static {
    try {
      properties.load(new FileInputStream(FILE_PROPERTIES));
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }

    String temp = properties.getProperty("DEBUG", "false");
    if (null != temp && temp.equalsIgnoreCase("true")) m_bDebug = true;

    m_sLogFile = properties.getProperty("LOGFILE");
    if (null != m_sLogFile) m_sLogFile = m_sLogFile.trim();
  }

  /**
   * Helper function to compare two role lists
   *
   * @param assignmentTypeList - ArrayList containing assignment types as Integers
   * @param roleList - first role list as ArrayList;
   * @param sRoleList - the second role list as comma separated list
   * @return the assignment type - -ne of the values defined in this file (RxTableInstallLogic.java)
   */
  public static int compareRoleList(
      ArrayList<Integer> assignmentTypeList, ArrayList<String> roleList, String sRoleList) {
    StringTokenizer sTokenizer = new StringTokenizer(sRoleList, ROLE_DELIMITER);
    String sRole = "";
    String sToken = "";
    boolean bPresent = false;
    int nAssignMentType = RxTableInstallLogic.ASSIGNMENT_TYPE_NONE;
    Integer index;
    while (sTokenizer.hasMoreElements()) {
      sToken = sTokenizer.nextToken().trim();
      for (int i = 0; i < roleList.size(); i++) {
        sRole = roleList.get(i).toString();
        if (sRole.trim().equalsIgnoreCase(sToken)) {
          if (false == bPresent) bPresent = true;
          index = assignmentTypeList.get(i);
          if (index.intValue() > nAssignMentType) nAssignMentType = index.intValue();
        }
      }
    }
    return nAssignMentType;
  }

  /**
   * Determine whether the user is an administrator. <br>
   * The user is an administrator if the Workflow admin name is the user's name or is one of the
   * user's roles.
   *
   * @param sAdminName The name of the workflow administrator
   * @param sUserName The user's name, cannot be <CODE>null</CODE>
   * @param sRoleList A comma-delimited list of the user's roles.
   * @return <CODE>true</CODE> if the user is an administrator, <CODE>false</CODE> if not.
   * @throws IllegalArgumentException if the user name is <CODE>null</CODE> or empty.
   */
  public static boolean isAdmin(String sAdminName, String sUserName, String sRoleList) {
    // If the admin name is null or empty, the person is not an admin
    if (null == sAdminName || 0 == sAdminName.trim().length()) {
      return (false);
    }

    sAdminName = sAdminName.trim();

    if (null == sUserName || 0 == sUserName.trim().length()) {
      throw new IllegalArgumentException("User name cannot be empty.");
    }

    /*
     * If the user name is the same as the Workflow admin name,
     * the user is an administrator
     */
    if (sUserName.trim().equalsIgnoreCase(sAdminName)) {
      return (true);
    }

    if (null == sRoleList || 0 == sRoleList.trim().length()) {
      return (false);
    }

    /*
     * If the Workflow admin name is one of the user's roles,
     * the user is an administrator.
     */

    StringTokenizer sTokenizer = new StringTokenizer(sRoleList, ROLE_DELIMITER);
    String sToken = "";
    boolean bAdmin = false;
    Integer index;
    while (sTokenizer.hasMoreElements()) {
      sToken = sTokenizer.nextToken().trim();
      if (sAdminName.equalsIgnoreCase(sToken)) {
        bAdmin = true;
        break;
      }
    }

    return (bAdmin);
  }

  /**
   * Helper function that returns user name after last comma. This is usefule when we need to
   * extract actual user name out of the user name that is obtainable by user contex/username. This
   * has of the form: host1,host2,host3,host4,username.
   *
   * @param sUserName the user name to filter, may not be <code>null</code>.
   * @return the user name after the last comma, never <code>null</code>.
   * @since 2.0
   */
  public static String filterUserName(String sUserName) {
    String sResult = "";
    StringTokenizer sTokenizer = new StringTokenizer(sUserName, ROLE_DELIMITER);

    while (sTokenizer.hasMoreElements()) {
      sResult = sTokenizer.nextToken().trim();
      if (-1 != sResult.indexOf('*') || -1 != sResult.indexOf('.')) continue;
      break;
    }
    return sResult;
    /*
    int nLoc = sUserName.lastIndexOf(',');
    if(-1 == nLoc)
       return sUserName;
    return (sUserName.length() > nLoc+2) ? sUserName.substring(nLoc+2).trim() : null;
    */
  }

  /**
   * All the error and other messages are stored in a resource file. This static method returns
   * (instantiates if required) the resource bundle.
   *
   * @return the resource bundle, never <code>null</code>.
   */
  public static ResourceBundle getResourceBundle() {
    /* load the resources first. this will throw an exception if we can't
    find them */
    if (null == m_ResourceBundle)
      m_ResourceBundle =
          ResourceBundle.getBundle(
              "com.percussion.cms.workflow." + "RxTableInstallLogicResources", Locale.getDefault());

    return m_ResourceBundle;
  }
}
