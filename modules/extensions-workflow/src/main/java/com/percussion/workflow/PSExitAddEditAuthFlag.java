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

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.IPSConstants;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSCms;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This exit is used to enforce workflow security when we are in active assembly. This exit
 * determines if the user has the authority to edit the current active item and its parent item. Two
 * flags will be added to the activeitem element "editauthorized" and "parenteditauthorized". The
 * "editauthorized" flag, if set to "yes" means the user can edit the activeitem. The
 * "parenteditauthorized" flag, if set to "yes" means the user can edit the parent item. This exit
 * is only meant to be used on the "sys_rcSupport/activeitem.xml" resource.
 */
public class PSExitAddEditAuthFlag implements IPSResultDocumentProcessor {

  /** Default constructor for the extension framework. */
  public PSExitAddEditAuthFlag() {}


  private static final Logger log = LogManager.getLogger(PSExitAddEditAuthFlag.class);

  /*
   * Implementation of the method required by the interface IPSExtension.
   */
  @SuppressWarnings("unused")
  public void init(IPSExtensionDef extensionDef, File file) throws PSExtensionException {
    ms_fullExtensionName = extensionDef.getRef().toString();
  }

  /*
   * Implementation of the method required by the interface
   * IPSResultDocumentProcessor.
   */
  public boolean canModifyStyleSheet() {
    return false;
  }

