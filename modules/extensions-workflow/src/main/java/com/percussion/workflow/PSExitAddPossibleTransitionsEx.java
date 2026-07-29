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

import com.percussion.cms.objectstore.PSCmsObject;
import com.percussion.data.PSDataExtractionException;
import com.percussion.extension.IPSExtension;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.server.webservices.PSSearchHandler;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.services.system.PSAssignmentTypeHelper;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.system.utils.PSCms;
import com.percussion.utils.exceptions.PSORMException;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Extended result-document processor exit that augments the supplied result document with the
 * workflow transitions that are permitted for the current item, including hidden form parameters
 * for action links. See {@link IPSResultDocumentProcessor} for the contract of {@link
 * #processResultDocument(Object[], IPSRequestContext, Document)}.
 */
public class PSExitAddPossibleTransitionsEx implements IPSResultDocumentProcessor {

  /** Default constructor for the extension framework. */
  public PSExitAddPossibleTransitionsEx() {}

  private static final Logger log = LogManager.getLogger(PSExitAddPossibleTransitionsEx.class);

  /**
   * This class is used to save and restore workflow state information during a single set of
   * related requests.
   */
  static class PSWorkflowStateCacheKey {
    /** The workflow and state being cached */
    private int m_workflowid, m_stateid;

    /**
     * Ctor
     *
     * @param w Workflow id
     * @param s State id
     */
    public PSWorkflowStateCacheKey(int w, int s) {
      m_workflowid = w;
      m_stateid = s;
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object b) {
      if (b instanceof PSWorkflowStateCacheKey) {
        PSWorkflowStateCacheKey bkey = (PSWorkflowStateCacheKey) b;
        return bkey.m_stateid == m_stateid && bkey.m_workflowid == m_workflowid;
      } else {
        return false;
      }
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return m_workflowid * 100 + m_stateid;
    }
  }

  /**
   * The internal name of the Check-in button. Will not change. The name is 'checkin'. This name
   * will be set as the name attribute of the ActionLink for this button in the output doc.
   */
  public static final String CHECKIN_BUTTON_NAME = "checkin";

  /**
   * The internal name of the Check-out button. Will not change. The name is 'checkout'. This name
   * will be set as the name attribute of the ActionLink for this button in the output doc.
   */
  public static final String CHECKOUT_BUTTON_NAME = "checkout";

  /**
   * The internal name of the Force Check-in button. Will not change. The name is 'forcecheckin'.
   * This name will be set as the name attribute of the ActionLink for this button in the output
   * doc.
   */
  public static final String FORCE_CHECKIN_BUTTON_NAME = "forcecheckin";

  /**
   * The internal name of the Edit button. Will not change. The name is 'edit'. This name will be
   * set as the name attribute of the ActionLink for this button in the output doc.
   */
  public static final String EDIT_BUTTON_NAME = "edit";

  /**
   * The internal name of the Preview button. Will not change. The name is 'preview'. This name will
   * be set as the name attribute of the ActionLink for this button in the output doc.
   */
  public static final String PREVIEW_BUTTON_NAME = "preview";

  /**
   * This is an inner class to encapsulate the parameters. We cannot keep these as class variables
   * due to threading issues. We instantiate this object in the main processrequest method (called
   * by server) and pass around the methods. This is meant for convenience only.
   */
  private static class Params {
    /** data base connection */
    public Connection m_connection = null;

    /** the user for whom the transitions are being computed */
    public String m_userName = null;

    /** the status document element name */
    public String m_statusDocElementName = null;

    /** the content ID Name node name */
    public String m_contentIDNodeName = null;

    /** the content ID Name */
    public String m_contentIDName = null;

    /** Comma-separated list of roles to which the user belongs */
    public String m_roleNameList = "";

    /** <CODE>true</CODE> if the user is an administrator, <CODE>false</CODE> if not. */
    public boolean m_isAdministrator = false;

    /** the assignment type for the user in their "new" role */
    public int m_assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;

    /** Comma-separated list of roles in which the user acts */
    public List m_actorRoleNameList = null;

    /** Comma-separated list of roles in which the user acts */
    public String m_actorRoleNames = "";

    /** Comma-separated list of roles in which the user acts */
    public boolean m_addAssignmentInfoOnly = false;

    /** user's login community */
    public int m_userCommunity = -1;

    /** The content status context for the current content item. */
    public PSContentStatusContext m_contentStatusCtx = null;
  }

  private static String ms_actionTriggerName = "";

  /** The fully qualified name of this extension. */
  private static String m_fullExtensionName = "";

  /* Set the parameter count to not initialized */
  private static int ms_correctParamCount = IPSExtension.NOT_INITIALIZED;

  @SuppressWarnings("unused")
  public Document processResultDocument(Object[] params, IPSRequestContext request, Document resDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    PSWorkFlowUtils.printWorkflowMessage(
        request, "\nAdd Possible Transitions: enter processResultDocument");

    try {
      if (null == request) {
        throw new PSExtensionProcessingException(
            m_fullExtensionName, new IllegalArgumentException("The request must not be null"));
      }

      String lang =
          (String) request.getSessionPrivateObject(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
      if (lang == null) lang = PSI18nUtils.DEFAULT_LANG;
      if (null == params) {
        PSWorkFlowUtils.printWorkflowMessage(
            request, "Add possible transitions: no parameters - exiting");
        return resDoc; // no parameters - exit with peace!
      }

      int nParamCount = params.length;

      Params localParams = new Params();

      try {
        if (nParamCount < 1 || null == params[0] || 0 == params[0].toString().trim().length()) {
          PSWorkFlowUtils.printWorkflowMessage(
              request, "Add possible transitions: no user name - exiting");
          return resDoc;
        }

        localParams.m_userName = params[0].toString();

        if (nParamCount < 2 || null == params[1] || 0 == params[1].toString().trim().length()) {
          throw new PSInvalidParameterTypeException(lang, IPSExtensionErrors.STATUS_DOC_EMPTY);
        }
        localParams.m_statusDocElementName = params[1].toString();

        if (nParamCount < 3 || null == params[2] || 0 == params[2].toString().trim().length()) {
          throw new PSInvalidParameterTypeException(
              lang, IPSExtensionErrors.CONTENTID_NODENAME_EMPTY);
        }
        localParams.m_contentIDNodeName = params[2].toString();

        localParams.m_addAssignmentInfoOnly = false;
        if (nParamCount > 3
            && null != params[3]
            && params[3].toString().trim().equalsIgnoreCase("yes")) {
          localParams.m_addAssignmentInfoOnly = true;
        }

        localParams.m_contentIDNodeName = params[2].toString();
      } catch (PSInvalidParameterTypeException te) {
        String language = te.getLanguageString();
        if (language == null) language = PSI18nUtils.DEFAULT_LANG;
        throw new PSExtensionProcessingException(language, m_fullExtensionName, te);
      }

      getUserInfo(request, localParams);

      // Phase 4d-1a: no longer opens a second pool connection — all reads below
      // (CONTENTSTATUS, STATEROLES, CONTENTTYPES, TRANSITIONS) route through Hibernate
      // factories on the shared session.

      Element element = null;
      NodeList nodes = resDoc.getElementsByTagName(localParams.m_statusDocElementName);

      for (int i = 0; i < nodes.getLength(); i++) {
        element = (Element) nodes.item(i);
        try {
          addWorkflowInfo(request, resDoc, element, localParams);
        } catch (PSEntryNotFoundException xe) {
          PSWorkFlowUtils.printWorkflowMessage(request, xe.getMessage());
        } catch (PSXMLNodeMissingException xe) {
          PSWorkFlowUtils.printWorkflowMessage(request, xe.getMessage());
        } catch (SQLException se) {
          PSWorkFlowUtils.printWorkflowMessage(request, se.getMessage());
        } catch (Exception e) {
          PSWorkFlowUtils.printWorkflowMessage(request, e.getMessage());
        }
      }
    } finally {
      PSWorkFlowUtils.printWorkflowMessage(
          request, "Add Possible Transitions: exit processResultDocument");
    }
    return resDoc;
  }

  /**
   * Gets the user's assigment type from the supplied request context and content id.
   *
   * @param request The request context, it may not be <code>null</code>.
   * @param contentid ID of the content item, it may not be empty or <code>null</code>.
   * @return The highest assignment type for the user, based on the roles in which they are acting.
   * @throws PSExtensionProcessingException if an error occurs.
   */
  public static int getAssignmentType(IPSRequestContext request, String contentid)
      throws PSExtensionProcessingException {
    PSWorkFlowUtils.printWorkflowMessage(
        request, "\nAdd Possible Transitions: enter getAssignmentType");
    int assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;

    try {
      if (request == null) {
        throw new PSExtensionProcessingException(
            m_fullExtensionName, new IllegalArgumentException("The request must not be null"));
      }

      if (contentid == null || contentid.trim().length() == 0) {
        throw new PSExtensionProcessingException(
            m_fullExtensionName,
            new IllegalArgumentException("The contentid must not be null or empty"));
      }

      String userName;
      try {
        userName = request.getUserContextInformation("User/name", "unknown").toString();
      } catch (PSDataExtractionException e) {
        throw new PSExtensionProcessingException(m_fullExtensionName, e);
      }

      String lang =
          (String) request.getSessionPrivateObject(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
      if (lang == null) lang = PSI18nUtils.DEFAULT_LANG;

      if (userName == null || userName.trim().length() == 0) {
        PSWorkFlowUtils.printWorkflowMessage(
            request, "Add possible transitions: no user name - exiting");
        return assignmentType;
      }

      Params localParams = new Params();
      getUserInfo(request, localParams);

      try {
        // Phase 4d-1a: CONTENTSTATUS read via Hibernate (Phase 4b loadComponentSummary).
        int contentID = Integer.parseInt(contentid);
        com.percussion.services.legacy.IPSCmsObjectMgr cms =
            com.percussion.services.legacy.PSCmsObjectMgrLocator.getObjectManager();
        com.percussion.cms.objectstore.PSComponentSummary summary =
            cms.loadComponentSummary(contentID);
        if (summary == null) {
          return assignmentType;
        }
        int nWorkFlowAppID = summary.getWorkflowAppId();
        int nContentStateID = summary.getContentStateId();

        assignmentType =
            getAssignmentType(
                nWorkFlowAppID,
                contentID,
                nContentStateID,
                userName,
                localParams.m_roleNameList,
                request);
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        throw new PSExtensionProcessingException(m_fullExtensionName, e);
      }
    } finally {
      PSWorkFlowUtils.printWorkflowMessage(
          request, "Add Possible Transitions: exit getAssignmentType");
    }

    return assignmentType;
  }

  private void addWorkflowInfo(
      IPSRequestContext req, Document doc, Element elemParent, Params localParams)
      throws SQLException, PSXMLNodeMissingException, PSEntryNotFoundException, PSORMException {
    PSWorkFlowUtils.printWorkflowMessage(req, "  Entering method addWorkflowInfo");

    // get content-id from the "elemParent"
    int contentID = 0;
    localParams.m_contentIDName = null;
    String sContentID = null;

    String lang = (String) req.getSessionPrivateObject(PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG);
    if (lang == null) lang = PSI18nUtils.DEFAULT_LANG;

    if (localParams.m_contentIDNodeName.startsWith("@")) {
      localParams.m_contentIDName = localParams.m_contentIDNodeName.substring(1);
      sContentID = elemParent.getAttribute(localParams.m_contentIDName);
      if (null != sContentID) sContentID = sContentID.trim();
      if (null == sContentID || sContentID.length() < 1) return;
      contentID = Integer.parseInt(sContentID);
    } else {
      NodeList nodes = elemParent.getElementsByTagName(localParams.m_contentIDNodeName);
      if (null == nodes || nodes.getLength() < 1) {
        throw new PSXMLNodeMissingException(lang, IPSExtensionErrors.CONTENTID_NODE_MISSING_EMPTY);
      }
      Element elem = (Element) nodes.item(0);
      localParams.m_contentIDName = elem.getNodeName();
      sContentID = ((Text) (elem.getFirstChild())).getData();
      if (null != sContentID) sContentID = sContentID.trim();
      if (null == sContentID || sContentID.length() < 1) {
        throw new PSXMLNodeMissingException(lang, IPSExtensionErrors.CONTENTID_NODE_MISSING_EMPTY);
      }
      contentID = Integer.parseInt(sContentID);
    }

    if (contentID < 1)
      throw new PSXMLNodeMissingException(lang, IPSExtensionErrors.CONTENTID_NODE_MISSING);

    // Phase 4d-1a: CONTENTSTATUS read via Hibernate (Phase 4b loadComponentSummary).
    com.percussion.cms.objectstore.PSComponentSummary summary =
        com.percussion.services.legacy.PSCmsObjectMgrLocator.getObjectManager()
            .loadComponentSummary(contentID);
    if (summary == null) {
      return;
    }
    if (summary.getObjectType() == PSCmsObject.TYPE_FOLDER) {
      // Phase 4d-1a parity with the legacy getContentInfo: folders do not participate in
      // workflow — do not emit workflow info elements for them.
      return;
    }
    int nWorkflowAppID = summary.getWorkflowAppId();
    int nContentStateID = summary.getContentStateId();
    int nContentTypeID = (int) summary.getContentTypeId();
    String sCheckoutUserName = summary.getCheckoutUserName();
    int nCommunityID = summary.getCommunityId();

    PSContentStatusContext legacyCsc = localParams.m_contentStatusCtx;
    localParams.m_contentStatusCtx = legacyCsc; // keep for compatibility

    // Add workflow info element and set required attributes
    Element elemWorkflowInfo = doc.createElement(ELEMENT_WORKFLOWINFO);
    Node firstChild = elemParent.getFirstChild();
    if (null == firstChild) {
      elemWorkflowInfo = (Element) elemParent.appendChild(elemWorkflowInfo);
    } else {
      elemWorkflowInfo = (Element) elemParent.insertBefore(elemWorkflowInfo, firstChild);
    }

    // Add checkout user name as an attribute of the workflow info element
    elemWorkflowInfo.setAttribute(ATTRIB_CHECKOUTUSERNAME, sCheckoutUserName);

    // Add user info element and attributes
    Element elemUserName = doc.createElement(ELEMENT_USERNAME);
    elemUserName = (Element) elemWorkflowInfo.appendChild(elemUserName);

    // Phase 4d-1a: assignment-type via Hibernate-backed loadFromHibernate.
    int nAssignmentType =
        getAssignmentType(
            nWorkflowAppID,
            contentID,
            nContentStateID,
            localParams.m_userName,
            localParams.m_roleNameList,
            req);

    elemUserName.setAttribute(ATTRIB_ASSIGNMENTTYPE, Integer.toString(nAssignmentType));

    Text text = doc.createTextNode(localParams.m_userName);
    elemUserName.appendChild(text);

    // Add assigned roles information
    addAssignedRolesInfo(doc, elemWorkflowInfo, nWorkflowAppID, nContentStateID, contentID);

    if (localParams.m_addAssignmentInfoOnly) return;

    Map systemParams = PSServer.getContentEditorSystemDef().getParamNames();
    String contentIdParam = "sys_contentid";
    contentIdParam =
        null == systemParams.get(contentIdParam)
            ? contentIdParam
            : (String) systemParams.get(contentIdParam);
    String revisionParam = "sys_revision";
    revisionParam =
        null == systemParams.get(revisionParam)
            ? revisionParam
            : (String) systemParams.get(revisionParam);

    Element hiddenParamsElem = doc.createElement(HIDDEN_PARAMS_NAME);
    hiddenParamsElem.appendChild(createParamNode(doc, contentIdParam, "" + contentID));
    hiddenParamsElem.appendChild(
        createParamNode(doc, revisionParam, req.getParameter(revisionParam)));
    elemWorkflowInfo.appendChild(hiddenParamsElem);

    // Add action list

    // if the user is not logged into the same community as the item
    // do not add the actions.

    if (nCommunityID == localParams.m_userCommunity) {
      addActions(
          doc,
          elemWorkflowInfo,
          contentID,
          nContentStateID,
          nContentTypeID,
          sCheckoutUserName,
          localParams,
          req);
    }

    PSWorkFlowUtils.printWorkflowMessage(req, "  Exiting method addWorkflowInfo");
  }

  /**
   * Get the user information from the supplied request context.
   *
   * @param request The request context, assume not <code>null</code>.
   * @param localParams Class used to contain the returned values, which are <code>m_isAdministrator
   *     </code>, <code>m_roleNameList</code>, <code>m_userCommunity</code>.
   */
  private static void getUserInfo(IPSRequestContext request, Params localParams) {
    String assignmentTypeString =
        request.getParameter(
            PSWorkFlowUtils.properties.getProperty(
                "HTML_PARAM_ASSIGNMENTTYPE", PSWorkFlowUtils.ASSIGNMENT_TYPE_CURRENT_USER));

    if (assignmentTypeString != null
        && assignmentTypeString.equals(String.valueOf((PSWorkFlowUtils.ASSIGNMENT_TYPE_ADMIN)))) {
      localParams.m_isAdministrator = true;
    }

    try {
      localParams.m_roleNameList =
          request.getUserContextInformation("Roles/RoleName", "").toString();
    } catch (Exception e) {
      localParams.m_roleNameList = "";
    }

    try {
      String usercomm = (String) request.getSessionPrivateObject(IPSHtmlParameters.SYS_COMMUNITY);
      if (usercomm != null) localParams.m_userCommunity = Integer.parseInt(usercomm);
    } catch (Exception e) {
      localParams.m_userCommunity = -1;
    }
  }

  /**
   * The the content info for the supplied content-id. The returned info is in the <code>localParams
   * </code> parameters.
   *
   * @param contentID ID of the content item. Pre-Phase-4d-1c callers passed the value the XML app
   *     supplied; the contract does not enforce {@code > 0} (the underlying Hibernate factory
   *     {@code PSContentStatusContext.loadFromHibernate(int)} throws {@code
   *     PSEntryNotFoundException} for unknown ids, which the caller handles).
   * @param userName The user's name, assume not <CODE>null</CODE>
   * @param roleNameList A comma-delimited list of the user's roles, assume not <code>null</code>,
   *     but may be empty.
   * @param localParams Class used to contain the returned values, which are <code>
   *     m_contentStatusCtx</code>, <code>m_isAdministrator</code>, <code>m_checkoutUserName</code>
   * @return <code>true</code> if successful get the content info; <code>false</code> otherwise.
   */
  private static boolean getContentInfo(
      int contentID, String userName, String roleNameList, Params localParams) throws SQLException {
    boolean success = true;
    PSContentStatusContext csc = null;
    try {
      // Phase 4d-1c: read CONTENTSTATUS via the Hibernate factory (Phase 4d-1b
      // PSContentStatusContext.loadFromHibernate) on the shared session -- no
      // second pool connection.
      csc = PSContentStatusContext.loadFromHibernate(contentID);
      if (csc.getObjectType() == PSCmsObject.TYPE_FOLDER) {
        return false;
      }
      localParams.m_contentStatusCtx = csc;
      csc = null;

      boolean isAdmin =
          PSWorkFlowUtils.isAdmin(localParams.m_contentStatusCtx, userName, roleNameList);
      localParams.m_isAdministrator = isAdmin;
    } catch (PSEntryNotFoundException e) {
      success = false;
    }

    return success;
  }

  /**
   * Creates the Param element in the ContentEditor dtd, using the supplied name and value.
   *
   * @param doc The document to which this node will eventually belong. Assumed not <code>null
   *     </code>.
   * @param name The name of the HTML parameter, never empty.
   * @param value The content of the parameter. May be <code>null</code>.
   * @throws IllegalArgumentException if name is <code>null</code> or empty.
   */
  private Node createParamNode(Document doc, String name, String value) {
    if (null == name || name.trim().length() == 0)
      throw new IllegalArgumentException("name can't be null or empty");

    Element paramElem = doc.createElement(PARAM_NAME);
    paramElem.setAttribute(PARAM_NAME_ATTRIB, name);
    paramElem.appendChild(doc.createTextNode(value));
    return paramElem;
  }

  /**
   * Adds assigned role ifo to the supplied doc
   *
   * @param doc The doc, assumed not <code>null</code>.
   * @param elemParent The element to which info is appended, assumed not <code>null</code>.
   * @param nWorkflowAppID The workflow id of the item being processed.
   * @param stateid The state id of the item being processed.
   * @param contentId The content id of the item being processed.
   * @param connection The connection to use, assumed not <code>null</code>.
   * @param request The request to use, assumed not <code>null</code>.
   * @throws SQLException
   */
  private void addAssignedRolesInfo(
      Document doc,
      Element elemParent,
      int nWorkflowAppID,
      int stateid,
      int contentId,
      Connection connection,
      IPSRequestContext request)
      throws SQLException {
    // Phase 4d-1a: ignore the legacy Connection; delegate to the no-Connection overload.
    addAssignedRolesInfo(doc, elemParent, nWorkflowAppID, stateid, contentId);
  }

  /**
   * Hibernate-backed #1561 Phase 4d-1a overload of {@code addAssignedRolesInfo}. Loads the
   * STATEROLES row via the shared Hibernate session — no second pool connection.
   */
  private void addAssignedRolesInfo(
      Document doc, Element elemParent, int nWorkflowAppID, int stateid, int contentId) {
    Element elemAssignedRoles = doc.createElement(ELEMENT_ASSIGNEDROLES);
    elemAssignedRoles = (Element) elemParent.appendChild(elemAssignedRoles);
    Element elemAssignedRole = null;

    PSStateRolesContext src = null;

    try {
      // Phase 4d-1a: STATEROLES read via Hibernate factory (Phase 4b).
      src =
          PSStateRolesContext.loadFromHibernate(
              nWorkflowAppID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE);

      if (null == src) {
        return;
      }

      if (!src.isEmpty()) {
        Text text = null;
        Map stateRoleNameMap = src.getStateRoleNameMap();
        Map assignmentTypeMap = src.getStateRoleAssignmentTypeMap();
        List adhocAnonymousStateRoleIDs = src.getAdhocAnonymousStateRoleIDs();
        List adhocNormalStateRoleIDs = src.getAdhocNormalStateRoleIDs();

        List stateRoleIDs = src.getStateRoleIDs();
        PSAssignmentTypeHelper.filterAssignedRolesByCommunity(contentId, stateRoleIDs);
        Iterator iter = stateRoleIDs.iterator();
        Integer roleID = null;
        Integer assignmentType = null;
        String roleName = null;
        Integer adhocType = null;

        while (iter.hasNext()) {
          roleID = (Integer) iter.next();
          roleName = (String) stateRoleNameMap.get(roleID);

          assignmentType = (Integer) assignmentTypeMap.get(roleID);

          adhocType = Integer.valueOf(PSWorkFlowUtils.ADHOC_DISABLED);
          if (adhocAnonymousStateRoleIDs.contains(roleID)) {
            adhocType = Integer.valueOf(PSWorkFlowUtils.ADHOC_ANONYMOUS);
          } else if (adhocNormalStateRoleIDs.contains(roleID)) {
            adhocType = Integer.valueOf(PSWorkFlowUtils.ADHOC_ENABLED);
          }

          elemAssignedRole = doc.createElement(ELEMENT_ASSIGNEDROLE);

          elemAssignedRole.setAttribute(ATTRIB_ASSIGNMENTTYPE, assignmentType.toString());

          elemAssignedRole.setAttribute(ATTRIB_ADHOCTYPE, adhocType.toString());

          elemAssignedRole.setAttribute(ATTRIB_ROLEID, roleID.toString());

          text = doc.createTextNode(roleName);
          elemAssignedRole.appendChild(text);

          elemAssignedRoles.appendChild(elemAssignedRole);
        }
      }
    } catch (PSEntryNotFoundException e) {
      // No state-roles for this state — ignore.
    } catch (PSRoleException e) {
      // No state-roles for this state — ignore.
    }
  }

  @SuppressWarnings({"unchecked", "unused"})
  private void addActions(
      Document doc,
      Element elemParent,
      int contentID,
      PSContentStatusContext csc,
      Params localParams,
      IPSRequestContext req)
      throws SQLException, PSXMLNodeMissingException, PSEntryNotFoundException, PSORMException {
    // Phase 4d-1a: ignore the legacy Connection and the legacy csc; delegate to the
    // no-Connection overload.
    addActions(
        doc,
        elemParent,
        contentID,
        csc.getContentStateID(),
        csc.getContentTypeID(),
        csc.getContentCheckedOutUserName(),
        localParams,
        req);
  }

  /**
   * Hibernate-backed #1561 Phase 4d-1a overload of {@code addActions}. Reads CONTENTTYPES and
   * TRANSITIONS from the shared Hibernate session — no second pool connection.
   */
  @SuppressWarnings({"unchecked", "unused"})
  private void addActions(
      Document doc,
      Element elemParent,
      int contentID,
      int contentStateID,
      int contentTypeID,
      String sCheckoutUserNameFromSummary,
      Params localParams,
      IPSRequestContext req)
      throws SQLException, PSORMException {
    PSContentTypesContext ctc = null;
    try {
      // Phase 4d-1a: CONTENTTYPES read via Hibernate factory.
      ctc = PSContentTypesContext.loadFromHibernate(contentTypeID);
    } finally {
      if (null != ctc) ctc.close();
    }

    Element elemActionList = doc.createElement(ACTIONLINKSET_NAME);
    elemActionList = (Element) elemParent.appendChild(elemActionList);

    String checkoutUserName = sCheckoutUserNameFromSummary;
    boolean bCheckedOut = ((null != checkoutUserName) && (checkoutUserName.trim().length() > 0));

    // get the dynamic names  for the HTML system params
    Map systemParams = PSServer.getContentEditorSystemDef().getParamNames();
    String commandParam = "sys_command";
    commandParam =
        null == systemParams.get(commandParam)
            ? commandParam
            : (String) systemParams.get(commandParam);

    // create check-in or check-out button
    String triggerValue = null;
    String buttonLabel = null;
    String buttonName = null; // the internal name of the button

    int nWorkflowAppID =
        localParams.m_contentStatusCtx == null ? 0 : localParams.m_contentStatusCtx.getWorkflowID();
    IPSCmsObjectMgr cms = PSCmsObjectMgrLocator.getObjectManager();
    boolean isPublic =
        cms.loadWorkflowState(nWorkflowAppID, contentStateID)
            .map(IPSStatesContext::getIsValid)
            .orElse(false);

    elemParent.setAttribute("isPublic", isPublic ? "y" : "n");

    // force check in button
    if (bCheckedOut
        && localParams.m_isAdministrator
        && (!checkoutUserName.trim().equals(localParams.m_userName.trim()))) {
      triggerValue = PSWorkFlowUtils.properties.getProperty(PSWorkFlowUtils.TRIGGER_FORCE_CHECK_IN);
      buttonLabel = FORCE_CHECKIN_BUTTON_LABEL;
      buttonName = FORCE_CHECKIN_BUTTON_NAME;
    } else if (bCheckedOut) {
      triggerValue = PSWorkFlowUtils.properties.getProperty(PSWorkFlowUtils.TRIGGER_CHECK_IN);
      buttonLabel = CHECKIN_BUTTON_LABEL;
      buttonName = CHECKIN_BUTTON_NAME;
    } else {
      /** Only add the check-out action if the current content item is not in a public state. */
      if (!isPublic) {
        triggerValue = PSWorkFlowUtils.properties.getProperty(PSWorkFlowUtils.TRIGGER_CHECK_OUT);
        buttonLabel = CHECKOUT_BUTTON_LABEL;
        buttonName = CHECKOUT_BUTTON_NAME;
      }
    }

    Map params = new HashMap();
    String lang = "";
    try {
      lang =
          req.getUserContextInformation(
                  PSI18nUtils.USER_CONTEXT_VAR_SYS_LANG, PSI18nUtils.DEFAULT_LANG)
              .toString();
    } catch (Exception e) {
      // ignore
    }
    String tmpLabel = null;

    /**
     * If the trigger value is <code>null</code> now, we do not add an action for checkin or
     * chechout.
     */
    if (triggerValue != null) {
      params.put(ms_actionTriggerName, triggerValue);
      params.put(commandParam, WORKFLOW_COMMAND);

      tmpLabel =
          PSI18nUtils.getString(
              PSI18nUtils.PSX_CE_ACTION + PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST + buttonLabel,
              lang);

      elemActionList.appendChild(
          createActionLink(
              doc, tmpLabel, buttonName, params.entrySet().iterator(), false, false, ""));
    }

    // other non-transition actions
    params.clear();
    params.put(commandParam, PREVIEW_COMMAND);
    params.put(ms_actionTriggerName, "view");

    tmpLabel =
        PSI18nUtils.getString(
            PSI18nUtils.PSX_CE_ACTION
                + PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST
                + PREVIEW_BUTTON_LABEL,
            lang);
    elemActionList.appendChild(
        createActionLink(
            doc, tmpLabel, PREVIEW_BUTTON_NAME, params.entrySet().iterator(), false, false, ""));

    params.clear();
    params.put(commandParam, EDIT_COMMAND);
    params.put(ms_actionTriggerName, "edit");

    tmpLabel =
        PSI18nUtils.getString(
            PSI18nUtils.PSX_CE_ACTION + PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST + EDIT_BUTTON_LABEL,
            lang);
    elemActionList.appendChild(
        createActionLink(
            doc, tmpLabel, EDIT_BUTTON_NAME, params.entrySet().iterator(), false, false, ""));

    // Phase 4d-1a: transition actions via Hibernate-backed loadAllFromHibernate.
    // The legacy PSWorkFlowUtils.getAllowedTransitions overload still requires a raw
    // second-connection read of CONTENTAPPROVALS, so we walk the cursor directly here.
    PSTransitionsContext tc =
        PSTransitionsContext.loadAllFromHibernate(nWorkflowAppID, contentStateID);
    if (!tc.isEmpty()) {
      while (true) {
        try {
          if (tc.isAgingTransition()) {
            if (!tc.moveNext() && tc.isEmpty()) break;
            if (tc.isEmpty()) break;
            continue;
          }
          // Mirror the legacy getAllowedTransitions isDisabled logic: if the user is admin,
          // the transition is always enabled. Otherwise consult the role list.
          boolean isDisabled = !localParams.m_isAdministrator;
          if (!isDisabled) {
            List transitionRequiredRoles = tc.getTransitionRoles();
            if (null != transitionRequiredRoles && transitionRequiredRoles.size() > 0) {
              isDisabled =
                  !PSWorkFlowUtils.compareRoleList(
                      transitionRequiredRoles, localParams.m_actorRoleNames);
            }
          }
          params.clear();
          params.put(ms_actionTriggerName, tc.getTransitionActionTrigger());
          params.put(IPSHtmlParameters.SYS_TRANSITIONID, Integer.toString(tc.getTransitionID()));
          params.put(commandParam, WORKFLOW_COMMAND);

          String transName =
              PSI18nUtils.getString(
                  PSI18nUtils.PSX_WORKFLOW_TRANSITION
                      + PSI18nUtils.LOOKUP_KEY_SEPARATOR
                      + String.valueOf(nWorkflowAppID)
                      + PSI18nUtils.LOOKUP_KEY_SEPARATOR
                      + String.valueOf(tc.getTransitionID())
                      + PSI18nUtils.LOOKUP_KEY_SEPARATOR_LAST
                      + tc.getTransitionLabel(),
                  lang);

          Element actionLink =
              (Element)
                  elemActionList.appendChild(
                      createActionLink(
                          doc,
                          transName,
                          null,
                          params.entrySet().iterator(),
                          true,
                          isDisabled,
                          tc.getTransitionComment()));
          if (actionLink != null) {
            addAssignedRolesInfo(
                doc, actionLink, nWorkflowAppID, tc.getTransitionToStateID(), contentID);
          }
          if (!tc.moveNext()) break;
        } catch (SQLException e) {
          break;
        }
      }
      tc.close();
    }

    return;
  }

  /**
   * Creates an ActionLink element according to the ContentEditor dtd.
   *
   * @param doc The document to which the element will eventually be added. Assumed not to be <code>
   *     null</code>.
   * @param label The display text for the action widget (usually a button). Assumed not to be
   *     <code>null</code> or empty.
   * @param name The internal name of the button. If not <code>null</code> or empty, then the name
   *     param is added to the generated ActionLink element.
   * @param params A list conting 0 or more Map.Entry objects, whose key is the param name and whose
   *     value is the param value. If the key is <code>null</code>, it is skipped. If the value is
   *     <code>null</code>, the parameter is added with an empty value.
   * @param isTransition A flag to indicate whether this action performs a workflow transition or
   *     not. If <code>true</code>, an attribute is set.
   * @param isDisabled A flag to indicate whether this action should be made available to the user.
   * @return The resulting element with its children, never <code>null</code>.
   */
  private static Node createActionLink(
      Document doc,
      String label,
      String name,
      Iterator params,
      boolean isTransition,
      boolean isDisabled,
      String comment) {
    Element actionElem = doc.createElement(ACTIONLINK_NAME);
    Element buttonLabel = doc.createElement(DISPLAYTEXT_NAME);
    buttonLabel.appendChild(doc.createTextNode(label));
    actionElem.appendChild(buttonLabel);
    if (comment.equalsIgnoreCase("y")) {
      actionElem.setAttribute(TRANSITION_ATTRIB_COMMENTREQUIRED, ATTRIB_BOOLEAN_TRUE);
    } else if (comment.equalsIgnoreCase("d")) {
      actionElem.setAttribute(TRANSITION_ATTRIB_COMMENTREQUIRED, ATTRIB_HIDE);
    }
    if (isTransition) actionElem.setAttribute(TRANSITION_ATTRIB, ATTRIB_BOOLEAN_TRUE);
    if (isDisabled) {
      actionElem.setAttribute(DISABLED_ATTRIB, ATTRIB_BOOLEAN_TRUE);
    } else if (isTransition) {
      // If this is a transition and we are not disabled then we need
      // to set the disabled attribute to no. If the attribute did
      // not exist at all we will have incorrect results when we filter
      // the users allowable transitions when we are building the
      // popup action menu.
      actionElem.setAttribute(DISABLED_ATTRIB, ATTRIB_BOOLEAN_FALSE);
    }
    if (null != name && name.trim().length() > 0) actionElem.setAttribute(PARAM_NAME_ATTRIB, name);

    while (params.hasNext()) {
      Map.Entry paramMapping = (Map.Entry) params.next();
      Element paramElem = doc.createElement(PARAM_NAME);
      Object key = paramMapping.getKey();
      if (null == key || key.toString().trim().length() == 0) continue;
      paramElem.setAttribute(PARAM_NAME_ATTRIB, key.toString());
      Object value = paramMapping.getValue();

      if (null == value) value = "";
      paramElem.appendChild(doc.createTextNode(value.toString()));
      actionElem.appendChild(paramElem);
    }
    return actionElem;
  }

  /**
   * Gets the user's assigment type and the roles the user acts in
   *
   * @param workflowID ID of the workflow
   * @param contentID ID of the content item
   * @param itemCommunityID ID of the item's community
   * @param connection open data base connection, assume not <code>null</code>.
   * @param stateid ID of the state
   * @param userName The user's name, assume not <CODE>null</CODE>
   * @param userCommunityID The community-id of the authenticated user in the session. It may be
   *     <code>-1</code> if the community object has not been set in the session of the request
   *     context.
   * @param roleNameList A comma-delimited list of the user's roles, assume not <code>null</code>,
   *     but may be empty.
   * @param isAdministrator <code>true</code> if the current user is administrator type in the
   *     request context; <code>false</code> otherwise.
   * @param localParams Class used to return some values. It may be <code>null</code>. If it is not
   *     <code>null</code>, then the returned values are: <code>m_assignmentType</code>, <code>
   *     m_actorRoleNames</code>, <code>m_actorRoleNameList</code>.
   * @param requestContext the requestContext, assumed not <code>null</code>, is used to store
   *     information across calls during the same request.
   * @return highest assignment type for the user, based on the roles in which they are acting.
   * @throws SQLException if a SQL error occurs
   */
  private static int getAssignmentInfo(
      int workflowID,
      int contentID,
      int itemCommunityID,
      Connection connection,
      int stateid,
      String userName,
      int userCommunityID,
      String roleNameList,
      boolean isAdministrator,
      Params localParams,
      IPSRequestContext requestContext)
      throws SQLException {
    List<Integer> actorRoles = null;
    int assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;
    PSStateRolesContext src = null;

    if (!PSCms.canReadInFolders(contentID)) return PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE;

    if (isAdministrator) {
      assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_ADMIN;
      assignmentType =
          PSWorkFlowUtils.modifyAssignmentType(assignmentType, itemCommunityID, userCommunityID);

      return assignmentType;
    }

    try {
      // Lookup the staterolescontext in the current request. These objects
      // are stored using the stateid and the workflowid of the specific
      // content id
      Object key = new PSWorkflowStateCacheKey(workflowID, stateid);
      Map cache =
          (Map) requestContext.getParameterObject(PSSearchHandler.PSX_WORKFLOW_STATE_INFO_CACHE);
      if (cache != null) {
        src = (PSStateRolesContext) cache.get(key);
      }
      if (src == null) {
        // Phase 4d-1c: read STATEROLES via the Hibernate factory (Phase 4b
        // PSStateRolesContext.loadFromHibernate) on the shared session -- no
        // second pool connection. The legacy `Connection connection` parameter
        // is still required by the downstream `getActorRoles(..., connection,
        // ...)` overload which performs the CONTENTADHOCUSERS read.
        src =
            PSStateRolesContext.loadFromHibernate(
                workflowID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE);
        if (cache != null) cache.put(key, src);
      }

      // By sending true for authUser we are imposing the same rule as authenticateuser
      actorRoles =
          PSWorkflowRoleInfoStatic.getActorRoles(
              contentID, src, userName, roleNameList, connection, true);

      if (null == actorRoles || actorRoles.isEmpty()) {
        /* local params keep their default value */
        return assignmentType;
      }
    } catch (PSEntryNotFoundException e) {
      // fall thru
    } catch (PSRoleException e) {
      // fall thru
    }
    // Get the assignmenttype based on the Workflow
    assignmentType = PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoles);
    // modify the assignment type based on user's login community and item's
    // community
    assignmentType =
        PSWorkFlowUtils.modifyAssignmentType(assignmentType, itemCommunityID, userCommunityID);

    if (localParams != null) {
      localParams.m_assignmentType = assignmentType;
      localParams.m_actorRoleNameList =
          PSWorkflowRoleInfoStatic.roleIDListToRoleNameList(actorRoles, src);
      localParams.m_actorRoleNames =
          PSWorkFlowUtils.listToDelimitedString(
              localParams.m_actorRoleNameList, PSWorkFlowUtils.ROLE_DELIMITER);
    }

    return assignmentType;
  }

  /**
   * Gets the user's assigment type
   *
   * @param workflowID ID of the workflow
   * @param contentID ID of the content item
   * @param connection open data base connection, may not be <code>null</code>.
   * @param stateid ID of the state
   * @param userName The user's name, may not be <CODE>null</CODE> or empty.
   * @param roleNameList A comma-delimited list of the user's roles, may not be <code>null</code>.
   * @param req The current request, may not be <code>null</code>.
   * @return highest assignment type for the user, based on the roles in which they are acting.
   * @throws SQLException if a SQL error occurs
   */
  public static int getAssignmentType(
      int workflowID,
      int contentID,
      Connection connection,
      int stateid,
      String userName,
      String roleNameList,
      IPSRequestContext req)
      throws SQLException {
    if (connection == null) throw new IllegalArgumentException("connection may not be null");
    if (StringUtils.isBlank(userName))
      throw new IllegalArgumentException("userName may not be null");
    if (roleNameList == null) throw new IllegalArgumentException("roleNameList may not be null");
    if (req == null) throw new IllegalArgumentException("req may not be null");

    if (!PSCms.canReadInFolders(contentID)) return PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE;

    List actorRoles = null;
    int assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;
    PSStateRolesContext src = null;

    try {
      // Phase 4d-1c: read STATEROLES via the Hibernate factory (Phase 4b
      // PSStateRolesContext.loadFromHibernate) on the shared session -- no
      // second pool connection. The legacy `Connection connection` parameter
      // is still required by the downstream `getActorRoles(..., connection,
      // ...)` overload which performs the CONTENTADHOCUSERS read.
      src =
          PSStateRolesContext.loadFromHibernate(
              workflowID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE);
      // By sending true for authUser we are imposing the same rule as authenticateuser
      actorRoles =
          PSWorkflowRoleInfoStatic.getActorRoles(
              contentID, src, userName, roleNameList, connection, true);

      if (null == actorRoles || actorRoles.isEmpty()) {
        return assignmentType;
      }
      assignmentType = PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoles);
    } catch (PSEntryNotFoundException e) {
      // fall thru
    } catch (PSRoleException e) {
      // fall thru
    }
    return assignmentType;
  }

  /**
   * Hibernate-backed #1561 Phase 4d-1a overload of the assignment-type lookup. Loads the {@code
   * STATEROLES} row and the {@code CONTENTADHOCUSERS} rows from the shared Hibernate session and
   * delegates to the no-connection {@code PSWorkflowRoleInfoStatic.getActorRoles} overload. No
   * second pool connection is opened.
   *
   * @param workflowID ID of the workflow, must be {@code > 0}.
   * @param contentID ID of the content item, must be {@code > 0}.
   * @param stateid ID of the state, must be {@code > 0}.
   * @param userName The user's name, must not be {@code null} or empty.
   * @param roleNameList A comma-delimited list of the user's roles, may not be {@code null}.
   * @param req The current request, must not be {@code null}.
   * @return highest assignment type for the user, based on the roles in which they are acting.
   */
  public static int getAssignmentType(
      int workflowID,
      int contentID,
      int stateid,
      String userName,
      String roleNameList,
      IPSRequestContext req) {
    if (workflowID <= 0) throw new IllegalArgumentException("workflowID must be > 0");
    if (contentID <= 0) throw new IllegalArgumentException("contentID must be > 0");
    if (stateid <= 0) throw new IllegalArgumentException("stateid must be > 0");
    if (StringUtils.isBlank(userName))
      throw new IllegalArgumentException("userName may not be null");
    if (roleNameList == null) throw new IllegalArgumentException("roleNameList may not be null");
    if (req == null) throw new IllegalArgumentException("req may not be null");

    try {
      if (!PSCms.canReadInFolders(contentID)) return PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE;
    } catch (java.sql.SQLException e) {
      return PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;
    }

    List actorRoles = null;
    int assignmentType = PSWorkFlowUtils.ASSIGNMENT_TYPE_NOT_IN_WORKFLOW;
    PSStateRolesContext src = null;
    PSContentAdhocUsersContext cauc = null;

    try {
      // Phase 4d-1a: STATEROLES read via Hibernate factory (Phase 4b).
      src =
          PSStateRolesContext.loadFromHibernate(
              workflowID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE);
      // Phase 4d-1a: CONTENTADHOCUSERS read via Hibernate factory (Phase 4b).
      cauc = PSContentAdhocUsersContext.loadFromHibernate(contentID);

      // By sending true for authUser we are imposing the same rule as authenticateuser.
      actorRoles = PSWorkflowRoleInfoStatic.getActorRoles(userName, roleNameList, src, cauc, true);

      if (null == actorRoles || actorRoles.isEmpty()) {
        return assignmentType;
      }
      assignmentType = PSWorkflowRoleInfoStatic.getAssignmentType(src, actorRoles);
    } catch (PSEntryNotFoundException e) {
      // fall thru
    } catch (PSRoleException e) {
      // fall thru
    }
    return assignmentType;
  }

  public boolean canModifyStyleSheet() {
    return true;
  }

  @SuppressWarnings("unused")
  public void init(IPSExtensionDef extensionDef, File file) throws PSExtensionException {
    // make sure this is initialized only once
    if (ms_correctParamCount == IPSExtension.NOT_INITIALIZED) {
      ms_correctParamCount = 0;
      ms_actionTriggerName =
          PSWorkFlowUtils.properties.getProperty(
              PSWorkFlowUtils.ACTION_TRIGGER_NAME, PSWorkFlowUtils.DEFAULT_ACTION_TRIGGER_NAME);

      Iterator iter = extensionDef.getRuntimeParameterNames();
      while (iter.hasNext()) {
        iter.next();
        ms_correctParamCount++;
      }

      m_fullExtensionName = extensionDef.getRef().toString();
    }
  }

  /** Element and attribute names for the workflow information node. */
  public static final String ELEMENT_WORKFLOWINFO = "BasicInfo";

  /** Content ID attribute name. */
  public static final String ATTRIB_CONTENTID = "contentId";

  /** Workflow ID attribute name. */
  public static final String ATTRIB_WORKFLOWID = "workflowId";

  /** Name of the hidden form parameters node attached to action links. */
  public static final String HIDDEN_PARAMS_NAME = "HiddenFormParams";

  // element/attribute names for ActionLink and its children
  private static final String ACTIONLINK_NAME = "ActionLink";
  private static final String ACTIONLINKSET_NAME = "ActionLinkList";
  private static final String DISPLAYTEXT_NAME = "DisplayLabel";
  private static final String PARAM_NAME = "Param";
  private static final String PARAM_NAME_ATTRIB = "name";
  private static final String TRANSITION_ATTRIB = "isTransition";
  private static final String TRANSITION_ATTRIB_COMMENTREQUIRED = "commentRequired";
  private static final String DISABLED_ATTRIB = "isDisabled";

  // The next 4 values are the button's label as seen by the end user
  /** Label for the check-in button. */
  private static final String CHECKIN_BUTTON_LABEL = "Check-in";

  /** Label for the check-out button. */
  private static final String CHECKOUT_BUTTON_LABEL = "Check-out";

  /** Label for the force check-in button. */
  private static final String FORCE_CHECKIN_BUTTON_LABEL = "Force Check-in";

  /** Label for the edit button. */
  private static final String EDIT_BUTTON_LABEL = "Edit";

  /** Label for the preview button. */
  private static final String PREVIEW_BUTTON_LABEL = "Preview";

  /** XML attribute value that represents true */
  private static final String ATTRIB_BOOLEAN_TRUE = "yes";

  /** XML attribute value that represents false */
  private static final String ATTRIB_BOOLEAN_FALSE = "no";

  /** XML attribute value that represents false */
  private static final String ATTRIB_HIDE = "hide";

  /** User name element name. */
  public static final String ELEMENT_USERNAME = "UserName";

  /** Assignment type attribute name. */
  public static final String ATTRIB_ASSIGNMENTTYPE = "assignmentType";

  /** Check-out user name attribute name. */
  public static final String ATTRIB_CHECKOUTUSERNAME = "CheckOutUserName";

  /** Adhoc role type attribute name. */
  public static final String ATTRIB_ADHOCTYPE = "adhocType";

  /** Assigned roles container element name. */
  public static final String ELEMENT_ASSIGNEDROLES = "AssignedRoles";

  /** Single assigned role element name. */
  public static final String ELEMENT_ASSIGNEDROLE = "Role";

  /** Role ID attribute name. */
  public static final String ATTRIB_ROLEID = "roleId";

  /** The internal name of the command handler that processes workflow actions. */
  private static final String WORKFLOW_COMMAND = "workflow";

  private static final String PREVIEW_COMMAND = "preview";
  private static final String EDIT_COMMAND = "edit";
}