  /*
   * Implementation of the method required by the interface
   * IPSResultDocumentProcessor.
   */
  @SuppressWarnings({"unused", "deprecation"})
  public Document processResultDocument(Object[] params, IPSRequestContext request, Document resDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {

    // If no result document then do nothing
    if (null == resDoc) return null;

    // Get language
    String lang =
        (String) request.getSessionPrivateObject(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
    if (lang == null) lang = PSI18nUtils.DEFAULT_LANG;

    // Create the parameter container object
    AuthParams localParams = new AuthParams();
    localParams.m_request = request;

    Element activeItemElem;

    try {

      activeItemElem = resDoc.getDocumentElement();
      // Set content id  and revision for the active item. We get this value
      // from the result documents contentid attribute.
      try {
        localParams.m_contentID =
            Integer.parseInt(activeItemElem.getAttribute(XML_ATTRIB_CONTENTID));
        localParams.m_revision = Integer.parseInt(activeItemElem.getAttribute(XML_ATTRIB_REVISION));
      } catch (NumberFormatException nfe) {
        return resDoc;
      }

      // Set username
      if (null == params[0] || params[0].toString().trim().length() == 0) {
        throw new PSInvalidParameterTypeException(lang, IPSExtensionErrors.EMPTY_USRNAME1);
      }
      localParams.m_userName = params[0].toString();
      localParams.m_userName = PSWorkFlowUtils.filterUserName(localParams.m_userName);
      if (0 == localParams.m_userName.length()) {
        throw new PSInvalidParameterTypeException(lang, IPSExtensionErrors.EMPTY_USRNAME2);
      }

      // Set RoleNameList
      if (null == params[1] || params[1].toString().trim().length() == 0) {
        throw new PSInvalidParameterTypeException(
            lang, IPSExtensionErrors.EMPTY_ROLE_LIST, localParams.m_userName);
      }
      localParams.m_roleNameList = params[1].toString();

      try {
        boolean canEdit =
            PSCms.canReadInFolders(localParams.m_contentID)
                && canUserEditContent(localParams);
        String strCanEdit = canEdit ? "yes" : "no";
        // Set the edit authorization flag attribute
        activeItemElem.setAttribute(XML_ATTRIB_EDIT_AUTH, strCanEdit);

        // Set content id for the parent item. We get this value
        // from the result documents contentid attribute.
        try {
          localParams.m_contentID =
              Integer.parseInt(activeItemElem.getAttribute(XML_ATTRIB_PARENTCONTENTID));
        } catch (NumberFormatException nfe) {
          return resDoc;
        }
        // Set the edit authorization flag attribute for parent
        strCanEdit = canUserEditContent(localParams) ? "yes" : "no";
        activeItemElem.setAttribute(XML_ATTRIB_PARENT_EDIT_AUTH, strCanEdit);

      } catch (Exception e) {
        PSWorkFlowUtils.printWorkflowException(request, e);
        throw new PSExtensionProcessingException(ms_fullExtensionName, e);
      }
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      log.error(t.getMessage());
      log.debug(t.getMessage(), t);
    } finally {
      PSWorkFlowUtils.printWorkflowMessage(request, "Exiting PSExitAddEditAuthFlag....");
    }

    return resDoc;
  }

  /**
   * This method verifies if the current user is allowed to edit the specified content item. It uses
   * the same authorization checks used in PSExitAuthenticateUser.
   *
   * <p>Phase 4b: the {@code Connection} argument is gone — this method now reads {@code CONTENTSTATUS}
   * via {@code PSCmsObjectMgr#loadComponentSummary(int)} and the {@code STATEROLES} +
   * {@code CONTENTADHOCUSERS} data via the new {@code loadFromHibernate} factories on
   * {@link PSStateRolesContext} and {@link PSContentAdhocUsersContext}, so the exit no longer opens
   * a second pool connection.
   *
   * @param localParams object containing parameters that will be used to authorize the user.
   * @return <code>true</code> if the user is allowed to edit this content item, else <code>false
   *     </code>.
   * @throws Exception catches all errors from the Hibernate lookup / role helpers.
   */
  private boolean canUserEditContent(AuthParams localParams) throws Exception {
    PSWorkFlowUtils.printWorkflowMessage(localParams.m_request, "  Entering canUserEditContent");

    int contentID = localParams.m_contentID;
    String userName = localParams.m_userName;
    String roleNameList = localParams.m_roleNameList;
    int requiredAccessLevel = localParams.m_requiredAccessLevel;
    int assignmentType;
    List<Integer> actorRoles;

    IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
    PSComponentSummary csc = cms.loadComponentSummary(contentID);
    if (csc == null) {
      PSWorkFlowUtils.printWorkflowMessage(
          localParams.m_request, "  No entry for this content. Exiting canUserEditContent");
      return true; // no entry for this content so proceed to transition
    }

    int nWorkFlowAppID = csc.getWorkflowAppId();
    int itemCommunityID = csc.getCommunityId();
    int userCommunityID = -1;
    String usercomm =
        (String) localParams.m_request.getSessionPrivateObject(IPSHtmlParameters.SYS_COMMUNITY);
    if (usercomm != null) userCommunityID = Integer.parseInt(usercomm);

    Optional<IPSWorkflowAppsContext> wacOpt = cms.loadWorkflowAppContext(nWorkFlowAppID);
    String sAdminName;
    if (wacOpt.isPresent()) {
      IPSWorkflowAppsContext wac = wacOpt.get();
      sAdminName = wac.getWorkFlowAdministrator();
    } else {
      // no workflow application context available, treat as no admin
      sAdminName = "";
    }

    // if the login community and user community are different from return
    // false
    if (itemCommunityID != userCommunityID) return false;

    // Check whether the user is Workflow admin
    boolean isAdmin = PSWorkFlowUtils.isAdmin(sAdminName, userName, roleNameList);

    // Determine the checkout status and checkedout user
    // and return false if the content is checked out
    // by another user or not checked out.

    String checkedOutUser = csc.getCheckoutUserName();
    if (null == checkedOutUser || checkedOutUser.trim().length() < 1) {
      // content item not checked out
      return false;
    } else {
      checkedOutUser = checkedOutUser.trim();
      if (!userName.equalsIgnoreCase(checkedOutUser)) {
        // content item checked out, but not by you
        return false;
      }
    }

    // If the user is Workflow admin, there is no more to do
    if (isAdmin) {
      PSWorkFlowUtils.printWorkflowMessage(
          localParams.m_request, "  User is Admin, done. \n  Exiting canUserEditContent");
      return true;
    }

    PSStateRolesContext src;
    try {
      src = PSStateRolesContext.loadFromHibernate(
          nWorkFlowAppID, csc.getContentStateId(), requiredAccessLevel);
    } catch (PSEntryNotFoundException | PSRoleException e) {
      return false;
    }

    PSContentAdhocUsersContext cauc =
        PSContentAdhocUsersContext.loadFromHibernate(contentID);

    actorRoles =
        PSWorkflowRoleInfoStatic.getActorRoles(userName, roleNameList, src, cauc, true);

    if (null == actorRoles || actorRoles.isEmpty()) {
      return false;
    }
    assignmentType = PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoles);
    if (PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE == assignmentType) {
      return false;
    }

    PSWorkFlowUtils.printWorkflowMessage(localParams.m_request, "  Exiting canUserEditContent");
    return true;
  }

  /**
   * This is an inner class to encapsulate the parameters. We cannot keep these as class variables
   * due to threading issues. We instantiate this object in the main processrequest method (called
   * by server) and pass around the methods. This is meant for convenience only.
   */
  private static class AuthParams {
    /** The content id of the active item */
    public int m_contentID = 0;

    /** The revision number of the active item */
    public int m_revision = 0;

    /** The current users' username */
    public String m_userName = null;

    /** The list of roles this user is in */
    public String m_roleNameList = null;

    /** The access level required to edit content */
    public int m_requiredAccessLevel = PSWorkFlowUtils.ASSIGNMENT_TYPE_ASSIGNEE;

    /** The assignment type for this content */
    public int m_assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;

    /** The request context passed in */
    public IPSRequestContext m_request = null;
  }

  /** The fully qualified name of this extension. */
  private static String ms_fullExtensionName = "";

  /** The content id XML attribute. */
  private static final String XML_ATTRIB_CONTENTID = "contentid";

  /** The revision id XML attribute. */
  private static final String XML_ATTRIB_REVISION = "revision";

  /** The parent content id XML attribute. */
  private static final String XML_ATTRIB_PARENTCONTENTID = "parentcontentid";

  /** The edit authorization XML attribute. */
  private static final String XML_ATTRIB_EDIT_AUTH = "editauthorized";

  /** The edit authorization XML attribute. */
  private static final String XML_ATTRIB_PARENT_EDIT_AUTH = "parenteditauthorized";
}
